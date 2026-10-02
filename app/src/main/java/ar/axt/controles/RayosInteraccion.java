package ar.axt.controles;

import android.util.Log;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import ar.axt.animar.Bones;
import ar.axt.animar.HistorialMovimientos;
import ar.axt.animar.InteraccionGismo;
import ar.axt.ficicas.Hitbox;
import ar.axt.leerobj.ObjetosCargados;
import ar.axt.materiales.Material;
import ar.axt.nopeby.MainActivity;
import ar.axt.nopeby.MyRenderer;

public class RayosInteraccion {

    private MainActivity activity;
    private MyRenderer renderer;
    private InteraccionGismo interaccionesUiGizmo;
    private HistorialMovimientos historialRender;

    public RayosInteraccion(MainActivity activity, MyRenderer renderer, InteraccionGismo interaccionesUiGizmo, HistorialMovimientos historialRender) {
        this.activity = activity;
        this.renderer = renderer;
        this.interaccionesUiGizmo = interaccionesUiGizmo;
        this.historialRender = historialRender;
    }

    public RayosInteraccion(){}

    public MyRenderer.Mesh selectedMesh = null;
    public MyRenderer.Mesh lastSelectedMesh = null;

    public void selectMesh(MyRenderer.Mesh mesh) {
        if(mesh == null) return;
        if(mesh == lastSelectedMesh) {
            selectedMesh = null;
            lastSelectedMesh = null;
            interaccionesUiGizmo.arrastreIniciado = false;
            return;
        }
        selectedMesh = mesh;
        lastSelectedMesh = mesh;
        interaccionesUiGizmo.arrastreIniciado = false;
        historialRender.estadoInicialGrupo.clear();
        List<MyRenderer.Mesh> relacionados = getMeshesRelacionados(mesh);
        float cx = 0; float cy = 0; float cz = 0;
        if(relacionados.isEmpty()) {
            cx = mesh.hitbox.center[0];
            cy = mesh.hitbox.center[1];
            cz = mesh.hitbox.center[2];
        } else {
            for(MyRenderer.Mesh m : relacionados) {
                cx += m.hitbox.center[0];
                cy += m.hitbox.center[1];
                cz += m.hitbox.center[2];
            }
            cx /= relacionados.size();
            cy /= relacionados.size();
            cz /= relacionados.size();
        }
        renderer.gizmo.setPosition(cx, cy, cz);
        interaccionesUiGizmo.gizmoPosition[0] = cx;
        interaccionesUiGizmo.gizmoPosition[1] = cy;
        interaccionesUiGizmo.gizmoPosition[2] = cz;
        if(renderer.gizmo != null) {
            float hx = mesh.hitbox.halfSizeWorld[0];
            float hy = mesh.hitbox.halfSizeWorld[1];
            float hz = mesh.hitbox.halfSizeWorld[2];
            float maxSize = Math.max(hx, Math.max(hy, hz)) * 2.0f;
            renderer.gizmo.setScale(maxSize * 1.2f);
        }
        historialRender.estadoInicialGrupo.clear();
        if (interaccionesUiGizmo != null) {
            interaccionesUiGizmo.actualizarCentroGizmo();
        } else if (renderer != null && renderer.activity != null) {
            renderer.activity.requestRender();
        }
    }

    public List<MyRenderer.Mesh> getMeshesRelacionados(MyRenderer.Mesh mesh) {
        List<MyRenderer.Mesh> relacionados = new ArrayList<MyRenderer.Mesh>();
        if(mesh == null || mesh.name == null) return relacionados;
        String nombreBase = extraerNombreBase(mesh.name);
        for(MyRenderer.Mesh m : renderer.meshes) {
            if(m.name == null) continue;
            if(m.name.equals(nombreBase) || m.name.matches(nombreBase+"_part\\d+$")) {
                relacionados.add(m);
                Log.d("GIZMO", "Encontrado: " + m.name);
            }
        } return relacionados;
    }

