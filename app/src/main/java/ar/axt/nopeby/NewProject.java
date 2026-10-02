package ar.axt.nopeby;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import ar.axt.database.AdministrarDatos;
import ar.axt.database.DatosProyectos;

public class NewProject extends AppCompatActivity {
    private RadioButton anime;
    private EditText et1;
    private RadioButton real;
    int selecciom = 0;
    private RadioButton simple;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_project);
        this.et1 = (EditText) findViewById(R.id.t1);
        this.real = (RadioButton) findViewById(R.id.rb1);
        this.simple = (RadioButton) findViewById(R.id.rb2);
        this.anime = (RadioButton) findViewById(R.id.rb3);
    }

    public void Crear(View v) {
        String nombre = this.et1.getText().toString().trim();
        if (nombre.isEmpty()) { return; }
        if (this.real.isChecked() || this.simple.isChecked() || this.anime.isChecked()) {
            if (this.real.isChecked()) { this.selecciom = 1;
            } else if (this.simple.isChecked()) { this.selecciom = 2;
            } else { this.selecciom = 3; }
            DatosProyectos datos = new DatosProyectos(this);
            if (datos.existeProyecto(nombre)) {
                Toast.makeText(this, "Proyecto ya existente", Toast.LENGTH_SHORT).show();
                return;
            }
            long idProyecto = datos.crearProyecto(nombre, this.selecciom);
            AdministrarDatos admin = new AdministrarDatos(this);
            admin.guardarProyectoBin(nombre, null, null, null, null, null);
            Intent b = new Intent(this, (Class<?>) MainActivity.class);
            b.putExtra("SL", this.selecciom);
            b.putExtra("ID_PROYECTO", idProyecto);
            b.putExtra("NOMBRE_PROYECTO", nombre);
            startActivity(b);
            finish();
        }
    }

}
