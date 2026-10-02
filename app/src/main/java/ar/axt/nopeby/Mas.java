package ar.axt.nopeby;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import ar.axt.animar.Bones;
import ar.axt.database.AdministrarDatos;
import ar.axt.leerobj.ObjetosCargados;
import ar.axt.materiales.Material;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Mas extends Fragment {
    private static final int PICK_FILE = 200;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.mas, container, false);
        Button btnAbrir = (Button) root.findViewById(R.id.btnAbrirObj);
        Button btnGuardar = (Button) root.findViewById(R.id.btnguardar);
        android.widget.ImageView imgPreview = (android.widget.ImageView) root.findViewById(R.id.impanel);
        MainActivity act = (MainActivity) getActivity();
        if (act != null && act.nombreProyecto != null) {
            File previewFile = new AdministrarDatos(act).getProyectoPreviewFile(act.nombreProyecto);
            if (previewFile.exists()) {
                imgPreview.setImageURI(Uri.fromFile(previewFile));
                imgPreview.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            } else {
                imgPreview.setImageResource(R.drawable.logo);
                imgPreview.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            }
        }

        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                Mas.this.guardarProyecto();
            }
        });
        btnAbrir.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                Mas.this.openFilePicker();
            }
        });

        Button btnTexturas = (Button) root.findViewById(R.id.btnTexturas);
        btnTexturas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity act = (MainActivity) getActivity();
                if (act != null) {
                    if (act.renderer.rayosInteraccion.selectedMesh == null) {
                        Toast.makeText(act, "Debes tener un mesh seleccionado", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    act.meshSeleccionadoNombre = act.renderer.rayosInteraccion.selectedMesh.name;
                    Mas.this.openTexturePicker();
                }
            }
        });

        final android.widget.TextView tvProgresoTextura = (android.widget.TextView) root.findViewById(R.id.tvProgresoTextura);
        Button btnCrearTexturas = (Button) root.findViewById(R.id.btnTexturascrear);
        if (btnCrearTexturas != null) {
            btnCrearTexturas.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final MainActivity act = (MainActivity) getActivity();
                    if (act == null || act.renderer == null) return;
                    if (act.renderer.rayosInteraccion == null || act.renderer.rayosInteraccion.selectedMesh == null) {
                        Toast.makeText(act, "Debes tener un modelo seleccionado", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    final String[] opciones = {"512 x 512 (Baja)", "1024 x 1024 (Recomendada)", "2048 x 2048 (Alta HD)", "4096 x 4096 (Ultra UHD)"};
                    final int[] resoluciones = {512, 1024, 2048, 4096};
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(act);
                    builder.setTitle("Selecciona Calidad de la Textura");
                    builder.setSingleChoiceItems(opciones, 1, new android.content.DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(android.content.DialogInterface dialog, int which) {
                            dialog.dismiss();
                            final int resolucionElegida = resoluciones[which];
                            if (tvProgresoTextura != null) {
                                tvProgresoTextura.setVisibility(View.VISIBLE);
                                tvProgresoTextura.setText("Iniciando exportación..."); }
                            ar.axt.leerobj.ExportarObj.exportarEscenaCompleta(act, resolucionElegida, new ar.axt.leerobj.ExportarObj.ExportProgressListener() {

                                @Override
                                public void onProgress(String fase, int porcentaje) {
                                    if (tvProgresoTextura != null) {
                                        tvProgresoTextura.setText(fase);
                                    }
                                }

                                @Override
                                public void onFinished(String rutaObj, String rutaCarpeta) {
                                    if (tvProgresoTextura != null) {
                                        tvProgresoTextura.setText("¡Exportación completada con éxito!");
                                    } Toast.makeText(act, "Escena Posed y Texturas guardadas en:\n" + rutaCarpeta, Toast.LENGTH_LONG).show();
                                }

                                @Override
                                public void onError(String error) {
                                    if (tvProgresoTextura != null) {
                                        tvProgresoTextura.setText("Error: " + error);
                                    }
                                    Toast.makeText(act, error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                    builder.setNegativeButton("Cancelar", null);
                    builder.show();
                }
            });
        }

        Button btnNormales = (Button) root.findViewById(R.id.normales);
        if (btnNormales != null) {
            btnNormales.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity act = (MainActivity) getActivity();
                    if (act != null) {
                        if (act.renderer == null || act.renderer.rayosInteraccion == null || act.renderer.rayosInteraccion.selectedMesh == null) {
                            Toast.makeText(act, "Debes seleccionar un modelo primero", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        act.meshSeleccionadoNombre = act.renderer.rayosInteraccion.selectedMesh.name;
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("*/*");
                        String[] mimeTypes = {"image/png", "image/jpeg", "image/jpg"};
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                        }
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        startActivityForResult(Intent.createChooser(intent, "Selecciona Mapa de Normales"), 202);
                    }
                }
            });
        }

        Button btnCerrar = (Button) root.findViewById(R.id.cerrar);
        if (btnCerrar != null) {
            btnCerrar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity act = (MainActivity) getActivity();
                    if (act != null) {
                        act.noGuardarAlSalir = true;
                        act.finish();
                    }
                }
            });
        }

        return root;
    }

    private void openTexturePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {"image/png", "image/jpeg", "image/jpg", "application/octet-stream"};
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Selecciona textura o MTL"), 201);
    }

    public void openFilePicker() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("*/*");
        intent.addCategory("android.intent.category.OPENABLE");
        startActivityForResult(Intent.createChooser(intent, "Selecciona un modelo"), 200);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri uri;
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != -1 || data == null || (uri = data.getData()) == null) {
            return;
        }
        MainActivity act = (MainActivity) getActivity();
        if (act == null) return;
        if (act.meshSeleccionadoNombre != null && act.renderer != null) {
            for (MyRenderer.Mesh mesh : act.renderer.meshes) {
                if (mesh != null && act.meshSeleccionadoNombre.equals(mesh.name)) {
                    act.renderer.rayosInteraccion.selectedMesh = null;
                    act.renderer.rayosInteraccion.lastSelectedMesh = null;
                    act.renderer.rayosInteraccion.selectMesh(mesh); 
                    break;
                }
            }
        }
        if (requestCode == 200) {
            try {
                FileUtils.getFileName(getActivity(), uri);
                ObjetosCargados loader = new ObjetosCargados(getActivity());
                List<ObjetosCargados.SubMesh> meshes = loader.loadFromUri(uri);
                act.loadObjInRenderer(meshes);
                act.listaArchivos.clear();
                act.listaArchivos.addAll(FileUtils.getArchivosImportados());
                Toast.makeText(getActivity(), "Modelo cargado", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(getActivity(), "Error al cargar modelo", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else if (requestCode == 201) {
            if (act.renderer != null && act.renderer.rayosInteraccion.selectedMesh != null) {
                final MyRenderer.Mesh meshTarget = act.renderer.rayosInteraccion.selectedMesh;
                final ar.axt.leerobj.TexturaLoader texLoader = new ar.axt.leerobj.TexturaLoader(getActivity());
                final String nombreArchivo = FileUtils.getFileName(getActivity(), uri);
                try {
                    if (nombreArchivo != null && (nombreArchivo.toLowerCase().endsWith(".png") || nombreArchivo.toLowerCase().endsWith(".jpg") || nombreArchivo.toLowerCase().endsWith(".jpeg"))) {
                        final android.graphics.Bitmap bitmapCargado = texLoader.loadFromUri(uri);
                        if (bitmapCargado != null) {
                            byte[] textureBytes = null;
                            String mimeType = nombreArchivo.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
                            try {
                                java.io.InputStream is = getActivity().getContentResolver().openInputStream(uri);
                                if (is != null) {
                                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                                    byte[] buf = new byte[8192];
                                    int len;
                                    while ((len = is.read(buf)) != -1) {
                                        baos.write(buf, 0, len);
                                    }
                                    textureBytes = baos.toByteArray();
                                    is.close();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            List<MyRenderer.Mesh> todosRelacionados = act.renderer.rayosInteraccion.getMeshesRelacionados(meshTarget);
                            if (todosRelacionados == null || todosRelacionados.isEmpty()) {
                                todosRelacionados = new ArrayList<>();
                                todosRelacionados.add(meshTarget);
                            }

                            for (MyRenderer.Mesh mRel : todosRelacionados) {
                                if (mRel != null && mRel.subMesh != null) {
                                    mRel.subMesh.pendingTexture = bitmapCargado;
                                    if (textureBytes != null) {
                                        mRel.subMesh.embeddedTexture = textureBytes;
                                        mRel.subMesh.embeddedTextureMimeType = mimeType;
                                    }
                                }
                            }
                            act.glSurfaceView.requestRender();
                            Toast.makeText(getActivity(), "Textura aplicada a todas las partes del modelo", Toast.LENGTH_SHORT).show();
                        }
                    } else if (nombreArchivo != null && nombreArchivo.toLowerCase().endsWith(".mtl")) {
                        List<String> requeridas = texLoader.extractPngFromMtl(uri);
                        Toast.makeText(getActivity(), "MTL: " + requeridas.toString(), Toast.LENGTH_LONG).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(getActivity(), "Error al abrir el archivo de imagen.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getActivity(), "Selecciona un modelo primero", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 202) {
            ar.axt.leerobj.NormalesLoader.aplicarMapaDeNormales(act, uri);
        }
    }


    public void guardarProyecto() {
        MainActivity act = (MainActivity) getActivity();
        if (act.nombreProyecto == null) { return; }
        AdministrarDatos admin = new AdministrarDatos(getActivity());
        List<Bones.Bone> bones = act.renderer.getBones() != null ? act.renderer.getBones().getAllBones() : new ArrayList<>();
        List<Material.EffectBox> boxes = act.renderer.material != null ? act.renderer.material.getBoxes() : new ArrayList<>();
        admin.guardarProyectoBin(act.nombreProyecto, act.renderer.meshes, bones, boxes, act.renderer.getShaderRealista(), act.renderer);
    }

}
