package ar.axt.animar;

import android.opengl.Matrix;
import android.view.MotionEvent;
import ar.axt.animar.Bones;
import ar.axt.animar.HistorialMovimientos;
import ar.axt.controles.AdministradorCamaras;
import ar.axt.controles.Gismo;
import ar.axt.controles.RayosInteraccion;
import ar.axt.controles.Tacto;
import ar.axt.materiales.Material;
import ar.axt.nopeby.MainActivity;
import ar.axt.nopeby.MyRenderer;
import java.nio.FloatBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class InteraccionGismo {
    public int RGE;
    private MainActivity activity;
    public boolean arrastreIniciado;
    private float currentDtx = 0f;
    private float currentDty = 0f;
    private float currentDtz = 0f;
    private final float[] boneCenterLocal;
    private final float[] boneCenterWorld;
    private final float[] boneInv;
    private final float[] boneLocal;
    private float[] dragPlaneNormal;
    private float[] dragPlanePoint;
    private float[] dragStartHit;
    private final float[] finalLocal;
    public Gismo gizmo;
    public float[] gizmoPosition;
    public float[] gizmoStartPos;
    private HistorialMovimientos historial;
    private final float[] invModel;
    private float[] lastHit;
    public float lastTouchX;
    public float lastTouchY;
    private final float[] localDelta;
    private final float[] model;
    public boolean movimientoEnCurso;
    private RayosInteraccion rayos;
    private MyRenderer renderer;
    private final float[] rotMatrix;
    private final float[] rotated;
    public float startTouchX;
    public float startTouchY;
    private final float[] tmpVec;
    private final float[] world;
    private final float[] worldDelta;
    public int xyz;

    public InteraccionGismo(MyRenderer renderer, MainActivity activity, HistorialMovimientos historial, RayosInteraccion rayos, Gismo gizmo) {
        this.movimientoEnCurso = false;
        this.arrastreIniciado = false;
        this.gizmoPosition = new float[]{0.0f, 0.0f, 0.0f};
        this.gizmoStartPos = new float[3];
        this.dragPlanePoint = new float[3];
        this.dragPlaneNormal = new float[3];
        this.dragStartHit = new float[3];
        this.xyz = 0;
        this.RGE = 0;
        this.lastHit = new float[3];
        this.worldDelta = new float[4];
        this.localDelta = new float[4];
        this.model = new float[16];
        this.invModel = new float[16];
        this.tmpVec = new float[4];
        this.world = new float[4];
        this.boneLocal = new float[4];
        this.rotated = new float[4];
        this.finalLocal = new float[4];
        this.boneInv = new float[16];
        this.rotMatrix = new float[16];
        this.boneCenterWorld = new float[4];
        this.boneCenterLocal = new float[4];
        this.renderer = renderer;
        this.activity = activity;
        this.historial = historial;
        this.gizmo = gizmo;
        this.rayos = rayos;
    }

    public InteraccionGismo() {
        this.movimientoEnCurso = false;
        this.arrastreIniciado = false;
        this.gizmoPosition = new float[]{0.0f, 0.0f, 0.0f};
        this.gizmoStartPos = new float[3];
        this.dragPlanePoint = new float[3];
        this.dragPlaneNormal = new float[3];
        this.dragStartHit = new float[3];
        this.xyz = 0;
        this.RGE = 0;
        this.lastHit = new float[3];
        this.worldDelta = new float[4];
        this.localDelta = new float[4];
        this.model = new float[16];
        this.invModel = new float[16];
        this.tmpVec = new float[4];
        this.world = new float[4];
        this.boneLocal = new float[4];
        this.rotated = new float[4];
        this.finalLocal = new float[4];
        this.boneInv = new float[16];
        this.rotMatrix = new float[16];
        this.boneCenterWorld = new float[4];
        this.boneCenterLocal = new float[4];
    }

    public int DireccionDeGismo() {
        return this.xyz;
    }

    public void setRayos(RayosInteraccion rayos) {
        this.rayos = rayos;
    }

    public void setLastTouch(float x, float y) {
        this.lastTouchX = x;
        this.lastTouchY = y;
    }

    public boolean isGizmoVisible() {
        return this.renderer.isGizmoVisible();
    }

    public void moverGizmo(Tacto.Ray ray, Tacto tacto, MotionEvent event) {
        if (ray == null || tacto == null || event == null) { return; }
        this.lastTouchX = event.getX();
        this.lastTouchY = event.getY();
        if (!this.arrastreIniciado) {
            this.arrastreIniciado = true;
            this.dragPlanePoint[0] = this.gizmoPosition[0];
            this.dragPlanePoint[1] = this.gizmoPosition[1];
            this.dragPlanePoint[2] = this.gizmoPosition[2];
            float[] forward = this.renderer.ajustesDeCamara.getCameraForward();
            this.dragPlaneNormal[0] = forward[0];
            this.dragPlaneNormal[1] = forward[1];
            this.dragPlaneNormal[2] = forward[2];
            float[] hit = tacto.intersectarRayoConPlano(ray, this.dragPlanePoint, this.dragPlaneNormal);
            if (hit != null) {
                this.dragStartHit = (float[]) hit.clone();
                this.lastHit = (float[]) hit.clone();
                this.gizmoStartPos[0] = this.gizmoPosition[0];
                this.gizmoStartPos[1] = this.gizmoPosition[1];
                this.gizmoStartPos[2] = this.gizmoPosition[2];
            }
        }
        float[] hit2 = tacto.intersectarRayoConPlano(ray, this.dragPlanePoint, this.dragPlaneNormal);
        if (hit2 == null) { return; }
        if (this.RGE == 2) {
            if (this.renderer.material.getSelectedBox() != null) {
                moverCajaGizmo(hit2);
            } else if (this.renderer.bone.getSelectedBone() != null) {
                moverBoneGizmo(hit2);
            } else if (this.renderer.rayosInteraccion.selectedMesh != null) {
                moverMeshGizmo(hit2);
            } else if (this.renderer.administradorCamaras != null && this.renderer.administradorCamaras.selectedCamara != null) {
                moverCamaraGizmo(hit2);
            }
        } else if (this.RGE == 1) {
            if (this.renderer.material.getSelectedBox() != null) {
                rotarCajaGizmo(hit2);
            } else if (this.renderer.bone.getSelectedBone() != null) {
                rotarBoneGizmo(hit2);
            } else if (this.renderer.rayosInteraccion.selectedMesh != null) {
                rotarMeshGizmo(hit2);
            } else if (this.renderer.administradorCamaras != null && this.renderer.administradorCamaras.selectedCamara != null) {
                rotarCamaraGizmo(hit2);
            }
        } else if (this.RGE == 3) {
            if (this.renderer.material.getSelectedBox() != null) {
                escalarCajaGizmo(hit2);
            } else if (this.renderer.bone.getSelectedBone() != null) {
                escalarBoneGizmo(hit2);
            } else if (this.renderer.rayosInteraccion.selectedMesh != null) {
                escalarMeshGizmo(hit2);
            } else if (this.renderer.administradorCamaras != null && this.renderer.administradorCamaras.selectedCamara != null) {
                escalarCamaraGizmo(hit2);
            }
        }
        this.lastHit[0] = hit2[0];
        this.lastHit[1] = hit2[1];
        this.lastHit[2] = hit2[2];
        this.renderer.bone.syncSelectedBoneWithGizmo(this.gizmo);
        this.renderer.material.syncSelectedBoxWithGizmo(this.gizmo);
        if (this.renderer.administradorCamaras != null) {
            this.renderer.administradorCamaras.syncSelectedCamaraWithGizmo(this.gizmo);
        }
    }

    public void moverMeshGizmo(float[] hit) {
        int dir;
        InteraccionGismo interaccionGismo = this;
        if (hit == null || (dir = DireccionDeGismo()) == 0) { return; }
        char c = 0;
        Matrix.setIdentityM(interaccionGismo.rotMatrix, 0);
        Matrix.rotateM(interaccionGismo.rotMatrix, 0, interaccionGismo.gizmo.rotation[0], 1.0f, 0.0f, 0.0f);
        char c2 = 1;
        Matrix.rotateM(interaccionGismo.rotMatrix, 0, interaccionGismo.gizmo.rotation[1], 0.0f, 1.0f, 0.0f);
        char c3 = 2;
        Matrix.rotateM(interaccionGismo.rotMatrix, 0, interaccionGismo.gizmo.rotation[2], 0.0f, 0.0f, 1.0f);
        interaccionGismo.tmpVec[0] = 0.0f;
        interaccionGismo.tmpVec[1] = 0.0f;
        interaccionGismo.tmpVec[2] = 0.0f;
        interaccionGismo.tmpVec[3] = 0.0f;
        if (dir == 2) {
            interaccionGismo.tmpVec[0] = 1.0f;
        } else if (dir == 1) {
            interaccionGismo.tmpVec[1] = 1.0f;
        } else if (dir == 3) {
            interaccionGismo.tmpVec[2] = 1.0f;
        }
        Matrix.multiplyMV(interaccionGismo.world, 0, interaccionGismo.rotMatrix, 0, interaccionGismo.tmpVec, 0);
        float dx = hit[0] - interaccionGismo.lastHit[0];
        float dy = hit[1] - interaccionGismo.lastHit[1];
        float dz = hit[2] - interaccionGismo.lastHit[2];
        float magnitude = (interaccionGismo.world[0] * dx) + (interaccionGismo.world[1] * dy) + (interaccionGismo.world[2] * dz);
        float moveX = interaccionGismo.world[0] * magnitude;
        float moveY = interaccionGismo.world[1] * magnitude;
        float moveZ = interaccionGismo.world[2] * magnitude;
        float[] fArr = interaccionGismo.gizmoPosition;
        fArr[0] = fArr[0] + moveX;
        float[] fArr2 = interaccionGismo.gizmoPosition;
        fArr2[1] = fArr2[1] + moveY;
        float[] fArr3 = interaccionGismo.gizmoPosition;
        fArr3[2] = fArr3[2] + moveZ;
        interaccionGismo.gizmo.setPosition(interaccionGismo.gizmoPosition[0], interaccionGismo.gizmoPosition[1], interaccionGismo.gizmoPosition[2]);
        for (HistorialMovimientos.EstadoInicial ei : interaccionGismo.historial.estadoInicialGrupo) {
            if (ei.mesh != null) {
                float[] fArr4 = ei.mesh.translation;
                fArr4[c] = fArr4[c] + moveX;
                float[] fArr5 = ei.mesh.translation;
                fArr5[c2] = fArr5[c2] + moveY;
                float[] fArr6 = ei.mesh.translation;
                fArr6[c3] = fArr6[c3] + moveZ;
                ei.mesh.hitbox.updateFromMesh(ei.mesh);
            } else if (ei.bone != null) {
                Bones.Bone b = ei.bone;
                boolean padreEnGrupo = false;
                if (b.parent != null) {
                    Iterator<HistorialMovimientos.EstadoInicial> it = interaccionGismo.historial.estadoInicialGrupo.iterator();
                    while (true) {
                        if (!it.hasNext()) { break; }
                        HistorialMovimientos.EstadoInicial ei2 = it.next();
                        if (ei2.bone == b.parent) { padreEnGrupo = true; break; }
                    }
                }
                if (!padreEnGrupo) {
                    if (b.parent != null) {
                        float[] parentMat = (float[]) b.parent.worldMatrix.clone();
                        Bones.normalizeMatrix(parentMat);
                        float[] invParent = new float[16];
                        Matrix.invertM(invParent, 0, parentMat, 0);
                        float[] worldDelta = {moveX, moveY, moveZ, 0.0f};
                        float[] localDelta = new float[4];
                        Matrix.multiplyMV(localDelta, 0, invParent, 0, worldDelta, 0);
                        b.position[0] += localDelta[0];
                        b.position[1] += localDelta[1];
                        b.position[2] += localDelta[2];
                    } else {
                        b.position[0] += moveX;
                        b.position[1] += moveY;
                        b.position[2] += moveZ;
                    } b.updateMatrixRecursive();
                }
            } interaccionGismo = this; }
    }

    public void rotarMeshGizmo(float[] hit) {
        int dir;
        float[] rotInc;
        int axis;
        int dir2;
        if (hit == null || (dir = DireccionDeGismo()) == 0) { return; }
        Matrix.setIdentityM(this.rotMatrix, 0);
        Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[0], 1.0f, 0.0f, 0.0f);
        Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[1], 0.0f, 1.0f, 0.0f);
        Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[2], 0.0f, 0.0f, 1.0f);
        this.tmpVec[0] = 0.0f;
        this.tmpVec[1] = 0.0f;
        this.tmpVec[2] = 0.0f;
        this.tmpVec[3] = 0.0f;
        if (dir == 2) {
            this.tmpVec[0] = 1.0f;
        } else if (dir == 1) {
            this.tmpVec[1] = 1.0f;
        } else if (dir == 3) {
            this.tmpVec[2] = 1.0f;
        }
        Matrix.multiplyMV(this.world, 0, this.rotMatrix, 0, this.tmpVec, 0);
        float dx = hit[0] - this.lastHit[0];
        float dy = hit[1] - this.lastHit[1];
        float dz = hit[2] - this.lastHit[2];
        float magnitude = (this.world[0] * dx) + (this.world[1] * dy) + (this.world[2] * dz);
        float delta = 100.0f * magnitude;
        int axis2 = dir == 1 ? 1 : dir == 2 ? 0 : 2;
        float cx = this.gizmoPosition[0];
        float cy = this.gizmoPosition[1];
        float cz = this.gizmoPosition[2];
        float[] rotInc2 = new float[16];
        Matrix.setIdentityM(rotInc2, 0);
        if (axis2 == 0) {
            rotInc = rotInc2;
            axis = axis2;
            Matrix.rotateM(rotInc2, 0, delta, 1.0f, 0.0f, 0.0f);
        } else {
            rotInc = rotInc2;
            axis = axis2;
            if (axis == 1) {
                Matrix.rotateM(rotInc, 0, delta, 0.0f, 1.0f, 0.0f);
            } else if (axis == 2) {
                Matrix.rotateM(rotInc, 0, delta, 0.0f, 0.0f, 1.0f);
            }
        }
        for (HistorialMovimientos.EstadoInicial ei : this.historial.estadoInicialGrupo) {
            if (ei.mesh != null) {
                float[] meshMat = new float[16];
                Matrix.setIdentityM(meshMat, 0);
                Matrix.translateM(meshMat, 0, ei.mesh.translation[0], ei.mesh.translation[1], ei.mesh.translation[2]);
                Matrix.rotateM(meshMat, 0, ei.mesh.rotation[0], 1.0f, 0.0f, 0.0f);
                Matrix.rotateM(meshMat, 0, ei.mesh.rotation[1], 0.0f, 1.0f, 0.0f);
                Matrix.rotateM(meshMat, 0, ei.mesh.rotation[2], 0.0f, 0.0f, 1.0f);
                Matrix.scaleM(meshMat, 0, ei.mesh.scale[0], ei.mesh.scale[1], ei.mesh.scale[2]);
                float[] newMat = new float[16];
                meshMat[12] -= cx;
                meshMat[13] -= cy;
                meshMat[14] -= cz;
                Matrix.multiplyMM(newMat, 0, rotInc, 0, meshMat, 0);
                newMat[12] += cx;
                newMat[13] += cy;
                newMat[14] += cz;
                ei.mesh.translation[0] = newMat[12];
                ei.mesh.translation[1] = newMat[13];
                ei.mesh.translation[2] = newMat[14];
                Bones.normalizeMatrix(newMat);
                double sinY = Math.max(-1.0, Math.min(1.0, newMat[8]));
                ei.mesh.rotation[1] = (float) Math.toDegrees(Math.asin(sinY));
                ei.mesh.rotation[0] = (float) Math.toDegrees(Math.atan2(-newMat[9], newMat[10]));
                ei.mesh.rotation[2] = (float) Math.toDegrees(Math.atan2(-newMat[4], newMat[0]));
                for (int i = 0; i < 3; i++) {
                    if (ei.mesh.rotation[i] < 0.0f) {
                        ei.mesh.rotation[i] += 360.0f;
                    }
                    ei.mesh.rotation[i] = ei.mesh.rotation[i] % 360.0f;
                }
                ei.mesh.hitbox.updateFromMesh(ei.mesh);
                dir2 = dir;
            } else if (ei.bone == null) {
                dir2 = dir;
            } else {
                Bones.Bone b = ei.bone;
                boolean padreEnGrupo = false;
                if (b.parent != null) {
                    Iterator<HistorialMovimientos.EstadoInicial> it = this.historial.estadoInicialGrupo.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        HistorialMovimientos.EstadoInicial ei2 = it.next();
                        if (ei2.bone == b.parent) {
                            padreEnGrupo = true;
                            break;
                        }
                    }
                }
                if (!padreEnGrupo) {
                    float[] oldWorld = b.worldMatrix.clone();
                    float[] newWorld = new float[16];
                    oldWorld[12] -= cx;
                    oldWorld[13] -= cy;
                    oldWorld[14] -= cz;
                    Matrix.multiplyMM(newWorld, 0, rotInc, 0, oldWorld, 0);
                    newWorld[12] += cx;
                    newWorld[13] += cy;
                    newWorld[14] += cz;
                    float[] localMat = new float[16];
                    if (b.parent != null) {
                        float[] parentMat = (float[]) b.parent.worldMatrix.clone();
                        Bones.normalizeMatrix(parentMat);
                        float[] invParent = new float[16];
                        Matrix.invertM(invParent, 0, parentMat, 0);
                        Matrix.multiplyMM(localMat, 0, invParent, 0, newWorld, 0);
                    } else {
                        System.arraycopy(newWorld, 0, localMat, 0, 16);
                    }
                    b.position[0] = localMat[12];
                    b.position[1] = localMat[13];
                    b.position[2] = localMat[14];
                    Bones.normalizeMatrix(localMat);
                    double sinY = Math.max(-1.0, Math.min(1.0, localMat[8]));
                    b.rotation[1] = (float) Math.toDegrees(Math.asin(sinY));
                    b.rotation[0] = (float) Math.toDegrees(Math.atan2(-localMat[9], localMat[10]));
                    b.rotation[2] = (float) Math.toDegrees(Math.atan2(-localMat[4], localMat[0]));
                    for (int i = 0; i < 3; i++) {
                        if (b.rotation[i] < 0.0f) {
                            b.rotation[i] += 360.0f;
                        }
                        b.rotation[i] = b.rotation[i] % 360.0f;
                    } b.updateMatrixRecursive();
                } dir2 = dir;
            } dir = dir2;
        }
        this.gizmo.rotation[axis] = (this.gizmo.rotation[axis] + delta) % 360.0f;
        if (this.gizmo.rotation[axis] < 0.0f) {
            float[] fArr3 = this.gizmo.rotation;
            fArr3[axis] = fArr3[axis] + 360.0f;
        }
    }

    public void escalarMeshGizmo(float[] hit) {
        float delta;
        if (hit == null) return;
        int dir = DireccionDeGismo();
        float currentGScale = this.gizmo.getScale();
        if (dir == 0) {
            float dLast = (float) Math.sqrt(Math.pow(this.lastHit[0] - this.gizmoPosition[0], 2.0d) + Math.pow(this.lastHit[1] - this.gizmoPosition[1], 2.0d) + Math.pow(this.lastHit[2] - this.gizmoPosition[2], 2.0d));
            float dCurr = (float) Math.sqrt(Math.pow(hit[0] - this.gizmoPosition[0], 2.0d) + Math.pow(hit[1] - this.gizmoPosition[1], 2.0d) + Math.pow(hit[2] - this.gizmoPosition[2], 2.0d));
            delta = (dCurr - dLast);
        } else {
            Matrix.setIdentityM(this.rotMatrix, 0);
            Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[0], 1.0f, 0.0f, 0.0f);
            Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[1], 0.0f, 1.0f, 0.0f);
            Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[2], 0.0f, 0.0f, 1.0f);
            this.tmpVec[0] = 0; this.tmpVec[1] = 0; this.tmpVec[2] = 0; this.tmpVec[3] = 0;
            if (dir == 2) this.tmpVec[0] = 1; else if (dir == 1) this.tmpVec[1] = 1; else if (dir == 3) this.tmpVec[2] = 1;
            Matrix.multiplyMV(this.world, 0, this.rotMatrix, 0, this.tmpVec, 0);
            delta = (this.world[0]*(hit[0]-lastHit[0]) + this.world[1]*(hit[1]-lastHit[1]) + this.world[2]*(hit[2]-lastHit[2]));
        }
        float nextGScale = Math.max(0.05f, currentGScale + delta);
        float ratio = nextGScale / currentGScale;
        int axis = -1;
        if (dir == 2) axis = 0; else if (dir == 1) axis = 1; else if (dir == 3) axis = 2;
        float rx = (axis == 0 || axis == -1) ? ratio : 1.0f;
        float ry = (axis == 1 || axis == -1) ? ratio : 1.0f;
        float rz = (axis == 2 || axis == -1) ? ratio : 1.0f;
        float cx = this.gizmoPosition[0];
        float cy = this.gizmoPosition[1];
        float cz = this.gizmoPosition[2];
        for (HistorialMovimientos.EstadoInicial ei : this.historial.estadoInicialGrupo) {
            if (ei.mesh != null) {
                ei.mesh.scale[0] *= rx;
                ei.mesh.scale[1] *= ry;
                ei.mesh.scale[2] *= rz;
                for(int i=0; i<3; i++) if(ei.mesh.scale[i] < 0.01f) ei.mesh.scale[i] = 0.01f;
                ei.mesh.translation[0] = ((ei.mesh.translation[0] - cx) * rx) + cx;
                ei.mesh.translation[1] = ((ei.mesh.translation[1] - cy) * ry) + cy;
                ei.mesh.translation[2] = ((ei.mesh.translation[2] - cz) * rz) + cz;
                ei.mesh.hitbox.updateFromMesh(ei.mesh);
            } else if (ei.bone != null) {
                Bones.Bone b = ei.bone;
                b.scale[0] *= rx; b.scale[1] *= ry; b.scale[2] *= rz;
                for(int i=0; i<3; i++) if(b.scale[i] < 0.01f) b.scale[i] = 0.01f;
                b.position[0] *= rx; b.position[1] *= ry; b.position[2] *= rz;

                boolean padreEnGrupo = false;
                if (b.parent != null) {
                    for (HistorialMovimientos.EstadoInicial ei2 : this.historial.estadoInicialGrupo) {
                        if (ei2.bone == b.parent) { padreEnGrupo = true; break; }
                    }
                }
                if (!padreEnGrupo) {
                    float[] wPos = {b.worldMatrix[12], b.worldMatrix[13], b.worldMatrix[14], 1.0f};
                    float nwx = ((wPos[0] - cx) * rx) + cx;
                    float nwy = ((wPos[1] - cy) * ry) + cy;
                    float nwz = ((wPos[2] - cz) * rz) + cz;
                    if (b.parent != null) {
                        float[] pMat = (float[]) b.parent.worldMatrix.clone();
                        Bones.normalizeMatrix(pMat);
                        float[] invP = new float[16]; Matrix.invertM(invP, 0, pMat, 0);
                        float[] nWVec = {nwx, nwy, nwz, 1.0f}; float[] nLVec = new float[4];
                        Matrix.multiplyMV(nLVec, 0, invP, 0, nWVec, 0);
                        b.position[0] = nLVec[0]; b.position[1] = nLVec[1]; b.position[2] = nLVec[2];
                    } else {
                        b.position[0] = nwx; b.position[1] = nwy; b.position[2] = nwz;
                    }
                } b.updateMatrixRecursive();
            }
        } this.gizmo.setScale(nextGScale);
    }

    public void actualizarCentroGizmo() {
        float cx = 0.0f;
        float cy = 0.0f;
        float cz = 0.0f;
        int count = 0;
        MyRenderer.Mesh refMesh = null;
        if (this.renderer.bone != null && this.renderer.bone.getSelectedBone() != null) {
            Bones.Bone b = this.renderer.bone.getSelectedBone();
            float cx2 = b.worldMatrix[12];
            float cy2 = b.worldMatrix[13];
            float cz2 = b.worldMatrix[14];
            this.gizmoPosition[0] = cx2;
            this.gizmoPosition[1] = cy2;
            this.gizmoPosition[2] = cz2;
            if (this.gizmo != null) {
                this.gizmo.setPosition(cx2, cy2, cz2);
                this.gizmo.rotation[0] = b.rotation[0];
                this.gizmo.rotation[1] = b.rotation[1];
                this.gizmo.rotation[2] = b.rotation[2];
                float maxS = Math.max(b.scale[0], Math.max(b.scale[1], b.scale[2]));
                this.gizmo.setScale(2.0f * maxS);
                return;
            } return;
        }
        if (this.historial.estadoInicialGrupo != null && !this.historial.estadoInicialGrupo.isEmpty()) {
            for (HistorialMovimientos.EstadoInicial ei : this.historial.estadoInicialGrupo) {
                if (ei.mesh != null) {
                    cx += ei.mesh.translation[0];
                    cy += ei.mesh.translation[1];
                    cz += ei.mesh.translation[2];
                    count++;
                    if (refMesh == null) { refMesh = ei.mesh; }
                }
            }
        } else if (this.renderer.rayosInteraccion != null && this.renderer.rayosInteraccion.selectedMesh != null) {
            refMesh = this.renderer.rayosInteraccion.selectedMesh;
            List<MyRenderer.Mesh> relacionados = this.renderer.rayosInteraccion.getMeshesRelacionados(refMesh);
            count = relacionados.size();
            if (count == 0) {
                cx = refMesh.translation[0];
                cy = refMesh.translation[1];
                cz = refMesh.translation[2];
                count = 1;
            } else {
                for (MyRenderer.Mesh m : relacionados) {
                    cx += m.translation[0];
                    cy += m.translation[1];
                    cz += m.translation[2];
                }
            }
        }
        if (count == 0 || refMesh == null) { return; }
        float cx3 = cx / count;
        float cy3 = cy / count;
        float cz3 = cz / count;
        this.gizmoPosition[0] = cx3;
        this.gizmoPosition[1] = cy3;
        this.gizmoPosition[2] = cz3;
        if (this.gizmo != null) {
            this.gizmo.setPosition(cx3, cy3, cz3);
            this.gizmo.rotation[0] = refMesh.rotation[0];
            this.gizmo.rotation[1] = refMesh.rotation[1];
            this.gizmo.rotation[2] = refMesh.rotation[2];
            float maxS2 = Math.max(refMesh.scale[0], Math.max(refMesh.scale[1], refMesh.scale[2]));
            this.gizmo.setScale(4.0f * maxS2);
            return;
        }
        if (this.renderer.administradorCamaras != null && this.renderer.administradorCamaras.selectedCamara != null) {
            AdministradorCamaras.MarcadorCamara cam = this.renderer.administradorCamaras.selectedCamara;
            this.gizmoPosition[0] = cam.position[0];
            this.gizmoPosition[1] = cam.position[1];
            this.gizmoPosition[2] = cam.position[2];
            if (this.gizmo != null) {
                this.gizmo.setPosition(cam.position[0], cam.position[1], cam.position[2]);
                this.gizmo.rotation[0] = cam.rotation[0];
                this.gizmo.rotation[1] = cam.rotation[1];
                this.gizmo.rotation[2] = cam.rotation[2];
                this.gizmo.setScale(cam.scale * 4.0f);
                return;
            }
        }
        if (this.renderer.material != null && this.renderer.material.selectedBox != null) {
            Material.EffectBox box = this.renderer.material.selectedBox;
            this.gizmoPosition[0] = box.position[0];
            this.gizmoPosition[1] = box.position[1];
            this.gizmoPosition[2] = box.position[2];
            if (this.gizmo != null) {
                this.gizmo.setPosition(box.position[0], box.position[1], box.position[2]);
                this.gizmo.rotation[0] = box.rotation[0];
                this.gizmo.rotation[1] = box.rotation[1];
                this.gizmo.rotation[2] = box.rotation[2];
                this.gizmo.setScale(box.scale * 4.0f);
            }
        }
        if (this.activity != null) {
            this.activity.requestRender();
        } else if (this.renderer != null && this.renderer.activity != null) {
            this.renderer.activity.requestRender();
        }
    }

    private void moverCamaraGizmo(float[] hit) {
        int dir = DireccionDeGismo();
        if (hit == null || dir == 0) return;
        Matrix.setIdentityM(this.rotMatrix, 0);
        Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[0], 1.0f, 0.0f, 0.0f);
        Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[1], 0.0f, 1.0f, 0.0f);
        Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[2], 0.0f, 0.0f, 1.0f);
        this.tmpVec[0] = 0; this.tmpVec[1] = 0; this.tmpVec[2] = 0; this.tmpVec[3] = 0;
        if (dir == 2) this.tmpVec[0] = 1; else if (dir == 1) this.tmpVec[1] = 1; else if (dir == 3) this.tmpVec[2] = 1;
        Matrix.multiplyMV(this.world, 0, this.rotMatrix, 0, this.tmpVec, 0);
        float dx = hit[0] - this.lastHit[0];
        float dy = hit[1] - this.lastHit[1];
        float dz = hit[2] - this.lastHit[2];
        float magnitude = (this.world[0] * dx) + (this.world[1] * dy) + (this.world[2] * dz);
        float moveX = this.world[0] * magnitude;
        float moveY = this.world[1] * magnitude;
        float moveZ = this.world[2] * magnitude;
        this.gizmoPosition[0] += moveX;
        this.gizmoPosition[1] += moveY;
        this.gizmoPosition[2] += moveZ;
        this.gizmo.setPosition(this.gizmoPosition[0], this.gizmoPosition[1], this.gizmoPosition[2]);
        for (HistorialMovimientos.EstadoInicial ei : this.historial.estadoInicialGrupo) {
            if (ei.cameraMarker != null) {
                ei.cameraMarker.position[0] += moveX;
                ei.cameraMarker.position[1] += moveY;
                ei.cameraMarker.position[2] += moveZ;
                ei.cameraMarker.actualizarHitboxes();
            }
        }
    }

    private void rotarCamaraGizmo(float[] hit) {
        int dir = DireccionDeGismo();
        if (hit == null || dir == 0) return;
        int axis = dir == 1 ? 1 : dir == 2 ? 0 : 2;
        Matrix.setIdentityM(this.rotMatrix, 0);
        Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[0], 1.0f, 0.0f, 0.0f);
        Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[1], 0.0f, 1.0f, 0.0f);
        Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[2], 0.0f, 0.0f, 1.0f);
        this.tmpVec[0] = 0; this.tmpVec[1] = 0; this.tmpVec[2] = 0; this.tmpVec[3] = 0;
        if (dir == 2) this.tmpVec[0] = 1; else if (dir == 1) this.tmpVec[1] = 1; else if (dir == 3) this.tmpVec[2] = 1;
        Matrix.multiplyMV(this.world, 0, this.rotMatrix, 0, this.tmpVec, 0);
        float dx = hit[0] - this.lastHit[0];
        float dy = hit[1] - this.lastHit[1];
        float dz = hit[2] - this.lastHit[2];
        float delta = ((this.world[0] * dx) + (this.world[1] * dy) + (this.world[2] * dz)) * 100.0f;
        for (HistorialMovimientos.EstadoInicial ei : this.historial.estadoInicialGrupo) {
            if (ei.cameraMarker != null) {
                ei.cameraMarker.rotation[axis] = (ei.cameraMarker.rotation[axis] + delta) % 360.0f;
                if (ei.cameraMarker.rotation[axis] < 0.0f) ei.cameraMarker.rotation[axis] += 360.0f;
                this.gizmo.rotation[axis] = ei.cameraMarker.rotation[axis];
                ei.cameraMarker.actualizarHitboxes();
            }
        }
    }

    private void escalarCamaraGizmo(float[] hit) {
        float delta;
        if (hit == null) return;
        int dir = DireccionDeGismo();
        if (dir == 0) {
            float dLast = (float) Math.sqrt(Math.pow(this.lastHit[0] - this.gizmoPosition[0], 2.0d) + Math.pow(this.lastHit[1] - this.gizmoPosition[1], 2.0d) + Math.pow(this.lastHit[2] - this.gizmoPosition[2], 2.0d));
            float dCurr = (float) Math.sqrt(Math.pow(hit[0] - this.gizmoPosition[0], 2.0d) + Math.pow(hit[1] - this.gizmoPosition[1], 2.0d) + Math.pow(hit[2] - this.gizmoPosition[2], 2.0d));
            delta = (dCurr - dLast);
        } else {
            Matrix.setIdentityM(this.rotMatrix, 0);
            Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[0], 1.0f, 0.0f, 0.0f);
            Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[1], 0.0f, 1.0f, 0.0f);
            Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[2], 0.0f, 0.0f, 1.0f);
            this.tmpVec[0] = 0; this.tmpVec[1] = 0; this.tmpVec[2] = 0; this.tmpVec[3] = 0;
            if (dir == 2) this.tmpVec[0] = 1; else if (dir == 1) this.tmpVec[1] = 1; else if (dir == 3) this.tmpVec[2] = 1;
            Matrix.multiplyMV(this.world, 0, this.rotMatrix, 0, this.tmpVec, 0);
            delta = (this.world[0]*(hit[0]-lastHit[0]) + this.world[1]*(hit[1]-lastHit[1]) + this.world[2]*(hit[2]-lastHit[2]));
        }
        for (HistorialMovimientos.EstadoInicial ei : this.historial.estadoInicialGrupo) {
            if (ei.cameraMarker != null) {
                float ns = Math.max(0.05f, this.gizmo.getScale() + delta);
                this.gizmo.setScale(ns);
                ei.cameraMarker.scale = ns / 4.0f;
                ei.cameraMarker.actualizarHitboxes();
            }
        }
    }

    public void captureCurrentGroup() {
        for (Bones.Bone b : this.renderer.bone.getAllBones()) {
            if (b.group.equals(this.renderer.bone.getCurrentGroup())) {
                b.capturedVertices.clear();
                if (b.groupedVertices != null) {
                    b.groupedVertices.clear();
                }
                for (MyRenderer.Mesh m : this.renderer.meshes) {
                    this.renderer.bone.capturePolygonsForBone(b, m.subMesh, m, this.gizmo.scale);
                }
            }
        }
    }

    private void moverBoneGizmo(float[] hit) {
        int dir;
        if (hit == null || (dir = DireccionDeGismo()) == 0) { return;
        }
        int i = 0;
        Matrix.setIdentityM(this.rotMatrix, 0);
        Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[0], 1.0f, 0.0f, 0.0f);
        char c = 1;
        Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[1], 0.0f, 1.0f, 0.0f);
        Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[2], 0.0f, 0.0f, 1.0f);
        this.tmpVec[0] = 0.0f;
        this.tmpVec[1] = 0.0f;
        this.tmpVec[2] = 0.0f;
        this.tmpVec[3] = 0.0f;
        if (dir == 2) {
            this.tmpVec[0] = 1.0f;
        } else if (dir == 1) {
            this.tmpVec[1] = 1.0f;
        } else if (dir == 3) {
            this.tmpVec[2] = 1.0f;
        }
        Matrix.multiplyMV(this.world, 0, this.rotMatrix, 0, this.tmpVec, 0);
        float dx = hit[0] - this.lastHit[0];
        float dy = hit[1] - this.lastHit[1];
        float dz = hit[2] - this.lastHit[2];
        float magnitude = (this.world[0] * dx) + (this.world[1] * dy) + (this.world[2] * dz);
        float moveX = this.world[0] * magnitude;
        float moveY = this.world[1] * magnitude;
        float moveZ = this.world[2] * magnitude;
        float[] fArr = this.gizmoPosition;
        fArr[0] = fArr[0] + moveX;
        float[] fArr2 = this.gizmoPosition;
        fArr2[1] = fArr2[1] + moveY;
        float[] fArr3 = this.gizmoPosition;
        fArr3[2] = fArr3[2] + moveZ;
        this.gizmo.setPosition(this.gizmoPosition[0], this.gizmoPosition[1], this.gizmoPosition[2]);
        if (this.renderer.bone.hasSelectedBone()) {
            Bones.Bone b = this.renderer.bone.getSelectedBone();
            if (b.parent != null) {
                float[] parentMat = (float[]) b.parent.worldMatrix.clone();
                float lx = (float) Math.sqrt((parentMat[0] * parentMat[0]) + (parentMat[1] * parentMat[1]) + (parentMat[2] * parentMat[2]));
                float ly = (float) Math.sqrt((parentMat[4] * parentMat[4]) + (parentMat[5] * parentMat[5]) + (parentMat[6] * parentMat[6]));
                float lz = (float) Math.sqrt((parentMat[8] * parentMat[8]) + (parentMat[9] * parentMat[9]) + (parentMat[10] * parentMat[10]));
                parentMat[0] = parentMat[0] / lx;
                parentMat[1] = parentMat[1] / lx;
                parentMat[2] = parentMat[2] / lx;
                parentMat[4] = parentMat[4] / ly;
                parentMat[5] = parentMat[5] / ly;
                parentMat[6] = parentMat[6] / ly;
                parentMat[8] = parentMat[8] / lz;
                parentMat[9] = parentMat[9] / lz;
                parentMat[10] = parentMat[10] / lz;
                float[] invParent = new float[16];
                Matrix.invertM(invParent, 0, parentMat, 0);
                float[] worldDelta = {moveX, moveY, moveZ, 0.0f};
                float[] localDelta = new float[4];
                Matrix.multiplyMV(localDelta, 0, invParent, 0, worldDelta, 0);
                float[] fArr4 = b.position;
                fArr4[0] = fArr4[0] + localDelta[0];
                float[] fArr5 = b.position;
                fArr5[1] = fArr5[1] + localDelta[1];
                float[] fArr6 = b.position;
                fArr6[2] = fArr6[2] + localDelta[2];
            } else {
                float[] fArr7 = b.position;
                fArr7[0] = fArr7[0] + moveX;
                float[] fArr8 = b.position;
                fArr8[1] = fArr8[1] + moveY;
                float[] fArr9 = b.position;
                fArr9[2] = fArr9[2] + moveZ;
            }
            b.updateMatrixRecursive();
            if (this.activity.huesos == 0) {
                aplicarTraslacionRecursiva(b, moveX, moveY, moveZ, new java.util.HashSet<String>());
            }
        }
    }

    private void rotarBoneGizmo(float[] hit) {
        int axisIdx;
        int dir = DireccionDeGismo();
        if (dir == 0) { return; }
        if (dir == 1) { axisIdx = 1;
        } else { axisIdx = dir == 2 ? 0 : 2; }
        Matrix.setIdentityM(this.rotMatrix, 0);
        Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[0], 1.0f, 0.0f, 0.0f);
        Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[1], 0.0f, 1.0f, 0.0f);
        Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[2], 0.0f, 0.0f, 1.0f);
        this.tmpVec[0] = 0.0f;
        this.tmpVec[1] = 0.0f;
        this.tmpVec[2] = 0.0f;
        this.tmpVec[3] = 0.0f;
        if (dir == 2) {
            this.tmpVec[0] = 1.0f;
        } else if (dir == 1) {
            this.tmpVec[1] = 1.0f;
        } else if (dir == 3) {
            this.tmpVec[2] = 1.0f;
        }
        Matrix.multiplyMV(this.world, 0, this.rotMatrix, 0, this.tmpVec, 0);
        float dx = hit[0] - this.lastHit[0];
        float dy = hit[1] - this.lastHit[1];
        float dz = hit[2] - this.lastHit[2];
        float delta = ((this.world[0] * dx) + (this.world[1] * dy) + (this.world[2] * dz)) * 100.0f;
        float[] fArr = this.gizmo.rotation;
        fArr[axisIdx] = fArr[axisIdx] + delta;
        float[] fArr2 = this.gizmo.rotation;
        fArr2[axisIdx] = fArr2[axisIdx] % 360.0f;
        if (this.gizmo.rotation[axisIdx] < 0.0f) {
            float[] fArr3 = this.gizmo.rotation;
            fArr3[axisIdx] = fArr3[axisIdx] + 360.0f;
        }
        if (this.renderer.bone.hasSelectedBone()) {
            Bones.Bone b = this.renderer.bone.getSelectedBone();
            float ax = axisIdx == 0 ? 1 : 0;
            float ay = axisIdx == 1 ? 1 : 0;
            float az = axisIdx == 2 ? 1 : 0;
            float px = b.headWorld[0];
            float py = b.headWorld[1];
            float pz = b.headWorld[2];
            b.rotation[axisIdx] = this.gizmo.rotation[axisIdx];
            b.updateMatrixRecursive();
            if (this.activity.huesos == 0) {
                aplicarRotacionRecursiva(b, delta, ax, ay, az, px, py, pz, new java.util.HashSet<String>());
            }
        }
    }

    private void escalarBoneGizmo(float[] hit) {
        float delta;
        if (hit == null) { return; }
        int dir = DireccionDeGismo();
        if (dir != 0) {
            Matrix.setIdentityM(this.rotMatrix, 0);
            Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[0], 1.0f, 0.0f, 0.0f);
            Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[1], 0.0f, 1.0f, 0.0f);
            Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[2], 0.0f, 0.0f, 1.0f);
            this.tmpVec[0] = 0.0f;
            this.tmpVec[1] = 0.0f;
            this.tmpVec[2] = 0.0f;
            this.tmpVec[3] = 0.0f;
            if (dir == 2) { // X
                this.tmpVec[0] = 1.0f;
            } else if (dir == 1) { // Y
                this.tmpVec[1] = 1.0f;
            } else if (dir == 3) { // Z
                this.tmpVec[2] = 1.0f;
            }
            Matrix.multiplyMV(this.world, 0, this.rotMatrix, 0, this.tmpVec, 0);
            float dx = hit[0] - this.lastHit[0];
            float dy = hit[1] - this.lastHit[1];
            float dz = hit[2] - this.lastHit[2];
            delta = ((this.world[0] * dx) + (this.world[1] * dy) + (this.world[2] * dz));
        } else {
            float dLast = (float) Math.sqrt(Math.pow(this.lastHit[0] - this.gizmoPosition[0], 2.0d) + Math.pow(this.lastHit[1] - this.gizmoPosition[1], 2.0d) + Math.pow(this.lastHit[2] - this.gizmoPosition[2], 2.0d));
            float dCurr = (float) Math.sqrt(Math.pow(hit[0] - this.gizmoPosition[0], 2.0d) + Math.pow(hit[1] - this.gizmoPosition[1], 2.0d) + Math.pow(hit[2] - this.gizmoPosition[2], 2.0d));
            delta = (dCurr - dLast);
        }
        int axis = -1;
        if (dir == 2) axis = 0;
        else if (dir == 1) axis = 1;
        else if (dir == 3) axis = 2;
        float ns = this.gizmo.getScale() + delta;
        if (ns < 0.05f) ns = 0.05f;
        this.gizmo.setScale(ns);
        if (this.renderer.bone.hasSelectedBone()) {
            Bones.Bone b = this.renderer.bone.getSelectedBone();
            if (axis != -1) {
                b.scale[axis] += (delta / 2.0f);
                if (b.scale[axis] < 0.025f) b.scale[axis] = 0.025f;
            } else {
                float factor = delta / 2.0f;
                b.scale[0] += factor;
                b.scale[1] += factor;
                b.scale[2] += factor;
                if (b.scale[0] < 0.025f) b.scale[0] = 0.025f;
                if (b.scale[1] < 0.025f) b.scale[1] = 0.025f;
                if (b.scale[2] < 0.025f) b.scale[2] = 0.025f;
            } b.updateMatrixRecursive();
            if (this.activity.huesos != 0) { return; }
            this.boneCenterWorld[0] = b.worldMatrix[12];
            this.boneCenterWorld[1] = b.worldMatrix[13];
            this.boneCenterWorld[2] = b.worldMatrix[14];
            this.boneCenterWorld[3] = 1.0f;
            for (Map.Entry<MyRenderer.Mesh, List<Bones.CapturedVertex>> entry : b.groupedVertices.entrySet()) {
                MyRenderer.Mesh mesh = entry.getKey();
                List<Bones.CapturedVertex> vertices = entry.getValue();
                if (mesh.subMesh.vertexBuffer != null) {
                    FloatBuffer vb = mesh.subMesh.vertexBuffer;
                    Matrix.setIdentityM(this.model, 0);
                    Matrix.translateM(this.model, 0, mesh.translation[0], mesh.translation[1], mesh.translation[2]);
                    Matrix.rotateM(this.model, 0, mesh.rotation[0], 1.0f, 0.0f, 0.0f);
                    Matrix.rotateM(this.model, 0, mesh.rotation[1], 0.0f, 1.0f, 0.0f);
                    Matrix.rotateM(this.model, 0, mesh.rotation[2], 0.0f, 0.0f, 1.0f);
                    Matrix.scaleM(this.model, 0, mesh.scale[0], mesh.scale[1], mesh.scale[2]);
                    Matrix.invertM(this.invModel, 0, this.model, 0);
                    Matrix.multiplyMV(this.boneCenterLocal, 0, this.invModel, 0, this.boneCenterWorld, 0);
                    float cx = this.boneCenterLocal[0];
                    float cy = this.boneCenterLocal[1];
                    float cz = this.boneCenterLocal[2];
                    for (Bones.CapturedVertex cv : vertices) {
                        int base = cv.index * 3;
                        float vx = vb.get(base);
                        float vy = vb.get(base + 1);
                        float vz = vb.get(base + 2);
                        float bdx = vx - cx;
                        float bdy = vy - cy;
                        float bdz = vz - cz;
                        float effectiveScaleX = 1.0f;
                        float effectiveScaleY = 1.0f;
                        float effectiveScaleZ = 1.0f;
                        if (axis == 0) {
                            effectiveScaleX = (cv.weight * delta) + 1.0f;
                        } else if (axis == 1) {
                            effectiveScaleY = (cv.weight * delta) + 1.0f;
                        } else if (axis == 2) {
                            effectiveScaleZ = (cv.weight * delta) + 1.0f;
                        } else {
                            float s = (cv.weight * delta) + 1.0f;
                            effectiveScaleX = s; effectiveScaleY = s; effectiveScaleZ = s;
                        }
                        vb.put(base, (bdx * effectiveScaleX) + cx);
                        vb.put(base + 1, cy + (bdy * effectiveScaleY));
                        vb.put(base + 2, (bdz * effectiveScaleZ) + cz);
                    } mesh.hitbox.updateFromMesh(mesh);
                }
            }
        }
    }

    private void moverCajaGizmo(float[] hit) {
        int dir;
        if (hit == null || (dir = DireccionDeGismo()) == 0) { return; }
        Matrix.setIdentityM(this.rotMatrix, 0);
        Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[0], 1.0f, 0.0f, 0.0f);
        Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[1], 0.0f, 1.0f, 0.0f);
        Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[2], 0.0f, 0.0f, 1.0f);
        this.tmpVec[0] = 0.0f;
        this.tmpVec[1] = 0.0f;
        this.tmpVec[2] = 0.0f;
        this.tmpVec[3] = 0.0f;
        if (dir == 2) {
            this.tmpVec[0] = 1.0f;
        } else if (dir == 1) {
            this.tmpVec[1] = 1.0f;
        } else if (dir == 3) {
            this.tmpVec[2] = 1.0f;
        }
        Matrix.multiplyMV(this.world, 0, this.rotMatrix, 0, this.tmpVec, 0);
        float dx = hit[0] - this.lastHit[0];
        float dy = hit[1] - this.lastHit[1];
        float dz = hit[2] - this.lastHit[2];
        float magnitude = (this.world[0] * dx) + (this.world[1] * dy) + (this.world[2] * dz);
        float moveX = this.world[0] * magnitude;
        float moveY = this.world[1] * magnitude;
        float moveZ = this.world[2] * magnitude;
        float[] fArr = this.gizmoPosition;
        fArr[0] = fArr[0] + moveX;
        float[] fArr2 = this.gizmoPosition;
        fArr2[1] = fArr2[1] + moveY;
        float[] fArr3 = this.gizmoPosition;
        fArr3[2] = fArr3[2] + moveZ;
        this.gizmo.setPosition(this.gizmoPosition[0], this.gizmoPosition[1], this.gizmoPosition[2]);
        for (HistorialMovimientos.EstadoInicial ei : this.historial.estadoInicialGrupo) {
            if (ei.box != null) {
                float[] fArr4 = ei.box.position;
                fArr4[0] = fArr4[0] + moveX;
                float[] fArr5 = ei.box.position;
                fArr5[1] = fArr5[1] + moveY;
                float[] fArr6 = ei.box.position;
                fArr6[2] = fArr6[2] + moveZ;
            }
        }
    }

    private void rotarCajaGizmo(float[] hit) {
        int dir;
        if (hit == null || (dir = DireccionDeGismo()) == 0) { return; }
        int axis = 0;
        Matrix.setIdentityM(this.rotMatrix, 0);
        Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[0], 1.0f, 0.0f, 0.0f);
        Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[1], 0.0f, 1.0f, 0.0f);
        Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[2], 0.0f, 0.0f, 1.0f);
        this.tmpVec[0] = 0.0f;
        this.tmpVec[1] = 0.0f;
        this.tmpVec[2] = 0.0f;
        this.tmpVec[3] = 0.0f;
        if (dir == 2) {
            this.tmpVec[0] = 1.0f;
        } else if (dir == 1) {
            this.tmpVec[1] = 1.0f;
        } else if (dir == 3) {
            this.tmpVec[2] = 1.0f;
        }
        Matrix.multiplyMV(this.world, 0, this.rotMatrix, 0, this.tmpVec, 0);
        float dx = hit[0] - this.lastHit[0];
        float dy = hit[1] - this.lastHit[1];
        float dz = hit[2] - this.lastHit[2];
        float magnitude = (this.world[0] * dx) + (this.world[1] * dy) + (this.world[2] * dz);
        float delta = 100.0f * magnitude;
        if (dir == 1) {
            axis = 1;
        } else if (dir != 2) {
            axis = 2;
        }
        for (HistorialMovimientos.EstadoInicial ei : this.historial.estadoInicialGrupo) {
            if (ei.box != null) {
                ei.box.rotation[axis] = (ei.box.rotation[axis] + delta) % 360.0f;
                if (ei.box.rotation[axis] < 0.0f) {
                    float[] fArr = ei.box.rotation;
                    fArr[axis] = fArr[axis] + 360.0f;
                } this.gizmo.rotation[axis] = ei.box.rotation[axis];
            }
        }
    }

    private void escalarCajaGizmo(float[] hit) {
        float delta;
        if (hit == null) { return; }
        int dir = DireccionDeGismo();
        if (dir == 0) {
            float dLast = (float) Math.sqrt(Math.pow(this.lastHit[0] - this.gizmoPosition[0], 2.0d) + Math.pow(this.lastHit[1] - this.gizmoPosition[1], 2.0d) + Math.pow(this.lastHit[2] - this.gizmoPosition[2], 2.0d));
            float dCurr = (float) Math.sqrt(Math.pow(hit[0] - this.gizmoPosition[0], 2.0d) + Math.pow(hit[1] - this.gizmoPosition[1], 2.0d) + Math.pow(hit[2] - this.gizmoPosition[2], 2.0d));
            delta = (dCurr - dLast) * 1.0f;
        } else {
            Matrix.setIdentityM(this.rotMatrix, 0);
            Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[0], 1.0f, 0.0f, 0.0f);
            Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[1], 0.0f, 1.0f, 0.0f);
            Matrix.rotateM(this.rotMatrix, 0, this.gizmo.rotation[2], 0.0f, 0.0f, 1.0f);
            this.tmpVec[0] = 0.0f;
            this.tmpVec[1] = 0.0f;
            this.tmpVec[2] = 0.0f;
            this.tmpVec[3] = 0.0f;
            if (dir == 2) {
                this.tmpVec[0] = 1.0f;
            } else if (dir == 1) {
                this.tmpVec[1] = 1.0f;
            } else if (dir == 3) {
                this.tmpVec[2] = 1.0f;
            }
            Matrix.multiplyMV(this.world, 0, this.rotMatrix, 0, this.tmpVec, 0);
            float dx = hit[0] - this.lastHit[0];
            float dy = hit[1] - this.lastHit[1];
            float dz = hit[2] - this.lastHit[2];
            delta = ((this.world[0] * dx) + (this.world[1] * dy) + (this.world[2] * dz)) * 1.0f;
        }
        for (HistorialMovimientos.EstadoInicial ei : this.historial.estadoInicialGrupo) {
            if (ei.box != null) {
                float nuevaEscalaGizmo = Math.max(0.05f, this.gizmo.getScale() + delta);
                this.gizmo.setScale(nuevaEscalaGizmo);
                ei.box.scale = nuevaEscalaGizmo / 4.0f;
            }
        }
    }

    public void iniciarMovimientoAgrupadoConRay(Tacto.Ray ray, Tacto tacto) {
        if (ray == null) { return; }
        this.historial.iniciarMovimientoAgrupado();
        if (this.renderer.material.getSelectedBox() != null) {
            this.rayos.selectedMesh = null;
            this.renderer.bone.selectedBone = null;
            iniciarMovimientoCaja(ray, tacto);
        } else if (this.rayos.selectedMesh != null) {
            this.renderer.material.selectedBox = null;
            iniciarMovimientoMesh(ray, tacto);
        } else if (this.renderer.bone.selectedBone != null) {
            this.renderer.material.selectedBox = null;
            iniciarMovimientoBone(ray, tacto);
        } else if (this.renderer.administradorCamaras != null && this.renderer.administradorCamaras.selectedCamara != null) {
            this.renderer.material.selectedBox = null;
            this.rayos.selectedMesh = null;
            this.renderer.bone.selectedBone = null;
            iniciarMovimientoCamara(ray, tacto);
        }
    }

    private void iniciarMovimientoCamara(Tacto.Ray ray, Tacto tacto) {
        AdministradorCamaras.MarcadorCamara cam = this.renderer.administradorCamaras.selectedCamara;
        if (cam == null) return;
        this.arrastreIniciado = true;
        this.historial.estadoInicialGrupo.clear();
        HistorialMovimientos.EstadoInicial ei = new HistorialMovimientos.EstadoInicial();
        ei.cameraMarker = cam;
        ei.pos = (float[]) cam.position.clone();
        ei.rot = (float[]) cam.rotation.clone();
        ei.scaleBox = cam.scale; // Reusing scaleBox for camera scale
        this.historial.estadoInicialGrupo.add(ei);
        this.gizmoStartPos[0] = this.gizmoPosition[0];
        this.gizmoStartPos[1] = this.gizmoPosition[1];
        this.gizmoStartPos[2] = this.gizmoPosition[2];
        float[] forward = this.renderer.ajustesDeCamara.getCameraForward();
        this.dragPlaneNormal[0] = forward[0];
        this.dragPlaneNormal[1] = forward[1];
        this.dragPlaneNormal[2] = forward[2];
        this.dragPlanePoint[0] = this.gizmoPosition[0];
        this.dragPlanePoint[1] = this.gizmoPosition[1];
        this.dragPlanePoint[2] = this.gizmoPosition[2];
        float[] hit = tacto.intersectarRayoConPlano(ray, this.dragPlanePoint, this.dragPlaneNormal);
        if (hit != null) {
            this.dragStartHit[0] = hit[0];
            this.dragStartHit[1] = hit[1];
            this.dragStartHit[2] = hit[2];
            this.lastHit[0] = hit[0];
            this.lastHit[1] = hit[1];
            this.lastHit[2] = hit[2];
        }
    }

    private void iniciarMovimientoMesh(Tacto.Ray ray, Tacto tacto) {
        if (this.rayos.selectedMesh == null) { return; }
        actualizarCentroGizmo();
        this.arrastreIniciado = true;
        this.startTouchX = this.lastTouchX;
        this.startTouchY = this.lastTouchY;
        this.historial.estadoInicialGrupo.clear();
        List<MyRenderer.Mesh> relacionados = new java.util.ArrayList<>();
        if (this.activity != null && this.activity.modoMultiseleccion == 1 && !this.activity.meshesMultiseleccionados.isEmpty()) {
            for (String nameM : this.activity.meshesMultiseleccionados) {
                MyRenderer.Mesh mReal = this.rayos.obtenerMeshPorNombre(nameM);
                if (mReal != null && !relacionados.contains(mReal)) {
                    relacionados.add(mReal);
                }
            }
        } else {
            relacionados = this.rayos.getMeshesRelacionados(this.rayos.selectedMesh);
        }
        for (MyRenderer.Mesh m : relacionados) {
            HistorialMovimientos.EstadoInicial ei = new HistorialMovimientos.EstadoInicial();
            ei.mesh = m;
            ei.pos = (float[]) m.translation.clone();
            ei.rot = (float[]) m.rotation.clone();
            ei.scale = (float[]) m.scale.clone();
            if (m.subMesh != null && m.subMesh.vertexBuffer != null) {
                float[] vData = new float[m.subMesh.vertexBuffer.capacity()];
                m.subMesh.vertexBuffer.position(0);
                m.subMesh.vertexBuffer.get(vData);
                m.subMesh.vertexBuffer.position(0);
                ei.vertexData = vData;
            }
            this.historial.estadoInicialGrupo.add(ei);
        }
        if (this.renderer.bone != null) {
            for (Bones.Bone b : this.renderer.bone.getAllBones()) {
                boolean estaAfectado = false;
                if (b.groupedVertices != null) {
                    Iterator<MyRenderer.Mesh> it = relacionados.iterator();
                    while (true) {
                        if (!it.hasNext()) { break;
                        }
                        if (b.groupedVertices.containsKey(it.next())) {
                            estaAfectado = true; break;
                        }
                    }
                }
                if (estaAfectado) {
                    HistorialMovimientos.EstadoInicial eiBone = new HistorialMovimientos.EstadoInicial();
                    eiBone.bone = b;
                    eiBone.pos = (float[]) b.position.clone();
                    eiBone.rot = (float[]) b.rotation.clone();
                    eiBone.scale = (float[]) b.scale.clone();
                    this.historial.estadoInicialGrupo.add(eiBone);
                }
            }
        }
        this.gizmoStartPos[0] = this.gizmoPosition[0];
        this.gizmoStartPos[1] = this.gizmoPosition[1];
        this.gizmoStartPos[2] = this.gizmoPosition[2];
        float[] forward = this.renderer.ajustesDeCamara.getCameraForward();
        this.dragPlaneNormal[0] = forward[0];
        this.dragPlaneNormal[1] = forward[1];
        this.dragPlaneNormal[2] = forward[2];
        this.dragPlanePoint[0] = this.gizmoPosition[0];
        this.dragPlanePoint[1] = this.gizmoPosition[1];
        this.dragPlanePoint[2] = this.gizmoPosition[2];
        float[] hit = tacto.intersectarRayoConPlano(ray, this.dragPlanePoint, this.dragPlaneNormal);
        if (hit != null) {
            this.dragStartHit[0] = hit[0];
            this.dragStartHit[1] = hit[1];
            this.dragStartHit[2] = hit[2];
            this.lastHit[0] = hit[0];
            this.lastHit[1] = hit[1];
            this.lastHit[2] = hit[2];
        }
    }

    private void iniciarMovimientoCaja(Tacto.Ray ray, Tacto tacto) {
        Material.EffectBox box = this.renderer.material.getSelectedBox();
        if (box == null) { return; }
        this.arrastreIniciado = true;
        this.historial.estadoInicialGrupo.clear();
        HistorialMovimientos.EstadoInicial ei = new HistorialMovimientos.EstadoInicial();
        ei.box = box;
        ei.pos = (float[]) box.position.clone();
        ei.rot = (float[]) box.rotation.clone();
        ei.scaleBox = box.scale;
        this.historial.estadoInicialGrupo.add(ei);
        this.gizmoStartPos[0] = this.gizmoPosition[0];
        this.gizmoStartPos[1] = this.gizmoPosition[1];
        this.gizmoStartPos[2] = this.gizmoPosition[2];
        float[] forward = this.renderer.ajustesDeCamara.getCameraForward();
        this.dragPlaneNormal[0] = forward[0];
        this.dragPlaneNormal[1] = forward[1];
        this.dragPlaneNormal[2] = forward[2];
        this.dragPlanePoint[0] = this.gizmoPosition[0];
        this.dragPlanePoint[1] = this.gizmoPosition[1];
        this.dragPlanePoint[2] = this.gizmoPosition[2];
        float[] hit = tacto.intersectarRayoConPlano(ray, this.dragPlanePoint, this.dragPlaneNormal);
        if (hit != null) {
            this.dragStartHit[0] = hit[0];
            this.dragStartHit[1] = hit[1];
            this.dragStartHit[2] = hit[2];
            this.lastHit[0] = hit[0];
            this.lastHit[1] = hit[1];
            this.lastHit[2] = hit[2];
        }
    }

    private void iniciarMovimientoBone(Tacto.Ray ray, Tacto tacto) {
        Bones.Bone boneSel = this.renderer.bone.getSelectedBone();
        if (boneSel == null) { return; }
        actualizarCentroGizmo();
        this.arrastreIniciado = true;
        this.startTouchX = this.lastTouchX;
        this.startTouchY = this.lastTouchY;
        this.historial.estadoInicialGrupo.clear();
        HistorialMovimientos.EstadoInicial eiB = new HistorialMovimientos.EstadoInicial();
        eiB.bone = boneSel;
        eiB.pos = (float[]) boneSel.position.clone();
        eiB.rot = (float[]) boneSel.rotation.clone();
        eiB.scale = (float[]) boneSel.scale.clone();
        eiB.initialWorldMatrix = (float[]) boneSel.worldMatrix.clone();
        this.historial.estadoInicialGrupo.add(eiB);
        if (this.activity.huesos == 0 && boneSel.groupedVertices != null) {
            for (MyRenderer.Mesh m : boneSel.groupedVertices.keySet()) {
                HistorialMovimientos.EstadoInicial eiM = new HistorialMovimientos.EstadoInicial();
                eiM.mesh = m;
                eiM.pos = (float[]) m.translation.clone();
                eiM.rot = (float[]) m.rotation.clone();
                eiM.scale = (float[]) m.scale.clone();
                if (m.subMesh != null && m.subMesh.vertexBuffer != null) {
                    float[] vData = new float[m.subMesh.vertexBuffer.capacity()];
                    m.subMesh.vertexBuffer.position(0);
                    m.subMesh.vertexBuffer.get(vData);
                    m.subMesh.vertexBuffer.position(0);
                    eiM.vertexData = vData;
                } this.historial.estadoInicialGrupo.add(eiM);
            }
        }
        this.gizmoStartPos[0] = this.gizmoPosition[0];
        this.gizmoStartPos[1] = this.gizmoPosition[1];
        this.gizmoStartPos[2] = this.gizmoPosition[2];
        float[] forward = this.renderer.ajustesDeCamara.getCameraForward();
        this.dragPlaneNormal[0] = forward[0];
        this.dragPlaneNormal[1] = forward[1];
        this.dragPlaneNormal[2] = forward[2];
        this.dragPlanePoint[0] = this.gizmoPosition[0];
        this.dragPlanePoint[1] = this.gizmoPosition[1];
        this.dragPlanePoint[2] = this.gizmoPosition[2];
        float[] hit = tacto.intersectarRayoConPlano(ray, this.dragPlanePoint, this.dragPlaneNormal);
        if (hit != null) {
            this.dragStartHit[0] = hit[0];
            this.dragStartHit[1] = hit[1];
            this.dragStartHit[2] = hit[2];
            this.lastHit[0] = hit[0];
            this.lastHit[1] = hit[1];
            this.lastHit[2] = hit[2];
        }
    }

    public void aplicarTraslacionRecursiva(Bones.Bone b, float moveX, float moveY, float moveZ, java.util.HashSet<String> procesados) {
        if (b.groupedVertices != null) {
            for (Map.Entry<MyRenderer.Mesh, List<Bones.CapturedVertex>> entry : b.groupedVertices.entrySet()) {
                MyRenderer.Mesh mesh = entry.getKey();
                List<Bones.CapturedVertex> vertices = entry.getValue();
                if (mesh.subMesh.vertexBuffer != null) {
                    FloatBuffer vb = mesh.subMesh.vertexBuffer;
                    Matrix.setIdentityM(this.model, 0);
                    Matrix.translateM(this.model, 0, mesh.translation[0], mesh.translation[1], mesh.translation[2]);
                    Matrix.rotateM(this.model, 0, mesh.rotation[0], 1.0f, 0.0f, 0.0f);
                    Matrix.rotateM(this.model, 0, mesh.rotation[1], 0.0f, 1.0f, 0.0f);
                    Matrix.rotateM(this.model, 0, mesh.rotation[2], 0.0f, 0.0f, 1.0f);
                    Matrix.scaleM(this.model, 0, mesh.scale[0], mesh.scale[1], mesh.scale[2]);
                    Matrix.invertM(this.invModel, 0, this.model, 0);
                    for (Bones.CapturedVertex cv : vertices) {
                        String key = mesh.hashCode() + "_" + cv.index;
                        if (procesados.contains(key)) { continue;} procesados.add(key);
                        int base = cv.index * 3;
                        this.tmpVec[0] = vb.get(base);
                        this.tmpVec[1] = vb.get(base + 1);
                        this.tmpVec[2] = vb.get(base + 2);
                        this.tmpVec[3] = 1.0f;
                        Matrix.multiplyMV(this.world, 0, this.model, 0, this.tmpVec, 0);
                        float[] fArr10 = this.world;
                        fArr10[0] = fArr10[0] + (cv.weight * moveX);
                        float[] fArr11 = this.world;
                        fArr11[1] = fArr11[1] + (cv.weight * moveY);
                        float[] fArr12 = this.world;
                        fArr12[2] = fArr12[2] + (cv.weight * moveZ);
                        Matrix.multiplyMV(this.finalLocal, 0, this.invModel, 0, this.world, 0);
                        vb.put(base, this.finalLocal[0]);
                        vb.put(base + 1, this.finalLocal[1]);
                        vb.put(base + 2, this.finalLocal[2]);
                    }
                }
            }
        }
        for (Bones.Bone child : b.children) { aplicarTraslacionRecursiva(child, moveX, moveY, moveZ, procesados); }
    }

    public void aplicarRotacionRecursiva(Bones.Bone b, float delta, float ax, float ay, float az, float px, float py, float pz, java.util.HashSet<String> procesados) {
        if (b.groupedVertices != null) {
            Matrix.setIdentityM(this.rotMatrix, 0);
            Matrix.rotateM(this.rotMatrix, 0, delta, ax, ay, az);
            for (Map.Entry<MyRenderer.Mesh, List<Bones.CapturedVertex>> entry : b.groupedVertices.entrySet()) {
                MyRenderer.Mesh mesh = entry.getKey();
                List<Bones.CapturedVertex> vertices = entry.getValue();
                if (mesh.subMesh.vertexBuffer != null) {
                    FloatBuffer vb = mesh.subMesh.vertexBuffer;
                    Matrix.setIdentityM(this.model, 0);
                    Matrix.translateM(this.model, 0, mesh.translation[0], mesh.translation[1], mesh.translation[2]);
                    Matrix.rotateM(this.model, 0, mesh.rotation[0], 1.0f, 0.0f, 0.0f);
                    Matrix.rotateM(this.model, 0, mesh.rotation[1], 0.0f, 1.0f, 0.0f);
                    Matrix.rotateM(this.model, 0, mesh.rotation[2], 0.0f, 0.0f, 1.0f);
                    Matrix.scaleM(this.model, 0, mesh.scale[0], mesh.scale[1], mesh.scale[2]);
                    Matrix.invertM(this.invModel, 0, this.model, 0);
                    for (Bones.CapturedVertex cv : vertices) {
                        String key = mesh.hashCode() + "_" + cv.index;
                        if (procesados.contains(key)) { continue; } procesados.add(key);
                        int base = cv.index * 3;
                        this.tmpVec[0] = vb.get(base);
                        this.tmpVec[1] = vb.get(base + 1);
                        this.tmpVec[2] = vb.get(base + 2);
                        this.tmpVec[3] = 1.0f;
                        Matrix.multiplyMV(this.world, 0, this.model, 0, this.tmpVec, 0);
                        float[] fArr4 = this.world;
                        fArr4[0] = fArr4[0] - px;
                        float[] fArr5 = this.world;
                        fArr5[1] = fArr5[1] - py;
                        float[] fArr6 = this.world;
                        fArr6[2] = fArr6[2] - pz;
                        Matrix.multiplyMV(this.rotated, 0, this.rotMatrix, 0, this.world, 0);
                        float[] fArr7 = this.rotated;
                        fArr7[0] = fArr7[0] + px;
                        float[] fArr8 = this.rotated;
                        fArr8[1] = fArr8[1] + py;
                        float[] fArr9 = this.rotated;
                        fArr9[2] = fArr9[2] + pz;
                        Matrix.multiplyMV(this.finalLocal, 0, this.invModel, 0, this.rotated, 0);
                        vb.put(base, this.finalLocal[0]);
                        vb.put(base + 1, this.finalLocal[1]);
                        vb.put(base + 2, this.finalLocal[2]);
                    } mesh.hitbox.updateFromMesh(mesh);
                }
            }
        }
        for (Bones.Bone child : b.children) {
            aplicarRotacionRecursiva(child, delta, ax, ay, az, px, py, pz, procesados);
        }
    }

    public void iniciarMovimientoAgrupado() {
        if (this.xyz != 0) {
            this.historial.iniciarMovimientoAgrupado();
        } this.movimientoEnCurso = false;
    }

    public void finalizarMovimientoAgrupado() {
        if (this.arrastreIniciado) {
            this.historial.finalizarMovimientoAgrupado();
            this.arrastreIniciado = false;
        }
    }

    public void iniciarEdicionDesdeUI() {
        this.currentDtx = 0f;
        this.currentDty = 0f;
        this.currentDtz = 0f;
        this.arrastreIniciado = true;
        this.historial.iniciarMovimientoAgrupado();
        this.historial.estadoInicialGrupo.clear();
        if (this.renderer.material != null && this.renderer.material.getSelectedBox() != null) {
            Material.EffectBox box = this.renderer.material.getSelectedBox();
            HistorialMovimientos.EstadoInicial ei = new HistorialMovimientos.EstadoInicial();
            ei.box = box;
            ei.pos = (float[]) box.position.clone();
            ei.rot = (float[]) box.rotation.clone();
            ei.scaleBox = box.scale;
            this.historial.estadoInicialGrupo.add(ei);
        } else if (this.rayos != null && this.rayos.selectedMesh != null) {
            actualizarCentroGizmo();
            List<MyRenderer.Mesh> relacionados = new java.util.ArrayList<>();
            if (this.activity != null && this.activity.modoMultiseleccion == 1 && !this.activity.meshesMultiseleccionados.isEmpty()) {
                for (String nameM : this.activity.meshesMultiseleccionados) {
                    MyRenderer.Mesh mReal = this.rayos.obtenerMeshPorNombre(nameM);
                    if (mReal != null && !relacionados.contains(mReal)) {
                        relacionados.add(mReal);
                    }
                }
            } else {
                relacionados = this.rayos.getMeshesRelacionados(this.rayos.selectedMesh);
            }
            for (MyRenderer.Mesh m : relacionados) {
                HistorialMovimientos.EstadoInicial ei = new HistorialMovimientos.EstadoInicial();
                ei.mesh = m;
                ei.pos = (float[]) m.translation.clone();
                ei.rot = (float[]) m.rotation.clone();
                ei.scale = (float[]) m.scale.clone();
                if (m.subMesh != null && m.subMesh.vertexBuffer != null) {
                    float[] vData = new float[m.subMesh.vertexBuffer.capacity()];
                    m.subMesh.vertexBuffer.position(0);
                    m.subMesh.vertexBuffer.get(vData);
                    m.subMesh.vertexBuffer.position(0);
                    ei.vertexData = vData;
                }
                this.historial.estadoInicialGrupo.add(ei);
            }
            if (this.renderer.bone != null && this.renderer.bone.getAllBones() != null) {
                List<Bones.Bone> afectados = new java.util.ArrayList<>();
                for (Bones.Bone b : this.renderer.bone.getAllBones()) {
                    boolean estaAfectado = false;
                    if (b.groupedVertices != null) {
                        for (MyRenderer.Mesh mRel : relacionados) {
                            if (b.groupedVertices.containsKey(mRel)) {
                                estaAfectado = true;
                                break;
                            }
                        }
                    }
                    if (!estaAfectado && b.group != null) {
                        for (MyRenderer.Mesh mRel : relacionados) {
                            String baseBuscado = ar.axt.database.AdministrarDatos.extraerNombreBase(mRel.name);
                            if (b.group.equalsIgnoreCase(baseBuscado) || baseBuscado.toLowerCase().contains(b.group.toLowerCase())) {
                                estaAfectado = true;
                                break;
                            }
                        }
                    }
                    if (estaAfectado) {
                        if (!afectados.contains(b)) afectados.add(b);
                        Bones.Bone curr = b.parent;
                        while (curr != null) {
                            if (!afectados.contains(curr)) {
                                afectados.add(curr);
                            }
                            curr = curr.parent;
                        }
                    }
                }
                for (Bones.Bone b : afectados) {
                    HistorialMovimientos.EstadoInicial eiBone = new HistorialMovimientos.EstadoInicial();
                    eiBone.bone = b;
                    eiBone.pos = (float[]) b.position.clone();
                    eiBone.rot = (float[]) b.rotation.clone();
                    eiBone.scale = (float[]) b.scale.clone();
                    eiBone.initialWorldMatrix = (float[]) b.worldMatrix.clone();
                    this.historial.estadoInicialGrupo.add(eiBone);
                }
            }
        } else if (this.renderer.bone != null && this.renderer.bone.getSelectedBone() != null) {
            Bones.Bone boneSel = this.renderer.bone.getSelectedBone();
            actualizarCentroGizmo();
            HistorialMovimientos.EstadoInicial eiB = new HistorialMovimientos.EstadoInicial();
            eiB.bone = boneSel;
            eiB.pos = (float[]) boneSel.position.clone();
            eiB.rot = (float[]) boneSel.rotation.clone();
            eiB.scale = (float[]) boneSel.scale.clone();
            eiB.initialWorldMatrix = (float[]) boneSel.worldMatrix.clone();
            this.historial.estadoInicialGrupo.add(eiB);
            if (this.activity != null && this.activity.huesos == 0 && boneSel.groupedVertices != null) {
                for (MyRenderer.Mesh m : boneSel.groupedVertices.keySet()) {
                    HistorialMovimientos.EstadoInicial eiM = new HistorialMovimientos.EstadoInicial();
                    eiM.mesh = m;
                    eiM.pos = (float[]) m.translation.clone();
                    eiM.rot = (float[]) m.rotation.clone();
                    eiM.scale = (float[]) m.scale.clone();
                    if (m.subMesh != null && m.subMesh.vertexBuffer != null) {
                        float[] vData = new float[m.subMesh.vertexBuffer.capacity()];
                        m.subMesh.vertexBuffer.position(0);
                        m.subMesh.vertexBuffer.get(vData);
                        m.subMesh.vertexBuffer.position(0);
                        eiM.vertexData = vData;
                    }
                    this.historial.estadoInicialGrupo.add(eiM);
                }
            }
        }
    }

    public void finalizarEdicionDesdeUI() {
        if (this.arrastreIniciado) {
            this.historial.finalizarMovimientoAgrupado();
            this.arrastreIniciado = false;
            this.currentDtx = 0f;
            this.currentDty = 0f;
            this.currentDtz = 0f;
        }
    }

    public void aplicarTraslacionDesdeUI(float dtx, float dty, float dtz) {
        if (this.historial == null || this.historial.estadoInicialGrupo == null || this.historial.estadoInicialGrupo.isEmpty()) { return; }
        this.currentDtx = dtx;
        this.currentDty = dty;
        this.currentDtz = dtz;
        for (HistorialMovimientos.EstadoInicial ei : this.historial.estadoInicialGrupo) {
            if (ei.mesh != null && ei.vertexData != null && ei.mesh.subMesh != null && ei.mesh.subMesh.vertexBuffer != null) {
                ei.mesh.subMesh.vertexBuffer.position(0);
                ei.mesh.subMesh.vertexBuffer.put(ei.vertexData);
                ei.mesh.subMesh.vertexBuffer.position(0);
            }
        }
        for (HistorialMovimientos.EstadoInicial ei : this.historial.estadoInicialGrupo) {
            if (ei.mesh != null) {
                ei.mesh.translation[0] = ei.pos[0] + dtx;
                ei.mesh.translation[1] = ei.pos[1] + dty;
                ei.mesh.translation[2] = ei.pos[2] + dtz;
                if (ei.mesh.hitbox != null) {
                    ei.mesh.hitbox.updateFromMesh(ei.mesh);
                }
            } else if (ei.bone != null) {
                Bones.Bone b = ei.bone;
                if (b.parent != null) {
                    float[] parentMat = (float[]) b.parent.worldMatrix.clone();
                    Bones.normalizeMatrix(parentMat);
                    float[] invParent = new float[16];
                    Matrix.invertM(invParent, 0, parentMat, 0);
                    float[] worldDelta = {dtx, dty, dtz, 0.0f};
                    float[] localDelta = new float[4];
                    Matrix.multiplyMV(localDelta, 0, invParent, 0, worldDelta, 0);
                    b.position[0] = ei.pos[0] + localDelta[0];
                    b.position[1] = ei.pos[1] + localDelta[1];
                    b.position[2] = ei.pos[2] + localDelta[2];
                } else {
                    b.position[0] = ei.pos[0] + dtx;
                    b.position[1] = ei.pos[1] + dty;
                    b.position[2] = ei.pos[2] + dtz;
                }
                b.updateMatrixRecursive();
                if (this.activity != null && this.activity.huesos == 0 && (this.rayos == null || this.rayos.selectedMesh == null)) {
                    aplicarTraslacionRecursiva(b, dtx, dty, dtz, new java.util.HashSet<String>());
                }
            } else if (ei.box != null) {
                ei.box.position[0] = ei.pos[0] + dtx;
                ei.box.position[1] = ei.pos[1] + dty;
                ei.box.position[2] = ei.pos[2] + dtz;
            } else if (ei.cameraMarker != null) {
                ei.cameraMarker.position[0] = ei.pos[0] + dtx;
                ei.cameraMarker.position[1] = ei.pos[1] + dty;
                ei.cameraMarker.position[2] = ei.pos[2] + dtz;
                ei.cameraMarker.actualizarHitboxes();
            }
        }
        actualizarCentroGizmo();
    }
    public void aplicarRotacionDesdeUI(float drx, float dry, float drz) {
        if (this.historial.estadoInicialGrupo == null || this.historial.estadoInicialGrupo.isEmpty()) return;
        for (HistorialMovimientos.EstadoInicial ei : this.historial.estadoInicialGrupo) {
            if (ei.mesh != null && ei.vertexData != null && ei.mesh.subMesh != null && ei.mesh.subMesh.vertexBuffer != null) {
                ei.mesh.subMesh.vertexBuffer.position(0);
                ei.mesh.subMesh.vertexBuffer.put(ei.vertexData);
                ei.mesh.subMesh.vertexBuffer.position(0);
            }
        }
        HistorialMovimientos.EstadoInicial eiRef = null;
        for (HistorialMovimientos.EstadoInicial ei : this.historial.estadoInicialGrupo) {
            if (ei.mesh != null || ei.bone != null || ei.box != null) {
                eiRef = ei;
                break;
            }
        }
        if (eiRef == null) return;
        float cx = eiRef.pos[0] + this.currentDtx;
        float cy = eiRef.pos[1] + this.currentDty;
        float cz = eiRef.pos[2] + this.currentDtz;
        boolean hayRotacion = (drx != 0f || dry != 0f || drz != 0f);
        float[] rotInc = new float[16];
        if (hayRotacion) {
            Matrix.setIdentityM(rotInc, 0);
            if (drx != 0f) Matrix.rotateM(rotInc, 0, drx, 1.0f, 0.0f, 0.0f);
            if (dry != 0f) Matrix.rotateM(rotInc, 0, dry, 0.0f, 1.0f, 0.0f);
            if (drz != 0f) Matrix.rotateM(rotInc, 0, drz, 0.0f, 0.0f, 1.0f);
        }
        for (HistorialMovimientos.EstadoInicial ei : this.historial.estadoInicialGrupo) {
            if (ei.mesh != null) {
                ei.mesh.rotation[0] = (ei.rot[0] + drx) % 360.0f;
                ei.mesh.rotation[1] = (ei.rot[1] + dry) % 360.0f;
                ei.mesh.rotation[2] = (ei.rot[2] + drz) % 360.0f;
                ei.mesh.hitbox.updateFromMesh(ei.mesh);
            } else if (ei.bone != null) {
                Bones.Bone b = ei.bone;
                boolean padreEnGrupo = false;
                if (b.parent != null) {
                    for (HistorialMovimientos.EstadoInicial ei2 : this.historial.estadoInicialGrupo) {
                        if (ei2.bone == b.parent) {
                            padreEnGrupo = true;
                            break;
                        }
                    }
                }
                if (!padreEnGrupo) {
                    float[] oldWorld = (ei.initialWorldMatrix != null) ? (float[]) ei.initialWorldMatrix.clone() : (float[]) b.worldMatrix.clone();
                    oldWorld[12] += this.currentDtx;
                    oldWorld[13] += this.currentDty;
                    oldWorld[14] += this.currentDtz;
                    float[] newWorld = new float[16];
                    oldWorld[12] -= cx;
                    oldWorld[13] -= cy;
                    oldWorld[14] -= cz;
                    if (hayRotacion) {
                        Matrix.multiplyMM(newWorld, 0, rotInc, 0, oldWorld, 0);
                    } else {
                        System.arraycopy(oldWorld, 0, newWorld, 0, 16);
                    }
                    newWorld[12] += cx;
                    newWorld[13] += cy;
                    newWorld[14] += cz;
                    float[] localMat = new float[16];
                    if (b.parent != null) {
                        float[] parentMat = (float[]) b.parent.worldMatrix.clone();
                        Bones.normalizeMatrix(parentMat);
                        float[] invParent = new float[16];
                        Matrix.invertM(invParent, 0, parentMat, 0);
                        Matrix.multiplyMM(localMat, 0, invParent, 0, newWorld, 0);
                    } else {
                        System.arraycopy(newWorld, 0, localMat, 0, 16);
                    }
                    b.position[0] = localMat[12];
                    b.position[1] = localMat[13];
                    b.position[2] = localMat[14];
                    if (hayRotacion) {
                        Bones.normalizeMatrix(localMat);
                        double sinY = Math.max(-1.0, Math.min(1.0, localMat[8]));
                        b.rotation[1] = (float) Math.toDegrees(Math.asin(sinY));
                        b.rotation[0] = (float) Math.toDegrees(Math.atan2(-localMat[9], localMat[10]));
                        b.rotation[2] = (float) Math.toDegrees(Math.atan2(-localMat[4], localMat[0]));
                        for (int i = 0; i < 3; i++) {
                            if (b.rotation[i] < 0.0f) b.rotation[i] += 360.0f;
                            b.rotation[i] = b.rotation[i] % 360.0f;
                        }
                    }
                    b.updateMatrixRecursive();
                    if (this.activity != null && this.activity.huesos == 0 && this.historial.estadoInicialGrupo.size() == 1) {
                        java.util.HashSet<String> procesados = new java.util.HashSet<>();
                        if (drx != 0f) aplicarRotacionRecursiva(b, drx, 1f, 0f, 0f, b.headWorld[0], b.headWorld[1], b.headWorld[2], procesados);
                        if (dry != 0f) aplicarRotacionRecursiva(b, dry, 0f, 1f, 0f, b.headWorld[0], b.headWorld[1], b.headWorld[2], procesados);
                        if (drz != 0f) aplicarRotacionRecursiva(b, drz, 0f, 0f, 1f, b.headWorld[0], b.headWorld[1], b.headWorld[2], procesados);
                    }
                }
            } else if (ei.box != null) {
                ei.box.rotation[0] = (ei.rot[0] + drx) % 360.0f;
                ei.box.rotation[1] = (ei.rot[1] + dry) % 360.0f;
                ei.box.rotation[2] = (ei.rot[2] + drz) % 360.0f;
            }
        }
        actualizarCentroGizmo();
    }

    public void aplicarEscalaDesdeUI(float dsx, float dsy, float dsz) {
        if (this.historial.estadoInicialGrupo == null || this.historial.estadoInicialGrupo.isEmpty()) return;
        HistorialMovimientos.EstadoInicial eiRef = null;
        for (HistorialMovimientos.EstadoInicial ei : this.historial.estadoInicialGrupo) {
            if (ei.mesh != null || ei.bone != null || ei.box != null) {
                eiRef = ei;
                break;
            }
        }
        if (eiRef == null) return;
        float cx = eiRef.pos[0] + this.currentDtx;
        float cy = eiRef.pos[1] + this.currentDty;
        float cz = eiRef.pos[2] + this.currentDtz;
        for (HistorialMovimientos.EstadoInicial ei : this.historial.estadoInicialGrupo) {
            if (ei.mesh != null) {
                ei.mesh.scale[0] = ei.scale[0] * dsx;
                ei.mesh.scale[1] = ei.scale[1] * dsy;
                ei.mesh.scale[2] = ei.scale[2] * dsz;
                for (int i = 0; i < 3; i++) if (ei.mesh.scale[i] < 0.01f) ei.mesh.scale[i] = 0.01f;
                ei.mesh.hitbox.updateFromMesh(ei.mesh);
            } else if (ei.bone != null) {
                Bones.Bone b = ei.bone;
                b.scale[0] = ei.scale[0] * dsx;
                b.scale[1] = ei.scale[1] * dsy;
                b.scale[2] = ei.scale[2] * dsz;
                for (int i = 0; i < 3; i++) if (b.scale[i] < 0.01f) b.scale[i] = 0.01f;
                boolean padreEnGrupo = false;
                if (b.parent != null) {
                    for (HistorialMovimientos.EstadoInicial ei2 : this.historial.estadoInicialGrupo) {
                        if (ei2.bone == b.parent) {
                            padreEnGrupo = true;
                            break;
                        }
                    }
                }
                if (!padreEnGrupo) {
                    float[] wPos = {ei.initialWorldMatrix != null ? ei.initialWorldMatrix[12] + this.currentDtx : b.worldMatrix[12], 
                                    ei.initialWorldMatrix != null ? ei.initialWorldMatrix[13] + this.currentDty : b.worldMatrix[13], 
                                    ei.initialWorldMatrix != null ? ei.initialWorldMatrix[14] + this.currentDtz : b.worldMatrix[14], 1.0f};
                    float nwx = ((wPos[0] - cx) * dsx) + cx;
                    float nwy = ((wPos[1] - cy) * dsy) + cy;
                    float nwz = ((wPos[2] - cz) * dsz) + cz;
                    if (b.parent != null) {
                        float[] pMat = (float[]) b.parent.worldMatrix.clone();
                        Bones.normalizeMatrix(pMat);
                        float[] invP = new float[16];
                        Matrix.invertM(invP, 0, pMat, 0);
                        float[] nWVec = {nwx, nwy, nwz, 1.0f};
                        float[] nLVec = new float[4];
                        Matrix.multiplyMV(nLVec, 0, invP, 0, nWVec, 0);
                        b.position[0] = nLVec[0];
                        b.position[1] = nLVec[1];
                        b.position[2] = nLVec[2];
                    } else {
                        b.position[0] = nwx;
                        b.position[1] = nwy;
                        b.position[2] = nwz;
                    }
                }
                b.updateMatrixRecursive();
            } else if (ei.box != null) {
                ei.box.scale = ei.scaleBox * dsx;
            }
        } actualizarCentroGizmo();
    }

}
