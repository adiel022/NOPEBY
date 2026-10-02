package ar.axt.nopeby;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import ar.axt.controles.Copiar;

public class Anadir extends Fragment {

    private ImageButton b1, b2, b3, b4, b5;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.anadir, container, false);
        b1 = root.findViewById(R.id.b1);
        b2 = root.findViewById(R.id.b2);
        b3 = root.findViewById(R.id.b3);
        b4 = root.findViewById(R.id.b4);
        b5 = root.findViewById(R.id.b5);

        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null && activity.renderer != null) {
                    Copiar copiador = new Copiar(activity, activity.renderer);
                    copiador.ejecutarCopia();
                }
            }
        });

        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null && activity.renderer != null && activity.renderer.administradorCamaras != null) {
                    activity.renderer.administradorCamaras.anadirCamara();
                }
            }
        });

        b3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null && activity.renderer != null && activity.renderer.administradorCamaras != null) {
                    activity.renderer.administradorCamaras.siguienteCamara();
                }
            }
        });

        b4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null && activity.renderer != null && activity.renderer.administradorCamaras != null) {
                    activity.renderer.administradorCamaras.anteriorCamara();
                }
            }
        });

        b5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null && activity.renderer != null && activity.renderer.administradorCamaras != null) {
                    activity.renderer.administradorCamaras.eliminarCamara();
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
