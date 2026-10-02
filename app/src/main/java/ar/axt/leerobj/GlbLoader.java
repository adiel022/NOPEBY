package ar.axt.leerobj;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;

public class GlbLoader {

    private static final int MAX_VERTICES = 65535;
    public List<ObjetosCargados.SubMesh> subMeshes = new ArrayList<ObjetosCargados.SubMesh>();
    private static final String TAG = "GlbLoader";

    public List<ObjetosCargados.SubMesh> parseStream(InputStream is) throws IOException {
        subMeshes.clear();
        byte[] data = readAll(is);
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        int magic = bb.getInt();
        if (magic != 0x46546C67) { throw new IOException("No es GLB"); }
        int version = bb.getInt();
        if (version != 2) { throw new IOException("Solo GLB 2.0 soportado"); }
        bb.getInt(); // length
        byte[] jsonChunk = null; byte[] binChunk = null;
        while (bb.remaining() >= 8) {
            int chunkLength = bb.getInt();
            int chunkType = bb.getInt();
            byte[] chunk = new byte[chunkLength]; bb.get(chunk);
            if (chunkType == 0x4E4F534A) { jsonChunk = chunk;
            } else if (chunkType == 0x004E4942) { binChunk = chunk;} }
        if (jsonChunk == null || binChunk == null) {
            throw new IOException("GLB inválido (chunks faltantes)"); }
        try { JSONObject root = new JSONObject(new String(jsonChunk, "UTF-8"));
            parseGlb(root, binChunk);
        } catch (JSONException e) {
            throw new IOException("Error JSON GLB", e);
        } return subMeshes;
    }

	private void parseGlb(JSONObject root, byte[] binChunk)
	throws JSONException, IOException {
		JSONArray meshes = root.getJSONArray("meshes");
		JSONArray accessors = root.getJSONArray("accessors");
		JSONArray bufferViews = root.getJSONArray("bufferViews");
		JSONArray nodes = root.optJSONArray("nodes");
		Map<Integer, List<float[]>> meshWorldMatrices = new HashMap<Integer, List<float[]>>();
		if (nodes != null) {
			java.util.Set<Integer> childIndices = new java.util.HashSet<Integer>();
			for (int n = 0; n < nodes.length(); n++) {
				JSONObject node = nodes.getJSONObject(n);
				JSONArray children = node.optJSONArray("children");
				if (children != null) {
					for (int c = 0; c < children.length(); c++) {
						childIndices.add(children.getInt(c));
					}
				}
			}
			float[] identity = new float[16];
			android.opengl.Matrix.setIdentityM(identity, 0);
			for (int n = 0; n < nodes.length(); n++) {
				if (!childIndices.contains(n)) {
					traverseNode(n, nodes, identity, meshWorldMatrices);
				}
			}
		}
		for (int meshIndex = 0; meshIndex < meshes.length(); meshIndex++) {
			JSONObject mesh = meshes.getJSONObject(meshIndex);
			JSONArray primitives = mesh.getJSONArray("primitives");
			List<float[]> transforms = meshWorldMatrices.get(meshIndex);
			if (transforms == null || transforms.size() == 0) {
				transforms = new ArrayList<float[]>();
				float[] identity = new float[16];
				android.opengl.Matrix.setIdentityM(identity, 0);
				transforms.add(identity);
			}
			for (int p = 0; p < primitives.length(); p++) {
				JSONObject primitive = primitives.getJSONObject(p);
				JSONObject attrs = primitive.getJSONObject("attributes");
				float[] basePositions = readFloatAccessor(
                attrs.getInt("POSITION"),
                accessors, bufferViews, binChunk);
				float[] baseNormals = null;
				if (attrs.has("NORMAL")) {
					baseNormals = readFloatAccessor(
                    attrs.getInt("NORMAL"), accessors, bufferViews, binChunk);
				}
				float[] texcoords = null;
				if (attrs.has("TEXCOORD_0")) {
					texcoords = readFloatAccessor(
                    attrs.getInt("TEXCOORD_0"),
                    accessors, bufferViews, binChunk);
				}
				float[] colors = null;
				if (attrs.has("COLOR_0")) {
					colors = readColorAccessor(
                    attrs.getInt("COLOR_0"),
                    accessors, bufferViews, binChunk);
				}
				int vertexCount = basePositions.length / 3;
				if (colors == null) {
					colors = generateColors(vertexCount);
				}
				short[] indices;
				if (primitive.has("indices")) {
					indices = readIndices(
                        primitive.getInt("indices"),
                        accessors, bufferViews, binChunk);
				} else { indices = generateSequentialIndices(vertexCount);
				}
				if (baseNormals == null) { baseNormals = generateNormals(basePositions, indices);
				}
				String name = mesh.optString(
                "name", "mesh_" + meshIndex + "_p" + p );
				String[] mimeHolder = new String[1];
				byte[] imgBytes = extractEmbeddedTexture(root, primitive, binChunk, mimeHolder);
				android.graphics.Bitmap decodedBitmap = null;
				if (imgBytes != null) {
					try {
						TexturaLoader loader = new TexturaLoader(null);
						decodedBitmap = loader.loadFromGlb(imgBytes, mimeHolder[0]);
					} catch (Exception e) { Log.e(TAG, "Error decodificando bitmap de textura GLB", e); }
				}
				for (int i = 0; i < transforms.size(); i++) {
					float[] positions = basePositions.clone();
					float[] normals = (baseNormals != null)
                    ? baseNormals.clone() : null;
					applyMatrix(positions, normals, transforms.get(i));
					List<ObjetosCargados.SubMesh> parts =
                        splitByIndexLimit( positions,  normals,
						texcoords,  colors,  indices,  name + "_node" + i);
					for (ObjetosCargados.SubMesh part : parts) {
						part.hasOriginalUVs = attrs.has("TEXCOORD_0");
						if (imgBytes != null) {
							part.embeddedTexture = imgBytes;
							part.embeddedTextureMimeType = mimeHolder[0];
						}
						if (decodedBitmap != null) { part.pendingTexture = decodedBitmap;
						}
					} subMeshes.addAll(parts);
				}
			}
		}
	}

