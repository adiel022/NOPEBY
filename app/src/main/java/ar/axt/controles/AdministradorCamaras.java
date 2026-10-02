package ar.axt.controles;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import android.widget.Toast;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import ar.axt.ficicas.Hitbox;
import ar.axt.leerobj.ObjetosCargados;
import ar.axt.nopeby.MainActivity;
import ar.axt.nopeby.MyRenderer;

public class AdministradorCamaras {
    private static final String TAG = "AdminCamaras";
    private final Context context;
    private final MyRenderer renderer;
    private final MainActivity activity;

    private int program;
    private List<ObjetosCargados.SubMesh> camaraMeshes;
    private Hitbox[] hitboxes;

    private int indexVista = -1;
    private int indexContraria = -1;

    public static class MarcadorCamara {
        public String id;
        public float[] position = {0f, 0f, 0f};
        public float[] rotation = {0f, 0f, 0f};
        public float angleRoll = 0f;
        public float scale = 1.0f;
        public Hitbox[] hitboxes;

        public MarcadorCamara(String id, float[] pos, float[] rot, float roll, Hitbox[] originalHitboxes) {
            this.id = id;
            System.arraycopy(pos, 0, this.position, 0, 3);
            System.arraycopy(rot, 0, this.rotation, 0, 3);
            this.angleRoll = roll;
            if (originalHitboxes != null) {
                this.hitboxes = new Hitbox[originalHitboxes.length];
                for (int i = 0; i < originalHitboxes.length; i++) {
                    this.hitboxes[i] = new Hitbox();
                    System.arraycopy(originalHitboxes[i].localCenter, 0, this.hitboxes[i].localCenter, 0, 3);
                    System.arraycopy(originalHitboxes[i].halfSize, 0, this.hitboxes[i].halfSize, 0, 3);
                }
            }
        }
        
        public void actualizarHitboxes() {
            if (hitboxes == null) return;
            float[] model = new float[16];
            Matrix.setIdentityM(model, 0);
            Matrix.translateM(model, 0, position[0], position[1], position[2]);
            Matrix.rotateM(model, 0, rotation[0], 1, 0, 0);
            Matrix.rotateM(model, 0, rotation[1], 0, 1, 0);
            Matrix.rotateM(model, 0, rotation[2], 0, 0, 1);
            Matrix.scaleM(model, 0, scale, scale, scale);
            for (Hitbox hb : hitboxes) {
                hb.updateFromMatrix(model);
            }
        }
    }

    private List<MarcadorCamara> marcadores = new ArrayList<MarcadorCamara>();
    private int indiceActual = 0;
    private float freeAngleX, freeAngleY, freeDistance, freeAngleRoll;
    private float freeCenterX, freeCenterY, freeCenterZ;
    public MarcadorCamara selectedCamara;

    public AdministradorCamaras(Context context, MyRenderer renderer, MainActivity activity) {
        this.context = context;
        this.renderer = renderer;
        this.activity = activity;
        init();
    }

    private void init() {
        try {
            ObjetosCargados loader = new ObjetosCargados(context);
            camaraMeshes = loader.loadFromAssets("camara.obj");
            hitboxes = new Hitbox[camaraMeshes.size()];
            for (int i = 0; i < camaraMeshes.size(); i++) {
                ObjetosCargados.SubMesh sm = camaraMeshes.get(i);
                hitboxes[i] = createHitboxFromMesh(sm);
                if (sm.name != null) {
                    String lowerName = sm.name.toLowerCase();
                    if (lowerName.contains("vista")) {
                        indexVista = i;
                    } else if (lowerName.contains("contraria")) {
                        indexContraria = i;
                    }
                }
            }
        } catch (IOException e) {Log.e(TAG, "Error cargando camara.obj", e);}
        String vs = "attribute vec3 aPosition; attribute vec3 aColor; uniform mat4 uMVP; varying vec3 vColor; " +
                   "void main() { vColor = aColor; gl_Position = uMVP * vec4(aPosition, 1.0); }";
        String fs = "precision mediump float; varying vec3 vColor; " +
                   "void main() { gl_FragColor = vec4(vColor, 1.0); }";
        program = createProgram(vs, fs);
    }

    public void eliminarCamara() {
        if (selectedCamara != null) {
            marcadores.remove(selectedCamara);
            Toast.makeText(context, "Cámara eliminada: " + selectedCamara.id, Toast.LENGTH_SHORT).show();
            selectedCamara = null;
            if (renderer.rayosInteraccion != null) {
                renderer.rayosInteraccion.deseleccionarTodo();
            } activity.requestRender();
        } else {
            Toast.makeText(context, "Ninguna cámara seleccionada", Toast.LENGTH_SHORT).show();
        }
    }

