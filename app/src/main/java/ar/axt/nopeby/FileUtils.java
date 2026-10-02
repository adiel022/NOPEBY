package ar.axt.nopeby;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import java.util.ArrayList;

public class FileUtils {
    public static ArrayList<String> archivosImportados = new ArrayList<>();

    public static String getFileName(Context context, Uri uri) {
        int nameIndex;
        String result = null;
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst() && (nameIndex = cursor.getColumnIndex("_display_name")) >= 0) {
                    result = cursor.getString(nameIndex);
                }
            } finally { if (cursor != null) { cursor.close(); }
            }
        }
        if (result == null) { result = "temp_file.obj"; }
        String lower = result.toLowerCase();
        if ((lower.endsWith(".obj") || lower.endsWith(".stl") || lower.endsWith(".glb")) && !archivosImportados.contains(result)) {
            archivosImportados.add(result);
        } return result;
    }

    public static ArrayList<String> getArchivosImportados() {
        return archivosImportados;
    }

    public static void limpiarArchivosImportados() {
        archivosImportados.clear();
    }
}
