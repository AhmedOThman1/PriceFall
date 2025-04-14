package com.pricefall.adapters;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.pricefall.R;
import com.pricefall.databinding.DialogSetPriceTargetBinding;
import com.pricefall.databinding.OneAuctionItemBinding;
import com.pricefall.models.AlarmReceiver;
import com.pricefall.models.LinePagerIndicatorDecoration;
import com.pricefall.pojo.Auction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class AuctionsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<Auction> Models;

    final private Context context;
    final private boolean isSeller;

    public AuctionsAdapter(Context context, boolean isSeller) {
        this.context = context;
        this.isSeller = isSeller;
    }

    public void setModels(ArrayList<Auction> models) {
        Models = models;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.one_auction_item, parent, false);
        return new AuctionViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final AuctionViewHolder ViewHolder = (AuctionViewHolder) holder;

        ViewHolder.initImageSlider(Models.get(position), context);

        ViewHolder.binding.title.setText(Models.get(position).title);
        ViewHolder.binding.date.setText(new SimpleDateFormat("dd MMMM", Locale.getDefault()).format(Models.get(position).startTime));
        ViewHolder.binding.time.setText(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Models.get(position).startTime));

        long days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - Models.get(position).startTime);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(Models.get(position).startTime);
        Calendar today = Calendar.getInstance();
        boolean isToday = calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) && calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
        today.add(Calendar.DAY_OF_YEAR, 1);
        boolean isTomorrow = calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) && calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);

        ViewHolder.binding.day.setVisibility(Models.get(position).startTime - System.currentTimeMillis() > 0 || isToday ? VISIBLE : GONE);
        ViewHolder.binding.day.setText(isToday ? "Today" : isTomorrow ? "Tomorrow" : "In " + days + " days");
        ViewHolder.binding.startingPrice.setText(Models.get(position).startingPrice + "");
        ViewHolder.binding.startingPrice.setPaintFlags(ViewHolder.binding.startingPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        ViewHolder.binding.quantity.setVisibility(isSeller ? VISIBLE : GONE);
        ViewHolder.binding.timer.setVisibility(isSeller ? GONE : VISIBLE);
        ViewHolder.binding.quantity.setText(
                Models.get(position).quantitySold > 0 ? (Models.get(position).quantity - Models.get(position).quantitySold) + " Out Of " + Models.get(position).quantity + " Items" :
                        Models.get(position).quantity + " Items Available");

        ViewHolder.startTimer(Models.get(position));

        ViewHolder.binding.notifyMe.setVisibility(isSeller ? GONE : VISIBLE);
        ViewHolder.binding.setPriceTarget.setVisibility(isSeller ? GONE : VISIBLE);

        ViewHolder.binding.notifyMe.setOnClickListener(v -> {
            ViewHolder.binding.notifyMe.setTextColor(Color.WHITE);
            ViewHolder.binding.notifyMe.setIcon(null);
            ViewHolder.binding.notifyMeImg.setVisibility(View.VISIBLE);
            ViewHolder.binding.notifyMe.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            Glide.with(context)
                    .load(R.drawable.ic_notification_animation)
                    .into(ViewHolder.binding.notifyMeImg);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;

            int requestCode = Models.get(position).getIdAsInt();
            long triggerAtMillis = Models.get(position).startTime;

            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra("title", Models.get(position).title);
            intent.putExtra("body", "Auction starts in 1 minute");
            intent.putExtra("id", Models.get(position).id);

// Try to get any existing PendingIntent
            PendingIntent existingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );

            if (existingIntent != null) {
                // Cancel the existing alarm
                Toast.makeText(context, "Reminder already set, you will be notified", Toast.LENGTH_SHORT).show();
//                alarmManager.cancel(existingIntent);
//                Toast.makeText(context, "Reminder canceled", Toast.LENGTH_SHORT).show();
//                Log.w("AlarmManager", "Canceled alarm for auction id: " + Models.get(position).id);
//                new Handler().postDelayed(() -> {
//                    binding.notifyMeImg.setVisibility(GONE);
//                    binding.notifyMe.setTextColor(Color.BLACK);
//                    binding.notifyMe.setIconResource(R.drawable.ic_notification);
//                    binding.notifyMe.setText("Notify Me");
//                    binding.notifyMe.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
//                    }, 1000);
            } else {
                // Create a new PendingIntent to schedule the alarm
                PendingIntent newIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                );

                try {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            newIntent
                    );
                } catch (SecurityException e) {
                    alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            newIntent
                    );
                }

                Toast.makeText(context, "You will be notified", Toast.LENGTH_SHORT).show();
                Log.w("AlarmManager", "Scheduled alarm for auction id: " + Models.get(position).id);
            }
            new Handler().postDelayed(() -> {
                ViewHolder.binding.notifyMeImg.setVisibility(GONE);
                ViewHolder.binding.notifyMe.setTextColor(Color.BLACK);
                ViewHolder.binding.notifyMe.setIconResource(R.drawable.ic_notification);
                ViewHolder.binding.notifyMe.setText("You will be notified");
                ViewHolder.binding.notifyMe.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#A3FFA7")));
            }, 1000);
        });
        ViewHolder.binding.setPriceTarget.setOnClickListener(v -> {
            final int[] price = {(int) (Models.get(position).minimumPrice + ((Models.get(position).startingPrice - Models.get(position).minimumPrice) / 2))};
            DialogSetPriceTargetBinding dialogBinding = DialogSetPriceTargetBinding.bind(((Activity) context).getLayoutInflater().inflate(R.layout.dialog_set_price_target, null));
            AlertDialog dialog = new AlertDialog.Builder(context).setView(dialogBinding.getRoot()).create();
            dialogBinding.price.setText(price[0] + " KD");
            dialogBinding.plus.setOnClickListener(v2 -> {
                if (price[0] + 1 < Models.get(position).startingPrice) {
                    price[0]++;
                    dialogBinding.price.setText(price[0] + " KD");
                }
            });
            dialogBinding.minus.setOnClickListener(v2 -> {
                if (price[0] - 1 > Models.get(position).minimumPrice) {
                    price[0]--;
                    dialogBinding.price.setText(price[0] + " KD");
                }
            });

            dialogBinding.notifyMe.setOnClickListener(v2 -> {
                //TO DO Make notification when auction start
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (alarmManager == null) return;

                int requestCode = new Random().nextInt();
                long triggerAtMillis = Models.get(position).getTimeByPrice(price[0]);

                Intent intent = new Intent(context, AlarmReceiver.class);
                intent.putExtra("title", Models.get(position).title);
                intent.putExtra("body", "Auction reaches your price target " + price[0] + " KD");
                intent.putExtra("id", Models.get(position).id);

// Try to get any existing PendingIntent
                PendingIntent existingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
                );

                if (existingIntent != null) {
                    // Cancel the existing alarm
                    Toast.makeText(context, "Reminder already set, you will be notified when auction reaches your price target.", Toast.LENGTH_SHORT).show();
//                alarmManager.cancel(existingIntent);
//                Toast.makeText(context, "Reminder canceled", Toast.LENGTH_SHORT).show();
//                Log.w("AlarmManager", "Canceled alarm for auction id: " + Models.get(position).id);
//                new Handler().postDelayed(() -> {
//                    binding.notifyMeImg.setVisibility(GONE);
//                    binding.notifyMe.setTextColor(Color.BLACK);
//                    binding.notifyMe.setIconResource(R.drawable.ic_notification);
//                    binding.notifyMe.setText("Notify Me");
//                    binding.notifyMe.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
//                    }, 1000);
                } else {
                    // Create a new PendingIntent to schedule the alarm
                    PendingIntent newIntent = PendingIntent.getBroadcast(
                            context,
                            new Random().nextInt(),
                            intent,
                            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                    );

                    try {
                        alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                triggerAtMillis,
                                newIntent
                        );
                    } catch (SecurityException e) {
                        alarmManager.set(
                                AlarmManager.RTC_WAKEUP,
                                triggerAtMillis,
                                newIntent
                        );
                    }

                    Toast.makeText(context, "You will be notified when auction reaches your price target.", Toast.LENGTH_SHORT).show();
                }
                Log.w("AlarmManager", price[0] + ", at " + new SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(triggerAtMillis));
                dialog.dismiss();
            });

            dialog.show();
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        });

    }

    @Override
    public int getItemCount() {
        return Models.size();
    }

    public static class AuctionViewHolder extends RecyclerView.ViewHolder {
        OneAuctionItemBinding binding;

        public AuctionViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = OneAuctionItemBinding.bind(itemView);
        }

        public void startTimer(Auction item) {
            long currentTime = System.currentTimeMillis();
            long startTime = item.startTime;
            long endTime = startTime + item.durationInMillis;
            // Auction hasn't started yet
            if (currentTime < startTime) {
                binding.startingPrice.setVisibility(INVISIBLE);
                binding.startingPriceKd.setVisibility(INVISIBLE);
                binding.endsAfter.setVisibility(INVISIBLE);
                binding.dateTime.setVisibility(VISIBLE);
                binding.price.setText(item.startingPrice + "");

                long timeLeft = (startTime - currentTime) / 1000;
                long d = timeLeft / 86400;
                long h = (timeLeft % 86400) / 3600;
                long m = (timeLeft % 3600) / 60;
                long s = timeLeft % 60;
                String format = d > 0 ? String.format(Locale.getDefault(), "Starts after %02d:%02d:%02d:%02d", d, h, m, s) :
                        h > 0 ? String.format(Locale.getDefault(), "Starts after %02d:%02d:%02d", h, m, s) :
                                String.format(Locale.getDefault(), "Starts after %02d:%02d", m, s);
                binding.timer.setText(format);
                return;
            }

            // Auction already ended
            if (currentTime >= endTime) {
                binding.startingPrice.setVisibility(INVISIBLE);
                binding.startingPriceKd.setVisibility(INVISIBLE);
                binding.endsAfter.setVisibility(INVISIBLE);
                binding.dateTime.setVisibility(VISIBLE);
                binding.price.setText(item.startingPrice + "");
                return;
            }

            binding.startingPrice.setVisibility(VISIBLE);
            binding.startingPriceKd.setVisibility(VISIBLE);
            binding.endsAfter.setVisibility(VISIBLE);
            binding.dateTime.setVisibility(INVISIBLE);

            long timeLeft = (endTime - currentTime) / 1000;
            Log.w("Time Left", "" + (timeLeft));
            long h = timeLeft / 3600;
            long m = (timeLeft % 3600) / 60;
            long s = timeLeft % 60;
            String format = h > 0 ? String.format(Locale.getDefault(), "Ends after %02d:%02d:%02d", h, m, s) :
                    String.format(Locale.getDefault(), "Ends after %02d:%02d", m, s);
            binding.endsAfter.setText(format);
            String currentPrice = item.getCurrentAuctionPriceString(System.currentTimeMillis());
            Log.w("Current Price", new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(System.currentTimeMillis()) + ": " + currentPrice);
            binding.price.setText(currentPrice);

/*
            countDownTimer = new CountDownTimer(timeLeft, 1000) {
                @SuppressLint("SetTextI18n")
                @Override
                public void onTick(long millisUntilFinished) {
                    long seconds = millisUntilFinished / 1000;
                    long h = seconds / 3600;
                    long m = (seconds % 3600) / 60;
                    long s = seconds % 60;
                    String format = h>0? String.format(Locale.getDefault(),"Ends after %02d:%02d:%02d", h, m, s):
                             String.format(Locale.getDefault(),"Ends after %02d:%02d", m, s);
                    binding.endsAfter.setText(format);
                    binding.price.setAnimationDuration(2000L);
                    binding.price.setCharStrategy(Strategy.NormalAnimation());
                    binding.price.addCharOrder(CharOrder.Number);
                    binding.price.setAnimationInterpolator(new AccelerateDecelerateInterpolator());
                    String currentPrice = item.getCurrentAuctionPrice(System.currentTimeMillis()) + "";
                    Log.w("Current Price", new SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(System.currentTimeMillis())+": " + currentPrice);
                    binding.price.setText(currentPrice);
                }

                @Override
                public void onFinish() {
                    binding.endsAfter.setText("Ended");
                    binding.price.setText(item.startingPrice + "");
                }
            }.start();
*/
        }

        public void initImageSlider(Auction item, Context context) {
            ImagesAdapter imagesAdapter = new ImagesAdapter(context);
            imagesAdapter.setModels(item.images);
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