    public void anadirCamara() {
        if (renderer.ajustesDeCamara == null) return;
        float[] camPos = renderer.ajustesDeCamara.getCameraPosition();
        float[] forward = renderer.ajustesDeCamara.getCameraForward();
        float offset = 2.0f;
        float[] markerPos = {
            camPos[0] - forward[0] * offset,
            camPos[1] - forward[1] * offset,
            camPos[2] - forward[2] * offset };
        float[] markerRot = {-renderer.ajustesDeCamara.angleY, -renderer.ajustesDeCamara.angleX, 0f};
        float markerRoll = renderer.ajustesDeCamara.angleRoll;
        String id = "cam_" + (marcadores.size() + 1);
        MarcadorCamara nuevo = new MarcadorCamara(id, markerPos, markerRot, markerRoll, hitboxes);
        marcadores.add(nuevo);
        selectedCamara = nuevo;
        if (renderer.gizmo != null) {
            renderer.gizmo.setPosition(markerPos[0], markerPos[1], markerPos[2]);
            renderer.gizmo.rotation[0] = markerRot[0];
            renderer.gizmo.rotation[1] = markerRot[1];
            renderer.gizmo.rotation[2] = markerRot[2];
        }
        Toast.makeText(context, "Cámara añadida: " + id, Toast.LENGTH_SHORT).show();
    }

    public void siguienteCamara() {
        if (marcadores.isEmpty()) return;
        if (indiceActual == 0) { guardarEstadoCamaraLibre(); }
        indiceActual++;
        if (indiceActual > marcadores.size()) { indiceActual = 0; restaurarCamaraLibre();
        } else { teletransportarA(marcadores.get(indiceActual - 1));
        } activity.requestRender();
    }

    public void anteriorCamara() {
        if (marcadores.isEmpty()) return;
        if (indiceActual == 0) { guardarEstadoCamaraLibre();
            indiceActual = marcadores.size();
        } else { indiceActual--; }
        if (indiceActual == 0) { restaurarCamaraLibre();
        } else { teletransportarA(marcadores.get(indiceActual - 1));
        } activity.requestRender();
    }

    private void guardarEstadoCamaraLibre() {
        AjustesDeCamara adj = renderer.ajustesDeCamara;
        freeAngleX = adj.angleX;
        freeAngleY = adj.angleY;
        freeDistance = adj.distance;
        freeAngleRoll = adj.angleRoll;
        freeCenterX = adj.centerX;
        freeCenterY = adj.centerY;
        freeCenterZ = adj.centerZ;
    }

    private void restaurarCamaraLibre() {
        AjustesDeCamara adj = renderer.ajustesDeCamara;
        adj.angleX = freeAngleX;
        adj.angleY = freeAngleY;
        adj.distance = freeDistance;
        adj.angleRoll = freeAngleRoll;
        adj.centerX = freeCenterX;
        adj.centerY = freeCenterY;
        adj.centerZ = freeCenterZ;
        Toast.makeText(context, "Cámara Libre", Toast.LENGTH_SHORT).show();
    }

