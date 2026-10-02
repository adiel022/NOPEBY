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

public class ClimaLluvia {

	private static final int TOTAL_GOTAS = 100;
    private static final float LIMITE_XZ = 100.0f;
    private static final float Y_INICIO = 100.0f;
    private static final float Y_FINAL = -100.0f;
    private static final float VELOCIDAD_CAIDA = 70f;

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
	

    private final Gota[] gotas = new Gota[TOTAL_GOTAS];


	private final float[] vertices = {
		-1.0f,  1.0f, 0.0f,
		-1.0f, -1.0f, 0.0f,
		 1.0f, -1.0f, 0.0f,
		-1.0f,  1.0f, 0.0f,
		 1.0f, -1.0f, 0.0f,
		 1.0f,  1.0f, 0.0f
	};
	
	private final float[] texCoords = {
		0f, 0f,  0f, 1f,  1f, 1f,
		0f, 0f,  1f, 1f,  1f, 0f
	};
	
	private int texturaGota;
	private int aTexCoord;
	private int uTexture;
	
	private FloatBuffer vertexBuffer;
	private FloatBuffer texBuffer;
	private final Random random = new Random();

	public ClimaLluvia(Context context) {
		activity = (MainActivity) context;
		texturaGota = cargarTextura(context, ar.axt.nopeby.R.drawable.particulagota);
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
		ByteBuffer tb = ByteBuffer.allocateDirect(texCoords.length * 4);
		tb.order(ByteOrder.nativeOrder());
		texBuffer = tb.asFloatBuffer();
		texBuffer.put(texCoords);
		texBuffer.position(0);
	}

	private int cargarTextura(Context context, int resourceId) {
		int[] texture = new int[1];
		GLES20.glGenTextures(1, texture, 0);
		android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeResource(
		context.getResources(), resourceId);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		android.opengl.GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
		bitmap.recycle();
		return texture[0];
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
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		GLES20.glEnableVertexAttribArray(aPosition);
		GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
		GLES20.glEnableVertexAttribArray(aTexCoord);
		GLES20.glVertexAttribPointer(aTexCoord, 2, GLES20.GL_FLOAT, false, 0, texBuffer);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturaGota);
		GLES20.glUniform1i(uTexture, 0);
		for (int i = 0; i < TOTAL_GOTAS; i++) {
			Gota g = gotas[i];
			Matrix.setIdentityM(modelMatrix, 0);
			Matrix.translateM(modelMatrix, 0, g.x, g.y, g.z);
			modelMatrix[0]  = viewMatrix[0];
			modelMatrix[1]  = viewMatrix[4];
			modelMatrix[2]  = viewMatrix[8];
			modelMatrix[4]  = viewMatrix[1];
			modelMatrix[5]  = viewMatrix[5];
			modelMatrix[6]  = viewMatrix[9];
			modelMatrix[8]  = viewMatrix[2];
			modelMatrix[9]  = viewMatrix[6];
			modelMatrix[10] = viewMatrix[10];
			Matrix.scaleM(modelMatrix, 0, 0.4f, 0.4f, 0.4f);
			Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
			Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);
			GLES20.glUniformMatrix4fv(uMVP, 1, false, mvpMatrix, 0);
			GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
		}
		GLES20.glDisableVertexAttribArray(aPosition);
		GLES20.glDisableVertexAttribArray(aTexCoord);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
		activity.requestRender();
	}

    public void setProjection(int width, int height) {
        float ratio = (float) width / (float) height;
        Matrix.frustumM(projectionMatrix,
		0,-ratio,ratio,-1,1,1,500);
    }

	private void crearShaders(Context context) {
		String vertexCode =
			"attribute vec3 aPosition; \n" +
			"attribute vec2 aTexCoord; \n" +
			"uniform mat4 uMVP; \n" +
			"varying vec2 vTexCoord; \n" +
			"void main(){\n" +
			"   gl_Position = uMVP * vec4(aPosition,1.0);\n" +
			"   vTexCoord = aTexCoord; \n" +
			"}";
		String fragmentCode =
			"precision mediump float; \n" +
			"uniform sampler2D uTexture; \n" +
			"varying vec2 vTexCoord; \n" +
			"void main(){ \n" +
			"   gl_FragColor = texture2D(uTexture, vTexCoord);\n" +
			"}";
		int vertexShader = compilarShader(GLES20.GL_VERTEX_SHADER, vertexCode);
		int fragmentShader = compilarShader(GLES20.GL_FRAGMENT_SHADER, fragmentCode);
		program = GLES20.glCreateProgram();
		GLES20.glAttachShader(program, vertexShader);
		GLES20.glAttachShader(program, fragmentShader);
		GLES20.glLinkProgram(program);
		aPosition = GLES20.glGetAttribLocation(program, "aPosition");
		aTexCoord = GLES20.glGetAttribLocation(program, "aTexCoord");
		uMVP = GLES20.glGetUniformLocation(program, "uMVP");
		uTexture = GLES20.glGetUniformLocation(program, "uTexture");
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
        BufferedReader br =
		new BufferedReader(
		new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) { sb.append(line).append("\n");
            } br.close();
        } catch (IOException e) { e.printStackTrace(); }
        return sb.toString();
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
