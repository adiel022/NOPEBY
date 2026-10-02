package ar.axt.leerobj;

import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import ar.axt.nopeby.MainActivity;
import ar.axt.nopeby.MyRenderer;
import java.io.IOException;
import java.util.List;

public class NormalesLoader {
    private static final String TAG = "NormalesLoader";

    public static void aplicarMapaDeNormales(MainActivity activity, Uri uri) {
        if (activity == null || activity.renderer == null) return;
        MyRenderer.Mesh selectedMesh = activity.renderer.rayosInteraccion != null ? 
                activity.renderer.rayosInteraccion.selectedMesh : null;
        if (selectedMesh == null || selectedMesh.subMesh == null) {
            Toast.makeText(activity, "Debes seleccionar un modelo primero", Toast.LENGTH_SHORT).show();
            return; }
        try {
            TexturaLoader texLoader = new TexturaLoader(activity);
            Bitmap bitmap = texLoader.loadFromUri(uri);
            byte[] normalBytes = null;
            String mimeType = "image/png";
            try (java.io.InputStream is = activity.getContentResolver().openInputStream(uri)) {
                if (is != null) {
                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = is.read(buf)) != -1) {
                        baos.write(buf, 0, len);
                    } normalBytes = baos.toByteArray();
                }
            } catch (Exception e) { e.printStackTrace(); }
            if (bitmap != null) {
                selectedMesh.subMesh.pendingNormalMap = bitmap;
                if (normalBytes != null) {
                    selectedMesh.subMesh.embeddedNormalTexture = normalBytes;
                    selectedMesh.subMesh.embeddedNormalMimeType = mimeType;
                }
                List<MyRenderer.Mesh> relacionados = activity.renderer.rayosInteraccion.getMeshesRelacionados(selectedMesh);
                if (relacionados != null) {
                    for (MyRenderer.Mesh m : relacionados) {
                        if (m != null && m.subMesh != null) {
                            m.subMesh.pendingNormalMap = bitmap;
                            if (normalBytes != null) {
                                m.subMesh.embeddedNormalTexture = normalBytes;
                                m.subMesh.embeddedNormalMimeType = mimeType;
                            }
                        }
                    }
                }
                activity.glSurfaceView.requestRender();
                Toast.makeText(activity, "Mapa de Normales listo para aplicar", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Mapa de normales cargado correctamente desde Uri");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error cargando mapa de normales", e);
            Toast.makeText(activity, "Error al abrir la imagen de normales", Toast.LENGTH_SHORT).show();
        }
    }

}
