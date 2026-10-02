package ar.axt.sombras;

import ar.axt.materiales.EditorReflejosMesh;
import ar.axt.nopeby.MainActivity;
import ar.axt.nopeby.MyRenderer;
import java.util.List;

public class CancelarIluminacion {

    public static class EsferaLuz {
        public float[] centroWorld = new float[3];
        public float radioEfectivo = 0.0f;
        public float[] colorLuz = new float[]{1.0f, 0.95f, 0.7f};
    }

    public static EsferaLuz calcularEsferaLuz(MyRenderer.Mesh mesh) {
        EsferaLuz esfera = new EsferaLuz();
        if (mesh == null) return esfera;
        float posX = mesh.translation[0];
        float posY = mesh.translation[1];
        float posZ = mesh.translation[2];
        if (mesh.hitbox != null) {
            posX += mesh.hitbox.localCenter[0];
            posY += mesh.hitbox.localCenter[1];
            posZ += mesh.hitbox.localCenter[2];
            float baseRadius = Math.max(mesh.hitbox.halfSize[0], Math.max(mesh.hitbox.halfSize[1], mesh.hitbox.halfSize[2]));
            if (baseRadius < 0.5f) baseRadius = 0.5f;
            esfera.radioEfectivo = baseRadius * mesh.lightRadiusMult;
        } else {esfera.radioEfectivo = 2.0f * mesh.lightRadiusMult;
        }
        esfera.centroWorld[0] = posX; esfera.centroWorld[1] = posY; esfera.centroWorld[2] = posZ;
        if (mesh.emissiveColor != null) {
            System.arraycopy(mesh.emissiveColor, 0, esfera.colorLuz, 0, 3);
        } return esfera;
    }


    public static void actualizarAlcanceLuz(MainActivity act, float multiplicador) {
        List<MyRenderer.Mesh> objetivos = EditorReflejosMesh.obtenerMeshesSeleccionados(act);
        for (MyRenderer.Mesh m : objetivos) {
            if (m != null) { m.lightRadiusMult = multiplicador; }
        }
        if (act != null && act.glSurfaceView != null) {
            act.glSurfaceView.requestRender();
        }
    }

    public static void actualizarColorLuz(MainActivity act, float r, float g, float b) {
        List<MyRenderer.Mesh> objetivos = EditorReflejosMesh.obtenerMeshesSeleccionados(act);
        for (MyRenderer.Mesh m : objetivos) {
            if (m != null) { m.emissiveColor[0] = r; m.emissiveColor[1] = g; m.emissiveColor[2] = b;
            }
        }
        if (act != null && act.glSurfaceView != null) {
            act.glSurfaceView.requestRender();
        }
    }

}
