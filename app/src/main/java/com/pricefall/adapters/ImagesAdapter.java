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
import com.pricefall.databinding.OneAuctionItemBinding;
import com.pricefall.databinding.OneImageItemBinding;
import com.pricefall.pojo.Auction;

import java.util.ArrayList;

public class ImagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<String> Models;

    final private Context context;

    public ImagesAdapter(Context context) {
        this.context = context;
    }

    public void setModels(ArrayList<String> models) {
        Models = models;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.one_image_item, parent, false);
        return new AdapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final AdapterViewHolder ViewHolder = (AdapterViewHolder) holder;
        Glide.with(context)
                .load(Models.get(position))
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .into(ViewHolder.binding.getRoot());
    }

    @Override
    public int getItemCount() {
        return Models.size();
    }

    static class AdapterViewHolder extends RecyclerView.ViewHolder {
        OneImageItemBinding binding;

        public AdapterViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = OneImageItemBinding.bind(itemView);
        }
    }

}
