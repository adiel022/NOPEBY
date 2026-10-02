package ar.axt.nopeby;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import ar.axt.animar.ExportarVideo;

public class AjustesDeVideo extends Fragment {

    private RadioGroup rgResolucion;
    private RadioGroup rgFPS;
    private Button btnProduccion;
    private TextView tvProgreso;
    private boolean exportando = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.ajustes_video, container, false);
        rgResolucion = root.findViewById(R.id.rgResolucion);
        rgFPS = root.findViewById(R.id.rgFPS);
        btnProduccion = root.findViewById(R.id.btnProduccion);
        tvProgreso = root.findViewById(R.id.tvProgreso);
        rgResolucion.clearCheck();
        rgFPS.clearCheck();

        btnProduccion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { iniciarExportacion(); }});
            return root;
    }

    private void iniciarExportacion() {
        if (exportando) return;
        final MainActivity activity = (MainActivity) getActivity();
        activity.renderer.bone.selectedBone = null;
        activity.renderer.material.selectedBox = null;
        activity.renderer.rayosInteraccion.selectedMesh = null;
        if (activity == null || activity.nombreProyecto == null || activity.renderer == null) {
            Toast.makeText(getActivity(), "Error: Proyecto no cargado", Toast.LENGTH_SHORT).show();
            return;
        }
        int width = -1;
        int height = -1;
        int resId = rgResolucion.getCheckedRadioButtonId();
        if (resId == R.id.rb144p) { width = 256; height = 144; }
        else if (resId == R.id.rb240p) { width = 426; height = 240; }
        else if (resId == R.id.rb360p) { width = 640; height = 360; }
        else if (resId == R.id.rb480p) { width = 854; height = 480; }
        else if (resId == R.id.rb720p) { width = 1280; height = 720; }
        else if (resId == R.id.rb1080p) { width = 1920; height = 1080; }
        else if (resId == R.id.rb2k) { width = 2560; height = 1440; }
        else if (resId == R.id.rb4k) { width = 3840; height = 2160; }
        int fps = -1;
        int fpsId = rgFPS.getCheckedRadioButtonId();
        if (fpsId == R.id.rb15fps) fps = 15;
        else if (fpsId == R.id.rb24fps) fps = 24;
        else if (fpsId == R.id.rb30fps) fps = 30;
        else if (fpsId == R.id.rb60fps) fps = 60;
        else if (fpsId == R.id.rb120fps) fps = 120;
        if (width == -1 || fps == -1) {
            Toast.makeText(getActivity(), "Debes seleccionar Resolución y FPS", Toast.LENGTH_SHORT).show();
            return;
        }
        exportando = true;
        btnProduccion.setEnabled(false);
        tvProgreso.setVisibility(View.VISIBLE);
        tvProgreso.setText("Iniciando exportación...");
        final ExportarVideo exportar = new ExportarVideo(activity, activity.renderer, activity.nombreProyecto, width, height, fps);
        // Guardar resolucion original para restaurar después
        final int originalWidth = MyRenderer.surfaceWidth;
        final int originalHeight = MyRenderer.surfaceHeight;
        final int targetW = width;
        final int targetH = height;

        exportar.setListener(new ExportarVideo.ExportListener() {
            @Override
            public void onProgress(int currentFrame, int totalFrames, String phase) {
                tvProgreso.setText(phase + "\nFrame: " + currentFrame + " / " + totalFrames);
            }

            @Override
            public void onFinished(String videoPath) {
                exportando = false;
                btnProduccion.setEnabled(true);
                tvProgreso.setText("¡Exportación finalizada!");
                Toast.makeText(activity, "Video guardado en: " + videoPath, Toast.LENGTH_LONG).show();
                activity.glSurfaceView.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        activity.renderer.updateProjection(originalWidth, originalHeight);
                    }
                });
            }

            @Override
            public void onError(String error) {
                exportando = false;
                btnProduccion.setEnabled(true);
                tvProgreso.setText("Error: " + error);
                Toast.makeText(activity, "Error en exportación: " + error, Toast.LENGTH_SHORT).show();
                // Restaurar resolucion
                activity.glSurfaceView.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        activity.renderer.updateProjection(originalWidth, originalHeight);
                    }
                });
            }
        });

        activity.glSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                activity.renderer.updateProjection(targetW, targetH);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        exportar.iniciarExportacion();
                    }
                });
            }
        });

    }


}
