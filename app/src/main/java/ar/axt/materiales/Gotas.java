package ar.axt.materiales;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import ar.axt.nopeby.MainActivity;

public class Gotas {

    private static final int MAX_PARTICULAS_SISTEMA = 5000;

    private int program;
    private int aPosition;
    private int aColor;
    private int uMVP;

    private float[] projectionMatrix;
    private float[] viewMatrix;
    private final float[] modelMatrix = new float[16];
    private final float[] tempMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;
    private MainActivity activity;
    private Material material;
    private final Random random = new Random();
	private float emitTimer = 0f;
	private static final float EMIT_INTERVAL = 0.08f;

    private final float[] vertices = {
        0.0f,  0.50f, 0.0f,
		-0.50f, -0.50f, 0.0f,
        0.50f, -0.50f, 0.0f
    };

    private final List<Particula> particulas = new ArrayList<Particula>();

    private final float[] colors = {
        0.85f, 0.90f, 0.95f, 0.5f,
        0.80f, 0.86f, 0.92f, 0.5f,
        0.75f, 0.82f, 0.90f, 0.5f
    };

    public Gotas(Context context, Material material) {
        this.activity = (MainActivity) context;
        this.material = material;
        crearBuffers();
        crearColorBuffer();
        crearShaders();
        crearParticulas();
    }

    public void setViewMatrix(float[] view) {
        this.viewMatrix = view;
    }

    public void setProjectionMatrix(float[] projection) {
        this.projectionMatrix = projection;
    }

