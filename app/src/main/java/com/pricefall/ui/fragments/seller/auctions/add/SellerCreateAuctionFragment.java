package com.pricefall.ui.fragments.seller.auctions.add;


import static com.pricefall.ui.activities.MainActivity.closeKeyboard;
import static com.pricefall.ui.activities.MainActivity.openKeyboard;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pricefall.R;
import com.pricefall.adapters.AddMediaAdapter;
import com.pricefall.databinding.FragmentSellerCreateAuctionBinding;
import com.pricefall.models.CenterZoomItemDecoration;
import com.pricefall.models.RecyclerViewTouchListener;
import com.pricefall.pojo.Auction;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@SuppressLint("SetTextI18n")
public class SellerCreateAuctionFragment extends Fragment {
    FragmentSellerCreateAuctionBinding binding;
    FirebaseUser firebaseUser;
    FirebaseDatabase database;
    DatabaseReference auctionRef;
    StorageReference auctionsStorageRef;

    Auction editItem;

    AddMediaAdapter mediaAdapter;
    ArrayList<String> mediaItems = new ArrayList<>();
    ArrayList<String> categories = new ArrayList<>();
    boolean editMode;
    int editPos = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentSellerCreateAuctionBinding.inflate(inflater, container, false);


        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();
        auctionsStorageRef = FirebaseStorage.getInstance().getReference("Auctions");
        auctionRef = database.getReference("Auctions");

        startDateTime = Calendar.getInstance();

        mediaAdapter = new AddMediaAdapter(requireContext());
        mediaAdapter.setModels(mediaItems);
        binding.auctionMediaRecycler.setAdapter(mediaAdapter);
        binding.auctionMediaRecycler.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.auctionMediaRecycler.addItemDecoration(new CenterZoomItemDecoration(1.5f));
        binding.auctionMediaRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();

