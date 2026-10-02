package ar.axt.database;

import android.content.Context;
import android.util.Log;
import ar.axt.animar.Bones;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class GuardarBones {
    private final Context context;

    public GuardarBones(Context context) {
        this.context = context;
    }

    public static String sanitizarNombreBone(String boneId) {
        return boneId == null ? "bone_null" : boneId.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public void guardarBoneEnCarpeta(File folder, Bones.Bone bone) {
        if (bone == null || bone.id == null) return;
        if (!folder.exists()) folder.mkdirs();
        String fileName = sanitizarNombreBone(bone.id) + ".bin";
        File finalFile = new File(folder, fileName);
        File tempFile = new File(folder, fileName + ".tmp");
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(tempFile))) {
            out.writeUTF("AXTBONE");
            out.writeInt(1); // version
            out.writeUTF(bone.id);
            out.writeUTF(bone.group != null ? bone.group : "");
            out.writeUTF((bone.parent == null || bone.parent.id == null) ? "" : bone.parent.id);
            out.writeBoolean(bone.captured);
            out.writeFloat(bone.color[0]);
            out.writeFloat(bone.color[1]);
            out.writeFloat(bone.color[2]);
            out.writeFloat(bone.color[3]);
            out.writeFloat(bone.position[0]);
            out.writeFloat(bone.position[1]);
            out.writeFloat(bone.position[2]);
            out.writeFloat(bone.rotation[0]);
            out.writeFloat(bone.rotation[1]);
            out.writeFloat(bone.rotation[2]);
            out.writeFloat(bone.scale[0]);
            out.writeFloat(bone.scale[1]);
            out.writeFloat(bone.scale[2]);
            if (bone.capturedVertices != null) {
                out.writeInt(bone.capturedVertices.size());
                for (Bones.CapturedVertex cv : bone.capturedVertices) {
                    out.writeUTF((cv.mesh == null || cv.mesh.name == null) ? "" : cv.mesh.name);
                    out.writeInt(cv.index);
                    out.writeFloat(cv.weight);
                }
            } else { out.writeInt(0); } out.flush();
        } catch (IOException e) {
            Log.e("GuardarBones", "Error guardando bone: " + bone.id, e); return; }
        if (tempFile.exists()) {
            if (finalFile.exists()) finalFile.delete();
            tempFile.renameTo(finalFile);
        }
    }

}