    private void teletransportarA(MarcadorCamara m) {
        AjustesDeCamara adj = renderer.ajustesDeCamara;
        float[] localVista = {0f, 0f, 0f};
        float[] localContraria = {0f, 0f, -1f};
        if (indexVista != -1) {
            localVista = hitboxes[indexVista].localCenter; }
        if (indexContraria != -1) {
            localContraria = hitboxes[indexContraria].localCenter; }
        float[] model = new float[16];
        Matrix.setLookAtM(model, 0, 0, 0, 0, 0, 0, -1, 0, 1, 0);
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, m.position[0], m.position[1], m.position[2]);
        Matrix.rotateM(model, 0, m.rotation[0], 1, 0, 0);
        Matrix.rotateM(model, 0, m.rotation[1], 0, 1, 0);
        Matrix.rotateM(model, 0, m.rotation[2], 0, 0, 1);
        Matrix.scaleM(model, 0, m.scale, m.scale, m.scale);
        float[] worldVista = new float[4];
        float[] worldContraria = new float[4];
        Matrix.multiplyMV(worldVista, 0, model, 0, new float[]{localVista[0], localVista[1], localVista[2], 1f}, 0);
        Matrix.multiplyMV(worldContraria, 0, model, 0, new float[]{localContraria[0], localContraria[1], localContraria[2], 1f}, 0);
        float dx = worldContraria[0] - worldVista[0];
        float dy = worldContraria[1] - worldVista[1];
        float dz = worldContraria[2] - worldVista[2];
        float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len > 0) { dx /= len; dy /= len; dz /= len; }
        adj.distance = 5.0f;
        adj.centerX = worldVista[0] + dx * adj.distance;
        adj.centerY = worldVista[1] + dy * adj.distance;
        adj.centerZ = worldVista[2] + dz * adj.distance;
        adj.angleX = (float) Math.toDegrees(Math.atan2(dx, dz));
        adj.angleY = (float) Math.toDegrees(Math.asin(Math.max(-1.0f, Math.min(1.0f, dy))));
        adj.angleRoll = m.angleRoll;
        Toast.makeText(context, "Viendo Cámara: " + m.id, Toast.LENGTH_SHORT).show();
    }

    public void draw(float[] vpMatrix) {
        if (camaraMeshes == null || marcadores.isEmpty() || renderer.isExporting) return;
        GLES20.glUseProgram(program);
        int posHandle = GLES20.glGetAttribLocation(program, "aPosition");
        int colorHandle = GLES20.glGetAttribLocation(program, "aColor");
        int mvpHandle = GLES20.glGetUniformLocation(program, "uMVP");
        GLES20.glEnableVertexAttribArray(posHandle);
        GLES20.glEnableVertexAttribArray(colorHandle);
        float[] model = new float[16];
        float[] mvp = new float[16];
        for (MarcadorCamara m : marcadores) {
            Matrix.setIdentityM(model, 0);
            Matrix.translateM(model, 0, m.position[0], m.position[1], m.position[2]);
            Matrix.rotateM(model, 0, m.rotation[0], 1, 0, 0);
            Matrix.rotateM(model, 0, m.rotation[1], 0, 1, 0);
            Matrix.rotateM(model, 0, m.rotation[2], 0, 0, 1);
            Matrix.scaleM(model, 0, m.scale, m.scale, m.scale);
            Matrix.multiplyMM(mvp, 0, vpMatrix, 0, model, 0);
            GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvp, 0);
            for (ObjetosCargados.SubMesh sm : camaraMeshes) {
                sm.vertexBuffer.position(0);
                GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 0, sm.vertexBuffer);
                if (sm.colorBuffer != null) {
                    sm.colorBuffer.position(0);
                    GLES20.glVertexAttribPointer(colorHandle, 3, GLES20.GL_FLOAT, false, 0, sm.colorBuffer);
                }
                GLES20.glDrawElements(GLES20.GL_TRIANGLES, sm.numIndices, GLES20.GL_UNSIGNED_SHORT, sm.indexBuffer);
            }
        }
        GLES20.glDisableVertexAttribArray(posHandle);
        GLES20.glDisableVertexAttribArray(colorHandle);
    }

    private Hitbox createHitboxFromMesh(ObjetosCargados.SubMesh sm) {
        Hitbox hb = new Hitbox();
        FloatBuffer vb = sm.vertexBuffer.duplicate();
        vb.position(0);
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        while (vb.remaining() >= 3) {
            float x = vb.get(), y = vb.get(), z = vb.get();
            if (x < minX) minX = x; if (y < minY) minY = y; if (z < minZ) minZ = z;
            if (x > maxX) maxX = x; if (y > maxY) maxY = y; if (z > maxZ) maxZ = z;
        }
        hb.localCenter[0] = (minX + maxX) * 0.5f;
        hb.localCenter[1] = (minY + maxY) * 0.5f;
        hb.localCenter[2] = (minZ + maxZ) * 0.5f;
        hb.halfSize[0] = (maxX - minX) * 0.5f;
        hb.halfSize[1] = (maxY - minY) * 0.5f;
        hb.halfSize[2] = (maxZ - minZ) * 0.5f;
        return hb;
    }

    private int createProgram(String vs, String fs) {
        int vShader = loadShader(GLES20.GL_VERTEX_SHADER, vs);
        int fShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fs);
        int prog = GLES20.glCreateProgram();
        GLES20.glAttachShader(prog, vShader);
        GLES20.glAttachShader(prog, fShader);
        GLES20.glLinkProgram(prog);
        return prog;
    }

    private int loadShader(int type, String code) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);
        return shader;
    }

    public List<MarcadorCamara> getMarcadores() {
        return marcadores;
    }
    
    public void syncSelectedCamaraWithGizmo(Gismo gizmo) {
        if (selectedCamara == null || gizmo == null) return;
        System.arraycopy(gizmo.position, 0, selectedCamara.position, 0, 3);
        System.arraycopy(gizmo.rotation, 0, selectedCamara.rotation, 0, 3);
        selectedCamara.scale = gizmo.scale;
        selectedCamara.actualizarHitboxes();
    }
}
