package com.pricefall.ui.activities;

import android.Manifest;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.gson.JsonObject;
import com.pricefall.FCM.FCM_ApiClient;
import com.pricefall.FCM.PushNotification;
import com.pricefall.R;
import com.pricefall.databinding.ActivityMainBinding;

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // for offline mode
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        reference.keepSynced(true);

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();


        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        Intent intent = getIntent();
        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(intent)
                .addOnSuccessListener(this, pendingDynamicLinkData -> {
                    if (pendingDynamicLinkData == null) {
                        return;
                    }
                    Uri deepLink = pendingDynamicLinkData.getLink();
                    if (deepLink == null) {
                        return;
                    }
                    Log.v("ShareText", deepLink.toString());
                    if (deepLink.getBooleanQueryParameter("auctionId", false)) {
                        String auctionId = deepLink.getQueryParameter("auctionId");
                        Log.w("ShareText", auctionId);
                        if (firebaseUser == null)
                            Toast.makeText(this, "Login first", Toast.LENGTH_LONG).show();
                        else {
                            Log.w("bookId", auctionId);
                            AuctionId = auctionId;
                        }
                    }
                }).addOnFailureListener(this, e -> Log.v("ShareText", "onFailure : ", e));

        handleNotificationIntent(getIntent()); // 👈 first intent when activity launches

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                // Show a dialog or guide user to settings
                Toast.makeText(this, "Allow Price Reverse Auction App to schedule exact alarms and alarms", Toast.LENGTH_SHORT).show();
                Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(i);
            }
        }

        checkNotificationPermissionForAndroid13AndAbove();
    }


    private void checkNotificationPermissionForAndroid13AndAbove() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(new String[]{Manifest.permission.POST_NOTIFICATIONS});
            }
        }
    }

    ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissions -> {
                AtomicBoolean b = new AtomicBoolean(true);
                permissions.forEach((key, value) -> {
                    if (!value) b.set(false);
                });
            });

    public static String AuctionId;

    public static void shareAuction(String auctionId, Activity activity) {
        FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLink(Uri.parse("https://pricefall.page.link/?auctionId=" + auctionId))
                .setDomainUriPrefix("https://pricefall.page.link/")
                // Open links with this app on Android
                .setAndroidParameters(new DynamicLink.AndroidParameters.Builder().setFallbackUrl(Uri.parse("https://bit.ly/download_pricefall_app")).build())
                .buildShortDynamicLink().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("ShareText", task.getResult().getShortLink().toString());
                        Intent share_app_intent = new Intent(Intent.ACTION_SEND);
                        share_app_intent.setType("text/plain");
                        share_app_intent.putExtra(Intent.EXTRA_SUBJECT, "Share Auction");
                        share_app_intent.putExtra(Intent.EXTRA_TEXT, "Look at this auction \uD83D\uDC40" + task.getResult().getShortLink().toString());
                        activity.startActivity(Intent.createChooser(share_app_intent, "Share Auction "));
                    } else {
                        Log.w("Failed", "" + task.getException().getMessage());
                    }
                }).addOnFailureListener(e -> {
                    Log.w("FAIL", "" + e.getMessage());
                });


    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleNotificationIntent(intent); // 👈 intent when app is resumed
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent != null && intent.hasExtra("id")) {
            AuctionId = intent.getStringExtra("id");
        }
    }

    public static void sendNotificationWithToken(String token, String title, String message) {
        FCM_ApiClient.getINSTANCE()
                .pushNotification(new PushNotification(new PushNotification.NotificationData(
                        title, message
                ), token)).enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                        if (response.isSuccessful()) {
                            Log.w("Success", "" + response.body() + "\n\n" + response.message());
                        } else {
                            Log.w("Not Success", "" + response.message() + " , " + response.errorBody().toString() + " , " + response.body());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                        Log.w("Error", "" + t.getMessage());
                    }
                });
    }

    public static void logout(Context context) {
        View vv = ((Activity) context).getLayoutInflater().inflate(R.layout.dialog_logout, null);
        AlertDialog dialog = new AlertDialog.Builder(context).setView(vv).create();

        vv.findViewById(R.id.cancel).setOnClickListener(v2 -> dialog.dismiss());

        vv.findViewById(R.id.confirm).setOnClickListener(v2 -> {
            FirebaseAuth.getInstance().signOut();
            new Handler().postDelayed(() -> restartApp(((Activity) context)), 1000);
            dialog.dismiss();
        });

        dialog.show();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    public static void restartApp(Activity activity) {
        Intent restartIntent = activity.getBaseContext()
                .getPackageManager()
                .getLaunchIntentForPackage(activity.getBaseContext().getPackageName());
        assert restartIntent != null;
        restartIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(restartIntent);
        activity.finish();
    }

    public static void openKeyboard(EditText textInputLayout, Activity activity) {
        textInputLayout.requestFocus();     // editText.requestFocus();
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);     // Context.INPUT_METHOD_SERVICE
        assert imm != null;
        imm.showSoftInput(textInputLayout, InputMethodManager.SHOW_IMPLICIT); //    first param -> editText
    }


    public static void closeKeyboard(Activity activity) {
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);     // Context.INPUT_METHOD_SERVICE
            assert imm != null;
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static int pxToDp(int px) {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }


    public static String formatPrice(Double price,boolean showCurrency){
        if (price == 0) return showCurrency?"0 KD":"0";
        if (price.intValue() == price)
            return price.intValue()+(showCurrency?" KD":"");
        return ( new DecimalFormat("#.##").format(price)+(showCurrency?" KD":""));
    }

    public static void setTextWithAnimation(TextView textView, double endVal) {
        Log.w("Animation", "" + endVal);
        if(endVal == 0) {
            textView.setText(formatPrice(endVal,true));
            return;
        }
        ValueAnimator animator = new ValueAnimator();
        animator.setObjectValues(0d, endVal);
        animator.addUpdateListener(animation ->
                textView.setText(formatPrice(((Double) animation.getAnimatedValue()),true)));
        // problem here
        animator.setEvaluator((TypeEvaluator<Double>) (fraction, startValue, endValue) -> (startValue + (double) ((endValue - startValue) * fraction)));
        animator.setDuration(1500); // here you set the duration of the anim
        animator.start();
    }

}