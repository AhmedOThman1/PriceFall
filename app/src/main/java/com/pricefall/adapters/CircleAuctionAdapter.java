package com.pricefall.adapters;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
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
import com.pricefall.pojo.Auction;

import java.util.ArrayList;

public class CircleAuctionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<Auction> Models;

    final private Context context;

   public int selected=-1;

    public CircleAuctionAdapter(Context context) {
        this.context = context;
    }

    public void setModels(ArrayList<Auction> models) {
        Models = models;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.one_circle_auction_item, parent, false);
        return new AdapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final AdapterViewHolder ViewHolder = (AdapterViewHolder) holder;
        Glide.with(context)
                .load(Models.get(position).images.get(0))
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .into(ViewHolder.binding.auctionImg);

        if (selected == position) {
            ViewHolder.binding.circleBorder.setIndicatorColor(Color.parseColor("#009688"));
        } else {
            ViewHolder.binding.circleBorder.setIndicatorColor(context.getColor(R.color.colorPrimary));
        }

        if(selected ==position || selected==-1) {
            ObjectAnimator anim = ObjectAnimator.ofInt(ViewHolder.binding.circleBorder, "progress", 0, 100);
            anim.setDuration(1000); // 1 second
            anim.setInterpolator(new AccelerateDecelerateInterpolator());
            anim.start();
        }else{
            ViewHolder.binding.circleBorder.setProgress(100);
        }
    }

    @Override
    public int getItemCount() {
        return Models.size();
    }

    static class AdapterViewHolder extends RecyclerView.ViewHolder {
        OneCircleAuctionItemBinding binding;

        public AdapterViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = OneCircleAuctionItemBinding.bind(itemView);
        }
    }

}
