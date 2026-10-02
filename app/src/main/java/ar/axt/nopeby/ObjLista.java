package ar.axt.nopeby;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import ar.axt.animar.Bones;
import ar.axt.nopeby.MyRenderer;
import java.util.ArrayList;

public class ObjLista extends Fragment {
    private static final String TAG = "OBJLISTA";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ArrayList<String> archivos;
        ArrayList<String> meshes;
        ArrayList<String> ultimos;
        int i;
        String meshNombre;
        String limite;
        Button btnObj;
        String objNombre;
        String limite2;
        Button btnObj2;
        String objNombre2;
        String meshNombre2;
        Button btnMesh;
        View root = inflater.inflate(R.layout.objlista, container, false);
        LinearLayout l1 = (LinearLayout) root.findViewById(R.id.l1);
        l1.removeAllViews();
        final MainActivity act = (MainActivity) getActivity();
        if (act != null) {
            Button btnMulti = new Button(getActivity());
            LinearLayout.LayoutParams mParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(35));
            mParams.setMargins(dpToPx(5), dpToPx(5), dpToPx(5), dpToPx(10));
            btnMulti.setLayoutParams(mParams);
            btnMulti.setText("MULTISELECCION");
            btnMulti.setTextSize(2, 11.0f);
            if (act.modoMultiseleccion == 1) {
                btnMulti.setBackgroundColor(Color.parseColor("#0000FF"));
                btnMulti.setTextColor(Color.WHITE);
            } else {
                btnMulti.setBackgroundColor(Color.WHITE);
                btnMulti.setTextColor(Color.BLACK);
            }
            
