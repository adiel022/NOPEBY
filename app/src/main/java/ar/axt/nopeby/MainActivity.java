package ar.axt.nopeby;

import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import ar.axt.animar.Bones;
import ar.axt.animar.HistorialMovimientos;
import ar.axt.animar.InteraccionGismo;
import ar.axt.animar.UndoRodoGuardado;
import ar.axt.controles.RayosInteraccion;
import ar.axt.controles.Tacto;
import ar.axt.database.AdministrarDatos;
import ar.axt.database.ReconstruirEscena;
import ar.axt.leerobj.ObjetosCargados;
import ar.axt.materiales.Material;
import ar.axt.nopeby.MyRenderer;
import ar.axt.servicios.CleanupService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_CODE = 100;
    public static ObjetosCargados currentObjLoader;
    public int CualShader;
    Button bhb;
    private GestureDetector gestureDetector;
    public GLSurfaceView glSurfaceView;
    public long idProyecto;
    private float lastSpan;
    public String nombreProyecto;
    private float previousTwoFingerX;
    private float previousTwoFingerY;
    private float previousX;
    private float previousY;
    public MyRenderer renderer;
    private ScaleGestureDetector scaleDetector;
    ImageButton ssb;
    private Tacto tacto;
    public TextView tv1;
    public TextView tv2;
    private UndoRodoGuardado undoRodoGuardado;
    public static String selectedName = null;
    public static Map<String, ObjetosCargados.SubMesh> meshMap = new HashMap();
    private long lastPinchTime = 0;
    private boolean isPinching = false;
    private float previousTwoFingerAngle = 0f;
    public int huesos = 0;
    public int cantidadDeHuesos = 0;
    public ArrayList<String> listaArchivos = new ArrayList<>();
    public ArrayList<String> listaMeshes = new ArrayList<>();
    public ArrayList<String> listaUltimoMesh = new ArrayList<>();
    private int verHitbox = 0;
    public boolean isPlayingAnimation = false;
    public String meshSeleccionadoNombre = null;
    public boolean noGuardarAlSalir = false;

    public int modoMultiseleccion = 0;
    public java.util.Set<String> meshesMultiseleccionados = new java.util.HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FileUtils.limpiarArchivosImportados();
        meshMap.clear();
        selectedName = null;
        setContentView(R.layout.main);
        startService(new Intent(this, (Class<?>) CleanupService.class));
        this.CualShader = getIntent().getIntExtra("SL", 0);
        checkStoragePermission();
        this.glSurfaceView = (GLSurfaceView) findViewById(R.id.glSurfaceView);
        this.glSurfaceView.setEGLContextClientVersion(2);
        this.renderer = new MyRenderer(this);
        this.glSurfaceView.setRenderer(this.renderer);
        this.glSurfaceView.setRenderMode(0);
        this.idProyecto = getIntent().getLongExtra("ID_PROYECTO", -1L);
        this.nombreProyecto = getIntent().getStringExtra("NOMBRE_PROYECTO");
        this.tacto = new Tacto(this);
        this.renderer.historialMovimientos = new HistorialMovimientos(this, this.renderer);
        this.renderer.interaccionGismo = new InteraccionGismo(this.renderer, this, this.renderer.historialMovimientos, this.renderer.rayosInteraccion, this.renderer.gizmo);
        this.renderer.rayosInteraccion = new RayosInteraccion(this, this.renderer, this.renderer.interaccionGismo, this.renderer.historialMovimientos);
        this.tv1 = (TextView) findViewById(R.id.t1);
        this.tv2 = (TextView) findViewById(R.id.t2);
        this.ssb = (ImageButton) findViewById(R.id.botonbone);
        this.undoRodoGuardado = new UndoRodoGuardado(this);
        if (this.nombreProyecto != null) {
            ReconstruirEscena loader = new ReconstruirEscena(this);
            ReconstruirEscena.EscenaCompleta escena = loader.cargarProyecto(this.nombreProyecto);
            ArrayList<MyRenderer.Mesh> meshesLoaded = escena.meshes;
            if (escena.shaderSettings != null && this.renderer != null) {
                this.renderer.pendingSettings = escena.shaderSettings;
            }
            if (this.renderer.getBones() != null && escena.bones != null) {
                this.renderer.getBones().setBones(escena.bones);
                this.cantidadDeHuesos = escena.bones.size();
            }
            if (escena.boxes != null) {
                this.renderer.loadedBoxes.addAll(escena.boxes);
                this.renderer.cantidadDeEfectos = escena.boxes.size();
            }
            if (escena.cameras != null && this.renderer.administradorCamaras != null) {
                this.renderer.administradorCamaras.getMarcadores().addAll(escena.cameras);
            }
            if (meshesLoaded != null && !meshesLoaded.isEmpty()) {
                Log.d("CARGA_ESCENA", "Cantidad meshes: " + meshesLoaded.size());
                for (MyRenderer.Mesh mesh : meshesLoaded) {
                    if (mesh != null) {
                        this.renderer.addMesh(mesh);
                        if (mesh.name != null) {
                            this.listaMeshes.add(mesh.name);
                        }
                    }
                }
                if (!this.listaArchivos.contains(this.nombreProyecto)) {
                    this.listaArchivos.add(this.nombreProyecto);
                }
                String nombreUltimo = meshesLoaded.get(meshesLoaded.size() - 1).name;
                this.listaUltimoMesh.add(nombreUltimo);
            }
            this.glSurfaceView.requestRender();
            this.glSurfaceView.post(new Runnable() {
                @Override // java.lang.Runnable
                public void run() {
                    if (MainActivity.this.undoRodoGuardado != null && MainActivity.this.renderer != null) {
                        MainActivity.this.undoRodoGuardado.reconstruir(MainActivity.this, MainActivity.this.renderer, MainActivity.this.nombreProyecto);
                    }
                }
            });
        }
        this.gestureDetector = new GestureDetector(this.glSurfaceView.getContext(), new GestureDetector.SimpleOnGestureListener() { // from class: ar.axt.nopeby.MainActivity.2
            @Override
            public boolean onSingleTapConfirmed(MotionEvent event) {
                if (event.getPointerCount() != 1) { return true; }
                final float xPix = event.getX();
                final float yPix = event.getY();
                MainActivity.this.glSurfaceView.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        Tacto.Ray ray = MainActivity.this.tacto.crearRay(xPix, yPix, MainActivity.this.renderer);
                        if (ray != null) {
                            MainActivity.this.renderer.rayosInteraccion.seleccionarPorRay(ray);
                            MainActivity.this.glSurfaceView.requestRender();
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    MainActivity.this.actualizarFragmentoAnadir();
                                }
                            });
                        }
                    }
                });
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent event) {
                MainActivity.this.glSurfaceView.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.renderer.rayosInteraccion.deseleccionarTodo();
                        MainActivity.this.glSurfaceView.requestRender();
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                MainActivity.this.actualizarFragmentoAnadir();
                            }
                        });
                    }
                });
                return true;
            }
        });
        this.scaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() { // from class: ar.axt.nopeby.MainActivity.3
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                MainActivity.this.lastSpan = detector.getCurrentSpan();
                MainActivity.this.isPinching = true;
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                MainActivity.this.renderer.ajustesDeCamara.zoomCamera(detector.getScaleFactor());
                MainActivity.this.lastSpan = detector.getCurrentSpan();
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                MainActivity.this.isPinching = false;
            }
        });
        this.glSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override // android.view.View.OnTouchListener
            public boolean onTouch(View v, MotionEvent event) {
                if (MainActivity.this.isAjustesDeVideoOpen()) {
                    return true;
                }
                MainActivity.this.gestureDetector.onTouchEvent(event);
                MainActivity.this.scaleDetector.onTouchEvent(event);
                int pointerCount = event.getPointerCount();
                float x = event.getX();
                float y = event.getY();
                switch (event.getActionMasked()) {
                    case 0:
                        MainActivity.this.previousX = x;
                        MainActivity.this.previousY = y;
                        if (MainActivity.this.isGizmoVisible() && pointerCount == 1) {
                            final float touchX = x;
                            final float touchY = y;
                            MainActivity.this.glSurfaceView.queueEvent(new Runnable() {
                                @Override
                                public void run() {
                                    Tacto.Ray ray = MainActivity.this.tacto.crearRay(touchX, touchY, MainActivity.this.renderer);
                                    ObjetosCargados.SubMesh hit = MainActivity.this.renderer.rayosInteraccion.checkGizmoHit(ray);
                                    if (hit != null) {
                                        MainActivity.this.renderer.rayosInteraccion.aplicarAccionGizmo(hit);
                                        MainActivity.this.renderer.interaccionGismo.movimientoEnCurso = true;
                                        MainActivity.this.renderer.interaccionGismo.iniciarMovimientoAgrupadoConRay(ray, MainActivity.this.tacto);
                                        MainActivity.this.renderer.interaccionGismo.setLastTouch(touchX, touchY);
                                    } else {
                                        MainActivity.this.renderer.interaccionGismo.movimientoEnCurso = false;
                                        MainActivity.this.renderer.interaccionGismo.xyz = 0;
                                    }
                                }
                            });
                        }
                        return true;
                    case 1:
                    case 3:
                        if (MainActivity.this.renderer.interaccionGismo.movimientoEnCurso) {
                            MainActivity.this.glSurfaceView.queueEvent(new Runnable() {
                                @Override
                                public void run() {
                                    MainActivity.this.renderer.historialMovimientos.finalizarMovimientoAgrupado();
                                    MainActivity.this.renderer.interaccionGismo.movimientoEnCurso = false;
                                    MainActivity.this.renderer.interaccionGismo.xyz = 0;
                                    MainActivity.this.renderer.interaccionGismo.arrastreIniciado = false;
                                }
                            });
                        }
                        return true;
                    case 2:
                        if (MainActivity.this.renderer.interaccionGismo.movimientoEnCurso && pointerCount == 1) {
                            final float touchX = x;
                            final float touchY = y;
                            final MotionEvent eventCopy = MotionEvent.obtain(event);
                            MainActivity.this.glSurfaceView.queueEvent(new Runnable() {
                                @Override
                                public void run() {
                                    Tacto.Ray ray = MainActivity.this.tacto.crearRay(touchX, touchY, MainActivity.this.renderer);
                                    MainActivity.this.renderer.interaccionGismo.moverGizmo(ray, MainActivity.this.tacto, eventCopy);
                                    eventCopy.recycle();
                                    MainActivity.this.glSurfaceView.requestRender();
                                }
                            });
                        } else if (pointerCount == 1) {
                            float dx = x - MainActivity.this.previousX;
                            float dy = y - MainActivity.this.previousY;
                            MainActivity.this.renderer.ajustesDeCamara.rotateCamera(dx, dy);
                            MainActivity.this.glSurfaceView.requestRender();
                        } else if (pointerCount == 2) {
                            float dx1 = event.getX(0) - MainActivity.this.previousX;
                            float dy1 = event.getY(0) - MainActivity.this.previousY;
                            float dx2 = event.getX(1) - MainActivity.this.previousTwoFingerX;
                            float dy2 = event.getY(1) - MainActivity.this.previousTwoFingerY;
                            if ((dx1 * dx2) + (dy1 * dy2) > 0.0f) {
                                MainActivity.this.renderer.ajustesDeCamara.panCamera((dx1 + dx2) * 0.5f, (-(dy1 + dy2)) * 0.5f);
                                MainActivity.this.glSurfaceView.requestRender();
                            }
                            float currentAngle = MainActivity.this.calculateTwoFingerAngle(event);
                            float deltaAngle = currentAngle - MainActivity.this.previousTwoFingerAngle;
                            if (deltaAngle > 180f) deltaAngle -= 360f;
                            if (deltaAngle < -180f) deltaAngle += 360f;
                            if (Math.abs(deltaAngle) > 0.05f) {
                                MainActivity.this.renderer.ajustesDeCamara.rollCamera(deltaAngle);
                                MainActivity.this.previousTwoFingerAngle = currentAngle;
                            }
                        }
                        MainActivity.this.previousX = x;
                        MainActivity.this.previousY = y;
                        if (pointerCount > 1) {
                            MainActivity.this.previousTwoFingerX = event.getX(1);
                            MainActivity.this.previousTwoFingerY = event.getY(1);
                        }
                        return true;
                    case 4:
                    default:
                        return true;
                    case 5:
                        if (pointerCount == 2) {
                            MainActivity.this.previousX = event.getX(0);
                            MainActivity.this.previousY = event.getY(0);
                            MainActivity.this.previousTwoFingerX = event.getX(1);
                            MainActivity.this.previousTwoFingerY = event.getY(1);
                            MainActivity.this.previousTwoFingerAngle = MainActivity.this.calculateTwoFingerAngle(event);
                        }
                        return true;
                    case 6:
                        int actionIndex = event.getActionIndex();
                        event.getPointerId(actionIndex);
                        int newIndex = actionIndex == 0 ? 1 : 0;
                        MainActivity.this.previousX = event.getX(newIndex);
                        MainActivity.this.previousY = event.getY(newIndex);
                        MainActivity.this.lastPinchTime = System.currentTimeMillis();
                        if (event.getPointerCount() - 1 == 2) {
                            MainActivity.this.previousTwoFingerAngle = MainActivity.this.calculateTwoFingerAngle(event);
                        }
                        return true;
                }
            }
        });
        setupOnBackPressed();
    }

    public float[] getGizmoWorldPosition() {
        return this.renderer.interaccionGismo.gizmoPosition;
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.glSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.glSurfaceView.onPause();
        if (this.undoRodoGuardado != null && this.renderer != null && this.renderer.historialMovimientos != null) {
            String t1 = this.tv1 != null ? this.tv1.getText().toString() : "0";
            String t2 = this.tv2 != null ? this.tv2.getText().toString() : "0";
            this.undoRodoGuardado.guardar(this.renderer.historialMovimientos, this.nombreProyecto, t1 + "_" + t2);
        }
        String t12 = this.nombreProyecto;
        if (t12 != null && this.renderer != null && !this.noGuardarAlSalir) {
            AdministrarDatos admin = new AdministrarDatos(this);
            List<Bones.Bone> bones = this.renderer.getBones() != null ? this.renderer.getBones().getAllBones() : new ArrayList<>();
            List<Material.EffectBox> boxes = this.renderer.material != null ? this.renderer.material.getBoxes() : new ArrayList<>();
            admin.guardarProyectoBin(this.nombreProyecto, this.renderer.meshes, bones, boxes, this.renderer.getShaderRealista(), this.renderer);
        }
    }

    @Override
    protected void onDestroy() {
        FileUtils.limpiarArchivosImportados();
        meshMap.clear();
        selectedName = null;
        if (isFinishing()) {
            if (this.undoRodoGuardado != null && this.nombreProyecto != null) {
                this.undoRodoGuardado.borrarHistorialGuardado(this.nombreProyecto);
            }
            if (this.renderer != null && this.renderer.historialMovimientos != null) {
                this.renderer.historialMovimientos.historial.clear();
                this.renderer.historialMovimientos.historialDos.clear();
            }
        }
        super.onDestroy();
    }

    private void setupOnBackPressed() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getFragmentManager().getBackStackEntryCount() > 0) {
                    getFragmentManager().popBackStack();
                    return;
                }
                android.app.Fragment f1 = getFragmentManager().findFragmentById(R.id.f1);
                android.app.Fragment f3 = getFragmentManager().findFragmentById(R.id.f3);
                if (f1 != null) {
                    getFragmentManager().beginTransaction().remove(f1).commit();
                    return;
                }
                if (f3 != null) {
                    getFragmentManager().beginTransaction().remove(f3).commit();
                    return;
                }
                if (MainActivity.this.huesos == 1 || getFragmentManager().findFragmentById(R.id.f4) instanceof LineaTiempo) {
                    return;
                }
                finish();
            }
        });
    }

    public void a(View v) {
        Mas fragment = new Mas();
        getFragmentManager().beginTransaction().replace(R.id.f1, fragment).addToBackStack(null).commit();
    }

    public void b(View v) {
        ObjLista fragment = new ObjLista();
        ArrayList<String> archivos = new ArrayList<>(this.listaArchivos);
        ArrayList<String> meshes = new ArrayList<>(this.listaMeshes);
        ArrayList<String> ultimos = new ArrayList<>(this.listaUltimoMesh);
        ArrayList<String> meshesFiltrados = new ArrayList<>();
        Set<String> nombresPrincipalesPart = new HashSet<>();
        for (String nombre : meshes) {
            if (nombre.matches("(.+)_part\\d+$")) {
                String nombrePrincipal = nombre.replaceAll("_part\\d+$", "");
                if (!nombresPrincipalesPart.contains(nombrePrincipal)) {
                    nombresPrincipalesPart.add(nombrePrincipal);
                    meshesFiltrados.add(nombrePrincipal);
                }
            } else {
                meshesFiltrados.add(nombre);
            }
        }
        Bundle args = new Bundle();
        args.putStringArrayList("listaObjs", archivos);
        args.putStringArrayList("listaMeshes", meshesFiltrados);
        args.putStringArrayList("listaUltimoMesh", ultimos);
        fragment.setArguments(args);
        getFragmentManager().beginTransaction().replace(R.id.f1, fragment).addToBackStack(null).commit();
    }

    public void c(View v) {
        EfectosDeMundo fragment = new EfectosDeMundo();
        getFragmentManager().beginTransaction().replace(R.id.f1, fragment).addToBackStack(null).commit();
    }

    public void d(View v) {
        ListaMaterialParticulas fragment = new ListaMaterialParticulas();
        getFragmentManager().beginTransaction().replace(R.id.f1, fragment).addToBackStack(null).commit();
    }

    public void e(View v) {
        EditarShaders fragment = new EditarShaders();
        getFragmentManager().beginTransaction().replace(R.id.f1, fragment).addToBackStack(null).commit();
    }

    public void f() {
        ValidarObj fragment = new ValidarObj();
        getFragmentManager().beginTransaction().replace(R.id.f3, fragment).addToBackStack(null).commit();
    }

    public void g(View v){
        LineaTiempo fragment = new LineaTiempo();
        getFragmentManager().beginTransaction().replace(R.id.f4, fragment).addToBackStack(null).commit();
    }

    public void h(View v) {
        AjustesDeVideo fragment = new AjustesDeVideo();
        getFragmentManager().beginTransaction().replace(R.id.f1, fragment).addToBackStack(null).commit();
    }

    public void i(View v) {
        EditarObjetos fragment = new EditarObjetos();
        Bundle args = new Bundle();
        if (this.renderer != null && this.renderer.rayosInteraccion != null && this.renderer.rayosInteraccion.selectedMesh != null) {
            MyRenderer.Mesh meshSel = this.renderer.rayosInteraccion.selectedMesh;
            List<MyRenderer.Mesh> relacionados;
            if (this.modoMultiseleccion == 1 && !this.meshesMultiseleccionados.isEmpty()) {
                relacionados = new ArrayList<>();
                for (String name : this.meshesMultiseleccionados) {
                    MyRenderer.Mesh m = this.renderer.rayosInteraccion.obtenerMeshPorNombre(name);
                    if (m != null && !relacionados.contains(m)) relacionados.add(m);
                }
            } else { relacionados = this.renderer.rayosInteraccion.getMeshesRelacionados(meshSel); }
            ArrayList<String> nombresMeshes = new ArrayList<>();
            for (MyRenderer.Mesh m : relacionados) {
                if (m.name != null) nombresMeshes.add(m.name);
            }
            args.putStringArrayList("SELECTED_MESHES_NAMES", nombresMeshes);
            ArrayList<String> idsBones = new ArrayList<>();
            if (this.renderer.getBones() != null && this.renderer.getBones().getAllBones() != null) {
                String nombreBase = this.renderer.rayosInteraccion.extraerNombreBase(meshSel.name);
                for (Bones.Bone b : this.renderer.getBones().getAllBones()) {
                    boolean afectado = false;
                    if (b.groupedVertices != null) {
                        for (MyRenderer.Mesh mRel : relacionados) {
                            if (b.groupedVertices.containsKey(mRel)) {
                                afectado = true;
                                break;
                            }
                        }
                    }
                    if (!afectado && b.group != null && nombreBase != null) {
                        if (b.group.equalsIgnoreCase(nombreBase) || nombreBase.toLowerCase().contains(b.group.toLowerCase())) {
                            afectado = true;
                        }
                    }
                    if (afectado && !idsBones.contains(b.id)) {
                        idsBones.add(b.id);
                    }
                }
            }
            args.putStringArrayList("SELECTED_BONES_IDS", idsBones);
        }
        fragment.setArguments(args);
        getFragmentManager().beginTransaction().replace(R.id.f1, fragment).addToBackStack(null).commit();
    }

    public void renderBone(View v) {
        List<Bones.Bone> todos;
        int total;
        if (this.huesos == 0) {
            this.huesos = 1;
            f();
            this.renderer.historialMovimientos.vaciarHistorialSecundario();
            this.ssb.setImageResource(R.drawable.boneactivado);
            this.renderer.getBones().finishGroup();
            this.cantidadDeHuesos = 0;
            return;
        }
        if (this.huesos == 1) {
            this.renderer.interaccionGismo.captureCurrentGroup();
            if (this.cantidadDeHuesos > 0 && this.renderer.historialMovimientos != null && (total = (todos = this.renderer.getBones().getAllBones()).size()) >= this.cantidadDeHuesos) {
                List<Bones.Bone> grupoFinal = new ArrayList<>(todos.subList(total - this.cantidadDeHuesos, total));
                int modoPrevio = this.huesos;
                this.huesos = 0;
                this.renderer.historialMovimientos.registrarCreacionBones(grupoFinal);
                this.huesos = modoPrevio;
            }
            this.renderer.getBones().finishGroup();
            this.huesos = 0;
            this.renderer.historialMovimientos.vaciarHistorialSecundario();
            this.ssb.setImageResource(R.drawable.boneclick);
            ValidarObj fragment = (ValidarObj) getFragmentManager().findFragmentById(R.id.f3);
            if (fragment != null) {
                fragment.cerrarFragmento();
            }
        }
    }

    public int renderizarHuesos() {
        return this.huesos;
    }

    public void boneAprobado() {
        if (this.huesos == 1) {
            Bones.Bone nuevoBone = this.renderer.getBones().createBone(this.renderer.gizmo);
            this.cantidadDeHuesos++;
            if (this.renderer.historialMovimientos != null) {
                this.renderer.historialMovimientos.registrarCreacionBone(nuevoBone);
                return;
            }
            return;
        }
        if (this.huesos == 0) {
            Bones.Bone nuevoBone2 = this.renderer.getBones().createBone(this.renderer.gizmo);
            if (this.renderer.historialMovimientos != null) {
                this.renderer.historialMovimientos.registrarCreacionBone(nuevoBone2);
            }
            this.glSurfaceView.requestRender();
        }
    }

    public void boneDesaprobado() {
        Bones.Bone selected;
        if (this.huesos == 1 && this.cantidadDeHuesos >= 1) {
            Bones.Bone ultimoBone = null;
            if (!this.renderer.getBones().getAllBones().isEmpty()) {
                Bones.Bone ultimoBone2 = this.renderer.getBones().getAllBones().get(this.renderer.getBones().getAllBones().size() - 1);
                ultimoBone = ultimoBone2;
            }
            this.renderer.getBones().deleteLastBone();
            this.cantidadDeHuesos--;
            if (this.renderer.historialMovimientos != null && ultimoBone != null) {
                this.renderer.historialMovimientos.registrarEliminacionBone(ultimoBone);
                return;
            }
            return;
        }
        if (this.huesos == 0 && (selected = this.renderer.getBones().selectedBone) != null) {
            if (this.renderer.historialMovimientos != null) {
                this.renderer.historialMovimientos.registrarEliminacionBone(selected);
            }
            this.renderer.rayosInteraccion.deseleccionarTodo();
            this.renderer.getBones().removeBoneSubtree(selected);
            this.glSurfaceView.requestRender();
        }
    }

    public void MeshDesaprobado() {
        MyRenderer.Mesh selected = this.renderer.rayosInteraccion.selectedMesh;
        if (selected != null) {
            if (this.renderer.historialMovimientos != null) {
                this.renderer.historialMovimientos.registrarEliminacionMesh(selected);
            }
            if (selected.name != null) {
                this.listaMeshes.remove(selected.name);
                this.listaUltimoMesh.remove(selected.name);
            }
            this.renderer.rayosInteraccion.deseleccionarTodo();
            this.renderer.removeMesh(selected);
            this.glSurfaceView.requestRender();
        }
    }

    public void CajaDesaprobada() {
        Material.EffectBox selected = this.renderer.material.selectedBox;
        if (selected != null) {
            if (this.renderer.historialMovimientos != null) {
                this.renderer.historialMovimientos.registrarEliminacionBox(selected);
            }
            this.renderer.rayosInteraccion.deseleccionarTodo();
            this.renderer.material.removeBox(selected);
            if (this.renderer.humo != null) this.renderer.humo.rebuildParticles();
            if (this.renderer.fuego != null) this.renderer.fuego.rebuildParticles();
            if (this.renderer.tierra != null) this.renderer.tierra.rebuildParticles();
            if (this.renderer.gotas != null) this.renderer.gotas.rebuildParticles();
            if (this.renderer.polvo != null) this.renderer.polvo.rebuildParticles();
            if (this.renderer.explocion != null) this.renderer.explocion.rebuildParticles();
            this.renderer.cantidadDeEfectos = this.renderer.material.getBoxes().size();
            this.glSurfaceView.requestRender();
        }
    }

    public void MeshAprobado() {}

    public void CajaAprobada() {
        this.renderer.material.addEffectBox(0);
        this.renderer.cantidadDeEfectos = this.renderer.material.getBoxes().size();
        this.glSurfaceView.requestRender();
    }

    public void eliminarObj(View v) {
        if (this.huesos == 1) {
            boneDesaprobado();
            actualizarFragmentoAnadir();
            return;
        }
        if (this.renderer.getBones().selectedBone != null) {
            boneDesaprobado();
        } else if (this.renderer.rayosInteraccion.selectedMesh != null) {
            MeshDesaprobado();
        } else if (this.renderer.material.selectedBox != null) {
            CajaDesaprobada();
        }
        actualizarFragmentoAnadir();
    }

    public void crearObj(View v) {
        if (this.huesos == 1) {
            boneAprobado();
        } else {
            boneAprobado();
        }
    }

    public int huesosarenderizar() {
        return this.cantidadDeHuesos;
    }

    public int verHitboxOpcion() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (MainActivity.this.verHitbox == 0) {
                    MainActivity.this.verHitbox = 1;
                }
            }
        }); return this.verHitbox;
    }

    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT < 33) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE") != 0 || ContextCompat.checkSelfPermission(this, "android.permission.READ_EXTERNAL_STORAGE") != 0) {
                ActivityCompat.requestPermissions(this, new String[]{"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"}, STORAGE_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == 0) {
                Toast.makeText(this, "Permiso de almacenamiento concedido", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permiso de almacenamiento denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void loadObjInRenderer(List<ObjetosCargados.SubMesh> meshes) {
        if (this.renderer == null || meshes == null) { return; }
        int contador = this.listaMeshes.size() + 1;
        ArrayList<MyRenderer.Mesh> nuevosMeshes = new ArrayList<>();
        for (ObjetosCargados.SubMesh sm : meshes) {
            if (sm != null) { MyRenderer.Mesh m = this.renderer.addMesh(sm);
                if (m != null) { nuevosMeshes.add(m); }
                if (sm.name != null && !sm.name.trim().isEmpty()) {
                    this.listaMeshes.add(sm.name);
                } else { this.listaMeshes.add("Mesh_" + contador);
                } contador++;
            }
        }
        if (!nuevosMeshes.isEmpty()) {
            if (this.renderer.historialMovimientos != null) {
                this.renderer.historialMovimientos.registrarCreacionMeshes(nuevosMeshes);
            }
            MyRenderer.Mesh ultimo = nuevosMeshes.get(nuevosMeshes.size() - 1);
            if (ultimo != null && ultimo.name != null) {
                this.listaUltimoMesh.add(ultimo.name);
            }
        }
        Log.d("OBJNAMES", "ListaMeshes=" + this.listaMeshes);
        Log.d("OBJNAMES", "ListaUltimoMesh=" + this.listaUltimoMesh);
        this.glSurfaceView.requestRender();
    }

    public void requestRender() {
        if (this.glSurfaceView != null) {
            this.glSurfaceView.requestRender();
        }
    }

    public String recibirTextoDesdeFragment(String texto) {
        selectedName = texto;
        gizmo1(texto);
        return texto;
    }

    public void gizmo1(String nombreSeleccionado) {
        MyRenderer.Mesh mesh;
        if (this.renderer == null || (mesh = this.renderer.rayosInteraccion.obtenerMeshPorNombre(nombreSeleccionado)) == null) {
            return;
        }
        this.renderer.rayosInteraccion.selectMesh(mesh);
        this.glSurfaceView.requestRender();
    }

    public boolean isGizmoVisible() {
        if (this.renderer != null) {
            return this.renderer.isGizmoVisible();
        } return false;
    }

    public void botonAtras(View v) {
        if (this.huesos == 1) { return; }
        int a = Integer.parseInt(this.tv1.getText().toString());
        int b = Integer.parseInt(this.tv2.getText().toString());
        if (a > 0 || b > 0) {
            this.renderer.historialMovimientos.retrocederMovimiento();
            this.renderer.interaccionGismo.actualizarCentroGizmo();
            this.renderer.material.actualizarCentroBox();
            int a2 = a - 1;
            if (a2 >= 0) {
                this.tv1.setText(String.valueOf(a2));
                this.tv2.setText(String.valueOf(b + 1));
            }
        }
    }

    public void botonAdelante(View v) {
        if (this.huesos == 1) { return; }
        int a = Integer.parseInt(this.tv1.getText().toString());
        int b = Integer.parseInt(this.tv2.getText().toString());
        if (a > 0 || b > 0) {
            this.renderer.historialMovimientos.avanzarMovimiento();
            this.renderer.interaccionGismo.actualizarCentroGizmo();
            this.renderer.material.actualizarCentroBox();
            int b2 = b - 1;
            if (b2 >= 0) {
                this.tv2.setText(String.valueOf(b2));
                this.tv1.setText(String.valueOf(a + 1));
            }
        }
    }

    public void incrementarMovimientosAtras() {
        if (this.huesos == 1) { return; }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (tv1 != null) {
                        int valor = Integer.parseInt(tv1.getText().toString());
                        tv1.setText(String.valueOf(valor + 1));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void resetearMovimientosAdelante() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (tv2 != null) {
                        tv2.setText("0");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void actualizarFragmentoAnadir() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (MainActivity.this.renderer != null) {
                    boolean seleccionado = (MainActivity.this.renderer.getBones() != null && MainActivity.this.renderer.getBones().selectedBone != null) ||
                                           (MainActivity.this.renderer.rayosInteraccion != null && MainActivity.this.renderer.rayosInteraccion.selectedMesh != null) ||
                                           (MainActivity.this.renderer.material != null && MainActivity.this.renderer.material.selectedBox != null);
                    if (seleccionado) {
                        if (getFragmentManager().findFragmentById(R.id.anadir_container) == null) {
                            Anadir fragment = new Anadir();
                            getFragmentManager().beginTransaction().replace(R.id.anadir_container, fragment).commit();
                        }
                    } else {
                        android.app.Fragment fragment = getFragmentManager().findFragmentById(R.id.anadir_container);
                        if (fragment != null) {
                            getFragmentManager().beginTransaction().remove(fragment).commit();
                        }
                    }
                }
            }
        });
    }

    public boolean isAjustesDeVideoOpen() {
        android.app.Fragment fragment = getFragmentManager().findFragmentById(R.id.f1);
        return fragment instanceof AjustesDeVideo;
    }

    private float calculateTwoFingerAngle(MotionEvent event) {
        if (event.getPointerCount() < 2) return 0f;
        float dx = event.getX(1) - event.getX(0);
        float dy = event.getY(1) - event.getY(0);
        return (float) Math.toDegrees(Math.atan2(dy, dx));
    }
}
