package ar.axt.nopeby;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;


public class Menu extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu);
    }

    public void crearProyecto(View v) {
        Intent a = new Intent(this, (Class<?>) NewProject.class);
        startActivity(a);
    }

    public void proyectos(View v) {
        Intent b = new Intent(this, (Class<?>) ProyectosGuardados.class);
        startActivity(b);
    }

    public void enviarReporteOBug(View v) {
        String emailDestino = "adielruiz2006@gmail.com";
        String asunto = "Reporte de bug o sugerir mejora";
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + Uri.encode(emailDestino)));
        intent.putExtra(Intent.EXTRA_SUBJECT, asunto);
        try {
            startActivity(Intent.createChooser(intent, "Enviar correo con:"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No se encontró ninguna aplicación de correo instalada", Toast.LENGTH_SHORT).show();
        }
    }

}
