package ar.axt.leerobj;

import android.net.Uri;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ar.axt.leerobj.ObjetosCargados;

public class ObjLoader {

    private static final int MAX_VERTICES = 65535;

    public List<ObjetosCargados.SubMesh> subMeshes = new ArrayList<>();

    public static class FloatArrayList {
        public float[] data;
        public int size;
        public FloatArrayList(int capacity) {
            data = new float[capacity];
            size = 0;
        }

        public void add(float val) {
            if (size == data.length) {
                float[] newData = new float[data.length * 2];
                System.arraycopy(data, 0, newData, 0, data.length);
                data = newData;
            } data[size++] = val;
        }

        public float get(int index) { return data[index]; }
        public void set(int index, float val) { data[index] = val; }
        public void clear() { size = 0; }
        public boolean isEmpty() { return size == 0; }
    }

    public static class IntArrayList {
        public int[] data;
        public int size;
        public IntArrayList(int capacity) {
            data = new int[capacity];
            size = 0;
        }

        public void add(int val) {
            if (size == data.length) {
                int[] newData = new int[data.length * 2];
                System.arraycopy(data, 0, newData, 0, data.length);
                data = newData;
            } data[size++] = val;
        }

        public int get(int index) { return data[index]; }
        public void set(int index, int val) { data[index] = val; }
        public void clear() { size = 0; }
        public boolean isEmpty() { return size == 0; }
    }

