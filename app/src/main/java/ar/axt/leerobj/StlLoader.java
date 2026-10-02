package ar.axt.leerobj;

import java.io.*;
import java.nio.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class StlLoader {

    private static final int MAX_VERTICES = 65535;
	private Map<VertexKey,Integer> vertexMap = new HashMap<VertexKey,Integer>();

	private static class VertexKey {
		final float vx, vy, vz;
		VertexKey(float vx, float vy, float vz) {
		  this.vx = vx; this.vy = vy; this.vz = vz;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof VertexKey)) return false;
			VertexKey v = (VertexKey) o;
			return vx == v.vx && vy == v.vy && vz == v.vz;
		}

		@Override
		public int hashCode() {
			int h = Float.floatToIntBits(vx);
			h = 31 * h + Float.floatToIntBits(vy);
			h = 31 * h + Float.floatToIntBits(vz);
			return h; }
	    }
	
    public List<ObjetosCargados.SubMesh> subMeshes = new ArrayList<ObjetosCargados.SubMesh>();
	  
	public List<ObjetosCargados.SubMesh> load(InputStream is) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(is); bis.mark(4096);
		byte[] header = new byte[80]; readFully(bis, header);
		boolean ascii = detectAscii(bis); bis.reset();
		// RESET CRÍTICO (evita OOM)
		vertexMap.clear(); subMeshes.clear();
		return ascii ? loadAscii(bis) : loadBinary(bis);
	}

    private boolean detectAscii(BufferedInputStream bis) throws IOException {
        byte[] probe = new byte[512]; int n = bis.read(probe); if (n <= 0) return false;
        String text = new String(probe, 0, n).toLowerCase();
        return text.indexOf("facet") != -1 &&
	    text.indexOf("vertex") != -1;
    }

    private List<ObjetosCargados.SubMesh> loadBinary(InputStream is) throws IOException {
		byte[] header = new byte[80]; readFully(is, header); int triCount = readIntLE(is);
		List<Float> vList = new ArrayList<>();
		List<Float> nList = new ArrayList<>();
		List<Integer> iList = new ArrayList<>();
		int vertexIndex = 0; int part = 0;
		for (int i = 0; i < triCount; i++) {
			float nx = readFloatLE(is);
			float ny = readFloatLE(is);
			float nz = readFloatLE(is);
			for (int k = 0; k < 3; k++) {
				float vx = readFloatLE(is);
				float vy = readFloatLE(is);
				float vz = readFloatLE(is);
				VertexKey key = new VertexKey(vx, vy, vz);
				Integer index = vertexMap.get(key);
				if (index == null) {index = vertexIndex++; vertexMap.put(key, index);
					vList.add(vx);
					vList.add(vy);
					vList.add(vz);
		       } iList.add(index);
			} is.read(new byte[2]);
			if (vertexIndex >= MAX_VERTICES) {
				flush(vList, nList, iList, "stl_bin_" + part++);
				vList.clear(); nList.clear(); iList.clear();
				vertexMap.clear();
				vertexIndex = 0;
			}
		}
		if (!iList.isEmpty()) { flush(vList, nList, iList, "stl_bin_" + part);
		} return subMeshes;
	}

    private List<ObjetosCargados.SubMesh> loadAscii(InputStream is) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		List<Float> vList = new ArrayList<>();
		List<Float> nList = new ArrayList<>();
		List<Integer> iList = new ArrayList<>();
		int vertexIndex = 0; int part = 0;
		float nx = 0, ny = 0, nz = 0;
		String line;
		while ((line = br.readLine()) != null) {
			line = line.trim();
			if (line.startsWith("facet normal")) {
				String[] t = line.split("\\s+");
				nx = Float.parseFloat(t[2]);
				ny = Float.parseFloat(t[3]);
				nz = Float.parseFloat(t[4]);
			}
			if (line.startsWith("vertex")) {
				String[] t = line.split("\\s+");
				float vx = Float.parseFloat(t[1]);
				float vy = Float.parseFloat(t[2]);
				float vz = Float.parseFloat(t[3]);
				VertexKey key = new VertexKey(vx, vy, vz);
				Integer index = vertexMap.get(key);
				if (index == null) {
					index = vertexIndex++;
					vertexMap.put(key, index);
					vList.add(vx);
					vList.add(vy);
					vList.add(vz);
				} iList.add(index);
			}
			if (vertexIndex >= MAX_VERTICES) {
				flush(vList, nList, iList, "stl_ascii_" + part++);
				vList.clear();
				nList.clear();
				iList.clear();
				vertexMap.clear();
				vertexIndex = 0;
			}
		}
		if (!iList.isEmpty()) { flush(vList, nList, iList, "stl_ascii_" + part);
		} return subMeshes;
	  }

	private void flush(List<Float> v,
		List<Float> n, List<Integer> i, String name) {
		if (v.isEmpty() || i.isEmpty()) return;
		n.clear();
		generateNormals(v, i, n);
		ObjetosCargados.SubMesh sm = new ObjetosCargados.SubMesh();
		sm.name = name;
		sm.vertexBuffer = toFloatBuffer(v);
		sm.normalBuffer = toFloatBuffer(n);
		List<Float> tList = new ArrayList<>();
		generateUVs(v, n, tList);
		sm.texcoordBuffer = toFloatBuffer(tList);
		sm.indexBuffer = toShortBuffer(i);
		sm.numIndices = i.size();
		sm.colorBuffer = toFloatBuffer(generateColors(v.size() / 3));
		subMeshes.add(sm);
	}
	
	private List<Float> generateColors(int vertexCount) {
		List<Float> c = new ArrayList<>(vertexCount * 3);
		float r = 0.75f;
		float g = 0.75f;
		float b = 0.75f;
		for (int i = 0; i < vertexCount; i++) {c.add(r); c.add(g); c.add(b); } return c;
	}
	
	private void generateNormals(List<Float> vList,
		List<Integer> iList, List<Float> nList) {
		int vertexCount = vList.size() / 3;
		for (int i = 0; i < vertexCount * 3; i++) {
			nList.add(0f); }
		for (int i = 0; i < iList.size(); i += 3) {
			int i0 = iList.get(i);
			int i1 = iList.get(i + 1);
			int i2 = iList.get(i + 2);
			float v0x = vList.get(i0 * 3);
			float v0y = vList.get(i0 * 3 + 1);
			float v0z = vList.get(i0 * 3 + 2);
			float v1x = vList.get(i1 * 3);
			float v1y = vList.get(i1 * 3 + 1);
			float v1z = vList.get(i1 * 3 + 2);
			float v2x = vList.get(i2 * 3);
			float v2y = vList.get(i2 * 3 + 1);
			float v2z = vList.get(i2 * 3 + 2);
			float e1x = v1x - v0x;
			float e1y = v1y - v0y;
			float e1z = v1z - v0z;
			float e2x = v2x - v0x;
			float e2y = v2y - v0y;
			float e2z = v2z - v0z;
			float nx = e1y * e2z - e1z * e2y;
			float ny = e1z * e2x - e1x * e2z;
			float nz = e1x * e2y - e1y * e2x;
			addNormal(nList, i0, nx, ny, nz);
			addNormal(nList, i1, nx, ny, nz);
			addNormal(nList, i2, nx, ny, nz);
		}
		for (int i = 0; i < vertexCount; i++) {
			float nx = nList.get(i * 3);
			float ny = nList.get(i * 3 + 1);
			float nz = nList.get(i * 3 + 2);
			float len = (float)Math.sqrt(nx*nx + ny*ny + nz*nz);
			if (len > 0f) {
				nList.set(i * 3, nx / len);
				nList.set(i * 3 + 1, ny / len);
				nList.set(i * 3 + 2, nz / len);
			}
	   	  }
	    }

	private void addNormal(List<Float> nList, int index,
		float nx, float ny, float nz) {int base = index * 3;
		nList.set(base,nList.get(base) + nx);
		nList.set(base + 1, nList.get(base + 1) + ny);
		nList.set(base + 2, nList.get(base + 2) + nz);
	}


	private void generateUVs(List<Float> vList, List<Float> nList, List<Float> tList) {
		tList.clear();
		int numVertices = vList.size() / 3;
		if (numVertices == 0) return;
		float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
		float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
		float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
		for (int i = 0; i < numVertices; i++) {
			float x = vList.get(i * 3);
			float y = vList.get(i * 3 + 1);
			float z = vList.get(i * 3 + 2);
			if (x < minX) minX = x; if (x > maxX) maxX = x;
			if (y < minY) minY = y; if (y > maxY) maxY = y;
			if (z < minZ) minZ = z; if (z > maxZ) maxZ = z;
		}
		float sizeX = Math.max(maxX - minX, 0.0001f);
		float sizeY = Math.max(maxY - minY, 0.0001f);
		float sizeZ = Math.max(maxZ - minZ, 0.0001f);
		for (int i = 0; i < numVertices; i++) {
			float x = vList.get(i * 3);
			float y = vList.get(i * 3 + 1);
			float z = vList.get(i * 3 + 2);
			float nx = nList.get(i * 3);
			float ny = nList.get(i * 3 + 1);
			float nz = nList.get(i * 3 + 2);
			float absX = Math.abs(nx), absY = Math.abs(ny), absZ = Math.abs(nz);
			float localU = 0f, localV = 0f;
			int col = 0, row = 0;
			if (absX >= absY && absX >= absZ) {
				if (nx > 0) { localU = (maxZ - z) / sizeZ; localV = (y - minY) / sizeY; col = 2; row = 0; }
				else        { localU = (z - minZ) / sizeZ; localV = (y - minY) / sizeY; col = 0; row = 1; }
			} else if (absY >= absX && absY >= absZ) {
				if (ny > 0) { localU = (x - minX) / sizeX; localV = (maxZ - z) / sizeZ; col = 1; row = 1; }
				else        { localU = (x - minX) / sizeX; localV = (z - minZ) / sizeZ; col = 2; row = 1; }
			} else {
				if (nz >= 0){ localU = (x - minX) / sizeX; localV = (y - minY) / sizeY; col = 0; row = 0; }
				else        { localU = (maxX - x) / sizeX; localV = (y - minY) / sizeY; col = 1; row = 0; }
			}
			tList.add((col + localU) / 3.0f);
			tList.add((row + localV) / 2.0f);
		}
	}

    private FloatBuffer toFloatBuffer(List<Float> list) {
        ByteBuffer bb = ByteBuffer.allocateDirect(list.size() * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        for (int i = 0; i < list.size(); i++) {
            fb.put(list.get(i).floatValue());
        } fb.position(0); return fb;
    }

    private ShortBuffer toShortBuffer(List<Integer> list) {
        ByteBuffer bb = ByteBuffer.allocateDirect(list.size() * 2);
        bb.order(ByteOrder.nativeOrder());
        ShortBuffer sb = bb.asShortBuffer();
        for (int i = 0; i < list.size(); i++) {
            sb.put((short) list.get(i).intValue());
        } sb.position(0); return sb;
    }

    private int readIntLE(InputStream is) throws IOException {
        return (is.read() & 0xFF) | ((is.read() & 0xFF) << 8) |
		((is.read() & 0xFF) << 16) | ((is.read() & 0xFF) << 24);
    }

    private float readFloatLE(InputStream is) throws IOException {
        return Float.intBitsToFloat(readIntLE(is));
    }

    private void readFully(InputStream is, byte[] buffer) throws IOException {
        int read = 0;
        while (read < buffer.length) {
            int r = is.read(buffer, read, buffer.length - read);
            if (r == -1) throw new IOException("EOF");
            read += r;
        }
    }

}
