package ar.axt.database;

import android.content.Context;
import android.util.Log;
import ar.axt.animar.Bones;
import ar.axt.controles.AdministradorCamaras;
import ar.axt.leerobj.ObjetosCargados;
import ar.axt.materiales.Material;
import ar.axt.nopeby.MyRenderer;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReconstruirEscena {
    private static final String TAG = "ReconstruirEscena";
    private final Context context;

    public static class EscenaCompleta {
        public DatosProyectos.ShaderSettings shaderSettings;
        public ArrayList<MyRenderer.Mesh> meshes = new ArrayList<>();
        public List<Bones.Bone> bones = new ArrayList<>();
        public List<Material.EffectBox> boxes = new ArrayList<>();
        public List<AdministradorCamaras.MarcadorCamara> cameras = new ArrayList<>();
    }

    public ReconstruirEscena(Context context) {
        this.context = context;
    }

    public EscenaCompleta cargarProyecto(String nombreProyecto) {
        EscenaCompleta escena = new EscenaCompleta();
        DatosProyectos dp = new DatosProyectos(this.context);
        try { escena.shaderSettings = dp.obtenerShaderSettings(nombreProyecto);
        } finally { dp.close(); }
        File projectDir = AdministrarDatos.getProyectoDir(nombreProyecto);
        File structFile = new File(projectDir, nombreProyecto + ".txt");
        if (!structFile.exists()) return escena;
        List<String> meshNames = new ArrayList<>();
        List<String> boneIds = new ArrayList<>();
        List<String> boxIds = new ArrayList<>();
        List<String> cameraIds = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(structFile)) {
            byte[] data = new byte[(int) structFile.length()];
            fis.read(data);
            String contenido = new String(data);
            String[] lineas = contenido.split("\n");
            String seccionActual = "";
            for (String linea : lineas) {
                String l = linea.trim();
                if (l.isEmpty()) continue;
                if (l.startsWith("[")) {
                    seccionActual = l.replace("[", "").replace("]", "");
                } else {
                    if ("MESHES".equals(seccionActual)) meshNames.add(l);
                    else if ("BONES".equals(seccionActual)) boneIds.add(l);
                    else if ("BOXES".equals(seccionActual)) boxIds.add(l);
                    else if ("CAMERAS".equals(seccionActual)) cameraIds.add(l);
                }
            }
        } catch (IOException e) { Log.e(TAG, "Error leyendo estructura del proyecto", e); }
        for (String name : meshNames) {
            MyRenderer.Mesh m = cargarMesh(nombreProyecto, name);
            if (m != null) escena.meshes.add(m);
        }
        Map<String, String> parentRelations = new HashMap<>();
        Map<String, Bones.Bone> boneMap = new HashMap<>();
        for (String id : boneIds) {
            RawBoneData rbd = cargarBoneBin(nombreProyecto, id, escena.meshes);
            if (rbd != null && rbd.bone != null) {
                escena.bones.add(rbd.bone);
                boneMap.put(rbd.bone.id, rbd.bone);
                if (rbd.parentId != null && !rbd.parentId.isEmpty()) {
                    parentRelations.put(rbd.bone.id, rbd.parentId);
                }
            }
        }
        for (String id : boxIds) {
            Material.EffectBox box = cargarCaja(nombreProyecto, id);
            if (box != null) escena.boxes.add(box);
        }
        for (String id : cameraIds) {
            AdministradorCamaras.MarcadorCamara cam = cargarCamara(nombreProyecto, id);
            if (cam != null) escena.cameras.add(cam);
        }
        for (Bones.Bone bone : escena.bones) {
            String pId = parentRelations.get(bone.id);
            if (pId != null && boneMap.containsKey(pId)) {
                Bones.Bone parent = boneMap.get(pId);
                if (parent != bone) {
                    bone.parent = parent;
                    if (!parent.children.contains(bone)) parent.children.add(bone);
                }
            }
        }
        for (Bones.Bone bone : escena.bones) {if (bone.parent == null) bone.updateMatrixRecursive();}
        return escena;
    }

    private MyRenderer.Mesh cargarMesh(String nombreProyecto, String meshName) {
        File folder = AdministrarDatos.getMeshFolder(nombreProyecto, meshName);
        File file = new File(folder, meshName + ".bin");
        if (!file.exists()) return null;
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            if (!"AXTMESH".equals(in.readUTF())) return null;
            int version = in.readInt();
            String name = in.readUTF();
            ObjetosCargados.SubMesh sm = new ObjetosCargados.SubMesh();
            sm.name = name;
            sm.vertexBuffer = readFloatBuffer(in);
            sm.normalBuffer = readFloatBuffer(in);
            sm.texcoordBuffer = readFloatBuffer(in);
            sm.colorBuffer = readFloatBuffer(in);
            sm.indexBuffer = readShortBuffer(in);
            sm.numIndices = in.readInt();
            MyRenderer.Mesh mesh = new MyRenderer.Mesh(sm);
            mesh.translation[0] = in.readFloat();
            mesh.translation[1] = in.readFloat();
            mesh.translation[2] = in.readFloat();
            mesh.rotation[0] = in.readFloat();
            mesh.rotation[1] = in.readFloat();
            mesh.rotation[2] = in.readFloat();
            mesh.scale[0] = in.readFloat();
            mesh.scale[1] = in.readFloat();
            mesh.scale[2] = in.readFloat();
            if (version >= 2) {
                mesh.specularStrength = in.readFloat();
                mesh.shininess = in.readFloat();
                mesh.opacity = in.readFloat();
                mesh.lightRadiusMult = in.readFloat();
                mesh.emissiveColor = new float[]{in.readFloat(), in.readFloat(), in.readFloat()};
                sm.hasOriginalUVs = in.readBoolean();
            }
            if (in.readBoolean()) {
                sm.embeddedTextureMimeType = in.readUTF();
                int size = in.readInt();
                sm.embeddedTexture = new byte[size];
                in.readFully(sm.embeddedTexture);
            }
            if (version >= 2 && in.available() > 0) {
                if (in.readBoolean()) {
                    sm.embeddedNormalMimeType = in.readUTF();
                    int sizeN = in.readInt();
                    sm.embeddedNormalTexture = new byte[sizeN];
                    in.readFully(sm.embeddedNormalTexture);
                }
            }
            return mesh;
        } catch (IOException e) {
            Log.e(TAG, "Error cargando mesh " + meshName, e);
        } return null;
    }

    private static class RawBoneData {
        Bones.Bone bone;
        String parentId;
    }

    private RawBoneData cargarBoneBin(String nombreProyecto, String boneId, ArrayList<MyRenderer.Mesh> meshes) {
        File objetosDir = AdministrarDatos.getObjetos3DDir(nombreProyecto);
        File boneFile = buscarArchivoRecursivo(objetosDir, GuardarBones.sanitizarNombreBone(boneId) + ".bin");
        if (boneFile == null) return null;
        try (DataInputStream in = new DataInputStream(new FileInputStream(boneFile))) {
            if (!"AXTBONE".equals(in.readUTF())) return null;
            in.readInt();
            String id = in.readUTF();
            String group = in.readUTF();
            String parentId = in.readUTF();
            Bones.Bone bone = new Bones.Bone(id, group, null);
            bone.captured = in.readBoolean();
            bone.color[0] = in.readFloat();
            bone.color[1] = in.readFloat();
            bone.color[2] = in.readFloat();
            bone.color[3] = in.readFloat();
            bone.position[0] = in.readFloat();
            bone.position[1] = in.readFloat();
            bone.position[2] = in.readFloat();
            bone.rotation[0] = in.readFloat();
            bone.rotation[1] = in.readFloat();
            bone.rotation[2] = in.readFloat();
            bone.scale[0] = in.readFloat();
            bone.scale[1] = in.readFloat();
            bone.scale[2] = in.readFloat();
            if (bone.groupedVertices == null) bone.groupedVertices = new HashMap<>();
            int numVertices = in.readInt();
            for (int i = 0; i < numVertices; i++) {
                String meshName = in.readUTF();
                int index = in.readInt();
                float weight = in.readFloat();
                MyRenderer.Mesh mesh = buscarMeshPorNombre(meshes, meshName);
                if (mesh != null) {
                    Bones.CapturedVertex cv = new Bones.CapturedVertex(mesh, index, weight);
                    bone.capturedVertices.add(cv);
                    List<Bones.CapturedVertex> list = bone.groupedVertices.get(mesh);
                    if (list == null) { list = new ArrayList<>(); bone.groupedVertices.put(mesh, list);
                    } list.add(cv); }
            }
            RawBoneData rbd = new RawBoneData();
            rbd.bone = bone;
            rbd.parentId = parentId;
            return rbd;
        } catch (IOException e) { Log.e(TAG, "Error cargando bone " + boneId, e);
        } return null;
    }

    private Material.EffectBox cargarCaja(String nombreProyecto, String idCaja) {
        File file = new File(AdministrarDatos.getCajasDir(nombreProyecto), GuardarCajas.sanitizarNombreCaja(idCaja) + ".bin");
        if (!file.exists()) return null;
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            if (!"AXTBOX".equals(in.readUTF())) return null;
            int version = in.readInt();
            String id = in.readUTF();
            int type = in.readInt();
            Material.EffectBox box = new Material.EffectBox(id, type);
            box.position[0] = in.readFloat();
            box.position[1] = in.readFloat();
            box.position[2] = in.readFloat();
            box.rotation[0] = in.readFloat();
            box.rotation[1] = in.readFloat();
            box.rotation[2] = in.readFloat();
            box.scale = in.readFloat();
            if (version >= 2) box.numParticles = in.readInt();
            return box;
        } catch (IOException e) { Log.e(TAG, "Error cargando caja " + idCaja, e);
        } return null;
    }

    private AdministradorCamaras.MarcadorCamara cargarCamara(String nombreProyecto, String idCamara) {
        File file = new File(AdministrarDatos.getCamarasDir(nombreProyecto), idCamara + ".bin");
        if (!file.exists()) return null;
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            if (!"AXTCAM".equals(in.readUTF())) return null;
            in.readInt();
            String id = in.readUTF();
            float[] pos = {in.readFloat(), in.readFloat(), in.readFloat()};
            float[] rot = {in.readFloat(), in.readFloat(), in.readFloat()};
            float roll = in.readFloat();
            float scale = in.readFloat();
            AdministradorCamaras.MarcadorCamara cam = new AdministradorCamaras.MarcadorCamara(id, pos, rot, roll, null);
            cam.scale = scale;
            return cam;
        } catch (IOException e) { Log.e(TAG, "Error cargando cámara " + idCamara, e);
        } return null;
    }

    private File buscarArchivoRecursivo(File parent, String fileName) {
        File[] children = parent.listFiles();
        if (children == null) return null;
        for (File child : children) {
            if (child.isDirectory()) {
                File found = buscarArchivoRecursivo(child, fileName);
                if (found != null) return found;
            } else if (child.getName().equals(fileName)) {
                return child;
            }
        } return null;
    }

    private MyRenderer.Mesh buscarMeshPorNombre(ArrayList<MyRenderer.Mesh> meshes, String nombre) {
        for (MyRenderer.Mesh m : meshes) {
            if (nombre.equals(m.name)) return m;
        } return null;
    }

    private FloatBuffer readFloatBuffer(DataInputStream in) throws IOException {
        int size = in.readInt();
        if (size <= 0) return null;
        ByteBuffer bb = ByteBuffer.allocateDirect(size * 4).order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        for (int i = 0; i < size; i++) fb.put(in.readFloat());
        fb.position(0);
        return fb;
    }

    private ShortBuffer readShortBuffer(DataInputStream in) throws IOException {
        int size = in.readInt();
        if (size <= 0) return null;
        ByteBuffer bb = ByteBuffer.allocateDirect(size * 2).order(ByteOrder.nativeOrder());
        ShortBuffer sb = bb.asShortBuffer();
        for (int i = 0; i < size; i++) sb.put(in.readShort());
        sb.position(0);
        return sb;
    }
}
