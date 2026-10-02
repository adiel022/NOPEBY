package ar.axt.nopeby;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import androidx.appcompat.app.AppCompatActivity;

public class Presentacion extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.presentacion);
        new CountDownTimer(2000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {}

            @Override
            public void onFinish() {
                Intent intent = new Intent(Presentacion.this, (Class<?>) Menu.class);
                Presentacion.this.startActivity(intent);
                Presentacion.this.finish();
            } }.start();

     }
}
