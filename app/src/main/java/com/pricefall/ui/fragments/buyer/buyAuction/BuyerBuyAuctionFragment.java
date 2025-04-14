package com.pricefall.ui.fragments.buyer.buyAuction;


import static com.pricefall.ui.activities.MainActivity.formatPrice;
import static com.pricefall.ui.activities.MainActivity.openKeyboard;
import static com.pricefall.ui.activities.MainActivity.sendNotificationWithToken;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.pricefall.R;
import com.pricefall.adapters.ImagesAdapter;
import com.pricefall.databinding.DialogConfirmRequestBinding;
import com.pricefall.databinding.FragmentBuyerBuyAuctionBinding;
import com.pricefall.models.LinePagerIndicatorDecoration;
import com.pricefall.pojo.Auction;
import com.pricefall.pojo.Payment;
import com.pricefall.pojo.PaymentCardInfo;
import com.pricefall.pojo.User;


public class BuyerBuyAuctionFragment extends Fragment {
    FragmentBuyerBuyAuctionBinding binding;

    FirebaseUser firebaseUser;

    FirebaseDatabase database;
    DatabaseReference usersRef, auctionRef;

    Auction auction;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        binding = FragmentBuyerBuyAuctionBinding.inflate(inflater, container, false);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("Users").child(firebaseUser.getUid());

