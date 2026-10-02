package ar.axt.nopeby;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import ar.axt.animar.Bones;
import ar.axt.animar.HistorialMovimientos;
import ar.axt.animar.InteraccionGismo;
import ar.axt.controles.AjustesDeCamara;
import ar.axt.controles.Gismo;
import ar.axt.controles.RayosInteraccion;
import ar.axt.database.DatosProyectos;
import ar.axt.ficicas.Hitbox;
import ar.axt.leerobj.ObjetosCargados;
import ar.axt.materiales.ClimaLluvia;
import ar.axt.materiales.ClimaNieve;
import ar.axt.materiales.Explocion;
import ar.axt.materiales.Fuego;
import ar.axt.materiales.Gotas;
import ar.axt.materiales.Humo;
import ar.axt.materiales.Material;
import ar.axt.materiales.Polvo;
import ar.axt.materiales.Tierra;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;
import java.util.Random;
import ar.axt.sombras.RealShadow;
import ar.axt.sombras.ShadowAnime;
import ar.axt.sombras.SimpleShader;
import android.graphics.Bitmap;
import java.io.File;
import java.io.FileOutputStream;
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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyRenderer implements GLSurfaceView.Renderer {
    private static final float BG_A = 1.0f;
    private static final float BG_B = 0.208f;
    private static final float BG_G = 0.192f;
    private static final float BG_R = 0.18f;
    private static final float GRID_A = 1.0f;
    private static final float GRID_B = 1.0f;
    private static final float GRID_G = 1.0f;
    private static final float GRID_R = 1.0f;
    private static final int GRID_SIZE = 20;
    private static final float GRID_STEP = 1.0f;
    public static int surfaceHeight;
    public static int surfaceWidth;
    private int aPosHitbox;
    private int aPositionLocationGrid;
    public MainActivity activity;
    public AjustesDeCamara ajustesDeCamara;
    public Bones bone;
    private final Context context;
    public Explocion explocion;
    public Fuego fuego;
    public Gismo gizmo;
    public Gotas gotas;
    private FloatBuffer gridBuffer;
    private int gridProgram;
    private int gridVertexCount;
    public HistorialMovimientos historialMovimientos;
    private ShortBuffer hitboxIndexBuffer;
    private int hitboxIndexCount;
    private FloatBuffer hitboxVertexBuffer;
    public Humo humo;
    public InteraccionGismo interaccionGismo;
    private ClimaLluvia lluvia;
    public Material material;
    private ClimaNieve nieve;
    public DatosProyectos.ShaderSettings pendingSettings;
    public float[] skyColor = {0.18f, 0.18f, 0.18f};
    public float cloudDensity = 0.0f;
    public float starDensity = 0.0f;
    private FloatBuffer starVertexBuffer;
    private int starCount = 200;
    private FloatBuffer astroVertexBuffer;
    private FloatBuffer cloudVertexBuffer;
    private FloatBuffer quadTexBuffer;
    private int texSol = -1;
    private int[] texNubes = {-1, -1, -1, -1};
    private int skyProgram = -1;
    private int uMvpSky;
    private int uTextureSky;
    private int uColorTintSky;
    private int aPosSky;
    private int aTexCoordSky;
    private int numCloudQuads = 12;
    private int[] cloudTexIndices = new int[12];
    public Polvo polvo;
    public RayosInteraccion rayosInteraccion;
    private ShadowAnime shaderAnime;
    private RealShadow shaderRealista;
    private int simpleColorProgram;
    private SimpleShader simpleShader;
    public Tierra tierra;
    private int uColorHitbox;
    private int uColorLocationGrid;
    private int uMvpHitbox;
    private int uMvpLocationGrid;
    public final float[] projectionMatrix = new float[16];
    public final float[] viewMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    String objVs = "";
    String objFs = "";
    public final List<Mesh> meshes = new CopyOnWriteArrayList();
    public Mesh linkedMesh = null;
    private long lastFrameTime = 0;
    public int nievesino = 0;
    public int lluviasino = 0;
    public int elementoParticulas = 0;
    public int cantidadDeEfectos = 0;
    public final List<Material.EffectBox> loadedBoxes = new ArrayList();
    public ar.axt.controles.AdministradorCamaras administradorCamaras;
    private File pendingScreenshotFile = null;
    public int targetFBO = 0;
    public boolean isExporting = false;

    public void rebuildAllParticles() {
        this.activity.glSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (humo != null) humo.rebuildParticles();
                if (fuego != null) fuego.rebuildParticles();
                if (tierra != null) tierra.rebuildParticles();
                if (gotas != null) gotas.rebuildParticles();
                if (polvo != null) polvo.rebuildParticles();
                if (explocion != null) explocion.rebuildParticles();
            }
        });
    }

    public void requestScreenshot(File file) {
        this.pendingScreenshotFile = file;
    }

    public Bones getBones() {
        return this.bone;
    }

    public MyRenderer(MainActivity activity) {
        this.activity = activity;
        this.context = activity;
        this.administradorCamaras = new ar.axt.controles.AdministradorCamaras(this.context, this, activity);
        this.bone = new Bones(this.context);
        this.bone.renderer = this;
    }

    public RealShadow getShaderRealista() {
        return this.shaderRealista;
    }

    public ShadowAnime getShaderAnime() {
        return this.shaderAnime;
    }

    public static class Mesh {
        public Hitbox hitbox;
        public String name;
        public ObjetosCargados.SubMesh subMesh;
        public float[] translation = {0.0f, 0.0f, 0.0f};
        public float[] rotation = {0.0f, 0.0f, 0.0f};
        public float[] scale = {1.0f, 1.0f, 1.0f};
        public float specularStrength = 0.5f;
        public float shininess = 32.0f;
        public float opacity = 1.0f;
        public float lightRadiusMult = 0.0f;
        public float[] emissiveColor = {1.0f, 0.95f, 0.7f};

        public Mesh(ObjetosCargados.SubMesh subMesh) {
            this.subMesh = subMesh;
            if (subMesh != null) {
                this.name = subMesh.name;
            }
            this.hitbox = new Hitbox();
            if (subMesh != null && subMesh.vertexBuffer != null) {
                FloatBuffer vb = subMesh.vertexBuffer.duplicate();
                vb.position(0);
                float minX = Float.MAX_VALUE;
                float minY = Float.MAX_VALUE;
                float minZ = Float.MAX_VALUE;
                float maxX = -3.4028235E38f;
                float maxY = -3.4028235E38f;
                float maxZ = -3.4028235E38f;
                while (vb.remaining() >= 3) {
                    float x = vb.get();
                    float y = vb.get();
                    float z = vb.get();
                    minX = x < minX ? x : minX;
                    minY = y < minY ? y : minY;
                    minZ = z < minZ ? z : minZ;
                    maxX = x > maxX ? x : maxX;
                    maxY = y > maxY ? y : maxY;
                    if (z > maxZ) {
                        maxZ = z;
                    }
                }
                this.hitbox.localCenter[0] = (minX + maxX) * 0.5f;
                this.hitbox.localCenter[1] = (minY + maxY) * 0.5f;
                this.hitbox.localCenter[2] = (minZ + maxZ) * 0.5f;
                this.hitbox.halfSize[0] = (maxX - minX) * 0.5f;
                this.hitbox.halfSize[1] = (maxY - minY) * 0.5f;
                this.hitbox.halfSize[2] = (maxZ - minZ) * 0.5f;
            }
        }
    }

    public static class ObjEntry {
        public List<Mesh> meshes;
        public int objId;
        ObjEntry(int id, List<Mesh> meshes) {
            this.objId = id;
            this.meshes = meshes;
        }
    }

    public boolean isGizmoVisible() {
        return (this.rayosInteraccion.selectedMesh == null && this.bone.selectedBone == null && this.material.getSelectedBox() == null) ? false : true;
    }

    public void clearMeshes() {
        this.meshes.clear();
    }

    public void removeMesh(Mesh mesh) {
        if (mesh != null) { this.meshes.remove(mesh); }
    }

    public Mesh addMesh(ObjetosCargados.SubMesh sm) {
        if (sm == null) { return null; }
        Mesh mesh = new Mesh(sm);
        this.meshes.add(mesh);
        return mesh;
    }

    public void addMesh(Mesh mesh) {
        if (mesh == null) { return; }
        this.meshes.add(mesh);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        for (Mesh mesh : this.meshes) {
            if (mesh != null && mesh.subMesh != null) {
                mesh.subMesh.textureId = -1;
            }
        }
        GLES20.glClearColor(BG_R, BG_G, BG_B, 1.0f);
        GLES20.glEnable(2929);
        GLES20.glDepthFunc(515);
        GLES20.glEnable(3042);
        GLES20.glBlendFunc(770, 771);
        String vsGrid = loadShaderFromAssets("grid_vertex.glsl");
        String fsGrid = loadShaderFromAssets("grid_fragment.glsl");
        this.gridProgram = createProgram(vsGrid, fsGrid);
        this.aPositionLocationGrid = GLES20.glGetAttribLocation(this.gridProgram, "aPosition");
        this.uMvpLocationGrid = GLES20.glGetUniformLocation(this.gridProgram, "uMVP");
        this.uColorLocationGrid = GLES20.glGetUniformLocation(this.gridProgram, "uColor");
        createGridBuffer(20, 1.0f);
        Matrix.setIdentityM(this.modelMatrix, 0);
        this.gizmo = new Gismo(this.context);
        if (this.bone == null) {
            this.bone = new Bones(this.context);
        }
        this.bone.initShader();
        this.interaccionGismo = new InteraccionGismo(this, this.activity, this.historialMovimientos, null, this.gizmo);
        this.rayosInteraccion = new RayosInteraccion(this.activity, this, this.interaccionGismo, this.historialMovimientos);
        this.interaccionGismo.setRayos(this.rayosInteraccion);
        this.ajustesDeCamara = new AjustesDeCamara(this, this.activity);
        this.lluvia = new ClimaLluvia(this.context);
        this.nieve = new ClimaNieve(this.context);
        List<Material.EffectBox> boxesActuales = new ArrayList<>();
        if (this.material != null) {
            boxesActuales.addAll(this.material.getBoxes());
        }
        this.material = new Material(this.context, this);
        this.material.getBoxes().addAll(boxesActuales);
        this.material.getBoxes().addAll(this.loadedBoxes);
        this.loadedBoxes.clear();
        this.humo = new Humo(this.context, this.material);
        this.fuego = new Fuego(this.context, this.material);
        this.tierra = new Tierra(this.context, this.material);
        this.gotas = new Gotas(this.context, this.material);
        this.polvo = new Polvo(this.context, this.material);
        this.explocion = new Explocion(this.context, this.material);
        this.shaderRealista = new RealShadow(this.context);
        this.shaderRealista.init();
        this.shaderAnime = new ShadowAnime(this.context);
        this.shaderAnime.init();
        if (this.pendingSettings != null) {
            if (this.pendingSettings.skyColor != null) {
                System.arraycopy(this.pendingSettings.skyColor, 0, this.skyColor, 0, 3);
            }
            this.cloudDensity = this.pendingSettings.cloudDensity;
            this.starDensity = this.pendingSettings.starDensity;
            if (this.shaderRealista != null) {
                this.shaderRealista.lightIntensity = this.pendingSettings.lightIntensity;
                this.shaderRealista.shadowStrength = this.pendingSettings.shadowStrength;
                System.arraycopy(this.pendingSettings.lightColor, 0, this.shaderRealista.lightColor, 0, 3);
                System.arraycopy(this.pendingSettings.shadowColor, 0, this.shaderRealista.shadowColor, 0, 3);
            }
            if (this.shaderAnime != null) {
                this.shaderAnime.lightIntensity = this.pendingSettings.lightIntensity;
                this.shaderAnime.shadowStrength = this.pendingSettings.shadowStrength;
                System.arraycopy(this.pendingSettings.lightColor, 0, this.shaderAnime.lightColor, 0, 3);
                System.arraycopy(this.pendingSettings.shadowColor, 0, this.shaderAnime.shadowColor, 0, 3);
            }
        }
        this.simpleShader = new SimpleShader(this.context);
        this.simpleShader.load("simple_vertex.glsl", "simple_fragment.glsl");
        if (this.bone != null) {
            this.bone.initShader();
        }
        Log.i("OpenGL", "onSurfaceCreated completado. Shaders cargados correctamente.");
        String vsHitbox = loadShaderFromAssets("hitbox_vertex.glsl");
        String fsHitbox = loadShaderFromAssets("hitbox_fragment.glsl");
        this.simpleColorProgram = createProgram(vsHitbox, fsHitbox);
        this.uMvpHitbox = GLES20.glGetUniformLocation(this.simpleColorProgram, "uMVP");
        this.uColorHitbox = GLES20.glGetUniformLocation(this.simpleColorProgram, "uColor");
        this.aPosHitbox = GLES20.glGetAttribLocation(this.simpleColorProgram, "aPosition");
        initHitboxGeometry();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        updateProjection(width, height);
    }

    public void updateProjection(int width, int height) {
        surfaceWidth = width;
        surfaceHeight = height;
        if (this.lluvia != null) this.lluvia.setProjection(width, height);
        if (this.nieve != null) this.nieve.setProjection(width, height);
        GLES20.glViewport(0, 0, width, height);
        float aspect = (float) width / height;
        float fovY = 45.0f;
        float near = 0.1f;
        float far = 100.0f;
        float top = (float) (near * Math.tan(Math.toRadians(fovY / 2.0d)));
        float bottom = -top;
        float left = bottom * aspect;
        float right = top * aspect;
        Matrix.frustumM(this.projectionMatrix, 0, left, right, bottom, top, near, far);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        for (Mesh m : this.meshes) {
            if (m != null && m.subMesh != null) {
                if (m.subMesh.textureId == -1 && m.subMesh.embeddedTexture != null && m.subMesh.pendingTexture == null) {
                    try {
                        ar.axt.leerobj.TexturaLoader loader = new ar.axt.leerobj.TexturaLoader(this.context);
                        m.subMesh.pendingTexture = loader.loadFromGlb(m.subMesh.embeddedTexture, m.subMesh.embeddedTextureMimeType);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (m.subMesh.pendingTexture != null) {
                    ar.axt.leerobj.TexturaLoader loader = new ar.axt.leerobj.TexturaLoader(this.context);
                    int newId = loader.loadTextureToGL(m.subMesh.pendingTexture);
                    if (newId != -1) { m.subMesh.textureId = newId; }
                    m.subMesh.pendingTexture.recycle();
                    m.subMesh.pendingTexture = null;
                }
                if (m.subMesh.normalMapTextureId == -1 && m.subMesh.embeddedNormalTexture != null && m.subMesh.pendingNormalMap == null) {
                    try {
                        ar.axt.leerobj.TexturaLoader loader = new ar.axt.leerobj.TexturaLoader(this.context);
                        m.subMesh.pendingNormalMap = loader.loadFromGlb(m.subMesh.embeddedNormalTexture, m.subMesh.embeddedNormalMimeType);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (m.subMesh.pendingNormalMap != null) {
                    ar.axt.leerobj.TexturaLoader loader = new ar.axt.leerobj.TexturaLoader(this.context);
                    int newId = loader.loadTextureToGL(m.subMesh.pendingNormalMap);
                    if (newId != -1) { m.subMesh.normalMapTextureId = newId; }
                    m.subMesh.pendingNormalMap.recycle();
                    m.subMesh.pendingNormalMap = null;
                }
            }
        }
        Hitbox.actualizarTodo(this.meshes, this.gizmo, this.bone);
        long currentTime = System.currentTimeMillis();
        float delta = (currentTime - this.lastFrameTime) / 1000.0f;
        this.lastFrameTime = currentTime;
        if (this.activity.CualShader == 1) {
            this.shaderRealista.updateLightMatrix(this.ajustesDeCamara.sceneCenter, this.ajustesDeCamara.sceneRadius);
            float[] lightMatrix = this.shaderRealista.getLightSpaceMatrix();
            GLES20.glDisable(3042);
            GLES20.glBindFramebuffer(36160, this.shaderRealista.getShadowFramebuffer());
            GLES20.glViewport(0, 0, 1024, 1024);
            GLES20.glClear(16640);
            int status = GLES20.glCheckFramebufferStatus(36160);
            if (status != 36053) { GLES20.glBindFramebuffer(36160, targetFBO); return; }
            PasoDeSombraRealista(lightMatrix);
            GLES20.glBindFramebuffer(36160, targetFBO);
            GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);
            GLES20.glClearColor(this.skyColor[0], this.skyColor[1], this.skyColor[2], 1.0f);
            GLES20.glClear(16640);
            float radX = (float) Math.toRadians(this.ajustesDeCamara.angleX);
            float radY = (float) Math.toRadians(this.ajustesDeCamara.angleY);
            double d = this.ajustesDeCamara.centerX;
            double d2 = this.ajustesDeCamara.distance;
            double dCos = Math.cos(radY);
            Double.isNaN(d2);
            double dSin = d2 * dCos * Math.sin(radX);
            Double.isNaN(d);
            float eyeX = (float) (d + dSin);
            double d3 = this.ajustesDeCamara.centerY;
            double d4 = this.ajustesDeCamara.distance;
            double dSin2 = Math.sin(radY);
            Double.isNaN(d4);
            Double.isNaN(d3);
            float eyeY = (float) (d3 + (d4 * dSin2));
            double d5 = this.ajustesDeCamara.centerZ;
            double d6 = this.ajustesDeCamara.distance;
            double dCos2 = Math.cos(radY);
            Double.isNaN(d6);
            double dCos3 = d6 * dCos2 * Math.cos(radX);
            Double.isNaN(d5);
            float eyeZ = (float) (d5 + dCos3);
            float[] up = this.ajustesDeCamara.getUpVector();
            Matrix.setLookAtM(this.viewMatrix, 0, eyeX, eyeY, eyeZ, this.ajustesDeCamara.centerX, this.ajustesDeCamara.centerY, this.ajustesDeCamara.centerZ, up[0], up[1], up[2]);
            Matrix.multiplyMM(this.mvpMatrix, 0, this.viewMatrix, 0, this.modelMatrix, 0);
            Matrix.multiplyMM(this.mvpMatrix, 0, this.projectionMatrix, 0, this.mvpMatrix, 0);
            if (!isExporting) drawGrid();
            float[] vpMatrix = new float[16];
            Matrix.multiplyMM(vpMatrix, 0, this.projectionMatrix, 0, this.viewMatrix, 0);
            dibujarCielo(vpMatrix);
            this.material.draw(vpMatrix);
            this.humo.setViewMatrix(this.viewMatrix);
            this.humo.setProjectionMatrix(this.projectionMatrix);
            this.humo.actualizar(delta);
            this.humo.dibujar();
            this.fuego.setViewMatrix(this.viewMatrix);
            this.fuego.setProjectionMatrix(this.projectionMatrix);
            this.fuego.actualizar(delta);
            this.fuego.dibujar();
            this.tierra.setViewMatrix(this.viewMatrix);
            this.tierra.setProjectionMatrix(this.projectionMatrix);
            this.tierra.actualizar(delta);
            this.tierra.dibujar();
            this.gotas.setViewMatrix(this.viewMatrix);
            this.gotas.setProjectionMatrix(this.projectionMatrix);
            this.gotas.actualizar(delta);
            this.gotas.dibujar();
            this.polvo.setViewMatrix(this.viewMatrix);
            this.polvo.setProjectionMatrix(this.projectionMatrix);
            this.polvo.actualizar(delta);
            this.polvo.dibujar();
            this.explocion.setViewMatrix(this.viewMatrix);
            this.explocion.setProjectionMatrix(this.projectionMatrix);
            this.explocion.actualizar(delta);
            this.explocion.dibujar();
            if (this.lluviasino == 1) {
                this.lluvia.actualizar(delta);
                this.lluvia.dibujar();
            }
            if (this.nievesino == 1) {
                this.nieve.actualizar(delta);
                this.nieve.dibujar();
            }
            PasoDeLuzRealista(lightMatrix, this.shaderRealista.getShadowTexture());
            GLES20.glDisable(2929);
            GLES20.glDepthMask(false);
            GLES20.glEnable(3042);
            GLES20.glBlendFunc(770, 771);
            GLES20.glDisable(2884);
            GLES20.glLineWidth(2.0f);
            int cantidad = this.activity.huesosarenderizar();
            this.bone.renderAll(this.viewMatrix, this.projectionMatrix, cantidad);
            if (this.administradorCamaras != null) { this.administradorCamaras.draw(vpMatrix); }
            if (this.gizmo != null && isGizmoVisible()) {
                Matrix.multiplyMM(vpMatrix, 0, this.projectionMatrix, 0, this.viewMatrix, 0);
                this.gizmo.setPosition(this.interaccionGismo.gizmoPosition[0], this.interaccionGismo.gizmoPosition[1], this.interaccionGismo.gizmoPosition[2]);
                this.gizmo.draw(vpMatrix);
            }
            GLES20.glDepthMask(true);
            GLES20.glDisable(3042);
            GLES20.glEnable(2929);
            if (this.pendingScreenshotFile != null) {
                saveFrameAsPng(this.pendingScreenshotFile);
                this.pendingScreenshotFile = null;
            } return;
        }
        if (this.activity.CualShader == 3) {
            this.shaderAnime.updateLightMatrix(this.ajustesDeCamara.sceneCenter, this.ajustesDeCamara.sceneRadius);
            float[] lightMatrix2 = this.shaderAnime.getLightSpaceMatrix();
            GLES20.glDisable(3042);
            GLES20.glBindFramebuffer(36160, this.shaderAnime.getShadowFramebuffer());
            GLES20.glViewport(0, 0, 1024, 1024);
            GLES20.glClear(16640);
            int status2 = GLES20.glCheckFramebufferStatus(36160);
            if (status2 != 36053) {
                GLES20.glBindFramebuffer(36160, targetFBO);
                return;
            }
            PasoDeSombraAnime(lightMatrix2);
            GLES20.glBindFramebuffer(36160, targetFBO);
            GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);
            GLES20.glClearColor(this.skyColor[0], this.skyColor[1], this.skyColor[2], 1.0f);
            GLES20.glClear(16640);
            float radX2 = (float) Math.toRadians(this.ajustesDeCamara.angleX);
            float radY2 = (float) Math.toRadians(this.ajustesDeCamara.angleY);
            double d7 = this.ajustesDeCamara.centerX;
            double d8 = this.ajustesDeCamara.distance;
            double dCos4 = Math.cos(radY2);
            Double.isNaN(d8);
            double dSin3 = d8 * dCos4 * Math.sin(radX2);
            Double.isNaN(d7);
            float eyeX2 = (float) (d7 + dSin3);
            double d9 = this.ajustesDeCamara.centerY;
            double d10 = this.ajustesDeCamara.distance;
            double dSin4 = Math.sin(radY2);
            Double.isNaN(d10);
            Double.isNaN(d9);
            float eyeY2 = (float) (d9 + (d10 * dSin4));
            double d11 = this.ajustesDeCamara.centerZ;
            double d12 = this.ajustesDeCamara.distance;
            double dCos5 = Math.cos(radY2);
            Double.isNaN(d12);
            double dCos6 = d12 * dCos5 * Math.cos(radX2);
            Double.isNaN(d11);
            float eyeZ2 = (float) (d11 + dCos6);
            float[] up2 = this.ajustesDeCamara.getUpVector();
            Matrix.setLookAtM(this.viewMatrix, 0, eyeX2, eyeY2, eyeZ2, this.ajustesDeCamara.centerX, this.ajustesDeCamara.centerY, this.ajustesDeCamara.centerZ, up2[0], up2[1], up2[2]);
            Matrix.multiplyMM(this.mvpMatrix, 0, this.viewMatrix, 0, this.modelMatrix, 0);
            Matrix.multiplyMM(this.mvpMatrix, 0, this.projectionMatrix, 0, this.mvpMatrix, 0);
            if (!isExporting) drawGrid();
            float[] vpMatrix3 = new float[16];
            Matrix.multiplyMM(vpMatrix3, 0, this.projectionMatrix, 0, this.viewMatrix, 0);
            dibujarCielo(vpMatrix3);
            this.material.draw(vpMatrix3);
            this.humo.setViewMatrix(this.viewMatrix);
            this.humo.setProjectionMatrix(this.projectionMatrix);
            this.humo.actualizar(delta);
            this.humo.dibujar();
            this.fuego.setViewMatrix(this.viewMatrix);
            this.fuego.setProjectionMatrix(this.projectionMatrix);
            this.fuego.actualizar(delta);
            this.fuego.dibujar();
            this.tierra.setViewMatrix(this.viewMatrix);
            this.tierra.setProjectionMatrix(this.projectionMatrix);
            this.tierra.actualizar(delta);
            this.tierra.dibujar();
            this.gotas.setViewMatrix(this.viewMatrix);
            this.gotas.setProjectionMatrix(this.projectionMatrix);
            this.gotas.actualizar(delta);
            this.gotas.dibujar();
            this.polvo.setViewMatrix(this.viewMatrix);
            this.polvo.setProjectionMatrix(this.projectionMatrix);
            this.polvo.actualizar(delta);
            this.polvo.dibujar();
            this.explocion.setViewMatrix(this.viewMatrix);
            this.explocion.setProjectionMatrix(this.projectionMatrix);
            this.explocion.actualizar(delta);
            this.explocion.dibujar();
            if (this.lluviasino == 1) {
                this.lluvia.actualizar(delta);
                this.lluvia.dibujar();
            }
            if (this.nievesino == 1) {
                this.nieve.actualizar(delta);
                this.nieve.dibujar();
            }
            PasoDeLuzAnime(lightMatrix2, this.shaderAnime.getShadowTexture());
            GLES20.glDisable(2929);
            GLES20.glDepthMask(false);
            GLES20.glEnable(3042);
            GLES20.glBlendFunc(770, 771);
            GLES20.glDisable(2884);
            GLES20.glLineWidth(2.0f);
            int cantidad2 = this.activity.huesosarenderizar();
            this.bone.renderAll(this.viewMatrix, this.projectionMatrix, cantidad2);
            if (this.gizmo != null && isGizmoVisible()) {
                float[] vpMatrix2 = new float[16];
                Matrix.multiplyMM(vpMatrix2, 0, this.projectionMatrix, 0, this.viewMatrix, 0);
                this.gizmo.setPosition(this.interaccionGismo.gizmoPosition[0], this.interaccionGismo.gizmoPosition[1], this.interaccionGismo.gizmoPosition[2]);
                this.gizmo.draw(vpMatrix2);
            }
            GLES20.glDepthMask(true); GLES20.glDisable(3042); GLES20.glEnable(2929);
            if (this.pendingScreenshotFile != null) {
                saveFrameAsPng(this.pendingScreenshotFile);
                this.pendingScreenshotFile = null;
            } return;
        }
        if (this.activity.CualShader == 2) {
            GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);
            GLES20.glClearColor(this.skyColor[0], this.skyColor[1], this.skyColor[2], 1.0f);
            GLES20.glClear(16640);
            float radX3 = (float) Math.toRadians(this.ajustesDeCamara.angleX);
            float radY3 = (float) Math.toRadians(this.ajustesDeCamara.angleY);
            double d13 = this.ajustesDeCamara.centerX;
            double d14 = this.ajustesDeCamara.distance;
            double dCos7 = Math.cos(radY3);
            Double.isNaN(d14);
            double dSin5 = d14 * dCos7 * Math.sin(radX3);
            Double.isNaN(d13);
            float eyeX3 = (float) (d13 + dSin5);
            double d15 = this.ajustesDeCamara.centerY;
            double d16 = this.ajustesDeCamara.distance;
            double dSin6 = Math.sin(radY3);
            Double.isNaN(d16);
            Double.isNaN(d15);
            float eyeY3 = (float) (d15 + (d16 * dSin6));
            double d17 = this.ajustesDeCamara.centerZ;
            double d18 = this.ajustesDeCamara.distance;
            double dCos8 = Math.cos(radY3);
            Double.isNaN(d18);
            double dCos9 = d18 * dCos8 * Math.cos(radX3);
            Double.isNaN(d17);
            float eyeZ3 = (float) (d17 + dCos9);
            float[] up3 = this.ajustesDeCamara.getUpVector();
            Matrix.setLookAtM(this.viewMatrix, 0, eyeX3, eyeY3, eyeZ3, this.ajustesDeCamara.centerX, this.ajustesDeCamara.centerY, this.ajustesDeCamara.centerZ, up3[0], up3[1], up3[2]);
            Matrix.multiplyMM(this.mvpMatrix, 0, this.viewMatrix, 0, this.modelMatrix, 0);
            Matrix.multiplyMM(this.mvpMatrix, 0, this.projectionMatrix, 0, this.mvpMatrix, 0);
            if (!isExporting) drawGrid();
            float[] vpMatrix4 = new float[16];
            Matrix.multiplyMM(vpMatrix4, 0, this.projectionMatrix, 0, this.viewMatrix, 0);
            dibujarCielo(vpMatrix4);
            this.material.draw(vpMatrix4);
            this.humo.setViewMatrix(this.viewMatrix);
            this.humo.setProjectionMatrix(this.projectionMatrix);
            this.humo.actualizar(delta);
            this.humo.dibujar();
            this.fuego.setViewMatrix(this.viewMatrix);
            this.fuego.setProjectionMatrix(this.projectionMatrix);
            this.fuego.actualizar(delta);
            this.fuego.dibujar();
            this.tierra.setViewMatrix(this.viewMatrix);
            this.tierra.setProjectionMatrix(this.projectionMatrix);
            this.tierra.actualizar(delta);
            this.tierra.dibujar();
            this.gotas.setViewMatrix(this.viewMatrix);
            this.gotas.setProjectionMatrix(this.projectionMatrix);
            this.gotas.actualizar(delta);
            this.gotas.dibujar();
            this.polvo.setViewMatrix(this.viewMatrix);
            this.polvo.setProjectionMatrix(this.projectionMatrix);
            this.polvo.actualizar(delta);
            this.polvo.dibujar();
            this.explocion.setViewMatrix(this.viewMatrix);
            this.explocion.setProjectionMatrix(this.projectionMatrix);
            this.explocion.actualizar(delta);
            this.explocion.dibujar();
            if (this.lluviasino == 1) { this.lluvia.actualizar(delta); this.lluvia.dibujar(); }
            if (this.nievesino == 1) { this.nieve.actualizar(delta); this.nieve.dibujar(); }
            RenderSimple(this.viewMatrix, this.projectionMatrix);
            GLES20.glDisable(2929);
            GLES20.glDepthMask(false);
            GLES20.glEnable(3042);
            GLES20.glBlendFunc(770, 771);
            GLES20.glDisable(2884);
            GLES20.glLineWidth(2.0f);
            int cantidad3 = this.activity.huesosarenderizar();
            this.bone.renderAll(this.viewMatrix, this.projectionMatrix, cantidad3);
            if (this.gizmo != null && isGizmoVisible()) {
                float[] vpMatrix3 = new float[16];
                Matrix.multiplyMM(vpMatrix3, 0, this.projectionMatrix, 0, this.viewMatrix, 0);
                this.gizmo.setPosition(this.interaccionGismo.gizmoPosition[0], this.interaccionGismo.gizmoPosition[1], this.interaccionGismo.gizmoPosition[2]);
                this.gizmo.draw(vpMatrix3);
            }
            GLES20.glDepthMask(true); GLES20.glDisable(3042); GLES20.glEnable(2929);
            if (this.pendingScreenshotFile != null) {
                saveFrameAsPng(this.pendingScreenshotFile);
                this.pendingScreenshotFile = null;
            }
        }
    }

    private void saveFrameAsPng(File file) {
        int width = surfaceWidth;
        int height = surfaceHeight;
        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);
        buffer.order(ByteOrder.nativeOrder());
        GLES20.glPixelStorei(GLES20.GL_PACK_ALIGNMENT, 1);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[width * height];
        buffer.asIntBuffer().get(pixels);
        int[] correctedPixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[(y * width) + x];
                int r = (pixel & 0xFF);
                int g = (pixel >> 8) & 0xFF;
                int b = (pixel >> 16) & 0xFF;
                int correctedPixel = (0xFF << 24) | (r << 16) | (g << 8) | b;
                correctedPixels[((height - y - 1) * width) + x] = correctedPixel;
            }
        }
        bitmap.setPixels(correctedPixels, 0, width, 0, 0, width, height);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            Log.d("Screenshot", "Screenshot saved (RGB Opaco) to: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e("Screenshot", "Error saving screenshot", e);
        } finally {
            if (fos != null) { try { fos.close();
                } catch (IOException ignored) {}
            } bitmap.recycle();
        }
    }

    private void createGridBuffer(int halfSize, float step) {
        int linesPerDir = ((int) ((halfSize * 2) / step)) + 1;
        int totalPoints = linesPerDir * 2 * 2;
        float[] vertices = new float[totalPoints * 3];
        int idx = 0;
        int i = -halfSize;
        while (i <= halfSize) {
            float z = i;
            int idx2 = idx + 1;
            vertices[idx] = -halfSize;
            int idx3 = idx2 + 1;
            vertices[idx2] = 0.0f;
            int idx4 = idx3 + 1;
            vertices[idx3] = z;
            int idx5 = idx4 + 1;
            vertices[idx4] = halfSize;
            int idx6 = idx5 + 1;
            vertices[idx5] = 0.0f;
            idx = idx6 + 1;
            vertices[idx6] = z;
            i += (int) step;
        }
        int i2 = -halfSize;
        while (i2 <= halfSize) {
            float x = i2;
            int idx7 = idx + 1;
            vertices[idx] = x;
            int idx8 = idx7 + 1;
            vertices[idx7] = 0.0f;
            int idx9 = idx8 + 1;
            vertices[idx8] = -halfSize;
            int idx10 = idx9 + 1;
            vertices[idx9] = x;
            int idx11 = idx10 + 1;
            vertices[idx10] = 0.0f;
            idx = idx11 + 1;
            vertices[idx11] = halfSize;
            i2 += (int) step;
        }
        int i3 = vertices.length;
        ByteBuffer bb = ByteBuffer.allocateDirect(i3 * 4);
        bb.order(ByteOrder.nativeOrder());
        this.gridBuffer = bb.asFloatBuffer();
        this.gridBuffer.put(vertices);
        this.gridBuffer.position(0);
        this.gridVertexCount = vertices.length / 3;
    }

    private void drawGrid() {
        GLES20.glUseProgram(this.gridProgram);
        Matrix.setIdentityM(this.modelMatrix, 0);
        Matrix.scaleM(this.modelMatrix, 0, 5.0f, 1.0f, 5.0f);
        float[] temp = new float[16];
        Matrix.multiplyMM(temp, 0, this.viewMatrix, 0, this.modelMatrix, 0);
        Matrix.multiplyMM(this.mvpMatrix, 0, this.projectionMatrix, 0, temp, 0);
        GLES20.glUniform4f(this.uColorLocationGrid, 1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glUniformMatrix4fv(this.uMvpLocationGrid, 1, false, this.mvpMatrix, 0);
        this.gridBuffer.position(0);
        GLES20.glEnableVertexAttribArray(this.aPositionLocationGrid);
        GLES20.glVertexAttribPointer(this.aPositionLocationGrid, 3, 5126, false, 0, (Buffer) this.gridBuffer);
        GLES20.glDrawArrays(1, 0, this.gridVertexCount);
        GLES20.glDisableVertexAttribArray(this.aPositionLocationGrid);
        GLES20.glUseProgram(0);
    }

    private void PasoDeSombraRealista(float[] lightMatrix) {
        int program;
        if (this.shaderRealista == null || this.meshes.isEmpty() || (program = this.shaderRealista.depthProgram) == 0) {
            return;
        }
        GLES20.glUseProgram(program);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_FRONT);
        int uLightSpaceMatrix = GLES20.glGetUniformLocation(program, "uLightSpaceMatrix");
        int uModel = GLES20.glGetUniformLocation(program, "uModel");
        int aPos = GLES20.glGetAttribLocation(program, "aPosition");
        GLES20.glUniformMatrix4fv(uLightSpaceMatrix, 1, false, lightMatrix, 0);
        for (Mesh m : this.meshes) {
            if (m != null && m.subMesh.vertexBuffer != null && m.subMesh.indexBuffer != null && m.subMesh.numIndices > 0) {
                float[] localModel = new float[16];
                Matrix.setIdentityM(localModel, 0);
                Matrix.translateM(localModel, 0, m.translation[0], m.translation[1], m.translation[2]);
                Matrix.rotateM(localModel, 0, m.rotation[0], 1.0f, 0.0f, 0.0f);
                Matrix.rotateM(localModel, 0, m.rotation[1], 0.0f, 1.0f, 0.0f);
                Matrix.rotateM(localModel, 0, m.rotation[2], 0.0f, 0.0f, 1.0f);
                Matrix.scaleM(localModel, 0, m.scale[0], m.scale[1], m.scale[2]);
                GLES20.glUniformMatrix4fv(uModel, 1, false, localModel, 0);
                GLES20.glEnableVertexAttribArray(aPos);
                m.subMesh.vertexBuffer.position(0);
                m.subMesh.indexBuffer.position(0);
                GLES20.glVertexAttribPointer(aPos, 3, 5126, false, 0, (Buffer) m.subMesh.vertexBuffer);
                GLES20.glDrawElements(4, m.subMesh.numIndices, 5123, m.subMesh.indexBuffer);
                GLES20.glDisableVertexAttribArray(aPos);
            }
        }
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glUseProgram(0);
    }

    private void PasoDeLuzRealista(float[] lightMatrix, int shadowTex) {
        int program;
        MyRenderer myRenderer = this;
        if (myRenderer.shaderRealista == null || myRenderer.meshes.isEmpty() || (program = myRenderer.shaderRealista.lightProgram) == 0) {
            return;
        }
        GLES20.glUseProgram(program);
        int uMVP = GLES20.glGetUniformLocation(program, "uMVP");
        int uModel = GLES20.glGetUniformLocation(program, "uModel");
        int uLightMatrix = GLES20.glGetUniformLocation(program, "uLightSpaceMatrix");
        int uShadowMap = GLES20.glGetUniformLocation(program, "uShadowMap");
        int uLightPos = GLES20.glGetUniformLocation(program, "uLightPos");
        int uViewPos = GLES20.glGetUniformLocation(program, "uViewPos");
        int uLightIntensity = GLES20.glGetUniformLocation(program, "uLightIntensity");
        int uShadowStrength = GLES20.glGetUniformLocation(program, "uShadowStrength");
        int uLightColor = GLES20.glGetUniformLocation(program, "uLightColor");
        int uShadowColor = GLES20.glGetUniformLocation(program, "uShadowColor");
        int aPos = GLES20.glGetAttribLocation(program, "aPosition");
        int aNormal = GLES20.glGetAttribLocation(program, "aNormal");
        int aColor = GLES20.glGetAttribLocation(program, "aColor");
        GLES20.glActiveTexture(33984);
        GLES20.glBindTexture(3553, shadowTex);
        GLES20.glUniform1i(uShadowMap, 0);
        float[] lightPos = myRenderer.shaderRealista.getLightPosition();
        GLES20.glUniform3f(uLightPos, lightPos[0], lightPos[1], lightPos[2]);
        GLES20.glUniform1f(uLightIntensity, myRenderer.shaderRealista.lightIntensity);
        GLES20.glUniform1f(uShadowStrength, myRenderer.shaderRealista.shadowStrength);
        GLES20.glUniform3fv(uLightColor, 1, myRenderer.shaderRealista.lightColor, 0);
        GLES20.glUniform3fv(uShadowColor, 1, myRenderer.shaderRealista.shadowColor, 0);
        float[] activePtPos = {0f, 0f, 0f};
        float activePtRadius = 0.0f;
        float[] activePtColor = {0f, 0f, 0f};
        for (Mesh meshCandidate : this.meshes) {
            if (meshCandidate != null && meshCandidate.lightRadiusMult > 0.01f) {
                ar.axt.sombras.CancelarIluminacion.EsferaLuz esfera = 
                        ar.axt.sombras.CancelarIluminacion.calcularEsferaLuz(meshCandidate);
                activePtPos = esfera.centroWorld;
                activePtRadius = esfera.radioEfectivo;
                activePtColor = esfera.colorLuz;
                break;
            }
        }
        int uPtPosLoc = GLES20.glGetUniformLocation(program, "uPointLightPos");
        int uPtRadLoc = GLES20.glGetUniformLocation(program, "uPointLightRadius");
        int uPtColLoc = GLES20.glGetUniformLocation(program, "uPointLightColor");
        if (uPtPosLoc >= 0) GLES20.glUniform3f(uPtPosLoc, activePtPos[0], activePtPos[1], activePtPos[2]);
        if (uPtRadLoc >= 0) GLES20.glUniform1f(uPtRadLoc, activePtRadius);
        if (uPtColLoc >= 0) GLES20.glUniform3f(uPtColLoc, activePtColor[0], activePtColor[1], activePtColor[2]);
        float[] invView = new float[16];
        float[] eyePos = new float[4];
        Matrix.invertM(invView, 0, myRenderer.viewMatrix, 0);
        Matrix.multiplyMV(eyePos, 0, invView, 0, new float[]{0.0f, 0.0f, 0.0f, 1.0f}, 0);
        GLES20.glUniform3f(uViewPos, eyePos[0], eyePos[1], eyePos[2]);
        Iterator<Mesh> it = myRenderer.meshes.iterator();
        while (it.hasNext()) {
            Mesh m = it.next();
            if (m == null || m.subMesh.vertexBuffer == null || m.subMesh.indexBuffer == null) {
                myRenderer = this;
                it = it;
            } else if (m.subMesh.numIndices > 0) {
                float[] eyePos2 = eyePos;
                float[] eyePos3 = new float[16];
                Matrix.setIdentityM(eyePos3, 0);
                Iterator<Mesh> it2 = it;
                int uViewPos2 = uViewPos;
                int uLightIntensity2 = uLightIntensity;
                Matrix.translateM(eyePos3, 0, m.translation[0], m.translation[1], m.translation[2]);
                Matrix.rotateM(eyePos3, 0, m.rotation[0], 1.0f, 0.0f, 0.0f);
                Matrix.rotateM(eyePos3, 0, m.rotation[1], 0.0f, 1.0f, 0.0f);
                Matrix.rotateM(eyePos3, 0, m.rotation[2], 0.0f, 0.0f, 1.0f);
                Matrix.scaleM(eyePos3, 0, m.scale[0], m.scale[1], m.scale[2]);
                float[] finalModel = new float[16];
                System.arraycopy(eyePos3, 0, finalModel, 0, 16);
                float[] normalMatrix4 = new float[16];
                float[] localModel = new float[16];
                Matrix.invertM(localModel, 0, finalModel, 0);
                Matrix.transposeM(normalMatrix4, 0, localModel, 0);
                float[] pv = new float[16];
                Matrix.multiplyMM(pv, 0, myRenderer.viewMatrix, 0, finalModel, 0);
                float[] modelInv = new float[16];
                Matrix.multiplyMM(modelInv, 0, myRenderer.projectionMatrix, 0, pv, 0);
                GLES20.glUniformMatrix4fv(uMVP, 1, false, modelInv, 0);
                GLES20.glUniformMatrix4fv(uModel, 1, false, finalModel, 0);
                GLES20.glUniformMatrix4fv(uLightMatrix, 1, false, lightMatrix, 0);
                int uNormalMatrix = GLES20.glGetUniformLocation(program, "uNormalMatrix");
                GLES20.glUniformMatrix4fv(uNormalMatrix, 1, false, normalMatrix4, 0);
                int uSpecStr = GLES20.glGetUniformLocation(program, "uSpecularStrength");
                int uShininess = GLES20.glGetUniformLocation(program, "uShininess");
                int uAlphaLoc = GLES20.glGetUniformLocation(program, "uAlpha");
                if (uSpecStr >= 0) GLES20.glUniform1f(uSpecStr, m.specularStrength);
                if (uShininess >= 0) GLES20.glUniform1f(uShininess, m.shininess);
                if (uAlphaLoc >= 0) GLES20.glUniform1f(uAlphaLoc, m.opacity);
                int uHasTex = GLES20.glGetUniformLocation(program, "uHasTexture");
                int uTex = GLES20.glGetUniformLocation(program, "uTexture");
                int aTex = GLES20.glGetAttribLocation(program, "aTexCoord");
                if (m.subMesh.textureId != -1) {
                    GLES20.glUniform1i(uHasTex, 1);
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, m.subMesh.textureId);
                    GLES20.glUniform1i(uTex, 1);
                    if (m.subMesh.texcoordBuffer != null) {
                        GLES20.glEnableVertexAttribArray(aTex);
                        m.subMesh.texcoordBuffer.position(0);
                        GLES20.glVertexAttribPointer(aTex, 2, GLES20.GL_FLOAT, false, 0, m.subMesh.texcoordBuffer);
                    }
                } else {
                    GLES20.glUniform1i(uHasTex, 0);
                }
                int uHasNormMap = GLES20.glGetUniformLocation(program, "uHasNormalMap");
                int uNormMap = GLES20.glGetUniformLocation(program, "uNormalMap");
                if (m.subMesh.normalMapTextureId != -1) {
                    if (uHasNormMap >= 0) GLES20.glUniform1i(uHasNormMap, 1);
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, m.subMesh.normalMapTextureId);
                    if (uNormMap >= 0) GLES20.glUniform1i(uNormMap, 2);
                } else {
                    if (uHasNormMap >= 0) GLES20.glUniform1i(uHasNormMap, 0);
                }
                int program2 = program;
                GLES20.glEnableVertexAttribArray(aPos);
                m.subMesh.vertexBuffer.position(0);
                m.subMesh.indexBuffer.position(0);
                float[] invView2 = invView;
                GLES20.glVertexAttribPointer(aPos, 3, 5126, false, 0, (Buffer) m.subMesh.vertexBuffer);
                if (m.subMesh.normalBuffer != null && aNormal >= 0) {
                    GLES20.glEnableVertexAttribArray(aNormal);
                    GLES20.glVertexAttribPointer(aNormal, 3, 5126, false, 0, (Buffer) m.subMesh.normalBuffer);
                }
                if (m.subMesh.colorBuffer != null && aColor >= 0) {
                    GLES20.glEnableVertexAttribArray(aColor);
                    GLES20.glVertexAttribPointer(aColor, 3, 5126, false, 0, (Buffer) m.subMesh.colorBuffer);
                }
                GLES20.glDrawElements(4, m.subMesh.numIndices, 5123, m.subMesh.indexBuffer);
                GLES20.glDisableVertexAttribArray(aPos);
                if (aNormal >= 0) { GLES20.glDisableVertexAttribArray(aNormal); }
                if (aColor >= 0) { GLES20.glDisableVertexAttribArray(aColor); }
                if (m.subMesh.textureId != -1) {
                    GLES20.glDisableVertexAttribArray(aTex);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
                }
                myRenderer = this;
                eyePos = eyePos2;
                it = it2;
                uViewPos = uViewPos2;
                uLightIntensity = uLightIntensity2;
                program = program2;
                invView = invView2;
            }
        } GLES20.glUseProgram(0);
    }

    private void PasoDeSombraAnime(float[] lightMatrix) {
        int program;
        if (this.shaderAnime == null || this.meshes.isEmpty() || (program = this.shaderAnime.depthProgram) == 0) {
            return;
        }
        GLES20.glUseProgram(program);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_FRONT);
        int uLightSpaceMatrix = GLES20.glGetUniformLocation(program, "uLightSpaceMatrix");
        int uModel = GLES20.glGetUniformLocation(program, "uModel");
        int aPos = GLES20.glGetAttribLocation(program, "aPosition");
        GLES20.glUniformMatrix4fv(uLightSpaceMatrix, 1, false, lightMatrix, 0);
        for (Mesh m : this.meshes) {
            if (m != null && m.subMesh.vertexBuffer != null && m.subMesh.indexBuffer != null && m.subMesh.numIndices > 0) {
                float[] localModel = new float[16];
                Matrix.setIdentityM(localModel, 0);
                Matrix.translateM(localModel, 0, m.translation[0], m.translation[1], m.translation[2]);
                Matrix.rotateM(localModel, 0, m.rotation[0], 1.0f, 0.0f, 0.0f);
                Matrix.rotateM(localModel, 0, m.rotation[1], 0.0f, 1.0f, 0.0f);
                Matrix.rotateM(localModel, 0, m.rotation[2], 0.0f, 0.0f, 1.0f);
                Matrix.scaleM(localModel, 0, m.scale[0], m.scale[1], m.scale[2]);
                GLES20.glUniformMatrix4fv(uModel, 1, false, localModel, 0);
                GLES20.glEnableVertexAttribArray(aPos);
                m.subMesh.vertexBuffer.position(0);
                m.subMesh.indexBuffer.position(0);
                GLES20.glVertexAttribPointer(aPos, 3, 5126, false, 0, (Buffer) m.subMesh.vertexBuffer);
                GLES20.glDrawElements(4, m.subMesh.numIndices, 5123, m.subMesh.indexBuffer);
                GLES20.glDisableVertexAttribArray(aPos);
            }
        }
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glUseProgram(0);
    }

    private void PasoDeLuzAnime(float[] lightMatrix, int shadowTex) {
        int program;
        if (this.shaderAnime == null || this.meshes.isEmpty() || (program = this.shaderAnime.lightProgram) == 0) {
            return;
        }
        GLES20.glUseProgram(program);
        int uMVP = GLES20.glGetUniformLocation(program, "uMVP");
        int uModel = GLES20.glGetUniformLocation(program, "uModel");
        int uLightMatrix = GLES20.glGetUniformLocation(program, "uLightSpaceMatrix");
        int uShadowMap = GLES20.glGetUniformLocation(program, "uShadowMap");
        int uLightPos = GLES20.glGetUniformLocation(program, "uLightPos");
        int uViewPos = GLES20.glGetUniformLocation(program, "uViewPos");
        int uLightIntensity = GLES20.glGetUniformLocation(program, "uLightIntensity");
        int uShadowStrength = GLES20.glGetUniformLocation(program, "uShadowStrength");
        int uLightColor = GLES20.glGetUniformLocation(program, "uLightColor");
        int uShadowColor = GLES20.glGetUniformLocation(program, "uShadowColor");
        int aPos = GLES20.glGetAttribLocation(program, "aPosition");
        int aNormal = GLES20.glGetAttribLocation(program, "aNormal");
        int aColor = GLES20.glGetAttribLocation(program, "aColor");
        GLES20.glActiveTexture(33984);
        GLES20.glBindTexture(3553, shadowTex);
        GLES20.glUniform1i(uShadowMap, 0);
        float[] lightPos = this.shaderAnime.getLightPosition();
        GLES20.glUniform3f(uLightPos, lightPos[0], lightPos[1], lightPos[2]);
        GLES20.glUniform1f(uLightIntensity, this.shaderAnime.lightIntensity);
        GLES20.glUniform1f(uShadowStrength, this.shaderAnime.shadowStrength);
        GLES20.glUniform3fv(uLightColor, 1, this.shaderAnime.lightColor, 0);
        GLES20.glUniform3fv(uShadowColor, 1, this.shaderAnime.shadowColor, 0);
        float[] activePtPosAnime = {0f, 0f, 0f};
        float activePtRadiusAnime = 0.0f;
        float[] activePtColorAnime = {0f, 0f, 0f};
        for (Mesh meshCandidate : this.meshes) {
            if (meshCandidate != null && meshCandidate.lightRadiusMult > 0.01f) {
                ar.axt.sombras.CancelarIluminacion.EsferaLuz esfera = 
                        ar.axt.sombras.CancelarIluminacion.calcularEsferaLuz(meshCandidate);
                activePtPosAnime = esfera.centroWorld;
                activePtRadiusAnime = esfera.radioEfectivo;
                activePtColorAnime = esfera.colorLuz;
                break;
            }
        }
        int uPtPosLocA = GLES20.glGetUniformLocation(program, "uPointLightPos");
        int uPtRadLocA = GLES20.glGetUniformLocation(program, "uPointLightRadius");
        int uPtColLocA = GLES20.glGetUniformLocation(program, "uPointLightColor");
        if (uPtPosLocA >= 0) GLES20.glUniform3f(uPtPosLocA, activePtPosAnime[0], activePtPosAnime[1], activePtPosAnime[2]);
        if (uPtRadLocA >= 0) GLES20.glUniform1f(uPtRadLocA, activePtRadiusAnime);
        if (uPtColLocA >= 0) GLES20.glUniform3f(uPtColLocA, activePtColorAnime[0], activePtColorAnime[1], activePtColorAnime[2]);
        float[] invView = new float[16];
        float[] eyePos = new float[4];
        Matrix.invertM(invView, 0, this.viewMatrix, 0);
        Matrix.multiplyMV(eyePos, 0, invView, 0, new float[]{0.0f, 0.0f, 0.0f, 1.0f}, 0);
        GLES20.glUniform3f(uViewPos, eyePos[0], eyePos[1], eyePos[2]);
        for (Mesh m : this.meshes) {
            if (m == null || m.subMesh.vertexBuffer == null || m.subMesh.indexBuffer == null) {
                eyePos = eyePos;
            } else if (m.subMesh.numIndices > 0) {
                float[] localModel = new float[16];
                Matrix.setIdentityM(localModel, 0);
                int uShadowMap2 = uShadowMap;
                int uLightPos2 = uLightPos;
                int uViewPos2 = uViewPos;
                Matrix.translateM(localModel, 0, m.translation[0], m.translation[1], m.translation[2]);
                Matrix.rotateM(localModel, 0, m.rotation[0], 1.0f, 0.0f, 0.0f);
                Matrix.rotateM(localModel, 0, m.rotation[1], 0.0f, 1.0f, 0.0f);
                Matrix.rotateM(localModel, 0, m.rotation[2], 0.0f, 0.0f, 1.0f);
                Matrix.scaleM(localModel, 0, m.scale[0], m.scale[1], m.scale[2]);
                float[] finalModel = new float[16];
                System.arraycopy(localModel, 0, finalModel, 0, 16);
                float[] normalMatrix4 = new float[16];
                float[] eyePos2 = eyePos;
                float[] eyePos3 = new float[16];
                Matrix.invertM(eyePos3, 0, finalModel, 0);
                Matrix.transposeM(normalMatrix4, 0, eyePos3, 0);
                float[] pv = new float[16];
                Matrix.multiplyMM(pv, 0, this.viewMatrix, 0, finalModel, 0);
                float[] modelInv = new float[16];
                Matrix.multiplyMM(modelInv, 0, this.projectionMatrix, 0, pv, 0);
                GLES20.glUniformMatrix4fv(uMVP, 1, false, modelInv, 0);
                GLES20.glUniformMatrix4fv(uModel, 1, false, finalModel, 0);
                GLES20.glUniformMatrix4fv(uLightMatrix, 1, false, lightMatrix, 0);
                int uNormalMatrix = GLES20.glGetUniformLocation(program, "uNormalMatrix");
                float[] invView2 = invView;
                GLES20.glUniformMatrix4fv(uNormalMatrix, 1, false, normalMatrix4, 0);
                int uSpecStr = GLES20.glGetUniformLocation(program, "uSpecularStrength");
                int uShininess = GLES20.glGetUniformLocation(program, "uShininess");
                int uAlphaLoc = GLES20.glGetUniformLocation(program, "uAlpha");
                if (uSpecStr >= 0) GLES20.glUniform1f(uSpecStr, m.specularStrength);
                if (uShininess >= 0) GLES20.glUniform1f(uShininess, m.shininess);
                if (uAlphaLoc >= 0) GLES20.glUniform1f(uAlphaLoc, m.opacity);
                int uHasTex = GLES20.glGetUniformLocation(program, "uHasTexture");
                int uTex = GLES20.glGetUniformLocation(program, "uTexture");
                int aTex = GLES20.glGetAttribLocation(program, "aTexCoord");
                if (m.subMesh.textureId != -1) {
                    GLES20.glUniform1i(uHasTex, 1);
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, m.subMesh.textureId);
                    GLES20.glUniform1i(uTex, 1);
                    if (m.subMesh.texcoordBuffer != null) {
                        GLES20.glEnableVertexAttribArray(aTex);
                        m.subMesh.texcoordBuffer.position(0);
                        GLES20.glVertexAttribPointer(aTex, 2, GLES20.GL_FLOAT, false, 0, m.subMesh.texcoordBuffer);
                    }
                } else { GLES20.glUniform1i(uHasTex, 0); }
                int uHasNormMapA = GLES20.glGetUniformLocation(program, "uHasNormalMap");
                int uNormMapA = GLES20.glGetUniformLocation(program, "uNormalMap");
                if (m.subMesh.normalMapTextureId != -1) {
                    if (uHasNormMapA >= 0) GLES20.glUniform1i(uHasNormMapA, 1);
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, m.subMesh.normalMapTextureId);
                    if (uNormMapA >= 0) GLES20.glUniform1i(uNormMapA, 2);
                } else {
                    if (uHasNormMapA >= 0) GLES20.glUniform1i(uHasNormMapA, 0);
                }
                GLES20.glEnableVertexAttribArray(aPos);
                GLES20.glVertexAttribPointer(aPos, 3, 5126, false, 0, (Buffer) m.subMesh.vertexBuffer);
                if (m.subMesh.normalBuffer != null && aNormal >= 0) {
                    GLES20.glEnableVertexAttribArray(aNormal);
                    GLES20.glVertexAttribPointer(aNormal, 3, 5126, false, 0, (Buffer) m.subMesh.normalBuffer);
                }
                if (m.subMesh.colorBuffer != null && aColor >= 0) {
                    GLES20.glEnableVertexAttribArray(aColor);
                    GLES20.glVertexAttribPointer(aColor, 3, 5126, false, 0, (Buffer) m.subMesh.colorBuffer);
                }
                GLES20.glDrawElements(4, m.subMesh.numIndices, 5123, m.subMesh.indexBuffer);
                GLES20.glDisableVertexAttribArray(aPos);
                if (aNormal >= 0) { GLES20.glDisableVertexAttribArray(aNormal); }
                if (aColor >= 0) { GLES20.glDisableVertexAttribArray(aColor); }
                if (m.subMesh.textureId != -1) {
                    GLES20.glDisableVertexAttribArray(aTex);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
                }
                eyePos = eyePos2;
                invView = invView2;
                uShadowMap = uShadowMap2;
                uLightPos = uLightPos2;
                uViewPos = uViewPos2;
            }
        } GLES20.glUseProgram(0);
    }

    private void RenderSimple(float[] viewMatrix, float[] projectionMatrix) {
        int program;
        if (this.simpleShader == null || this.meshes.isEmpty() || (program = this.simpleShader.getProgram()) == 0) {
            return;
        }
        GLES20.glUseProgram(program);
        int uMVP = this.simpleShader.uMVPLocation;
        int aPos = this.simpleShader.aPositionLocation;
        int aColor = this.simpleShader.aColorLocation;
        int aTex = this.simpleShader.aTexCoordLocation;
        int uHasTex = this.simpleShader.uHasTextureLocation;
        int uTex = this.simpleShader.uTextureLocation;
        for (Mesh m : this.meshes) {
            if (m != null && m.subMesh.vertexBuffer != null && m.subMesh.indexBuffer != null && m.subMesh.numIndices > 0) {
                float[] model = new float[16];
                Matrix.setIdentityM(model, 0);
                Matrix.translateM(model, 0, m.translation[0], m.translation[1], m.translation[2]);
                Matrix.rotateM(model, 0, m.rotation[0], 1.0f, 0.0f, 0.0f);
                Matrix.rotateM(model, 0, m.rotation[1], 0.0f, 1.0f, 0.0f);
                Matrix.rotateM(model, 0, m.rotation[2], 0.0f, 0.0f, 1.0f);
                Matrix.scaleM(model, 0, m.scale[0], m.scale[1], m.scale[2]);
                float[] vm = new float[16];
                float[] mvp = new float[16];
                Matrix.multiplyMM(vm, 0, viewMatrix, 0, model, 0);
                Matrix.multiplyMM(mvp, 0, projectionMatrix, 0, vm, 0);
                GLES20.glUniformMatrix4fv(uMVP, 1, false, mvp, 0);
                if (m.subMesh.textureId != -1) {
                    GLES20.glUniform1i(uHasTex, 1);
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, m.subMesh.textureId);
                    GLES20.glUniform1i(uTex, 0);
                    if (m.subMesh.texcoordBuffer != null) {
                        GLES20.glEnableVertexAttribArray(aTex);
                        m.subMesh.texcoordBuffer.position(0);
                        GLES20.glVertexAttribPointer(aTex, 2, GLES20.GL_FLOAT, false, 0, m.subMesh.texcoordBuffer);
                    }
                } else { GLES20.glUniform1i(uHasTex, 0); }
                GLES20.glEnableVertexAttribArray(aPos);
                m.subMesh.vertexBuffer.position(0);
                m.subMesh.indexBuffer.position(0);
                GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 0, (java.nio.Buffer) m.subMesh.vertexBuffer);
                if (m.subMesh.colorBuffer != null && aColor >= 0) {
                    GLES20.glEnableVertexAttribArray(aColor);
                    GLES20.glVertexAttribPointer(aColor, 3, GLES20.GL_FLOAT, false, 0, (java.nio.Buffer) m.subMesh.colorBuffer);
                }
                GLES20.glDrawElements(GLES20.GL_TRIANGLES, m.subMesh.numIndices, GLES20.GL_UNSIGNED_SHORT, m.subMesh.indexBuffer);
                GLES20.glDisableVertexAttribArray(aPos);
                if (aColor >= 0) GLES20.glDisableVertexAttribArray(aColor);
                if (m.subMesh.textureId != -1) {
                    GLES20.glDisableVertexAttribArray(aTex);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
                }
            }
        } GLES20.glUseProgram(0);
    }

    private void drawHitboxes() {
        if (this.activity.verHitboxOpcion() == 0) { return; }
        GLES20.glEnable(3042);
        GLES20.glBlendFunc(770, 771);
        GLES20.glDisable(2929);
        GLES20.glDisable(2884);
        for (Mesh m : this.meshes) {
            if (m != null && m.hitbox != null) { drawSingleHitbox(m.hitbox); }
        }
        if (this.gizmo != null && this.gizmo.hitboxes != null) {
            for (Hitbox hb : this.gizmo.hitboxes) {
                if (hb != null) { drawSingleHitbox(hb); }
            }
        }
        GLES20.glEnable(2929);
        GLES20.glDisable(3042);
    }

    private void initHitboxGeometry() {
        this.hitboxVertexBuffer = ByteBuffer.allocateDirect(96).order(ByteOrder.nativeOrder()).asFloatBuffer();
        short[] edges = {0, 1, 1, 3, 3, 2, 2, 0, 4, 5, 5, 7, 7, 6, 6, 4, 0, 4, 1, 5, 2, 6, 3, 7};
        this.hitboxIndexBuffer = ByteBuffer.allocateDirect(edges.length * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
        this.hitboxIndexBuffer.put(edges).position(0);
        this.hitboxIndexCount = edges.length;
    }

    private void drawSingleHitbox(Hitbox hb) {
        float[] vertices = new float[24];
        hb.getVertices(vertices);
        this.hitboxVertexBuffer.clear();
        this.hitboxVertexBuffer.put(vertices);
        this.hitboxVertexBuffer.position(0);
        GLES20.glUseProgram(this.simpleColorProgram);
        float[] mvp = new float[16];
        Matrix.multiplyMM(mvp, 0, this.projectionMatrix, 0, this.viewMatrix, 0);
        GLES20.glUniformMatrix4fv(this.uMvpHitbox, 1, false, mvp, 0);
        GLES20.glUniform4f(this.uColorHitbox, 1.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glEnableVertexAttribArray(this.aPosHitbox);
        GLES20.glVertexAttribPointer(this.aPosHitbox, 3, 5126, false, 0, (Buffer) this.hitboxVertexBuffer);
        GLES20.glDrawElements(1, this.hitboxIndexCount, 5123, this.hitboxIndexBuffer);
        GLES20.glDisableVertexAttribArray(this.aPosHitbox);
    }

    private int cargarTexturaResource(int resId) {
        int[] texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        Bitmap bitmap = BitmapFactory.decodeResource(this.context.getResources(), resId);
        if (bitmap == null) return -1;
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle();
        return texture[0];
    }

    private void initSkyBuffers() {
        texSol = cargarTexturaResource(R.drawable.sol);
        texNubes[0] = cargarTexturaResource(R.drawable.ambiente_nube_uno);
        texNubes[1] = cargarTexturaResource(R.drawable.ambiente_nube_dos);
        texNubes[2] = cargarTexturaResource(R.drawable.ambiente_nube_tres);
        texNubes[3] = cargarTexturaResource(R.drawable.ambiente_nube_cuatro);
        String vsCode = "attribute vec3 aPosition;\n" +
                "attribute vec2 aTexCoord;\n" +
                "uniform mat4 uMVP;\n" +
                "varying vec2 vTexCoord;\n" +
                "void main() {\n" +
                "    gl_Position = uMVP * vec4(aPosition, 1.0);\n" +
                "    vTexCoord = aTexCoord;\n" +
                "}\n";
        String fsCode = "precision mediump float;\n" +
                "uniform sampler2D uTexture;\n" +
                "uniform vec4 uColorTint;\n" +
                "varying vec2 vTexCoord;\n" +
                "void main() {\n" +
                "    vec4 texColor = texture2D(uTexture, vTexCoord);\n" +
                "    gl_FragColor = texColor * uColorTint;\n" +
                "}\n";
        skyProgram = createProgram(vsCode, fsCode);
        uMvpSky = GLES20.glGetUniformLocation(skyProgram, "uMVP");
        uTextureSky = GLES20.glGetUniformLocation(skyProgram, "uTexture");
        uColorTintSky = GLES20.glGetUniformLocation(skyProgram, "uColorTint");
        aPosSky = GLES20.glGetAttribLocation(skyProgram, "aPosition");
        aTexCoordSky = GLES20.glGetAttribLocation(skyProgram, "aTexCoord");
        float[] uvs = { 0.0f, 0.0f,  1.0f, 0.0f,  1.0f, 1.0f,  0.0f, 0.0f,  1.0f, 1.0f,  0.0f, 1.0f };
        ByteBuffer ubb = ByteBuffer.allocateDirect(uvs.length * 4).order(ByteOrder.nativeOrder());
        quadTexBuffer = ubb.asFloatBuffer();
        quadTexBuffer.put(uvs).position(0);
        starCount = 200;
        float[] starCoords = new float[starCount * 3];
        Random rand = new Random(12345);
        for (int i = 0; i < starCount; i++) {
            float theta = rand.nextFloat() * (float) Math.PI * 2.0f;
            float phi = rand.nextFloat() * (float) Math.PI * 0.45f;
            float dist = 70.0f;
            starCoords[i * 3] = (float) (dist * Math.cos(theta) * Math.sin(phi));
            starCoords[i * 3 + 1] = (float) (dist * Math.cos(phi)) + 10.0f;
            starCoords[i * 3 + 2] = (float) (dist * Math.sin(theta) * Math.sin(phi));
        }
        ByteBuffer sbb = ByteBuffer.allocateDirect(starCoords.length * 4).order(ByteOrder.nativeOrder());
        starVertexBuffer = sbb.asFloatBuffer();
        starVertexBuffer.put(starCoords).position(0);
        float astroSize = 4.0f;
        float[] astroQuad = {
            -astroSize, -astroSize, 0.0f,
             astroSize, -astroSize, 0.0f,
             astroSize,  astroSize, 0.0f,
            -astroSize, -astroSize, 0.0f,
             astroSize,  astroSize, 0.0f,
            -astroSize,  astroSize, 0.0f
        };
        ByteBuffer abb = ByteBuffer.allocateDirect(astroQuad.length * 4).order(ByteOrder.nativeOrder());
        astroVertexBuffer = abb.asFloatBuffer();
        astroVertexBuffer.put(astroQuad).position(0);
        numCloudQuads = 12;
        float[] cloudCoords = new float[numCloudQuads * 6 * 3];
        int idx = 0;
        Random cloudRand = new Random(54321);
        for (int i = 0; i < numCloudQuads; i++) {
            cloudTexIndices[i] = cloudRand.nextInt(4);
            float cx = (cloudRand.nextFloat() - 0.5f) * 90.0f;
            float cy = 22.0f + cloudRand.nextFloat() * 12.0f;
            float cz = (cloudRand.nextFloat() - 0.5f) * 90.0f;
            float sizeX = 12.0f + cloudRand.nextFloat() * 10.0f;
            float sizeZ = 10.0f + cloudRand.nextFloat() * 8.0f;
            float[][] v = {
                {cx - sizeX, cy, cz - sizeZ},
                {cx + sizeX, cy, cz - sizeZ},
                {cx + sizeX, cy, cz + sizeZ},
                {cx - sizeX, cy, cz - sizeZ},
                {cx + sizeX, cy, cz + sizeZ},
                {cx - sizeX, cy, cz + sizeZ}
            };
            for (float[] p : v) {
                cloudCoords[idx++] = p[0];
                cloudCoords[idx++] = p[1];
                cloudCoords[idx++] = p[2];
            }
        }
        ByteBuffer cbb = ByteBuffer.allocateDirect(cloudCoords.length * 4).order(ByteOrder.nativeOrder());
        cloudVertexBuffer = cbb.asFloatBuffer();
        cloudVertexBuffer.put(cloudCoords).position(0);
    }

    public void dibujarCielo(float[] vpMatrix) {
        if (starVertexBuffer == null) { initSkyBuffers(); }
        float brightness = (skyColor[0] + skyColor[1] + skyColor[2]) / 3.0f;
        GLES20.glEnable(3042);
        GLES20.glBlendFunc(770, 771);
        GLES20.glDisable(2884);
        GLES20.glDepthMask(false);
        if (brightness <= 0.35f && starDensity > 0.0f) {
            float alphaEstrellas = (1.0f - (brightness / 0.35f)) * starDensity;
            if (alphaEstrellas > 1.0f) alphaEstrellas = 1.0f;
            if (alphaEstrellas > 0.02f) {
                GLES20.glUseProgram(simpleColorProgram);
                GLES20.glUniformMatrix4fv(uMvpHitbox, 1, false, vpMatrix, 0);
                GLES20.glUniform4f(uColorHitbox, 1.0f, 1.0f, 1.0f, alphaEstrellas);
                GLES20.glEnableVertexAttribArray(aPosHitbox);
                GLES20.glVertexAttribPointer(aPosHitbox, 3, 5126, false, 0, (Buffer) starVertexBuffer);
                GLES20.glDrawArrays(0, 0, starCount);
                GLES20.glDisableVertexAttribArray(aPosHitbox);
            }
        }
        float[] lPos = {10.0f, 15.0f, 10.0f};
        if (shaderRealista != null) { System.arraycopy(shaderRealista.lightPos, 0, lPos, 0, 3);
        } else if (shaderAnime != null) { System.arraycopy(shaderAnime.lightPos, 0, lPos, 0, 3);
        }
        float len = (float) Math.sqrt(lPos[0] * lPos[0] + lPos[1] * lPos[1] + lPos[2] * lPos[2]);
        if (len < 0.001f) len = 1.0f;
        float dist = 60.0f;
        float astroX = (lPos[0] / len) * dist;
        float astroY = (lPos[1] / len) * dist;
        float astroZ = (lPos[2] / len) * dist;
        float[] modelAstro = new float[16];
        Matrix.setIdentityM(modelAstro, 0);
        Matrix.translateM(modelAstro, 0, astroX, astroY, astroZ);
        modelAstro[0] = viewMatrix[0]; modelAstro[1] = viewMatrix[4]; modelAstro[2] = viewMatrix[8];
        modelAstro[4] = viewMatrix[1]; modelAstro[5] = viewMatrix[5]; modelAstro[6] = viewMatrix[9];
        modelAstro[8] = viewMatrix[2]; modelAstro[9] = viewMatrix[6]; modelAstro[10] = viewMatrix[10];
        float[] mvpAstro = new float[16];
        Matrix.multiplyMM(mvpAstro, 0, vpMatrix, 0, modelAstro, 0);
        if (texSol != -1 && skyProgram != -1) {
            GLES20.glUseProgram(skyProgram);
            GLES20.glUniformMatrix4fv(uMvpSky, 1, false, mvpAstro, 0);
            if (brightness > 0.15f) {
                GLES20.glUniform4f(uColorTintSky, 1.0f, 0.95f, 0.7f, 1.0f);
            } else {
                GLES20.glUniform4f(uColorTintSky, 0.85f, 0.95f, 1.0f, 0.95f);
            }
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texSol);
            GLES20.glUniform1i(uTextureSky, 0);
            GLES20.glEnableVertexAttribArray(aPosSky);
            astroVertexBuffer.position(0);
            GLES20.glVertexAttribPointer(aPosSky, 3, GLES20.GL_FLOAT, false, 0, (Buffer) astroVertexBuffer);
            GLES20.glEnableVertexAttribArray(aTexCoordSky);
            quadTexBuffer.position(0);
            GLES20.glVertexAttribPointer(aTexCoordSky, 2, GLES20.GL_FLOAT, false, 0, (Buffer) quadTexBuffer);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
            GLES20.glDisableVertexAttribArray(aPosSky);
            GLES20.glDisableVertexAttribArray(aTexCoordSky);
        }
        if (cloudDensity > 0.01f && skyProgram != -1) {
            GLES20.glUseProgram(skyProgram);
            float alphaNubes = cloudDensity * 0.9f;
            if (alphaNubes > 0.95f) alphaNubes = 0.95f;
            float nr = skyColor[0] * 0.6f + 0.4f;
            float ng = skyColor[1] * 0.6f + 0.4f;
            float nb = skyColor[2] * 0.6f + 0.4f;
            GLES20.glUniform4f(uColorTintSky, nr, ng, nb, alphaNubes);
            GLES20.glUniformMatrix4fv(uMvpSky, 1, false, vpMatrix, 0);
            GLES20.glEnableVertexAttribArray(aPosSky);
            GLES20.glEnableVertexAttribArray(aTexCoordSky);
            for (int i = 0; i < numCloudQuads; i++) {
                int texHandle = texNubes[cloudTexIndices[i]];
                if (texHandle != -1) {
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texHandle);
                    GLES20.glUniform1i(uTextureSky, 0);
                    cloudVertexBuffer.position(i * 6 * 3);
                    GLES20.glVertexAttribPointer(aPosSky, 3, GLES20.GL_FLOAT, false, 0, (Buffer) cloudVertexBuffer);
                    quadTexBuffer.position(0);
                    GLES20.glVertexAttribPointer(aTexCoordSky, 2, GLES20.GL_FLOAT, false, 0, (Buffer) quadTexBuffer);
                    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
                }
            }
            GLES20.glDisableVertexAttribArray(aPosSky);
            GLES20.glDisableVertexAttribArray(aTexCoordSky);
        }
        GLES20.glDepthMask(true);
        GLES20.glEnable(2884);
    }

    private int createProgram(String vertexCode, String fragmentCode) {
        int vs = loadShader(35633, vertexCode);
        int fs = loadShader(35632, fragmentCode);
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vs);
        GLES20.glAttachShader(program, fs);
        GLES20.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, 35714, linkStatus, 0);
        if (linkStatus[0] == 0) {
            String log = GLES20.glGetProgramInfoLog(program);
            GLES20.glDeleteProgram(program);
            throw new RuntimeException("Link error: " + log);
        } return program;
    }

    private int loadShader(int type, String code) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, 35713, compiled, 0);
        if (compiled[0] == 0) {
            String log = GLES20.glGetShaderInfoLog(shader);
            GLES20.glDeleteShader(shader);
            throw new RuntimeException("Compile error: " + log);
        } return shader;
    }

    private String loadShaderFromAssets(String filename) {
        StringBuilder sb = new StringBuilder();
        try {
            InputStream is = this.context.getAssets().open(filename);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            while (true) { String line = br.readLine();
                if (line != null) { sb.append(line).append("\n");
                } else { br.close(); return sb.toString(); }
            }
        } catch (IOException e) { return null; }
    }


}
