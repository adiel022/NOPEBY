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

public class Tierra {

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
		-0.06f,  0.06f, 0.0f,
		-0.06f, -0.06f, 0.0f,
		0.06f, -0.06f, 0.0f,
		-0.06f,  0.06f, 0.0f,
		0.06f, -0.06f, 0.0f,
		0.06f,  0.06f, 0.0f
	};
	
	private final float[] texCoords = {
		0f,0f, 0f,1f, 1f,1f,
		0f,0f, 1f,1f, 1f,0f
	};

    private FloatBuffer vertexBuffer;
	private FloatBuffer texBuffer;
    private final Random random = new Random();
    private final List<Particula> particulas = new ArrayList<Particula>();
    private Material material;

	private int texturaUno;
	private int texturaDos;
	private int texturaTres;
	private int aTexCoord;
	private int uTexture;

    public Tierra(
        Context context, Material material){
        this.activity = (MainActivity) context;
		texturaUno = cargarTextura(context, ar.axt.nopeby.R.drawable.particulatierrauno);
		texturaDos = cargarTextura(context, ar.axt.nopeby.R.drawable.particulatierrados);
		texturaTres = cargarTextura(context, ar.axt.nopeby.R.drawable.particulatierratres);
        this.material = material;
        crearBuffers();
        crearShaders(context);
        crearParticulas();
    }

    private void crearBuffers(){
        ByteBuffer bb =  ByteBuffer.allocateDirect( vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);
		ByteBuffer tb = ByteBuffer.allocateDirect(
		texCoords.length * 4);
		tb.order(ByteOrder.nativeOrder());
		texBuffer = tb.asFloatBuffer();
		texBuffer.put(texCoords);
		texBuffer.position(0);
    }
	
	private int cargarTextura(Context context, int resourceId){
		int[] texture = new int[1];
		GLES20.glGenTextures(1, texture, 0);
		android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeResource(
		context.getResources(),resourceId);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		android.opengl.GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
		bitmap.recycle();
		return texture[0];
	}

    private void crearParticulas(){
        List<Material.EffectBox> allBoxes = material.getBoxes();
		int totalAAsignar = 0;
		for(Material.EffectBox b : allBoxes){
			if(b.effectType == 2){
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
        float alturaBase = b.scale * 2f;
        if(index % 3 == 0){p.alturaMaxima =  alturaBase;
        }else if(index % 3 == 1){
			p.alturaMaxima = alturaBase * 0.5f;
        }else{ p.alturaMaxima =  alturaBase * 0.75f;}
    }

    public void rebuildParticles(){
        particulas.clear();
        crearParticulas();
    }

    private void resetParticula(Particula p){
		if(p.box == null){return;}
		Material.EffectBox b = p.box;
        int c = random.nextInt(3);
        switch(c){
			case 0: p.textura = texturaUno; break;
			case 1: p.textura = texturaDos; break;
			default: p.textura = texturaTres; break;
		}
        p.scale = 0.3f +
			random.nextFloat() * 0.7f;
        p.angulo = random.nextFloat() * 360f;
        p.velocidadAngular = 60f +
			random.nextFloat() * 120f;
        p.radio = random.nextFloat() *
			(b.scale * 0.35f);
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
		int vertexShader =
			compileShader(GLES20.GL_VERTEX_SHADER, vertexCode);
		int fragmentShader =
			compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentCode);
		program = GLES20.glCreateProgram();
		GLES20.glAttachShader(program, vertexShader);
		GLES20.glAttachShader(program, fragmentShader);
		GLES20.glLinkProgram(program);
		aPosition = GLES20.glGetAttribLocation(program, "aPosition");
		aTexCoord = GLES20.glGetAttribLocation(program, "aTexCoord");
		uMVP = GLES20.glGetUniformLocation(program, "uMVP");
		uTexture = GLES20.glGetUniformLocation(program, "uTexture");
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
		GLES20.glVertexAttribPointer(aPosition,3,GLES20.GL_FLOAT,false,0,vertexBuffer);
		GLES20.glEnableVertexAttribArray(aTexCoord);
		GLES20.glVertexAttribPointer(aTexCoord, 2, GLES20.GL_FLOAT, false, 0, texBuffer);
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		for(int i = 0;i < particulas.size(); i++){
			Particula p = particulas.get(i);
			if(p == null){continue;}
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, p.textura);
			GLES20.glUniform1i(uTexture,0);
			Matrix.setIdentityM(modelMatrix,0);
			Matrix.translateM(modelMatrix,0, p.x,p.y,p.z);
			modelMatrix[0]  = viewMatrix[0];
			modelMatrix[1]  = viewMatrix[4];
			modelMatrix[2]  = viewMatrix[8];
			modelMatrix[4]  = viewMatrix[1];
			modelMatrix[5]  = viewMatrix[5];
			modelMatrix[6]  = viewMatrix[9];
			modelMatrix[8]  = viewMatrix[2];
			modelMatrix[9]  = viewMatrix[6];
			modelMatrix[10] = viewMatrix[10];
			Matrix.scaleM(modelMatrix,0, p.scale,p.scale,p.scale);
			Matrix.multiplyMM(tempMatrix,0, viewMatrix,0,modelMatrix,0);
			Matrix.multiplyMM(mvpMatrix,0, projectionMatrix,0,tempMatrix,0);
			GLES20.glUniformMatrix4fv(uMVP,1,false,mvpMatrix,0);
			GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,6);
		}
		GLES20.glDisableVertexAttribArray(aPosition);
		GLES20.glDisableVertexAttribArray(aTexCoord);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
		activity.requestRender();
	}

	private static class Particula{
		int textura;
		float x;
		float y;
		float z;
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
