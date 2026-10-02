package ar.axt.leerobj;

import android.content.Context;
import android.net.Uri;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import ar.axt.nopeby.FileUtils;

public class ObjetosCargados {

    private final Context context;

    public ObjetosCargados(Context context) {
        this.context = context;
    }

    public static class SubMesh {
        public String name;
        public java.nio.FloatBuffer vertexBuffer;
        public java.nio.FloatBuffer normalBuffer;
        public java.nio.FloatBuffer texcoordBuffer;
        public java.nio.FloatBuffer colorBuffer;
        public java.nio.ShortBuffer indexBuffer;
        public int numIndices;
        public float[] translation = new float[]{0f, 0f, 0f};
		public byte[] embeddedTexture;
		public String embeddedTextureMimeType;
		public int textureId = -1;
		public android.graphics.Bitmap pendingTexture;
		public int normalMapTextureId = -1;
		public android.graphics.Bitmap pendingNormalMap;
		public byte[] embeddedNormalTexture;
		public String embeddedNormalMimeType;
		public boolean hasOriginalUVs = false;
    }

    public List<SubMesh> loadFromUri(Uri uri) throws IOException {
        String name = FileUtils.getFileName(context, uri);
        if (name == null) {
            throw new IOException("No se pudo obtener el nombre del archivo");
        }name = name.toLowerCase();
        InputStream is = context.getContentResolver().openInputStream(uri);
        if (is == null) {throw new IOException("No se pudo abrir el archivo");} List<SubMesh> resultado;
        try {
            if (name.endsWith(".obj")) {
                resultado = new ObjLoader().parseStream(is);}
            else if (name.endsWith(".stl")) {
                resultado = new StlLoader().load(is);}
            else if (name.endsWith(".glb")) {
                resultado = new GlbLoader().parseStream(is);}
            else {
                throw new IOException("Formato no soportado: " + name);
            }
        } finally {is.close();
        }
        normalizarEscalaGlobal(resultado);
        asegurarCoordenadasUVGlobal(resultado);
        return resultado;
    }

    public List<SubMesh> loadFromAssets(String filename) throws IOException {
        String name = filename.toLowerCase();
        InputStream is = context.getAssets().open(filename);
        List<SubMesh> resultado;
        try {
			if (name.endsWith(".obj")) {
                resultado = new ObjLoader().parseStream(is);}
            else if (name.endsWith(".stl")) {
                resultado = new StlLoader().load(is);}
            else if (name.endsWith(".glb")) {
                resultado = new GlbLoader().parseStream(is);}
            else {
                throw new IOException("Formato no soportado: " + filename);
            }
        } finally {is.close();
        }
        normalizarEscalaGlobal(resultado);
        asegurarCoordenadasUVGlobal(resultado);
        return resultado;
    }

    private void asegurarCoordenadasUVGlobal(List<SubMesh> subMeshes) {
        if (subMeshes == null) return;
        for (SubMesh sm : subMeshes) {
            if (!sm.hasOriginalUVs && (sm.texcoordBuffer == null || sm.texcoordBuffer.capacity() == 0)) {
                TexturaLoader.asegurarCoordenadasUV(sm);
            }
        }
    }

    private void normalizarEscalaGlobal(List<SubMesh> subMeshes) {
        if (subMeshes == null || subMeshes.isEmpty()) return;
        float maxAbsVal = 0f;
        for (SubMesh sm : subMeshes) {
            if (sm.vertexBuffer == null) continue;
            java.nio.FloatBuffer buffer = sm.vertexBuffer.duplicate();
            buffer.position(0);
            int remaining = buffer.remaining();
            for (int i = 0; i < remaining; i++) {
                float val = Math.abs(buffer.get());
                if (val > maxAbsVal) {
                    maxAbsVal = val;
                }
            }
        }
        float limiteMaximo = 2.0f;
        if (maxAbsVal > limiteMaximo) {
            float factorEscala = limiteMaximo / maxAbsVal;
            for (SubMesh sm : subMeshes) {
                if (sm.vertexBuffer == null) continue;
                java.nio.FloatBuffer buffer = sm.vertexBuffer;
                int remaining = buffer.remaining();
                for (int i = 0; i < remaining; i++) {
                    buffer.put(i, buffer.get(i) * factorEscala);
                }
            }
        }
    }

    private final List<SubMesh> meshes = new ArrayList<>();

    public void addAll(List<SubMesh> list) {
        meshes.addAll(list);
    }

    public List<SubMesh> getMeshes() {
        return meshes;
    }

    public void clear() {
        meshes.clear();
    }
}
