package ar.axt.sombras;

import android.content.Context;
import android.opengl.GLES20;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SimpleShader {
    public int aColorLocation;
    public int aPositionLocation;
    public int aTexCoordLocation;
    private final Context context;
    private int programId;
    public int uMVPLocation;
    public int uTextureLocation;
    public int uHasTextureLocation;

    public SimpleShader(Context context) {
        this.context = context;
    }

    public void load(String vertexAsset, String fragmentAsset) {
        String vertexSource = readAsset(vertexAsset);
        String fragmentSource = readAsset(fragmentAsset);
        int vertexShader = compileShader(35633, vertexSource);
        int fragmentShader = compileShader(35632, fragmentSource);
        this.programId = GLES20.glCreateProgram();
        GLES20.glAttachShader(this.programId, vertexShader);
        GLES20.glAttachShader(this.programId, fragmentShader);
        GLES20.glLinkProgram(this.programId);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(this.programId, 35714, linkStatus, 0);
        if (linkStatus[0] == 0) {
            String log = GLES20.glGetProgramInfoLog(this.programId);
            GLES20.glDeleteProgram(this.programId);
            throw new RuntimeException("Error linkeando programa: " + log);
        }
        this.aPositionLocation = GLES20.glGetAttribLocation(this.programId, "aPosition");
        this.aColorLocation = GLES20.glGetAttribLocation(this.programId, "aColor");
        this.aTexCoordLocation = GLES20.glGetAttribLocation(this.programId, "aTexCoord");
        this.uMVPLocation = GLES20.glGetUniformLocation(this.programId, "uMVP");
        this.uTextureLocation = GLES20.glGetUniformLocation(this.programId, "uTexture");
        this.uHasTextureLocation = GLES20.glGetUniformLocation(this.programId, "uHasTexture");
        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);
    }

    public int getProgram() {
        return this.programId;
    }

    private String readAsset(String name) {
        StringBuilder builder = new StringBuilder();
        try {
            InputStream is = this.context.getAssets().open(name);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            while (true) { String line = reader.readLine();
                if (line != null) { builder.append(line).append('\n');
                } else { reader.close(); is.close(); return builder.toString();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error leyendo shader: " + name, e);
        }
    }

    private int compileShader(int type, String src) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, src);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, 35713, compiled, 0);
        if (compiled[0] == 0) {
            String log = GLES20.glGetShaderInfoLog(shader);
            GLES20.glDeleteShader(shader);
            throw new RuntimeException("Error compilando shader: " + log);
        } return shader;
    }

}
