package ar.axt.nopeby;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

public class ValidarObj extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.validacion_obj, container, false);
        ImageButton b1 = (ImageButton) root.findViewById(R.id.b1);
        ImageButton b2 = (ImageButton) root.findViewById(R.id.b2);
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity activity = (MainActivity) ValidarObj.this.getActivity();
                if (activity != null && activity.huesos == 1 && activity.renderer != null && activity.renderer.historialMovimientos != null) {
                    activity.renderer.historialMovimientos.retrocederMovimientoSecundario();
                }
            }
        });
        b2.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                MainActivity activity = (MainActivity) ValidarObj.this.getActivity();
                if (activity != null && activity.huesos == 1 && activity.renderer != null && activity.renderer.historialMovimientos != null) {
                    activity.renderer.historialMovimientos.avanzarMovimientoSecundario();
                }
            }
        });
        return root;
    }

    public void cerrarFragmento() {
        if (getFragmentManager() != null) {
            getFragmentManager().beginTransaction().remove(this).commit();
        }
    }
}
