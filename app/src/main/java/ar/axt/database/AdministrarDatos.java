package ar.axt.database;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import ar.axt.animar.Bones;
import ar.axt.controles.AdministradorCamaras;
import ar.axt.materiales.Material;
import ar.axt.nopeby.MainActivity;
import ar.axt.nopeby.MyRenderer;
import ar.axt.sombras.RealShadow;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AdministrarDatos {
    private static final String TAG = "AdminDatos";
    public static final String NOMBRE_CARPETA_APP = "NOPEBY_PROYECTOS";
    private final Context context;

    public AdministrarDatos(Context context) {
        this.context = context;
    }

    public static File getAppBaseDir() {
        File docsFolder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            docsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        } else {
            docsFolder = new File(Environment.getExternalStorageDirectory(), "Documents");
        }
        File base = new File(docsFolder, NOMBRE_CARPETA_APP);
        if (!base.exists()) {
            base.mkdirs();
        }
        if (!base.exists()) {
            base = new File(Environment.getExternalStorageDirectory(), NOMBRE_CARPETA_APP);
            if (!base.exists()) base.mkdirs();
        }
        return base;
    }

    public static File getProyectoDir(String nombreProyecto) {
        File pDir = new File(getAppBaseDir(), nombreProyecto);
        if (!pDir.exists()) pDir.mkdirs();
        return pDir;
    }

    public static File getAnimacionesDir(String nombreProyecto) {
        File dir = new File(getProyectoDir(nombreProyecto), "animaciones");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static File getUndoRedoDir(String nombreProyecto) {
        File dir = new File(getProyectoDir(nombreProyecto), "undorodo");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static File getObjetos3DDir(String nombreProyecto) {
        File dir = new File(getProyectoDir(nombreProyecto), "objetos3D");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static File getCajasDir(String nombreProyecto) {
        File dir = new File(getObjetos3DDir(nombreProyecto), "cajas");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static File getCamarasDir(String nombreProyecto) {
        File dir = new File(getObjetos3DDir(nombreProyecto), "camaras");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public File getProyectoPreviewFile(String nombreProyecto) {
        return new File(getProyectoDir(nombreProyecto), nombreProyecto + ".png");
    }

    public static File getMeshFolder(String nombreProyecto, String meshName) {
        String baseName = extraerNombreBase(meshName);
        File dir = new File(getObjetos3DDir(nombreProyecto), baseName);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static String extraerNombreBase(String nombre) {
        if (nombre == null) return "objeto";
        return nombre.replaceAll("_part\\d+$", "");
    }

    public void guardarProyectoBin(String nombreProyecto, List<MyRenderer.Mesh> meshes, List<Bones.Bone> bones, List<Material.EffectBox> boxes, RealShadow realShadow, MyRenderer renderer) {
        GuardarMeshes gm = new GuardarMeshes(this.context);
        GuardarBones gb = new GuardarBones(this.context);
        GuardarCajas gc = new GuardarCajas(this.context);
        GuardarCamaras gcam = new GuardarCamaras(this.context);
        if (realShadow != null) {
            DatosProyectos dp = new DatosProyectos(this.context);
            try {
                dp.actualizarShaderSettings(nombreProyecto, realShadow.lightIntensity, realShadow.shadowStrength, 
                        realShadow.lightColor[0], realShadow.lightColor[1], realShadow.lightColor[2], 
                        realShadow.shadowColor[0], realShadow.shadowColor[1], realShadow.shadowColor[2]);
            } finally { dp.close(); }
        }
        if (renderer != null) {
            DatosProyectos dp = new DatosProyectos(this.context);
            try {
                dp.actualizarWorldSettings(nombreProyecto, renderer.skyColor[0], renderer.skyColor[1], renderer.skyColor[2],
                        renderer.cloudDensity, renderer.starDensity);
            } finally { dp.close(); }
        }
        if (meshes != null) {
            synchronized (meshes) {
                for (MyRenderer.Mesh mesh : meshes) {
                    File meshFolder = getMeshFolder(nombreProyecto, mesh.name);
                    gm.guardarMeshEnCarpeta(meshFolder, mesh);
                    if (bones != null) {
                        for (Bones.Bone bone : bones) {
                            if (bone.groupedVertices != null && bone.groupedVertices.containsKey(mesh)) {
                                gb.guardarBoneEnCarpeta(meshFolder, bone);
                            }
                        }
                    }
                }
            }
        }
        if (bones != null) {
            for (Bones.Bone bone : bones) {
                if (!bone.captured || bone.capturedVertices == null || bone.capturedVertices.isEmpty()) {
                    File looseBonesDir = new File(getObjetos3DDir(nombreProyecto), "huesos_sueltos");
                    if (!looseBonesDir.exists()) looseBonesDir.mkdirs();
                    gb.guardarBoneEnCarpeta(looseBonesDir, bone);
                }
            }
        }
        if (boxes != null) {
            File cajasDir = getCajasDir(nombreProyecto);
            for (Material.EffectBox box : boxes) {
                gc.guardarCajaEnCarpeta(cajasDir, box);
            }
        }
        if (renderer != null && renderer.administradorCamaras != null) {
            File camarasDir = getCamarasDir(nombreProyecto);
            for (AdministradorCamaras.MarcadorCamara cam : renderer.administradorCamaras.getMarcadores()) {
                gcam.guardarCamara(camarasDir, cam);
            }
        }
        actualizarProyectoTxt(nombreProyecto, meshes, bones, boxes, renderer);
        if (renderer != null) {
            File screenshotFile = new File(getProyectoDir(nombreProyecto), nombreProyecto + ".png");
            renderer.requestScreenshot(screenshotFile);
        }
    }

    private void actualizarProyectoTxt(String nombreProyecto, List<MyRenderer.Mesh> meshes, List<Bones.Bone> bones, List<Material.EffectBox> boxes, MyRenderer renderer) {
        File file = new File(getProyectoDir(nombreProyecto), nombreProyecto + ".txt");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            StringBuilder sb = new StringBuilder();
            sb.append("[MESHES]\n");
            if (meshes != null) {
                for (MyRenderer.Mesh m : meshes) sb.append(m.name).append("\n");
            }
            sb.append("[BONES]\n");
            if (bones != null) {
                for (Bones.Bone b : bones) sb.append(b.id).append("\n");
            }
            sb.append("[BOXES]\n");
            if (boxes != null) {
                for (Material.EffectBox box : boxes) sb.append(box.id).append("\n");
            }
            if (renderer != null && renderer.administradorCamaras != null) {
                sb.append("[CAMERAS]\n");
                for (AdministradorCamaras.MarcadorCamara cam : renderer.administradorCamaras.getMarcadores()) {
                    sb.append(cam.id).append("\n");
                }
            }
            fos.write(sb.toString().getBytes());
            fos.flush();
        } catch (IOException e) {
            Log.e(TAG, "Error actualizando proyecto.txt", e);
        }
    }

    public void eliminarProyectoCompleto(String nombre) {
        DatosProyectos dp = new DatosProyectos(this.context);
        try { dp.eliminarProyecto(nombre);
        } finally { dp.close(); }
        File dir = new File(getAppBaseDir(), nombre);
        if (dir.exists()) { borrarRecursivo(dir); }
    }

    private void borrarRecursivo(File fileOrDirectory) {
        if (fileOrDirectory != null && fileOrDirectory.exists()) {
            if (fileOrDirectory.isDirectory()) {
                File[] children = fileOrDirectory.listFiles();
                if (children != null) {
                    for (File child : children) {
                        borrarRecursivo(child);
                    }
                }
            } fileOrDirectory.delete();
        }
    }

    public boolean existeProyecto(String nombre) {
        return new File(getProyectoDir(nombre), nombre + ".txt").exists();
    }
}
