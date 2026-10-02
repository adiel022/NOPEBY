package ar.axt.leerobj;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class TexturaLoader {

    private final Context context;

    public TexturaLoader(Context context) {
        this.context = context;
    }

    public Bitmap loadFromUri(Uri uri) throws IOException {
        InputStream is = context.getContentResolver().openInputStream(uri);
        if (is == null) { throw new IOException("No se pudo abrir URI"); }
        try {
            Bitmap bmp = BitmapFactory.decodeStream(is);
            if (bmp == null) { throw new IOException("No se pudo decodificar imagen"); }
            return bmp; } finally { is.close(); }
    }

    public Bitmap loadFromAssets(String fileName) throws IOException {
        InputStream is = context.getAssets().open(fileName);
        try {
            Bitmap bmp = BitmapFactory.decodeStream(is);
            if (bmp == null) { throw new IOException("No se pudo decodificar asset: " + fileName); }
            return bmp; } finally { is.close(); }
    }

    public Bitmap loadFromGlb(byte[] textureData, String mimeType) throws IOException {
        if (textureData == null) { throw new IOException("Texture data null"); }
        Bitmap bmp = BitmapFactory.decodeByteArray(textureData, 0, textureData.length);
        if (bmp == null) { throw new IOException("No se pudo decodificar textura GLB"); }
        return bmp;
    }

    public int loadTextureToGL(Bitmap bitmap) {
        return loadTextureToGL(bitmap, true);
    }

    public int loadTextureToGL(Bitmap bitmap, boolean flipY) {
        if (!isValidBitmap(bitmap)) {
            Log.e("TexturaLoader", "Bitmap inválido o reciclado");
            return -1; }
        Bitmap finalBmp = bitmap;
        if (flipY) {
            Matrix matrix = new Matrix();
            matrix.postScale(1, -1);
            finalBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
        int[] textureHandle = new int[1];
        GLES20.glGenTextures(1, textureHandle, 0);
        if (textureHandle[0] != 0) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, finalBmp, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            Log.d("TexturaLoader", "Textura generada con éxito. ID: " + textureHandle[0]);
            if (flipY && finalBmp != bitmap) { finalBmp.recycle(); }
            return textureHandle[0];
        }
        Log.e("TexturaLoader", "No se pudo generar el ID de textura en OpenGL");
        return -1;
    }

    public int processTextureUri(Uri uri, ar.axt.nopeby.MyRenderer.Mesh meshTarget) {
        String nombre = ar.axt.nopeby.FileUtils.getFileName(context, uri);
        if (nombre == null) return -1;
        nombre = nombre.toLowerCase();
        try {
            if (nombre.endsWith(".png") || nombre.endsWith(".jpg") || nombre.endsWith(".jpeg")) {
                Bitmap bmp = loadFromUri(uri);
                int glId = loadTextureToGL(bmp, true);
                if (glId != -1 && meshTarget != null && meshTarget.subMesh != null) {
                    meshTarget.subMesh.textureId = glId;
                    if (meshTarget.subMesh.texcoordBuffer == null || meshTarget.subMesh.texcoordBuffer.capacity() == 0) {
                        asegurarCoordenadasUV(meshTarget.subMesh);
                    }
                }
                if (bmp != null) bmp.recycle();
                return glId;
            } else if (nombre.endsWith(".mtl")) {
                List<String> texturasRequeridas = extractPngFromMtl(uri);
                Log.d("MTL_LOADER", "El archivo MTL requiere las siguientes texturas: " + texturasRequeridas.toString());
                return -2;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } return -1;
    }

    public List<String> extractPngFromMtl(Uri uri) {
        List<String> listaPng = new ArrayList<>();
        try {
            InputStream is = context.getContentResolver().openInputStream(uri);
            if (is == null) return listaPng;
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("map_Kd")) {
                    String[] tokens = line.split("\\s+");
                    if (tokens.length > 1) {
                        String file = tokens[tokens.length - 1];
                        listaPng.add(file);
                    }
                }
            }
            br.close(); is.close();
        } catch (Exception e) { e.printStackTrace(); } return listaPng;
    }

    public static void asegurarCoordenadasUV(ObjetosCargados.SubMesh sm) {
        if (sm == null || sm.vertexBuffer == null) return;
        // Si ya existen coordenadas UV en el subMesh, NO LAS SOBREESCRIBIMOS JAMÁS
        if (sm.texcoordBuffer != null && sm.texcoordBuffer.capacity() >= (sm.vertexBuffer.capacity() / 3) * 2) {
            return;
        }
        java.nio.FloatBuffer vb = sm.vertexBuffer.duplicate();
        vb.position(0);
        int totalCoords = vb.remaining();
        int vertexCount = totalCoords / 3;
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int i = 0; i < vertexCount; i++) {
            float x = vb.get();
            float y = vb.get();
            vb.get(); // saltar z
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }
        float rangeX = maxX - minX > 0.0001f ? maxX - minX : 1.0f;
        float rangeY = maxY - minY > 0.0001f ? maxY - minY : 1.0f;
        java.nio.ByteBuffer tb = java.nio.ByteBuffer.allocateDirect(vertexCount * 2 * 4).order(java.nio.ByteOrder.nativeOrder());
        java.nio.FloatBuffer tbuf = tb.asFloatBuffer();
        vb.position(0);
        for (int i = 0; i < vertexCount; i++) {
            float x = vb.get();
            float y = vb.get();
            vb.get();
            float u = (x - minX) / rangeX;
            float v = (y - minY) / rangeY;
            tbuf.put(u);
            tbuf.put(v);
        }
        tbuf.position(0); sm.texcoordBuffer = tbuf;
    }

    public boolean isValidBitmap(Bitmap bmp) {
        return bmp != null && !bmp.isRecycled();
    }

}