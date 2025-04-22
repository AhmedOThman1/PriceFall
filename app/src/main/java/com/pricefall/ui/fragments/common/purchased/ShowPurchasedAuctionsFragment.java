package com.pricefall.ui.fragments.common.purchased;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pricefall.R;
import com.pricefall.adapters.PaymentsAdapter;
import com.pricefall.databinding.FragmentShowPurchasedAuctionsBinding;
import com.pricefall.pojo.Auction;
import com.pricefall.pojo.Payment;
import com.pricefall.pojo.User;
import com.pricefall.ui.fragments.buyer.main.BuyerMainFragment;
import com.pricefall.ui.fragments.seller.main.SellerMainFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ShowPurchasedAuctionsFragment extends Fragment {
    FragmentShowPurchasedAuctionsBinding binding;

    FirebaseUser firebaseUser;
    FirebaseDatabase database;
    DatabaseReference usersRef, auctionsRef, paymentsRef;
    ArrayList<Payment> payments = new ArrayList<>();
    ArrayList<Payment> tempPayments = new ArrayList<>();

    PaymentsAdapter paymentsAdapter;
    LinearLayoutManager layoutManager;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentShowPurchasedAuctionsBinding.inflate(inflater, container, false);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("Users");
        auctionsRef = database.getReference("Auctions");
        paymentsRef = database.getReference("Payments");


        paymentsAdapter = new PaymentsAdapter(requireContext());
        paymentsAdapter.setModels(payments);
        binding.paymentsRecycler.setAdapter(paymentsAdapter);
        layoutManager = new LinearLayoutManager(requireContext());
        binding.paymentsRecycler.setLayoutManager(layoutManager);

        binding.search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tempPayments = new ArrayList<>();
                String query = s.toString().trim().toLowerCase();
                if (query.isEmpty()) {
                    tempPayments = new ArrayList<>();
                    tempPayments.addAll(payments);
                } else for (int i = 0; i < payments.size(); i++) {
                    if (payments.get(i).user.name.toLowerCase().contains(query) ||
                            payments.get(i).auction.title.toLowerCase().contains(query))
                        tempPayments.add(payments.get(i));
                }


                binding.noResultDesign.noResultDesign.setVisibility(tempPayments.isEmpty() && !payments.isEmpty() ? View.VISIBLE : View.GONE);

                paymentsAdapter.setModels(tempPayments);
                paymentsAdapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        getClients();

        return binding.getRoot();
    }

    Map<String, User> usersMap = new HashMap<>();

    User currentUser;
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
                currentUser = usersMap.get(firebaseUser.getUid());
                paymentsAdapter.setSeller(currentUser.userType==User.SELLER);

                if(currentUser.userType==User.BUYER)
                    BuyerMainFragment.bottomNavigationView.setSelectedItemId(R.id.nav_payment_history);
                else
                    SellerMainFragment.bottomNavigationView.setSelectedItemId(R.id.nav_payment_history);

                getAuction();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    Map<String, Auction> auctionsMap = new HashMap<>();

    private void getAuction() {
        // Read from the database
        auctionsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                auctionsMap = new HashMap<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Auction item = dataSnapshot.getValue(Auction.class);
                    assert item != null;
                    item.seller = usersMap.get(item.sellerId);
                    auctionsMap.put(item.id, item);
                }

                getPayments();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getPayments() {
        // Read from the database
        paymentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                payments = new ArrayList<>();
                tempPayments = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Payment item = dataSnapshot.getValue(Payment.class);
                    assert item != null;
                    item.auction = auctionsMap.get(item.auctionId);
                    item.user = usersMap.get(item.userId);
                    if (item.userId.equals(firebaseUser.getUid()) ||
                            item.auction.sellerId.equals(firebaseUser.getUid())) {
                        Log.w("Payment", "" + item.status.toString()+","+item.status.isEmpty());
                        payments.add(item);
                    }
                }
                tempPayments.addAll(payments);
                paymentsAdapter.setModels(payments);
                paymentsAdapter.notifyDataSetChanged();
                binding.emptyStates.setVisibility(payments.isEmpty() ? View.VISIBLE : View.GONE);
                binding.search.setVisibility(!payments.isEmpty() ? View.VISIBLE : View.GONE);
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
                if (!binding.search.getText().toString().trim().isEmpty())
                    binding.search.setText("");
                else
                    Navigation.findNavController(requireActivity(), R.id.nav_buyer_host_fragment).popBackStack();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);

    }
}
