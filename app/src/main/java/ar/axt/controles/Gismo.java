package ar.axt.controles;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import java.nio.FloatBuffer;
import java.util.List;
import ar.axt.ficicas.Hitbox;
import ar.axt.leerobj.ObjetosCargados;

public class Gismo {
    private int program;
	private int uAlphaHandle;
    public float[] position = {0f, 0f, 0f};
    public float scale = 1.0f;
	public float[] rotation = {0f, 0f, 0f};
    private final Context context;
	public List<ObjetosCargados.SubMesh> meshes;
	public Hitbox[] hitboxes;

    public Gismo(Context ctx) {
		this.context = ctx;
		try {
			ObjetosCargados loader = new ObjetosCargados(ctx);
			meshes = loader.loadFromAssets("gizmo.obj");
			hitboxes = new Hitbox[meshes.size()];
			for(int i=0;i<meshes.size();i++){
				hitboxes[i] = createHitboxFromMesh(meshes.get(i));
			}
		} catch (Exception e) { e.printStackTrace(); }
		String vertexSrc = loadShaderFromAssets("simpleAlfha_vertex.glsl");
		String fragmentSrc = loadShaderFromAssets("simpleAlfha_fragment.glsl");
		if (vertexSrc == null || fragmentSrc == null) {
			vertexSrc = "attribute vec3 aPosition; attribute vec3 aColor; uniform mat4 uMVPMatrix; varying vec3 vColor; void main(){ vColor = aColor; gl_Position = uMVPMatrix * vec4(aPosition,1.0); }";
			fragmentSrc = "precision mediump float; varying vec3 vColor; uniform float uAlpha; void main(){ gl_FragColor = vec4(vColor,uAlpha); }";
		}
		program = createProgram(vertexSrc, fragmentSrc);
		uAlphaHandle = GLES20.glGetUniformLocation(program, "uAlpha");
	}

	private String loadShaderFromAssets(String filename) {
		if (context == null) return null;
		StringBuilder sb = new StringBuilder();
		try {
			java.io.InputStream is = context.getAssets().open(filename);
			java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(is));
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line).append("\n");
			} br.close(); return sb.toString();
		} catch (java.io.IOException e) { return null; }
	}

	public void draw(float[] vpMatrix) {
		if (meshes == null) return;
		GLES20.glUseProgram(program);
		int posHandle = GLES20.glGetAttribLocation(program, "aPosition");
		int colorHandle = GLES20.glGetAttribLocation(program, "aColor");
		int mvpHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
		GLES20.glEnableVertexAttribArray(posHandle);
		GLES20.glEnableVertexAttribArray(colorHandle);
		float[] model = new float[16];
		float[] mvp = new float[16];
		Matrix.setIdentityM(model,0);
		Matrix.translateM(model,0,position[0],position[1],position[2]);
		Matrix.rotateM(model,0,rotation[0],1,0,0);
		Matrix.rotateM(model,0,rotation[1],0,1,0);
		Matrix.rotateM(model,0,rotation[2],0,0,1);
		Matrix.scaleM(model,0,scale,scale,scale);
		Matrix.multiplyMM(mvp,0,vpMatrix,0,model,0);
		int mvpName = GLES20.glGetUniformLocation(program, "uMVP");
		if (mvpName == -1) mvpName = GLES20.glGetUniformLocation(program, "uMVPMatrix");
		GLES20.glUniformMatrix4fv(mvpName,1,false,mvp,0);
		GLES20.glUniform1f(uAlphaHandle, 0.25f);
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		for(int i=0;i<hitboxes.length;i++){}
		for(ObjetosCargados.SubMesh sm : meshes){
			sm.vertexBuffer.position(0);
			GLES20.glVertexAttribPointer(
				posHandle, 3, GLES20.GL_FLOAT,
				false, 0, sm.vertexBuffer);
			if(sm.colorBuffer != null){
				sm.colorBuffer.position(0);
				GLES20.glVertexAttribPointer(
				colorHandle, 3, GLES20.GL_FLOAT,
				false, 0, sm.colorBuffer);
			}
			GLES20.glDrawElements(
				GLES20.GL_TRIANGLES,
				sm.numIndices,
				GLES20.GL_UNSIGNED_SHORT,
				sm.indexBuffer
			);
		}
		GLES20.glDisableVertexAttribArray(posHandle);
		GLES20.glDisableVertexAttribArray(colorHandle);
		GLES20.glDisable(GLES20.GL_BLEND);
	}

    public void setPosition(float x, float y, float z) {
        position[0] = x;
        position[1] = y;
        position[2] = z;
    }

    public void setScale(float s) {
        this.scale = s;
    }

    private int loadShader(int type, String code) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);
        return shader;
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        int vs = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        int fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        int prog = GLES20.glCreateProgram();
        GLES20.glAttachShader(prog, vs);
        GLES20.glAttachShader(prog, fs);
        GLES20.glLinkProgram(prog);
        return prog;
    }
	
	private Hitbox createHitboxFromMesh(ObjetosCargados.SubMesh sm){
		Hitbox hb = new Hitbox();
		FloatBuffer vb = sm.vertexBuffer.duplicate();
		vb.position(0);
		float minX = Float.MAX_VALUE;
		float minY = Float.MAX_VALUE;
		float minZ = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE;
		float maxY = -Float.MAX_VALUE;
		float maxZ = -Float.MAX_VALUE;
		while(vb.remaining() >= 3){
			float x = vb.get();
			float y = vb.get();
			float z = vb.get();
			if(x < minX) minX = x;
			if(y < minY) minY = y;
			if(z < minZ) minZ = z;
			if(x > maxX) maxX = x;
			if(y > maxY) maxY = y;
			if(z > maxZ) maxZ = z;
		}
		float cx = (minX + maxX) * 0.5f;
		float cy = (minY + maxY) * 0.5f;
		float cz = (minZ + maxZ) * 0.5f;
		hb.localCenter[0] = cx;
		hb.localCenter[1] = cy;
		hb.localCenter[2] = cz;
		hb.halfSize[0] = (maxX - minX) * 0.5f;
		hb.halfSize[1] = (maxY - minY) * 0.5f;
		hb.halfSize[2] = (maxZ - minZ) * 0.5f;
		vb.position(0);
		return hb;
		
	}

    public float getX() {
        return position[0];
    }

    public float getY() {
        return position[1];
    }

    public float getZ() {
        return position[2];
    }

    public float[] getPosition() {
        return position;
    }

    public float getScale() {
        return scale;
    }
	
	public float[] getRotation() {
		return rotation;
	}

}

