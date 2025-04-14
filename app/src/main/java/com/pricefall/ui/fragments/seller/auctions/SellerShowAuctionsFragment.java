package com.pricefall.ui.fragments.seller.auctions;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pricefall.databinding.FragmentSellerAuctionsBinding;
import com.pricefall.R;
import com.pricefall.adapters.AuctionsPagerAdapter;
import com.pricefall.pojo.Auction;

public class SellerShowAuctionsFragment extends Fragment {
    FragmentSellerAuctionsBinding binding;

    FirebaseUser firebaseUser;

    FirebaseDatabase database;
    DatabaseReference auctionsRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentSellerAuctionsBinding.inflate(inflater, container, false);

        binding.viewPager.setAdapter(new AuctionsPagerAdapter(getChildFragmentManager(), requireContext()));

        binding.tabs.setupWithViewPager(binding.viewPager);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();
        auctionsRef = database.getReference("Auctions");
//        getAuction();

        return binding.getRoot();
    }

    int upcoming = 0, live = 0, done = 0;

    private void getAuction() {
        // Read from the database
        auctionsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!isAdded())
                    return;

                upcoming = 0;
                live = 0;
                done = 0;

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Auction item = dataSnapshot.getValue(Auction.class);
                    assert item != null;
                    if (item.sellerId.equals(firebaseUser.getUid())) {
                        if (item.getAuctionStatus(System.currentTimeMillis())==Auction.UPCOMING)
                            upcoming++;
                        else if (item.getAuctionStatus(System.currentTimeMillis())==Auction.LIVE)
                            live++;
                        else
                            done++;
                    }
                }

                Log.w("UCD", +upcoming + "," + live + "," + done);

                if (live > 0) {
                    binding.tabs.getTabAt(1).getOrCreateBadge().setNumber(live);
                    binding.tabs.getTabAt(1).getBadge().setBackgroundColor(requireActivity().getColor(R.color.red));
                    binding.tabs.getTabAt(1).getBadge().setBadgeTextColor(requireActivity().getColor(R.color.white));
                } else {
                    binding.tabs.getTabAt(1).removeBadge();
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This callback will only be called when MyFragment is at least Started.
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                // Handle the back button event

                if (binding.tabs.getSelectedTabPosition() == 0)
                    requireActivity().finish();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
    }
}