                float middleItemPosition = layoutManager.findFirstCompletelyVisibleItemPosition();
                for (int i = firstVisibleItemPosition; i <= lastVisibleItemPosition; i++) {
                    View child = layoutManager.findViewByPosition(i);
                    if (child == null) continue;
                    float scale = i == middleItemPosition ? 1.1f : 0.9f;// - Math.abs(middleItemPosition - i) / 5f; // Adjust the scaling factor as needed
                    Log.w("Scale", i + ": " + scale);
                    child.setScaleX(scale);
                    child.setScaleY(scale);
                }
            }
        });
        binding.auctionMediaRecycler.addOnItemTouchListener(new RecyclerViewTouchListener(requireContext(), binding.auctionMediaRecycler, new RecyclerViewTouchListener.RecyclerViewClickListener() {
            @Override
            public void onClick(View view, int position) {
                if (position == 0) {
                    checkPermissionAndOpenGal13();
                }
            }

            @Override
            public void onLongClick(View view, int position) {
                if (position != 0) {
                    PopupMenu popupMenu = new PopupMenu(requireContext(), view);
                    popupMenu.getMenu().add("Edit");
                    popupMenu.getMenu().add("Delete");
                    popupMenu.setOnMenuItemClickListener(item -> {
                        if (item.getTitle().equals("Edit")) {
                            editPos = position;
                            checkPermissionAndOpenGal13();
                        } else if (item.getTitle().equals("Delete")) {
                            mediaItems.remove(position - 1);
                            mediaAdapter.notifyDataSetChanged();
                        }
                        return true;
                    });
                    popupMenu.show();
                }
            }
        }));
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.DOWN | ItemTouchHelper.UP | ItemTouchHelper.START | ItemTouchHelper.END) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int swipeDir) {
                //Remove swiped item from list and notify the RecyclerView
                int position = viewHolder.getAdapterPosition();
                if (position == 0) return;
                mediaItems.remove(position - 1);
                mediaAdapter.notifyDataSetChanged();
            }
        });
        itemTouchHelper.attachToRecyclerView(binding.auctionMediaRecycler);


        Bundle args = getArguments();
        if (args != null) {
            String categoriesJson = getArguments().getString("categories"); // or getIntent().getStringExtra if Activity

            Type type = new TypeToken<ArrayList<String>>(){}.getType();
            categories = new Gson().fromJson(categoriesJson, type);

            String json = args.getString("item", "");
            if (!json.isEmpty()) {
                editItem = new Gson().fromJson(json, Auction.class);
                binding.title.getEditText().setText(editItem.title);
                ((AutoCompleteTextView)binding.category.getEditText()).setText(editItem.category,false);
                binding.startingPrice.getEditText().setText(editItem.startingPrice + "");
                binding.minimumPrice.getEditText().setText(editItem.minimumPrice + "");
                binding.quantity.getEditText().setText(editItem.quantity + "");
                binding.description.getEditText().setText(editItem.description);
                startDateTime.setTimeInMillis(editItem.startTime);
                binding.startDate.getEditText().setText(new SimpleDateFormat("EEE, dd MMMM • hh:mm a", Locale.ENGLISH).format(startDateTime.getTimeInMillis()));

                //to do duration and unit
                int hours = (int) TimeUnit.MILLISECONDS.toHours(editItem.durationInMillis);
                int minutes = (int) TimeUnit.MILLISECONDS.toMinutes(editItem.durationInMillis);

                ((AutoCompleteTextView) binding.durationUnit.getEditText()).setText(hours > 0 ? units[1] : units[0], false);
                binding.duration.getEditText().setText(hours > 0 ? hours + "" : minutes + "");

                mediaItems = new ArrayList<>(editItem.images);
                mediaAdapter.setModels(mediaItems);
                mediaAdapter.notifyDataSetChanged();
                editMode = true;

                binding.toolbar.setTitle(R.string.edit_auction);

            } else
                binding.toolbar.setTitle(R.string.create_auction);
        } else
            binding.toolbar.setTitle(R.string.create_auction);


        binding.create.setOnClickListener(v -> {
            if (binding.title.getEditText().getText().toString().trim().isEmpty()) {
                binding.title.setError(getString(R.string.empty));
                binding.title.requestFocus();
                openKeyboard(binding.title.getEditText(), requireActivity());
            } else if (binding.startingPrice.getEditText().getText().toString().trim().isEmpty()) {
                binding.title.setError(null);
                binding.startingPrice.setError(getString(R.string.empty));
                binding.startingPrice.requestFocus();
                openKeyboard(binding.startingPrice.getEditText(), requireActivity());
            } else if (binding.minimumPrice.getEditText().getText().toString().trim().isEmpty()) {
                binding.title.setError(null);
                binding.startingPrice.setError(null);
                binding.minimumPrice.setError(getString(R.string.empty));
                binding.minimumPrice.requestFocus();
                openKeyboard(binding.minimumPrice.getEditText(), requireActivity());
            } else if (binding.quantity.getEditText().getText().toString().trim().isEmpty()) {
                binding.title.setError(null);
                binding.startingPrice.setError(null);
                binding.minimumPrice.setError(null);
                binding.quantity.setError(getString(R.string.empty));
                binding.quantity.requestFocus();
                openKeyboard(binding.quantity.getEditText(), requireActivity());
            } else if (binding.startDate.getEditText().getText().toString().trim().isEmpty()) {
                binding.title.setError(null);
                binding.startingPrice.setError(null);
                binding.minimumPrice.setError(null);
                binding.quantity.setError(null);
                binding.startDate.setError(getString(R.string.empty));
                pickDateDialog();
            } else if (binding.duration.getEditText().getText().toString().trim().isEmpty()) {
                binding.title.setError(null);
                binding.startingPrice.setError(null);
                binding.minimumPrice.setError(null);
                binding.quantity.setError(null);
                binding.startDate.setError(null);
                binding.duration.setError(getString(R.string.empty));
                binding.duration.requestFocus();
                openKeyboard(binding.duration.getEditText(), requireActivity());
            } else if (binding.durationUnit.getEditText().getText().toString().trim().isEmpty()) {
                binding.title.setError(null);
                binding.startingPrice.setError(null);
                binding.minimumPrice.setError(null);
                binding.quantity.setError(null);
                binding.startDate.setError(null);
                binding.durationUnit.setError(getString(R.string.empty));
                binding.durationUnit.requestFocus();
            } else if (binding.description.getEditText().getText().toString().trim().isEmpty()) {
                binding.title.setError(null);
                binding.startingPrice.setError(null);
                binding.minimumPrice.setError(null);
                binding.quantity.setError(null);
                binding.startDate.setError(null);
                binding.durationUnit.setError(null);
                binding.description.setError(getString(R.string.empty));
                binding.description.requestFocus();
                openKeyboard(binding.description.getEditText(), requireActivity());
            } else if (mediaItems.isEmpty()) {
                binding.title.setError(null);
                binding.startingPrice.setError(null);
                binding.minimumPrice.setError(null);
                binding.quantity.setError(null);
                binding.startDate.setError(null);
                binding.durationUnit.setError(null);
                Toast.makeText(requireContext(), "Add at least one image!", Toast.LENGTH_SHORT).show();
            } else {
                binding.title.setError(null);
                binding.startingPrice.setError(null);
                binding.minimumPrice.setError(null);
                binding.quantity.setError(null);
                binding.startDate.setError(null);
                binding.durationUnit.setError(null);
                binding.create.setEnabled(false);
                binding.loading.setVisibility(View.VISIBLE);
                uploadDocs();
            }
        });

        binding.pickStartDate.setOnClickListener(v -> pickDateDialog());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, categories);
        ((AutoCompleteTextView) binding.category.getEditText()).setAdapter(adapter);

        ArrayAdapter<String> adapter1 = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, units);
        ((AutoCompleteTextView) binding.durationUnit.getEditText()).setAdapter(adapter1);
        ((AutoCompleteTextView) binding.durationUnit.getEditText()).setText("Minutes", false); // false = no filtering animation

        return binding.getRoot();
    }

    String[] units = {"Minutes", "Hours"};

    ArrayList<String> media = new ArrayList<>();