        Bundle args = getArguments();
        if (args != null) {
            String json = args.getString("item", "");
            if (!json.isEmpty()) {
                auction = new Gson().fromJson(json, Auction.class);
                auctionRef = database.getReference("Auctions").child(auction.id);
                binding.title.setText(auction.title);
                binding.price.setText(auction.getCurrentAuctionPriceString(System.currentTimeMillis()));

                ImagesAdapter imagesAdapter = new ImagesAdapter(requireContext());
                imagesAdapter.setModels(auction.images);
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
        }
        getCurrentUser();

        binding.plus.setOnClickListener(v -> {
            if (quantity == 5) return;
            quantity++;
            binding.quantity.setText(String.valueOf(quantity));
        });

        binding.minus.setOnClickListener(v -> {
            if (quantity == 1) return;
            quantity--;
            binding.quantity.setText(String.valueOf(quantity));
        });

        binding.pay.setOnClickListener(v -> {
            if (quantity > auction.quantity - auction.quantitySold) {
                Toast.makeText(requireContext(), "Not enough quantity", Toast.LENGTH_SHORT).show();
            } else if (binding.address.getText().toString().isEmpty()) {
                Toast.makeText(requireContext(), "Please enter address", Toast.LENGTH_SHORT).show();
                binding.address.setError(getString(R.string.empty));
                binding.address.requestFocus();
                openKeyboard(binding.address, requireActivity());
            } else if (binding.cardNo.getEditText().getText().toString().trim().isEmpty()) {
                binding.cardNo.setError(getString(R.string.empty));
                binding.cardNo.requestFocus();
                openKeyboard(binding.cardNo.getEditText(), requireActivity());
            } else if (binding.cardNo.getEditText().getText().toString().trim().length() != 19) {
                binding.cardNo.setError(getString(R.string.card_error));
                binding.cardNo.requestFocus();
                openKeyboard(binding.cardNo.getEditText(), requireActivity());
            } else if (binding.month.getEditText().getText().toString().trim().isEmpty()) {
                binding.cardNo.setError(null);
                binding.month.setError(getString(R.string.empty));
                binding.month.requestFocus();
                openKeyboard(binding.month.getEditText(), requireActivity());
            } else if (binding.year.getEditText().getText().toString().trim().isEmpty()) {
                binding.cardNo.setError(null);
                binding.month.setError(null);
                binding.year.setError(getString(R.string.empty));
                binding.year.requestFocus();
                openKeyboard(binding.year.getEditText(), requireActivity());
            } else if (binding.cvv.getEditText().getText().toString().trim().isEmpty()) {
                binding.cardNo.setError(null);
                binding.month.setError(null);
                binding.year.setError(null);
                binding.cvv.setError(getString(R.string.empty));
                binding.cvv.requestFocus();
                openKeyboard(binding.cvv.getEditText(), requireActivity());
            } else {
                binding.cardNo.setError(null);
                binding.month.setError(null);
                binding.year.setError(null);
                binding.cvv.setError(null);


                // payment info
                PaymentCardInfo paymentCardInfo = new PaymentCardInfo();
                paymentCardInfo.card_number = (binding.cardNo.getEditText().getText().toString().trim());
                paymentCardInfo.expiration_month = (binding.month.getEditText().getText().toString().trim());
                paymentCardInfo.expiration_year = (binding.year.getEditText().getText().toString().trim());
                paymentCardInfo.cvv = (binding.cvv.getEditText().getText().toString().trim());
                usersRef.child("paymentCardInfo").setValue(paymentCardInfo);
                usersRef.child("address").setValue(binding.address.getText().toString().trim());
                //TO DO
                confirmPayment();
            }

        });

        binding.cardNo.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                if (s.length() > 0 && (s.length() % 5) == 0) {
                    Log.w("TAG", "afterTextChanged: " + s.length() + "," + (s.length() % 5));
                    final char c = s.charAt(s.length() - 1);
                    if (space == c) {
                        // Remove spacing char
                        s.delete(s.length() - 1, s.length());
                    } else
                        // Insert char where needed.
                        s.insert(s.length() - 1, String.valueOf(space));
                }
            }
        });

        binding.month.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() == 1 && Integer.parseInt(editable.toString()) > 1) {
                    editable.insert(editable.length() - 1, "0");
                    binding.year.requestFocus();
                }

            }
        });

        return binding.getRoot();
    }

    char space = ' ';

    int quantity = 1;

    User currentUser;

    private void getCurrentUser() {
        // Read from the database
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                currentUser = snapshot.getValue(User.class);
                binding.address.setText(currentUser.address);

                if (currentUser.paymentCardInfo != null) {
                    binding.cardNo.getEditText().setText(currentUser.paymentCardInfo.card_number);
                    binding.month.getEditText().setText(currentUser.paymentCardInfo.expiration_month);
                    binding.year.getEditText().setText(currentUser.paymentCardInfo.expiration_year);
                    binding.cvv.getEditText().setText(currentUser.paymentCardInfo.cvv);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void confirmPayment() {
        long date = System.currentTimeMillis();
        double price = auction.getCurrentAuctionPrice(date);

        View view = requireActivity().getLayoutInflater().inflate(R.layout.dialog_confirm_request, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(view).create();
        DialogConfirmRequestBinding dialogBinding = DialogConfirmRequestBinding.bind(view);
        dialogBinding.question.setText("Just to confirm — would you like to pay " +
                formatPrice(price * quantity,true) +
                " for " +
                quantity +
                " “" + auction.title + "” items?");

        dialogBinding.confirm.setOnClickListener(v -> {
            //Request payment
            auction.quantitySold += quantity;
            auctionRef.child("quantitySold").setValue(auction.quantitySold);
            ////

            //// payment notifications
            DatabaseReference paymentNotificationsRef = database.getReference("Payments");
            Payment item = new Payment();
            item.id = paymentNotificationsRef.push().getKey();
            item.userId = firebaseUser.getUid();
            item.auctionId = auction.id;
            item.date = date;
            item.quantity = quantity;
            item.price = price;
            item.totalAmount = price * quantity;
            item.status.add(new Payment.Status("Paid", date));
            paymentNotificationsRef.child(item.id).setValue(item);
            /////

            //TO DO Notify
            sendNotificationWithToken(auction.seller.token,
                    currentUser.name,
                    "paid " + formatPrice(price,true) + " for " +
                            quantity +
                            " “" + auction.title + "” items?");

            Toast.makeText(requireContext(), "Done successfully!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            Navigation.findNavController(requireActivity(), R.id.nav_buyer_host_fragment).popBackStack();
        });
        dialogBinding.cancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }


    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            // Notify adapter to update visible countdowns
            binding.price.setText(auction.getCurrentAuctionPriceString(System.currentTimeMillis()));
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
}