            btnMulti.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (act.modoMultiseleccion == 0) {
                        act.modoMultiseleccion = 1;
                        Toast.makeText(act, "Multiselección Activada", Toast.LENGTH_SHORT).show();
                    } else {
                        act.modoMultiseleccion = 0;
                        act.meshesMultiseleccionados.clear();
                        Toast.makeText(act, "Multiselección Desactivada", Toast.LENGTH_SHORT).show();
                    }
                    getFragmentManager().beginTransaction().detach(ObjLista.this).attach(ObjLista.this).commit();
                }
            }); l1.addView(btnMulti);
        }

        if (getArguments() == null) {
            archivos = null;
            meshes = null;
            ultimos = null;
        } else {
            ArrayList<String> archivos2 = getArguments().getStringArrayList("listaObjs");
            ArrayList<String> meshes2 = getArguments().getStringArrayList("listaMeshes");
            ArrayList<String> ultimos2 = getArguments().getStringArrayList("listaUltimoMesh");
            Log.d(TAG, "archivos=" + archivos2);
            Log.d(TAG, "meshes=" + meshes2);
            Log.d(TAG, "ultimos=" + ultimos2);
            archivos = archivos2;
            meshes = meshes2;
            ultimos = ultimos2;
        }
        if (archivos == null || meshes == null || ultimos == null) {
            Log.w(TAG, "⚠️ Alguna lista es null"); return root;
        }
        MyRenderer renderer = act != null ? act.renderer : null;
        int indexMesh = 0;
        int i2 = 0;
        while (i2 < archivos.size()) {
            String tempObjNombre = archivos.get(i2);
            final String objNombre3 = tempObjNombre;
            final int posArchivo = i2;
            final ArrayList<String> finalMeshes = meshes;
            final ArrayList<String> finalUltimos = ultimos;
            final int startIndexMesh = indexMesh;
            Button btnObj3 = crearBoton(objNombre3, 5);
            btnObj3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (act != null) {
                        act.modoMultiseleccion = 1;
                        act.meshesMultiseleccionados.clear();
                        if (posArchivo < finalUltimos.size()) {
                            String limiteArchivo = finalUltimos.get(posArchivo);
                            int k = startIndexMesh;
                            while (k < finalMeshes.size()) {
                                String nameM = finalMeshes.get(k);
                                act.meshesMultiseleccionados.add(nameM);
                                if (nameM.equals(limiteArchivo)) {
                                    break;
                                } k++;
                            }
                            if (startIndexMesh < finalMeshes.size()) {
                                act.recibirTextoDesdeFragment(finalMeshes.get(startIndexMesh));
                            }
                        }
                        Toast.makeText(act, "Grupo completo seleccionado (" + act.meshesMultiseleccionados.size() + " meshes)", Toast.LENGTH_SHORT).show();
                        getFragmentManager().beginTransaction().detach(ObjLista.this).attach(ObjLista.this).commit();
                    }
                }
            });
            l1.addView(btnObj3);
            if (i2 < ultimos.size()) {
                String limite3 = ultimos.get(i2);
                int indexMesh2 = indexMesh;
                while (true) {
                    int indexMesh3 = meshes.size();
                    if (indexMesh2 >= indexMesh3) { i = i2; indexMesh = indexMesh2;
                        break;
                    }
                    String meshNombre3 = meshes.get(indexMesh2);
                    MyRenderer.Mesh meshReal = obtenerMeshDesdeNombre(meshNombre3);
                    int indexMesh4 = indexMesh2;
                    Button btnMesh2 = crearBotonMesh(meshNombre3, 10);
                    l1.addView(btnMesh2);
                    if (renderer == null || renderer.getBones() == null || meshReal == null) {
                        meshNombre = meshNombre3;
                        limite = limite3;
                        btnObj = btnObj3;
                        objNombre = objNombre3;
                        i = i2;
                    } else {
                        for (Bones.Bone b : renderer.getBones().getAllBones()) {
                            int i3 = i2;
                            String meshNombre4 = meshNombre3;
                            if (b.parent != null) {
                                limite2 = limite3;
                                btnObj2 = btnObj3;
                                objNombre2 = objNombre3;
                                meshNombre2 = meshNombre4;
                                btnMesh = btnMesh2;
                            } else {
                                meshNombre2 = meshNombre4;
                                btnMesh = btnMesh2;
                                limite2 = limite3;
                                btnObj2 = btnObj3;
                                objNombre2 = objNombre3;
                                agregarBoneRecursivoFiltrado(l1, b, meshReal, 1, 15);
                            }
                            String mNombre3 = meshNombre2;
                            btnMesh2 = btnMesh;
                            i2 = i3;
                            meshNombre3 = mNombre3;
                            limite3 = limite2;
                            btnObj3 = btnObj2;
                        }
                        meshNombre = meshNombre3;
                        limite = limite3;
                        btnObj = btnObj3;
                        objNombre = objNombre3;
                        i = i2;
                    }
                    indexMesh2 = indexMesh4 + 1;
                    String limite4 = limite;
                    if (meshNombre.equals(limite4)) {
                        indexMesh = indexMesh2;
                        break;
                    }
                    limite3 = limite4;
                    i2 = i;
                    btnObj3 = btnObj;
                }
            } else { i = i2; } i2 = i + 1; } return root;
    }

    private Button crearBoton(final String texto, int marginLeftDp) {
        Button btn = new Button(getActivity());
        int ancho = dpToPx(220);
        int alto = dpToPx(30);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ancho, alto);
        params.setMargins(dpToPx(marginLeftDp), dpToPx(5), dpToPx(5), dpToPx(5));
        params.gravity = GravityCompat.START;
        btn.setLayoutParams(params);
        btn.setText(texto);
        btn.setBackgroundColor(-1);
        btn.setTextColor(ViewCompat.MEASURED_STATE_MASK);
        btn.setTextSize(2, 10.0f);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ObjLista.this.getActivity(), "Seleccionaste: " + texto, 0).show();
                if (ObjLista.this.getActivity() instanceof MainActivity) {
                    ((MainActivity) ObjLista.this.getActivity()).recibirTextoDesdeFragment(texto);
                }
            }
        }); return btn;
    }

    private Button crearBotonMesh(final String meshNombre, int marginLeftDp) {
        Button btn = crearBoton("🧩 " + meshNombre, marginLeftDp);
        MainActivity act = (MainActivity) getActivity();
        if (act != null && act.modoMultiseleccion == 1 && act.meshesMultiseleccionados.contains(meshNombre)) {
            btn.setBackgroundColor(Color.parseColor("#CCE5FF"));
        }
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity act2 = (MainActivity) ObjLista.this.getActivity();
                if (act2 != null) {
                    if (act2.modoMultiseleccion == 1) {
                        if (act2.meshesMultiseleccionados.contains(meshNombre)) {
                            act2.meshesMultiseleccionados.remove(meshNombre);
                        } else {
                            act2.meshesMultiseleccionados.add(meshNombre);
                        }
                        act2.recibirTextoDesdeFragment(meshNombre);
                        getFragmentManager().beginTransaction().detach(ObjLista.this).attach(ObjLista.this).commit();
                    } else {
                        act2.recibirTextoDesdeFragment(meshNombre);
                    }
                }
            }
        }); return btn;
    }

    private Button crearBotonBone(final Bones.Bone bone, final MyRenderer.Mesh mesh, int marginLeftDp) {
        Button btn = crearBoton(bone.id, marginLeftDp);
        btn.setBackgroundColor(Color.parseColor("#EEEEEE"));
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity act = (MainActivity) ObjLista.this.getActivity();
                if (act == null || act.renderer == null || act.renderer.rayosInteraccion == null) {
                    return;
                }
                act.renderer.getBones().selectBone(bone.id);
                act.renderer.rayosInteraccion.selectMesh(mesh);
                float[] wm = bone.worldMatrix;
                act.renderer.gizmo.setPosition(wm[12], wm[13], wm[14]);
            }
        }); return btn;
    }

    private MyRenderer.Mesh obtenerMeshDesdeNombre(String nombre) {
        MainActivity act = (MainActivity) getActivity();
        if (act == null || act.renderer == null || act.renderer.rayosInteraccion == null) {
            return null;
        } return act.renderer.rayosInteraccion.obtenerMeshPorNombre(nombre);
    }

    private boolean boneAfectaMesh(Bones.Bone b, MyRenderer.Mesh mesh) {
        if (b.groupedVertices == null || mesh == null || mesh.name == null) { return false; }
        MainActivity act = (MainActivity) getActivity();
        if (act == null || act.renderer == null || act.renderer.rayosInteraccion == null) {
            return b.groupedVertices.containsKey(mesh);
        }
        String nombreBaseBuscado = act.renderer.rayosInteraccion.extraerNombreBase(mesh.name);
        for (MyRenderer.Mesh m : b.groupedVertices.keySet()) {
            if (m.name != null) {
                String baseM = act.renderer.rayosInteraccion.extraerNombreBase(m.name);
                if (baseM.equals(nombreBaseBuscado)) {
                    return true;
                }
            }
        } return false;
    }

    private void agregarBoneRecursivoFiltrado(LinearLayout layout, Bones.Bone bone, MyRenderer.Mesh mesh, int nivel, int baseMarginDp) {
        if (boneAfectaMesh(bone, mesh)) {
            int margin = (nivel * 5) + baseMarginDp;
            Button btnBone = crearBotonBone(bone, mesh, margin);
            btnBone.setBackgroundColor(Color.parseColor("#EEEEEE"));
            layout.addView(btnBone);
            for (Bones.Bone hijo : bone.children) {
                agregarBoneRecursivoFiltrado(layout, hijo, mesh, nivel + 1, baseMarginDp);
            }
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}