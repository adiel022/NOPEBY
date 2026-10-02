package ar.axt.controles;

import android.opengl.Matrix;
import ar.axt.nopeby.MainActivity;
import ar.axt.nopeby.MyRenderer;

public class AjustesDeCamara {
    public float angleX = 30f;
    public float angleY = 45f;
    public float angleRoll = 0f;
    public float distance = 15f;
    public float centerX = 0f;
    public float centerY = 0f;
    public float centerZ = 0f;
    public float camOffsetX = 0f;
    public float camOffsetY = 0f;
    public float sceneRadius = distance * 1.5f;
    public float[] sceneCenter = { camOffsetX, camOffsetY, 0f };
    public float[] shadowMatrix = {1,0,0,0, 0,0,0,0, 0,0,1,0, 0,0,0,1};

    private MyRenderer renderer;
    private MainActivity activity;

    public AjustesDeCamara(MyRenderer renderer , MainActivity activity) {
        this.renderer = renderer;
        this.activity = activity;
    }

    public void changeRotation(float dx, float dy) {
        if (Math.abs(dx) > 50 || Math.abs(dy) > 50) return;
        float sens = 0.5f;
        angleX = (angleX - dx * sens) % 360f;
        if (angleX < 0) angleX += 360f;
        angleY += dy * sens;
        angleY = Math.max(-89f, Math.min(89f, angleY));
    }

    public void changeDistance(float scaleFactor) {
        distance *= (1.0f / scaleFactor);
        if (distance < 1f) distance = 1f;
        if (distance > 50f) distance = 50f;
    }

    public void pan(float dx, float dy) {
        float panSpeed = 0.002f * distance;
        float radX = (float) Math.toRadians(angleX);
        float radY = (float) Math.toRadians(angleY);
        float forwardX = (float) (Math.cos(radY) * Math.sin(radX));
        float forwardY = (float) (Math.sin(radY));
        float forwardZ = (float) (Math.cos(radY) * Math.cos(radX));
        float upX = 0f; float upY = 1f; float upZ = 0f;
        float rightX = forwardY * upZ - forwardZ * upY;
        float rightY = forwardZ * upX - forwardX * upZ;
        float rightZ = forwardX * upY - forwardY * upX;
        float len = (float) Math.sqrt(rightX * rightX + rightY * rightY + rightZ * rightZ);
        if (len > 0) {rightX /= len;rightY /= len;rightZ /= len; }
        float camUpX = rightY * forwardZ - rightZ * forwardY;
        float camUpY = rightZ * forwardX - rightX * forwardZ;
        float camUpZ = rightX * forwardY - rightY * forwardX;
        centerX -= (-dx * rightX + dy * camUpX) * panSpeed;
        centerY -= (-dx * rightY + dy * camUpY) * panSpeed;
        centerZ -= (-dx * rightZ + dy * camUpZ) * panSpeed;
    }

    public float[] getCameraPosition() {
        float radX = (float) Math.toRadians(angleX);
        float radY = (float) Math.toRadians(angleY);
        float x = (float) (Math.cos(radY) * Math.sin(radX));
        float y = (float) (Math.sin(radY));
        float z = (float) (Math.cos(radY) * Math.cos(radX));
        return new float[] {centerX + x * distance, centerY + y * distance, centerZ + z * distance};
    }

    public float[] getCameraForward() {
        float[] viewMatrix = renderer.viewMatrix;
        return new float[]{-viewMatrix[2], -viewMatrix[6], -viewMatrix[10]};
    }

    public float getSceneRadius() {return distance * 1.5f;}

    public void rotateCamera(float dx, float dy) {
        if (renderer != null) { changeRotation(dx, dy);
            activity.glSurfaceView.requestRender();
        }
    }

    public void zoomCamera(float scaleFactor) {
        if (renderer != null) {
            changeDistance(scaleFactor);
            activity.glSurfaceView.requestRender();
        }
    }

    public void panCamera(float dx, float dy) {
        if (renderer != null) {pan(dx, dy);
            activity.glSurfaceView.requestRender();
        }
    }

    public void rollCamera(float dAngle) {
        angleRoll = (angleRoll + dAngle) % 360f;
        if (angleRoll < 0) angleRoll += 360f;
        if (activity != null) {
            activity.glSurfaceView.requestRender();
        }
    }

    public float[] getUpVector() {
        float radX = (float) Math.toRadians(angleX);
        float radY = (float) Math.toRadians(angleY);
        float radRoll = (float) Math.toRadians(angleRoll);
        float fx = (float) -(Math.cos(radY) * Math.sin(radX));
        float fy = (float) -(Math.sin(radY));
        float fz = (float) -(Math.cos(radY) * Math.cos(radX));
        float rx = -fz;
        float ry = 0.0f;
        float rz = fx;
        float rLen = (float) Math.sqrt(rx * rx + rz * rz);
        if (rLen > 0.0001f) {rx /= rLen; rz /= rLen;
        } else { rx = 1.0f; rz = 0.0f; }
        float ux = ry * fz - rz * fy;
        float uy = rz * fx - rx * fz;
        float uz = rx * fy - ry * fx;
        float cosR = (float) Math.cos(radRoll);
        float sinR = (float) Math.sin(radRoll);
        return new float[] {
            ux * cosR + rx * sinR,
            uy * cosR + ry * sinR,
            uz * cosR + rz * sinR
        };
    }

}
