package ar.axt.materiales;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;
import ar.axt.nopeby.MainActivity;

public class ClimaNieve {

	private static final int TOTAL_GOTAS = 100;
    private static final float LIMITE_XZ = 100.0f;
    private static final float Y_INICIO = 100.0f;
    private static final float Y_FINAL = -100.0f;
    private static final float VELOCIDAD_CAIDA = 40f;

    private int program;
    private int aPosition;
    private int aColor;
    private int uMVP;

    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    private final float[] tempMatrix = new float[16];
	
	private MainActivity activity;

    // GOTAS
    private final Gota[] gotas = new Gota[TOTAL_GOTAS];


	private final float[] vertices = {
		0.0f,  0.2f,  0.0f,
		-0.2f, -0.2f,  0.0f,
		0.2f, -0.2f,  0.0f,};

	 private final float[] colors = {
	 0.8f, 0.8f, 0.8f,
	 0.8f, 0.8f, 0.8f,
	 0.8f, 0.8f, 0.8f};
	 
    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;
    private final Random random = new Random();

    public ClimaNieve(Context context) {
		activity = (MainActivity) context;
        crearBuffers();
        crearShaders(context);
        crearGotas();
        Matrix.setLookAtM(
			viewMatrix,0,0f, 0f, 5f,
			0f, 0f, 0f,0f, 1f, 0f);
    }

    private void crearBuffers() {
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);
        ByteBuffer cb = ByteBuffer.allocateDirect(colors.length * 4);
        cb.order(ByteOrder.nativeOrder());
        colorBuffer = cb.asFloatBuffer();
        colorBuffer.put(colors);
        colorBuffer.position(0);
    }

    private void crearGotas() {
        for (int i = 0; i < TOTAL_GOTAS; i++) {
            Gota g = new Gota();
            g.x = randomXZ();
            g.z = randomXZ();
            g.y = Y_INICIO;
            g.rotX = random.nextFloat() * 360f;
            g.rotY = random.nextFloat() * 360f;
            g.rotZ = random.nextFloat() * 360f;
            gotas[i] = g;
        }
    }

	public void actualizar(float delta) {
		for (int i = 0; i < TOTAL_GOTAS; i++) {
			Gota g = gotas[i];
			g.y -= VELOCIDAD_CAIDA * delta;
			if (g.y <= Y_FINAL) {
				g.y = random.nextFloat() * (Y_INICIO - Y_FINAL) + Y_FINAL;
				g.x = randomXZ();
				g.z = randomXZ();
			}
		}
	}

	public void dibujar() {
		GLES20.glUseProgram(program);
		GLES20.glEnableVertexAttribArray(aPosition);
		GLES20.glEnableVertexAttribArray(aColor);
		GLES20.glVertexAttribPointer(
			aPosition, 3, GLES20.GL_FLOAT,
			false, 0, vertexBuffer);
		GLES20.glVertexAttribPointer(
			aColor, 3, GLES20.GL_FLOAT,
			false, 0, colorBuffer);
		for (int i = 0; i < TOTAL_GOTAS; i++) {
			Gota g = gotas[i];
			Matrix.setIdentityM(modelMatrix, 0);
			Matrix.translateM(modelMatrix, 0, g.x, g.y, g.z);
			Matrix.multiplyMM(tempMatrix, 0,
							  viewMatrix, 0, modelMatrix, 0);
			Matrix.multiplyMM(mvpMatrix, 0,
							  projectionMatrix, 0, tempMatrix, 0);
			GLES20.glUniformMatrix4fv(
				uMVP, 1, false, mvpMatrix, 0);
			GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
		}
		GLES20.glDisableVertexAttribArray(aPosition);
		GLES20.glDisableVertexAttribArray(aColor);
		activity.requestRender();
	}

    public void setProjection(int width, int height) {
        float ratio = (float) width / (float) height;
        Matrix.frustumM(projectionMatrix,  0,-ratio,ratio,-1,1,1,500);
    }

    private void crearShaders(Context context) {
        String vertexCode =
			leerAsset(context, "simple_vertex.glsl");
        String fragmentCode =
			leerAsset(context, "simple_fragment.glsl");
        int vertexShader =
			compilarShader(
			GLES20.GL_VERTEX_SHADER,
			vertexCode);
        int fragmentShader =
			compilarShader(
			GLES20.GL_FRAGMENT_SHADER,
			fragmentCode);
        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        aPosition = GLES20.glGetAttribLocation(program, "aPosition");
        aColor = GLES20.glGetAttribLocation(program, "aColor");
        uMVP = GLES20.glGetUniformLocation(program, "uMVP");
    }

    private int compilarShader(int type, String code) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);
        return shader;
    }

    private String leerAsset(Context context, String nombre) {
        StringBuilder sb = new StringBuilder();
        try {InputStream is =
				context.getAssets().open(nombre);
			BufferedReader br =  new BufferedReader( new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            } br.close();
        } catch (IOException e) { e.printStackTrace();
        } return sb.toString();
    }

    private float randomXZ() {
        return (random.nextFloat() * LIMITE_XZ * 2f)  - LIMITE_XZ;
    }

    private static class Gota {
        float x;
        float y;
        float z;
        float rotX;
        float rotY;
        float rotZ;
    }

}
