package com.pricefall.models;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.pricefall.R;
import com.pricefall.ui.activities.MainActivity;

import java.util.Random;

public class AlarmReceiver extends BroadcastReceiver {
    Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.w("BroadcastReceiver", "Check Today Shifts");
        this.context = context;

        sendNotification(intent);
    }

    private void sendNotification(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("ReminderNotificationChannel", "ReminderNotificationChannel", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            assert manager != null;
            manager.createNotificationChannel(channel);
        }


        // when user click the notification open the app and open the oneAuctionScreen with the id as data
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.putExtra("id", intent.getStringExtra("id"));
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(notificationIntent);
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_MUTABLE);
        } else
            pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "ReminderNotificationChannel")
                .setContentTitle(intent.getStringExtra("title"))
                .setContentText(intent.getStringExtra("body"))
                .setTicker("New Message Alert!")
                .setSmallIcon(R.drawable.price_fall)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(intent.getStringExtra("body")))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        builder.getNotification().flags |= Notification.FLAG_AUTO_CANCEL;
        NotificationManagerCompat compat = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TO DO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        compat.notify(new Random().nextInt(10001) + 10000, builder.build());

    }


}