    private void crearBuffers() {
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);
    }

    private void crearColorBuffer() {
        ByteBuffer bb = ByteBuffer.allocateDirect(colors.length * 4);
        bb.order(ByteOrder.nativeOrder());
        colorBuffer = bb.asFloatBuffer();
        colorBuffer.put(colors);
        colorBuffer.position(0);
    }

    private void crearParticulas() {
        List<Material.EffectBox> allBoxes = material.getBoxes();
		int totalAAsignar = 0;
		for(Material.EffectBox b : allBoxes){
			if(b.effectType == 3){
				int num = b.numParticles;
				int limit = Math.min(num, 500); 
				for(int i=0; i<limit && totalAAsignar < MAX_PARTICULAS_SISTEMA; i++){
					Particula p = new Particula();
					p.box = b;
					resetParticula(p, i);
					particulas.add(p);
					totalAAsignar++;
				}
			}
		}
    }

    private void getEmisor(Material.EffectBox b, float[] out) {
        out[0] = b.position[0] + randomRange(-b.scale * 0.2f, b.scale * 0.2f);
        out[1] = b.position[1] + (b.scale * 0.5f) + 0.02f;
        out[2] = b.position[2] + randomRange(-b.scale * 0.2f, b.scale * 0.2f);
    }

    private void resetParticula(Particula p, int index) {
        float[] c = new float[3];
        getEmisor(p.box, c);
        p.startX = c[0];
        p.startY = c[1];
        p.startZ = c[2];
        p.x = p.startX;
        p.y = p.startY;
        p.z = p.startZ;
        float spread;
        float forceY;
        if (index < 2) {
            spread = 0.2f;
            forceY = 1.8f;
        } else {
            spread = 0.8f;
            forceY = 2.5f;
        }
        float angle = random.nextFloat() * (float)Math.PI * 2f;
        float vx = (float)Math.cos(angle) * spread;
        float vz = (float)Math.sin(angle) * spread;
        p.vx = vx * (2f + random.nextFloat() * 2f);
        p.vy = forceY + random.nextFloat() * 1.5f;
        p.vz = vz * (2f + random.nextFloat() * 2f);
        p.life = 0f;
        p.maxLife = 0.6f + random.nextFloat() * 0.4f;
        p.scale = 0.25f + random.nextFloat() * 0.25f;
    }

	public void actualizar(float delta) {
		emitTimer += delta;
		if (emitTimer >= EMIT_INTERVAL && !particulas.isEmpty()) {
			emitTimer = 0f;
			int i = random.nextInt(particulas.size());
			Particula p = particulas.get(i);
			if (p != null) {
				resetParticula(p, i);
			}
		}
		for (int i = 0; i < particulas.size(); i++) {
			Particula p = particulas.get(i);
			if (p == null || p.box == null || !material.getBoxes().contains(p.box)) continue;
			p.life += delta;
			float t = p.life / p.maxLife;
			if (t >= 1.0f) {
				resetParticula(p, i);
				continue;
			}
			p.x = p.startX + p.vx * t;
			p.z = p.startZ + p.vz * t;
			float arc = 4f * t * (1f - t);
			p.y = p.startY + (p.vy * t) - arc;
			p.scale += delta * 0.05f;
		}
	}
	
	public void rebuildParticles() {
		particulas.clear();
		crearParticulas();
	}

    public void dibujar() {
        if (viewMatrix == null || projectionMatrix == null) return;
        GLES20.glUseProgram(program);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(
            GLES20.GL_SRC_ALPHA,
            GLES20.GL_ONE_MINUS_SRC_ALPHA
        );
        GLES20.glEnableVertexAttribArray(aPosition);
        GLES20.glEnableVertexAttribArray(aColor);
        GLES20.glVertexAttribPointer(
            aPosition, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer
        );
        GLES20.glVertexAttribPointer(
            aColor, 4, GLES20.GL_FLOAT, false, 0, colorBuffer
        );
        for (int i = 0; i < particulas.size(); i++) {
            Particula p = particulas.get(i);
            if (p == null) continue;
            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.translateM(modelMatrix, 0, p.x, p.y, p.z);
            modelMatrix[0]  = viewMatrix[0];
            modelMatrix[1]  = viewMatrix[4];
            modelMatrix[2]  = viewMatrix[8];
            modelMatrix[4]  = viewMatrix[1];
            modelMatrix[5]  = viewMatrix[5];
            modelMatrix[6]  = viewMatrix[9];
            modelMatrix[8]  = viewMatrix[2];
            modelMatrix[9]  = viewMatrix[6];
            modelMatrix[10] = viewMatrix[10];
            Matrix.scaleM(modelMatrix, 0, p.scale, p.scale, p.scale);
            Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);
            GLES20.glUniformMatrix4fv(uMVP, 1, false, mvpMatrix, 0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
        }
        GLES20.glDisableVertexAttribArray(aColor);
        GLES20.glDisableVertexAttribArray(aPosition);
        activity.requestRender();
    }

    private void crearShaders() {
        String vertexCode =
            "attribute vec3 aPosition;\n" +
            "attribute vec4 aColor;\n" +
            "uniform mat4 uMVP;\n" +
            "varying vec4 vColor;\n" +
            "void main() {\n" +
            "  gl_Position = uMVP * vec4(aPosition, 1.0);\n" +
            "  vColor = aColor;\n" +
            "}";
        String fragmentCode =
            "precision mediump float;\n" +
            "varying vec4 vColor;\n" +
            "void main() {\n" +
            "  gl_FragColor = vColor;\n" +
            "}";
        int vs = compile(GLES20.GL_VERTEX_SHADER, vertexCode);
        int fs = compile(GLES20.GL_FRAGMENT_SHADER, fragmentCode);
        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vs);
        GLES20.glAttachShader(program, fs);
        GLES20.glLinkProgram(program);
        aPosition = GLES20.glGetAttribLocation(program, "aPosition");
        aColor = GLES20.glGetAttribLocation(program, "aColor");
        uMVP = GLES20.glGetUniformLocation(program, "uMVP");
    }

    private int compile(int type, String code) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, code);
        GLES20.glCompileShader(s);
        return s;
    }

    private float randomRange(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private static class Particula {
        float x, y, z;
        float startX, startY, startZ;
        float vx, vy, vz;
        float life;
        float maxLife;
        float scale;
        Material.EffectBox box;
    }
	
}
