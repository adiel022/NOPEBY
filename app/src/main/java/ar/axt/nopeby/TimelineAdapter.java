package ar.axt.nopeby;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.HashSet;
import java.util.Set;

public class TimelineAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_MAJOR = 1;

    private final Set<Integer> segundosConAnimacion = new HashSet<>();

    public void setSegundosConAnimacion(Set<Integer> segundos) {
        this.segundosConAnimacion.clear();
        if (segundos != null) {
            this.segundosConAnimacion.addAll(segundos);
        } notifyDataSetChanged();
    }

    public void agregarSegundoAnimado(int segundo) {
        segundosConAnimacion.add(segundo);
        notifyItemChanged(segundo);
    }

    public void removerSegundoAnimado(int segundo) {
        segundosConAnimacion.remove(segundo);
        notifyItemChanged(segundo);
    }

    @Override
    public int getItemViewType(int position) {
        if (position % 10 == 0) { return TYPE_MAJOR; }
        return TYPE_NORMAL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_MAJOR) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tiempo_major, parent, false);
            return new MajorViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tiempo, parent, false);
            return new NormalViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        boolean tieneAnimacion = segundosConAnimacion.contains(position);
        if (holder instanceof MajorViewHolder) {
            MajorViewHolder major = (MajorViewHolder) holder;
            major.textSegundo.setText(String.valueOf(position));
            if (major.marcaVerde != null) {
                major.marcaVerde.setVisibility(tieneAnimacion ? View.VISIBLE : View.GONE);
            }
        } else if (holder instanceof NormalViewHolder) {
            NormalViewHolder normal = (NormalViewHolder) holder;
            if (normal.marcaVerde != null) {
                normal.marcaVerde.setVisibility(tieneAnimacion ? View.VISIBLE : View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return Integer.MAX_VALUE;
    }

    static class NormalViewHolder extends RecyclerView.ViewHolder {
        View marcaVerde;
        NormalViewHolder(View itemView) {
            super(itemView);
            marcaVerde = itemView.findViewById(R.id.marca_verde);
        }
    }

    static class MajorViewHolder extends RecyclerView.ViewHolder {
        TextView textSegundo;
        View marcaVerde;
        MajorViewHolder(View itemView) {
            super(itemView);
            textSegundo = itemView.findViewById(R.id.text_segundo);
            marcaVerde = itemView.findViewById(R.id.marca_verde);
        }
    }
}
