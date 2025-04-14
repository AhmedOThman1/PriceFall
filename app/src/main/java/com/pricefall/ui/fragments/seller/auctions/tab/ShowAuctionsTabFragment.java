package com.pricefall.ui.fragments.seller.auctions.tab;

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

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

public class ShowAuctionsTabFragment extends Fragment {
    FragmentSellerAuctionsTabBinding binding;

    FirebaseUser firebaseUser;
    FirebaseDatabase database;
    DatabaseReference usersRef, auctionsRef;
    ArrayList<Auction> auctions = new ArrayList<>();
    ArrayList<Auction> tempAuctions = new ArrayList<>();

    AuctionsAdapter auctionAdapter;
    LinearLayoutManager layoutManager;

    int mode;

    public ShowAuctionsTabFragment(int mode) {
        this.mode = mode;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentSellerAuctionsTabBinding.inflate(inflater, container, false);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("Users");
        auctionsRef = database.getReference("Auctions");


        auctionAdapter = new AuctionsAdapter(requireContext(), true);
        auctionAdapter.setModels(auctions);
        binding.auctionsRecycler.setAdapter(auctionAdapter);
        layoutManager = new LinearLayoutManager(requireContext());
        binding.auctionsRecycler.setLayoutManager(layoutManager);
        binding.auctionsRecycler.addOnItemTouchListener(new RecyclerViewTouchListener(requireContext(), binding.auctionsRecycler, new RecyclerViewTouchListener.RecyclerViewClickListener() {
            @Override
            public void onClick(View view, int position) {
            }

            @Override
            public void onLongClick(View view, int position) {
                if (auctions.size() > position && auctions.get(position).sellerId.equals(firebaseUser.getUid()) && auctions.get(position).getAuctionStatus(System.currentTimeMillis()) == UPCOMING) {
                    PopupMenu popupMenu = new PopupMenu(requireContext(), view);
                    popupMenu.getMenu().add("Edit");
                    popupMenu.getMenu().add("Delete");
                    popupMenu.setOnMenuItemClickListener(item -> {
                        if (item.getTitle().equals("Edit")) {
                            Bundle bundle = new Bundle();
                            bundle.putString("item", new Gson().toJson(auctions.get(position)));
                            bundle.putString("categories", new Gson().toJson(categories));
                            Navigation.findNavController(requireActivity(), R.id.nav_seller_host_fragment)
                                    .navigate(R.id.sellerCreateAuctionFragment, bundle);
                        } else if (item.getTitle().equals("Delete")) {
                            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                                    .setTitle("Delete Auction")
                                    .setMessage("Are you sure you want to delete this auction?")
                                    .setPositiveButton("Yes", (dialog1, which) -> {
                                        auctionsRef.child(auctions.get(position).id).removeValue();
                                        auctions.remove(position);
                                        auctionAdapter.notifyDataSetChanged();
                                        dialog1.dismiss();
                                    })
                                    .setNegativeButton("No", (dialog1, which) -> dialog1.dismiss())
                                    .show();
                        }
                        return true;
                    });
                    popupMenu.show();
                }
            }
        }));

        binding.search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                auctions = new ArrayList<>();
                String query = s.toString().trim().toLowerCase();
                if (query.isEmpty()) {
                    auctions = new ArrayList<>();
                    auctions.addAll(tempAuctions);
                } else for (int i = 0; i < tempAuctions.size(); i++) {
                    if (tempAuctions.get(i).title.toLowerCase().contains(query) ||
                            tempAuctions.get(i).description.toLowerCase().contains(query))
                        auctions.add(tempAuctions.get(i));
                }


                binding.noResultDesign.noResultDesign.setVisibility(auctions.isEmpty() && !tempAuctions.isEmpty() ? View.VISIBLE : View.GONE);

                auctionAdapter.setModels(auctions);
                auctionAdapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        binding.addAuction.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("categories", new Gson().toJson(categories));
            Navigation.findNavController(requireActivity(), R.id.nav_seller_host_fragment)
                    .navigate(R.id.sellerCreateAuctionFragment, bundle);
        });

        getClients();

        return binding.getRoot();
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
            if (tempAuctions.contains(auction) && mode == auction.getAuctionStatus(System.currentTimeMillis()))
                continue;
            if (tempAuctions.contains(auction) && mode != auction.getAuctionStatus(System.currentTimeMillis()))
                tempAuctions.remove(auction);
            if (!tempAuctions.contains(auction) && mode == auction.getAuctionStatus(System.currentTimeMillis()))
                tempAuctions.add(auction);
        }
        Log.d("ITEMS", "checkItems: " + allAuctions.size() + "," + tempAuctions.size() + "," + size + "," + mode);
        if (tempAuctions.isEmpty() && binding.emptyStates.getVisibility() != View.VISIBLE) {
            binding.emptyStates.setVisibility(View.VISIBLE);
            binding.search.setVisibility(View.INVISIBLE);
        }
        if (size == tempAuctions.size()) return;
        tempAuctions.sort(Comparator.comparing(s -> s.startTime));

        auctions = new ArrayList<>(tempAuctions);
        auctionAdapter.setModels(auctions);
        auctionAdapter.notifyDataSetChanged();
        binding.emptyStates.setVisibility(auctions.isEmpty() ? View.VISIBLE : View.GONE);
        binding.search.setVisibility(auctions.isEmpty() ? View.INVISIBLE : View.VISIBLE);
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
                    if (item.sellerId.equals(firebaseUser.getUid())) {
                        item.seller = usersMap.get(item.sellerId);
                        if(!categories.contains(item.category))
                            categories.add(item.category);
                        allAuctions.add(item);
                    }
                }

//                createDummyData();

                checkItems();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

//    int i=0;
//    void createDummyData(){
//        Auction item = new Auction("1",firebaseUser.getUid(),getRandomImages(),"Title"+(tempAuctions.size()+1),25*(tempAuctions.size()+1),15,System.currentTimeMillis()+5*1000,60000*3,5,"Some description here ahmed");
//        if (item.sellerId.equals(firebaseUser.getUid())&&mode == item.getAuctionStatus(System.currentTimeMillis())) {
//            item.seller = usersMap.get(item.sellerId);
//            tempAuctions.add(item);
//        }
//        i++;
//        if(i>5)return;
//        createDummyData();
//    }

    ArrayList<String> getRandomImages() {
        ArrayList<String> images = new ArrayList<>(Arrays.asList(
                "https://picsum.photos/800/400?random=1",
                "https://picsum.photos/800/400?random=2",
                "https://picsum.photos/800/400?random=3",
                "https://picsum.photos/800/400?random=4",
                "https://picsum.photos/800/400?random=5",
                "https://picsum.photos/800/400?random=6",
                "https://picsum.photos/800/400?random=7",
                "https://picsum.photos/800/400?random=8",
                "https://picsum.photos/800/400?random=9",
                "https://picsum.photos/800/400?random=10"
        ));

        // Shuffle the list for randomness
        Collections.shuffle(images);

        return images;
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
                else if (mode == UPCOMING)
                    requireActivity().finish();
                else
                    Navigation.findNavController(requireActivity(), R.id.nav_seller_host_fragment).popBackStack();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);

    }
}
