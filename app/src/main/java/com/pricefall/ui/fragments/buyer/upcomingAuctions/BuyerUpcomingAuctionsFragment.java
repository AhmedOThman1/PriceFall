package com.pricefall.ui.fragments.buyer.upcomingAuctions;

import static com.pricefall.pojo.Auction.UPCOMING;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.slider.RangeSlider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.pricefall.R;
import com.pricefall.adapters.AuctionsAdapter;
import com.pricefall.databinding.FragmentBuyerUpcomingAuctionsBinding;
import com.pricefall.databinding.FragmentSellerAuctionsTabBinding;
import com.pricefall.models.RecyclerViewTouchListener;
import com.pricefall.pojo.Auction;
import com.pricefall.pojo.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class BuyerUpcomingAuctionsFragment extends Fragment {
    FragmentBuyerUpcomingAuctionsBinding binding;

    FirebaseUser firebaseUser;
    FirebaseDatabase database;
    DatabaseReference usersRef, auctionsRef;
    ArrayList<Auction> auctions = new ArrayList<>();
    ArrayList<Auction> tempAuctions = new ArrayList<>();

    AuctionsAdapter auctionAdapter;
    LinearLayoutManager layoutManager;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentBuyerUpcomingAuctionsBinding.inflate(inflater, container, false);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("Users");
        auctionsRef = database.getReference("Auctions");


        auctionAdapter = new AuctionsAdapter(requireContext(), false);
        auctionAdapter.setModels(auctions);
        binding.auctionsRecycler.setAdapter(auctionAdapter);
        layoutManager = new LinearLayoutManager(requireContext());
        binding.auctionsRecycler.setLayoutManager(layoutManager);

        binding.search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase();
                filterAuctions();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        binding.searchIcon.setOnClickListener(v -> {
            binding.search.setVisibility(binding.search.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            binding.filter.setVisibility(binding.filter.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            binding.filterLayout.setVisibility(View.GONE);
        });

        binding.filter.setOnClickListener(v -> {
            binding.filterLayout.setVisibility(binding.filterLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        binding.priceRangeSlider.addOnSliderTouchListener(new RangeSlider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull RangeSlider slider) {
            }

            @Override
            public void onStopTrackingTouch(@NonNull RangeSlider slider) {
                Log.w("Range", slider.getValues().toString());
                startPrice = slider.getValues().get(0);
                endPrice = slider.getValues().get(1);
                filterAuctions();
            }
        });
        binding.priceRangeSlider.setValues(0f, 0f); // Set two values for two thumbs

        getClients();

        return binding.getRoot();
    }

    String searchQuery = "";
    String category = "";
    double startPrice = 0;
    double endPrice = 0;
    double maxPrice = 0;

    void filterAuctions() {
        auctions = new ArrayList<>();
        if (searchQuery.isEmpty() && category.isEmpty() && startPrice == 0 && endPrice == 0) {
            auctions = new ArrayList<>();
            auctions.addAll(tempAuctions);
        } else for (int i = 0; i < tempAuctions.size(); i++) {
            if (
                    (searchQuery.isEmpty() || (tempAuctions.get(i).title.toLowerCase().contains(searchQuery) ||
                            tempAuctions.get(i).description.toLowerCase().contains(searchQuery))) &&
                            (category.isEmpty() || category.equals("ALL") || tempAuctions.get(i).category.equals(category)) &&
                            (startPrice == 0 || tempAuctions.get(i).startingPrice >= startPrice) &&
                            (endPrice == 0 || tempAuctions.get(i).startingPrice <= endPrice)
            )
                auctions.add(tempAuctions.get(i));
        }


        binding.noResultDesign.noResultDesign.setVisibility(auctions.isEmpty() && !tempAuctions.isEmpty() ? View.VISIBLE : View.GONE);

        auctionAdapter.setModels(auctions);
        auctionAdapter.notifyDataSetChanged();
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            // Notify adapter to update visible countdowns
            checkItems();
            if (layoutManager != null) {
                int visibleStartPosition = layoutManager.findFirstVisibleItemPosition();
                int visibleEndPosition = layoutManager.findLastVisibleItemPosition();

                for (int position = visibleStartPosition; position <= visibleEndPosition; position++) {
                    RecyclerView.ViewHolder holder = binding.auctionsRecycler.findViewHolderForAdapterPosition(position);
                    if (holder != null && holder instanceof AuctionsAdapter.AuctionViewHolder) {
//                        if(mode != tempAuctions.get(position).getAuctionStatus(System.currentTimeMillis())){
//                            String id =tempAuctions.get(position).id;
//                            tempAuctions.removeIf(auction -> auction.id.equals(id));
//                            auctions.removeIf(auction -> auction.id.equals(id));
//                            auctionAdapter.setModels(auctions);
//                            auctionAdapter.notifyItemRemoved(position);
//                            return;
//                        }

                        AuctionsAdapter.AuctionViewHolder auctionHolder = (AuctionsAdapter.AuctionViewHolder) holder;
                        // Update only the countdown TextViews
                        if (position <= auctions.size() - 1)
                            auctionHolder.startTimer(auctions.get(position));
                    }
                }

                // Example: notify only the visible range
//                auctionAdapter.notifyItemRangeChanged(visibleStartPosition, visibleEndPosition - visibleStartPosition + 1);
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
        int size = tempAuctions.size();
        for (Auction auction : allAuctions) {
            if (tempAuctions.contains(auction) && UPCOMING == auction.getAuctionStatus(System.currentTimeMillis()))
                continue;
            if (tempAuctions.contains(auction) && UPCOMING != auction.getAuctionStatus(System.currentTimeMillis()))
                tempAuctions.remove(auction);
            if (!tempAuctions.contains(auction) && UPCOMING == auction.getAuctionStatus(System.currentTimeMillis())) {
                tempAuctions.add(auction);
                if (maxPrice < auction.startingPrice) maxPrice = auction.startingPrice;
            }

        }
        Log.d("ITEMS", "checkItems: " + allAuctions.size() + "," + tempAuctions.size() + "," + size + "," + UPCOMING);
        if (tempAuctions.isEmpty() && binding.emptyStates.getVisibility() != View.VISIBLE) {
            binding.emptyStates.setVisibility(View.VISIBLE);
        }
        if (size == tempAuctions.size()) return;
        tempAuctions.sort(Comparator.comparing(s -> s.startTime));

        auctions = new ArrayList<>(tempAuctions);
        auctionAdapter.setModels(auctions);
        auctionAdapter.notifyDataSetChanged();
        binding.emptyStates.setVisibility(auctions.isEmpty() ? View.VISIBLE : View.GONE);
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
    ArrayList<String> categories = new ArrayList<>();

    private void getAuction() {
        // Read from the database
        auctionsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                auctions = new ArrayList<>();
                tempAuctions = new ArrayList<>();
                allAuctions = new ArrayList<>();
                categories = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Auction item = dataSnapshot.getValue(Auction.class);
                    assert item != null;
                    item.seller = usersMap.get(item.sellerId);
                    if (!categories.contains(item.category))
                        categories.add(item.category);
                    allAuctions.add(item);
                }

                checkItems();
                buildCategoriesChips();
                binding.priceRangeSlider.setValueFrom(0);
                binding.priceRangeSlider.setValueTo(((float) maxPrice));
                binding.priceRangeSlider.setStepSize(1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    void buildCategoriesChips() {
        binding.categoriesChips.removeAllViews();
        addChip("ALL");
        for (String category : categories) {
            addChip(category);
        }
    }

    void addChip(String category) {
        Chip chip = new Chip(requireContext()); // If you're in a Fragment, use requireContext() instead of this

        chip.setText(category);
        chip.setCheckable(true); // Makes it a ChoiceChip
        chip.setClickable(true);
        chip.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                this.category = category;
                filterAuctions();
            }
        });
        binding.categoriesChips.addView(chip);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This callback will only be called when MyFragment is at least Started.
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                // Handle the back button event
                if (!binding.search.getText().toString().trim().isEmpty())
                    binding.search.setText("");
                else
                    Navigation.findNavController(requireActivity(), R.id.nav_buyer_host_fragment).popBackStack();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);

    }
}