	private float[] readFloatAccessor(int accessorIndex,
        JSONArray accessors,JSONArray bufferViews,
        byte[] binChunk) throws IOException, JSONException {
		JSONObject accessor = accessors.getJSONObject(accessorIndex);
		int count = accessor.getInt("count");
		int componentType = accessor.getInt("componentType");
		boolean normalized = accessor.optBoolean("normalized", false);
		String type = accessor.getString("type");
		int comps = getTypeCount(type);
		JSONObject view = bufferViews.getJSONObject(accessor.getInt("bufferView"));
		int offset = view.optInt("byteOffset",0) + accessor.optInt("byteOffset",0);
		int componentSize = getComponentSize(componentType);
		int stride = view.optInt("byteStride", comps * componentSize);
		if(offset<0 || offset>=binChunk.length)
		throw new IOException("Accessor fuera del buffer");
		ByteBuffer bb = ByteBuffer.wrap(binChunk);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		float[] out = new float[count*comps];
		for(int i=0;i<count;i++){
			bb.position(offset + i*stride);
			for(int c=0;c<comps;c++){
				out[i*comps+c]=readComponent(
                bb, componentType, normalized); }
		  } return out;
	  }
	  
	private int getComponentSize(int componentType){
		switch(componentType){
			case 5120:
			case 5121:
				return 1;
			case 5122:
			case 5123:
				return 2;
			case 5125:
			case 5126:
				return 4; }
		throw new RuntimeException(
        "componentType no soportado "
        + componentType);
	}
	
