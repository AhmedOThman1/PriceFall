package com.pricefall.ui.fragments;


import static com.pricefall.pojo.User.BUYER;
import static com.pricefall.pojo.User.SELLER;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pricefall.R;
import com.pricefall.databinding.FragmentLauncherBinding;
import com.pricefall.pojo.User;


public class LauncherFragment extends Fragment {
    FragmentLauncherBinding binding;

    FirebaseUser currentUser;

    FirebaseDatabase database;
    DatabaseReference usersRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        binding = FragmentLauncherBinding.inflate(inflater, container, false);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();

        if (currentUser == null || currentUser.getEmail() == null) { // Not login
            new Handler().postDelayed(() -> {
                FragmentNavigator.Extras extras = new FragmentNavigator.Extras.Builder().addSharedElement(binding.imageSplash, "app_img").build();
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigate(R.id.action_launcherFragment_to_authFragment, null, null, extras);
            }, 800);
        } else {
            usersRef = database.getReference("Users").child(currentUser.getUid());
            getCurrentUser();
        }

        return binding.getRoot();
    }


    private void getCurrentUser() {
        // Read from the database
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                User item = snapshot.getValue(User.class);
                if (item == null) {
                    FragmentNavigator.Extras extras = new FragmentNavigator.Extras.Builder().addSharedElement(binding.imageSplash, "app_img").build();
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigate(R.id.action_launcherFragment_to_authFragment, null, null, extras);
                } else if (item.userType == SELLER) {
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigate(R.id.action_launcherFragment_to_sellerMainFragment);
                } else if (item.userType == BUYER) {
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigate(R.id.action_launcherFragment_to_buyerMainFragment);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

}
