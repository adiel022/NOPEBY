package ar.axt.ficicas;

import android.opengl.Matrix;
import ar.axt.controles.Tacto;
import ar.axt.nopeby.MyRenderer;
import ar.axt.animar.Bones;
import ar.axt.controles.Gismo;
import java.util.List;

public class Hitbox {

	public static void actualizarTodo(List<MyRenderer.Mesh> meshes, Gismo gizmo, Bones bones) {
		if (meshes != null) {
			for (MyRenderer.Mesh m : meshes) {
				if (m != null && m.hitbox != null) {
					m.hitbox.updateFromMesh(m);
				}
			}
		}
		if (gizmo != null && gizmo.hitboxes != null) {
			for (Hitbox hb : gizmo.hitboxes) {
				if (hb != null) {
					hb.updateFromGizmo(gizmo);
				}
			}
		}
		if (bones != null) {
			for (Bones.Bone b : bones.getAllBones()) {
				if (b.hitbox != null) {
					b.hitbox.updateFromBone(b);
				}
			}
		}
	}

    public final float[] localCenter = new float[]{0f, 0f, 0f};
    public final float[] halfSize    = new float[]{0.5f, 0.5f, 0.5f};
    public final float[] position = new float[]{0f, 0f, 0f};
    public final float[] rotation = new float[]{0f, 0f, 0f};
    public final float[] scale    = new float[]{1f, 1f, 1f};
    public final float[] localMatrix = new float[16];
    public final float[] worldMatrix = new float[16];
	public final float[][] axes = new float[3][3];
	public final float[] center = new float[3];
	public final float[] halfSizeWorld = new float[3];

	public void updateFromMesh(MyRenderer.Mesh m) {
		float[] model = new float[16];
		Matrix.setIdentityM(model, 0);
		Matrix.translateM(model, 0, m.translation[0], m.translation[1], m.translation[2]);
		Matrix.rotateM(model, 0, m.rotation[0], 1, 0, 0);
		Matrix.rotateM(model, 0, m.rotation[1], 0, 1, 0);
		Matrix.rotateM(model, 0, m.rotation[2], 0, 0, 1);
		Matrix.scaleM(model, 0, m.scale[0], m.scale[1], m.scale[2]);
		updateFromMatrix(model);
	}

	public void updateFromGizmo(ar.axt.controles.Gismo g) {
		float[] model = new float[16];
		Matrix.setIdentityM(model, 0);
		Matrix.translateM(model, 0, g.position[0], g.position[1], g.position[2]);
		Matrix.rotateM(model, 0, g.rotation[0], 1, 0, 0);
		Matrix.rotateM(model, 0, g.rotation[1], 0, 1, 0);
		Matrix.rotateM(model, 0, g.rotation[2], 0, 0, 1);
		Matrix.scaleM(model, 0, g.scale, g.scale, g.scale);
		updateFromMatrix(model);
	}

	public void updateFromBone(ar.axt.animar.Bones.Bone b) {
		updateFromMatrix(b.worldMatrix);
	}
	
	public void updateFromMatrix(float[] model){
		System.arraycopy(model, 0, worldMatrix, 0, 16);
		float[] localCenter4 = {
			localCenter[0],  localCenter[1],  localCenter[2],  1f };
		float[] worldCenter4 = new float[4];
		Matrix.multiplyMV(worldCenter4, 0, model, 0, localCenter4, 0);
		center[0] = worldCenter4[0];
		center[1] = worldCenter4[1];
		center[2] = worldCenter4[2];
		// EXTRAER EJES + ESCALA REAL
		for (int i = 0; i < 3; i++) {
			int idx = i * 4;
			float x = model[idx + 0];
			float y = model[idx + 1];
			float z = model[idx + 2];
			float len = (float)Math.sqrt(x*x + y*y + z*z);
			if (len == 0f) len = 1f;
			axes[i][0] = x / len;
			axes[i][1] = y / len;
			axes[i][2] = z / len;
			halfSizeWorld[i] = halfSize[i] * len;
		}
	}