	private float readComponent(
        ByteBuffer bb, int componentType, boolean normalized){
		switch(componentType){
			case 5126:
				return bb.getFloat();
			case 5120:{
					byte v=bb.get();
					if(normalized)
						return Math.max(v/127f,-1f);return v;
			}
			case 5121: {
				int v = bb.get() & 0xFF;
				return v / 255f;
			}
			case 5122:{
					short v=bb.getShort();
					if(normalized)
						return Math.max(v/32767f,-1f); return v;
				}
			case 5123:{ // UNSIGNED_SHORT
					int v=bb.getShort()&0xFFFF;
					if(normalized)
						return v/65535f; return v;
				}
			default: throw new RuntimeException(
            "componentType no soportado " + componentType);
		 }
	   }

	private short[] readIndices(int accessorIndex,
        JSONArray accessors, JSONArray bufferViews,
        byte[] bin) throws JSONException, IOException {
		JSONObject acc = accessors.getJSONObject(accessorIndex);
		int count = acc.getInt("count");
		int componentType = acc.getInt("componentType");
		JSONObject view = bufferViews.getJSONObject(acc.getInt("bufferView"));
		int offset = view.optInt("byteOffset", 0) + acc.optInt("byteOffset", 0);
		ByteBuffer bb = ByteBuffer.wrap(bin);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		short[] out = new short[count];
		for (int i = 0; i < count; i++) {
			bb.position(offset + i * getIndexComponentSize(componentType));
			switch (componentType) {
				case 5121:
					out[i] = (short) (bb.get() & 0xFF);
					break;
				case 5123:
					out[i] = (short) (bb.getShort() & 0xFFFF);
					break;
				case 5125: {
						long v = bb.getInt() & 0xFFFFFFFFL;
						if (v > 65535)
							throw new IOException("Indice fuera de rango 16-bit: " + v);
						    out[i] = (short) v; break; }  default:
					throw new IOException( "Tipo indice no soportado: " + componentType);
			  }
		  } return out;
	   }
	
	private int getIndexComponentSize(int componentType) {
		switch (componentType) {
			case 5121: return 1;
			case 5123: return 2;
			case 5125: return 4;
			default:
			throw new RuntimeException("Index type no soportado: " + componentType);
		}
	}

    private float[] generateNormals(float[] v, short[] i) {
		Log.d(TAG, "generateNormals()");
		Log.d(TAG, "vertexCount = " + (v.length / 3));
		Log.d(TAG, "indexCount = " + i.length);
		int vertexCount = v.length / 3;
		for (int k = 0; k < i.length; k += 3) {
			int a = i[k] & 0xFFFF;
			int b = i[k + 1] & 0xFFFF;
			int c = i[k + 2] & 0xFFFF;
			if (a >= vertexCount || b >= vertexCount || c >= vertexCount) {
				throw new RuntimeException( "Indice fuera de rango: "
				+ a + "," + b + "," + c + " vertexCount=" + vertexCount); }
		 }
		float[] n = new float[v.length];
		for (int k = 0; k < i.length; k += 3) {
			int a = i[k] & 0xFFFF;
			int b = i[k + 1] & 0xFFFF;
			int c = i[k + 2] & 0xFFFF;
			int i0 = a * 3;
			int i1 = b * 3;
			int i2 = c * 3;
			float x1 = v[i1] - v[i0];
			float y1 = v[i1 + 1] - v[i0 + 1];
			float z1 = v[i1 + 2] - v[i0 + 2];
			float x2 = v[i2] - v[i0];
			float y2 = v[i2 + 1] - v[i0 + 1];
			float z2 = v[i2 + 2] - v[i0 + 2];
			float nx = y1 * z2 - z1 * y2;
			float ny = z1 * x2 - x1 * z2;
			float nz = x1 * y2 - y1 * x2;
			n[i0] += nx; n[i0 + 1] += ny; n[i0 + 2] += nz;
			n[i1] += nx; n[i1 + 1] += ny; n[i1 + 2] += nz;
			n[i2] += nx; n[i2 + 1] += ny; n[i2 + 2] += nz;
		}
		for (int j = 0; j < n.length; j += 3) {
			float l = (float)Math.sqrt( n[j] * n[j] +
			n[j + 1] * n[j + 1] + n[j + 2] * n[j + 2]);
			if (l > 0) { n[j] /= l; n[j + 1] /= l; n[j + 2] /= l; }
		} return n;
	 }
	 
