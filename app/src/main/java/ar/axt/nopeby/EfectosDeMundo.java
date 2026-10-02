package ar.axt.nopeby;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;

public class EfectosDeMundo extends Fragment {
    private MainActivity activity;
    private MyRenderer render;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.activity = (MainActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.efectosdemundo, container, false);
        this.render = this.activity.renderer;
        Button a = (Button) root.findViewById(R.id.lluvia);
        Button b = (Button) root.findViewById(R.id.nieve);
        ImageButton fuego = (ImageButton) root.findViewById(R.id.fuegoef);
        ImageButton tierra = (ImageButton) root.findViewById(R.id.tierraef);
        ImageButton agua = (ImageButton) root.findViewById(R.id.aguaef);
        ImageButton humo = (ImageButton) root.findViewById(R.id.humoef);
        ImageButton polvo = (ImageButton) root.findViewById(R.id.polvoef);
        ImageButton explocion = (ImageButton) root.findViewById(R.id.explocionef);
        a.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EfectosDeMundo.this.toggleLluvia();
            }
        });
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EfectosDeMundo.this.toggleNieve();
            }
        });
        fuego.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EfectosDeMundo.this.materialFuego();
            }
        });
        agua.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EfectosDeMundo.this.materialAgua();
            }
        });
        tierra.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EfectosDeMundo.this.materialTierra();
            }
        });
        polvo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EfectosDeMundo.this.materialPolvo();
            }
        });
        humo.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                EfectosDeMundo.this.materialHumo();
            }
        });
        explocion.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                EfectosDeMundo.this.materialExplocion();
            }
        });

        SeekBar sbR = (SeekBar) root.findViewById(R.id.sb_cielo_r);
        SeekBar sbG = (SeekBar) root.findViewById(R.id.sb_cielo_g);
        SeekBar sbB = (SeekBar) root.findViewById(R.id.sb_cielo_b);
        SeekBar sbNubes = (SeekBar) root.findViewById(R.id.sb_cielo_nubes);
        SeekBar sbEstrellas = (SeekBar) root.findViewById(R.id.sb_cielo_estrellas);

        if (this.render != null) {
            if (sbR != null) sbR.setProgress((int) (this.render.skyColor[0] * 255.0f));
            if (sbG != null) sbG.setProgress((int) (this.render.skyColor[1] * 255.0f));
            if (sbB != null) sbB.setProgress((int) (this.render.skyColor[2] * 255.0f));
            if (sbNubes != null) sbNubes.setProgress((int) (this.render.cloudDensity * 100.0f));
            if (sbEstrellas != null) sbEstrellas.setProgress((int) (this.render.starDensity * 100.0f));
        }

        SeekBar.OnSeekBarChangeListener skyListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && EfectosDeMundo.this.render != null) {
                    int id = seekBar.getId();
                    if (id == R.id.sb_cielo_r) {
                        EfectosDeMundo.this.render.skyColor[0] = progress / 255.0f;
                    } else if (id == R.id.sb_cielo_g) {
                        EfectosDeMundo.this.render.skyColor[1] = progress / 255.0f;
                    } else if (id == R.id.sb_cielo_b) {
                        EfectosDeMundo.this.render.skyColor[2] = progress / 255.0f;
                    } else if (id == R.id.sb_cielo_nubes) {
                        EfectosDeMundo.this.render.cloudDensity = progress / 100.0f;
                    } else if (id == R.id.sb_cielo_estrellas) {
                        EfectosDeMundo.this.render.starDensity = progress / 100.0f;
                    }
                    if (EfectosDeMundo.this.activity != null && EfectosDeMundo.this.activity.glSurfaceView != null) {
                        EfectosDeMundo.this.activity.glSurfaceView.requestRender();
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        if (sbR != null) sbR.setOnSeekBarChangeListener(skyListener);
        if (sbG != null) sbG.setOnSeekBarChangeListener(skyListener);
        if (sbB != null) sbB.setOnSeekBarChangeListener(skyListener);
        if (sbNubes != null) sbNubes.setOnSeekBarChangeListener(skyListener);
        if (sbEstrellas != null) sbEstrellas.setOnSeekBarChangeListener(skyListener);

        return root;
    }

    public void toggleLluvia() {
        if (this.render.lluviasino == 0) {
            this.render.lluviasino = 1;
        } else {
            this.render.lluviasino = 0;
        }
    }

    public void toggleNieve() {
        if (this.render.nievesino == 0) {
            this.render.nievesino = 1;
        } else {
            this.render.nievesino = 0;
        }
    }

    public void materialFuego() {
        this.render.material.addEffectBox(1);
        this.render.fuego.rebuildParticles();
        this.render.humo.rebuildParticles();
    }


    public void materialTierra() {
        this.render.material.addEffectBox(2);
        this.render.tierra.rebuildParticles();
    }

    public void materialAgua() {
        this.render.material.addEffectBox(3);
        this.render.gotas.rebuildParticles();
    }

    public void materialHumo() {
        this.render.material.addEffectBox(4);
        this.render.humo.rebuildParticles();
    }


    public void materialPolvo() {
        this.render.material.addEffectBox(5);
        this.render.polvo.rebuildParticles();
    }

    public void materialExplocion() {
        this.render.material.addEffectBox(6);
        this.render.explocion.rebuildParticles();
        this.render.polvo.rebuildParticles();
    }
}
