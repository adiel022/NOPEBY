package ar.axt.nopeby;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import android.widget.EditText;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.Editable;
import ar.axt.materiales.Material;
import java.util.List;

public class ListaMaterialParticulas extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        List<Material.EffectBox> boxes;
        View root = inflater.inflate(R.layout.objlista, container, false);
        LinearLayout l1 = (LinearLayout) root.findViewById(R.id.l1);
        l1.removeAllViews();
        MainActivity act = (MainActivity) getActivity();
        if (act == null || act.renderer == null || act.renderer.material == null || (boxes = act.renderer.material.getBoxes()) == null) {
            return root;
        }
        for (int i = 0; i < boxes.size(); i++) {
            Material.EffectBox box = boxes.get(i);
            View row = crearFila(box);
            l1.addView(row);
        } return root;
    }

    private View crearFila(final Material.EffectBox box) {
        LinearLayout row = new LinearLayout(getActivity());
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(35));
        rowParams.setMargins(dpToPx(5), dpToPx(2), dpToPx(5), dpToPx(2));
        row.setLayoutParams(rowParams);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        Button btn = new Button(getActivity());
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(dpToPx(140), dpToPx(30));
        btn.setLayoutParams(btnParams);
        btn.setText(box.id);
        btn.setBackgroundColor(-1);
        btn.setTextColor(ViewCompat.MEASURED_STATE_MASK);
        btn.setTextSize(2, 10.0f);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity act = (MainActivity) getActivity();
                if (act != null && act.renderer != null && act.renderer.material != null) {
                    act.renderer.material.selectBox(box.id);
                    Toast.makeText(act, "Caja seleccionada: " + box.id, Toast.LENGTH_SHORT).show();
                }
            }
        });
        final EditText etParticles = new EditText(getActivity());
        LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(dpToPx(70), dpToPx(30));
        etParams.setMargins(dpToPx(5), 0, 0, 0);
        etParticles.setLayoutParams(etParams);
        etParticles.setPadding(dpToPx(5), 0, dpToPx(5), 0);
        etParticles.setBackgroundColor(android.graphics.Color.WHITE);
        etParticles.setTextColor(android.graphics.Color.BLACK);
        etParticles.setTextSize(2, 12.0f);
        etParticles.setInputType(InputType.TYPE_CLASS_NUMBER);
        etParticles.setText(String.valueOf(box.numParticles));
        etParticles.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    String valStr = s.toString().trim();
                    if (!valStr.isEmpty()) {
                        int val = Integer.parseInt(valStr);
                        if (val > 1000) val = 1000;
                        if (val < 0) val = 0;
                        box.numParticles = val;
                        MainActivity act = (MainActivity) getActivity();
                        if (act != null && act.renderer != null) {
                            act.renderer.rebuildAllParticles();
                        }
                    }
                } catch (NumberFormatException e) {
                }
            }
        });
        row.addView(btn);
        row.addView(etParticles);
        return row;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
