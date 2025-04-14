package com.pricefall.ui.activities;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

public class ErrorActivity extends AppCompatActivity {

    boolean white = false;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));
        scrollView.setBackgroundColor(Color.BLACK);

        TextView textView = new TextView(this);
        textView.setTextColor(Color.RED);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int marginInPx = (int) (16 * getResources().getDisplayMetrics().density + 0.5f);
        layoutParams.setMargins(marginInPx, marginInPx, marginInPx, marginInPx);
        textView.setLayoutParams(layoutParams);

        // Get the exception message from the intent
        String json = getIntent().getStringExtra("EXTRA_EXCEPTION_MESSAGE");
        // Display the exception message
        Throwable throwable = new Gson().fromJson(json, Throwable.class);

        StringBuilder exceptionMessage = new StringBuilder(throwable.getMessage() + "\n\n");
        for (StackTraceElement element : throwable.getStackTrace())
            exceptionMessage.append("at ").append(element.toString()).append("\n");

        textView.setText(exceptionMessage.toString());

        scrollView.removeAllViews();
        scrollView.addView(textView);

        textView.setOnClickListener(v -> {
            if (white) {
                scrollView.setBackgroundColor(Color.BLACK);
                white = false;
            } else {
                scrollView.setBackgroundColor(Color.WHITE);
                white = true;
            }
        });

        textView.setOnLongClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Copied Text", exceptionMessage);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Copied Text!", Toast.LENGTH_SHORT).show();
            return true;
        });


        setContentView(scrollView);
    }


    public static class MyExceptionHandler implements Thread.UncaughtExceptionHandler {
        private final Context context;

        public MyExceptionHandler(Context context) {
            this.context = context;
        }

        @SuppressLint("LogNotTimber")
        @Override
        public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
            // Handle the exception here, for example, you can log it
            Log.e("MyExceptionHandler", "Uncaught Exception", throwable);

            // Launch the error activity with the exception details
            Intent intent = new Intent(context, ErrorActivity.class);
            intent.putExtra("EXTRA_EXCEPTION_MESSAGE", new Gson().toJson(throwable));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);

            // Kill the current process to ensure the app restarts
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(2);
        }
    }
}