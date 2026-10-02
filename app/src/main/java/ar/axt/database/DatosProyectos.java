package ar.axt.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;

public class DatosProyectos extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "DatosProyectos.db";
    private static final int DATABASE_VERSION = 4;
    public static final String TABLA_PROYECTOS = "proyectos";

    public static class ShaderSettings {
        public float lightIntensity = 0.65f;
        public float shadowStrength = 0.75f;
        public float[] lightColor = {1.0f, 1.0f, 1.0f};
        public float[] shadowColor = {0.6f, 0.6f, 0.6f};
        public float[] skyColor = {0.18f, 0.18f, 0.18f};
        public float cloudDensity = 0.0f;
        public float starDensity = 0.0f;
    }

    public DatosProyectos(Context context) {
        super(context, DATABASE_NAME, (SQLiteDatabase.CursorFactory) null, 4);
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE proyectos (id INTEGER PRIMARY KEY AUTOINCREMENT,nombre TEXT UNIQUE NOT NULL,fecha INTEGER,shader INTEGER DEFAULT 2,light_intensity REAL DEFAULT 0.65,shadow_strength REAL DEFAULT 0.75,light_r REAL DEFAULT 1.0,light_g REAL DEFAULT 1.0,light_b REAL DEFAULT 1.0,shadow_r REAL DEFAULT 0.6,shadow_g REAL DEFAULT 0.6,shadow_b REAL DEFAULT 0.6,sky_r REAL DEFAULT 0.18,sky_g REAL DEFAULT 0.18,sky_b REAL DEFAULT 0.18,cloud_density REAL DEFAULT 0.0,star_density REAL DEFAULT 0.0);");
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE proyectos ADD COLUMN shader INTEGER DEFAULT 2");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE proyectos ADD COLUMN light_intensity REAL DEFAULT 0.65");
            db.execSQL("ALTER TABLE proyectos ADD COLUMN shadow_strength REAL DEFAULT 0.75");
            db.execSQL("ALTER TABLE proyectos ADD COLUMN light_r REAL DEFAULT 1.0");
            db.execSQL("ALTER TABLE proyectos ADD COLUMN light_g REAL DEFAULT 1.0");
            db.execSQL("ALTER TABLE proyectos ADD COLUMN light_b REAL DEFAULT 1.0");
            db.execSQL("ALTER TABLE proyectos ADD COLUMN shadow_r REAL DEFAULT 0.6");
            db.execSQL("ALTER TABLE proyectos ADD COLUMN shadow_g REAL DEFAULT 0.6");
            db.execSQL("ALTER TABLE proyectos ADD COLUMN shadow_b REAL DEFAULT 0.6");
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE proyectos ADD COLUMN sky_r REAL DEFAULT 0.18");
            db.execSQL("ALTER TABLE proyectos ADD COLUMN sky_g REAL DEFAULT 0.18");
            db.execSQL("ALTER TABLE proyectos ADD COLUMN sky_b REAL DEFAULT 0.18");
            db.execSQL("ALTER TABLE proyectos ADD COLUMN cloud_density REAL DEFAULT 0.0");
            db.execSQL("ALTER TABLE proyectos ADD COLUMN star_density REAL DEFAULT 0.0");
        }
    }

    public long crearProyecto(String nombre, int shader) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put("nombre", nombre);
        valores.put("fecha", Long.valueOf(System.currentTimeMillis()));
        valores.put("shader", Integer.valueOf(shader));
        return db.insert("proyectos", null, valores);
    }

    public boolean existeProyecto(String nombre) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT 1 FROM proyectos WHERE nombre=? LIMIT 1", new String[]{nombre});
        boolean existe = c.moveToFirst();
        c.close(); return existe;
    }

    public ArrayList<String> obtenerNombresProyectos() {
        ArrayList<String> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT nombre FROM proyectos ORDER BY fecha DESC", null);
        while (c.moveToNext()) {
            lista.add(c.getString(0));
        } c.close(); return lista;
    }

    public long obtenerIdPorNombre(String nombre) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id FROM proyectos WHERE nombre=?", new String[]{nombre});
        long id = -1;
        if (c.moveToFirst()) {
            id = c.getLong(0);
        } c.close(); return id;
    }

    public void eliminarProyecto(String nombre) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("proyectos", "nombre=?", new String[]{nombre});
    }

    public void renombrarProyecto(String nombreViejo, String nombreNuevo) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put("nombre", nombreNuevo);
        db.update("proyectos", valores, "nombre=?", new String[]{nombreViejo});
    }

    public int obtenerShaderPorNombre(String nombre) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT shader FROM proyectos WHERE nombre=?", new String[]{nombre});
        int shader = 2;
        if (c.moveToFirst()) {
            shader = c.getInt(0);
        } c.close(); return shader;
    }

    public void actualizarShaderSettings(String nombre, float intensity, float strength, float lr, float lg, float lb, float sr, float sg, float sb) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put("light_intensity", Float.valueOf(intensity));
        valores.put("shadow_strength", Float.valueOf(strength));
        valores.put("light_r", Float.valueOf(lr));
        valores.put("light_g", Float.valueOf(lg));
        valores.put("light_b", Float.valueOf(lb));
        valores.put("shadow_r", Float.valueOf(sr));
        valores.put("shadow_g", Float.valueOf(sg));
        valores.put("shadow_b", Float.valueOf(sb));
        db.update("proyectos", valores, "nombre=?", new String[]{nombre});
    }

    public void actualizarWorldSettings(String nombre, float skyR, float skyG, float skyB, float cloudDensity, float starDensity) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put("sky_r", Float.valueOf(skyR));
        valores.put("sky_g", Float.valueOf(skyG));
        valores.put("sky_b", Float.valueOf(skyB));
        valores.put("cloud_density", Float.valueOf(cloudDensity));
        valores.put("star_density", Float.valueOf(starDensity));
        db.update("proyectos", valores, "nombre=?", new String[]{nombre});
    }

    public ShaderSettings obtenerShaderSettings(String nombre) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT light_intensity, shadow_strength, light_r, light_g, light_b, shadow_r, shadow_g, shadow_b, sky_r, sky_g, sky_b, cloud_density, star_density FROM proyectos WHERE nombre=?", new String[]{nombre});
        ShaderSettings settings = new ShaderSettings();
        if (c.moveToFirst()) {
            settings.lightIntensity = c.getFloat(0);
            settings.shadowStrength = c.getFloat(1);
            settings.lightColor[0] = c.getFloat(2);
            settings.lightColor[1] = c.getFloat(3);
            settings.lightColor[2] = c.getFloat(4);
            settings.shadowColor[0] = c.getFloat(5);
            settings.shadowColor[1] = c.getFloat(6);
            settings.shadowColor[2] = c.getFloat(7);
            if (c.getColumnCount() >= 13 && !c.isNull(8)) {
                settings.skyColor[0] = c.getFloat(8);
                settings.skyColor[1] = c.getFloat(9);
                settings.skyColor[2] = c.getFloat(10);
                settings.cloudDensity = c.getFloat(11);
                settings.starDensity = c.getFloat(12);
            }
        } c.close(); return settings;
    }
}
