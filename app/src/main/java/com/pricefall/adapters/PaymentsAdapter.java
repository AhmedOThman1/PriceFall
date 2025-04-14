package com.pricefall.adapters;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.pricefall.ui.activities.MainActivity.formatPrice;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.pricefall.R;
import com.pricefall.databinding.DialogUpdateStatusBinding;
import com.pricefall.databinding.OnePaymentItemBinding;
import com.pricefall.models.LinePagerIndicatorDecoration;
import com.pricefall.models.RecyclerViewTouchListener;
import com.pricefall.pojo.Payment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PaymentsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<Payment> Models;

    final private Context context;
    private boolean isSeller;

    public PaymentsAdapter(Context context) {
        this.context = context;
    }

    public void setSeller(boolean seller) {
        isSeller = seller;
    }

    public void setModels(ArrayList<Payment> models) {
        Models = models;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.one_payment_item, parent, false);
        return new AuctionViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final AuctionViewHolder ViewHolder = (AuctionViewHolder) holder;
        Glide.with(context)
                .load(Models.get(position).user.photo)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(ViewHolder.binding.userImage);

        ViewHolder.binding.userName.setText(Models.get(position).user.name);


        long days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - Models.get(position).date);
        long hours = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - Models.get(position).date);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - Models.get(position).date);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(Models.get(position).date);
        String temp = days < 1 ? (hours < 1 ?
                (minutes < 1 ? "Just now" : minutes + " minutes ago") : hours + " hours ago") :
                days == 1 ? "Yesterday" :
                        days < 5 ? days + " days ago" :
                                days < 300 ?
                                        new SimpleDateFormat("d MMMM", Locale.getDefault()).format(calendar.getTimeInMillis())
                                        : new SimpleDateFormat("d MMM yy", Locale.getDefault()).format(calendar.getTimeInMillis());
        ViewHolder.binding.notificationDate.setText(temp);

        ViewHolder.binding.notificationBody.setText(
                "Paid " + formatPrice(Models.get(position).totalAmount, true) + " for " +
                        Models.get(position).quantity +
                        " “" + Models.get(position).auction.title + "” items");
        ViewHolder.binding.auctionLayout.setVisibility(GONE);

        ViewHolder.binding.getRoot().setOnClickListener(v -> {
            ViewHolder.binding.auctionLayout.setVisibility(ViewHolder.binding.auctionLayout.getVisibility() == GONE ? VISIBLE : GONE);
            ViewHolder.binding.statusRecycler.setVisibility(GONE);
        });
        ViewHolder.binding.getRoot().setOnLongClickListener(v -> {
            if (!isSeller) return false;
            PopupMenu popupMenu = new PopupMenu(context, v);
            popupMenu.getMenu().add("Update Status");
            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getTitle().equals("Update Status")) {
                    showAddStatus(position, -1);
                }
                return false;
            });
            popupMenu.show();
            return false;
        });
        ViewHolder.binding.showStatus.setOnClickListener(v -> {
            ViewHolder.binding.statusRecycler.setVisibility(ViewHolder.binding.statusRecycler.getVisibility() == GONE ? VISIBLE : GONE);
        });

        ViewHolder.initImageSlider(Models.get(position).auction.images, context);

        ViewHolder.binding.price.setText(formatPrice(Models.get(position).price, false));
        ViewHolder.binding.quantity.setText(Models.get(position).quantity + "X");
        ViewHolder.binding.totalAmount.setText(formatPrice(Models.get(position).totalAmount, false));
        ViewHolder.binding.oldPrice.setText(formatPrice(Models.get(position).auction.startingPrice, false));
        ViewHolder.binding.oldPrice.setPaintFlags(ViewHolder.binding.oldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        ViewHolder.initStatusItems(position, context);
    }

    @Override
    public int getItemCount() {
        return Models.size();
    }

    public void showAddStatus(int paymentPosition, int statusPosition) {
        Payment paymentItem = Models.get(paymentPosition);
        Payment.Status editItem = statusPosition == -1 ? null : paymentItem.status.get(statusPosition);
        //to do add and edit status
        DialogUpdateStatusBinding dialogBinding = DialogUpdateStatusBinding.bind(((Activity) context).getLayoutInflater().inflate(R.layout.dialog_update_status, null));
        AlertDialog dialog = new AlertDialog.Builder(context).setView(dialogBinding.getRoot()).create();
        if (editItem != null)
            dialogBinding.status.setText(editItem.status);

        dialogBinding.update.setOnClickListener(v -> {
            if (editItem == null) {
                Payment.Status status = new Payment.Status();
                status.date = System.currentTimeMillis();
                status.status = dialogBinding.status.getText().toString();
                paymentItem.status.add(status);
            } else {
                editItem.status = dialogBinding.status.getText().toString();
                paymentItem.status.set(statusPosition, editItem);
            }
            DatabaseReference paymentsRef = FirebaseDatabase.getInstance().getReference("Payments").child(Models.get(paymentPosition).id);
            paymentsRef.child("status").setValue(paymentItem.status);
            dialog.dismiss();
        });

        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    public class AuctionViewHolder extends RecyclerView.ViewHolder {
        OnePaymentItemBinding binding;

        public AuctionViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = OnePaymentItemBinding.bind(itemView);
        }

        Map<Integer, RecyclerViewTouchListener> touchListenerMap = new HashMap<>();

        public void initStatusItems(int paymentPosition, Context context) {
            Payment paymentItem = Models.get(paymentPosition);
            paymentItem.status.sort(Comparator.comparing(s -> s.date, Comparator.reverseOrder()));
            StatusAdapter statusAdapter = new StatusAdapter(context);
            statusAdapter.setModels(paymentItem.status);
            binding.statusRecycler.setAdapter(statusAdapter);
            binding.statusRecycler.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
            if (touchListenerMap.get(paymentPosition) != null)
                binding.statusRecycler.removeOnItemTouchListener(touchListenerMap.get(paymentPosition));
            RecyclerViewTouchListener itemTouchListener = new RecyclerViewTouchListener(context, binding.statusRecycler, new RecyclerViewTouchListener.RecyclerViewClickListener() {
                @Override
                public void onClick(View view, int position) {

                }

                @Override
                public void onLongClick(View v, int statusPosition) {
                    if (!isSeller) return;
                    PopupMenu popupMenu = new PopupMenu(context, v);
                    popupMenu.getMenu().add("Edit");
                    popupMenu.getMenu().add("Delete");
                    popupMenu.setOnMenuItemClickListener(item -> {
                        if (item.getTitle().equals("Edit")) {
                            showAddStatus(paymentPosition, statusPosition);
                        } else if (item.getTitle().equals("Delete")) {
                            paymentItem.status.remove(statusPosition);
                            Log.e("Delete", "Status Long Click" + statusPosition);
                            DatabaseReference paymentNotificationsRef = FirebaseDatabase.getInstance().getReference("Payments").child(paymentItem.id);
                            paymentNotificationsRef.child("status").setValue(paymentItem.status);
                        }
                        return false;
                    });
                    Log.e("ERROR HERE", "Status Long Click" + statusPosition);
                    popupMenu.show();
                }
            });
            touchListenerMap.put(paymentPosition, itemTouchListener);
            binding.statusRecycler.addOnItemTouchListener(itemTouchListener);
        }

        public void initImageSlider(ArrayList<String> images, Context context) {
            ImagesAdapter imagesAdapter = new ImagesAdapter(context);
            imagesAdapter.setModels(images);
            binding.imagesRecycler.setAdapter(imagesAdapter);
            binding.imagesRecycler.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));

            // Clear any existing SnapHelper or fling listener
            if (binding.imagesRecycler.getOnFlingListener() != null) {
                binding.imagesRecycler.setOnFlingListener(null);
            }

            // Attach SnapHelper just once
            PagerSnapHelper snapHelper = new PagerSnapHelper();
            snapHelper.attachToRecyclerView(binding.imagesRecycler);

            // Prevent duplicate decoration
            if (binding.imagesRecycler.getItemDecorationCount() == 0) {
                binding.imagesRecycler.addItemDecoration(new LinePagerIndicatorDecoration(
                        Color.parseColor("#FFFFFF"),         // Selected color
                        Color.parseColor("#BBBBBB"),         // Unselected color
                        4,                                   // Radius in dp
                        12,                                  // Unselected length in dp
                        36,                                  // Selected length in dp
                        8,                                    // Padding between indicators in dp
                        true                                  // Draw indicators on bottom
                ));
            }
        }
    }

}
