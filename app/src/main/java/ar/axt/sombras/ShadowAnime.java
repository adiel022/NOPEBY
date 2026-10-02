package ar.axt.sombras;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ShadowAnime {

    private final Context context;

    private int shadowFBO;
    private int shadowTexture;
    private int shadowDepthRBO;
    private int shadowWidth = 1024;
    private int shadowHeight = 1024;

    public int depthProgram;
    public int lightProgram;

    private final float[] lightViewMatrix = new float[16];
    private final float[] lightProjMatrix = new float[16];
    private final float[] lightSpaceMatrix = new float[16];

    public final float[] lightPos = {10f, 15f, 10f};

    public float lightIntensity = 0.65f;
    public float shadowStrength = 0.75f;
    public float[] lightColor = {1.0f, 1.0f, 1.0f};
    public float[] shadowColor = {0.6f, 0.6f, 0.6f};

    public ShadowAnime(Context context) {
        this.context = context;
    }

    public void init() {
        createShadowFramebuffer();
        String vsDepth = loadShaderFromAssets("anime_depth_vertex.glsl");
        String fsDepth = loadShaderFromAssets("anime_depth_fragment.glsl");
        depthProgram = createProgram(vsDepth, fsDepth);
        String vsLight = loadShaderFromAssets("anime_light_vertex.glsl");
        String fsLight = loadShaderFromAssets("anime_light_fragment.glsl");
        lightProgram = createProgram(vsLight, fsLight);
        Log.i("shadowAnime", "✅ Sistema de sombras inicializado correctamente.");
    }

    private void createShadowFramebuffer() {
        int[] fbo = new int[1];
        int[] tex = new int[1];
        int[] rbo = new int[1];
        GLES20.glGenFramebuffers(1, fbo, 0);
        GLES20.glGenTextures(1, tex, 0);
        GLES20.glGenRenderbuffers(1, rbo, 0);
        shadowFBO = fbo[0];
        shadowTexture = tex[0];
        shadowDepthRBO = rbo[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, shadowTexture);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                shadowWidth, shadowHeight, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, shadowDepthRBO);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, shadowWidth, shadowHeight);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, shadowFBO);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, shadowTexture, 0);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,									 GLES20.GL_RENDERBUFFER, shadowDepthRBO);
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e("shadowAnime", "❌ Error creando framebuffer de sombra. Código: " + status);
        } else {
            Log.i("shadowAnime", "✅ FBO de sombra creado correctamente (" +
                    shadowWidth + "x" + shadowHeight + ", RGBA codificado)");
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
    }

    public void updateLightMatrix(float[] sceneCenter, float sceneRadius) {
        Matrix.setLookAtM(lightViewMatrix, 0,  lightPos[0], lightPos[1], lightPos[2],
                sceneCenter[0], sceneCenter[1], sceneCenter[2],  0f, 1f, 0f);
        float radius = sceneRadius;
        float near = 1f;
        float far = radius * 4f;
        Matrix.orthoM(lightProjMatrix, 0,  -radius, radius, -radius, radius,  near, far);
        Matrix.multiplyMM(lightSpaceMatrix, 0,  lightProjMatrix, 0, lightViewMatrix, 0);
    }

    public int getShadowTexture() {
        return shadowTexture;
    }

    public int getShadowFramebuffer() {
        return shadowFBO;
    }

    public float[] getLightSpaceMatrix() {
        return lightSpaceMatrix;
    }

    public float[] getLightPosition() {
        return lightPos;
    }

    private int createProgram(String vertexCode, String fragmentCode) {
        int vs = loadShader(GLES20.GL_VERTEX_SHADER, vertexCode);
        int fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentCode);
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vs);
        GLES20.glAttachShader(program, fs);
        GLES20.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            String log = GLES20.glGetProgramInfoLog(program);
            GLES20.glDeleteProgram(program);
            throw new RuntimeException("❌ Error enlazando shader program: " + log);
        } return program;
    }

    private int loadShader(int type, String code) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            String log = GLES20.glGetShaderInfoLog(shader);
            GLES20.glDeleteShader(shader);
            throw new RuntimeException("❌ Error compilando shader: " + log);
        } return shader;
    }

    private String loadShaderFromAssets(String filename) {
        InputStream is = null;
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        try {
            is = context.getAssets().open(filename);
            br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            Log.e("shadowAnime", "❌ Error leyendo shader: " + filename, e);
        } finally {
            try {
                if (br != null) br.close();
                if (is != null) is.close();
            } catch (IOException e) {
            }
        } return sb.toString();
    }

}

