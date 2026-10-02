package ar.axt.database;

import android.content.Context;
import ar.axt.nopeby.MyRenderer;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class GuardarMeshes {
    private final Context context;

    public GuardarMeshes(Context context) {
        this.context = context;
    }

    public void guardarMeshEnCarpeta(File folder, MyRenderer.Mesh mesh) {
        if (!folder.exists()) folder.mkdirs();
        File finalFile = new File(folder, mesh.name + ".bin");
        File tempFile = new File(folder, mesh.name + ".bin.tmp");
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(tempFile))) {
            out.writeUTF("AXTMESH");
            out.writeInt(2); // Version 2
            out.writeUTF(mesh.name);
            writeFloatBuffer(out, mesh.subMesh.vertexBuffer);
            writeFloatBuffer(out, mesh.subMesh.normalBuffer);
            writeFloatBuffer(out, mesh.subMesh.texcoordBuffer);
            writeFloatBuffer(out, mesh.subMesh.colorBuffer);
            writeShortBuffer(out, mesh.subMesh.indexBuffer);
            out.writeInt(mesh.subMesh.numIndices);
            out.writeFloat(mesh.translation[0]);
            out.writeFloat(mesh.translation[1]);
            out.writeFloat(mesh.translation[2]);
            out.writeFloat(mesh.rotation[0]);
            out.writeFloat(mesh.rotation[1]);
            out.writeFloat(mesh.rotation[2]);
            out.writeFloat(mesh.scale[0]);
            out.writeFloat(mesh.scale[1]);
            out.writeFloat(mesh.scale[2]);
            out.writeFloat(mesh.specularStrength);
            out.writeFloat(mesh.shininess);
            out.writeFloat(mesh.opacity);
            out.writeFloat(mesh.lightRadiusMult);
            out.writeFloat(mesh.emissiveColor != null ? mesh.emissiveColor[0] : 1.0f);
            out.writeFloat(mesh.emissiveColor != null ? mesh.emissiveColor[1] : 0.95f);
            out.writeFloat(mesh.emissiveColor != null ? mesh.emissiveColor[2] : 0.7f);
            out.writeBoolean(mesh.subMesh != null && mesh.subMesh.hasOriginalUVs);
            if (mesh.subMesh.embeddedTexture != null) {
                out.writeBoolean(true);
                out.writeUTF(mesh.subMesh.embeddedTextureMimeType == null ? "" : mesh.subMesh.embeddedTextureMimeType);
                out.writeInt(mesh.subMesh.embeddedTexture.length);
                out.write(mesh.subMesh.embeddedTexture);
            } else { out.writeBoolean(false); }
            if (mesh.subMesh.embeddedNormalTexture != null) {
                out.writeBoolean(true);
                out.writeUTF(mesh.subMesh.embeddedNormalMimeType == null ? "" : mesh.subMesh.embeddedNormalMimeType);
                out.writeInt(mesh.subMesh.embeddedNormalTexture.length);
                out.write(mesh.subMesh.embeddedNormalTexture);
            } else { out.writeBoolean(false); } out.flush();
        } catch (IOException e) { e.printStackTrace(); return; }
        if (tempFile.exists()) { if (finalFile.exists()) finalFile.delete(); tempFile.renameTo(finalFile); }
    }

    private void writeFloatBuffer(DataOutputStream out, FloatBuffer buffer) throws IOException {
        if (buffer == null) { out.writeInt(0); return; }
        FloatBuffer copy = buffer.duplicate();
        copy.position(0);
        int size = copy.remaining();
        out.writeInt(size);
        for (int i = 0; i < size; i++) {
            out.writeFloat(copy.get());
        }
    }

    private void writeShortBuffer(DataOutputStream out, ShortBuffer buffer) throws IOException {
        if (buffer == null) { out.writeInt(0); return; }
        ShortBuffer copy = buffer.duplicate();
        copy.position(0);
        int size = copy.remaining();
        out.writeInt(size);
        for (int i = 0; i < size; i++) {
            out.writeShort(copy.get());
        }
    }
}