	private List<ObjetosCargados.SubMesh> splitByIndexLimit(
        float[] positions, float[] normals, float[] texcoords,
        float[] colors, short[] indices, String baseName) {
		List<ObjetosCargados.SubMesh> result = new ArrayList<>();
		List<Float> vList = new ArrayList<>();
		List<Float> nList = new ArrayList<>();
		List<Float> tList = new ArrayList<>();
		List<Float> cList = new ArrayList<>();
		List<Integer> iList = new ArrayList<>();
		Map<Integer, Integer> remap = new HashMap<>();
		int localVertexCount = 0; int part = 0;
		int colorComponents = 0;
		if (colors != null) {
			colorComponents = colors.length / (positions.length / 3); }
		for (int i = 0; i < indices.length; i++) {
			int oldIndex = indices[i] & 0xFFFF;
			Integer newIndex = remap.get(oldIndex);
			if (newIndex == null) {
				if (localVertexCount >= MAX_VERTICES) {
					result.add(flushPartial(
				    vList, nList, tList,
				    cList, iList, baseName + "_part" + part));
					vList.clear(); nList.clear(); tList.clear();
					cList.clear(); iList.clear(); remap.clear();
					localVertexCount = 0; part++;
				}
				newIndex = localVertexCount++;
				remap.put(oldIndex, newIndex);
				int vi = oldIndex * 3;
				vList.add(positions[vi]);
				vList.add(positions[vi + 1]);
				vList.add(positions[vi + 2]);
				if (normals != null) {
					nList.add(normals[vi]);
					nList.add(normals[vi + 1]);
					nList.add(normals[vi + 2]);
				}
				if (texcoords != null) {
					int ti = oldIndex * 2;
					tList.add(texcoords[ti]);
					tList.add(texcoords[ti + 1]);
				}
				if (colors != null) {
					int ci = oldIndex * colorComponents;
					for (int c = 0; c < colorComponents; c++) {
						cList.add(colors[ci + c]);
					}
				}
			} iList.add(newIndex);
		}
		if (!iList.isEmpty()) {
			result.add(flushPartial( vList, nList, tList,
		    cList, iList, baseName + "_part" + part));
		} return result;
	}
	
	private float[] readColorAccessor(int accessorIndex,
		JSONArray accessors, JSONArray bufferViews,
	    byte[] binChunk) throws IOException, JSONException {
		JSONObject accessor = accessors.getJSONObject(accessorIndex);
		int count = accessor.getInt("count");
		String type = accessor.getString("type");
		int comps = getTypeCount(type);
		float[] raw = readFloatAccessor(
        accessorIndex, accessors, bufferViews, binChunk);
		if (comps == 3) return raw;
		if (comps == 4) {float[] rgb = new float[count * 3];
		int src = 0;
			for (int i = 0; i < count; i++) {
				rgb[i * 3] = raw[src];
				rgb[i * 3 + 1] = raw[src + 1];
				rgb[i * 3 + 2] = raw[src + 2];
				src += 4;
			} return rgb;
		}
		throw new IOException("Color formato inválido: " + type);
	}
	
