package ar.axt.animar;

import android.content.Context;
import android.util.Log;
import ar.axt.database.AdministrarDatos;
import ar.axt.materiales.Material;
import ar.axt.nopeby.MyRenderer;
import java.io.*;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CrearAnimaciones {
    private static final String TAG = "CrearAnimaciones";
    private static final String FILE_NAME = "animacion.bin";
    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static class MeshFrameState {
        public String name;
        public float[] pos = new float[3];
        public float[] rot = new float[3];
        public float[] scale = new float[3];
        public float[] vertexData;
    }

    public static class BoxFrameState {
        public String id;
        public float[] pos = new float[3];
        public float[] rot = new float[3];
        public float scale;
    }

    public static class BoneFrameState {
        public String id;
        public float[] pos = new float[3];
        public float[] rot = new float[3];
        public float[] scale = new float[3];
    }

    public static class CameraFrameState {
        public String markerId;
        public float centerX, centerY, centerZ;
        public float angleX, angleY, angleRoll;
        public float distance;
    }

    public static class LightFrameState {
        public float[] pos = new float[3];
        public float intensity;
        public float shadowStrength;
        public float[] lightColor = new float[3];
        public float[] shadowColor = new float[3];
    }

    public static class SkyFrameState {
        public float[] skyColor = new float[3];
        public float cloudDensity;
        public float starDensity;
    }

    public static class Keyframe {
        public int segundo;
        public List<MeshFrameState> meshes = new ArrayList<>();
        public List<BoxFrameState> boxes = new ArrayList<>();
        public List<BoneFrameState> bones = new ArrayList<>();
        public CameraFrameState camera;
        public LightFrameState light;
        public SkyFrameState sky;
    }

    public CrearAnimaciones(Context context) {
        this.context = context;
    }

    private File getAnimacionDir(String nombreProyecto) {
        if (nombreProyecto == null) return new File(AdministrarDatos.getAppBaseDir(), "default/animaciones");
        return AdministrarDatos.getAnimacionesDir(nombreProyecto);
    }

    public File getAnimacionFile(String nombreProyecto) {
        return new File(getAnimacionDir(nombreProyecto), FILE_NAME);
    }

    public TreeMap<Integer, Keyframe> cargarKeyframes(String nombreProyecto) {
        TreeMap<Integer, Keyframe> keyframes = new TreeMap<>();
        File file = getAnimacionFile(nombreProyecto);
        if (!file.exists()) return keyframes;
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            if (in.available() <= 0) return keyframes;
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                Keyframe kf = new Keyframe();
                kf.segundo = in.readInt();
                int meshCount = in.readInt();
                for (int j = 0; j < meshCount; j++) {
                    MeshFrameState m = new MeshFrameState();
                    m.name = in.readUTF();
                    m.pos[0] = in.readFloat();
                    m.pos[1] = in.readFloat();
                    m.pos[2] = in.readFloat();
                    m.rot[0] = in.readFloat();
                    m.rot[1] = in.readFloat();
                    m.rot[2] = in.readFloat();
                    m.scale[0] = in.readFloat();
                    m.scale[1] = in.readFloat();
                    m.scale[2] = in.readFloat();
                    int vLen = in.readInt();
                    if (vLen > 0) {
                        m.vertexData = new float[vLen];
                        for (int v = 0; v < vLen; v++) {
                            m.vertexData[v] = in.readFloat();
                        }
                    } kf.meshes.add(m);
                }
                int boxCount = in.readInt();
                for (int j = 0; j < boxCount; j++) {
                    BoxFrameState b = new BoxFrameState();
                    b.id = in.readUTF();
                    b.pos[0] = in.readFloat();
                    b.pos[1] = in.readFloat();
                    b.pos[2] = in.readFloat();
                    b.rot[0] = in.readFloat();
                    b.rot[1] = in.readFloat();
                    b.rot[2] = in.readFloat();
                    b.scale = in.readFloat();
                    kf.boxes.add(b);
                }
                if (in.available() > 0) {
                    int boneCount = in.readInt();
                    for (int j = 0; j < boneCount; j++) {
                        BoneFrameState bn = new BoneFrameState();
                        bn.id = in.readUTF();
                        bn.pos[0] = in.readFloat();
                        bn.pos[1] = in.readFloat();
                        bn.pos[2] = in.readFloat();
                        bn.rot[0] = in.readFloat();
                        bn.rot[1] = in.readFloat();
                        bn.rot[2] = in.readFloat();
                        bn.scale[0] = in.readFloat();
                        bn.scale[1] = in.readFloat();
                        bn.scale[2] = in.readFloat();
                        kf.bones.add(bn);
                    }
                }
                if (in.available() > 0) {
                    if (in.readBoolean()) { // Tiene datos de cámara
                        CameraFrameState c = new CameraFrameState();
                        c.markerId = in.readUTF();
                        c.centerX = in.readFloat();
                        c.centerY = in.readFloat();
                        c.centerZ = in.readFloat();
                        c.angleX = in.readFloat();
                        c.angleY = in.readFloat();
                        c.angleRoll = in.readFloat();
                        c.distance = in.readFloat();
                        kf.camera = c;
                    }
                }
                if (in.available() > 0) {
                    if (in.readBoolean()) { // Tiene datos de luz
                        LightFrameState l = new LightFrameState();
                        l.pos[0] = in.readFloat();
                        l.pos[1] = in.readFloat();
                        l.pos[2] = in.readFloat();
                        l.intensity = in.readFloat();
                        l.shadowStrength = in.readFloat();
                        l.lightColor[0] = in.readFloat();
                        l.lightColor[1] = in.readFloat();
                        l.lightColor[2] = in.readFloat();
                        l.shadowColor[0] = in.readFloat();
                        l.shadowColor[1] = in.readFloat();
                        l.shadowColor[2] = in.readFloat();
                        kf.light = l;
                    }
                }
                if (in.available() > 0) {
                    if (in.readBoolean()) {
                        SkyFrameState s = new SkyFrameState();
                        s.skyColor[0] = in.readFloat();
                        s.skyColor[1] = in.readFloat();
                        s.skyColor[2] = in.readFloat();
                        s.cloudDensity = in.readFloat();
                        s.starDensity = in.readFloat();
                        kf.sky = s;
                    }
                }
                keyframes.put(kf.segundo, kf);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error al cargar keyframes", e);
        } return keyframes;
    }

    public void guardarKeyframesAsync(final String nombreProyecto, final Map<Integer, Keyframe> keyframes) {
        final Map<Integer, Keyframe> copiaKeyframes = new HashMap<>();
        synchronized (keyframes) {
            for (Map.Entry<Integer, Keyframe> entry : keyframes.entrySet()) {
                copiaKeyframes.put(entry.getKey(), entry.getValue());
            }
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                File file = getAnimacionFile(nombreProyecto);
                try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
                    out.writeInt(copiaKeyframes.size());
                    for (Keyframe kf : copiaKeyframes.values()) {
                        out.writeInt(kf.segundo);
                        out.writeInt(kf.meshes.size());
                        for (MeshFrameState m : kf.meshes) {
                            out.writeUTF(m.name != null ? m.name : "");
                            out.writeFloat(m.pos[0]);
                            out.writeFloat(m.pos[1]);
                            out.writeFloat(m.pos[2]);
                            out.writeFloat(m.rot[0]);
                            out.writeFloat(m.rot[1]);
                            out.writeFloat(m.rot[2]);
                            out.writeFloat(m.scale[0]);
                            out.writeFloat(m.scale[1]);
                            out.writeFloat(m.scale[2]);
                            if (m.vertexData != null) {
                                out.writeInt(m.vertexData.length);
                                for (float f : m.vertexData) {
                                    out.writeFloat(f);
                                }
                            } else { out.writeInt(0); }
                        }
                        out.writeInt(kf.boxes.size());
                        for (BoxFrameState b : kf.boxes) {
                            out.writeUTF(b.id != null ? b.id : "");
                            out.writeFloat(b.pos[0]);
                            out.writeFloat(b.pos[1]);
                            out.writeFloat(b.pos[2]);
                            out.writeFloat(b.rot[0]);
                            out.writeFloat(b.rot[1]);
                            out.writeFloat(b.rot[2]);
                            out.writeFloat(b.scale);
                        }
                        out.writeInt(kf.bones.size());
                        for (BoneFrameState bn : kf.bones) {
                            out.writeUTF(bn.id != null ? bn.id : "");
                            out.writeFloat(bn.pos[0]);
                            out.writeFloat(bn.pos[1]);
                            out.writeFloat(bn.pos[2]);
                            out.writeFloat(bn.rot[0]);
                            out.writeFloat(bn.rot[1]);
                            out.writeFloat(bn.rot[2]);
                            out.writeFloat(bn.scale[0]);
                            out.writeFloat(bn.scale[1]);
                            out.writeFloat(bn.scale[2]);
                        }
                        if (kf.camera != null) {
                            out.writeBoolean(true);
                            out.writeUTF(kf.camera.markerId != null ? kf.camera.markerId : "");
                            out.writeFloat(kf.camera.centerX);
                            out.writeFloat(kf.camera.centerY);
                            out.writeFloat(kf.camera.centerZ);
                            out.writeFloat(kf.camera.angleX);
                            out.writeFloat(kf.camera.angleY);
                            out.writeFloat(kf.camera.angleRoll);
                            out.writeFloat(kf.camera.distance);
                        } else { out.writeBoolean(false); }
                        if (kf.light != null) {
                            out.writeBoolean(true);
                            out.writeFloat(kf.light.pos[0]);
                            out.writeFloat(kf.light.pos[1]);
                            out.writeFloat(kf.light.pos[2]);
                            out.writeFloat(kf.light.intensity);
                            out.writeFloat(kf.light.shadowStrength);
                            out.writeFloat(kf.light.lightColor[0]);
                            out.writeFloat(kf.light.lightColor[1]);
                            out.writeFloat(kf.light.lightColor[2]);
                            out.writeFloat(kf.light.shadowColor[0]);
                            out.writeFloat(kf.light.shadowColor[1]);
                            out.writeFloat(kf.light.shadowColor[2]);
                        } else { out.writeBoolean(false); }
                        if (kf.sky != null) {
                            out.writeBoolean(true);
                            out.writeFloat(kf.sky.skyColor[0]);
                            out.writeFloat(kf.sky.skyColor[1]);
                            out.writeFloat(kf.sky.skyColor[2]);
                            out.writeFloat(kf.sky.cloudDensity);
                            out.writeFloat(kf.sky.starDensity);
                        } else { out.writeBoolean(false); }
                    }
                    out.flush();
                    Log.d(TAG, "Animación guardada correctamente en segundo plano para el proyecto: " + nombreProyecto);
                } catch (IOException e) {
                    Log.e(TAG, "Error al guardar la animación en segundo plano", e);
                }
            }
        });
    }

    public void aplicarKeyframe(Keyframe kf, MyRenderer renderer) {
        if (kf == null || renderer == null) return;
        if (kf.meshes != null && renderer.meshes != null) {
            for (MeshFrameState mState : kf.meshes) {
                if (mState == null || mState.name == null) continue;
                for (MyRenderer.Mesh mesh : renderer.meshes) {
                    if (mesh != null && mState.name.equals(mesh.name)) {
                        System.arraycopy(mState.pos, 0, mesh.translation, 0, 3);
                        System.arraycopy(mState.rot, 0, mesh.rotation, 0, 3);
                        System.arraycopy(mState.scale, 0, mesh.scale, 0, 3);
                        if (mState.vertexData != null && mesh.subMesh != null && mesh.subMesh.vertexBuffer != null) {
                            FloatBuffer vb = mesh.subMesh.vertexBuffer;
                            vb.position(0);
                            if (mState.vertexData.length <= vb.remaining()) {
                                vb.put(mState.vertexData);
                            } else {
                                vb.put(mState.vertexData, 0, vb.remaining());
                            }
                            vb.position(0);
                        }
                        if (mesh.hitbox != null) {
                            mesh.hitbox.updateFromMesh(mesh);
                        }
                        break;
                    }
                }
            }
        }
        if (kf.boxes != null && renderer.material != null && renderer.material.getBoxes() != null) {
            for (BoxFrameState bState : kf.boxes) {
                if (bState == null || bState.id == null) continue;
                for (ar.axt.materiales.Material.EffectBox box : renderer.material.getBoxes()) {
                    if (box != null && bState.id.equals(box.id)) {
                        System.arraycopy(bState.pos, 0, box.position, 0, 3);
                        System.arraycopy(bState.rot, 0, box.rotation, 0, 3);
                        box.scale = bState.scale;
                        break;
                    }
                }
            }
        }
        if (kf.bones != null && renderer.getBones() != null) {
            for (BoneFrameState bnState : kf.bones) {
                if (bnState == null || bnState.id == null) continue;
                for (Bones.Bone bone : renderer.getBones().getAllBones()) {
                    if (bone != null && bnState.id.equals(bone.id)) {
                        System.arraycopy(bnState.pos, 0, bone.position, 0, 3);
                        System.arraycopy(bnState.rot, 0, bone.rotation, 0, 3);
                        System.arraycopy(bnState.scale, 0, bone.scale, 0, 3);
                        bone.updateMatrixRecursive();
                        break;
                    }
                }
            }
        }
        if (kf.light != null) { aplicarLightState(kf.light, renderer); }
        if (kf.sky != null) { aplicarSkyState(kf.sky, renderer); }
        if (renderer.activity != null && renderer.activity.glSurfaceView != null) {
            renderer.activity.glSurfaceView.requestRender();
        }
    }

    private void aplicarSkyState(SkyFrameState ss, MyRenderer renderer) {
        if (ss == null || renderer == null) return;
        System.arraycopy(ss.skyColor, 0, renderer.skyColor, 0, 3);
        renderer.cloudDensity = ss.cloudDensity;
        renderer.starDensity = ss.starDensity;
    }

    private void aplicarCameraState(CameraFrameState cs, MyRenderer renderer) {
        if (cs == null || renderer == null || renderer.ajustesDeCamara == null) return;
        ar.axt.controles.AjustesDeCamara adj = renderer.ajustesDeCamara;
        adj.centerX = cs.centerX;
        adj.centerY = cs.centerY;
        adj.centerZ = cs.centerZ;
        adj.angleX = cs.angleX;
        adj.angleY = cs.angleY;
        adj.angleRoll = cs.angleRoll;
        adj.distance = cs.distance;
        if (renderer.administradorCamaras != null) {
            for (ar.axt.controles.AdministradorCamaras.MarcadorCamara m : renderer.administradorCamaras.getMarcadores()) {
                if (cs.markerId != null && cs.markerId.equals(m.id)) {
                    renderer.administradorCamaras.selectedCamara = m;
                    break;
                }
            }
        }
    }

    private void aplicarLightState(LightFrameState ls, MyRenderer renderer) {
        if (ls == null || renderer == null) return;
        ar.axt.sombras.RealShadow real = renderer.getShaderRealista();
        ar.axt.sombras.ShadowAnime anime = renderer.getShaderAnime();
        if (real != null) {
            System.arraycopy(ls.pos, 0, real.lightPos, 0, 3);
            real.lightIntensity = ls.intensity;
            real.shadowStrength = ls.shadowStrength;
            System.arraycopy(ls.lightColor, 0, real.lightColor, 0, 3);
            System.arraycopy(ls.shadowColor, 0, real.shadowColor, 0, 3);
        }
        if (anime != null) {
            System.arraycopy(ls.pos, 0, anime.lightPos, 0, 3);
            anime.lightIntensity = ls.intensity;
            anime.shadowStrength = ls.shadowStrength;
            System.arraycopy(ls.lightColor, 0, anime.lightColor, 0, 3);
            System.arraycopy(ls.shadowColor, 0, anime.shadowColor, 0, 3);
        }
    }

    public void interpolarEscena(float tiempoActual, MyRenderer renderer, TreeMap<Integer, Keyframe> cacheKeyframes) {
        if (renderer == null || cacheKeyframes == null || cacheKeyframes.isEmpty()) return;
        Integer keyA = cacheKeyframes.floorKey((int) tiempoActual);
        Integer keyB = cacheKeyframes.ceilingKey((int) tiempoActual);
        if (keyA == null && keyB == null) return;
        if (keyA == null) {
            Keyframe kf = cacheKeyframes.get(keyB);
            aplicarKeyframe(kf, renderer);
            aplicarCameraState(kf.camera, renderer);
            aplicarLightState(kf.light, renderer);
            aplicarSkyState(kf.sky, renderer);
            return;
        }
        if (keyB == null) {
            Keyframe kf = cacheKeyframes.get(keyA);
            aplicarKeyframe(kf, renderer);
            aplicarCameraState(kf.camera, renderer);
            aplicarLightState(kf.light, renderer);
            aplicarSkyState(kf.sky, renderer);
            return;
        }
        if (keyA.equals(keyB)) {
            Keyframe kf = cacheKeyframes.get(keyA);
            aplicarKeyframe(kf, renderer);
            aplicarCameraState(kf.camera, renderer);
            aplicarLightState(kf.light, renderer);
            aplicarSkyState(kf.sky, renderer);
            return;
        }
        Keyframe kfA = cacheKeyframes.get(keyA);
        Keyframe kfB = cacheKeyframes.get(keyB);
        if (kfA == null || kfB == null) return;
        float progreso = (tiempoActual - keyA) / (float) (keyB - keyA);
        if (progreso < 0.0f) progreso = 0.0f;
        if (progreso > 1.0f) progreso = 1.0f;
        if (renderer.meshes != null) {
            for (MyRenderer.Mesh mesh : renderer.meshes) {
                if (mesh == null || mesh.name == null) continue;
                MeshFrameState stateA = buscarMeshEnKeyframe(kfA, mesh.name);
                MeshFrameState stateB = buscarMeshEnKeyframe(kfB, mesh.name);
                float[] posA = stateA != null ? stateA.pos : mesh.translation;
                float[] rotA = stateA != null ? stateA.rot : mesh.rotation;
                float[] sclA = stateA != null ? stateA.scale : mesh.scale;
                float[] posB = stateB != null ? stateB.pos : mesh.translation;
                float[] rotB = stateB != null ? stateB.rot : mesh.rotation;
                float[] sclB = stateB != null ? stateB.scale : mesh.scale;
                for (int i = 0; i < 3; i++) {
                    mesh.translation[i] = posA[i] + (posB[i] - posA[i]) * progreso;
                    mesh.rotation[i] = rotA[i] + (rotB[i] - rotA[i]) * progreso;
                    mesh.scale[i] = sclA[i] + (sclB[i] - sclA[i]) * progreso;
                }
                if (mesh.subMesh != null && mesh.subMesh.vertexBuffer != null) {
                    float[] vDataA = (stateA != null && stateA.vertexData != null) ? stateA.vertexData : null;
                    float[] vDataB = (stateB != null && stateB.vertexData != null) ? stateB.vertexData : null;
                    if (vDataA != null || vDataB != null) {
                        FloatBuffer vb = mesh.subMesh.vertexBuffer;
                        int capacity = vb.capacity();
                        vb.position(0);
                        float[] tempVertices = new float[capacity];
                        for (int v = 0; v < capacity; v++) {
                            float valA = (vDataA != null && v < vDataA.length) ? vDataA[v] : 0.0f;
                            float valB = (vDataB != null && v < vDataB.length) ? vDataB[v] : 0.0f;
                            tempVertices[v] = valA + (valB - valA) * progreso;
                        }
                        vb.position(0);
                        if (tempVertices.length <= vb.remaining()) {
                            vb.put(tempVertices);
                        } else { vb.put(tempVertices, 0, vb.remaining()); } vb.position(0);
                    }
                }
                if (mesh.hitbox != null) { mesh.hitbox.updateFromMesh(mesh); }
            }
        }
        if (renderer.material != null && renderer.material.getBoxes() != null) {
            for (ar.axt.materiales.Material.EffectBox box : renderer.material.getBoxes()) {
                if (box == null || box.id == null) continue;
                BoxFrameState stateA = buscarBoxEnKeyframe(kfA, box.id);
                BoxFrameState stateB = buscarBoxEnKeyframe(kfB, box.id);
                float[] posA = stateA != null ? stateA.pos : box.position;
                float[] rotA = stateA != null ? stateA.rot : box.rotation;
                float sclA = stateA != null ? stateA.scale : box.scale;
                float[] posB = stateB != null ? stateB.pos : box.position;
                float[] rotB = stateB != null ? stateB.rot : box.rotation;
                float sclB = stateB != null ? stateB.scale : box.scale;
                for (int i = 0; i < 3; i++) {
                    box.position[i] = posA[i] + (posB[i] - posA[i]) * progreso;
                    box.rotation[i] = rotA[i] + (rotB[i] - rotA[i]) * progreso;
                } box.scale = sclA + (sclB - sclA) * progreso;
            }
        }
        if (renderer.getBones() != null) {
            for (Bones.Bone bone : renderer.getBones().getAllBones()) {
                if (bone == null || bone.id == null) continue;
                BoneFrameState stateA = buscarBoneEnKeyframe(kfA, bone.id);
                BoneFrameState stateB = buscarBoneEnKeyframe(kfB, bone.id);
                float[] posA = stateA != null ? stateA.pos : bone.position;
                float[] rotA = stateA != null ? stateA.rot : bone.rotation;
                float[] sclA = stateA != null ? stateA.scale : bone.scale;
                float[] posB = stateB != null ? stateB.pos : bone.position;
                float[] rotB = stateB != null ? stateB.rot : bone.rotation;
                float[] sclB = stateB != null ? stateB.scale : bone.scale;
                for (int i = 0; i < 3; i++) {
                    bone.position[i] = posA[i] + (posB[i] - posA[i]) * progreso;
                    bone.rotation[i] = rotA[i] + (rotB[i] - rotA[i]) * progreso;
                    bone.scale[i] = sclA[i] + (sclB[i] - sclA[i]) * progreso;
                } bone.updateMatrixRecursive();
            }
        }
        if (kfA.camera != null && kfB.camera != null) {
            ar.axt.controles.AjustesDeCamara adj = renderer.ajustesDeCamara;
            if (kfA.camera.markerId != null && kfA.camera.markerId.equals(kfB.camera.markerId)) {
                adj.centerX = kfA.camera.centerX + (kfB.camera.centerX - kfA.camera.centerX) * progreso;
                adj.centerY = kfA.camera.centerY + (kfB.camera.centerY - kfA.camera.centerY) * progreso;
                adj.centerZ = kfA.camera.centerZ + (kfB.camera.centerZ - kfA.camera.centerZ) * progreso;
                adj.angleX = lerpAngle(kfA.camera.angleX, kfB.camera.angleX, progreso);
                adj.angleY = lerpAngle(kfA.camera.angleY, kfB.camera.angleY, progreso);
                adj.angleRoll = lerpAngle(kfA.camera.angleRoll, kfB.camera.angleRoll, progreso);
                adj.distance = kfA.camera.distance + (kfB.camera.distance - kfA.camera.distance) * progreso;
            } else {
                CameraFrameState elegida = (progreso < 0.5f) ? kfA.camera : kfB.camera;
                aplicarCameraState(elegida, renderer);
            }
        } else if (kfA.camera != null) {
            aplicarCameraState(kfA.camera, renderer);
        } else if (kfB.camera != null) {
            aplicarCameraState(kfB.camera, renderer);
        }
        if (kfA.light != null && kfB.light != null) {
            LightFrameState lInterp = new LightFrameState();
            for (int i = 0; i < 3; i++) {
                lInterp.pos[i] = kfA.light.pos[i] + (kfB.light.pos[i] - kfA.light.pos[i]) * progreso;
                lInterp.lightColor[i] = kfA.light.lightColor[i] + (kfB.light.lightColor[i] - kfA.light.lightColor[i]) * progreso;
                lInterp.shadowColor[i] = kfA.light.shadowColor[i] + (kfB.light.shadowColor[i] - kfA.light.shadowColor[i]) * progreso;
            }
            lInterp.intensity = kfA.light.intensity + (kfB.light.intensity - kfA.light.intensity) * progreso;
            lInterp.shadowStrength = kfA.light.shadowStrength + (kfB.light.shadowStrength - kfA.light.shadowStrength) * progreso;
            aplicarLightState(lInterp, renderer);
        } else if (kfA.light != null) {
            aplicarLightState(kfA.light, renderer);
        } else if (kfB.light != null) {
            aplicarLightState(kfB.light, renderer);
        }
        if (kfA.sky != null && kfB.sky != null) {
            SkyFrameState sInterp = new SkyFrameState();
            for (int i = 0; i < 3; i++) {
                sInterp.skyColor[i] = kfA.sky.skyColor[i] + (kfB.sky.skyColor[i] - kfA.sky.skyColor[i]) * progreso;
            }
            sInterp.cloudDensity = kfA.sky.cloudDensity + (kfB.sky.cloudDensity - kfA.sky.cloudDensity) * progreso;
            sInterp.starDensity = kfA.sky.starDensity + (kfB.sky.starDensity - kfA.sky.starDensity) * progreso;
            aplicarSkyState(sInterp, renderer);
        } else if (kfA.sky != null) {
            aplicarSkyState(kfA.sky, renderer);
        } else if (kfB.sky != null) {
            aplicarSkyState(kfB.sky, renderer);
        }
        if (renderer.activity != null && renderer.activity.glSurfaceView != null) {
            renderer.activity.glSurfaceView.requestRender();
        }
    }

    public int getUltimoSegundo(TreeMap<Integer, Keyframe> keyframes) {
        if (keyframes == null || keyframes.isEmpty()) return 0;
        return keyframes.lastKey();
    }

    private float lerpAngle(float start, float end, float progress) {
        float delta = end - start;
        if (delta > 180f) delta -= 360f;
        else if (delta < -180f) delta += 360f;
        return start + delta * progress;
    }

    private MeshFrameState buscarMeshEnKeyframe(Keyframe kf, String name) {
        if (kf == null || kf.meshes == null || name == null) return null;
        for (MeshFrameState m : kf.meshes) {
            if (m != null && name.equals(m.name)) return m;
        } return null;
    }

    private BoxFrameState buscarBoxEnKeyframe(Keyframe kf, String id) {
        if (kf == null || kf.boxes == null || id == null) return null;
        for (BoxFrameState b : kf.boxes) {
            if (b != null && id.equals(b.id)) return b;
        } return null;
    }

    private BoneFrameState buscarBoneEnKeyframe(Keyframe kf, String id) {
        if (kf == null || kf.bones == null || id == null) return null;
        for (BoneFrameState bn : kf.bones) {
            if (bn != null && id.equals(bn.id)) return bn;
        } return null;
    }

}
