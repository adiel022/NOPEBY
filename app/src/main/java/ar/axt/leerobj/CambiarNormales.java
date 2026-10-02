package ar.axt.leerobj;

import android.util.Log;
import android.widget.Toast;
import ar.axt.materiales.EditorReflejosMesh;
import ar.axt.nopeby.MainActivity;
import ar.axt.nopeby.MyRenderer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.List;

public class CambiarNormales {
    private static final String TAG = "CambiarNormales";

    // b1: Normales Difusas / Suaves (Smooth Normals)
    public static void aplicarNormalesDifusas(MainActivity activity) {
        if (activity == null || activity.renderer == null) return;

        List<MyRenderer.Mesh> objetivos = EditorReflejosMesh.obtenerMeshesSeleccionados(activity);
        if (objetivos == null || objetivos.isEmpty()) {
            Toast.makeText(activity, "Debes seleccionar un modelo primero", Toast.LENGTH_SHORT).show();
            return;
        }

        for (MyRenderer.Mesh m : objetivos) {
            if (m != null && m.subMesh != null && m.subMesh.vertexBuffer != null) {
                m.subMesh.normalBuffer = calcularNormalesSuaves(m.subMesh.vertexBuffer, m.subMesh.indexBuffer);
            }
        }

        if (activity.glSurfaceView != null) {
            activity.glSurfaceView.requestRender();
        }
        Toast.makeText(activity, "Normales Difusas (Suaves) Aplicadas", Toast.LENGTH_SHORT).show();
    }

    // b2: Normales Directas / Planas / Rígidas (Flat / Faceted Normals)
    public static void aplicarNormalesDirectas(MainActivity activity) {
        if (activity == null || activity.renderer == null) return;

        List<MyRenderer.Mesh> objetivos = EditorReflejosMesh.obtenerMeshesSeleccionados(activity);
        if (objetivos == null || objetivos.isEmpty()) {
            Toast.makeText(activity, "Debes seleccionar un modelo primero", Toast.LENGTH_SHORT).show();
            return;
        }

        for (MyRenderer.Mesh m : objetivos) {
            if (m != null && m.subMesh != null && m.subMesh.vertexBuffer != null) {
                m.subMesh.normalBuffer = calcularNormalesDirectasPlanas(m.subMesh.vertexBuffer, m.subMesh.indexBuffer);
            }
        }

        if (activity.glSurfaceView != null) {
            activity.glSurfaceView.requestRender();
        }
        Toast.makeText(activity, "Normales Directas (Planas) Aplicadas", Toast.LENGTH_SHORT).show();
    }

    private static FloatBuffer calcularNormalesDirectasPlanas(FloatBuffer vb, ShortBuffer ib) {
        FloatBuffer vbDup = vb.duplicate();
        ShortBuffer ibDup = ib != null ? ib.duplicate() : null;

        int numVerts = vbDup.capacity() / 3;
        float[] normals = new float[numVerts * 3];

        int numTri = (ibDup != null) ? ibDup.capacity() / 3 : numVerts / 3;

        for (int t = 0; t < numTri; t++) {
            int i0, i1, i2;
            if (ibDup != null) {
                i0 = ibDup.get(t * 3) & 0xFFFF;
                i1 = ibDup.get(t * 3 + 1) & 0xFFFF;
                i2 = ibDup.get(t * 3 + 2) & 0xFFFF;
            } else {
                i0 = t * 3;
                i1 = t * 3 + 1;
                i2 = t * 3 + 2;
            }

            float v0x = vbDup.get(i0 * 3), v0y = vbDup.get(i0 * 3 + 1), v0z = vbDup.get(i0 * 3 + 2);
            float v1x = vbDup.get(i1 * 3), v1y = vbDup.get(i1 * 3 + 1), v1z = vbDup.get(i1 * 3 + 2);
            float v2x = vbDup.get(i2 * 3), v2y = vbDup.get(i2 * 3 + 1), v2z = vbDup.get(i2 * 3 + 2);

            float e1x = v1x - v0x, e1y = v1y - v0y, e1z = v1z - v0z;
            float e2x = v2x - v0x, e2y = v2y - v0y, e2z = v2z - v0z;

            float nx = e1y * e2z - e1z * e2y;
            float ny = e1z * e2x - e1x * e2z;
            float nz = e1x * e2y - e1y * e2x;

            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len > 0.00001f) {
                nx /= len; ny /= len; nz /= len;
            } else {
                nx = 0f; ny = 0f; nz = 1f;
            }

            int[] indices = {i0, i1, i2};
            for (int idx : indices) {
                normals[idx * 3] = nx;
                normals[idx * 3 + 1] = ny;
                normals[idx * 3 + 2] = nz;
            }
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(normals.length * 4).order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(normals);
        fb.position(0);
        return fb;
    }

    private static FloatBuffer calcularNormalesSuaves(FloatBuffer vb, ShortBuffer ib) {
        FloatBuffer vbDup = vb.duplicate();
        ShortBuffer ibDup = ib != null ? ib.duplicate() : null;

        int numVerts = vbDup.capacity() / 3;
        float[] normals = new float[numVerts * 3];

        int numTri = (ibDup != null) ? ibDup.capacity() / 3 : numVerts / 3;

        for (int t = 0; t < numTri; t++) {
            int i0, i1, i2;
            if (ibDup != null) {
                i0 = ibDup.get(t * 3) & 0xFFFF;
                i1 = ibDup.get(t * 3 + 1) & 0xFFFF;
                i2 = ibDup.get(t * 3 + 2) & 0xFFFF;
            } else {
                i0 = t * 3;
                i1 = t * 3 + 1;
                i2 = t * 3 + 2;
            }

            float v0x = vbDup.get(i0 * 3), v0y = vbDup.get(i0 * 3 + 1), v0z = vbDup.get(i0 * 3 + 2);
            float v1x = vbDup.get(i1 * 3), v1y = vbDup.get(i1 * 3 + 1), v1z = vbDup.get(i1 * 3 + 2);
            float v2x = vbDup.get(i2 * 3), v2y = vbDup.get(i2 * 3 + 1), v2z = vbDup.get(i2 * 3 + 2);

            float e1x = v1x - v0x, e1y = v1y - v0y, e1z = v1z - v0z;
            float e2x = v2x - v0x, e2y = v2y - v0y, e2z = v2z - v0z;

            float nx = e1y * e2z - e1z * e2y;
            float ny = e1z * e2x - e1x * e2z;
            float nz = e1x * e2y - e1y * e2x;

            int[] indices = {i0, i1, i2};
            for (int idx : indices) {
                normals[idx * 3] += nx;
                normals[idx * 3 + 1] += ny;
                normals[idx * 3 + 2] += nz;
            }
        }

        for (int i = 0; i < numVerts; i++) {
            float nx = normals[i * 3];
            float ny = normals[i * 3 + 1];
            float nz = normals[i * 3 + 2];
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len > 0.00001f) {
                normals[i * 3] = nx / len;
                normals[i * 3 + 1] = ny / len;
                normals[i * 3 + 2] = nz / len;
            } else {
                normals[i * 3] = 0f;
                normals[i * 3 + 1] = 0f;
                normals[i * 3 + 2] = 1f;
            }
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(normals.length * 4).order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(normals);
        fb.position(0);
        return fb;
    }
}