	private ObjetosCargados.SubMesh flushPartial(
		List<Float> vList, List<Float> nList,
		List<Float> tList, List<Float> cList,
		List<Integer> iList, String name) {
		ObjetosCargados.SubMesh sm = new ObjetosCargados.SubMesh();
		sm.name = name;
		sm.vertexBuffer = toFloatBuffer(listToArray(vList));
		sm.normalBuffer = toFloatBuffer(listToArray(nList));
		sm.texcoordBuffer = toFloatBuffer(listToArray(tList));
		sm.colorBuffer = toFloatBuffer(listToArray(cList));
		short[] idx = new short[iList.size()];
		for (int i = 0; i < iList.size(); i++) {
		idx[i] = (short) (int) iList.get(i); }
		sm.indexBuffer = toShortBuffer(idx);
		sm.numIndices = idx.length;
		return sm;
	}
	
	private void traverseNode(int nodeIdx, JSONArray nodes, float[] parentMatrix, Map<Integer, List<float[]>> meshWorldMatrices) throws JSONException {
		if (nodes == null || nodeIdx < 0 || nodeIdx >= nodes.length()) return;
		JSONObject node = nodes.getJSONObject(nodeIdx);
		float[] localMatrix = getLocalNodeMatrix(node);
		float[] globalMatrix = new float[16];
		if (parentMatrix != null) {
			android.opengl.Matrix.multiplyMM(globalMatrix, 0, parentMatrix, 0, localMatrix, 0);
		} else { System.arraycopy(localMatrix, 0, globalMatrix, 0, 16); }
		if (node.has("mesh")) {
			int meshId = node.getInt("mesh");
			List<float[]> list = meshWorldMatrices.get(meshId);
			if (list == null) {
				list = new ArrayList<float[]>();
				meshWorldMatrices.put(meshId, list);
			} list.add(globalMatrix);
		}
		JSONArray children = node.optJSONArray("children");
		if (children != null) {
			for (int c = 0; c < children.length(); c++) {
				traverseNode(children.getInt(c), nodes, globalMatrix, meshWorldMatrices);
			}
		}
	}

	private float[] getLocalNodeMatrix(JSONObject node) {
		float[] m = new float[16];
		android.opengl.Matrix.setIdentityM(m, 0);
		if (node.has("matrix")) {
			JSONArray mt = node.optJSONArray("matrix");
			if (mt != null && mt.length() == 16) {
				for (int i = 0; i < 16; i++) {
					m[i] = (float) mt.optDouble(i, 0.0);
				} return m;
			}
		}
		float[] t = {0f, 0f, 0f};
		JSONArray tr = node.optJSONArray("translation");
		if (tr != null) {
			t[0] = (float) tr.optDouble(0, 0.0);
			t[1] = (float) tr.optDouble(1, 0.0);
			t[2] = (float) tr.optDouble(2, 0.0);
		}
		float[] s = {1f, 1f, 1f};
		JSONArray sc = node.optJSONArray("scale");
		if (sc != null) {
			s[0] = (float) sc.optDouble(0, 1.0);
			s[1] = (float) sc.optDouble(1, 1.0);
			s[2] = (float) sc.optDouble(2, 1.0);
		}
		float[] q = {0f, 0f, 0f, 1f};
		JSONArray rt = node.optJSONArray("rotation");
		if (rt != null) {
			q[0] = (float) rt.optDouble(0, 0.0);
			q[1] = (float) rt.optDouble(1, 0.0);
			q[2] = (float) rt.optDouble(2, 0.0);
			q[3] = (float) rt.optDouble(3, 1.0);
			q = normalizeQuat(q);
		}
		float[] rotMat = quatToMatrix(q);
		float[] scaleMat = new float[16];
		android.opengl.Matrix.setIdentityM(scaleMat, 0);
		android.opengl.Matrix.scaleM(scaleMat, 0, s[0], s[1], s[2]);
		float[] rotScaleMat = new float[16];
		android.opengl.Matrix.multiplyMM(rotScaleMat, 0, rotMat, 0, scaleMat, 0);
		android.opengl.Matrix.setIdentityM(m, 0);
		android.opengl.Matrix.translateM(m, 0, t[0], t[1], t[2]);
		android.opengl.Matrix.multiplyMM(m, 0, m, 0, rotScaleMat, 0);
		return m;
	}

