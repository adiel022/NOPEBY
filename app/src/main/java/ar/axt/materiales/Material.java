package ar.axt.materiales;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.List;
import ar.axt.controles.Gismo;
import java.util.ArrayList;
import ar.axt.nopeby.MyRenderer;
import ar.axt.leerobj.ObjetosCargados;

public class Material {

    private Context context;
    private int program;
    private int aPosition;
    private int aColor;
    private int uMVP;
    private List<ObjetosCargados.SubMesh> meshes;

    public float[] position = {0f,0f,0f};
    public float[] rotation = {0f,0f,0f};
    public float scale = 1f;

    private final float[] modelMatrix = new float[16];
    private final float[] finalMVP = new float[16];
	private MyRenderer render;
	private List<EffectBox> boxes = new ArrayList<EffectBox>();
	public EffectBox selectedBox;
	
	public EffectBox getSelectedBox(){
		return selectedBox;
	}
	public void removeBox(EffectBox b){
		boxes.remove(b);
	}

	public Material(Context context, MyRenderer render) {
		this.context = context;
		this.render = render;
		initShader();
		loadModel();
	}
	
	public static class EffectBox {
		public String id;
		public int effectType;
		public float[] position = {0,0,0};
		public float[] rotation = {0,0,0};
		public float scale = 1f;
		public int numParticles = 100;
		public EffectBox(String id, int effectType){
			this.id = id;
			this.effectType = effectType;
		}
	}
	
	public List<EffectBox> getBoxes(){
		return boxes;
	}

    private void loadModel() {
        try {ObjetosCargados loader = new ObjetosCargados(context);
            meshes = loader.loadFromAssets("box.obj");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
    // Inicializar shadejr
    private void initShader() {
        String vertexShaderCode =
            "attribute vec3 aPosition;\n" +
            "attribute vec3 aColor;\n" +
            "uniform mat4 uMVP;\n" +
            "varying vec3 vColor;\n" +
            "void main(){\n" +
            "   gl_Position = uMVP * vec4(aPosition,1.0);\n" +
            "   vColor = aColor;\n" +
            "}";
        String fragmentShaderCode =
            "precision mediump float;\n" +
            "varying vec3 vColor;\n" +
            "void main(){\n" +
            "   gl_FragColor = vec4(vColor,1.0);\n" +
            "}";
        int vertexShader =
            loadShader(
			GLES20.GL_VERTEX_SHADER,
			vertexShaderCode
		);
        int fragmentShader =
            loadShader(
			GLES20.GL_FRAGMENT_SHADER,
			fragmentShaderCode
		);
        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        aPosition = GLES20.glGetAttribLocation(
		program,"aPosition");
        aColor = GLES20.glGetAttribLocation(
		program,"aColor");
        uMVP = GLES20.glGetUniformLocation(
		program,"uMVP");
    }

	public void draw(float[] vpMatrix) {
		if(meshes == null) return;
		GLES20.glUseProgram(program);
		GLES20.glEnableVertexAttribArray(aPosition);
		GLES20.glEnableVertexAttribArray(aColor);
		if (render != null && !render.isExporting && (render.activity == null || !render.activity.isPlayingAnimation)) {
			for(EffectBox box : boxes){
				Matrix.setIdentityM(modelMatrix,0);
				Matrix.translateM(
					modelMatrix,0,
					box.position[0],
					box.position[1],
					box.position[2]);
				Matrix.rotateM(modelMatrix,0,
				box.rotation[0],1,0,0);
				Matrix.rotateM(modelMatrix,0,
				box.rotation[1],0,1,0);
				Matrix.rotateM(modelMatrix,0,
				box.rotation[2],0,0,1);
				Matrix.scaleM(modelMatrix,0,
				box.scale,box.scale,box.scale);
				Matrix.multiplyMM(finalMVP,0,
				vpMatrix,0,modelMatrix,0);
				GLES20.glUniformMatrix4fv(
				uMVP,1,false,finalMVP,0);
				drawMeshes();
			}
		}
		GLES20.glDisableVertexAttribArray(aPosition);
		GLES20.glDisableVertexAttribArray(aColor);
	}
	
	private void drawMeshes(){
		for(ObjetosCargados.SubMesh mesh : meshes){
			FloatBuffer vertexBuffer =
			mesh.vertexBuffer;
			FloatBuffer colorBuffer =
			mesh.colorBuffer;
			ShortBuffer indexBuffer =
			mesh.indexBuffer;
			vertexBuffer.position(0);
			GLES20.glVertexAttribPointer(
			aPosition,3,GLES20.GL_FLOAT,
		    false,0,vertexBuffer);
			colorBuffer.position(0);
			GLES20.glVertexAttribPointer(
			aColor,3,GLES20.GL_FLOAT,
			false,0,colorBuffer);
			indexBuffer.position(0);
			GLES20.glDrawElements(
				GLES20.GL_TRIANGLES,
				mesh.numIndices,
				GLES20.GL_UNSIGNED_SHORT,
				indexBuffer);
		}
	}

    private int loadShader(
        int type,
        String shaderCode){
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader,shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
	
	public void addEffectBox(int effectType){
		String id = "box_" + render.cantidadDeEfectos;
		EffectBox b =new EffectBox(id, effectType);
		b.numParticles = 300;
		boxes.add(b);
		selectedBox = b;
		render.cantidadDeEfectos++;
		actualizarCentroBox();
	}
	
	public void actualizarCentroBox(){
		if(selectedBox == null || render.gizmo == null) return;
        if (render.interaccionGismo != null) {
            render.interaccionGismo.actualizarCentroGizmo();
        }
		if(render.rayosInteraccion != null) {
			render.rayosInteraccion.selectedMesh = null;
			render.bone.clearSelectedBone();
		}
	}

	public void selectBox(String id){
		for(EffectBox b : boxes){
			if(b.id.equals(id)){
				if(b == selectedBox){ selectedBox = null; return;
				} selectedBox = b; actualizarCentroBox(); return;
			}
		} selectedBox = null;
	}
	
	public void syncSelectedBoxWithGizmo(Gismo gizmo) {
		if (selectedBox == null || gizmo == null) return;
		selectedBox.position[0] = gizmo.getPosition()[0];
		selectedBox.position[1] = gizmo.getPosition()[1];
		selectedBox.position[2] = gizmo.getPosition()[2];
		selectedBox.rotation[0] = gizmo.getRotation()[0];
		selectedBox.rotation[1] = gizmo.getRotation()[1];
		selectedBox.rotation[2] = gizmo.getRotation()[2];
		selectedBox.scale = gizmo.getScale() / 4.0f;
	}
	
}
