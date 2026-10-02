package ar.axt.nopeby;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import java.util.TreeMap;

public class LineaTiempo extends Fragment {

    private RecyclerView recyclerView;
    private TimelineAdapter adapter;
    private ar.axt.animar.CrearAnimaciones crearAnimaciones;
    private final TreeMap<Integer, ar.axt.animar.CrearAnimaciones.Keyframe> cacheKeyframes = new TreeMap<>();
    private int segundoActual = 0;
    private float tiempoActual = 0.0f;

    private final Handler playHandler = new Handler(Looper.getMainLooper());
    private Runnable playRunnable;
    private boolean isPlaying = false;
    private int itemWidthPixels = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.linea_tiempo, container, false);
        recyclerView = root.findViewById(R.id.recycler_timeline);
        setupRecyclerView();
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            crearAnimaciones = new ar.axt.animar.CrearAnimaciones(activity);
            if (activity.nombreProyecto != null) {
                TreeMap<Integer, ar.axt.animar.CrearAnimaciones.Keyframe> loaded = crearAnimaciones.cargarKeyframes(activity.nombreProyecto);
                if (loaded != null) {
                    cacheKeyframes.clear();
                    cacheKeyframes.putAll(loaded);
                    adapter.setSegundosConAnimacion(cacheKeyframes.keySet());
                }
            }
        }

        ImageButton b0 = root.findViewById(R.id.b0);
        b0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.renderer.rayosInteraccion.selectedMesh = null;
                activity.renderer.material.selectedBox = null;
                activity.renderer.bone.selectedBone = null;
                togglePlayAnimation();
            }
        });

        ImageButton b1 = root.findViewById(R.id.b1);
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarEstadoEnSegundoActual();
            }
        });

        ImageButton b2 = root.findViewById(R.id.b2);
        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eliminarEstadoEnSegundoActual();
            }
        });

        ImageButton b3 = root.findViewById(R.id.b3);
        b3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cerrarFragmento();
            }
        });
        
        return root;
    }

    @Override
    public void onPause() {
        super.onPause();
        detenerAnimacion();
    }

    public void cerrarFragmento() {
        detenerAnimacion();
        if (getFragmentManager() != null) {
            getFragmentManager().beginTransaction().remove(this).commit();
        }
    }

    private void setupRecyclerView() {
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new TimelineAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                int halfWidth = recyclerView.getWidth() / 2;
                recyclerView.setPadding(halfWidth, 0, halfWidth, 0);
                View dummy = layoutManager.findViewByPosition(0);
                if (dummy != null) {
                    itemWidthPixels = dummy.getWidth();
                } else {
                    float density = getResources().getDisplayMetrics().density;
                    itemWidthPixels = (int) (20 * density); 
                }
            }
        });
        LinearSnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                calculateCurrentTime();
            }
        });
    }

    private void calculateCurrentTime() {
        View centerView = findCenterView();
        if (centerView != null) {
            int position = recyclerView.getChildAdapterPosition(centerView);
            segundoActual = position;
            if (!isPlaying) {
                tiempoActual = (float) segundoActual;
            }
        }
    }

    @Nullable
    private View findCenterView() {
        int center = recyclerView.getWidth() / 2;
        int minDistance = Integer.MAX_VALUE;
        View centerView = null;
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View child = recyclerView.getChildAt(i);
            int childCenter = (child.getLeft() + child.getRight()) / 2;
            int distance = Math.abs(center - childCenter);
            if (distance < minDistance) {
                minDistance = distance;
                centerView = child;
            }
        } return centerView;
    }

    private void togglePlayAnimation() {
        final MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;
        if (isPlaying) {
            detenerAnimacion();
        } else {
            isPlaying = true;
            activity.isPlayingAnimation = true;
            tiempoActual = (float) segundoActual;
            if (activity.glSurfaceView != null) {
                activity.glSurfaceView.requestRender(); 
            }
            playRunnable = new Runnable() {
                @Override
                public void run() {
                    if (!isPlaying) return;
                    tiempoActual += 0.04f;
                    // Aplicar LERP en el renderizador
                    if (crearAnimaciones != null && activity.renderer != null && activity.glSurfaceView != null) {
                        final float tiempoLoop = tiempoActual;
                        activity.glSurfaceView.queueEvent(new Runnable() {
                            @Override
                            public void run() {
                                crearAnimaciones.interpolarEscena(tiempoLoop, activity.renderer, cacheKeyframes);
                            }
                        });
                    }
                    if (itemWidthPixels > 0) {
                        int halfWidth = recyclerView.getWidth() / 2;
                        LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                        if (lm != null) {
                            int offsetPixeles = (int) (tiempoActual * itemWidthPixels) - halfWidth;
                            lm.scrollToPositionWithOffset(0, -offsetPixeles);
                        }
                    } playHandler.postDelayed(this, 40);
                }
            }; playHandler.postDelayed(playRunnable, 40);
        }
    }

    private void detenerAnimacion() {
        isPlaying = false;
        playHandler.removeCallbacksAndMessages(null);
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.isPlayingAnimation = false;
            if (activity.glSurfaceView != null) {
                activity.glSurfaceView.requestRender(); 
            }
            segundoActual = Math.round(tiempoActual);
            recyclerView.scrollToPosition(segundoActual);
        }
    }

    private void guardarEstadoEnSegundoActual() {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null || activity.renderer == null || activity.nombreProyecto == null || crearAnimaciones == null) {
            return;
        }
        ar.axt.animar.CrearAnimaciones.Keyframe kf = new ar.axt.animar.CrearAnimaciones.Keyframe();
        kf.segundo = segundoActual;
        if (activity.renderer.meshes != null) {
            for (MyRenderer.Mesh m : activity.renderer.meshes) {
                if (m == null) continue;
                ar.axt.animar.CrearAnimaciones.MeshFrameState ms = new ar.axt.animar.CrearAnimaciones.MeshFrameState();
                ms.name = m.name;
                System.arraycopy(m.translation, 0, ms.pos, 0, 3);
                System.arraycopy(m.rotation, 0, ms.rot, 0, 3);
                System.arraycopy(m.scale, 0, ms.scale, 0, 3);
                if (m.subMesh != null && m.subMesh.vertexBuffer != null) {
                    java.nio.FloatBuffer vb = m.subMesh.vertexBuffer.duplicate();
                    vb.position(0);
                    float[] vData = new float[vb.capacity()];
                    vb.get(vData);
                    ms.vertexData = vData;
                } kf.meshes.add(ms);
            }
        }
        if (activity.renderer.material != null && activity.renderer.material.getBoxes() != null) {
            for (ar.axt.materiales.Material.EffectBox box : activity.renderer.material.getBoxes()) {
                if (box == null) continue;
                ar.axt.animar.CrearAnimaciones.BoxFrameState bs = new ar.axt.animar.CrearAnimaciones.BoxFrameState();
                bs.id = box.id;
                System.arraycopy(box.position, 0, bs.pos, 0, 3);
                System.arraycopy(box.rotation, 0, bs.rot, 0, 3);
                bs.scale = box.scale;
                kf.boxes.add(bs);
            }
        }
        if (activity.renderer.getBones() != null) {
            for (ar.axt.animar.Bones.Bone bone : activity.renderer.getBones().getAllBones()) {
                if (bone == null) continue;
                ar.axt.animar.CrearAnimaciones.BoneFrameState bns = new ar.axt.animar.CrearAnimaciones.BoneFrameState();
                bns.id = bone.id;
                System.arraycopy(bone.position, 0, bns.pos, 0, 3);
                System.arraycopy(bone.rotation, 0, bns.rot, 0, 3);
                System.arraycopy(bone.scale, 0, bns.scale, 0, 3);
                kf.bones.add(bns);
            }
        }
        if (activity.renderer.administradorCamaras != null && activity.renderer.administradorCamaras.selectedCamara != null) {
            ar.axt.controles.AjustesDeCamara adj = activity.renderer.ajustesDeCamara;
            ar.axt.animar.CrearAnimaciones.CameraFrameState cs = new ar.axt.animar.CrearAnimaciones.CameraFrameState();
            cs.markerId = activity.renderer.administradorCamaras.selectedCamara.id;
            cs.centerX = adj.centerX;
            cs.centerY = adj.centerY;
            cs.centerZ = adj.centerZ;
            cs.angleX = adj.angleX;
            cs.angleY = adj.angleY;
            cs.angleRoll = adj.angleRoll;
            cs.distance = adj.distance;
            kf.camera = cs;
        }
        ar.axt.sombras.RealShadow real = activity.renderer.getShaderRealista();
        ar.axt.sombras.ShadowAnime anime = activity.renderer.getShaderAnime();
        if (real != null) {
            ar.axt.animar.CrearAnimaciones.LightFrameState ls = new ar.axt.animar.CrearAnimaciones.LightFrameState();
            System.arraycopy(real.lightPos, 0, ls.pos, 0, 3);
            ls.intensity = real.lightIntensity;
            ls.shadowStrength = real.shadowStrength;
            System.arraycopy(real.lightColor, 0, ls.lightColor, 0, 3);
            System.arraycopy(real.shadowColor, 0, ls.shadowColor, 0, 3);
            kf.light = ls;
        } else if (anime != null) {
            ar.axt.animar.CrearAnimaciones.LightFrameState ls = new ar.axt.animar.CrearAnimaciones.LightFrameState();
            System.arraycopy(anime.lightPos, 0, ls.pos, 0, 3);
            ls.intensity = anime.lightIntensity;
            ls.shadowStrength = anime.shadowStrength;
            System.arraycopy(anime.lightColor, 0, ls.lightColor, 0, 3);
            System.arraycopy(anime.shadowColor, 0, ls.shadowColor, 0, 3);
            kf.light = ls;
        }
        if (activity.renderer != null) {
            ar.axt.animar.CrearAnimaciones.SkyFrameState ss = new ar.axt.animar.CrearAnimaciones.SkyFrameState();
            System.arraycopy(activity.renderer.skyColor, 0, ss.skyColor, 0, 3);
            ss.cloudDensity = activity.renderer.cloudDensity;
            ss.starDensity = activity.renderer.starDensity;
            kf.sky = ss;
        }
        cacheKeyframes.put(segundoActual, kf);
        adapter.agregarSegundoAnimado(segundoActual);
        crearAnimaciones.guardarKeyframesAsync(activity.nombreProyecto, cacheKeyframes);
    }

    private void eliminarEstadoEnSegundoActual() {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null || activity.nombreProyecto == null || crearAnimaciones == null) {
            return;
        }
        if (cacheKeyframes.containsKey(segundoActual)) {
            cacheKeyframes.remove(segundoActual);
            adapter.removerSegundoAnimado(segundoActual);
            crearAnimaciones.guardarKeyframesAsync(activity.nombreProyecto, cacheKeyframes);
        }
    }

}