	private float[] quatToMatrix(float[] q) {
		float[] m = new float[16];
		float x = q[0], y = q[1], z = q[2], w = q[3];
		float x2 = x + x, y2 = y + y, z2 = z + z;
		float xx = x * x2, xy = x * y2, xz = x * z2;
		float yy = y * y2, yz = y * z2, zz = z * z2;
		float wx = w * x2, wy = w * y2, wz = w * z2;
		m[0] = 1.0f - (yy + zz);
		m[1] = xy + wz;
		m[2] = xz - wy;
		m[3] = 0.0f;
		m[4] = xy - wz;
		m[5] = 1.0f - (xx + zz);
		m[6] = yz + wx;
		m[7] = 0.0f;
		m[8] = xz + wy;
		m[9] = yz - wx;
		m[10] = 1.0f - (xx + yy);
		m[11] = 0.0f;
		m[12] = 0.0f;
		m[13] = 0.0f;
		m[14] = 0.0f;
		m[15] = 1.0f;
		return m;
	}


	private static class NodeTransform {
		public float[] translation = new float[]{ 0f,0f,0f };
		public float[] rotation = new float[]{ 0f,0f,0f,1f};
		public float[] scale = new float[]{ 1f,1f,1f};
		public float[] matrix = null; }

	private void applyTransform(float[] pos, float[] norm, NodeTransform t) {
		if (t.matrix != null) {applyMatrix(pos, norm, t.matrix);return;}
		float sx = t.scale[0]; float sy = t.scale[1]; float sz = t.scale[2];
		for (int i = 0; i < pos.length; i += 3) {
			float x = pos[i] * sx;
			float y = pos[i + 1] * sy;
			float z = pos[i + 2] * sz;
			float[] r = rotateQuat(x, y, z, t.rotation);
			pos[i]     = r[0] + t.translation[0];
			pos[i + 1] = r[1] + t.translation[1];
			pos[i + 2] = r[2] + t.translation[2];
		}
		if (norm == null) return;
		for (int i = 0; i < norm.length; i += 3) {
			float[] r = rotateQuat(norm[i],
            norm[i + 1], norm[i + 2], t.rotation );
			norm[i] = r[0];
			norm[i + 1] = r[1];
			norm[i + 2] = r[2];
		}
	}
	
	private float[] normalizeQuat(float[] q) {
		float len = (float) Math.sqrt(
		q[0] * q[0] + q[1] * q[1] +
		q[2] * q[2] + q[3] * q[3] );
		if (len == 0f) return new float[]{0, 0, 0, 1};
		return new float[]{
		q[0] / len, q[1] / len,
		q[2] / len, q[3] / len};
	}

	private float[] rotateQuat(float x, float y, float z, float[] q) {
		float qx = q[0], qy = q[1], qz = q[2], qw = q[3];
		float ix =  qw * x + qy * z - qz * y;
		float iy =  qw * y + qz * x - qx * z;
		float iz =  qw * z + qx * y - qy * x;
		float iw = -qx * x - qy * y - qz * z;
		float rx = ix * qw + iw * (-qx) + iy * (-qz) - iz * (-qy);
		float ry = iy * qw + iw * (-qy) + iz * (-qx) - ix * (-qz);
		float rz = iz * qw + iw * (-qz) + ix * (-qy) - iy * (-qx);
		return new float[]{rx, ry, rz};
	}
	 
	private void applyMatrix(float[] positions, float[] normals, float[] m) {
		for (int i = 0; i < positions.length; i += 3) {
			float x = positions[i]; float y = positions[i + 1];float z = positions[i + 2];
			positions[i]= m[0]*x + m[4]*y + m[8]*z  + m[12];
			positions[i + 1] = m[1]*x + m[5]*y + m[9]*z  + m[13];
			positions[i + 2] = m[2]*x + m[6]*y + m[10]*z + m[14];
		}
		if (normals == null) return;
		for (int i = 0; i < normals.length; i += 3) {
			float x = normals[i];
			float y = normals[i + 1];
			float z = normals[i + 2];
			normals[i]     = m[0]*x + m[4]*y + m[8]*z;
			normals[i + 1] = m[1]*x + m[5]*y + m[9]*z;
			normals[i + 2] = m[2]*x + m[6]*y + m[10]*z;
		}
	}
		
