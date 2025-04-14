package com.pricefall.ui.fragments.common.profile;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.pricefall.ui.activities.MainActivity.closeKeyboard;
import static com.pricefall.ui.activities.MainActivity.openKeyboard;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pricefall.R;
import com.pricefall.databinding.FragmentShowEditProfileBinding;
import com.pricefall.pojo.User;

public class ShowEditProfileFragment extends Fragment {
    FragmentShowEditProfileBinding binding;
    FirebaseDatabase database;
    DatabaseReference usersRef;
    StorageReference usersStorageRef;
    Uri ImageUri;


    FirebaseUser firebaseUser;
    User currentUser;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentShowEditProfileBinding.inflate(inflater, container, false);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("Users").child(firebaseUser.getUid());
        usersStorageRef = FirebaseStorage.getInstance().getReference("Users");

        binding.editProfile.setOnClickListener(v -> {
            if (binding.name.getEditText().getText().toString().trim().isEmpty()) {
                binding.name.setError(getResources().getString(R.string.empty));
                binding.name.requestFocus();
                openKeyboard(binding.name.getEditText(), requireActivity());
            } else if (binding.phone.getEditText().getText().toString().trim().isEmpty()) {
                binding.name.setError(null);
                binding.phone.setError(getResources().getString(R.string.empty));
                binding.phone.requestFocus();
                openKeyboard(binding.phone.getEditText(), requireActivity());
            } else if (!Patterns.PHONE.matcher(binding.phone.getEditText().getText().toString().trim()).matches()) {
                binding.name.setError(null);
                binding.phone.setError(getResources().getString(R.string.phone_not_valid));
                binding.phone.requestFocus();
                openKeyboard(binding.phone.getEditText(), requireActivity());
            } else if (binding.address.getVisibility() == VISIBLE && binding.address.getEditText().getText().toString().trim().isEmpty()) {
                binding.address.setError(getResources().getString(R.string.empty));
                binding.address.requestFocus();
                openKeyboard(binding.address.getEditText(), requireActivity());
            } else if (ImageUri == null) {
                binding.name.setError(null);
                binding.phone.setError(null);
                Toast.makeText(
                        requireContext(),
                        "Upload photo first",
                        Toast.LENGTH_SHORT
                ).show();
            } else if (!binding.currentPassword.getEditText().getText().toString().trim().isEmpty() &&
                    binding.newPassword.getEditText().getText().toString().trim().isEmpty()) {
                binding.name.setError(null);
                binding.phone.setError(null);
                binding.currentPassword.setError(null);
                binding.newPassword.setError(getResources().getString(R.string.empty));
                binding.newPassword.requestFocus();
                openKeyboard(binding.newPassword.getEditText(), requireActivity());
            } else if (binding.currentPassword.getEditText().getText().toString().trim().isEmpty() &&
                    !binding.newPassword.getEditText().getText().toString().trim().isEmpty()) {
                binding.name.setError(null);
                binding.phone.setError(null);
                binding.newPassword.setError(null);
                binding.currentPassword.setError(getResources().getString(R.string.empty));
                binding.currentPassword.requestFocus();
                openKeyboard(binding.currentPassword.getEditText(), requireActivity());
            } else if (!binding.currentPassword.getEditText().getText().toString().trim().isEmpty() &&
                    binding.currentPassword.getEditText().getText().toString().trim().length() < 6) {
                binding.name.setError(null);
                binding.phone.setError(null);
                binding.newPassword.setError(null);
                binding.currentPassword.setError(getResources().getString(R.string.can_not_be_less_than_6));
                binding.currentPassword.requestFocus();
                openKeyboard(binding.currentPassword.getEditText(), requireActivity());
            } else {
                binding.name.setError(null);
                binding.phone.setError(null);
                binding.currentPassword.setError(null);
                binding.newPassword.setError(null);
                binding.loading.setVisibility(VISIBLE);
                binding.editProfile.setEnabled(false);
                if (!binding.currentPassword.getEditText().getText().toString().trim().isEmpty())
                    changePassword();
                else
                    editProfile();
            }
        });

