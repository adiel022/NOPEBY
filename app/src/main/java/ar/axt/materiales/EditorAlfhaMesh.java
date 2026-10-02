package ar.axt.materiales;

import ar.axt.nopeby.MainActivity;
import ar.axt.nopeby.MyRenderer;
import java.util.ArrayList;
import java.util.List;

public class EditorAlfhaMesh {

    public static List<MyRenderer.Mesh> obtenerMeshesSeleccionados(MainActivity act) {
        List<MyRenderer.Mesh> objetivos = new ArrayList<>();
        if (act == null || act.renderer == null) return objetivos;
        if (act.modoMultiseleccion == 1 && act.meshesMultiseleccionados != null && !act.meshesMultiseleccionados.isEmpty()) {
            for (String nameM : act.meshesMultiseleccionados) {
                for (MyRenderer.Mesh mesh : act.renderer.meshes) {
                    if (mesh != null && nameM.equals(mesh.name)) {
                        objetivos.add(mesh);
                    }
                }
            }
        } else if (act.renderer.rayosInteraccion != null && act.renderer.rayosInteraccion.selectedMesh != null) {
            List<MyRenderer.Mesh> relacionados = act.renderer.rayosInteraccion.getMeshesRelacionados(act.renderer.rayosInteraccion.selectedMesh);
            if (relacionados != null && !relacionados.isEmpty()) {
                objetivos.addAll(relacionados);
            } else {
                objetivos.add(act.renderer.rayosInteraccion.selectedMesh);
            }
        } return objetivos;
    }

    public static void actualizarTransparencia(MainActivity act, float alfa) {
        List<MyRenderer.Mesh> objetivos = obtenerMeshesSeleccionados(act);
        for (MyRenderer.Mesh m : objetivos) {
            if (m != null) {
                m.opacity = alfa;
            }
        }
        if (act != null && act.glSurfaceView != null) {
            act.glSurfaceView.requestRender();
        }
    }

}
