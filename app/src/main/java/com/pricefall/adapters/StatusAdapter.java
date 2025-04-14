package com.pricefall.adapters;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.pricefall.R;
import com.pricefall.databinding.OneCircleAuctionItemBinding;
import com.pricefall.databinding.OneStatusItemBinding;
import com.pricefall.pojo.Auction;
import com.pricefall.pojo.Payment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class StatusAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<Payment.Status> Models;

    final private Context context;

    public StatusAdapter(Context context) {
        this.context = context;
    }

    public void setModels(ArrayList<Payment.Status> models) {
        Models = models;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Get the screen width in pixels
        int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels-80;

        // Calculate 70% of the screen width
        int itemWidth = (int) (screenWidth * 0.7); // 70% of the screen width

        // Inflate the item layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.one_status_item, parent, false);

        // Set the width of the item to 70% of the screen width
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = itemWidth;  // Set the calculated width
        view.setLayoutParams(params);

        return new AdapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final AdapterViewHolder ViewHolder = (AdapterViewHolder) holder;
        ViewHolder.binding.title.setText(Models.get(position).status);
        ViewHolder.binding.date.setText(new SimpleDateFormat("dd MMMM 'at' hh:mm a", Locale.getDefault()).format(Models.get(position).date));
    }

    @Override
    public int getItemCount() {
        return Models.size();
    }

    static class AdapterViewHolder extends RecyclerView.ViewHolder {
        OneStatusItemBinding binding;

        public AdapterViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = OneStatusItemBinding.bind(itemView);
        }
    }

}
