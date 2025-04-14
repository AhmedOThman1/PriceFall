package com.pricefall.FCM;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.RemoteMessage;
import com.pricefall.R;
import com.pricefall.ui.activities.MainActivity;

import java.util.Random;

public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {

    Context context;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        context = this;
        // TO DO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d("TAGA", "From: " + remoteMessage.getFrom());

        Log.w("TAG", remoteMessage.getData() + "\n" + remoteMessage.getData().get("title") + "," + remoteMessage.getData().get("body"));
        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d("TAGAB", "Message data payload: " + remoteMessage.getData());

            String title = remoteMessage.getData().get("title");
            String body = remoteMessage.getData().get("message");

            Log.w("TAGABC", title + "," + body);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel("ExpertNotification", "ExpertNotification", NotificationManager.IMPORTANCE_HIGH);
                NotificationManager manager = context.getSystemService(NotificationManager.class);
                assert manager != null;
                manager.createNotificationChannel(channel);
            }
            // when user click the notification open the app
            Intent notificationIntent = new Intent(context, MainActivity.class);
            notificationIntent.putExtra("Notification", "OpenChat");
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addParentStack(MainActivity.class);
            stackBuilder.addNextIntent(notificationIntent);
            PendingIntent pendingIntent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_MUTABLE);
            } else
                pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "ExpertNotification")
                    .setContentTitle(title)
                    .setContentText(body)
                    .setTicker("New Message Alert!")
                    .setSmallIcon(R.drawable.app_logo)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(body))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            builder.getNotification().flags |= Notification.FLAG_AUTO_CANCEL;
            NotificationManagerCompat compat = NotificationManagerCompat.from(context);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                Toast.makeText(context, "missing permissions", Toast.LENGTH_SHORT).show();
                return;
            }
            compat.notify(new Random().nextInt(4001) + 4000, builder.build());

        }
 
    }


    @Override
    public void onNewToken(@NonNull String token) {
        getApplicationContext().getSharedPreferences("Shared", MODE_PRIVATE).edit().putString("token", token).apply();
        super.onNewToken(token);
    }


}
