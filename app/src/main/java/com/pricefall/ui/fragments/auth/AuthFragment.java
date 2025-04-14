package com.pricefall.ui.fragments.auth;

import static com.pricefall.pojo.User.BUYER;
import static com.pricefall.pojo.User.SELLER;
import static com.pricefall.ui.activities.MainActivity.closeKeyboard;
import static com.pricefall.ui.activities.MainActivity.openKeyboard;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.transition.TransitionInflater;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

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
import com.pricefall.databinding.FragmentLoginBinding;
import com.pricefall.pojo.User;

import java.io.File;


public class AuthFragment extends Fragment {

    FragmentLoginBinding binding;

    FirebaseAuth mAuth;

    FirebaseUser firebaseUser;

    FirebaseDatabase database;
    DatabaseReference usersRef, staffsRef;
    StorageReference usersStorageRef;

    File file;

    @SuppressLint("LogNotTimber")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentLoginBinding.inflate(inflater, container, false);

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("Users");
        usersStorageRef = FirebaseStorage.getInstance().getReference("Users");

        binding.login.setOnClickListener(v -> {
            if (binding.email.getEditText().getText().toString().trim().isEmpty()) {
                binding.email.setError(getResources().getString(R.string.empty));
                binding.email.requestFocus();
                openKeyboard(binding.email.getEditText(), requireActivity());
            } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.email.getEditText().getText().toString().trim())
                    .matches()) {
                binding.email.setError(getResources().getString(R.string.email_not_valid));
                binding.email.requestFocus();
                openKeyboard(binding.email.getEditText(), requireActivity());
            } else if (binding.password.getEditText().getText().toString().trim().isEmpty()) {
                binding.email.setError(null);
                binding.password.setError(getResources().getString(R.string.empty));
                binding.password.requestFocus();
                openKeyboard(binding.password.getEditText(), requireActivity());
            } else {
                binding.email.setError(null);
                binding.password.setError(null);
                binding.login.setEnabled(false);
                binding.loading.setIndeterminateTintList(ColorStateList.valueOf(requireContext().getColor(R.color.white)));
                binding.loading.setVisibility(View.VISIBLE);
                closeKeyboard(requireActivity());
                login(binding.email.getEditText().getText().toString().trim(), binding.password.getEditText().getText().toString().trim());
            }
        });


        binding.signUp.setOnClickListener(v -> {
            if (binding.fullName.getEditText().getText().toString().trim().isEmpty()) {
                binding.fullName.setError(getResources().getString(R.string.empty));
                binding.fullName.requestFocus();
                openKeyboard(binding.fullName.getEditText(), requireActivity());
            } else if (binding.phone.getEditText().getText().toString().trim().isEmpty()) {
                binding.fullName.setError(null);
                binding.phone.setError(getResources().getString(R.string.empty));
                binding.phone.requestFocus();
                openKeyboard(binding.phone.getEditText(), requireActivity());
            } else if (!Patterns.PHONE.matcher(binding.phone.getEditText().getText().toString().trim())
                    .matches()) {
                binding.fullName.setError(null);
                binding.phone.setError(getResources().getString(R.string.phone_not_valid));
                binding.phone.requestFocus();
                openKeyboard(binding.phone.getEditText(), requireActivity());
            } else if (binding.email.getEditText().getText().toString().trim().isEmpty()) {
                binding.fullName.setError(null);
                binding.phone.setError(null);
                binding.email.setError(getResources().getString(R.string.empty));
                binding.email.requestFocus();
                openKeyboard(binding.email.getEditText(), requireActivity());
            } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.email.getEditText().getText().toString().trim())
                    .matches()) {
                binding.fullName.setError(null);
                binding.phone.setError(null);
                binding.email.setError(getResources().getString(R.string.email_not_valid));
                binding.email.requestFocus();
                openKeyboard(binding.email.getEditText(), requireActivity());
            } else if (binding.password.getEditText().getText().toString().trim().isEmpty()) {
                binding.fullName.setError(null);
                binding.phone.setError(null);
                binding.email.setError(null);
                binding.password.setError(getResources().getString(R.string.empty));
                binding.password.requestFocus();
                openKeyboard(binding.password.getEditText(), requireActivity());
            } else if (binding.password.getEditText().getText().toString().trim().length() < 6) {
                binding.fullName.setError(null);
                binding.phone.setError(null);
                binding.email.setError(null);
                binding.password.setError(getResources().getString(R.string.can_not_be_less_than_6));
                binding.password.requestFocus();
                openKeyboard(binding.password.getEditText(), requireActivity());
            } else if (ImageUri == null) {
                binding.fullName.setError(null);
                binding.phone.setError(null);
                binding.email.setError(null);
                binding.password.setError(null);
                Toast.makeText(
                        requireContext(),
                        "Upload image first",
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                binding.fullName.setError(null);
                binding.phone.setError(null);
                binding.email.setError(null);
                binding.password.setError(null);
                binding.loading.setIndeterminateTintList(ColorStateList.valueOf(requireContext().getColor(R.color.colorPrimary)));
                binding.loading.setVisibility(View.VISIBLE);
                binding.signUp.setEnabled(false);
                createNewAccount(
                        binding.email.getEditText().getText().toString().trim(),
                        binding.password.getEditText().getText().toString().trim());
            }
        });


        binding.motionLayout.addTransitionListener(new MotionLayout.TransitionListener() {
            @Override
            public void onTransitionStarted(MotionLayout motionLayout, int startId, int endId) {

            }

            @Override
            public void onTransitionChange(MotionLayout motionLayout, int startId, int endId, float progress) {

            }

            @Override
            public void onTransitionCompleted(MotionLayout motionLayout, int currentId) {
                if (currentId == R.id.start) {
                    binding.login.setBackgroundResource(R.drawable.background_rounded);
                    binding.signUp.setBackgroundResource(R.drawable.background_rounded);
                    binding.signUp.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                } else if (currentId == R.id.endLogin) {
                    binding.login.setBackgroundResource(R.drawable.background_rounded_2);
                } else if (currentId == R.id.endSign) {
                    binding.signUp.setBackgroundResource(R.drawable.background_rounded_2);
                    binding.signUp.setBackgroundTintList(null);
                }
            }

            @Override
            public void onTransitionTrigger(MotionLayout motionLayout, int triggerId, boolean positive, float progress) {

            }
        });

        binding.userImg.setOnClickListener(v -> checkPermissionAndOpenGal13());


        return binding.getRoot();
    }

    //Login

    private void login(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(
                        requireActivity(), task -> {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                                // other
                                getCurrentUser();
                            } else {
                                // If sign in fails, display a message to the user.
                                binding.loading.setVisibility(View.GONE);
                                binding.login.setEnabled(true);
                                Toast.makeText(
                                        requireContext(), "Email or password is incorrect.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        });
    }


    private void getCurrentUser() {
        // Read from the database
        usersRef.child(firebaseUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                User item = snapshot.getValue(User.class);
                if (item == null) {
                    Toast.makeText(requireContext(), "Failed loading account data!", Toast.LENGTH_SHORT).show();
                    binding.login.setEnabled(true);
                    binding.loading.setVisibility(View.GONE);//to do
                } else if (item.userType == SELLER) {
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigate(R.id.action_authFragment_to_sellerMainFragment);
                } else if (item.userType == BUYER) {
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigate(R.id.action_authFragment_to_buyerMainFragment);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }

        });
    }
    //End of Login


    //SignUp
    Uri ImageUri;

    String picture;

    private void createNewAccount(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(
                        requireActivity(), task -> {
                            if (task.isSuccessful()) {
                                firebaseUser = mAuth.getCurrentUser();
                                uploadImage();
                            } else {
                                binding.loading.setVisibility(View.GONE);
                                binding.signUp.setEnabled(true);
                                Toast.makeText(
                                        requireContext(), "Authentication failed.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        });
    }

    private void uploadImage() {
        usersStorageRef.child(firebaseUser.getUid() + "_picture")
                .putFile(ImageUri)
                .addOnCompleteListener(task2 -> {
                    if (task2.isSuccessful()) {
                        usersStorageRef.child(firebaseUser.getUid() + "_picture")
                                .getDownloadUrl().addOnSuccessListener(uri -> {
                                    picture = uri.toString();

                                    UserProfileChangeRequest profileUpdates =
                                            new UserProfileChangeRequest.Builder()
                                                    .setDisplayName(
                                                            binding.fullName.getEditText().getText().toString().trim()
                                                    )
                                                    .setPhotoUri(Uri.parse(picture)).build();

                                    firebaseUser.updateProfile(profileUpdates).addOnCompleteListener(
                                            task1 -> FINISH()
                                    );


                                }).addOnFailureListener(e -> {
                                    Toast.makeText(
                                            requireContext(),
                                            "Failed to upload ! Try again." + e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                    binding.signUp.setEnabled(true);
                                    binding.loading.setVisibility(View.GONE);
                                });
                    }
                }).addOnProgressListener(snapshot -> {
                    binding.loading.setVisibility(View.VISIBLE);
                }).continueWith(task3 -> {
                    binding.loading.setVisibility(View.GONE);
                    binding.signUp.setEnabled(true);
                    if (!task3.isSuccessful()) {
                        Toast.makeText(
                                requireContext(),
                                "Failed to upload! Try again.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                    return task3;
                });
    }


    private void FINISH() {
        binding.loading.setVisibility(View.GONE);
        binding.signUp.setEnabled(true);
        User userItem = new User();
        userItem.id = (firebaseUser.getUid());
        userItem.name = binding.fullName.getEditText().getText().toString().trim();
        userItem.phone = binding.phone.getEditText().getText().toString().trim();
        userItem.email = firebaseUser.getEmail();
        userItem.photo = picture;
        userItem.userType = binding.userType.getCheckedRadioButtonId() == R.id.buyer ? BUYER : SELLER;
        usersRef.child(userItem.id).setValue(userItem);
        //To DO
        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigate(
                userItem.userType == BUYER ? R.id.action_authFragment_to_buyerMainFragment :
                R.id.action_authFragment_to_sellerMainFragment);
    }


    ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    ImageUri = uri;
                    binding.userImg.setImageURI(ImageUri);
                } else
                    Toast.makeText(requireContext(), "NULL", Toast.LENGTH_SHORT).show();
            });

    private void checkPermissionAndOpenGal13() {
        Log.w("HERE", "" + ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable());
        pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    //End of SignUp


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSharedElementEnterTransition(TransitionInflater.from(requireContext()).inflateTransition(android.R.transition.move));

        // This callback will only be called when MyFragment is at least Started.
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                // Handle the back button event

                if (binding.startAnimation.getVisibility() == View.GONE || binding.startSignupAnimation.getVisibility() == View.GONE)
                    binding.motionLayout.transitionToStart();
                else
                    requireActivity().finish();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);

    }

}
