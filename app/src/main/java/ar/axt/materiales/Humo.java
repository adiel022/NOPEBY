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
import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;
import ar.axt.nopeby.MainActivity;
import ar.axt.nopeby.R;

public class Humo {

    private static final int MAX_PARTICULAS_SISTEMA = 5000;
    private static final float VELOCIDAD_SUBIDA = 7f;
    private static final float DISPERSION = 0.02f;

    private int program;
    private int aPosition;
    private int uMVP;

	private float[] projectionMatrix;
	private float[] viewMatrix;
    private final float[] modelMatrix =
	new float[16];
    private final float[] tempMatrix =
	new float[16];
    private final float[] mvpMatrix =
	new float[16];
	private int texturaAmarilla;
	private int texturaNaranja;
	private int texturaOscura;
	private int aTexCoord;
	private int uTexture;

	private MainActivity activity;
	
	public void setViewMatrix(float[] view){
		this.viewMatrix = view;
	}

	public void setProjectionMatrix(float[] projection){
		this.projectionMatrix = projection;
	}

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

    private final Random random =
	new Random();
    private final List<Particula> particulas = new ArrayList<Particula>();
    private Material material;

	public Humo(Context context,
		Material material){
		this.activity = (MainActivity) context;
        texturaAmarilla = cargarTextura(context,
		R.drawable.particulahumodos);
        texturaNaranja = cargarTextura(context,
		R.drawable.particulahumouno);
        texturaOscura = cargarTextura(context,
		R.drawable.particulahumotres);
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
		ByteBuffer tb = ByteBuffer.allocateDirect(
		texCoords.length * 4);
		tb.order(ByteOrder.nativeOrder());
		texBuffer = tb.asFloatBuffer();
		texBuffer.put(texCoords);
		texBuffer.position(0);
    }
	
