package com.pricefall.models;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pricefall.ui.activities.ErrorActivity;

public class App extends Application {
    FirebaseDatabase database;
    DatabaseReference showTestingRef;
    Context context;
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        initErrorActivity();
    }

    private void initErrorActivity() {
        database = FirebaseDatabase.getInstance();
        showTestingRef = database.getReference("showTestingActivity");
        // Read from the database
        context = this;
        showTestingRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                Boolean show = snapshot.getValue(Boolean.class);
                if (show == null || show)
                    Thread.setDefaultUncaughtExceptionHandler(new ErrorActivity.MyExceptionHandler(context));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


}