    public boolean intersects(Hitbox b) {
        float[] T = { b.center[0] - center[0],  b.center[1] - center[1],  b.center[2] - center[2] };
        float[][] R = new float[3][3];
        float[][] AbsR = new float[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                R[i][j] = dot(axes[i], b.axes[j]);
                AbsR[i][j] = Math.abs(R[i][j]) + 1e-6f;
            }
        }
        float[] t = { dot(T, axes[0]),  dot(T, axes[1]),  dot(T, axes[2]) };
        float ra, rb;
        for (int i = 0; i < 3; i++) {
            ra = halfSizeWorld[i];
            rb = b.halfSizeWorld[0] * AbsR[i][0]
				+ b.halfSizeWorld[1] * AbsR[i][1]
				+ b.halfSizeWorld[2] * AbsR[i][2];
            if (Math.abs(t[i]) > ra + rb) return false;
        }
        for (int i = 0; i < 3; i++) {
            ra = halfSizeWorld[0] * AbsR[0][i]
				+ halfSizeWorld[1] * AbsR[1][i]
				+ halfSizeWorld[2] * AbsR[2][i];
            rb = b.halfSizeWorld[i];
            if (Math.abs( t[0]*R[0][i] + t[1]*R[1][i] + t[2]*R[2][i]
				) > ra + rb) return false;
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                ra = halfSizeWorld[(i+1)%3] * AbsR[(i+2)%3][j]
					+ halfSizeWorld[(i+2)%3] * AbsR[(i+1)%3][j];
                rb = b.halfSizeWorld[(j+1)%3] * AbsR[i][(j+2)%3]
					+ b.halfSizeWorld[(j+2)%3] * AbsR[i][(j+1)%3];
                float val = Math.abs(
                    t[(i+2)%3] * R[(i+1)%3][j] -
                    t[(i+1)%3] * R[(i+2)%3][j]
                );
                if (val > ra + rb) return false;
            }
        } return true;
    }

    public boolean intersectsRay(Tacto.Ray ray, float[] outT) {
        float tMin = 0f;
        float tMax = Float.MAX_VALUE;
        float[] p = {
            center[0] - ray.origin[0],
            center[1] - ray.origin[1],
            center[2] - ray.origin[2] };
        for (int i = 0; i < 3; i++) {
            float e = dot(axes[i], p);
            float f = dot(axes[i], ray.direction);
            float h = halfSizeWorld[i];
            if (Math.abs(f) > 1e-6f) {
                float t1 = (e + h) / f;
                float t2 = (e - h) / f;
                if (t1 > t2) { float tmp = t1; t1 = t2; t2 = tmp; }
                tMin = Math.max(tMin, t1);
                tMax = Math.min(tMax, t2);
                if (tMin > tMax) return false;
            } else { if (-e - h > 0f || -e + h < 0f) return false; }
        } if (outT != null) outT[0] = tMin; return true;
    }

    private float dot(float[] a, float[] b) {
        return a[0]*b[0] + a[1]*b[1] + a[2]*b[2];
    }
	public void getVertices(float[] out) {
		float hx = halfSizeWorld[0];
		float hy = halfSizeWorld[1];
		float hz = halfSizeWorld[2];
		float[][] v = {
			{-hx,-hy,-hz},
			{-hx,-hy, hz},
			{-hx, hy,-hz},
			{-hx, hy, hz},
			{ hx,-hy,-hz},
			{ hx,-hy, hz},
			{ hx, hy,-hz},
			{ hx, hy, hz} };
		int index = 0;
		for (int i=0;i<8;i++) {
			float x = v[i][0];
			float y = v[i][1];
			float z = v[i][2];
			float wx =
				center[0] +
				axes[0][0]*x +
				axes[1][0]*y +
				axes[2][0]*z;
			float wy =
				center[1] +
				axes[0][1]*x +
				axes[1][1]*y +
				axes[2][1]*z;
			float wz =
				center[2] +
				axes[0][2]*x +
				axes[1][2]*y +
				axes[2][2]*z;
			out[index++] = wx;
			out[index++] = wy;
			out[index++] = wz;
		}
	}
	
}