    // --- Parser genérico, usado tanto por loadFromAssets como por loadFromUri ---
	public List<ObjetosCargados.SubMesh> parseStream(InputStream is) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
        Map<String, Integer> nameCountMap = new HashMap<>();
		FloatArrayList tempVertices = new FloatArrayList(60000);
		FloatArrayList tempNormals = new FloatArrayList(60000);
		FloatArrayList tempTexcoords = new FloatArrayList(40000);
		FloatArrayList tempColors = new FloatArrayList(60000);
		FloatArrayList vList = new FloatArrayList(10000);
		FloatArrayList nList = new FloatArrayList(10000);
		FloatArrayList tList = new FloatArrayList(10000);
		FloatArrayList cList = new FloatArrayList(10000);
		IntArrayList iList = new IntArrayList(10000);
		Map<String, Integer> vertexMap = new HashMap<>();
		String currentObjectName = "default";
		int partCounter = 0;
		int localVertexCount = 0;
		String line;
		while ((line = br.readLine()) != null) {
			line = line.trim().replace(',', '.');
			if (line.isEmpty() || line.startsWith("#")) continue;
			if (line.startsWith("o ") || line.startsWith("g ")) {
				if (!iList.isEmpty() && !vList.isEmpty()) {
					flushSubMesh(vList, nList, tList, cList, iList, currentObjectName);
					vList.clear(); nList.clear(); tList.clear(); cList.clear(); iList.clear();
					vertexMap.clear();
					localVertexCount = 0;
					partCounter = 0;
				}
				currentObjectName = line.substring(2).trim();
				if (currentObjectName.isEmpty()) currentObjectName = "unnamed";
				currentObjectName = getUniqueName(currentObjectName, nameCountMap);
				continue;
			}
			if (line.startsWith("v ")) {
				String[] tok = line.split("\\s+");
				float vx = Float.parseFloat(tok[1]);
				float vy = Float.parseFloat(tok[2]);
				float vz = Float.parseFloat(tok[3]);
				tempVertices.add(vx);
				tempVertices.add(vy);
				tempVertices.add(vz);
				// detectar color si hay más valores
				if (tok.length >= 7) {
					tempColors.add(Float.parseFloat(tok[4]));
					tempColors.add(Float.parseFloat(tok[5]));
					tempColors.add(Float.parseFloat(tok[6]));
				} else {
					tempColors.add(0.3f);
					tempColors.add(0.3f);
					tempColors.add(0.3f);
				}
				continue;
			}
			if (line.startsWith("vn ")) {
				String[] tok = line.split("\\s+");
				tempNormals.add(Float.parseFloat(tok[1]));
				tempNormals.add(Float.parseFloat(tok[2]));
				tempNormals.add(Float.parseFloat(tok[3]));
				continue;
			}
			if (line.startsWith("vt ")) {
				String[] tok = line.split("\\s+");
				tempTexcoords.add(Float.parseFloat(tok[1]));
				tempTexcoords.add(Float.parseFloat(tok[2]));
				continue;
			}
			if (line.startsWith("f ")) {
				String[] faceTokens = line.substring(2).trim().split("\\s+");
				if (faceTokens.length < 3) continue;
				int[] faceIndices = new int[faceTokens.length];
				for (int i = 0; i < faceTokens.length; i++) {
					String key = faceTokens[i];
					Integer index = vertexMap.get(key);
					if (index == null) {
						String[] parts = key.split("/");
						int vIndex = Integer.parseInt(parts[0]) - 1;
						vList.add(tempVertices.get(vIndex * 3));
						vList.add(tempVertices.get(vIndex * 3 + 1));
						vList.add(tempVertices.get(vIndex * 3 + 2));
						// normales
						if (parts.length > 2 && !parts[2].isEmpty()) {
							int nIndex = Integer.parseInt(parts[2]) - 1;
							if (nIndex >= 0 && nIndex * 3 < tempNormals.size) {
								nList.add(tempNormals.get(nIndex * 3));
								nList.add(tempNormals.get(nIndex * 3 + 1));
								nList.add(tempNormals.get(nIndex * 3 + 2));
							}
						}
						// texcoords
						if (parts.length > 1 && !parts[1].isEmpty()) {
							int tIndex = Integer.parseInt(parts[1]) - 1;
							if (tIndex >= 0 && tIndex * 2 < tempTexcoords.size) {
								tList.add(tempTexcoords.get(tIndex * 2));
								tList.add(tempTexcoords.get(tIndex * 2 + 1));
							} else { tList.add(0f); tList.add(0f); }
						} else { tList.add(0f); tList.add(0f); }
						// color
						if (vIndex * 3 < tempColors.size) {
							cList.add(tempColors.get(vIndex * 3));
							cList.add(tempColors.get(vIndex * 3 + 1));
							cList.add(tempColors.get(vIndex * 3 + 2));
						} else { cList.add(0.3f); cList.add(0.3f); cList.add(0.3f); }
						index = localVertexCount; vertexMap.put(key, index); localVertexCount++;
					} faceIndices[i] = index;
				}
				// triangulación fan
				for (int i = 1; i < faceIndices.length - 1; i++) {
					iList.add(faceIndices[0]);
					iList.add(faceIndices[i]);
					iList.add(faceIndices[i + 1]);
				}
				if (localVertexCount >= MAX_VERTICES) {
					String partName = currentObjectName + "_part" + partCounter;
					flushSubMesh(vList, nList, tList, cList, iList, partName);
					partCounter++;
					vList.clear(); nList.clear(); tList.clear(); cList.clear(); iList.clear();
					vertexMap.clear();
					localVertexCount = 0;
				}
			}
		}
		if (!iList.isEmpty() && !vList.isEmpty()) {
			String partName = currentObjectName + (partCounter > 0 ? "_part" + partCounter : "");
			flushSubMesh(vList, nList, tList, cList, iList, partName);
		}
		br.close(); return subMeshes;
	}

	private String getUniqueName(String baseName, Map<String, Integer> map) {
		Integer count = map.get(baseName);
		if (count == null) { map.put(baseName, 0); return baseName;
		} else { count++; map.put(baseName, count); return baseName + "_" + count;
		}
	}

	private void generateNormals(FloatArrayList vList, IntArrayList iList, FloatArrayList nList) {
		int vertexCount = vList.size / 3;
		// Inicializar normales en 0
		for (int i = 0; i < vertexCount * 3; i++) {
			nList.add(0f);
		}
		// Recorrer triángulos
		for (int i = 0; i < iList.size; i += 3) {
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
			// edges
			float e1x = v1x - v0x;
			float e1y = v1y - v0y;
			float e1z = v1z - v0z;
			float e2x = v2x - v0x;
			float e2y = v2y - v0y;
			float e2z = v2z - v0z;
			// cross product
			float nx = e1y * e2z - e1z * e2y;
			float ny = e1z * e2x - e1x * e2z;
			float nz = e1x * e2y - e1y * e2x;
			// sumar a cada vértice
			addNormal(nList, i0, nx, ny, nz);
			addNormal(nList, i1, nx, ny, nz);
			addNormal(nList, i2, nx, ny, nz);
		}
		// Normalizar
		for (int i = 0; i < vertexCount; i++) {
			float nx = nList.get(i * 3);
			float ny = nList.get(i * 3 + 1);
			float nz = nList.get(i * 3 + 2);
			float length = (float)Math.sqrt(nx*nx + ny*ny + nz*nz);
			if (length > 0f) {
				nList.set(i * 3, nx / length);
				nList.set(i * 3 + 1, ny / length);
				nList.set(i * 3 + 2, nz / length);
			}
		}
	}

	private void addNormal(FloatArrayList nList, int index, float nx, float ny, float nz) {
		int base = index * 3;
		nList.set(base,     nList.get(base)     + nx);
		nList.set(base + 1, nList.get(base + 1) + ny);
		nList.set(base + 2, nList.get(base + 2) + nz);
	}

	private void generateUVs(FloatArrayList vList, IntArrayList iList, FloatArrayList nList, FloatArrayList tList) {
		tList.clear();
		int numVertices = vList.size / 3;
		if (numVertices == 0) return;
		// Reservar espacio inicial para las UVs
		for (int i = 0; i < numVertices * 2; i++) {
			tList.add(0f);
		}
		FloatArrayList tempNormals = nList;
		if (nList.isEmpty()) {
			tempNormals = new FloatArrayList(numVertices * 3);
			generateNormals(vList, iList, tempNormals);
		}
		// Calculamos el Bounding Box global
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
		// Mapeo por cara de triangulo para asegurar que cada dirección (X+, X-, Y+, Y-, Z+, Z-)
		// quede aislada en su cuadrante correspondiente de la textura 3x2.
		for (int i = 0; i < iList.size; i += 3) {
			int i0 = iList.get(i);
			int i1 = iList.get(i + 1);
			int i2 = iList.get(i + 2);
			// Promedio de normal del triángulo para determinar orientación
			float nx = (tempNormals.get(i0 * 3) + tempNormals.get(i1 * 3) + tempNormals.get(i2 * 3)) / 3.0f;
			float ny = (tempNormals.get(i0 * 3 + 1) + tempNormals.get(i1 * 3 + 1) + tempNormals.get(i2 * 3 + 1)) / 3.0f;
			float nz = (tempNormals.get(i0 * 3 + 2) + tempNormals.get(i1 * 3 + 2) + tempNormals.get(i2 * 3 + 2)) / 3.0f;
			float absX = Math.abs(nx);
			float absY = Math.abs(ny);
			float absZ = Math.abs(nz);
			int col = 0, row = 0;
			int[] indices = {i0, i1, i2};
			for (int idx : indices) {
				float x = vList.get(idx * 3);
				float y = vList.get(idx * 3 + 1);
				float z = vList.get(idx * 3 + 2);
				float localU = 0f, localV = 0f;
				if (absX >= absY && absX >= absZ) {
					if (nx > 0) { // Derecha (+X)
						localU = (maxZ - z) / sizeZ;
						localV = (y - minY) / sizeY;
						col = 2; row = 0;
					} else { // Izquierda (-X)
						localU = (z - minZ) / sizeZ;
						localV = (y - minY) / sizeY;
						col = 0; row = 1;
					}
				} else if (absY >= absX && absY >= absZ) {
					if (ny > 0) { // Arriba (+Y)
						localU = (x - minX) / sizeX;
						localV = (maxZ - z) / sizeZ;
						col = 1; row = 1;
					} else { // Abajo (-Y)
						localU = (x - minX) / sizeX;
						localV = (z - minZ) / sizeZ;
						col = 2; row = 1;
					}
				} else {
					if (nz >= 0) { // Frente (+Z)
						localU = (x - minX) / sizeX;
						localV = (y - minY) / sizeY;
						col = 0; row = 0;
					} else { // Espalda (-Z)
						localU = (maxX - x) / sizeX;
						localV = (y - minY) / sizeY;
						col = 1; row = 0;
					}
				}
				float u = (col + localU) / 3.0f;
				float v = (row + localV) / 2.0f;
				tList.set(idx * 2, u);
				tList.set(idx * 2 + 1, v);
			}
		}
	}

	public void flushSubMesh(FloatArrayList vList, FloatArrayList nList, FloatArrayList tList, FloatArrayList cList, IntArrayList iList, String objectName) {
		if (vList.isEmpty() || iList.isEmpty()) return;
		ObjetosCargados.SubMesh sm = new ObjetosCargados.SubMesh();
		sm.name = objectName;
		ByteBuffer vb = ByteBuffer.allocateDirect(vList.size * 4);
		vb.order(ByteOrder.nativeOrder());
		FloatBuffer vbuf = vb.asFloatBuffer();
		for (int i = 0; i < vList.size; i++) vbuf.put(vList.get(i));
		vbuf.position(0);
		sm.vertexBuffer = vbuf;
		if (nList.isEmpty()) { generateNormals(vList, iList, nList); }
		// 1. Verificar si el archivo traía coordenadas UV originales (vt)
		boolean tieneUVsOriginales = false;
		for (int i = 0; i < tList.size; i++) {
			if (tList.get(i) != 0f) { tieneUVsOriginales = true; break; }
		} sm.hasOriginalUVs = tieneUVsOriginales;
		// 2. Verificar si el archivo traía colores por vértice (v x y z r g b)
		boolean tieneColoresPorVertice = !cList.isEmpty();
		// 3. SOLO generar UVs proyectadas si NO hay UVs de archivo Y TAMPOCO hay colores por vértice
		if (!tieneUVsOriginales && !tieneColoresPorVertice) { generateUVs(vList, iList, nList, tList); }
		// Normal buffer
		ByteBuffer nb = ByteBuffer.allocateDirect(nList.size * 4);
		nb.order(ByteOrder.nativeOrder());
		FloatBuffer nbuf = nb.asFloatBuffer();
		for (int i = 0; i < nList.size; i++) nbuf.put(nList.get(i));
		nbuf.position(0);
		sm.normalBuffer = nbuf;
		// Texcoord buffer
		ByteBuffer tb = ByteBuffer.allocateDirect(tList.size * 4);
		tb.order(ByteOrder.nativeOrder());
		FloatBuffer tbuf = tb.asFloatBuffer();
		for (int i = 0; i < tList.size; i++) tbuf.put(tList.get(i));
		tbuf.position(0);
		sm.texcoordBuffer = tbuf;
		// Color buffer (RGB)
		int numVertices = vList.size / 3;
		ByteBuffer cb = ByteBuffer.allocateDirect(numVertices * 3 * 4);
		cb.order(ByteOrder.nativeOrder());
		FloatBuffer cbuf = cb.asFloatBuffer();
		for (int i = 0; i < numVertices; i++) {
			if (i * 3 + 2 < cList.size) {
				cbuf.put(cList.get(i * 3));     // R
				cbuf.put(cList.get(i * 3 + 1)); // G
				cbuf.put(cList.get(i * 3 + 2)); // B
			} else { cbuf.put(0.3f); cbuf.put(0.3f); cbuf.put(0.3f);
			}
		} cbuf.position(0); sm.colorBuffer = cbuf;
		// Index buffer
		ByteBuffer ib = ByteBuffer.allocateDirect(iList.size * 2);
		ib.order(ByteOrder.nativeOrder());
		ShortBuffer ibuf = ib.asShortBuffer();
		for (int i = 0; i < iList.size; i++) ibuf.put((short) iList.get(i));
		ibuf.position(0);
		sm.indexBuffer = ibuf;
		sm.numIndices = iList.size;
		subMeshes.add(sm);
	}


}
