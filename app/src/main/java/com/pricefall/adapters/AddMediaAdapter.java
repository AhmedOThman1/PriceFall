package com.pricefall.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.pricefall.R;
import com.pricefall.databinding.OneNewDocItemBinding;

import java.util.ArrayList;

public class AddMediaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private ArrayList<String> Models;

    public AddMediaAdapter(@NonNull Context context) {
        this.context = context;
    }

    public void setModels(ArrayList<String> models) {
        Models = models;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? 0 : 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewType == 1) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.one_new_doc_item, parent, false);
            return new ImageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.one_new_doc_add_item, parent, false);
            return new TempViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        if (holder.getItemViewType() == 1) {
            final ImageViewHolder ViewHolder = (ImageViewHolder) holder;
            Glide.with(context)
                    .load(Models.get(position - 1))
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .into(ViewHolder.binding.docImage);
        }
    }

    @Override
    public int getItemCount() {
        return Models.size() + 1;
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {

        OneNewDocItemBinding binding;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = OneNewDocItemBinding.bind(itemView);
        }
    }

    static class TempViewHolder extends RecyclerView.ViewHolder {
        // views
        public TempViewHolder(@NonNull View itemView) {
            super(itemView);
            // find view by id
        }
    }

}
