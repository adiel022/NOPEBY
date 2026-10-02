package ar.axt.leerobj;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;
import ar.axt.database.AdministrarDatos;
import ar.axt.nopeby.MainActivity;
import ar.axt.nopeby.MyRenderer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CrearTextura {
    private static final String TAG = "CrearTextura";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface TexturaProgressListener {
        void onProgress(String fase, int porcentaje);
        void onFinished(String rutaArchivo);
        void onError(String error);
    }

    public static void generarTexturaParaSeleccionado(final MainActivity activity, final int resolucion, final TexturaProgressListener listener) {
        if (activity == null || activity.renderer == null || activity.nombreProyecto == null) {
            if (listener != null) listener.onError("Error: Proyecto no activo");
            return;
        }
        MyRenderer.Mesh selectedMesh = activity.renderer.rayosInteraccion != null ?
                activity.renderer.rayosInteraccion.selectedMesh : null;
        if (selectedMesh == null) {
            if (listener != null) listener.onError("Debes tener un modelo seleccionado");
            return;
        }
        final String baseName = AdministrarDatos.extraerNombreBase(selectedMesh.name);
        final List<MyRenderer.Mesh> partes = new ArrayList<>();
        if (activity.renderer.meshes != null) {
            for (MyRenderer.Mesh m : activity.renderer.meshes) {
                if (m != null && m.name != null && AdministrarDatos.extraerNombreBase(m.name).equals(baseName)) {
                    partes.add(m);
                }
            }
        }
        if (partes.isEmpty()) {
            if (listener != null) listener.onError("No se encontraron partes para el modelo");
            return;
        }
        if (listener != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listener.onProgress("Iniciando creación de textura...", 0);
                }
            });
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    List<TriangleData> triangulos = recolectarTriangulos(partes);
                    int totalTriangulos = triangulos.size();
                    if (totalTriangulos == 0) {
                        if (listener != null) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() { listener.onError("El modelo no tiene geometría válida"); }});}return;}
                    int gridCount = (int) Math.ceil(Math.sqrt(totalTriangulos));
                    float cellSize = 1.0f / gridCount;
                    Bitmap bitmap = Bitmap.createBitmap(resolucion, resolucion, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    canvas.drawColor(Color.TRANSPARENT);
                    Paint paint = new Paint();
                    paint.setAntiAlias(true);
                    paint.setStyle(Paint.Style.FILL_AND_STROKE);
                    paint.setStrokeWidth(2.5f);
                    paint.setStrokeJoin(Paint.Join.ROUND);
                    paint.setStrokeCap(Paint.Cap.ROUND);
                    int idx = 0;
                    int ultimoPorcentajeNotificado = -1;
                    for (TriangleData tri : triangulos) {
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
                        final int pct = (int) ((idx / (float) totalTriangulos) * 90.0f);
                        if (pct != ultimoPorcentajeNotificado && pct % 10 == 0) {
                            ultimoPorcentajeNotificado = pct;
                            if (listener != null) {
                                final int finalPct = pct;
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        listener.onProgress("Horneando textura (" + finalPct + "%)...", finalPct);
                                    }
                                });
                            }
                        }
                    }
                    if (listener != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                listener.onProgress("Guardando archivo PNG...", 95);
                            }
                        });
                    }
                    File meshFolder = AdministrarDatos.getMeshFolder(activity.nombreProyecto, baseName);
                    final File outputFile = obtenerArchivoUnico(meshFolder, baseName);
                    try (FileOutputStream out = new FileOutputStream(outputFile)) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        out.flush();
                        Log.d(TAG, "Textura PNG generada en: " + outputFile.getAbsolutePath());
                        if (listener != null) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onFinished(outputFile.getAbsolutePath());
                                }
                            });
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error guardando textura PNG", e);
                        if (listener != null) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onError("Error al guardar imagen PNG");
                                }
                            });
                        }
                    } finally { bitmap.recycle(); }
                } catch (Exception e) {
                    Log.e(TAG, "Error durante la creación de la textura", e);
                    if (listener != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                listener.onError("Error en creación de textura: " + e.getMessage());
                            }
                        });
                    }
                }
            }
        });
    }

    private static class TriangleData {
        int colorA = Color.WHITE;
        int colorB = Color.WHITE;
        int colorC = Color.WHITE;
        float[] uvA = new float[2];
        float[] uvB = new float[2];
        float[] uvC = new float[2];
    }

    private static List<TriangleData> recolectarTriangulos(List<MyRenderer.Mesh> partes) {
        List<TriangleData> triangulos = new ArrayList<>();
        for (MyRenderer.Mesh m : partes) {
            if (m == null || m.subMesh == null || m.subMesh.vertexBuffer == null) continue;
            FloatBuffer vb = m.subMesh.vertexBuffer.duplicate();
            FloatBuffer cb = m.subMesh.colorBuffer != null ? m.subMesh.colorBuffer.duplicate() : null;
            ShortBuffer ib = m.subMesh.indexBuffer != null ? m.subMesh.indexBuffer.duplicate() : null;
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
                tri.colorA = extraerColorVertice(cb, idxA);
                tri.colorB = extraerColorVertice(cb, idxB);
                tri.colorC = extraerColorVertice(cb, idxC);
                triangulos.add(tri);
            }
        } return triangulos;
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

    private static File obtenerArchivoUnico(File folder, String baseName) {
        File file = new File(folder, baseName + "_textura.png");
        if (!file.exists()) { return file; }
        int contador = 1;
        while (true) {
            file = new File(folder, baseName + "_textura_" + contador + ".png");
            if (!file.exists()) { return file; } contador++;
        }
    }

}