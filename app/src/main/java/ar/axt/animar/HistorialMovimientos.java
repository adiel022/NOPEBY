package ar.axt.animar;

import ar.axt.animar.Bones;
import ar.axt.controles.AdministradorCamaras;
import ar.axt.materiales.Material;
import ar.axt.nopeby.MainActivity;
import ar.axt.nopeby.MyRenderer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HistorialMovimientos {
    private MainActivity activity;
    private MyRenderer renderer;
    public int indiceActual = -1;
    public final List<Movimiento> historial = new ArrayList();
    public int indiceActualDos = -1;
    public final List<Movimiento> historialDos = new ArrayList();
    public final List<EstadoInicial> estadoInicialGrupo = new ArrayList();

    public static class EstadoInicial {
        public Bones.Bone bone;
        public Material.EffectBox box;
        public AdministradorCamaras.MarcadorCamara cameraMarker;
        public float[] initialWorldMatrix;
        public MyRenderer.Mesh mesh;
        public float[] pos;
        public float[] rot;
        public float[] scale;
        public float scaleBox;
        public float[] vertexData;
    }

    public HistorialMovimientos(MainActivity activity, MyRenderer renderer) {
        this.activity = activity;
        this.renderer = renderer;
    }

    public HistorialMovimientos() {}

    public static class Movimiento {
        public TipoAccion tipo = TipoAccion.TRANSFORMACION;
        public List<MyRenderer.Mesh> meshesAfectados = new ArrayList();
        public List<Bones.Bone> bonesAfectados = new ArrayList();
        public List<Material.EffectBox> boxesAfectadas = new ArrayList();
        public List<EstadoMesh> antesMesh = new ArrayList();
        public List<EstadoMesh> despuesMesh = new ArrayList();
        public List<EstadoBone> antesBones = new ArrayList();
        public List<EstadoBone> despuesBones = new ArrayList();
        public List<EstadoBox> antesBoxes = new ArrayList();
        public List<EstadoBox> despuesBoxes = new ArrayList();
        public List<EstadoCamera> antesCamera = new ArrayList();
        public List<EstadoCamera> despuesCamera = new ArrayList();

        public enum TipoAccion { TRANSFORMACION,  CREACION,  ELIMINACION }

        public static class EstadoMesh {
            public MyRenderer.Mesh mesh;
            public float[] pos;
            public float[] rot;
            public float[] scale;
            public float[] vertexData;
            public EstadoMesh(MyRenderer.Mesh mesh, float[] pos, float[] rot, float[] scale, float[] vertexData) {
                this.mesh = mesh;
                this.pos = (float[]) pos.clone();
                this.rot = (float[]) rot.clone();
                this.scale = (float[]) scale.clone();
                this.vertexData = vertexData != null ? (float[]) vertexData.clone() : null;
            }
        }

        public static class EstadoBone {
            public Bones.Bone bone;
            public float[] pos;
            public float[] rot;
            public float[] scale;
            public EstadoBone(Bones.Bone bone, float[] pos, float[] rot, float[] scale) {
                this.bone = bone;
                this.pos = (float[]) pos.clone();
                this.rot = (float[]) rot.clone();
                this.scale = (float[]) scale.clone();
            }
        }

        public static class EstadoBox {
            public Material.EffectBox box;
            public float[] pos;
            public float[] rot;
            public float scale;
            public EstadoBox(Material.EffectBox box, float[] pos, float[] rot, float scale) {
                this.box = box;
                this.pos = (float[]) pos.clone();
                this.rot = (float[]) rot.clone();
                this.scale = scale;
            }
        }

        public static class EstadoCamera {
            public AdministradorCamaras.MarcadorCamara marker;
            public float[] pos;
            public float[] rot;
            public float scale;
            public EstadoCamera(AdministradorCamaras.MarcadorCamara marker, float[] pos, float[] rot, float scale) {
                this.marker = marker;
                this.pos = (float[]) pos.clone();
                this.rot = (float[]) rot.clone();
                this.scale = scale;
            }
        }
    }

    private void aplicarEstados(List<Movimiento.EstadoMesh> meshes, List<Movimiento.EstadoBone> bones, List<Movimiento.EstadoBox> boxes, List<Movimiento.EstadoCamera> cameras) {
        for (Movimiento.EstadoMesh est : meshes) {
            MyRenderer.Mesh m = est.mesh;
            m.translation[0] = est.pos[0];
            m.translation[1] = est.pos[1];
            m.translation[2] = est.pos[2];
            m.rotation[0] = est.rot[0];
            m.rotation[1] = est.rot[1];
            m.rotation[2] = est.rot[2];
            m.scale[0] = est.scale[0];
            m.scale[1] = est.scale[1];
            m.scale[2] = est.scale[2];
            if (est.vertexData != null && m.subMesh != null && m.subMesh.vertexBuffer != null) {
                m.subMesh.vertexBuffer.position(0);
                m.subMesh.vertexBuffer.put(est.vertexData);
                m.subMesh.vertexBuffer.position(0);
            } m.hitbox.updateFromMesh(m);
        }
        for (Movimiento.EstadoBone est2 : bones) {
            Bones.Bone b = est2.bone;
            b.position[0] = est2.pos[0];
            b.position[1] = est2.pos[1];
            b.position[2] = est2.pos[2];
            b.rotation[0] = est2.rot[0];
            b.rotation[1] = est2.rot[1];
            b.rotation[2] = est2.rot[2];
            b.scale[0] = est2.scale[0];
            b.scale[1] = est2.scale[1];
            b.scale[2] = est2.scale[2];
            Bones.Bone root = b;
            while (root.parent != null) {
                root = root.parent;
            } root.updateMatrixRecursive();
        }
        for (Movimiento.EstadoBox est3 : boxes) {
            Material.EffectBox b2 = est3.box;
            b2.position[0] = est3.pos[0];
            b2.position[1] = est3.pos[1];
            b2.position[2] = est3.pos[2];
            b2.rotation[0] = est3.rot[0];
            b2.rotation[1] = est3.rot[1];
            b2.rotation[2] = est3.rot[2];
            b2.scale = est3.scale;
        }
        for (Movimiento.EstadoCamera est4 : cameras) {
            AdministradorCamaras.MarcadorCamara m = est4.marker;
            m.position[0] = est4.pos[0];
            m.position[1] = est4.pos[1];
            m.position[2] = est4.pos[2];
            m.rotation[0] = est4.rot[0];
            m.rotation[1] = est4.rot[1];
            m.rotation[2] = est4.rot[2];
            m.scale = est4.scale;
            m.actualizarHitboxes();
        }
    }

    private void refrescarRender() {
        if (this.activity != null) {
            this.activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.glSurfaceView.requestRender();
                    if (renderer.interaccionGismo != null) {
                        renderer.interaccionGismo.actualizarCentroGizmo();
                    }
                    if (activity.getFragmentManager() != null) {
                        android.app.Fragment f = activity.getFragmentManager().findFragmentById(ar.axt.nopeby.R.id.f1);
                        if (f instanceof ar.axt.nopeby.EditarObjetos) {
                            ((ar.axt.nopeby.EditarObjetos) f).actualizarCamposPublico();
                        }
                    }
                }
            });
        }
    }

    public void iniciarMovimientoAgrupado() {
        borrarMovimientosAdelante();
    }

    public void finalizarMovimientoAgrupado() {
        if (this.estadoInicialGrupo.isEmpty()) { return; }
        boolean enModoHuesos = this.activity != null && this.activity.huesos == 1;
        Movimiento mov = new Movimiento();
        boolean hayCambioMesh = false;
        boolean hayCambioBone = false;
        boolean hayCambioBox = false;
        for (EstadoInicial ei : this.estadoInicialGrupo) {
            if (ei.mesh != null) {
                mov.antesMesh.add(new Movimiento.EstadoMesh(ei.mesh, ei.pos, ei.rot, ei.scale, ei.vertexData));
                MyRenderer.Mesh m = ei.mesh;
                m.hitbox.updateFromMesh(m);
                float[] vDataFinal = null;
                if (m.subMesh != null && m.subMesh.vertexBuffer != null) {
                    vDataFinal = new float[m.subMesh.vertexBuffer.capacity()];
                    m.subMesh.vertexBuffer.position(0);
                    m.subMesh.vertexBuffer.get(vDataFinal);
                    m.subMesh.vertexBuffer.position(0);
                }
                Movimiento.EstadoMesh despues = new Movimiento.EstadoMesh(m, (float[]) m.translation.clone(), (float[]) m.rotation.clone(), (float[]) m.scale.clone(), vDataFinal);
                mov.despuesMesh.add(despues);
                Movimiento.EstadoMesh antes = mov.antesMesh.get(mov.antesMesh.size() - 1);
                if (!Arrays.equals(antes.pos, despues.pos) || !Arrays.equals(antes.rot, despues.rot) || !Arrays.equals(antes.scale, despues.scale)) {
                    hayCambioMesh = true;
                }
            } else if (ei.bone != null) {
                mov.antesBones.add(new Movimiento.EstadoBone(ei.bone, ei.pos, ei.rot, ei.scale));
                Bones.Bone b = ei.bone;
                Movimiento.EstadoBone despuesB = new Movimiento.EstadoBone(b, (float[]) b.position.clone(), (float[]) b.rotation.clone(), (float[]) b.scale.clone());
                mov.despuesBones.add(despuesB);
                Movimiento.EstadoBone antesB = mov.antesBones.get(mov.antesBones.size() - 1);
                if (!Arrays.equals(antesB.pos, despuesB.pos) || !Arrays.equals(antesB.rot, despuesB.rot) || !Arrays.equals(antesB.scale, despuesB.scale)) {
                    hayCambioBone = true;
                }
            } else if (ei.box != null) {
                mov.antesBoxes.add(new Movimiento.EstadoBox(ei.box, ei.pos, ei.rot, ei.scaleBox));
                Material.EffectBox b2 = ei.box;
                Movimiento.EstadoBox despuesBox = new Movimiento.EstadoBox(b2, (float[]) b2.position.clone(), (float[]) b2.rotation.clone(), b2.scale);
                mov.despuesBoxes.add(despuesBox);
                Movimiento.EstadoBox antesBox = mov.antesBoxes.get(mov.antesBoxes.size() - 1);
                if (!Arrays.equals(antesBox.pos, despuesBox.pos) || !Arrays.equals(antesBox.rot, despuesBox.rot) || antesBox.scale != despuesBox.scale) {
                    hayCambioBox = true;
                }
            } else if (ei.cameraMarker != null) {
                mov.antesCamera.add(new Movimiento.EstadoCamera(ei.cameraMarker, ei.pos, ei.rot, ei.scaleBox));
                AdministradorCamaras.MarcadorCamara m = ei.cameraMarker;
                Movimiento.EstadoCamera despuesCam = new Movimiento.EstadoCamera(m, (float[]) m.position.clone(), (float[]) m.rotation.clone(), m.scale);
                mov.despuesCamera.add(despuesCam);
                Movimiento.EstadoCamera antesCam = mov.antesCamera.get(mov.antesCamera.size() - 1);
                if (!Arrays.equals(antesCam.pos, despuesCam.pos) || !Arrays.equals(antesCam.rot, despuesCam.rot) || antesCam.scale != despuesCam.scale) {
                    hayCambioBox = true; // reusing hayCambioBox for simplicity or add hayCambioCam
                }
            }
        }
        if (hayCambioMesh || hayCambioBone || hayCambioBox) {
            if (enModoHuesos && hayCambioBone) {
                this.historialDos.add(mov);
                this.indiceActualDos++;
            } else {
                this.historial.add(mov);
                this.indiceActual++;
                incrementarTv1();
            }
        } this.estadoInicialGrupo.clear();
    }

    private void incrementarTv1() {
        if (this.activity != null) {
            this.activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String texto = activity.tv1.getText().toString();
                        int valor = Integer.parseInt(texto);
                        if (valor <= indiceActual) {
                            activity.tv1.setText(String.valueOf(indiceActual + 1));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void borrarMovimientosAdelante() {
        if (this.activity != null && this.activity.huesos == 1) {
            if (this.indiceActualDos < this.historialDos.size() - 1) {
                this.historialDos.subList(this.indiceActualDos + 1, this.historialDos.size()).clear();
            }
        } else if (this.indiceActual < this.historial.size() - 1) {
            this.historial.subList(this.indiceActual + 1, this.historial.size()).clear();
            if (this.activity != null) {
                this.activity.resetearMovimientosAdelante();
            }
        }
    }

    public void retrocederMovimientoSecundario() {
        if (this.historialDos.isEmpty()) {
            this.indiceActualDos = -1; return;
        }
        if (this.indiceActualDos >= this.historialDos.size()) {
            this.indiceActualDos = this.historialDos.size() - 1;
        }
        if (this.indiceActualDos < 0) { return; }
        Movimiento mov = this.historialDos.get(this.indiceActualDos);
        if (mov.tipo == Movimiento.TipoAccion.CREACION) {
            for (MyRenderer.Mesh m : mov.meshesAfectados) {
                this.renderer.removeMesh(m);
            }
            for (Bones.Bone b : mov.bonesAfectados) {
                remueveBone(b);
                if (this.activity != null) {
                    MainActivity mainActivity = this.activity;
                    mainActivity.cantidadDeHuesos--;
                }
            }
            for (Material.EffectBox box : mov.boxesAfectadas) {
                this.renderer.material.removeBox(box);
            }
        } else if (mov.tipo == Movimiento.TipoAccion.ELIMINACION) {
            for (MyRenderer.Mesh m2 : mov.meshesAfectados) {
                this.renderer.addMesh(m2);
            }
            for (Bones.Bone b2 : mov.bonesAfectados) {
                insertaBone(b2);
                if (this.activity != null) {
                    this.activity.cantidadDeHuesos++;
                }
            }
            for (Material.EffectBox box2 : mov.boxesAfectadas) {
                this.renderer.material.getBoxes().add(box2);
            }
        } else {
            aplicarEstados(mov.antesMesh, mov.antesBones, mov.antesBoxes, mov.antesCamera);
        }
        this.indiceActualDos--;
        refrescarRender();
    }

    public void avanzarMovimientoSecundario() {
        if (this.historialDos.isEmpty()) { this.indiceActualDos = -1; return; }
        if (this.indiceActualDos >= this.historialDos.size() - 1) { return; }
        if (this.indiceActualDos < -1) { this.indiceActualDos = -1; }
        this.indiceActualDos++;
        Movimiento mov = this.historialDos.get(this.indiceActualDos);
        if (mov.tipo == Movimiento.TipoAccion.CREACION) {
            for (MyRenderer.Mesh m : mov.meshesAfectados) {
                this.renderer.addMesh(m);
            }
            for (Bones.Bone b : mov.bonesAfectados) {
                insertaBone(b);
                if (this.activity != null) {
                    this.activity.cantidadDeHuesos++;
                }
            }
            for (Material.EffectBox box : mov.boxesAfectadas) {
                this.renderer.material.getBoxes().add(box);
            }
        } else if (mov.tipo == Movimiento.TipoAccion.ELIMINACION) {
            for (MyRenderer.Mesh m2 : mov.meshesAfectados) {
                this.renderer.removeMesh(m2);
            }
            for (Bones.Bone b2 : mov.bonesAfectados) {
                remueveBone(b2);
                if (this.activity != null) {
                    MainActivity mainActivity = this.activity;
                    mainActivity.cantidadDeHuesos--;
                }
            }
            for (Material.EffectBox box2 : mov.boxesAfectadas) {
                this.renderer.material.removeBox(box2);
            }
        } else {
            aplicarEstados(mov.despuesMesh, mov.despuesBones, mov.despuesBoxes, mov.despuesCamera);
        } refrescarRender();
    }

    public void retrocederMovimiento() {
        if (this.historial.isEmpty()) {this.indiceActual = -1; return; }
        if (this.indiceActual >= this.historial.size()) { this.indiceActual = this.historial.size() - 1; }
        if (this.indiceActual < 0) { return; }
        Movimiento mov = this.historial.get(this.indiceActual);
        if (mov.tipo == Movimiento.TipoAccion.CREACION) {
            for (MyRenderer.Mesh m : mov.meshesAfectados) {
                this.renderer.removeMesh(m);
            }
            for (Bones.Bone b : mov.bonesAfectados) {
                remueveBone(b);
                if (this.activity != null) {
                    MainActivity mainActivity = this.activity;
                    mainActivity.cantidadDeHuesos--;
                }
            }
            for (Material.EffectBox box : mov.boxesAfectadas) {
                this.renderer.material.removeBox(box);
            }
        } else if (mov.tipo == Movimiento.TipoAccion.ELIMINACION) {
            for (MyRenderer.Mesh m2 : mov.meshesAfectados) {
                this.renderer.addMesh(m2);
            }
            for (Bones.Bone b2 : mov.bonesAfectados) {
                insertaBone(b2);
                if (this.activity != null) {
                    this.activity.cantidadDeHuesos++;
                }
            }
            for (Material.EffectBox box2 : mov.boxesAfectadas) {
                this.renderer.material.getBoxes().add(box2);
            }
        } else {
            aplicarEstados(mov.antesMesh, mov.antesBones, mov.antesBoxes, mov.antesCamera);
        }
        this.indiceActual--;
        refrescarRender();
    }

    public void avanzarMovimiento() {
        if (this.historial.isEmpty()) { this.indiceActual = -1; return; }
        if (this.indiceActual >= this.historial.size() - 1) { return; }
        if (this.indiceActual < -1) { this.indiceActual = -1; }
        this.indiceActual++;
        Movimiento mov = this.historial.get(this.indiceActual);
        if (mov.tipo == Movimiento.TipoAccion.CREACION) {
            for (MyRenderer.Mesh m : mov.meshesAfectados) {
                this.renderer.addMesh(m);
            }
            for (Bones.Bone b : mov.bonesAfectados) {
                insertaBone(b);
                if (this.activity != null) {
                    this.activity.cantidadDeHuesos++;
                }
            }
            for (Material.EffectBox box : mov.boxesAfectadas) {
                this.renderer.material.getBoxes().add(box);
            }
        } else if (mov.tipo == Movimiento.TipoAccion.ELIMINACION) {
            for (MyRenderer.Mesh m2 : mov.meshesAfectados) {
                this.renderer.removeMesh(m2);
            }
            for (Bones.Bone b2 : mov.bonesAfectados) {
                remueveBone(b2);
                if (this.activity != null) {
                    MainActivity mainActivity = this.activity;
                    mainActivity.cantidadDeHuesos--;
                }
            }
            for (Material.EffectBox box2 : mov.boxesAfectadas) {
                this.renderer.material.removeBox(box2);
            }
        } else {
            aplicarEstados(mov.despuesMesh, mov.despuesBones, mov.despuesBoxes, mov.despuesCamera);
        } refrescarRender();
    }

    public void capturarEstadoMesh(MyRenderer.Mesh mesh) {
        if (mesh == null) { return; }
        EstadoInicial estado = new EstadoInicial();
        estado.mesh = mesh;
        estado.pos = (float[]) mesh.translation.clone();
        estado.rot = (float[]) mesh.rotation.clone();
        estado.scale = (float[]) mesh.scale.clone();
        this.estadoInicialGrupo.add(estado);
    }

    public void limpiarEstadoInicial() {
        this.estadoInicialGrupo.clear();
    }

    public void vaciarHistorialSecundario() {
        this.historialDos.clear();
        this.indiceActualDos = -1;
    }

    public void registrarCreacionMesh(MyRenderer.Mesh mesh) {
        iniciarMovimientoAgrupado();
        Movimiento mov = new Movimiento();
        mov.tipo = Movimiento.TipoAccion.CREACION;
        mov.meshesAfectados.add(mesh);
        guardarEnHistorial(mov);
    }

    public void registrarCreacionMeshes(List<MyRenderer.Mesh> meshes) {
        iniciarMovimientoAgrupado();
        Movimiento mov = new Movimiento();
        mov.tipo = Movimiento.TipoAccion.CREACION;
        mov.meshesAfectados.addAll(meshes);
        guardarEnHistorial(mov);
    }

    public void registrarCreacionBone(Bones.Bone bone) {
        iniciarMovimientoAgrupado();
        Movimiento mov = new Movimiento();
        mov.tipo = Movimiento.TipoAccion.CREACION;
        mov.bonesAfectados.add(bone);
        guardarEnHistorial(mov);
    }

    public void registrarCreacionBones(List<Bones.Bone> bones) {
        iniciarMovimientoAgrupado();
        Movimiento mov = new Movimiento();
        mov.tipo = Movimiento.TipoAccion.CREACION;
        mov.bonesAfectados.addAll(bones);
        guardarEnHistorial(mov);
    }

    public void registrarCreacionBox(Material.EffectBox box) {
        iniciarMovimientoAgrupado();
        Movimiento mov = new Movimiento();
        mov.tipo = Movimiento.TipoAccion.CREACION;
        mov.boxesAfectadas.add(box);
        guardarEnHistorial(mov);
    }

    public void registrarEliminacionMesh(MyRenderer.Mesh mesh) {
        iniciarMovimientoAgrupado();
        Movimiento mov = new Movimiento();
        mov.tipo = Movimiento.TipoAccion.ELIMINACION;
        mov.meshesAfectados.add(mesh);
        guardarEnHistorial(mov);
    }

    public void registrarEliminacionBone(Bones.Bone bone) {
        iniciarMovimientoAgrupado();
        Movimiento mov = new Movimiento();
        mov.tipo = Movimiento.TipoAccion.ELIMINACION;
        mov.bonesAfectados.add(bone);
        guardarEnHistorial(mov);
    }

    public void registrarEliminacionBox(Material.EffectBox box) {
        iniciarMovimientoAgrupado();
        Movimiento mov = new Movimiento();
        mov.tipo = Movimiento.TipoAccion.ELIMINACION;
        mov.boxesAfectadas.add(box);
        guardarEnHistorial(mov);
    }

    private void guardarEnHistorial(Movimiento mov) {
        if (this.activity != null && this.activity.huesos == 1) {
            if (!mov.bonesAfectados.isEmpty() || !mov.antesBones.isEmpty()) {
                this.historialDos.add(mov);
                this.indiceActualDos++;
            }
        } else {
            this.historial.add(mov);
            this.indiceActual++;
        }
    }

    private void remueveBone(Bones.Bone b) {
        this.renderer.getBones().removeBoneCompletely(b);
    }

    private void insertaBone(Bones.Bone b) {
        this.renderer.getBones().addBoneCompletely(b);
    }
}
