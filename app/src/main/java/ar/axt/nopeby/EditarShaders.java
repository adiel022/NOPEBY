package ar.axt.nopeby;

import android.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;
import ar.axt.sombras.RealShadow;
import ar.axt.sombras.ShadowAnime;

public class EditarShaders extends Fragment {
    private ShadowAnime animeShadow;
    private SeekBar bluz;
    private SeekBar bsombra;
    private EditText etLuz;
    private EditText etSombra;
    private SeekBar gluz;
    private SeekBar gsombra;
    private MainActivity mainActivity;
    private RealShadow realShadow;
    private SeekBar rluz;
    private SeekBar rsombra;
    private SeekBar sbLuzX, sbLuzY, sbLuzZ;

    @Override // android.app.Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.editar_shaders, container, false);
        this.mainActivity = (MainActivity) getActivity();
        if (this.mainActivity != null && this.mainActivity.renderer != null) {
            this.realShadow = this.mainActivity.renderer.getShaderRealista();
            this.animeShadow = this.mainActivity.renderer.getShaderAnime();
        }
        this.etLuz = (EditText) root.findViewById(R.id.et1);
        this.etSombra = (EditText) root.findViewById(R.id.et2);
        this.rluz = (SeekBar) root.findViewById(R.id.rluz);
        this.gluz = (SeekBar) root.findViewById(R.id.gluz);
        this.bluz = (SeekBar) root.findViewById(R.id.bluz);
        this.rsombra = (SeekBar) root.findViewById(R.id.rsombra);
        this.gsombra = (SeekBar) root.findViewById(R.id.gsombra);
        this.bsombra = (SeekBar) root.findViewById(R.id.bsombra);
        this.sbLuzX = (SeekBar) root.findViewById(R.id.sb_luz_x);
        this.sbLuzY = (SeekBar) root.findViewById(R.id.sb_luz_y);
        this.sbLuzZ = (SeekBar) root.findViewById(R.id.sb_luz_z);
        if (this.realShadow != null) {
            this.etLuz.setText(String.valueOf(this.realShadow.lightIntensity));
            this.etSombra.setText(String.valueOf(this.realShadow.shadowStrength));
            this.rluz.setProgress((int) (this.realShadow.lightColor[0] * 255.0f));
            this.gluz.setProgress((int) (this.realShadow.lightColor[1] * 255.0f));
            this.bluz.setProgress((int) (this.realShadow.lightColor[2] * 255.0f));
            this.rsombra.setProgress((int) (this.realShadow.shadowColor[0] * 255.0f));
            this.gsombra.setProgress((int) (this.realShadow.shadowColor[1] * 255.0f));
            this.bsombra.setProgress((int) (this.realShadow.shadowColor[2] * 255.0f));
            
            this.sbLuzX.setProgress((int) (this.realShadow.lightPos[0] + 50.0f));
            this.sbLuzY.setProgress((int) (this.realShadow.lightPos[1] + 50.0f));
            this.sbLuzZ.setProgress((int) (this.realShadow.lightPos[2] + 50.0f));
        } else if (this.animeShadow != null) {
            this.etLuz.setText(String.valueOf(this.animeShadow.lightIntensity));
            this.etSombra.setText(String.valueOf(this.animeShadow.shadowStrength));
            
            this.sbLuzX.setProgress((int) (this.animeShadow.lightPos[0] + 50.0f));
            this.sbLuzY.setProgress((int) (this.animeShadow.lightPos[1] + 50.0f));
            this.sbLuzZ.setProgress((int) (this.animeShadow.lightPos[2] + 50.0f));
        }
        setupListeners();
        return root;
    }

    private void setupListeners() {
        this.etLuz.addTextChangedListener(new TextWatcher() { // from class: ar.axt.nopeby.EditarShaders.1
            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable s) {
                try {
                    float val = Float.parseFloat(s.toString());
                    if (EditarShaders.this.realShadow != null) {
                        EditarShaders.this.realShadow.lightIntensity = val;
                    }
                    if (EditarShaders.this.animeShadow != null) {
                        EditarShaders.this.animeShadow.lightIntensity = val;
                    }
                    EditarShaders.this.mainActivity.glSurfaceView.requestRender();
                } catch (NumberFormatException e) {
                }
            }
        });
        this.etSombra.addTextChangedListener(new TextWatcher() { // from class: ar.axt.nopeby.EditarShaders.2
            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable s) {
                try {
                    float val = Float.parseFloat(s.toString());
                    if (EditarShaders.this.realShadow != null) {
                        EditarShaders.this.realShadow.shadowStrength = val;
                    }
                    if (EditarShaders.this.animeShadow != null) {
                        EditarShaders.this.animeShadow.shadowStrength = val;
                    }
                    EditarShaders.this.mainActivity.glSurfaceView.requestRender();
                } catch (NumberFormatException e) {
                }
            }
        });
        SeekBar.OnSeekBarChangeListener colorListener = new SeekBar.OnSeekBarChangeListener() { // from class: ar.axt.nopeby.EditarShaders.3
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float val = progress / 255.0f;
                    int id = seekBar.getId();
                    if (EditarShaders.this.realShadow != null) {
                        if (id == R.id.rluz) {
                            EditarShaders.this.realShadow.lightColor[0] = val;
                        } else if (id == R.id.gluz) {
                            EditarShaders.this.realShadow.lightColor[1] = val;
                        } else if (id == R.id.bluz) {
                            EditarShaders.this.realShadow.lightColor[2] = val;
                        } else if (id == R.id.rsombra) {
                            EditarShaders.this.realShadow.shadowColor[0] = val;
                        } else if (id == R.id.gsombra) {
                            EditarShaders.this.realShadow.shadowColor[1] = val;
                        } else if (id == R.id.bsombra) {
                            EditarShaders.this.realShadow.shadowColor[2] = val;
                        }
                    }
                    EditarShaders.this.mainActivity.glSurfaceView.requestRender();
                }
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
        this.rluz.setOnSeekBarChangeListener(colorListener);
        this.gluz.setOnSeekBarChangeListener(colorListener);
        this.bluz.setOnSeekBarChangeListener(colorListener);
        this.rsombra.setOnSeekBarChangeListener(colorListener);
        this.gsombra.setOnSeekBarChangeListener(colorListener);
        this.bsombra.setOnSeekBarChangeListener(colorListener);

        SeekBar.OnSeekBarChangeListener lightPosListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float val = progress - 50.0f;
                    int id = seekBar.getId();
                    if (EditarShaders.this.realShadow != null) {
                        if (id == R.id.sb_luz_x) {
                            EditarShaders.this.realShadow.lightPos[0] = val;
                        } else if (id == R.id.sb_luz_y) {
                            EditarShaders.this.realShadow.lightPos[1] = val;
                        } else if (id == R.id.sb_luz_z) {
                            EditarShaders.this.realShadow.lightPos[2] = val;
                        }
                    }
                    if (EditarShaders.this.animeShadow != null) {
                        if (id == R.id.sb_luz_x) {
                            EditarShaders.this.animeShadow.lightPos[0] = val;
                        } else if (id == R.id.sb_luz_y) {
                            EditarShaders.this.animeShadow.lightPos[1] = val;
                        } else if (id == R.id.sb_luz_z) {
                            EditarShaders.this.animeShadow.lightPos[2] = val;
                        }
                    }
                    EditarShaders.this.mainActivity.glSurfaceView.requestRender();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        this.sbLuzX.setOnSeekBarChangeListener(lightPosListener);
        this.sbLuzY.setOnSeekBarChangeListener(lightPosListener);
        this.sbLuzZ.setOnSeekBarChangeListener(lightPosListener);
    }
}