        binding.userImg.setOnClickListener(v -> checkPermissionAndOpenGal13());

        getUser();

        return binding.getRoot();
    }

    private void getUser() {
        // Read from the database
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUser = snapshot.getValue(User.class);

                initUI();
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void initUI() {
        picture = currentUser.photo;
        ImageUri = Uri.parse(currentUser.photo);

        Glide.with(requireContext())
                .load(currentUser.photo)
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .into(binding.userImg);

        binding.name.getEditText().setText(currentUser.name);
        binding.phone.getEditText().setText(currentUser.phone);
        binding.address.getEditText().setText(currentUser.address);
        binding.address.setVisibility(currentUser.userType == User.BUYER ? VISIBLE : GONE);
    }


    private void changePassword() {
        AuthCredential authCredential = EmailAuthProvider.getCredential(currentUser.email, binding.currentPassword.getEditText().getText().toString().trim());

        firebaseUser.reauthenticate(authCredential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                firebaseUser.updatePassword(binding.newPassword.getEditText().getText().toString().trim()).addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        Toast.makeText(requireContext(), "Password changed successfully!", Toast.LENGTH_SHORT).show();
                        editProfile();
                    } else {
                        binding.loading.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Failed, try again!", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    binding.loading.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed, try again!", Toast.LENGTH_SHORT).show();
                });
            }
        }).addOnFailureListener(e -> {
            binding.loading.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "Make sure that the current password is correct", Toast.LENGTH_SHORT).show();
        });

    }

    private void editProfile() {
        if (!ImageUri.equals(Uri.parse(currentUser.photo)))
            changePhoto();
        else
            uploadData();
    }

    private void uploadData() {
        UserProfileChangeRequest profileUpdates =
                new UserProfileChangeRequest.Builder()
                        .setDisplayName(binding.name.getEditText().getText().toString().trim())
                        .setPhotoUri(Uri.parse(picture)).build();

        firebaseUser.updateProfile(profileUpdates).addOnCompleteListener(task1 -> {
            currentUser.photo = picture;
            currentUser.name = (binding.name.getEditText().getText().toString().trim());
            currentUser.phone = (binding.phone.getEditText().getText().toString().trim());
            usersRef.setValue(currentUser);

            Toast.makeText(requireContext(), "Your data has been modified successfully", Toast.LENGTH_SHORT).show();
            binding.loading.setVisibility(GONE);
            binding.editProfile.setEnabled(true);
            closeKeyboard(requireActivity());
            Navigation.findNavController(requireActivity(), R.id.nav_seller_host_fragment).popBackStack();
        });
    }

    String picture;

    private void changePhoto() {
        usersStorageRef.child(currentUser.id).putFile(ImageUri).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                usersStorageRef.child(currentUser.id).getDownloadUrl().addOnSuccessListener(uri -> {
                            picture = uri.toString();
                            uploadData();
                        }
                ).addOnFailureListener(e -> {
                    Toast.makeText(
                            requireContext(),
                            "Failed to upload ! Try again." + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                    binding.loading.setVisibility(GONE);
                    binding.editProfile.setEnabled(true);
                });
            }
        }).addOnProgressListener(snapshot -> {
            binding.loading.setVisibility(VISIBLE);
        }).continueWith(task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(
                        requireContext(),
                        "Failed to upload! Try again.",
                        Toast.LENGTH_LONG
                ).show();
                binding.loading.setVisibility(GONE);
                binding.editProfile.setEnabled(true);
            }
            return task;
        });
    }

    ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    ImageUri = uri;
                    Glide.with(requireContext())
                            .load(ImageUri)
                            .diskCacheStrategy(DiskCacheStrategy.DATA)
                            .into(binding.userImg);
                } else
                    Toast.makeText(requireContext(), "NULL", Toast.LENGTH_SHORT).show();
            });

    private void checkPermissionAndOpenGal13() {
        Log.w("HERE", "" + ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable());
        pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }


}
