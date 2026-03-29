package com.ecads.pulse.android.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.ecads.pulse.android.R;
import com.ecads.pulse.android.activity.SplashActivity;
import com.ecads.pulse.android.utils.Helper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MessagingService extends FirebaseMessagingService {
    private static final String TAG = MessagingService.class.getSimpleName();
    private FirebaseAuth firebaseAuth;

    public MessagingService(){
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onNewToken(@NonNull String token) {

        SharedPreferences preferences = getSharedPreferences(SplashActivity.APP_DATA, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        if (firebaseAuth.getCurrentUser() != null) {
            firebaseAuth.getCurrentUser().getIdToken(true)
                    .addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {
                        @Override
                        public void onSuccess(GetTokenResult getTokenResult) {
                            Helper.updateFcmToken(firebaseAuth.getUid(), getTokenResult.getToken(), token);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            editor.putString("fcmToken", token);
                        }
                    });
        } else {
            editor.putString("fcmToken", token);
        }
        editor.apply();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        String title = message.getNotification() != null ?
                message.getNotification().getTitle() : "Title";
        String messageBody = message.getNotification() != null ?
                message.getNotification().getBody() : "Empty Body";
        sendNotification(title, messageBody);
    }

    private void sendNotification(String title, String messageBody) {

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, SplashActivity.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(SplashActivity.NOTIFICATION_CHANNEL_ID, "Default Channel", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
        int notificationId = 1;
        notificationManager.notify(notificationId, notificationBuilder.build());
    }
}