String id;
    @SuppressLint("SetTextI18n")
    private void uploadDocs() {
        Log.w("uploadDocs", "^_<");
        media = new ArrayList<>();

        id = editMode? editItem.id : auctionRef.push().getKey();
        if (!mediaItems.isEmpty()) {
            binding.create.setText("Uploading " + (mediaItems.size() + 1) + " media...");
            for (int i = 0; i < mediaItems.size(); i++) {
                int finalI = i;

                int rand = new Random().nextInt();
                if (editMode && editItem.images.contains(mediaItems.get(i))) {
                    media.add(mediaItems.get(i));
                    if (media.size() == mediaItems.size()) {
                        uploadAuction();
                    }
                } else
                    auctionsStorageRef.child(id).child(rand + "_media").putFile(Uri.parse(mediaItems.get(i))).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            auctionsStorageRef.child(id).child(rand + "_media").getDownloadUrl().addOnSuccessListener(uri -> {
                                media.add(uri.toString());
                                if (media.size() == mediaItems.size()) {
                                    uploadAuction();
                                }
                            }).addOnFailureListener(e -> {
                                Toast.makeText(
                                        requireContext(),
                                        "Failed to upload ! Try again." + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                                binding.loading.setVisibility(View.GONE);
                                binding.create.setEnabled(true);
                            });
                        }
                    }).addOnProgressListener(snapshot -> {
                        binding.loading.setVisibility(View.VISIBLE);
                        String progress = String.format(Locale.getDefault(), "%.1f", ((100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount()));
                        binding.create.setText("Media #" + (finalI + 1) + " -> " + progress + "%");
                    }).continueWith(task -> {
                        binding.loading.setVisibility(View.GONE);
                        binding.create.setEnabled(true);
                        if (!task.isSuccessful()) {
                            Toast.makeText(
                                    requireContext(),
                                    "Failed to upload! Try again.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                        return task;
                    });

            }
        } else {
            media = new ArrayList<>(mediaItems);
            uploadAuction();
        }
    }


    @SuppressLint("SetTextI18n")
    private void uploadAuction() {
        binding.create.setText("Uploading auction data");

        if(editMode){
            editItem.id = id;
            editItem.images = new ArrayList<>(media);
            editItem.title = binding.title.getEditText().getText().toString().trim();
            editItem.category = binding.category.getEditText().getText().toString().trim();
            editItem.startingPrice = Double.parseDouble(binding.startingPrice.getEditText().getText().toString());
            editItem.minimumPrice = Double.parseDouble(binding.minimumPrice.getEditText().getText().toString());
            editItem.quantity = Integer.parseInt(binding.quantity.getEditText().getText().toString());
            editItem.startTime = startDateTime.getTimeInMillis();
            editItem.durationInMillis =
                    binding.durationUnit.getEditText().getText().toString().equals(units[0])?
                            Integer.parseInt(binding.duration.getEditText().getText().toString()) * 60000L :
                            Integer.parseInt(binding.duration.getEditText().getText().toString()) * 3600000L;
            editItem.description = binding.description.getEditText().getText().toString();
            editItem.sellerId = firebaseUser.getUid();
            auctionRef.child(id).setValue(editItem);
        }else{
            Auction auction = new Auction();
            auction.id = id;
            auction.images = new ArrayList<>(media);
            auction.title = binding.title.getEditText().getText().toString();
            auction.category = binding.category.getEditText().getText().toString().trim();
            auction.startingPrice = Double.parseDouble(binding.startingPrice.getEditText().getText().toString());
            auction.minimumPrice = Double.parseDouble(binding.minimumPrice.getEditText().getText().toString());
            auction.quantity = Integer.parseInt(binding.quantity.getEditText().getText().toString());
            auction.startTime = startDateTime.getTimeInMillis();
            auction.durationInMillis =
                    binding.durationUnit.getEditText().getText().toString().equals(units[0])?
                            Integer.parseInt(binding.duration.getEditText().getText().toString()) * 60000L :
                            Integer.parseInt(binding.duration.getEditText().getText().toString()) * 3600000L;
            auction.description = binding.description.getEditText().getText().toString();
            auction.sellerId = firebaseUser.getUid();
            auctionRef.child(id).setValue(auction);

            Toast.makeText(
                    requireContext(),
                    "Uploaded Successfully!",
                    Toast.LENGTH_LONG
            ).show();

        }

        binding.loading.setVisibility(View.GONE);
        binding.create.setEnabled(true);
        Navigation.findNavController(requireActivity(), R.id.nav_seller_host_fragment).popBackStack();
    }


    Calendar startDateTime;

    private void pickDateDialog() {
        closeKeyboard(requireActivity());
        int year = startDateTime.get(Calendar.YEAR);
        int month = startDateTime.get(Calendar.MONTH);
        int day = startDateTime.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog pickerDialog = new DatePickerDialog(getContext(), (view, year1, month1, dayOfMonth) -> {
            startDateTime.set(Calendar.YEAR, year1);
            startDateTime.set(Calendar.MONTH, month1);
            startDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            //open time picker dialog
            TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), (view1, hourOfDay, minute) -> {
                startDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                startDateTime.set(Calendar.MINUTE, minute);
                binding.startDate.getEditText().setText(new SimpleDateFormat("EEE, dd MMMM • hh:mm a", Locale.ENGLISH).format(startDateTime.getTimeInMillis()));
            }, startDateTime.get(Calendar.HOUR_OF_DAY), startDateTime.get(Calendar.MINUTE), false);
            timePickerDialog.show();
        }, year, month, day);
        pickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        pickerDialog.show();
    }


    private void checkPermissionAndOpenGal13() {
        pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageAndVideo.INSTANCE)
                .build());
    }


    ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    if (editPos != -1) {
                        mediaItems.remove(editPos - 1);
                        editPos = -1;
                    }

                    for (Uri uri : uris)
                        mediaItems.add(uri.toString());

                    mediaAdapter.notifyDataSetChanged();

                } else
                    Toast.makeText(requireContext(), "NULL", Toast.LENGTH_SHORT).show();
            });

}
