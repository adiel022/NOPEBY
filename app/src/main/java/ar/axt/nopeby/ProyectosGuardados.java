package ar.axt.nopeby;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.net.Uri;
import android.widget.ImageView;
import ar.axt.database.AdministrarDatos;
import ar.axt.database.DatosProyectos;
import java.io.File;
import java.util.ArrayList;

public class ProyectosGuardados extends AppCompatActivity {
    DatosProyectos db;
    LinearLayout l1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.proyectos);
        this.l1 = (LinearLayout) findViewById(R.id.l1);
        this.db = new DatosProyectos(this);
        cargarProyectos();
    }

    private void cargarProyectos() {
        ArrayList<String> nombres = this.db.obtenerNombresProyectos();
        if (nombres == null || nombres.isEmpty()) {
            ImageButton b = new ImageButton(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION), dp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION));
            b.setLayoutParams(lp);
            b.setBackgroundResource(R.drawable.crear);
            b.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View v) {
                    ProyectosGuardados.this.crearProyecto(v);
                }
            }); this.l1.addView(b); return;
        }
        for (final String nombre : nombres) {
            LinearLayout contenedor = new LinearLayout(this);
            contenedor.setOrientation(1);
            LinearLayout.LayoutParams lpCont = new LinearLayout.LayoutParams(-2, -2);
            lpCont.setMargins(0, 0, dp(5), 0);
            contenedor.setLayoutParams(lpCont);
            ImageButton img = new ImageButton(this);
            img.setLayoutParams(new LinearLayout.LayoutParams(dp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION), dp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION)));
            File previewFile = new AdministrarDatos(this).getProyectoPreviewFile(nombre);
            if (previewFile.exists()) {
                img.setImageURI(Uri.fromFile(previewFile));
                img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                img.setImageResource(R.drawable.logo);
                img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }
            final TextView tv = new TextView(this);
            tv.setText(nombre);
            tv.setTextSize(18.0f);
            contenedor.addView(img);
            contenedor.addView(tv);
            ImageButton btnEliminar = new ImageButton(this);
            btnEliminar.setLayoutParams(new LinearLayout.LayoutParams(dp(50), dp(50)));
            btnEliminar.setBackgroundResource(R.drawable.x);
            btnEliminar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String texto = tv.getText().toString();
                    new AdministrarDatos(ProyectosGuardados.this).eliminarProyectoCompleto(texto);
                    ProyectosGuardados.this.l1.removeAllViews();
                    ProyectosGuardados.this.cargarProyectos();
                }
            }); contenedor.addView(btnEliminar);
            this.l1.addView(contenedor);
            img.setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View v) {
                    int shader = ProyectosGuardados.this.db.obtenerShaderPorNombre(nombre);
                    Intent i = new Intent(ProyectosGuardados.this, (Class<?>) MainActivity.class);
                    i.putExtra("NOMBRE_PROYECTO", nombre);
                    i.putExtra("SL", shader);
                    ProyectosGuardados.this.startActivity(i);
                    ProyectosGuardados.this.finish();
                }
            });
        }
    }

    private int dp(int valor) {
        return (int) TypedValue.applyDimension(1, valor, getResources().getDisplayMetrics());
    }

    public void crearProyecto(View v) {
        Intent a = new Intent(this, (Class<?>) NewProject.class);
        startActivity(a);
        finish();
    }
}
