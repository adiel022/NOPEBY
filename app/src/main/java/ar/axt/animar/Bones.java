package ar.axt.animar;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import ar.axt.controles.Gismo;
import ar.axt.ficicas.Hitbox;
import ar.axt.leerobj.ObjetosCargados;
import ar.axt.nopeby.MyRenderer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class Bones {
    private static final boolean DEBUG_CAPTURE = true;
    private static final String FRAG_SHADER = "precision mediump float;uniform vec4 uColor;void main(){ gl_FragColor = uColor; }";
    private static final String TAG = "BonesCapture";
    private static final String VERT_SHADER = "attribute vec3 aPos;uniform mat4 uMVP;void main(){ gl_Position = uMVP * vec4(aPos,1.0); }";
    private int aPos;
    private List<Bone> allBones;
    private Context context;
    private String currentGroup;
    private Map<String, List<Bone>> groupBones;
    private int groupIndex;
    private Map<String, List<MyRenderer.Mesh>> groupMeshes;
    private boolean groupStarted;
    private int modelAColor;
    private int modelAPos;
    private List<ObjetosCargados.SubMesh> modelMeshes;
    private int modelProgram;
    private int modelUAlpha;
    private int modelUMVP;
    private int program;
    public MyRenderer renderer;
    public Bone selectedBone;
    private float[] tempMVP;
    private int uColor;
    private int uMVP;

    public Bones(Context context) {
        this.modelProgram = -1;
        this.program = -1;
        this.groupMeshes = new HashMap();
        this.allBones = new CopyOnWriteArrayList();
        this.groupBones = new HashMap();
        this.currentGroup = null;
        this.groupStarted = false;
        this.selectedBone = null;
        this.groupIndex = 0;
        this.tempMVP = new float[16];
        if (context != null) {
            try {
                ObjetosCargados loader = new ObjetosCargados(context);
                this.modelMeshes = loader.loadFromAssets("hueso.obj");
                this.context = context;
            } catch (IOException e) {
                Log.e("Bones", "Error cargando hueso.obj: " + e.getMessage());
            }
        }
    }

    public Bones() {
        this.modelProgram = -1;
        this.program = -1;
        this.groupMeshes = new HashMap();
        this.allBones = new CopyOnWriteArrayList();
        this.groupBones = new HashMap();
        this.currentGroup = null;
        this.groupStarted = false;
        this.selectedBone = null;
        this.groupIndex = 0;
        this.tempMVP = new float[16];
    }

    public void initShader() {
        if (this.program == -1 || !GLES20.glIsProgram(this.program)) {
            int vs = loadShader(35633, VERT_SHADER);
            int fs = loadShader(35632, FRAG_SHADER);
            this.program = GLES20.glCreateProgram();
            GLES20.glAttachShader(this.program, vs);
            GLES20.glAttachShader(this.program, fs);
            GLES20.glLinkProgram(this.program);
            this.aPos = GLES20.glGetAttribLocation(this.program, "aPos");
            this.uMVP = GLES20.glGetUniformLocation(this.program, "uMVP");
            this.uColor = GLES20.glGetUniformLocation(this.program, "uColor");
        }
        initModelShader();
    }

    private int loadShader(int type, String code) {
        int id = GLES20.glCreateShader(type);
        GLES20.glShaderSource(id, code);
        GLES20.glCompileShader(id);
        return id;
    }

    public static class CapturedVertex {
        public int index;
        public MyRenderer.Mesh mesh;
        public float weight;

        public CapturedVertex(MyRenderer.Mesh m, int i, float w) {
            this.mesh = m;
            this.index = i;
            this.weight = w;
        }
    }

    public static class Bone {
        public List<CapturedVertex> capturedVertices;
        public String group;
        public Map<MyRenderer.Mesh, List<CapturedVertex>> groupedVertices;
        public Hitbox hitbox;
        public String id;
        public Bone parent;
        public boolean captured = false;
        public List<Bone> children = new ArrayList();
        public float[] color = new float[4];
        public float[] position = {0.0f, 0.0f, 0.0f};
        public float[] rotation = {0.0f, 0.0f, 0.0f};
        public float[] scale = {1.0f, 1.0f, 1.0f};
        public final float[] headLocal = {0.0f, 0.5f, 0.0f};
        public final float[] tailLocal = {0.0f, -0.5f, 0.0f};
        public final float[] headWorld = new float[4];
        public final float[] tailWorld = new float[4];
        public float[] localMatrix = new float[16];
        public float[] worldMatrix = new float[16];
        public final float[] prismVertices = {-0.2f, -0.5f, -0.2f, 0.2f, -0.5f, -0.2f, 0.2f, 0.5f, -0.2f, -0.2f, 0.5f, -0.2f, -0.2f, -0.5f, 0.2f, 0.2f, -0.5f, 0.2f, 0.2f, 0.5f, 0.2f, -0.2f, 0.5f, 0.2f};
        public final short[] prismLines = {0, 1, 1, 2, 2, 3, 3, 0, 4, 5, 5, 6, 6, 7, 7, 4, 0, 4, 1, 5, 2, 6, 3, 7};

        public Bone(String id, String group, Bone parent) {
            this.id = id;
            this.group = group;
            this.parent = parent;
            if (parent != null) {parent.children.add(this); }
            this.capturedVertices = new ArrayList();
            this.color[0] = (((float) Math.random()) * 0.7f) + 0.3f;
            this.color[1] = (((float) Math.random()) * 0.7f) + 0.3f;
            this.color[2] = (((float) Math.random()) * 0.7f) + 0.3f;
            this.color[3] = 1.0f;
            this.hitbox = new Hitbox();
            this.hitbox.localCenter[0] = 0.0f;
            this.hitbox.localCenter[1] = 0.0f;
            this.hitbox.localCenter[2] = 0.0f;
            this.hitbox.halfSize[0] = 0.2f;
            this.hitbox.halfSize[1] = 0.5f;
            this.hitbox.halfSize[2] = 0.2f;
        }

        public void updateMatrixRecursive() {
            updateMatrix();
            for (Bone c : this.children) {
                c.updateMatrixRecursive();
            }
        }

        public void updateMatrix() {
            Matrix.setIdentityM(this.localMatrix, 0);
            Matrix.translateM(this.localMatrix, 0, this.position[0], this.position[1], this.position[2]);
            Matrix.rotateM(this.localMatrix, 0, this.rotation[0], 1.0f, 0.0f, 0.0f);
            Matrix.rotateM(this.localMatrix, 0, this.rotation[1], 0.0f, 1.0f, 0.0f);
            Matrix.rotateM(this.localMatrix, 0, this.rotation[2], 0.0f, 0.0f, 1.0f);
            Matrix.scaleM(this.localMatrix, 0, this.scale[0], this.scale[1], this.scale[2]);
            if (this.parent == null) {
                System.arraycopy(this.localMatrix, 0, this.worldMatrix, 0, 16);
            } else {
                float[] parentNoScale = new float[16];
                System.arraycopy(this.parent.worldMatrix, 0, parentNoScale, 0, 16);
                Bones.normalizeMatrix(parentNoScale);
                Matrix.multiplyMM(this.worldMatrix, 0, parentNoScale, 0, this.localMatrix, 0);
            }
            this.hitbox.updateFromMatrix(this.worldMatrix);
            float[] tmp = {this.headLocal[0], this.headLocal[1], this.headLocal[2], 1.0f};
            Matrix.multiplyMV(this.headWorld, 0, this.worldMatrix, 0, tmp, 0);
            tmp[0] = this.tailLocal[0];
            tmp[1] = this.tailLocal[1];
            tmp[2] = this.tailLocal[2];
            tmp[3] = 1.0f;
            Matrix.multiplyMV(this.tailWorld, 0, this.worldMatrix, 0, tmp, 0);
        }
    }

    private String nextGroupName() {
        int x = this.groupIndex;
        this.groupIndex = x + 1;
        String g = "";
        while (x >= 0) {
            g = ((char) ((x % 26) + 97)) + g;
            x = (x / 26) - 1;
        } return g;
    }

    private String makeBoneID(String group, Bone parent, int order) {
        if (parent == null) {
            return "id_bone/" + group + "_0." + order;
        }
        return "id_bone/" + group + "_" + (getLevel(parent) + 1) + "." + order + "/id_bone/" + parent.group + "_" + getLevel(parent) + ".1";
    }

    private int getLevel(Bone b) {
        int level = 0;
        while (b.parent != null) {
            b = b.parent;
            level++;
        } return level;
    }

    public Bone createBone(Gismo gizmo) {
        if (!this.groupStarted) {
            this.currentGroup = nextGroupName();
            this.groupStarted = DEBUG_CAPTURE;
            this.groupBones.put(this.currentGroup, new ArrayList());
        }
        List<Bone> list = this.groupBones.get(this.currentGroup);
        int order = list.size() + 1;
        Bone parent = this.selectedBone;
        Bone b = new Bone(makeBoneID(this.currentGroup, parent, order), this.currentGroup, parent);
        if (parent != null) {
            b.scale[0] = parent.scale[0];
            b.scale[1] = parent.scale[1];
            b.scale[2] = parent.scale[2];
            b.position[0] = 0.0f;
            b.position[1] = -((parent.scale[1] * 0.5f) + (b.scale[1] * 0.5f));
            b.position[2] = 0.0f;
            b.rotation[0] = 0.0f;
            b.rotation[1] = 0.0f;
            b.rotation[2] = 0.0f;
        } else if (gizmo != null) {
            b.position[0] = gizmo.position[0];
            b.position[1] = gizmo.position[1];
            b.position[2] = gizmo.position[2];
        }
        list.add(b);
        this.allBones.add(b);
        this.selectedBone = b;
        Bone root = b;
        while (root.parent != null) {
            root = root.parent;
        }
        root.updateMatrixRecursive();
        if (this.renderer != null && this.renderer.interaccionGismo != null) {
            this.renderer.interaccionGismo.actualizarCentroGizmo();
        } return b;
    }

    public void finishGroup() {
        this.groupStarted = false;
    }

    public void selectBone(String id) {
        for (Bone b : this.allBones) {
            if (b.id.equals(id)) {
                this.selectedBone = b;
                if (this.renderer != null && this.renderer.interaccionGismo != null) {
                    this.renderer.interaccionGismo.actualizarCentroGizmo();
                } return;
            }
        }
    }

    public void syncSelectedBoneWithGizmo(Gismo gizmo) {
        if (this.selectedBone == null || gizmo == null) { return; }
        if (this.selectedBone.parent != null) {
            float[] parentNoScale = (float[]) this.selectedBone.parent.worldMatrix.clone();
            normalizeMatrix(parentNoScale);
            float[] parentInv = new float[16];
            Matrix.invertM(parentInv, 0, parentNoScale, 0);
            float[] worldPos = {gizmo.position[0], gizmo.position[1], gizmo.position[2], 1.0f};
            float[] localPos = new float[4];
            Matrix.multiplyMV(localPos, 0, parentInv, 0, worldPos, 0);
            this.selectedBone.position[0] = localPos[0];
            this.selectedBone.position[1] = localPos[1];
            this.selectedBone.position[2] = localPos[2];
            this.selectedBone.rotation[0] = gizmo.rotation[0];
            this.selectedBone.rotation[1] = gizmo.rotation[1];
            this.selectedBone.rotation[2] = gizmo.rotation[2];
        } else {
            this.selectedBone.position[0] = gizmo.position[0];
            this.selectedBone.position[1] = gizmo.position[1];
            this.selectedBone.position[2] = gizmo.position[2];
            this.selectedBone.rotation[0] = gizmo.rotation[0];
            this.selectedBone.rotation[1] = gizmo.rotation[1];
            this.selectedBone.rotation[2] = gizmo.rotation[2];
        }
        Bone root = this.selectedBone;
        while (root.parent != null) {
            root = root.parent;
        } root.updateMatrixRecursive();
    }

    public float[] multPoint(float[] M, float x, float y, float z) {
        float[] in = {x, y, z, 1.0f};
        float[] out = new float[4];
        Matrix.multiplyMV(out, 0, M, 0, in, 0);
        return new float[]{out[0], out[1], out[2]};
    }

    public static void normalizeMatrix(float[] m) {
        float lx = (float) Math.sqrt((m[0] * m[0]) + (m[1] * m[1]) + (m[2] * m[2]));
        float ly = (float) Math.sqrt((m[4] * m[4]) + (m[5] * m[5]) + (m[6] * m[6]));
        float lz = (float) Math.sqrt((m[8] * m[8]) + (m[9] * m[9]) + (m[10] * m[10]));
        if (lx != 0.0f && ly != 0.0f && lz != 0.0f) {
            m[0] = m[0] / lx;
            m[1] = m[1] / lx;
            m[2] = m[2] / lx;
            m[4] = m[4] / ly;
            m[5] = m[5] / ly;
            m[6] = m[6] / ly;
            m[8] = m[8] / lz;
            m[9] = m[9] / lz;
            m[10] = m[10] / lz;
        }
    }

    public void capturePolygonsForBone(Bone b, ObjetosCargados.SubMesh submesh, MyRenderer.Mesh mesh, float gizmoScale) {
        int capturedCount = 0;
        float[] M = new float[16];
        Matrix.setIdentityM(M, 0);
        Matrix.translateM(M, 0, mesh.translation[0], mesh.translation[1], mesh.translation[2]);
        Matrix.rotateM(M, 0, mesh.rotation[0], 1.0f, 0.0f, 0.0f);
        Matrix.rotateM(M, 0, mesh.rotation[1], 0.0f, 1.0f, 0.0f);
        Matrix.rotateM(M, 0, mesh.rotation[2], 0.0f, 0.0f, 1.0f);
        Matrix.scaleM(M, 0, mesh.scale[0], mesh.scale[1], mesh.scale[2]);
        float[] boneInv = new float[16];
        Matrix.invertM(boneInv, 0, b.worldMatrix, 0);
        FloatBuffer vb = submesh.vertexBuffer;
        ShortBuffer ib = submesh.indexBuffer.duplicate();
        ib.position(0); vb.position(0);
        int numIndices = submesh.numIndices;
        HashSet<Long> existing = new HashSet<>();
        for (CapturedVertex cv : b.capturedVertices) {
            existing.add((((long) cv.mesh.hashCode()) & 0xFFFFFFFFL) | (((long) cv.index) << 32));
        }
        if (b.groupedVertices == null) {b.groupedVertices = new HashMap<>(); }
        float[] hs = b.hitbox.halfSize;
        float[] tmp = new float[4];
        float[] world = new float[4];
        float[] local = new float[4];
        for (int t = 0; t < numIndices; t++) {
            int index = ib.get() & 0xFFFF;
            int base = index * 3;
            if (base < 0 || base + 2 >= vb.limit()) {
                Log.e(TAG, "Índice fuera de rango: i=" + index + " base=" + base + " limit=" + vb.limit());
                continue;
            }
            float vx = vb.get(base);
            float vy = vb.get(base + 1);
            float vz = vb.get(base + 2);
            tmp[0] = vx;
            tmp[1] = vy;
            tmp[2] = vz;
            tmp[3] = 1.0f;
            Matrix.multiplyMV(world, 0, M, 0, tmp, 0);
            Matrix.multiplyMV(local, 0, boneInv, 0, world, 0);
            float x = local[0];
            float y = local[1];
            float z = local[2];
            if (x >= -hs[0] && x <= hs[0] &&
                y >= -hs[1] && y <= hs[1] &&
                z >= -hs[2] && z <= hs[2]) {
                long key = (((long) index) << 32) | (((long) mesh.hashCode()) & 0xFFFFFFFFL);
                if (!existing.contains(key)) {
                    float distAlEje = (float) Math.sqrt(x * x + z * z);
                    float maxDist = (float) Math.sqrt(hs[0] * hs[0] + hs[2] * hs[2]);
                    float weight = 1.0f - (distAlEje / maxDist);
                    weight = Math.max(0.1f, Math.min(1.0f, weight));
                    CapturedVertex cv = new CapturedVertex(mesh, index, weight);
                    b.capturedVertices.add(cv);
                    List<CapturedVertex> list = b.groupedVertices.get(mesh);
                    if (list == null) {
                        list = new ArrayList<>();
                        b.groupedVertices.put(mesh, list);
                    } list.add(cv);
                    List<MyRenderer.Mesh> meshes = this.groupMeshes.get(b.group);
                    if (meshes == null) {
                        meshes = new ArrayList<>();
                        this.groupMeshes.put(b.group, meshes);
                    }
                    if (!meshes.contains(mesh)) {meshes.add(mesh);}
                    existing.add(key); capturedCount++;
                }
            }
        }
        if (capturedCount > 0) {b.captured = true; }
        Log.d(TAG, "DONE bone=" + b.id + " totalCaptured=" + b.capturedVertices.size() + " addedThisRun=" + capturedCount);
    }

    public void initModelShader() {
        if (this.modelProgram == -1 || !GLES20.glIsProgram(this.modelProgram)) {
            String vertexSrc = loadShaderFromAssets("simpleAlfha_vertex.glsl");
            String fragmentSrc = loadShaderFromAssets("simpleAlfha_fragment.glsl");
            if (vertexSrc == null || fragmentSrc == null) {
                vertexSrc = "attribute vec3 aPosition; attribute vec3 aColor; uniform mat4 uMVP; varying vec3 vColor; void main(){ vColor = aColor; gl_Position = uMVP * vec4(aPosition,1.0); }";
                fragmentSrc = "precision mediump float; varying vec3 vColor; uniform float uAlpha; void main(){ gl_FragColor = vec4(vColor, uAlpha); }";
            }
            this.modelProgram = createProgram(vertexSrc, fragmentSrc);
            this.modelAPos = GLES20.glGetAttribLocation(this.modelProgram, "aPosition");
            this.modelAColor = GLES20.glGetAttribLocation(this.modelProgram, "aColor");
            this.modelUMVP = GLES20.glGetUniformLocation(this.modelProgram, "uMVP");
            this.modelUAlpha = GLES20.glGetUniformLocation(this.modelProgram, "uAlpha");
        }
    }

    private String loadShaderFromAssets(String filename) {
        if (this.context == null) { return null; }
        StringBuilder sb = new StringBuilder();
        try {
            InputStream is = this.context.getAssets().open(filename);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            while (true) {
                String line = br.readLine();
                if (line != null) {
                    sb.append(line).append("\n");
                } else {
                    br.close();
                    return sb.toString();
                }
            }
        } catch (IOException e) { return null; }
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        int vs = loadShader(35633, vertexSource);
        int fs = loadShader(35632, fragmentSource);
        int prog = GLES20.glCreateProgram();
        GLES20.glAttachShader(prog, vs);
        GLES20.glAttachShader(prog, fs);
        GLES20.glLinkProgram(prog);
        return prog;
    }

    public void renderBone(Bone b, float[] viewMatrix, float[] projection) {
        initShader();
        GLES20.glUseProgram(this.program);
        Matrix.multiplyMM(this.tempMVP, 0, viewMatrix, 0, b.worldMatrix, 0);
        Matrix.multiplyMM(this.tempMVP, 0, projection, 0, this.tempMVP, 0);
        GLES20.glUniformMatrix4fv(this.uMVP, 1, false, this.tempMVP, 0);
        float[] colorFinal = (float[]) b.color.clone();
        if (this.renderer != null && (this.renderer.isExporting || (this.renderer.activity != null && this.renderer.activity.isPlayingAnimation))) {
            colorFinal[3] = 0.0f; 
        }
        GLES20.glUniform4fv(this.uColor, 1, colorFinal, 0);
        GLES20.glEnableVertexAttribArray(this.aPos);
        FloatBuffer fb = makeFloatBuffer(b.prismVertices);
        GLES20.glVertexAttribPointer(this.aPos, 3, 5126, false, 0, (Buffer) fb);
        ShortBuffer sb = makeShortBuffer(b.prismLines);
        GLES20.glDrawElements(1, b.prismLines.length, 5123, sb);
        GLES20.glDisableVertexAttribArray(this.aPos);
        if (this.modelMeshes != null && !this.modelMeshes.isEmpty()) {
            initModelShader();
            GLES20.glUseProgram(this.modelProgram);
            GLES20.glUniformMatrix4fv(this.modelUMVP, 1, false, this.tempMVP, 0);
            float alphaModel = 0.25f;
            if (this.renderer != null && this.renderer.activity != null && this.renderer.activity.isPlayingAnimation) {
                alphaModel = 0.0f;
            }
            GLES20.glUniform1f(this.modelUAlpha, alphaModel);
            GLES20.glEnable(3042);
            GLES20.glBlendFunc(770, 771);
            GLES20.glEnableVertexAttribArray(this.modelAPos);
            GLES20.glEnableVertexAttribArray(this.modelAColor);
            for (ObjetosCargados.SubMesh sm : this.modelMeshes) {
                sm.vertexBuffer.position(0);
                GLES20.glVertexAttribPointer(this.modelAPos, 3, 5126, false, 0, (Buffer) sm.vertexBuffer);
                if (sm.colorBuffer != null) {
                    sm.colorBuffer.position(0);
                    GLES20.glVertexAttribPointer(this.modelAColor, 3, 5126, false, 0, (Buffer) sm.colorBuffer);
                }
                GLES20.glDrawElements(4, sm.numIndices, 5123, sm.indexBuffer);
            }
            GLES20.glDisableVertexAttribArray(this.modelAPos);
            GLES20.glDisableVertexAttribArray(this.modelAColor);
            GLES20.glDisable(3042);
        }
    }

    private FloatBuffer makeFloatBuffer(float[] a) {
        ByteBuffer bb = ByteBuffer.allocateDirect(a.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(a);
        fb.position(0);
        return fb;
    }

    private ShortBuffer makeShortBuffer(short[] a) {
        ByteBuffer bb = ByteBuffer.allocateDirect(a.length * 2);
        bb.order(ByteOrder.nativeOrder());
        ShortBuffer sb = bb.asShortBuffer();
        sb.put(a);
        sb.position(0);
        return sb;
    }

    public void renderAll(float[] view, float[] proj, int cantidadActual) {
        if (this.renderer != null && (this.renderer.isExporting || (this.renderer.activity != null && this.renderer.activity.isPlayingAnimation))) {
            return;
        }
        for (Bone b : this.allBones) { renderBone(b, view, proj); }
    }

    public boolean hasSelectedBone() {
        if (this.selectedBone != null) {
            return DEBUG_CAPTURE;
        } return false;
    }

    public Bone getSelectedBone() {
        return this.selectedBone;
    }

    public List<Bone> getAllBones() {
        return this.allBones;
    }

    public void clearSelectedBone() {
        this.selectedBone = null;
    }

    public void deleteLastBone() {
        if (this.allBones.isEmpty()) { return; }
        Bone b = this.allBones.get(this.allBones.size() - 1);
        removeBoneCompletely(b);
    }

    public void removeBoneCompletely(Bone b) {
        if (b == null) { return; }
        if (b.parent != null) { b.parent.children.remove(b); }
        List<Bone> list = this.groupBones.get(b.group);
        if (list != null) { list.remove(b); }
        this.allBones.remove(b);
        if (this.selectedBone == b) { this.selectedBone = null; }
    }

    public void removeBoneSubtree(Bone b) {
        if (b == null) { return; }
        List<Bone> childrenCopy = new ArrayList<>(b.children);
        for (Bone child : childrenCopy) {
            removeBoneSubtree(child);
        } removeBoneCompletely(b);
    }

    public void addBoneCompletely(Bone b) {
        if (b == null) { return; }
        if (b.parent != null && !b.parent.children.contains(b)) { b.parent.children.add(b); }
        if (b.group != null) {
            List<Bone> list = this.groupBones.get(b.group);
            if (list == null) { list = new ArrayList();
                this.groupBones.put(b.group, list);
            }
            if (!list.contains(b)) { list.add(b); }
        }
        if (!this.allBones.contains(b)) { this.allBones.add(b); }
    }

    public List<MyRenderer.Mesh> getMeshesFromGroup(String group) {
        List<MyRenderer.Mesh> list = this.groupMeshes.get(group);
        return list == null ? new ArrayList() : list;
    }

    public String getCurrentGroup() {
        return this.currentGroup;
    }

    public void setBones(List<Bone> bones) {
        if (bones == null) { return; }
        this.allBones.clear();
        this.allBones.addAll(bones);
        this.groupBones.clear();
        this.groupMeshes.clear();
        int maxIdx = -1;
        for (Bone b : this.allBones) {
            if (b.group != null) {
                List<Bone> list = this.groupBones.get(b.group);
                if (list == null) {
                    list = new ArrayList();
                    this.groupBones.put(b.group, list);
                    int idx = groupNameToIndex(b.group);
                    if (idx > maxIdx) {
                        maxIdx = idx;
                    }
                }
                if (!list.contains(b)) { list.add(b); }
                if (b.groupedVertices != null) {
                    for (MyRenderer.Mesh m : b.groupedVertices.keySet()) {
                        List<MyRenderer.Mesh> meshesInGroup = this.groupMeshes.get(b.group);
                        if (meshesInGroup == null) {
                            meshesInGroup = new ArrayList();
                            this.groupMeshes.put(b.group, meshesInGroup);
                        }
                        if (!meshesInGroup.contains(m)) { meshesInGroup.add(m); }
                    }
                }
            }
            if (b.parent == null) { b.updateMatrixRecursive(); }
        } this.groupIndex = maxIdx + 1;
    }

    private int groupNameToIndex(String g) {
        if (g == null || g.isEmpty()) { return -1; }
        int idx = 0;
        for (int i = 0; i < g.length(); i++) {
            idx = (idx * 26) + (g.charAt(i) - 'a') + 1;
        }
        int i2 = idx - 1; return i2;
    }

}
