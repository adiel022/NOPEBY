package ar.axt.database;

import android.content.Context;
import android.util.Log;
import ar.axt.controles.AdministradorCamaras;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class GuardarCamaras {
    private final Context context;

    public GuardarCamaras(Context context) {
        this.context = context;
    }

    public void guardarCamara(File folder, AdministradorCamaras.MarcadorCamara marker) {
        if (marker == null || marker.id == null) return;
        String fileName = marker.id + ".bin";
        File finalFile = new File(folder, fileName);
        File tempFile = new File(folder, fileName + ".tmp");
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(tempFile))) {
            out.writeUTF("AXTCAM");
            out.writeInt(1); // Version
            out.writeUTF(marker.id);
            out.writeFloat(marker.position[0]);
            out.writeFloat(marker.position[1]);
            out.writeFloat(marker.position[2]);
            out.writeFloat(marker.rotation[0]);
            out.writeFloat(marker.rotation[1]);
            out.writeFloat(marker.rotation[2]);
            out.writeFloat(marker.angleRoll);
            out.writeFloat(marker.scale);
            out.flush();
        } catch (IOException e) {
            Log.e("GuardarCamaras", "Error guardando cámara: " + marker.id, e); return; }
        if (tempFile.exists()) {
            if (finalFile.exists()) finalFile.delete();
            tempFile.renameTo(finalFile);
        }
    }

}
