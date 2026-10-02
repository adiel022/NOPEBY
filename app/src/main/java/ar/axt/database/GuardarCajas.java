package ar.axt.database;

import android.content.Context;
import ar.axt.materiales.Material;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class GuardarCajas {
    private final Context context;

    public GuardarCajas(Context context) {
        this.context = context;
    }

    public static String sanitizarNombreCaja(String cajaId) {
        return cajaId == null ? "box_null" : cajaId.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public void guardarCajaEnCarpeta(File folder, Material.EffectBox box) {
        if (box == null || box.id == null) return;
        if (!folder.exists()) folder.mkdirs();
        String fileName = sanitizarNombreCaja(box.id) + ".bin";
        File finalFile = new File(folder, fileName);
        File tempFile = new File(folder, fileName + ".tmp");
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(tempFile))) {
            out.writeUTF("AXTBOX");
            out.writeInt(2); // Version (includes numParticles)
            out.writeUTF(box.id);
            out.writeInt(box.effectType);
            out.writeFloat(box.position[0]);
            out.writeFloat(box.position[1]);
            out.writeFloat(box.position[2]);
            out.writeFloat(box.rotation[0]);
            out.writeFloat(box.rotation[1]);
            out.writeFloat(box.rotation[2]);
            out.writeFloat(box.scale);
            out.writeInt(box.numParticles);
            out.flush();
        } catch (IOException e) { e.printStackTrace(); return; }
        if (tempFile.exists()) {
            if (finalFile.exists()) finalFile.delete();
            tempFile.renameTo(finalFile);
        }
    }

}
