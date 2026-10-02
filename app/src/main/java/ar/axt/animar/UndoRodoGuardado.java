package ar.axt.animar;

import android.content.Context;
import android.util.Log;
import ar.axt.animar.Bones;
import ar.axt.animar.HistorialMovimientos;
import ar.axt.database.AdministrarDatos;
import ar.axt.materiales.Material;
import ar.axt.nopeby.MainActivity;
import ar.axt.nopeby.MyRenderer;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.List;

public class UndoRodoGuardado {
    private static final String TAG = "UndoRodoGuardado";
    private static final String ARCHIVO_HISTORIAL = "historial_estado.bin";
    private static final String HEADER_MAGIC = "AXTUNDORODO";
    private static final int VERSION = 4;
    private final Context context;

    public UndoRodoGuardado(Context context) {
        this.context = context;
    }

    private File getArchivoHistorial(String nombreProyecto) {
        File dir = AdministrarDatos.getUndoRedoDir(nombreProyecto);
        return new File(dir, ARCHIVO_HISTORIAL);
    }

    public void guardar(HistorialMovimientos historialMov, String nombreProyecto, String nombreEscena) {
        if (historialMov == null || nombreProyecto == null) return;
        File file = getArchivoHistorial(nombreProyecto);
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
            out.writeUTF(HEADER_MAGIC);
            out.writeInt(VERSION);
            out.writeUTF(nombreProyecto);
            out.writeUTF(nombreEscena != null ? nombreEscena : "0");
            out.writeInt(historialMov.indiceActual);
            out.writeInt(historialMov.indiceActualDos);
            guardarListaMovimientos(out, historialMov.historial);
            guardarListaMovimientos(out, historialMov.historialDos);
            out.flush();
            Log.d(TAG, "Historial guardado exitosamente en: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error al guardar historial", e);
        }
    }

    private void guardarListaMovimientos(DataOutputStream out, List<HistorialMovimientos.Movimiento> lista) throws IOException {
        if (lista == null) { out.writeInt(0); return; }
        out.writeInt(lista.size());
        for (HistorialMovimientos.Movimiento mov : lista) {
            out.writeUTF((mov.tipo != null ? mov.tipo : HistorialMovimientos.Movimiento.TipoAccion.TRANSFORMACION).name());
            out.writeInt(mov.meshesAfectados.size());
            for (MyRenderer.Mesh m : mov.meshesAfectados) {
                out.writeUTF(m != null && m.name != null ? m.name : "");
            }
            out.writeInt(mov.bonesAfectados.size());
            for (Bones.Bone b : mov.bonesAfectados) {
                out.writeUTF(b != null && b.id != null ? b.id : "");
            }
            out.writeInt(mov.boxesAfectadas.size());
            for (Material.EffectBox box : mov.boxesAfectadas) {
                out.writeUTF(box != null && box.id != null ? box.id : "");
            }
            guardarEstadosMesh(out, mov.antesMesh);
            guardarEstadosMesh(out, mov.despuesMesh);
            guardarEstadosBone(out, mov.antesBones);
            guardarEstadosBone(out, mov.despuesBones);
            guardarEstadosBox(out, mov.antesBoxes);
            guardarEstadosBox(out, mov.despuesBoxes);
        }
    }

    private void guardarEstadosMesh(DataOutputStream out, List<HistorialMovimientos.Movimiento.EstadoMesh> lista) throws IOException {
        if (lista == null) { out.writeInt(0); return; }
        out.writeInt(lista.size());
        for (HistorialMovimientos.Movimiento.EstadoMesh est : lista) {
            out.writeUTF((est.mesh == null || est.mesh.name == null) ? "" : est.mesh.name);
            out.writeFloat(est.pos != null ? est.pos[0] : 0.0f);
            out.writeFloat(est.pos != null ? est.pos[1] : 0.0f);
            out.writeFloat(est.pos != null ? est.pos[2] : 0.0f);
            out.writeFloat(est.rot != null ? est.rot[0] : 0.0f);
            out.writeFloat(est.rot != null ? est.rot[1] : 0.0f);
            out.writeFloat(est.rot != null ? est.rot[2] : 0.0f);
            out.writeFloat(est.scale != null ? est.scale[0] : 1.0f);
            out.writeFloat(est.scale != null ? est.scale[1] : 1.0f);
            out.writeFloat(est.scale != null ? est.scale[2] : 1.0f);
            if (est.vertexData != null) {
                out.writeInt(est.vertexData.length);
                for (float v : est.vertexData) {
                    out.writeFloat(v);
                }
            } else { out.writeInt(0); }
        }
    }

    private void guardarEstadosBone(DataOutputStream out, List<HistorialMovimientos.Movimiento.EstadoBone> lista) throws IOException {
        if (lista == null) { out.writeInt(0); return; }
        out.writeInt(lista.size());
        for (HistorialMovimientos.Movimiento.EstadoBone est : lista) {
            out.writeUTF((est.bone == null || est.bone.id == null) ? "" : est.bone.id);
            out.writeFloat(est.pos != null ? est.pos[0] : 0.0f);
            out.writeFloat(est.pos != null ? est.pos[1] : 0.0f);
            out.writeFloat(est.pos != null ? est.pos[2] : 0.0f);
            out.writeFloat(est.rot != null ? est.rot[0] : 0.0f);
            out.writeFloat(est.rot != null ? est.rot[1] : 0.0f);
            out.writeFloat(est.rot != null ? est.rot[2] : 0.0f);
            out.writeFloat(est.scale != null ? est.scale[0] : 1.0f);
            out.writeFloat(est.scale != null ? est.scale[1] : 1.0f);
            out.writeFloat(est.scale != null ? est.scale[2] : 1.0f);
        }
    }

    private void guardarEstadosBox(DataOutputStream out, List<HistorialMovimientos.Movimiento.EstadoBox> lista) throws IOException {
        if (lista == null) { out.writeInt(0); return; }
        out.writeInt(lista.size());
        for (HistorialMovimientos.Movimiento.EstadoBox est : lista) {
            out.writeUTF((est.box == null || est.box.id == null) ? "" : est.box.id);
            out.writeFloat(est.pos != null ? est.pos[0] : 0.0f);
            out.writeFloat(est.pos != null ? est.pos[1] : 0.0f);
            out.writeFloat(est.pos != null ? est.pos[2] : 0.0f);
            out.writeFloat(est.rot != null ? est.rot[0] : 0.0f);
            out.writeFloat(est.rot != null ? est.rot[1] : 0.0f);
            out.writeFloat(est.rot != null ? est.rot[2] : 0.0f);
            out.writeFloat(est.scale);
        }
    }

    public void reconstruir(final MainActivity mainActivity, final MyRenderer renderer, String nombreProyecto) {
        if (nombreProyecto == null) return;
        File file = getArchivoHistorial(nombreProyecto);
        if (!file.exists()) return;
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            String magic = in.readUTF();
            if (!HEADER_MAGIC.equals(magic)) return;
            int version = in.readInt();
            if (version != VERSION) return;
            final String nP = in.readUTF();
            final String nE = in.readUTF();
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Reconstruyendo para: " + nP + " / " + nE);
                } });
            final HistorialMovimientos historialMov = renderer.historialMovimientos;
            if (historialMov == null) return;
            int idx1 = in.readInt();
            int idx2 = in.readInt();
            historialMov.historial.clear();
            cargarListaMovimientos(in, historialMov.historial, renderer);
            historialMov.historialDos.clear();
            cargarListaMovimientos(in, historialMov.historialDos, renderer);
            historialMov.indiceActual = Math.min(idx1, historialMov.historial.size() - 1);
            historialMov.indiceActualDos = Math.min(idx2, historialMov.historialDos.size() - 1);
            sincronizarEscenaActual(historialMov, renderer);
            Log.d(TAG, "Historial reconstruido exitosamente.");
        } catch (Exception e) {
            Log.e(TAG, "Error al reconstruir historial", e);
        }
    }

    private void cargarListaMovimientos(DataInputStream in, List<HistorialMovimientos.Movimiento> lista, MyRenderer renderer) throws IOException {
        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            HistorialMovimientos.Movimiento mov = new HistorialMovimientos.Movimiento();
            mov.tipo = HistorialMovimientos.Movimiento.TipoAccion.valueOf(in.readUTF());
            int meshCount = in.readInt();
            for (int j = 0; j < meshCount; j++) {
                String mName = in.readUTF();
                if (!mName.isEmpty() && renderer.rayosInteraccion != null) {
                    MyRenderer.Mesh m = renderer.rayosInteraccion.obtenerMeshPorNombre(mName);
                    if (m != null) mov.meshesAfectados.add(m);
                }
            }
            int boneCount = in.readInt();
            for (int j = 0; j < boneCount; j++) {
                String bId = in.readUTF();
                if (!bId.isEmpty() && renderer.getBones() != null) {
                    Bones.Bone b = obtenerBonePorId(renderer.getBones(), bId);
                    if (b != null) mov.bonesAfectados.add(b);
                }
            }
            int boxCount = in.readInt();
            for (int j = 0; j < boxCount; j++) {
                String boxId = in.readUTF();
                if (!boxId.isEmpty() && renderer.material != null) {
                    Material.EffectBox box = obtenerBoxPorId(renderer.material, boxId);
                    if (box != null) mov.boxesAfectadas.add(box);
                }
            }
            cargarEstadosMesh(in, mov.antesMesh, renderer);
            cargarEstadosMesh(in, mov.despuesMesh, renderer);
            cargarEstadosBone(in, mov.antesBones, renderer);
            cargarEstadosBone(in, mov.despuesBones, renderer);
            cargarEstadosBox(in, mov.antesBoxes, renderer);
            cargarEstadosBox(in, mov.despuesBoxes, renderer);
            lista.add(mov);
        }
    }

    private void cargarEstadosMesh(DataInputStream in, List<HistorialMovimientos.Movimiento.EstadoMesh> lista, MyRenderer renderer) throws IOException {
        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            String mName = in.readUTF();
            MyRenderer.Mesh mesh = (renderer.rayosInteraccion != null) ? renderer.rayosInteraccion.obtenerMeshPorNombre(mName) : null;
            float[] pos = {in.readFloat(), in.readFloat(), in.readFloat()};
            float[] rot = {in.readFloat(), in.readFloat(), in.readFloat()};
            float[] scale = {in.readFloat(), in.readFloat(), in.readFloat()};
            int vSize = in.readInt();
            float[] vData = null;
            if (vSize > 0) {
                vData = new float[vSize];
                for (int j = 0; j < vSize; j++) {
                    vData[j] = in.readFloat();
                }
            } lista.add(new HistorialMovimientos.Movimiento.EstadoMesh(mesh, pos, rot, scale, vData));
        }
    }

    private void cargarEstadosBone(DataInputStream in, List<HistorialMovimientos.Movimiento.EstadoBone> lista, MyRenderer renderer) throws IOException {
        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            String bId = in.readUTF();
            Bones.Bone bone = (renderer.getBones() != null) ? obtenerBonePorId(renderer.getBones(), bId) : null;
            float[] pos = {in.readFloat(), in.readFloat(), in.readFloat()};
            float[] rot = {in.readFloat(), in.readFloat(), in.readFloat()};
            float[] scale = {in.readFloat(), in.readFloat(), in.readFloat()};
            lista.add(new HistorialMovimientos.Movimiento.EstadoBone(bone, pos, rot, scale));
        }
    }

    private void cargarEstadosBox(DataInputStream in, List<HistorialMovimientos.Movimiento.EstadoBox> lista, MyRenderer renderer) throws IOException {
        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            String boxId = in.readUTF();
            Material.EffectBox box = (renderer.material != null) ? obtenerBoxPorId(renderer.material, boxId) : null;
            float[] pos = {in.readFloat(), in.readFloat(), in.readFloat()};
            float[] rot = {in.readFloat(), in.readFloat(), in.readFloat()};
            float scale = in.readFloat();
            lista.add(new HistorialMovimientos.Movimiento.EstadoBox(box, pos, rot, scale));
        }
    }

    private Bones.Bone obtenerBonePorId(Bones bones, String id) {
        if (bones == null || id == null || id.isEmpty()) return null;
        for (Bones.Bone b : bones.getAllBones()) {
            if (id.equals(b.id)) return b;
        } return null;
    }

    private Material.EffectBox obtenerBoxPorId(Material material, String id) {
        if (material == null || id == null || id.isEmpty()) return null;
        for (Material.EffectBox box : material.getBoxes()) {
            if (id.equals(box.id)) return box;
        } return null;
    }

    private void sincronizarEscenaActual(HistorialMovimientos historialMov, MyRenderer renderer) {
        if (historialMov.indiceActual >= 0 && historialMov.indiceActual < historialMov.historial.size()) {
            HistorialMovimientos.Movimiento mov = historialMov.historial.get(historialMov.indiceActual);
            aplicarEstadosDespues(mov);}
        if (historialMov.indiceActualDos >= 0 && historialMov.indiceActualDos < historialMov.historialDos.size()) {
            HistorialMovimientos.Movimiento mov = historialMov.historialDos.get(historialMov.indiceActualDos);
            aplicarEstadosDespues(mov); }
        if (renderer.interaccionGismo != null) {
            renderer.interaccionGismo.actualizarCentroGizmo(); }
    }

    private void aplicarEstadosDespues(HistorialMovimientos.Movimiento mov) {
        if (mov == null) return;
        if (mov.despuesMesh != null) {
            for (HistorialMovimientos.Movimiento.EstadoMesh est : mov.despuesMesh) {
                if (est.mesh != null) {
                    System.arraycopy(est.pos, 0, est.mesh.translation, 0, 3);
                    System.arraycopy(est.rot, 0, est.mesh.rotation, 0, 3);
                    System.arraycopy(est.scale, 0, est.mesh.scale, 0, 3);
                    if (est.vertexData != null && est.mesh.subMesh != null && est.mesh.subMesh.vertexBuffer != null) {
                        FloatBuffer vb = est.mesh.subMesh.vertexBuffer;
                        vb.position(0);
                        vb.put(est.vertexData);
                        vb.position(0);
                    }
                }
            }
        }
        if (mov.despuesBones != null) {
            for (HistorialMovimientos.Movimiento.EstadoBone est : mov.despuesBones) {
                if (est.bone != null) {
                    System.arraycopy(est.pos, 0, est.bone.position, 0, 3);
                    System.arraycopy(est.rot, 0, est.bone.rotation, 0, 3);
                    System.arraycopy(est.scale, 0, est.bone.scale, 0, 3);
                }
            }
        }
        if (mov.despuesBoxes != null) {
            for (HistorialMovimientos.Movimiento.EstadoBox est : mov.despuesBoxes) {
                if (est.box != null) {
                    System.arraycopy(est.pos, 0, est.box.position, 0, 3);
                    System.arraycopy(est.rot, 0, est.box.rotation, 0, 3);
                    est.box.scale = est.scale;
                }
            }
        }
    }

    public void borrarHistorialGuardado(String nombreProyecto) {
        if (nombreProyecto == null) return;
        File file = getArchivoHistorial(nombreProyecto);
        if (file.exists()) file.delete();
    }
}