    public String extraerNombreBase(String nombre) {
        if(nombre == null) return null;
        return nombre.replaceAll("_part\\d+$", "");
    }

    public MyRenderer.Mesh obtenerMeshPorNombre(String nombre) {
        if(nombre == null) return null;
        for(MyRenderer.Mesh m : renderer.meshes) {
            if(m.name == null) continue;
            if(m.name.equals(nombre)) return m;
        }
        for(MyRenderer.Mesh m : renderer.meshes) {
            if(m.name == null) continue;
            if(extraerNombreBase(m.name).equals(nombre)) return m;
        } return null;
    }

    public Bones.Bone seleccionarBonePorRay(Tacto.Ray ray) {
        if (renderer != null) Hitbox.actualizarTodo(renderer.meshes, renderer.gizmo, renderer.bone);
        if(renderer.bone.getSelectedBone() != null && renderer.gizmo != null && renderer.gizmo.hitboxes != null) {
            float[] tHitGizmo = new float[1];
            for(Hitbox hb : renderer.gizmo.hitboxes) {
                if(hb.intersectsRay(ray, tHitGizmo)) {
                    return renderer.bone.getSelectedBone();
                }
            }
        }
        Bones.Bone boneHit = null;
        float minDist = Float.MAX_VALUE;
        float[] tHit = new float[1];
        for(Bones.Bone b : renderer.bone.getAllBones()) {
            if(b.hitbox != null && b.hitbox.intersectsRay(ray, tHit)) {
                float dist = tHit[0];
                if(dist < minDist) {minDist = dist; boneHit = b;
                }
            }
        }
        if(boneHit != null && boneHit == renderer.bone.getSelectedBone()) {
            renderer.bone.clearSelectedBone();
            return null;
        }
        if(boneHit != null) {renderer.bone.selectBone(boneHit.id);
            if(interaccionesUiGizmo != null) {
                interaccionesUiGizmo.actualizarCentroGizmo(); }
            if(selectedMesh == null) {
                List<MyRenderer.Mesh> relacionados =  renderer.bone.getMeshesFromGroup(boneHit.group);
                if(relacionados != null && !relacionados.isEmpty()) {
                    MyRenderer.Mesh m = relacionados.get(0);
                    selectMesh(m);
                }
            }
        } return boneHit;
    }

    public void deseleccionarTodo() {
        selectedMesh = null;
        lastSelectedMesh = null;
        if (interaccionesUiGizmo != null) {
            interaccionesUiGizmo.arrastreIniciado = false; }
        if (renderer.bone != null) { renderer.bone.clearSelectedBone(); }
        if (renderer.material != null) { renderer.material.selectedBox = null; }
        if (renderer.administradorCamaras != null) { renderer.administradorCamaras.selectedCamara = null;}
    }

    public void deseleccionarTodoMenosCamara() {
        selectedMesh = null;
        lastSelectedMesh = null;
        if (interaccionesUiGizmo != null) { interaccionesUiGizmo.arrastreIniciado = false;
        }
        if (renderer.bone != null) { renderer.bone.clearSelectedBone();
        }
        if (renderer.material != null) { renderer.material.selectedBox = null; }
    }