	private float[] listToArray(List<Float> list) {
		float[] arr = new float[list.size()];
		for (int i = 0; i < list.size(); i++) {
			arr[i] = list.get(i); } return arr;
	}

    private short[] generateSequentialIndices(int count) {
        short[] i = new short[count];
        for (int j = 0; j < count; j++) i[j] = (short) j;
        return i;
    }

    private float[] generateColors(int count) {
        float[] c = new float[count * 3];
        for (int i = 0; i < count; i++) {
            c[i * 3] = 0.75f;
            c[i * 3 + 1] = 0.75f;
            c[i * 3 + 2] = 0.75f;
        } return c;
    }

    private int getTypeCount(String type) {
        if ("VEC2".equals(type)) return 2;
        if ("VEC3".equals(type)) return 3;
        if ("VEC4".equals(type)) return 4;
        return 1;
    }

    private FloatBuffer toFloatBuffer(float[] d) {
        if (d == null) return null;
        ByteBuffer bb = ByteBuffer.allocateDirect(d.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(d); fb.position(0); return fb;
    }

    private ShortBuffer toShortBuffer(short[] d) {
        ByteBuffer bb = ByteBuffer.allocateDirect(d.length * 2);
        bb.order(ByteOrder.nativeOrder());
        ShortBuffer sb = bb.asShortBuffer();
        sb.put(d); sb.position(0);return sb;
    }

    private byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
        return baos.toByteArray();
    }

	private byte[] extractEmbeddedTexture(JSONObject root, JSONObject primitive, byte[] binChunk, String[] outMimeType) {
		try {
			if (!primitive.has("material")) return null;
			int materialIndex = primitive.getInt("material");
			JSONArray materials = root.optJSONArray("materials");
			if (materials == null || materialIndex < 0 || materialIndex >= materials.length()) return null;
			JSONObject mat = materials.getJSONObject(materialIndex);
			JSONObject pbr = mat.optJSONObject("pbrMetallicRoughness");
			if (pbr == null) return null;
			JSONObject baseColorTex = pbr.optJSONObject("baseColorTexture");
			if (baseColorTex == null) return null;
			int texIndex = baseColorTex.getInt("index");
			JSONArray textures = root.optJSONArray("textures");
			if (textures == null || texIndex < 0 || texIndex >= textures.length()) return null;
			JSONObject tex = textures.getJSONObject(texIndex);
			int imgIndex = tex.optInt("source", -1);
			JSONArray images = root.optJSONArray("images");
			if (images == null || imgIndex < 0 || imgIndex >= images.length()) return null;
			JSONObject img = images.getJSONObject(imgIndex);
			if (outMimeType != null && outMimeType.length > 0) {
				outMimeType[0] = img.optString("mimeType", "image/png");
			}
			if (img.has("bufferView")) {
				int bvIndex = img.getInt("bufferView");
				JSONArray bufferViews = root.getJSONArray("bufferViews");
				if (bvIndex >= 0 && bvIndex < bufferViews.length()) {
					JSONObject bv = bufferViews.getJSONObject(bvIndex);
					int offset = bv.optInt("byteOffset", 0);
					int length = bv.getInt("byteLength");
					if (offset >= 0 && offset + length <= binChunk.length) {
						byte[] imgBytes = new byte[length];
						System.arraycopy(binChunk, offset, imgBytes, 0, length);
						return imgBytes;
					}
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Error leyendo textura embebida en GLB", e);
		} return null;
	}

}
