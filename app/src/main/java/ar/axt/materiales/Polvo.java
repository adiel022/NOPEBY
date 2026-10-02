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

public class Polvo {

    private static final int MAX_PARTICULAS_SISTEMA = 5000;
    private static final float VELOCIDAD_SUBIDA = 3f;
    private int program;
    private int aPosition;
    private int uMVP;
    private int uColor;
    private float[] projectionMatrix;
    private float[] viewMatrix;
    private final float[] modelMatrix =
	new float[16];
    private final float[] tempMatrix =
	new float[16];
    private final float[] mvpMatrix =
	new float[16];

    private MainActivity activity;
    public void setViewMatrix(float[] view){
        this.viewMatrix = view;}
    public void setProjectionMatrix(float[] projection){
        this.projectionMatrix = projection;}

    private final float[] vertices = {
        0.0f,  0.06f, 0.0f,
		-0.06f,-0.06f, 0.0f,
        0.06f,-0.06f, 0.0f};

    private FloatBuffer vertexBuffer;
    private final Random random = new Random();
    private final List<Particula> particulas = new ArrayList<Particula>();
    private Material material;

    private final float[][] tonos = {
		{0.90f, 0.90f, 0.90f},
		{0.75f, 0.75f, 0.75f},
		{0.60f, 0.60f, 0.60f},
		{0.45f, 0.45f, 0.45f}
	};
	
    public Polvo(
        Context context, Material material){
        this.activity = (MainActivity) context;
        this.material = material;
        crearBuffers();
        crearShaders(context);
        crearParticulas();
    }

    private void crearBuffers(){
        ByteBuffer bb =
			ByteBuffer.allocateDirect(
			vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);
    }

    private void crearParticulas(){
        List<Material.EffectBox> allBoxes = material.getBoxes();
		int totalAAsignar = 0;
		for(Material.EffectBox b : allBoxes){
			if(b.effectType == 5 || b.effectType == 6){
				int num = b.numParticles;
				for(int i=0; i<num && totalAAsignar < MAX_PARTICULAS_SISTEMA; i++){
					Particula p = new Particula();
					p.box = b;
					configurarAltura(p, i);
					resetParticula(p);
					particulas.add(p);
					totalAAsignar++;
				}
			}
		}
	}

    private void configurarAltura(
		Particula p,int index){
		Material.EffectBox b = p.box;
		float alturaBase = (b.scale * 2f) / 2.5f;
		if(index % 3 == 0){
			p.alturaMaxima = alturaBase;
		}else if(index % 3 == 1){
			p.alturaMaxima = alturaBase * 0.5f;
		}else{
			p.alturaMaxima = alturaBase * 0.75f;
		}
	}

    public void rebuildParticles(){
        particulas.clear();
        crearParticulas();
    }

    private void resetParticula(Particula p){
		if(p.box == null){return;}
		Material.EffectBox b = p.box;
        int c = random.nextInt(4);
        p.r = tonos[c][0];
        p.g = tonos[c][1];
        p.b = tonos[c][2];
        p.scale = 0.3f +
			random.nextFloat() * 0.7f;
        p.angulo = random.nextFloat() * 360f;
        p.velocidadAngular = 60f +
			random.nextFloat() * 120f;
		p.radio = random.nextFloat() *
			(b.scale * 0.35f * 2.5f);
        p.altura = random.nextFloat() *
			p.alturaMaxima; p.oscilacion =
			random.nextFloat() * 6.28f;}

    public void actualizar(
        float delta){for(int i = 0;  i < particulas.size();i++){
			Particula p = particulas.get(i);
            if(p == null){continue;}
            if(p.box == null || !material.getBoxes().contains(p.box)){continue;}
            p.altura += delta * VELOCIDAD_SUBIDA;
            p.angulo += p.velocidadAngular * delta;
            float rad = (float)Math.toRadians(p.angulo);
            Material.EffectBox b = p.box;
            float alturaOndulada = p.altura +
				(float)Math.sin(p.oscilacion +  p.altura * 4f)* 0.15f;p.x = b.position[0] +
				(float)Math.cos(rad) * p.radio; p.z =
				b.position[2] + (float)Math.sin(rad) * p.radio;
            p.y = b.position[1] + alturaOndulada;
            p.scale += delta * 0.1f;
            if(p.altura > p.alturaMaxima){
                resetParticula(p);
            }
        }
    }

	private void crearShaders(Context context){
		String vertexCode =
			"attribute vec3 aPosition; \n" +
			"uniform mat4 uMVP; \n" +
			"void main(){ \n" +
			" gl_Position = uMVP * vec4(aPosition,1.0); \n" +
			"}";
		String fragmentCode =
			"precision mediump float; \n" +
			"uniform vec3 uColor; \n" +
			"void main(){ \n" +
			" gl_FragColor = vec4(uColor,1.0); \n" +
			"}";
		int vertexShader =
			compileShader(GLES20.GL_VERTEX_SHADER, vertexCode);
		int fragmentShader =
			compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentCode);
		program = GLES20.glCreateProgram();
		GLES20.glAttachShader(program, vertexShader);
		GLES20.glAttachShader(program, fragmentShader);
		GLES20.glLinkProgram(program);
		aPosition = GLES20.glGetAttribLocation(program, "aPosition");
		uMVP = GLES20.glGetUniformLocation(program, "uMVP");
		uColor = GLES20.glGetUniformLocation(program, "uColor");
	}

	private int compileShader(int type, String code){
		int shader = GLES20.glCreateShader(type);
		GLES20.glShaderSource(shader, code);
		GLES20.glCompileShader(shader);
		return shader;
	}

	public void dibujar(){
		GLES20.glUseProgram(program);
		GLES20.glEnableVertexAttribArray(aPosition);
		GLES20.glVertexAttribPointer(
			aPosition,3,GLES20.GL_FLOAT,false,0,vertexBuffer);
		for(int i = 0;i < particulas.size(); i++){
			Particula p = particulas.get(i);
			if(p == null){continue;}
			Matrix.setIdentityM(modelMatrix,0);
			Matrix.translateM(modelMatrix,0,  p.x,p.y,p.z);
			// BILLBOARD (igual que Humo)
			modelMatrix[0]  = viewMatrix[0];
			modelMatrix[1]  = viewMatrix[4];
			modelMatrix[2]  = viewMatrix[8];
			modelMatrix[4]  = viewMatrix[1];
			modelMatrix[5]  = viewMatrix[5];
			modelMatrix[6]  = viewMatrix[9];
			modelMatrix[8]  = viewMatrix[2];
			modelMatrix[9]  = viewMatrix[6];
			modelMatrix[10] = viewMatrix[10];
			Matrix.scaleM(modelMatrix,0,  p.scale,p.scale,p.scale);
			Matrix.multiplyMM(tempMatrix,0,  viewMatrix,0,modelMatrix,0);
			Matrix.multiplyMM(mvpMatrix,0,  projectionMatrix,0,tempMatrix,0);
			GLES20.glUniformMatrix4fv(
				uMVP,1,false,mvpMatrix,0);
			GLES20.glUniform3f(
				uColor,p.r,p.g,p.b);
			GLES20.glDrawArrays(
				GLES20.GL_TRIANGLES,0,3);}
		GLES20.glDisableVertexAttribArray(
			aPosition);
		activity.requestRender();
	}

	private static class Particula{
		float x;
		float y;
		float z;
		float r;
		float g;
		float b;
		float angulo;
		float velocidadAngular;
		float radio;
		float altura;
		float oscilacion;
		float alturaMaxima;
		float scale;
		Material.EffectBox box;
	}

}
