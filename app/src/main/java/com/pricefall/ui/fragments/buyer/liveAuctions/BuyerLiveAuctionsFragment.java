package com.pricefall.ui.fragments.buyer.liveAuctions;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.pricefall.pojo.Auction.LIVE;
import static com.pricefall.pojo.Auction.UPCOMING;
import static com.pricefall.ui.activities.MainActivity.AuctionId;
import static com.pricefall.ui.activities.MainActivity.formatPrice;
import static com.pricefall.ui.activities.MainActivity.pxToDp;
import static com.pricefall.ui.activities.MainActivity.shareAuction;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.pricefall.R;
import com.pricefall.adapters.CircleAuctionAdapter;
import com.pricefall.adapters.ImagesAdapter;
import com.pricefall.databinding.DialogSetPriceTargetBinding;
import com.pricefall.databinding.FragmentBuyerLiveAuctionsBinding;
import com.pricefall.models.AlarmReceiver;
import com.pricefall.models.LinePagerIndicatorDecoration;
import com.pricefall.models.RecyclerViewTouchListener;
import com.pricefall.pojo.Auction;
import com.pricefall.pojo.Payment;
import com.pricefall.pojo.User;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class BuyerLiveAuctionsFragment extends Fragment {
    FragmentBuyerLiveAuctionsBinding binding;

    FirebaseUser firebaseUser;
    FirebaseDatabase database;
    DatabaseReference usersRef, auctionsRef, paymentsRef;
    ArrayList<Auction> auctions = new ArrayList<>();

    CircleAuctionAdapter auctionAdapter;
    LinearLayoutManager layoutManager;


    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentBuyerLiveAuctionsBinding.inflate(inflater, container, false);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("Users");
        auctionsRef = database.getReference("Auctions");
        paymentsRef = database.getReference("Payments");


        auctionAdapter = new CircleAuctionAdapter(requireContext());
        auctionAdapter.setModels(auctions);
        binding.auctionsRecycler.setAdapter(auctionAdapter);
        layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        binding.auctionsRecycler.setLayoutManager(layoutManager);
        binding.auctionsRecycler.addOnItemTouchListener(new RecyclerViewTouchListener(requireContext(), binding.auctionsRecycler, new RecyclerViewTouchListener.RecyclerViewClickListener() {
            @Override
            public void onClick(View view, int position) {
                if (auctionAdapter.selected != -1)
                    auctionAdapter.notifyItemChanged(auctionAdapter.selected);
                auctionAdapter.selected = position;
                auctionAdapter.notifyItemChanged(position);

                selectedAuction = auctions.get(position);
                binding.auctionLayout.setVisibility(VISIBLE);
                openAuction();
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));

        getClients();


        binding.notifyMe.setOnClickListener(v -> {
            binding.notifyMe.setTextColor(Color.WHITE);
            binding.notifyMe.setIcon(null);
            binding.notifyMeImg.setVisibility(View.VISIBLE);
            binding.notifyMe.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            Glide.with(requireContext())
                    .load(R.drawable.ic_notification_animation)
                    .into(binding.notifyMeImg);
            //TO DO Make notification when auction start
            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;

            int requestCode = selectedAuction.getIdAsInt();
            long triggerAtMillis = selectedAuction.startTime;

            Intent intent = new Intent(requireContext(), AlarmReceiver.class);
            intent.putExtra("title", selectedAuction.title);
            intent.putExtra("body", "Auction starts in 1 minute");
            intent.putExtra("id", selectedAuction.id);

// Try to get any existing PendingIntent
            PendingIntent existingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );

            if (existingIntent != null) {
                // Cancel the existing alarm
                Toast.makeText(requireContext(), "Reminder already set, you will be notified", Toast.LENGTH_SHORT).show();
//                alarmManager.cancel(existingIntent);
//                Toast.makeText(requireContext(), "Reminder canceled", Toast.LENGTH_SHORT).show();
//                Log.w("AlarmManager", "Canceled alarm for auction id: " + selectedAuction.id);
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
                        requireContext(),
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
                    Log.e("AlarmManager", "Failed to schedule exact alarm", e);
                    alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            newIntent
                    );
                }

                Toast.makeText(requireContext(), "You will be notified", Toast.LENGTH_SHORT).show();
                Log.w("AlarmManager", "Scheduled alarm for auction id: " + selectedAuction.id);
            }
            new Handler().postDelayed(() -> {
                binding.notifyMeImg.setVisibility(GONE);
                binding.notifyMe.setTextColor(Color.BLACK);
                binding.notifyMe.setIconResource(R.drawable.ic_notification);
                binding.notifyMe.setText("You will be notified");
                binding.notifyMe.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#A3FFA7")));
            }, 1000);
        });

        binding.setPriceTarget.setOnClickListener(v -> {
            final int[] price = {(int) (selectedAuction.minimumPrice + ((selectedAuction.startingPrice - selectedAuction.minimumPrice) / 2))};
            DialogSetPriceTargetBinding dialogBinding = DialogSetPriceTargetBinding.bind(requireActivity().getLayoutInflater().inflate(R.layout.dialog_set_price_target, null));
            AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(dialogBinding.getRoot()).create();
            dialogBinding.price.setText(price[0] + " KD");
            dialogBinding.plus.setOnClickListener(v2 -> {
                if (price[0] + 1 < selectedAuction.startingPrice) {
                    price[0]++;
                    dialogBinding.price.setText(price[0] + " KD");
                }
            });
            dialogBinding.minus.setOnClickListener(v2 -> {
                if (price[0] - 1 > selectedAuction.minimumPrice) {
                    price[0]--;
                    dialogBinding.price.setText(price[0] + " KD");
                }
            });

            dialogBinding.notifyMe.setOnClickListener(v2 -> {
                //TO DO Make notification when auction start
                AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
                if (alarmManager == null) return;

                int requestCode = new Random().nextInt();
                long triggerAtMillis = selectedAuction.getTimeByPrice(price[0]);

                Intent intent = new Intent(requireContext(), AlarmReceiver.class);
                intent.putExtra("title", selectedAuction.title);
                intent.putExtra("body", "Auction reaches your price target " + price[0] + " KD");
                intent.putExtra("id", selectedAuction.id);

// Try to get any existing PendingIntent
                PendingIntent existingIntent = PendingIntent.getBroadcast(
                        requireContext(),
                        requestCode,
                        intent,
                        PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
                );

                if (existingIntent != null) {
                    // Cancel the existing alarm
                    Toast.makeText(requireContext(), "Reminder already set, you will be notified when auction reaches your price target.", Toast.LENGTH_SHORT).show();
//                alarmManager.cancel(existingIntent);
//                Toast.makeText(requireContext(), "Reminder canceled", Toast.LENGTH_SHORT).show();
//                Log.w("AlarmManager", "Canceled alarm for auction id: " + selectedAuction.id);
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
                            requireContext(),
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

                    Toast.makeText(requireContext(), "You will be notified when auction reaches your price target.", Toast.LENGTH_SHORT).show();
                }
                Log.w("AlarmManager", price[0] + ", at " + new SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(triggerAtMillis));
                dialog.dismiss();
            });

            dialog.show();
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        });

        binding.buy.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("item", new Gson().toJson(selectedAuction));
            Navigation.findNavController(requireActivity(), R.id.nav_buyer_host_fragment)
                    .navigate(R.id.buyerBuyAuctionFragment, bundle);
            selectedAuction = null;
        });

        binding.shareAuction.setOnClickListener(v -> shareAuction(selectedAuction.id, requireActivity()));

        binding.viewMore.setOnClickListener(v -> binding.description.setVisibility(binding.description.getVisibility() == GONE ? View.VISIBLE : GONE));
        return binding.getRoot();
    }


    Auction selectedAuction = null;

    @SuppressLint("SetTextI18n")
    void openAuction() {
        binding.title.setText(selectedAuction.title);
        binding.description.setText(selectedAuction.description);
        binding.date.setText(new SimpleDateFormat("dd MMMM", Locale.getDefault()).format(selectedAuction.startTime));
        binding.time.setText(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(selectedAuction.startTime));
        binding.startingPrice.setText("Starting Price: KD " + selectedAuction.startingPrice);
        binding.startingPrice2.setText("Starting Price: KD " + selectedAuction.startingPrice);
        Intent intent = new Intent(requireContext(), AlarmReceiver.class);
        intent.putExtra("title", selectedAuction.title);
        intent.putExtra("body", "Auction starts in 1 minute");
        intent.putExtra("id", selectedAuction.id);

// Try to retrieve existing PendingIntent WITHOUT creating a new one
        PendingIntent existingIntent = PendingIntent.getBroadcast(
                requireContext(),
                selectedAuction.getIdAsInt(),
                intent,
                PendingIntent.FLAG_NO_CREATE  // 👈 This is the key!
                        | PendingIntent.FLAG_IMMUTABLE // ✅ both flags
        );
        if (existingIntent != null) {
            binding.notifyMe.setText("You will be notified");
            binding.notifyMe.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#A3FFA7")));
        } else {
            binding.notifyMe.setText("Notify Me");
            binding.notifyMe.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
        }

        initImageSlider();
    }

    public void initImageSlider() {
        ImagesAdapter imagesAdapter = new ImagesAdapter(requireContext());
        imagesAdapter.setModels(selectedAuction.images);
        binding.imagesRecycler.setAdapter(imagesAdapter);
        binding.imagesRecycler.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

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

    boolean isShared = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            // Notify adapter to update visible countdowns
            checkItems();
            if (selectedAuction == null) {
                if (auctions.isEmpty()) {
                    handler.postDelayed(this, 1000); // Run again after 1 second
                    return;
                }
                selectedAuction = AuctionId != null ? allAuctions.stream().filter(a -> a.id.equals(AuctionId)).findFirst().orElse(auctions.get(0)) : auctions.get(0);
                binding.auctionLayout.setVisibility(VISIBLE);
                openAuction();
                handler.postDelayed(this, 1000); // Run again after 1 second
                return;
            }
            long currentTime = System.currentTimeMillis();
            long startTime = selectedAuction.startTime;
            long endTime = startTime + selectedAuction.durationInMillis;
            // Auction hasn't started yet
            Log.w("Running", "" + selectedAuction + "," + (currentTime < startTime) + "," + (endTime >= currentTime));
            if (currentTime < startTime) {
                binding.timer.setVisibility(VISIBLE);
                binding.dateTime.setVisibility(VISIBLE);
                binding.startingPrice.setVisibility(VISIBLE);
                binding.layout1.setVisibility(GONE);
                binding.layout2.setVisibility(GONE);
                binding.divider.setVisibility(GONE);
                binding.buy.setVisibility(GONE);
                binding.notifyMe.setVisibility(VISIBLE);

                long timeLeft = (startTime - currentTime) / 1000;
                long d = timeLeft / 86400;
                long h = (timeLeft % 86400) / 3600;
                long m = (timeLeft % 3600) / 60;
                long s = timeLeft % 60;
                String format = d > 0 ? String.format(Locale.getDefault(), "%02d:%02d:%02d:%02d", d, h, m, s) :
                        h > 0 ? String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s) :
                                String.format(Locale.getDefault(), "%02d:%02d", m, s);
                binding.timer.setText(format);

            }
            // Auction is live
            else if (endTime >= currentTime) {
                Log.w("LIVE", "AUCTION IS LIVE");
                binding.timer.setVisibility(GONE);
                binding.dateTime.setVisibility(GONE);
                binding.startingPrice.setVisibility(GONE);
                binding.layout1.setVisibility(VISIBLE);
                binding.layout2.setVisibility(VISIBLE);
                binding.divider.setVisibility(VISIBLE);
                binding.buy.setVisibility(VISIBLE);
                binding.notifyMe.setVisibility(GONE);
                double currentPrice = selectedAuction.getCurrentAuctionPrice(currentTime);
                binding.price.setText(new DecimalFormat("#.###").format(currentPrice));
                binding.savedPrice.setText(new DecimalFormat("#.###").format(selectedAuction.startingPrice - currentPrice));
            }
            // Auction has ended
            else {
                Log.w("ENDED", "AUCTION HAS ENDED");
                if (AuctionId != null) {
                    Toast.makeText(requireContext(), "This auction has ended", Toast.LENGTH_SHORT).show();
                    AuctionId = null;
                    isShared = true;
                }
                if (isShared) {
                    binding.timer.setVisibility(GONE);
                    binding.dateTime.setVisibility(VISIBLE);
                    binding.startingPrice.setVisibility(VISIBLE);
                    binding.description.setVisibility(VISIBLE);
                    binding.layout1.setVisibility(GONE);
                    binding.layout2.setVisibility(GONE);
                    binding.divider.setVisibility(GONE);
                    binding.buy.setVisibility(GONE);
                    binding.notifyMe.setVisibility(GONE);
                    binding.setPriceTarget.setVisibility(GONE);
                } else {
                    selectedAuction = null;
                }
            }

            handler.postDelayed(this, 1000); // Run again after 1 second
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        handler.post(tickRunnable); // Start the timer
    }

    @Override
    public void onStop() {
        super.onStop();
        handler.removeCallbacks(tickRunnable); // Stop when view is gone
    }

    void checkItems() {
        int firstCompletelyVisibleItemPosition = layoutManager.findFirstCompletelyVisibleItemPosition();
        binding.startArrow.setVisibility(firstCompletelyVisibleItemPosition != 0 &&!auctions.isEmpty()? View.VISIBLE : GONE);

        int lastCompletelyVisibleItemPosition = layoutManager.findLastCompletelyVisibleItemPosition();
        binding.endArrow.setVisibility(lastCompletelyVisibleItemPosition != auctions.size() - 1 &&!auctions.isEmpty()? View.VISIBLE : GONE);

        int size = auctions.size();
        for (Auction auction : allAuctions) {
            if (auctions.contains(auction) && (auction.getAuctionStatus(System.currentTimeMillis()) == LIVE || (auction.getAuctionStatus(System.currentTimeMillis()) == UPCOMING && isAuctionToday(auction.startTime))))
                continue;
            if (auctions.contains(auction) && !(auction.getAuctionStatus(System.currentTimeMillis()) == LIVE || (auction.getAuctionStatus(System.currentTimeMillis()) == UPCOMING && isAuctionToday(auction.startTime))))
                auctions.remove(auction);
            if (!auctions.contains(auction) && (auction.getAuctionStatus(System.currentTimeMillis()) == LIVE || (auction.getAuctionStatus(System.currentTimeMillis()) == UPCOMING && isAuctionToday(auction.startTime))))
                auctions.add(auction);
        }

        binding.emptyStates.setVisibility(auctions.isEmpty() && isAuctionsLoaded ? View.VISIBLE : View.GONE);

        if (size == auctions.size()) return;
        auctions.sort(Comparator.comparing(s -> s.startTime));

        Log.w("Itejms", "" + auctions.size());
        binding.emptyStates.setVisibility(auctions.isEmpty() && isAuctionsLoaded ? View.VISIBLE : View.GONE);
        auctionAdapter.setModels(auctions);
        auctionAdapter.notifyDataSetChanged();
    }

    boolean isAuctionToday(long startTime) {
        long timeLeft = (startTime - System.currentTimeMillis()) / 1000;
        long h = timeLeft / 3600;
        return h < 24;
    }

    Map<String, User> usersMap = new HashMap<>();

    private void getClients() {
        // Read from the database
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                usersMap = new HashMap<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    User item = dataSnapshot.getValue(User.class);
                    assert item != null;
                    usersMap.put(item.id, item);
                }

                getAuction();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    ArrayList<Auction> allAuctions = new ArrayList<>();
    boolean isAuctionsLoaded = false;
    private void getAuction() {
        // Read from the database
        auctionsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                auctions = new ArrayList<>();
                allAuctions = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Auction item = dataSnapshot.getValue(Auction.class);
                    assert item != null;
                    item.seller = usersMap.get(item.sellerId);
                    allAuctions.add(item);
                }
                isAuctionsLoaded = true;
                checkItems();
                getPayments();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getPayments() {
        // Read from the database
        paymentsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Payment payment = snapshot.getValue(Payment.class);
                if (payment != null && selectedAuction != null && payment.auctionId.equals(selectedAuction.id)) {
                    if (TimeUnit.MILLISECONDS.toMinutes(payment.date - System.currentTimeMillis()) < 2) {
                        showSnackBar(usersMap.get(payment.userId).name + " Bought " + (payment.quantity == 1 ? " " : payment.quantity + " ") + "at " + formatPrice(payment.price,true));
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    void showSnackBar(String message) {
        try {
            Snackbar snackbar = Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG);
            snackbar.setBackgroundTint(Color.WHITE);
            snackbar.setTextColor(Color.BLACK);
            View snackBarView = snackbar.getView();
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackBarView.getLayoutParams();
            params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            snackBarView.setLayoutParams(params);
            snackbar.show();
        } catch (Exception ignored) {
        }
    }

}