    public void seleccionarPorRay(Tacto.Ray ray) {
        if (renderer != null) Hitbox.actualizarTodo(renderer.meshes, renderer.gizmo, renderer.bone);
        ObjetosCargados.SubMesh gizmoHit = checkGizmoHit(ray);
        if(gizmoHit != null) { aplicarAccionGizmo(gizmoHit); return; }
        Bones.Bone boneHit = seleccionarBonePorRay(ray);
        if(boneHit != null) { return; }
        if (renderer.administradorCamaras != null) {
            float[] tCam = new float[1];
            AdministradorCamaras.MarcadorCamara bestCam = null;
            float minCamDist = Float.MAX_VALUE;
            for (AdministradorCamaras.MarcadorCamara m : renderer.administradorCamaras.getMarcadores()) {
                if (m.hitboxes != null) {
                    for (Hitbox hb : m.hitboxes) {
                        if (hb.intersectsRay(ray, tCam)) {
                            if (tCam[0] < minCamDist) {
                                minCamDist = tCam[0];
                                bestCam = m;
                            }
                        }
                    }
                }
            }
            if (bestCam != null) {
                renderer.administradorCamaras.selectedCamara = bestCam;
                deseleccionarTodoMenosCamara();
                if (interaccionesUiGizmo != null) {interaccionesUiGizmo.actualizarCentroGizmo();
                } return;
            }
        }
        MyRenderer.Mesh meshRay = null;
        float meshRayDist = Float.MAX_VALUE;
        float[] tHit = new float[1];
        for(MyRenderer.Mesh m : renderer.meshes) {
            if(m.hitbox.intersectsRay(ray, tHit)) {
                float dist = tHit[0];
                if(dist < meshRayDist) {
                    meshRayDist = dist;
                    meshRay = m;
                }
            }
        }
        if(meshRay != null) {
            selectMesh(meshRay);
            meshRay.hitbox.updateFromMesh(meshRay);
            return;
        }
    }

    public ObjetosCargados.SubMesh checkGizmoHit(Tacto.Ray ray) {
        if (renderer.gizmo == null || renderer.gizmo.hitboxes == null) return null;
        boolean haySeleccion = selectedMesh != null ||
                renderer.bone.getSelectedBone() != null ||
                renderer.material.getSelectedBox() != null;
        if (!haySeleccion) return null;
        float[] tHit = new float[1];
        float gizmoDist = Float.MAX_VALUE;
        ObjetosCargados.SubMesh gizmoHit = null;
        for (int i = 0; i < renderer.gizmo.hitboxes.length; i++) {
            Hitbox hb = renderer.gizmo.hitboxes[i];
            if (hb.intersectsRay(ray, tHit)) {
                float dist = tHit[0];
                if (dist < gizmoDist) {
                    gizmoDist = dist;
                    gizmoHit = renderer.gizmo.meshes.get(i);
                }
            }
        } return gizmoHit;
    }

    public void aplicarAccionGizmo(ObjetosCargados.SubMesh gizmoHit) {
        if(gizmoHit == null) return;
        String nombre = gizmoHit.name.toLowerCase();
        switch(nombre) {
            case "escalar":
                interaccionesUiGizmo.RGE = 3;
                interaccionesUiGizmo.xyz = 0;
                break;
            case "escalarx":
                interaccionesUiGizmo.RGE = 3;
                interaccionesUiGizmo.xyz = 2;
                break;
            case "escalary":
                interaccionesUiGizmo.RGE = 3;
                interaccionesUiGizmo.xyz = 1;
                break;
            case "escalarz":
                interaccionesUiGizmo.RGE = 3;
                interaccionesUiGizmo.xyz = 3;
                break;
            case "movery":
                interaccionesUiGizmo.RGE = 2;
                interaccionesUiGizmo.xyz = 1;
                break;
            case "moverx":
                interaccionesUiGizmo.RGE = 2;
                interaccionesUiGizmo.xyz = 2;
                break;
            case "moverz":
                interaccionesUiGizmo.RGE = 2;
                interaccionesUiGizmo.xyz = 3;
                break;
            case "rotarx":
                interaccionesUiGizmo.RGE = 1;
                interaccionesUiGizmo.xyz = 1;
                break;
            case "rotary":
                interaccionesUiGizmo.RGE = 1;
                interaccionesUiGizmo.xyz = 3;
                break;
            case "rotarz":
                interaccionesUiGizmo.RGE = 1;
                interaccionesUiGizmo.xyz = 2;                                                                                                                                                                                                            interaccionesUiGizmo.xyz = 2;
                break;
        }
    }

}