	private int cargarTextura(
		Context context, int resourceId){
		int[] texture = new int[1];
		GLES20.glGenTextures(1, texture, 0);
		Bitmap bitmap = BitmapFactory.decodeResource(
		context.getResources(),resourceId);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture[0]);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
		GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
		GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,
	    0, bitmap, 0); bitmap.recycle();
		return texture[0];
	}

    private void crearParticulas(){
		List<Material.EffectBox> allBoxes = material.getBoxes();
		int totalAAsignar = 0;
		for(Material.EffectBox b : allBoxes){
			if(b.effectType == 4 || b.effectType == 1){
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
		Particula p,
		int index){Material.EffectBox b = p.box;
		float alturaBase = b.scale * 4f;
		if(index % 3 == 0){ p.alturaMaxima = alturaBase; }
		else if(index % 3 == 1){ p.alturaMaxima = alturaBase * 0.5f; }
		else{p.alturaMaxima = alturaBase * 0.75f; }
	}
	
	public void rebuildParticles(){
		particulas.clear(); crearParticulas();
	}

    private void resetParticula(
        Particula p){
        Material.EffectBox b = p.box;
        float half =  b.scale * 0.5f;
        p.x = b.position[0]
        + randomRange(-half,half);
        p.z = b.position[2]
        + randomRange(-half,half);
		p.y = b.position[1]
		+ random.nextFloat()
		* p.alturaMaxima;
        int c = random.nextInt(3);
        switch(c){case 0:
	    p.textura = texturaAmarilla; break;
	    case 1: p.textura = texturaNaranja; break;
		default: p.textura = texturaOscura; break;}
        p.scale = 0.5f +
        random.nextFloat();
		float[] rot = b.rotation;
		float pitch = (float)Math.toRadians(rot[0]);
		float yaw   = (float)Math.toRadians(rot[1]);
		float dx = 0f;
		float dy = 1f;
		float dz = 0f;
		float cosP = (float)Math.cos(pitch);
		float sinP = (float)Math.sin(pitch);
		float ry = dy * cosP - dz * sinP;
		float rz = dy * sinP + dz * cosP;
		dy = ry;
		dz = rz;
		float cosY = (float)Math.cos(yaw);
		float sinY = (float)Math.sin(yaw);
		float rx = dx * cosY + dz * sinY;
		rz = -dx * sinY + dz * cosY;
		dx = rx;
		dz = rz;
		p.dirX = dx;
		p.dirY = dy;
		p.dirZ = dz;
    }

    public void actualizar(
        float delta){
        for(int i = 0;
        i < particulas.size(); i++){
        Particula p = particulas.get(i);
        if(p == null){ continue; }
		if (p.box == null || !material.getBoxes().contains(p.box)) {
            continue;
        }
		p.x += p.dirX * VELOCIDAD_SUBIDA * delta;
		p.y += p.dirY * VELOCIDAD_SUBIDA * delta;
		p.z += p.dirZ * VELOCIDAD_SUBIDA * delta;
        p.x += randomRange(
	   -DISPERSION, DISPERSION);
        p.z += randomRange(
	    -DISPERSION, DISPERSION);
        p.scale += delta * 0.2f;
        Material.EffectBox b = p.box;
		float maxY = b.position[1]
		+ p.alturaMaxima;
        if(p.y > maxY){
        resetParticula(p);
            }
        }
    }

	public void dibujar(){
		GLES20.glUseProgram(program);
		GLES20.glEnableVertexAttribArray(aPosition);
		GLES20.glVertexAttribPointer(
		aPosition,3,GLES20.GL_FLOAT,false,0,vertexBuffer);
		GLES20.glEnableVertexAttribArray(aTexCoord);
		GLES20.glVertexAttribPointer(aTexCoord,
	    2, GLES20.GL_FLOAT, false, 0, texBuffer);
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		for(int i = 0;i < particulas.size(); i++){
			Particula p = particulas.get(i);
			if(p == null){continue;}
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,p.textura);
			GLES20.glUniform1i(uTexture,0);
			Matrix.setIdentityM(modelMatrix,0);
			Matrix.translateM(modelMatrix,0,
			p.x,p.y,p.z);
			modelMatrix[0]  = viewMatrix[0];
			modelMatrix[1]  = viewMatrix[4];
			modelMatrix[2]  = viewMatrix[8];
			modelMatrix[4]  = viewMatrix[1];
			modelMatrix[5]  = viewMatrix[5];
			modelMatrix[6]  = viewMatrix[9];
			modelMatrix[8]  = viewMatrix[2];
			modelMatrix[9]  = viewMatrix[6];
			modelMatrix[10] = viewMatrix[10];
			Matrix.scaleM(modelMatrix,0,
		    p.scale,p.scale,p.scale);
			Matrix.multiplyMM(tempMatrix,0,
		    viewMatrix,0,modelMatrix,0);
			Matrix.multiplyMM(mvpMatrix,0,
		    projectionMatrix,0,tempMatrix,0);
			GLES20.glUniformMatrix4fv(
			uMVP,1,false,mvpMatrix,0);
			GLES20.glDrawArrays(
			GLES20.GL_TRIANGLES,0,6);}
		    GLES20.glDisableVertexAttribArray(aPosition);
			GLES20.glDisableVertexAttribArray(aTexCoord);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
			activity.requestRender();
	    }

    private void crearShaders(Context context){
		String vertexCode =
			"attribute vec3 aPosition; \n" +
			"attribute vec2 aTexCoord; \n" +
			"uniform mat4 uMVP; \n" +
			"varying vec2 vTexCoord; \n" +
			"void main(){\n" + " gl_Position = \n" +
			" uMVP * vec4(aPosition,1.0);\n" +
			" vTexCoord = aTexCoord; \n" + "}";
		String fragmentCode =
			"precision mediump float; \n" +
			"uniform sampler2D uTexture; \n" +
			"varying vec2 vTexCoord; \n" +
			"void main(){ \n" + " gl_FragColor = \n" +
			" texture2D(\n" + " uTexture, \n" +
			" vTexCoord);\n" + "}";
		int vertexShader = compileShader(
			GLES20.GL_VERTEX_SHADER, vertexCode);
		int fragmentShader = compileShader(
			GLES20.GL_FRAGMENT_SHADER, fragmentCode);
		program = GLES20.glCreateProgram();
		GLES20.glAttachShader(program,vertexShader);
		GLES20.glAttachShader(program,fragmentShader);
		GLES20.glLinkProgram(program);
		aPosition = GLES20.glGetAttribLocation(
			program, "aPosition");
		aTexCoord = GLES20.glGetAttribLocation(
			program, "aTexCoord");
		uMVP = GLES20.glGetUniformLocation(
			program, "uMVP");
		uTexture = GLES20.glGetUniformLocation(
			program, "uTexture");
	}

    private int compileShader(
        int type,
        String code){
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(
        shader, code);
        GLES20.glCompileShader( shader);
        return shader;
    }

    private float randomRange(
        float min,float max){
        return min +
        random.nextFloat()
        * (max - min);
    }

    private static class Particula{
		int textura;
        float x;
        float y;
        float z;
		float dirX;
		float dirY;
		float dirZ;
        float scale;
		float alturaMaxima;
        Material.EffectBox box;
    }
}
