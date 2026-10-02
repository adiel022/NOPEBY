package ar.axt.leerobj;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.opengl.Matrix;
import android.util.Log;
import ar.axt.database.AdministrarDatos;
import ar.axt.nopeby.MainActivity;
import ar.axt.nopeby.MyRenderer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExportarObj {
    private static final String TAG = "ExportarObj";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface ExportProgressListener {
        void onProgress(String fase, int porcentaje);
        void onFinished(String rutaObj, String rutaCarpeta);
        void onError(String error);
    }

    private static class TriangleData {
        float[] vA = new float[3];
        float[] vB = new float[3];
        float[] vC = new float[3];
        int colorA = Color.WHITE, colorB = Color.WHITE, colorC = Color.WHITE;
        float[] normA = new float[3];
        float[] normB = new float[3];
        float[] normC = new float[3];
        float[] uvA = new float[2];
        float[] uvB = new float[2];
        float[] uvC = new float[2];
    }

    public static void exportarEscenaCompleta(final MainActivity activity, final int resolucion, final ExportProgressListener listener) {
        if (activity == null || activity.renderer == null || activity.nombreProyecto == null) {
            if (listener != null) listener.onError("Error: Proyecto no activo");
            return;
        }

        if (activity.renderer.meshes == null || activity.renderer.meshes.isEmpty()) {
            if (listener != null) listener.onError("La escena no contiene modelos para exportar");
            return;
        }

        if (listener != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listener.onProgress("Iniciando exportación de escena...", 5);
                }
            });
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, List<MyRenderer.Mesh>> gruposBase = new HashMap<>();
                    for (MyRenderer.Mesh m : activity.renderer.meshes) {
                        if (m != null && m.name != null && m.subMesh != null && m.subMesh.vertexBuffer != null) {
                            String baseName = AdministrarDatos.extraerNombreBase(m.name);
                            List<MyRenderer.Mesh> lista = gruposBase.get(baseName);
                            if (lista == null) {
                                lista = new ArrayList<>();
                                gruposBase.put(baseName, lista);
                            } lista.add(m);
                        }
                    }
                    if (gruposBase.isEmpty()) {
                        if (listener != null) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() { listener.onError("No hay modelos válidos en la escena"); }
                            });
                        } return;
                    }
                    File exportFolder = AdministrarDatos.getMeshFolder(activity.nombreProyecto, activity.nombreProyecto);
                    if (!exportFolder.exists()) { exportFolder.mkdirs(); }
                    File mtlFile = new File(exportFolder, activity.nombreProyecto + "_pose.mtl");
                    File objFile = new File(exportFolder, activity.nombreProyecto + "_pose.obj");
                    Map<String, List<TriangleData>> triangulosPorGrupo = new HashMap<>();
                    Map<String, String> texturaFileNames = new HashMap<>();
                    int totalGrupos = gruposBase.size();
                    int grupoActual = 0;
                    for (Map.Entry<String, List<MyRenderer.Mesh>> entry : gruposBase.entrySet()) {
                        String baseName = entry.getKey();
                        List<MyRenderer.Mesh> partes = entry.getValue();
                        grupoActual++;
                        final String faseMsg = "Horneando textura para " + baseName + " (" + grupoActual + "/" + totalGrupos + ")...";
                        final int pctNotif = 10 + (int) (((float) grupoActual / totalGrupos) * 60.0f);
                        if (listener != null) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() { listener.onProgress(faseMsg, pctNotif); }
                            });
                        }
                        List<TriangleData> triangulosGrupo = recolectarTriangulos(partes);
                        triangulosPorGrupo.put(baseName, triangulosGrupo);
                        int totalTriangulos = triangulosGrupo.size();
                        int gridCount = (int) Math.ceil(Math.sqrt(Math.max(totalTriangulos, 1)));
                        float cellSize = 1.0f / gridCount;
                        Bitmap bitmap = Bitmap.createBitmap(resolucion, resolucion, Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(bitmap);
                        canvas.drawColor(Color.TRANSPARENT);
                        Paint paint = new Paint();
                        paint.setAntiAlias(true);
                        paint.setStyle(Paint.Style.FILL_AND_STROKE);
                        paint.setStrokeWidth(3.5f);
                        paint.setStrokeJoin(Paint.Join.ROUND);
                        paint.setStrokeCap(Paint.Cap.ROUND);
                        int idx = 0;
                        for (TriangleData tri : triangulosGrupo) {
                            int col = idx % gridCount;
                            int row = idx / gridCount;
                            float u0 = (float) col * cellSize;
                            float v0 = (float) row * cellSize;
                            tri.uvA[0] = u0 + cellSize * 0.2f;
                            tri.uvA[1] = v0 + cellSize * 0.2f;
                            tri.uvB[0] = u0 + cellSize * 0.8f;
                            tri.uvB[1] = v0 + cellSize * 0.2f;
                            tri.uvC[0] = u0 + cellSize * 0.5f;
                            tri.uvC[1] = v0 + cellSize * 0.8f;
                            float xA = tri.uvA[0] * (resolucion - 1);
                            float yA = (1.0f - tri.uvA[1]) * (resolucion - 1);
                            float xB = tri.uvB[0] * (resolucion - 1);
                            float yB = (1.0f - tri.uvB[1]) * (resolucion - 1);
                            float xC = tri.uvC[0] * (resolucion - 1);
                            float yC = (1.0f - tri.uvC[1]) * (resolucion - 1);
                            Path path = new Path();
                            path.moveTo(xA, yA);
                            path.lineTo(xB, yB);
                            path.lineTo(xC, yC);
                            path.close();
                            int avgR = (Color.red(tri.colorA) + Color.red(tri.colorB) + Color.red(tri.colorC)) / 3;
                            int avgG = (Color.green(tri.colorA) + Color.green(tri.colorB) + Color.green(tri.colorC)) / 3;
                            int avgB = (Color.blue(tri.colorA) + Color.blue(tri.colorB) + Color.blue(tri.colorC)) / 3;
                            int avgA = (Color.alpha(tri.colorA) + Color.alpha(tri.colorB) + Color.alpha(tri.colorC)) / 3;
                            paint.setColor(Color.argb(avgA, avgR, avgG, avgB));
                            canvas.drawPath(path, paint);
                            idx++;
                        }
                        aplicarSangradoDeColor(bitmap, 3);
                        String pngFileName = baseName + "_textura.png";
                        File pngFile = new File(exportFolder, pngFileName);
                        try (FileOutputStream out = new FileOutputStream(pngFile)) {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); out.flush(); }
                        final Bitmap bitmapCopia = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                        bitmap.recycle();
                        for (MyRenderer.Mesh mPart : partes) {
                            if (mPart != null && mPart.subMesh != null) {
                                mPart.subMesh.pendingTexture = bitmapCopia;
                            }
                        } texturaFileNames.put(baseName, pngFileName);
                    }
                    if (listener != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() { listener.onProgress("Escribiendo archivo de materiales MTL...", 75); }
                        });
                    }
                    try (PrintWriter mtlWriter = new PrintWriter(new FileWriter(mtlFile))) {
                        mtlWriter.println("# MTL generado por NOPEBY 3D Animation");
                        for (String baseName : gruposBase.keySet()) {
                            mtlWriter.println("newmtl " + baseName + "_mat");
                            mtlWriter.println("Ka 1.0 1.0 1.0");
                            mtlWriter.println("Kd 1.0 1.0 1.0");
                            mtlWriter.println("Ks 0.2 0.2 0.2");
                            String pngName = texturaFileNames.get(baseName);
                            if (pngName != null) {
                                mtlWriter.println("map_Kd " + pngName);
                            } mtlWriter.println();
                        }
                    }
                    if (listener != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() { listener.onProgress("Escribiendo modelo OBJ de la pose...", 85); }
                        });
                    }
                    try (PrintWriter objWriter = new PrintWriter(new FileWriter(objFile))) {
                        objWriter.println("# Escena 3D Pose exportada desde NOPEBY 3D Animation");
                        objWriter.println("mtllib " + mtlFile.getName());
                        int vertexIndexOffset = 1;
                        for (Map.Entry<String, List<MyRenderer.Mesh>> entry : gruposBase.entrySet()) {
                            String baseName = entry.getKey();
                            List<TriangleData> triangulosGrupo = triangulosPorGrupo.get(baseName);
                            if (triangulosGrupo == null || triangulosGrupo.isEmpty()) continue;
                            objWriter.println();
                            objWriter.println("o " + baseName);
                            objWriter.println("usemtl " + baseName + "_mat");
                            for (TriangleData tri : triangulosGrupo) {
                                objWriter.printf(java.util.Locale.US, "v %.6f %.6f %.6f\n", tri.vA[0], tri.vA[1], tri.vA[2]);
                                objWriter.printf(java.util.Locale.US, "v %.6f %.6f %.6f\n", tri.vB[0], tri.vB[1], tri.vB[2]);
                                objWriter.printf(java.util.Locale.US, "v %.6f %.6f %.6f\n", tri.vC[0], tri.vC[1], tri.vC[2]);
                                objWriter.printf(java.util.Locale.US, "vt %.6f %.6f\n", tri.uvA[0], tri.uvA[1]);
                                objWriter.printf(java.util.Locale.US, "vt %.6f %.6f\n", tri.uvB[0], tri.uvB[1]);
                                objWriter.printf(java.util.Locale.US, "vt %.6f %.6f\n", tri.uvC[0], tri.uvC[1]);
                                objWriter.printf(java.util.Locale.US, "vn %.6f %.6f %.6f\n", tri.normA[0], tri.normA[1], tri.normA[2]);
                                objWriter.printf(java.util.Locale.US, "vn %.6f %.6f %.6f\n", tri.normB[0], tri.normB[1], tri.normB[2]);
                                objWriter.printf(java.util.Locale.US, "vn %.6f %.6f %.6f\n", tri.normC[0], tri.normC[1], tri.normC[2]);
                            }
                            for (int i = 0; i < triangulosGrupo.size(); i++) {
                                int idx0 = vertexIndexOffset;
                                int idx1 = vertexIndexOffset + 1;
                                int idx2 = vertexIndexOffset + 2;
                                objWriter.printf(java.util.Locale.US, "f %d/%d/%d %d/%d/%d %d/%d/%d\n",
                                        idx0, idx0, idx0,  idx1, idx1, idx1,  idx2, idx2, idx2); vertexIndexOffset += 3;
                            }
                        }
                    }
                    final String finalObjPath = objFile.getAbsolutePath();
                    final String finalFolderPath = exportFolder.getAbsolutePath();
                    Log.d(TAG, "Escena completa exportada en: " + finalObjPath);
                    if (listener != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                listener.onProgress("¡Exportación Completada con Éxito!", 100);
                                listener.onFinished(finalObjPath, finalFolderPath);
                            }
                        });
                    }
                } catch (final Exception e) {
                    Log.e(TAG, "Error exportando escena completa", e);
                    if (listener != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                listener.onError("Error al exportar escena: " + e.getMessage());
                            }
                        });
                    }
                }
            }
        });
    }

    private static List<TriangleData> recolectarTriangulos(List<MyRenderer.Mesh> partes) {
        List<TriangleData> triangulos = new ArrayList<>();
        for (MyRenderer.Mesh m : partes) {
            if (m == null || m.subMesh == null || m.subMesh.vertexBuffer == null) continue;
            FloatBuffer vb = m.subMesh.vertexBuffer.duplicate();
            FloatBuffer cb = m.subMesh.colorBuffer != null ? m.subMesh.colorBuffer.duplicate() : null;
            ShortBuffer ib = m.subMesh.indexBuffer != null ? m.subMesh.indexBuffer.duplicate() : null;
            FloatBuffer nb = m.subMesh.normalBuffer != null ? m.subMesh.normalBuffer.duplicate() : null;
            float[] modelMatrix = new float[16];
            Matrix.setIdentityM(modelMatrix, 0);
            if (m.translation != null) {
                Matrix.translateM(modelMatrix, 0, m.translation[0], m.translation[1], m.translation[2]);
            }
            if (m.rotation != null) {
                Matrix.rotateM(modelMatrix, 0, m.rotation[0], 1.0f, 0.0f, 0.0f);
                Matrix.rotateM(modelMatrix, 0, m.rotation[1], 0.0f, 1.0f, 0.0f);
                Matrix.rotateM(modelMatrix, 0, m.rotation[2], 0.0f, 0.0f, 1.0f);
            }
            if (m.scale != null) {
                Matrix.scaleM(modelMatrix, 0, m.scale[0], m.scale[1], m.scale[2]);
            }
            float[] normalMatrix = new float[16];
            float[] invModel = new float[16];
            Matrix.invertM(invModel, 0, modelMatrix, 0);
            Matrix.transposeM(normalMatrix, 0, invModel, 0);
            int numTri = (ib != null && m.subMesh.numIndices > 0) ? m.subMesh.numIndices / 3 : (vb.capacity() / 9);
            for (int t = 0; t < numTri; t++) {
                int idxA, idxB, idxC;
                if (ib != null) {
                    ib.position(t * 3);
                    idxA = ib.get() & 0xFFFF;
                    idxB = ib.get() & 0xFFFF;
                    idxC = ib.get() & 0xFFFF;
                } else { idxA = t * 3; idxB = t * 3 + 1; idxC = t * 3 + 2; }
                TriangleData tri = new TriangleData();
                transformarVertice(vb, idxA, modelMatrix, tri.vA);
                transformarVertice(vb, idxB, modelMatrix, tri.vB);
                transformarVertice(vb, idxC, modelMatrix, tri.vC);
                tri.colorA = extraerColorVertice(cb, idxA);
                tri.colorB = extraerColorVertice(cb, idxB);
                tri.colorC = extraerColorVertice(cb, idxC);
                if (nb != null && nb.capacity() >= (Math.max(idxA, Math.max(idxB, idxC)) + 1) * 3) {
                    transformarNormal(nb, idxA, normalMatrix, tri.normA);
                    transformarNormal(nb, idxB, normalMatrix, tri.normB);
                    transformarNormal(nb, idxC, normalMatrix, tri.normC);
                } else {
                    float e1x = tri.vB[0] - tri.vA[0], e1y = tri.vB[1] - tri.vA[1], e1z = tri.vB[2] - tri.vA[2];
                    float e2x = tri.vC[0] - tri.vA[0], e2y = tri.vC[1] - tri.vA[1], e2z = tri.vC[2] - tri.vA[2];
                    float nx = e1y * e2z - e1z * e2y;
                    float ny = e1z * e2x - e1x * e2z;
                    float nz = e1x * e2y - e1y * e2x;
                    float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                    if (len > 0.0001f) { nx /= len; ny /= len; nz /= len; }
                    tri.normA[0] = tri.normB[0] = tri.normC[0] = nx;
                    tri.normA[1] = tri.normB[1] = tri.normC[1] = ny;
                    tri.normA[2] = tri.normB[2] = tri.normC[2] = nz;
                } triangulos.add(tri);
            }
        } return triangulos;
    }

    private static void transformarVertice(FloatBuffer vb, int idx, float[] matrix, float[] outV) {
        float[] inV = new float[]{vb.get(idx * 3), vb.get(idx * 3 + 1), vb.get(idx * 3 + 2), 1.0f};
        float[] res = new float[4];
        Matrix.multiplyMV(res, 0, matrix, 0, inV, 0);
        outV[0] = res[0]; outV[1] = res[1]; outV[2] = res[2];
    }

    private static void transformarNormal(FloatBuffer nb, int idx, float[] matrix, float[] outN) {
        float[] inN = new float[]{nb.get(idx * 3), nb.get(idx * 3 + 1), nb.get(idx * 3 + 2), 0.0f};
        float[] res = new float[4];
        Matrix.multiplyMV(res, 0, matrix, 0, inN, 0);
        float len = (float) Math.sqrt(res[0] * res[0] + res[1] * res[1] + res[2] * res[2]);
        if (len > 0.0001f) {
            outN[0] = res[0] / len; outN[1] = res[1] / len; outN[2] = res[2] / len;
        } else {
            outN[0] = res[0]; outN[1] = res[1]; outN[2] = res[2];
        }
    }

    private static int extraerColorVertice(FloatBuffer cb, int idx) {
        if (cb == null) return Color.WHITE;
        int cCap = cb.capacity();
        int cStride = (cCap % 4 == 0) ? 4 : 3;
        int pos = idx * cStride;
        if (pos >= 0 && pos + cStride <= cCap) {
            cb.position(pos);
            float r = cb.get(), g = cb.get(), b = cb.get();
            float a = (cStride == 4) ? cb.get() : 1.0f;
            r = (float) Math.pow(Math.max(0f, Math.min(1f, r)), 1.0 / 2.2);
            g = (float) Math.pow(Math.max(0f, Math.min(1f, g)), 1.0 / 2.2);
            b = (float) Math.pow(Math.max(0f, Math.min(1f, b)), 1.0 / 2.2);
            return Color.argb((int)(a * 255), (int)(r * 255), (int)(g * 255), (int)(b * 255));
        } return Color.WHITE;
    }

    private static void aplicarSangradoDeColor(Bitmap bitmap, int pasadas) {
        if (bitmap == null) return;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int p = 0; p < pasadas; p++) {
            int[] tempPixels = pixels.clone();
            boolean huboCambios = false;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = y * width + x;
                    if ((pixels[index] >>> 24) == 0) {
                        int sumR = 0, sumG = 0, sumB = 0, sumA = 0, count = 0;
                        for (int dy = -1; dy <= 1; dy++) {
                            int ny = y + dy;
                            if (ny < 0 || ny >= height) continue;
                            for (int dx = -1; dx <= 1; dx++) {
                                if (dx == 0 && dy == 0) continue;
                                int nx = x + dx;
                                if (nx < 0 || nx >= width) continue;
                                int neighbor = pixels[ny * width + nx];
                                int alpha = neighbor >>> 24;
                                if (alpha > 0) {
                                    sumR += (neighbor >> 16) & 0xFF;
                                    sumG += (neighbor >> 8) & 0xFF;
                                    sumB += neighbor & 0xFF;
                                    sumA += alpha;
                                    count++;
                                }
                            }
                        }
                        if (count > 0) {
                            tempPixels[index] = Color.argb(sumA / count, sumR / count, sumG / count, sumB / count);
                            huboCambios = true;
                        }
                    }
                }
            }
            pixels = tempPixels; if (!huboCambios) break; }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    }

}