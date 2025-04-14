package com.pricefall.ui.fragments.seller.main;


import static com.pricefall.ui.activities.MainActivity.logout;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.pricefall.R;
import com.pricefall.databinding.FragmentBuyerMainBinding;
import com.pricefall.databinding.FragmentSellerMainBinding;


public class SellerMainFragment extends Fragment {
    FragmentSellerMainBinding binding;
    public static BottomNavigationView bottomNavigationView; 
    FirebaseDatabase database;
    FirebaseUser firebaseUser;

    DatabaseReference currentUserRef;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentSellerMainBinding.inflate(inflater, container, false);


        bottomNavigationView = binding.bottomNavigation; 

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();

        currentUserRef = database.getReference("Users").child(firebaseUser.getUid());

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int currentFragmentId = Navigation.findNavController(requireActivity(), R.id.nav_seller_host_fragment).getCurrentDestination().getId();
            if (item.getItemId() == R.id.nav_my_auctions) {
                binding.liveAuctionImg.setImageResource(R.drawable.price_fall);
                if (currentFragmentId != R.id.sellerShowAuctionsFragment)
                    Navigation.findNavController(requireActivity(), R.id.nav_seller_host_fragment).navigate(R.id.sellerShowAuctionsFragment);
            } else if (item.getItemId() == R.id.nav_statistics) {
                binding.liveAuctionImg.setImageResource(R.drawable.price_fall_inactive);
                if (currentFragmentId != R.id.sellerStatisticsFragment)
                    Navigation.findNavController(requireActivity(), R.id.nav_seller_host_fragment).navigate(R.id.sellerStatisticsFragment);

            } else if (item.getItemId() == R.id.nav_payment_history) {
                binding.liveAuctionImg.setImageResource(R.drawable.price_fall_inactive);
                if (currentFragmentId != R.id.showPurchasedAuctionsFragment)
                    Navigation.findNavController(requireActivity(), R.id.nav_seller_host_fragment).navigate(R.id.showPurchasedAuctionsFragment);
            } else if (item.getItemId() == R.id.nav_profile) {
                binding.liveAuctionImg.setImageResource(R.drawable.price_fall_inactive);
                if (currentFragmentId != R.id.showEditProfileFragment)
                    Navigation.findNavController(requireActivity(), R.id.nav_seller_host_fragment).navigate(R.id.showEditProfileFragment);
            } else if (item.getItemId() == R.id.nav_logout) {
                logout(requireActivity());
                return false;
            }
            return true;
        });

        binding.liveAuctionImg.setOnClickListener(v->
                bottomNavigationView.setSelectedItemId(R.id.nav_my_auctions));

        getUserToken();
        return binding.getRoot();
    }



    private void getUserToken() {
        // Get token
        // [START log_reg_token]
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("TAG", "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();
                    currentUserRef.child("token").setValue(token);
                    // Log and toast
                });
        // [END log_reg_token]
    }

}
