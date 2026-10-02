package ar.axt.controles;

import android.content.Context;
import android.opengl.Matrix;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import ar.axt.nopeby.MyRenderer;

public class Tacto {

	private final Context context;
	private int anchoPx;
	private int altoPx;
	private float radioToquePx = 60f;

	public Tacto(Context ctx) {
		this.context = ctx;
		pantallaEnXp();
	}

	public static class Ray {
		public final float[] origin = new float[3];
		public final float[] direction = new float[3];
	}

	public void setResolucion(int ancho, int alto) {
		this.anchoPx = ancho;
		this.altoPx = alto;
	}

	public void pantallaEnXp() {
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		if (wm == null) return;
		DisplayMetrics metrics = new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(metrics);
		anchoPx = metrics.widthPixels;
		altoPx = metrics.heightPixels;
	}

	public int getAnchoPx() { return anchoPx; }
	public int getAltoPx() { return altoPx; }

	public Ray crearRay(float xPix, float yPix, MyRenderer renderer) {
		if (renderer == null || renderer.ajustesDeCamara == null) return null;
		float xNdc = (2.0f * xPix / anchoPx) - 1.0f;
		float yNdc = 1.0f - (2.0f * yPix / altoPx);
		float[] viewMatrix = new float[16];
		AjustesDeCamara cam = renderer.ajustesDeCamara;
		float radX = (float) Math.toRadians(cam.angleX);
		float radY = (float) Math.toRadians(cam.angleY);
		float eyeX = (float) (cam.centerX + cam.distance * Math.cos(radY) * Math.sin(radX));
		float eyeY = (float) (cam.centerY + cam.distance * Math.sin(radY));
		float eyeZ = (float) (cam.centerZ + cam.distance * Math.cos(radY) * Math.cos(radX));
		Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, cam.centerX, cam.centerY, cam.centerZ, 0f, 1f, 0f);
		float[] projectionMatrix = new float[16];
		float aspect = (float) anchoPx / altoPx;
		Matrix.frustumM(projectionMatrix, 0, -aspect * 0.0414f, aspect * 0.0414f, -0.0414f, 0.0414f, 0.1f, 100.0f);
		float[] viewProj = new float[16];
		float[] invViewProj = new float[16];
		Matrix.multiplyMM(viewProj, 0, projectionMatrix, 0, viewMatrix, 0);
		if (!Matrix.invertM(invViewProj, 0, viewProj, 0)) return null;
		float[] nearClip = { xNdc, yNdc, -1f, 1f };
		float[] farClip  = { xNdc, yNdc,  1f, 1f };
		float[] nearWorld = new float[4];
		float[] farWorld  = new float[4];
		Matrix.multiplyMV(nearWorld, 0, invViewProj, 0, nearClip, 0);
		Matrix.multiplyMV(farWorld,  0, invViewProj, 0, farClip,  0);
		Ray ray = new Ray();
		for (int i = 0; i < 3; i++) {
			ray.origin[i] = nearWorld[i] / nearWorld[3];
			ray.direction[i] = (farWorld[i] / farWorld[3]) - ray.origin[i];
		}
		normalize(ray.direction); return ray;
	}

	private void normalize(float[] v) {
		float len = (float) Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
		if (len == 0f) return;
		v[0] /= len; v[1] /= len; v[2] /= len;
	}

	public float[] intersectarRayoConPlano(Ray ray, float[] puntoPlano, float[] normalPlano) {
		float denom = normalPlano[0] * ray.direction[0] + normalPlano[1] * ray.direction[1] + normalPlano[2] * ray.direction[2];
		if (Math.abs(denom) < 1e-6f) return null;
		float t = ((puntoPlano[0] - ray.origin[0]) * normalPlano[0] + (puntoPlano[1] - ray.origin[1]) * normalPlano[1] + (puntoPlano[2] - ray.origin[2]) * normalPlano[2]) / denom;
		if (t < 0f) return null;
		return new float[] {
				ray.origin[0] + ray.direction[0] * t,
				ray.origin[1] + ray.direction[1] * t,
				ray.origin[2] + ray.direction[2] * t
		};
	}
}