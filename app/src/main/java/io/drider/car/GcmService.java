package io.drider.car;

/**
 * Created by devel on 6/29/15.
 */
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;


/**
 * Service used for receiving GCM messages. When a message is received this service will log it.
 */
public class GcmService extends GcmListenerService {


    public GcmService() {
    }

    @Override
    public void onMessageReceived(String from, Bundle data) {
        sendNotification(data.getString("title"));
        DriverService.singleton(this).processMessage(data.getString("payload"));
    }

    @Override
    public void onDeletedMessages() {
        sendNotification("Deleted messages on server");
    }

    @Override
    public void onMessageSent(String msgId) {
        sendNotification("Upstream message sent. Id=" + msgId);
    }

    @Override
    public void onSendError(String msgId, String error) {
        sendNotification("Upstream message send error. Id=" + msgId + ", error" + error);
    }

    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void sendNotification(String message) {
        int icon = R.mipmap.ic_stat_drider;
        long when = System.currentTimeMillis();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(icon, message, when);
        String title = getString(R.string.app_name);
        Intent notificationIntent = new Intent(this, WebViewActivity.class);
        // set intent so it does not start a new activity
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(this, title, message, intent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
//        notification.flags |= Notification.FLAG_ONGOING_EVENT ;//| Notification.FLAG_FOREGROUND_SERVICE  ;
//        notification.flags |= Notification.FLAG_NO_CLEAR ;
//        notification.flags |=Notification.FLAG_FOREGROUND_SERVICE;
        notificationManager.notify(1, notification);
    }
}