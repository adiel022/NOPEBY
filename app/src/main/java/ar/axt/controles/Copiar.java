package ar.axt.controles;

import android.util.Log;
import ar.axt.animar.Bones;
import ar.axt.materiales.Material;
import ar.axt.nopeby.MainActivity;
import ar.axt.nopeby.MyRenderer;
import ar.axt.leerobj.ObjetosCargados;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Copiar {

    private static final String TAG = "CopiarObjeto";
    private final MainActivity activity;
    private final MyRenderer renderer;

    public Copiar(MainActivity activity, MyRenderer renderer) {
        this.activity = activity;
        this.renderer = renderer;
    }

    public void ejecutarCopia() {
        if (renderer == null) return;
        // Caso 1: Multiselección de Meshes activada
        if (activity != null && activity.modoMultiseleccion == 1 && activity.meshesMultiseleccionados != null && !activity.meshesMultiseleccionados.isEmpty()) {
            Set<MyRenderer.Mesh> todosLosMeshesAProcesar = new HashSet<MyRenderer.Mesh>();
            if (renderer.meshes != null && renderer.rayosInteraccion != null) {
                for (MyRenderer.Mesh m : renderer.meshes) {
                    if (m != null && m.name != null && activity.meshesMultiseleccionados.contains(m.name)) {
                        List<MyRenderer.Mesh> relacionados = renderer.rayosInteraccion.getMeshesRelacionados(m);
                        if (relacionados != null && !relacionados.isEmpty()) {
                            todosLosMeshesAProcesar.addAll(relacionados);
                        } else { todosLosMeshesAProcesar.add(m); }
                    }
                }
            }
            if (!todosLosMeshesAProcesar.isEmpty()) {
                List<MyRenderer.Mesh> lasCopias = new ArrayList<MyRenderer.Mesh>();
                Set<Bones.Bone> huesosYaClonadosEnCiclo = new HashSet<Bones.Bone>();
                for (MyRenderer.Mesh m : todosLosMeshesAProcesar) {
                    MyRenderer.Mesh copia = copiarMeshIndividualConFiltroDeHuesos(m, huesosYaClonadosEnCiclo);
                    if (copia != null) {
                        lasCopias.add(copia);
                    }
                }
                activity.meshesMultiseleccionados.clear();
                for (MyRenderer.Mesh copia : lasCopias) {
                    if (copia.name != null) {
                        activity.meshesMultiseleccionados.add(copia.name);
                    }
                }
                if (!lasCopias.isEmpty()) {
                    MyRenderer.Mesh ultimaCopia = lasCopias.get(lasCopias.size() - 1);
                    renderer.rayosInteraccion.selectedMesh = ultimaCopia;
                    renderer.rayosInteraccion.lastSelectedMesh = ultimaCopia;
                    if (renderer.gizmo != null) {
                        renderer.gizmo.setPosition(ultimaCopia.translation[0], ultimaCopia.translation[1], ultimaCopia.translation[2]);
                        if (ultimaCopia.rotation != null && renderer.gizmo.rotation != null) {
                            System.arraycopy(ultimaCopia.rotation, 0, renderer.gizmo.rotation, 0, 3);
                        } renderer.gizmo.setScale(ultimaCopia.scale[0] * 4.0f);
                    }
                    if (renderer.interaccionGismo != null) {
                        renderer.interaccionGismo.actualizarCentroGizmo();
                    }
                } return;
            }
        }
        // Caso 2: Mesh individual
        if (renderer.rayosInteraccion != null && renderer.rayosInteraccion.selectedMesh != null) {
            MyRenderer.Mesh originalMesh = renderer.rayosInteraccion.selectedMesh;
            List<MyRenderer.Mesh> partesDelMesh = renderer.rayosInteraccion.getMeshesRelacionados(originalMesh);
            if (partesDelMesh == null || partesDelMesh.isEmpty()) {
                partesDelMesh = new ArrayList<MyRenderer.Mesh>();
                partesDelMesh.add(originalMesh);
            }
            List<MyRenderer.Mesh> lasCopiasIndividuales = new ArrayList<MyRenderer.Mesh>();
            Set<Bones.Bone> huesosYaClonadosEnCiclo = new HashSet<Bones.Bone>();
            for (MyRenderer.Mesh parte : partesDelMesh) {
                MyRenderer.Mesh copiaParte = copiarMeshIndividualConFiltroDeHuesos(parte, huesosYaClonadosEnCiclo);
                if (copiaParte != null) {
                    lasCopiasIndividuales.add(copiaParte);
                }
            }
            if (!lasCopiasIndividuales.isEmpty()) {
                MyRenderer.Mesh ultimaCopiaIndividual = lasCopiasIndividuales.get(lasCopiasIndividuales.size() - 1);
                renderer.rayosInteraccion.selectedMesh = ultimaCopiaIndividual;
                renderer.rayosInteraccion.lastSelectedMesh = ultimaCopiaIndividual;
                if (renderer.gizmo != null) {
                    renderer.gizmo.setPosition(ultimaCopiaIndividual.translation[0], ultimaCopiaIndividual.translation[1], ultimaCopiaIndividual.translation[2]);
                    if (ultimaCopiaIndividual.rotation != null && renderer.gizmo.rotation != null) {
                        System.arraycopy(ultimaCopiaIndividual.rotation, 0, renderer.gizmo.rotation, 0, 3);
                    }
                    renderer.gizmo.setScale(ultimaCopiaIndividual.scale[0] * 4.0f);
                }
                if (renderer.interaccionGismo != null) {
                    renderer.interaccionGismo.actualizarCentroGizmo();
                }
            } return;
        }
        if (activity != null && activity.renderizarHuesos() == 1 && renderer.getBones() != null && renderer.getBones().getSelectedBone() != null) {
            Bones.Bone originalBone = renderer.getBones().getSelectedBone();
            copiarBoneSubtree(originalBone); return; }
        if (renderer.material != null && renderer.material.getSelectedBox() != null) {
            Material.EffectBox originalBox = renderer.material.getSelectedBox();
            copiarCaja(originalBox); return; }
    }

    private MyRenderer.Mesh copiarMeshIndividualConFiltroDeHuesos(MyRenderer.Mesh original, Set<Bones.Bone> huesosYaClonadosEnCiclo) {
        if (original == null || original.subMesh == null) return null;
        ObjetosCargados.SubMesh subMeshCopia = new ObjetosCargados.SubMesh();
        ObjetosCargados.SubMesh origSub = original.subMesh;
        subMeshCopia.numIndices = origSub.numIndices;
        subMeshCopia.textureId = origSub.textureId;
        subMeshCopia.embeddedTexture = origSub.embeddedTexture;
        subMeshCopia.embeddedTextureMimeType = origSub.embeddedTextureMimeType;
        subMeshCopia.pendingTexture = origSub.pendingTexture;
        if (origSub.translation != null) {
            subMeshCopia.translation = origSub.translation.clone();
        }
        subMeshCopia.vertexBuffer = clonarFloatBuffer(origSub.vertexBuffer);
        subMeshCopia.normalBuffer = clonarFloatBuffer(origSub.normalBuffer);
        subMeshCopia.texcoordBuffer = clonarFloatBuffer(origSub.texcoordBuffer);
        subMeshCopia.colorBuffer = clonarFloatBuffer(origSub.colorBuffer);
        subMeshCopia.indexBuffer = clonarShortBuffer(origSub.indexBuffer);
        MyRenderer.Mesh meshCopia = new MyRenderer.Mesh(subMeshCopia);
        String nuevoNombre = generarNombreCopia(original.name != null ? original.name : "Mesh");
        meshCopia.name = nuevoNombre;
        subMeshCopia.name = nuevoNombre;
        if (original.translation != null) meshCopia.translation = original.translation.clone();
        if (original.rotation != null) meshCopia.rotation = original.rotation.clone();
        if (original.scale != null) meshCopia.scale = original.scale.clone();
        renderer.addMesh(meshCopia);
        if (activity != null) {
            if (meshCopia.name != null) {
                activity.listaMeshes.add(meshCopia.name);
                activity.listaUltimoMesh.add(meshCopia.name);
            }
        }
        if (renderer.getBones() != null && original.subMesh.name != null) {
            String grupoOriginal = original.subMesh.name;
            List<Bones.Bone> huesosDelGrupo = renderer.getBones().getAllBones();
            List<Bones.Bone> huesosAClonar = new ArrayList<Bones.Bone>();
            for (Bones.Bone b : huesosDelGrupo) {
                if (huesosYaClonadosEnCiclo.contains(b)) continue;
                if ((b.groupedVertices != null && b.groupedVertices.containsKey(original)) || (b.group != null && b.group.equals(grupoOriginal))) {
                    huesosAClonar.add(b);
                }
            }
            if (!huesosAClonar.isEmpty()) {
                String nuevoGrupoId = "g_" + System.currentTimeMillis() + "_" + (int)(Math.random()*100);
                Map<Bones.Bone, Bones.Bone> mapOriginalClon = new HashMap<Bones.Bone, Bones.Bone>();
                for (Bones.Bone origBone : huesosAClonar) {
                    String nuevoBoneId = "id_bone/" + nuevoGrupoId + "_" + System.currentTimeMillis() + "_" + (int)(Math.random()*1000);
                    Bones.Bone clonBone = new Bones.Bone(nuevoBoneId, nuevoGrupoId, null);
                    if (origBone.position != null) clonBone.position = origBone.position.clone();
                    if (origBone.rotation != null) clonBone.rotation = origBone.rotation.clone();
                    if (origBone.scale != null) clonBone.scale = origBone.scale.clone();
                    if (origBone.color != null) clonBone.color = origBone.color.clone();
                    if (origBone.hitbox != null && clonBone.hitbox != null) {
                        System.arraycopy(origBone.hitbox.localCenter, 0, clonBone.hitbox.localCenter, 0, 3);
                        System.arraycopy(origBone.hitbox.halfSize, 0, clonBone.hitbox.halfSize, 0, 3);
                    }
                    mapOriginalClon.put(origBone, clonBone);
                    huesosYaClonadosEnCiclo.add(origBone);
                }
                for (Bones.Bone origBone : huesosAClonar) {
                    Bones.Bone clonBone = mapOriginalClon.get(origBone);
                    if (clonBone == null) continue;
                    if (origBone.parent != null && mapOriginalClon.containsKey(origBone.parent)) {
                        clonBone.parent = mapOriginalClon.get(origBone.parent);
                        if (clonBone.parent != null && !clonBone.parent.children.contains(clonBone)) {
                            clonBone.parent.children.add(clonBone);
                        }
                    }
                    if (origBone.capturedVertices != null && !origBone.capturedVertices.isEmpty()) {
                        clonBone.groupedVertices = new HashMap<MyRenderer.Mesh, List<Bones.CapturedVertex>>();
                        List<Bones.CapturedVertex> listaNuevosVertices = new ArrayList<Bones.CapturedVertex>();
                        if (origBone.groupedVertices != null && origBone.groupedVertices.containsKey(original)) {
                            List<Bones.CapturedVertex> verticesOriginales = origBone.groupedVertices.get(original);
                            if (verticesOriginales != null) {
                                for (Bones.CapturedVertex cvOrig : verticesOriginales) {
                                    Bones.CapturedVertex cvClon = new Bones.CapturedVertex(meshCopia, cvOrig.index, cvOrig.weight);
                                    listaNuevosVertices.add(cvClon);
                                    clonBone.capturedVertices.add(cvClon);
                                }
                            }
                        }
                        clonBone.groupedVertices.put(meshCopia, listaNuevosVertices);
                        clonBone.captured = true;
                    } renderer.getBones().addBoneCompletely(clonBone);
                }
                for (Bones.Bone clonBone : mapOriginalClon.values()) {
                    if (clonBone.parent == null) clonBone.updateMatrixRecursive();
                }
                if (activity != null) activity.cantidadDeHuesos = renderer.getBones().getAllBones().size();
            }
        } return meshCopia;
    }

    private void copiarBoneSubtree(Bones.Bone rootBone) {
        if (rootBone == null) return;
        List<Bones.Bone> jerarquiaOriginal = new ArrayList<Bones.Bone>();
        recolectarHuesosSubarbol(rootBone, jerarquiaOriginal);
        String grupoDestino = rootBone.group;
        Map<Bones.Bone, Bones.Bone> mapaClonacion = new HashMap<Bones.Bone, Bones.Bone>();
        for (Bones.Bone orig : jerarquiaOriginal) {
            String nuevoId = "id_bone/" + grupoDestino + "_" + System.currentTimeMillis() + "_" + (int)(Math.random()*1000);
            Bones.Bone clon = new Bones.Bone(nuevoId, grupoDestino, null);
            if (orig.position != null) clon.position = orig.position.clone();
            if (orig.rotation != null) clon.rotation = orig.rotation.clone();
            if (orig.scale != null) clon.scale = orig.scale.clone();
            if (orig.color != null) clon.color = orig.color.clone();
            if (orig.hitbox != null && clon.hitbox != null) {
                System.arraycopy(orig.hitbox.localCenter, 0, clon.hitbox.localCenter, 0, 3);
                System.arraycopy(orig.hitbox.halfSize, 0, clon.hitbox.halfSize, 0, 3);
            } mapaClonacion.put(orig, clon);
        }
        for (Bones.Bone orig : jerarquiaOriginal) {
            Bones.Bone clon = mapaClonacion.get(orig);
            if (clon == null) continue;
            if (orig == rootBone) {
                clon.parent = null;
            } else if (orig.parent != null && mapaClonacion.containsKey(orig.parent)) {
                clon.parent = mapaClonacion.get(orig.parent);
                if (clon.parent != null && !clon.parent.children.contains(clon)) {
                    clon.parent.children.add(clon);
                }
            } else { clon.parent = null; }
            if (orig.capturedVertices != null && !orig.capturedVertices.isEmpty()) {
                clon.groupedVertices = new HashMap<MyRenderer.Mesh, List<Bones.CapturedVertex>>();
                for (Bones.CapturedVertex cv : orig.capturedVertices) {
                    Bones.CapturedVertex nuevoCv = new Bones.CapturedVertex(cv.mesh, cv.index, cv.weight);
                    clon.capturedVertices.add(nuevoCv);
                    if (cv.mesh != null) {
                        List<Bones.CapturedVertex> list = clon.groupedVertices.get(cv.mesh);
                        if (list == null) {
                            list = new ArrayList<Bones.CapturedVertex>();
                            clon.groupedVertices.put(cv.mesh, list);
                        } list.add(nuevoCv);
                    }
                } clon.captured = true;
            } renderer.getBones().addBoneCompletely(clon);
        }
        Bones.Bone clonRaiz = mapaClonacion.get(rootBone);
        if (clonRaiz != null) {
            if (rootBone.parent != null) {
                clonRaiz.position[0] = rootBone.worldMatrix[12];
                clonRaiz.position[1] = rootBone.worldMatrix[13];
                clonRaiz.position[2] = rootBone.worldMatrix[14];
                float[] euler = extraerRotacionDeMatriz(rootBone.worldMatrix);
                clonRaiz.rotation[0] = euler[0];
                clonRaiz.rotation[1] = euler[1];
                clonRaiz.rotation[2] = euler[2];
                clonRaiz.scale[0] = (float) Math.sqrt(rootBone.worldMatrix[0]*rootBone.worldMatrix[0] + rootBone.worldMatrix[1]*rootBone.worldMatrix[1] + rootBone.worldMatrix[2]*rootBone.worldMatrix[2]);
                clonRaiz.scale[1] = (float) Math.sqrt(rootBone.worldMatrix[4]*rootBone.worldMatrix[4] + rootBone.worldMatrix[5]*rootBone.worldMatrix[5] + rootBone.worldMatrix[6]*rootBone.worldMatrix[6]);
                clonRaiz.scale[2] = (float) Math.sqrt(rootBone.worldMatrix[8]*rootBone.worldMatrix[8] + rootBone.worldMatrix[9]*rootBone.worldMatrix[9] + rootBone.worldMatrix[10]*rootBone.worldMatrix[10]);
            }
            clonRaiz.updateMatrixRecursive();
            renderer.getBones().selectBone(clonRaiz.id);
            if (renderer.gizmo != null) {
                renderer.gizmo.setPosition(clonRaiz.position[0], clonRaiz.position[1], clonRaiz.position[2]);
                System.arraycopy(clonRaiz.rotation, 0, renderer.gizmo.rotation, 0, 3);
            }
        }
        if (activity != null) activity.cantidadDeHuesos = renderer.getBones().getAllBones().size();
    }

    private float[] extraerRotacionDeMatriz(float[] m) {
        float[] euler = new float[3];
        float m11 = m[0], m12 = m[4], m13 = m[8];
        float m21 = m[1], m22 = m[5], m23 = m[9];
        float m31 = m[2], m32 = m[6], m33 = m[10];
        float sX = (float) Math.sqrt(m11*m11 + m21*m21 + m31*m31);
        float sY = (float) Math.sqrt(m12*m12 + m22*m22 + m32*m32);
        float sZ = (float) Math.sqrt(m13*m13 + m23*m23 + m33*m33);
        m11 /= sX; m21 /= sX; m31 /= sX;
        m12 /= sY; m22 /= sY; m32 /= sY;
        m13 /= sZ; m23 /= sZ; m33 /= sZ;
        euler[0] = (float) Math.toDegrees(Math.asin(-clamp(m23, -1, 1)));
        if (Math.abs(m23) < 0.99999) {
            euler[1] = (float) Math.toDegrees(Math.atan2(m13, m33));
            euler[2] = (float) Math.toDegrees(Math.atan2(m21, m22));
        } else {
            euler[1] = (float) Math.toDegrees(Math.atan2(-m31, m11));
            euler[2] = 0;
        } return euler;
    }

    private float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private void recolectarHuesosSubarbol(Bones.Bone nodo, List<Bones.Bone> lista) {
        if (nodo == null) return;
        lista.add(nodo);
        if (nodo.children != null) {
            for (Bones.Bone hijo : nodo.children) recolectarHuesosSubarbol(hijo, lista);
        }
    }

    private void copiarCaja(Material.EffectBox original) {
        if (original == null || renderer.material == null) return;
        String nuevoId = "box_" + renderer.cantidadDeEfectos;
        Material.EffectBox copiaBox = new Material.EffectBox(nuevoId, original.effectType);
        if (original.position != null) copiaBox.position = original.position.clone();
        if (original.rotation != null) copiaBox.rotation = original.rotation.clone();
        copiaBox.scale = original.scale;
        copiaBox.numParticles = original.numParticles;
        renderer.material.getBoxes().add(copiaBox);
        renderer.material.selectedBox = copiaBox;
        renderer.cantidadDeEfectos++;
        renderer.material.actualizarCentroBox();
    }

    private FloatBuffer clonarFloatBuffer(FloatBuffer original) {
        if (original == null) return null;
        FloatBuffer copia = ByteBuffer.allocateDirect(original.capacity() * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        FloatBuffer duplicado = original.duplicate();
        duplicado.position(0);
        copia.put(duplicado);
        copia.position(0);
        return copia;
    }

    private String generarNombreCopia(String nombre) {
        if (nombre == null) return "copy";
        if (nombre.matches("(.+)_part\\d+$")) {
            String base = nombre.replaceAll("_part\\d+$", "");
            String suffix = nombre.substring(base.length()); // esto extrae el "_partN"
            return base + "_copy" + suffix;
        } return nombre + "_copy";
    }

    private ShortBuffer clonarShortBuffer(ShortBuffer original) {
        if (original == null) return null;
        ShortBuffer copia = ByteBuffer.allocateDirect(original.capacity() * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
        ShortBuffer duplicado = original.duplicate();
        duplicado.position(0);
        copia.put(duplicado);
        copia.position(0);
        return copia;
    }
}
