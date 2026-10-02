package ar.axt.nopeby;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import ar.axt.animar.Bones;
import ar.axt.animar.HistorialMovimientos;
import ar.axt.materiales.Material;
import ar.axt.nopeby.MyRenderer;

public class EditarObjetos extends Fragment {

    private EditText etX, etY, etZ;
    private EditText etRotX, etRotY, etRotZ;
    private EditText etScaleX, etScaleY, etScaleZ;
    private boolean isUpdatingFields = false;
    private boolean isEditingSessionActive = false;

    private PopupWindow popupTeclado;
    private EditText activeEditText = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.editarobj, container, false);

        etX = root.findViewById(R.id.et1);
        etY = root.findViewById(R.id.et2);
        etZ = root.findViewById(R.id.et3);
        etRotX = root.findViewById(R.id.et4);
        etRotY = root.findViewById(R.id.et5);
        etRotZ = root.findViewById(R.id.et6);
        etScaleX = root.findViewById(R.id.et7);
        etScaleY = root.findViewById(R.id.et8);
        etScaleZ = root.findViewById(R.id.et9);

        configurarCamposComoBotones();
        cargarValoresActuales();

        SeekBar sbFuerza = root.findViewById(R.id.sb_especular_fuerza);
        SeekBar sbBrillo = root.findViewById(R.id.sb_especular_brillo);
        SeekBar sbAlfa = root.findViewById(R.id.sb_mesh_transparencia);
        final SeekBar sbAlcance = root.findViewById(R.id.sb_luz_alcance);
        final SeekBar sbLuzR = root.findViewById(R.id.sb_luz_r);
        final SeekBar sbLuzG = root.findViewById(R.id.sb_luz_g);
        final SeekBar sbLuzB = root.findViewById(R.id.sb_luz_b);

        MainActivity act = (MainActivity) getActivity();
        if (act != null && act.renderer != null && act.renderer.rayosInteraccion != null && act.renderer.rayosInteraccion.selectedMesh != null) {
            MyRenderer.Mesh m = act.renderer.rayosInteraccion.selectedMesh;
            if (sbFuerza != null) sbFuerza.setProgress((int) (m.specularStrength * 50.0f));
            if (sbBrillo != null) sbBrillo.setProgress((int) m.shininess);
            if (sbAlfa != null) sbAlfa.setProgress((int) (m.opacity * 100.0f));
            if (sbAlcance != null) sbAlcance.setProgress((int) (m.lightRadiusMult * 20.0f));
            if (sbLuzR != null) sbLuzR.setProgress((int) (m.emissiveColor[0] * 255.0f));
            if (sbLuzG != null) sbLuzG.setProgress((int) (m.emissiveColor[1] * 255.0f));
            if (sbLuzB != null) sbLuzB.setProgress((int) (m.emissiveColor[2] * 255.0f));
        }

        SeekBar.OnSeekBarChangeListener specListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity == null) return;
                    int id = seekBar.getId();
                    if (id == R.id.sb_especular_fuerza) {
                        float fuerza = progress / 50.0f;
                        ar.axt.materiales.EditorReflejosMesh.actualizarFuerzaEspecular(activity, fuerza);
                    } else if (id == R.id.sb_especular_brillo) {
                        float shininess = Math.max(1.0f, (float) progress);
                        ar.axt.materiales.EditorReflejosMesh.actualizarShininess(activity, shininess);
                    } else if (id == R.id.sb_mesh_transparencia) {
                        float alfa = progress / 100.0f;
                        ar.axt.materiales.EditorAlfhaMesh.actualizarTransparencia(activity, alfa);
                    } else if (id == R.id.sb_luz_alcance) {
                        float mult = progress / 20.0f;
                        ar.axt.sombras.CancelarIluminacion.actualizarAlcanceLuz(activity, mult);
                    } else if (id == R.id.sb_luz_r || id == R.id.sb_luz_g || id == R.id.sb_luz_b) {
                        float r = (sbLuzR != null ? sbLuzR.getProgress() : 255) / 255.0f;
                        float g = (sbLuzG != null ? sbLuzG.getProgress() : 255) / 255.0f;
                        float b = (sbLuzB != null ? sbLuzB.getProgress() : 255) / 255.0f;
                        ar.axt.sombras.CancelarIluminacion.actualizarColorLuz(activity, r, g, b);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        if (sbFuerza != null) sbFuerza.setOnSeekBarChangeListener(specListener);
        if (sbBrillo != null) sbBrillo.setOnSeekBarChangeListener(specListener);
        if (sbAlfa != null) sbAlfa.setOnSeekBarChangeListener(specListener);
        if (sbAlcance != null) sbAlcance.setOnSeekBarChangeListener(specListener);
        if (sbLuzR != null) sbLuzR.setOnSeekBarChangeListener(specListener);
        if (sbLuzG != null) sbLuzG.setOnSeekBarChangeListener(specListener);
        if (sbLuzB != null) sbLuzB.setOnSeekBarChangeListener(specListener);

        Button btnB1 = root.findViewById(R.id.b1);
        Button btnB2 = root.findViewById(R.id.b2);

        if (btnB1 != null) {
            btnB1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity != null) {
                        ar.axt.leerobj.CambiarNormales.aplicarNormalesDifusas(activity);
                    }
                }
            });
        }

        if (btnB2 != null) {
            btnB2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity == null) return;
                    ar.axt.leerobj.CambiarNormales.aplicarNormalesDirectas(activity);
                }
            });
        }

        return root;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (popupTeclado != null && popupTeclado.isShowing()) {
            popupTeclado.dismiss();
        }
        finalizarSesionDeEdicion();
    }

    private void configurarCamposComoBotones() {
        EditText[] fields = {etX, etY, etZ, etRotX, etRotY, etRotZ, etScaleX, etScaleY, etScaleZ};
        for (final EditText et : fields) {
            if (et == null) continue;
            et.setFocusable(false);
            et.setFocusableInTouchMode(false);
            et.setClickable(true);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                et.setShowSoftInputOnFocus(false);
            }

            et.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    abrirTecladoParaCampo(et);
                }
            });
        }
    }

    private void abrirTecladoParaCampo(EditText et) {
        if (et == null) return;
        if (isEditingSessionActive && activeEditText != et) {
            finalizarSesionDeEdicion();
        }
        activeEditText = et;
        destacarCampoActivo(et);
        if (!isEditingSessionActive) {
            iniciarSesionDeEdicion();
        }
        mostrarPopupWindowTeclado(et);
    }

    private void destacarCampoActivo(EditText campoActivo) {
        EditText[] fields = {etX, etY, etZ, etRotX, etRotY, etRotZ, etScaleX, etScaleY, etScaleZ};
        for (EditText et : fields) {
            if (et != null) {
                if (et == campoActivo) {
                    et.setBackgroundColor(Color.parseColor("#334466"));
                    et.setTextColor(Color.YELLOW);
                } else {
                    et.setBackgroundColor(Color.parseColor("#222222"));
                    et.setTextColor(Color.WHITE);
                }
            }
        }
    }

    private void restaurarEstiloCampos() {
        EditText[] fields = {etX, etY, etZ, etRotX, etRotY, etRotZ, etScaleX, etScaleY, etScaleZ};
        for (EditText et : fields) {
            if (et != null) {
                et.setBackgroundColor(Color.parseColor("#222222"));
                et.setTextColor(Color.WHITE);
            }
        }
    }

    private void mostrarPopupWindowTeclado(View anchorView) {
        if (popupTeclado != null && popupTeclado.isShowing()) {
            popupTeclado.dismiss();
        }
        Context ctx = getActivity();
        if (ctx == null) return;
        LinearLayout layoutPadre = new LinearLayout(ctx);
        layoutPadre.setOrientation(LinearLayout.VERTICAL);
        layoutPadre.setPadding(12, 12, 12, 12);
        layoutPadre.setBackgroundColor(Color.parseColor("#1E1E2C"));
        String[][] tecladoLayout = {
            {"7", "8", "9", "⌫"},
            {"4", "5", "6", "±"},
            {"1", "2", "3", "."},
            {"0", "C", "✔ HECHO"}
        };
        for (String[] fila : tecladoLayout) {
            LinearLayout filaLayout = new LinearLayout(ctx);
            filaLayout.setOrientation(LinearLayout.HORIZONTAL);
            filaLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            for (final String textoBoton : fila) {
                Button btn = new Button(ctx);
                btn.setText(textoBoton);
                btn.setTextSize(16);
                btn.setTextColor(Color.WHITE);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                lp.setMargins(4, 4, 4, 4);
                btn.setLayoutParams(lp);
                if (textoBoton.equals("✔ HECHO")) {
                    btn.setBackgroundColor(Color.parseColor("#2E7D32"));
                } else if (textoBoton.equals("⌫") || textoBoton.equals("C")) {
                    btn.setBackgroundColor(Color.parseColor("#C62828"));
                } else if (textoBoton.equals("±") || textoBoton.equals(".")) {
                    btn.setBackgroundColor(Color.parseColor("#1565C0"));
                } else {
                    btn.setBackgroundColor(Color.parseColor("#37474F"));
                }

                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        procesarPulsacionTecla(textoBoton);
                    }
                });
                filaLayout.addView(btn);
            }
            layoutPadre.addView(filaLayout);
        }

        int ancho = (int) (ctx.getResources().getDisplayMetrics().density * 260);
        popupTeclado = new PopupWindow(layoutPadre, ancho, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupTeclado.setOutsideTouchable(true);
        popupTeclado.setFocusable(true);
        popupTeclado.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                restaurarEstiloCampos();
                finalizarSesionDeEdicion();
            }
        });
        popupTeclado.showAsDropDown(anchorView, 0, 8);
    }

    private void procesarPulsacionTecla(String tecla) {
        if (activeEditText == null) return;
        String actual = activeEditText.getText().toString();
        if (tecla.equals("✔ HECHO")) {
            if (popupTeclado != null && popupTeclado.isShowing()) {
                popupTeclado.dismiss();
            }
            return;
        }
        if (tecla.equals("C")) {
            activeEditText.setText("0");
            aplicarCambiosDesdeCampos();
            return;
        }
        if (tecla.equals("⌫")) {
            if (actual.length() > 1) {
                actual = actual.substring(0, actual.length() - 1);
                if (actual.equals("-") || actual.equals(".")) actual = "0";
            } else {
                actual = "0";
            }
            activeEditText.setText(actual);
            aplicarCambiosDesdeCampos();
            return;
        }
        if (tecla.equals("±")) {
            if (actual.startsWith("-")) {
                actual = actual.substring(1);
            } else if (!actual.equals("0")) {
                actual = "-" + actual;
            }
            activeEditText.setText(actual);
            aplicarCambiosDesdeCampos();
            return;
        }
        if (tecla.equals(".")) {
            if (!actual.contains(".")) {
                actual = actual + ".";
                activeEditText.setText(actual);
            }
            return;
        }
        if (actual.equals("0")) {
            actual = tecla;
        } else {
            actual = actual + tecla;
        }
        activeEditText.setText(actual);
        aplicarCambiosDesdeCampos();
    }

    private void cargarValoresActuales() {
        MainActivity act = (MainActivity) getActivity();
        if (act == null || act.renderer == null) return;
        isUpdatingFields = true;
        if (act.renderer.getBones() != null && act.renderer.getBones().getSelectedBone() != null) {
            Bones.Bone b = act.renderer.getBones().getSelectedBone();
            poblarCampos(b.position, b.rotation, b.scale);
        } else if (act.renderer.material != null && act.renderer.material.getSelectedBox() != null) {
            Material.EffectBox box = act.renderer.material.getSelectedBox();
            poblarCampos(box.position, box.rotation, new float[]{box.scale, box.scale, box.scale});
        } else if (act.renderer.rayosInteraccion.selectedMesh != null) {
            MyRenderer.Mesh m = act.renderer.rayosInteraccion.selectedMesh;
            poblarCampos(m.translation, m.rotation, m.scale);
        }
        isUpdatingFields = false;
    }

    public void actualizarCamposPublico() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    cargarValoresActuales();
                }
            });
        }
    }

    private void poblarCampos(float[] pos, float[] rot, float[] scale) {
        etX.setText(String.valueOf(pos[0]));
        etY.setText(String.valueOf(pos[1]));
        etZ.setText(String.valueOf(pos[2]));
        etRotX.setText(String.valueOf(rot[0]));
        etRotY.setText(String.valueOf(rot[1]));
        etRotZ.setText(String.valueOf(rot[2]));
        etScaleX.setText(String.valueOf(scale[0]));
        etScaleY.setText(String.valueOf(scale[1]));
        etScaleZ.setText(String.valueOf(scale[2]));
    }

    private float parsearFloat(EditText et, float valorPorDefecto) {
        try {
            String text = et.getText().toString().trim();
            if (text.isEmpty() || text.equals("-") || text.equals(".")) return valorPorDefecto;
            return Float.parseFloat(text);
        } catch (NumberFormatException e) {
            return valorPorDefecto;
        }
    }

    private void iniciarSesionDeEdicion() {
        MainActivity act = (MainActivity) getActivity();
        if (act == null || act.renderer == null || act.renderer.interaccionGismo == null) return;
        if (isEditingSessionActive) {
            finalizarSesionDeEdicion();
        }
        isEditingSessionActive = true;
        act.renderer.interaccionGismo.iniciarEdicionDesdeUI();
    }

    private void finalizarSesionDeEdicion() {
        if (!isEditingSessionActive) return;
        isEditingSessionActive = false;
        MainActivity act = (MainActivity) getActivity();
        if (act != null && act.renderer != null && act.renderer.interaccionGismo != null) {
            act.renderer.interaccionGismo.finalizarEdicionDesdeUI();
        }
    }


    private void aplicarCambiosDesdeCampos() {
        MainActivity act = (MainActivity) getActivity();
        if (act == null || act.renderer == null || act.renderer.interaccionGismo == null) return;
        float x = parsearFloat(etX, 0f);
        float y = parsearFloat(etY, 0f);
        float z = parsearFloat(etZ, 0f);
        float rx = parsearFloat(etRotX, 0f);
        float ry = parsearFloat(etRotY, 0f);
        float rz = parsearFloat(etRotZ, 0f);
        float sx = parsearFloat(etScaleX, 1f);
        float sy = parsearFloat(etScaleY, 1f);
        float sz = parsearFloat(etScaleZ, 1f);
        HistorialMovimientos historial = act.renderer.historialMovimientos;
        if (historial == null || historial.estadoInicialGrupo == null || historial.estadoInicialGrupo.isEmpty()) {
            iniciarSesionDeEdicion();
            historial = act.renderer.historialMovimientos;
            if (historial == null || historial.estadoInicialGrupo == null || historial.estadoInicialGrupo.isEmpty()) return;
        }
        HistorialMovimientos.EstadoInicial eiRef = null;
        for (HistorialMovimientos.EstadoInicial ei : historial.estadoInicialGrupo) {
            if (ei.mesh != null || ei.bone != null || ei.box != null) {
                eiRef = ei;
                break;
            }
        }
        if (eiRef == null) return;
        float dx = x - eiRef.pos[0];
        float dy = y - eiRef.pos[1];
        float dz = z - eiRef.pos[2];
        float drx = rx - eiRef.rot[0];
        float dry = ry - eiRef.rot[1];
        float drz = rz - eiRef.rot[2];
        float initialScaleX = eiRef.mesh != null ? eiRef.scale[0] : eiRef.bone != null ? eiRef.scale[0] : eiRef.scaleBox;
        float initialScaleY = eiRef.mesh != null ? eiRef.scale[1] : eiRef.bone != null ? eiRef.scale[1] : eiRef.scaleBox;
        float initialScaleZ = eiRef.mesh != null ? eiRef.scale[2] : eiRef.bone != null ? eiRef.scale[2] : eiRef.scaleBox;
        float dsx = sx / (initialScaleX == 0f ? 1f : initialScaleX);
        float dsy = sy / (initialScaleY == 0f ? 1f : initialScaleY);
        float dsz = sz / (initialScaleZ == 0f ? 1f : initialScaleZ);
        act.renderer.interaccionGismo.aplicarTraslacionDesdeUI(dx, dy, dz);
        act.renderer.interaccionGismo.aplicarRotacionDesdeUI(drx, dry, drz);
        act.renderer.interaccionGismo.aplicarEscalaDesdeUI(dsx, dsy, dsz);
        act.glSurfaceView.requestRender();
    }

}
