package io.drider.car;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.DetectedActivity;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_10;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.UUID;

import io.nlopez.smartlocation.OnActivityUpdatedListener;
import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.config.LocationParams;
import io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesWithFallbackProvider;

/**
 * Created by devel on 6/4/15.
 */
public class DriverService {
    public static final String HOST = "drider.io";
//    public static final String HOST = "drider.dev:4000";
    private static final String TAG = "DriverService";
    private static DriverService mSingleton;
    private Boolean isWorking = false;
    WebSocketClient mWebSocketClient;
    Context lastContext;
    BroadcastReceiver receiver;
    public Long sessionId = (long) 0;
    public Long sessionNumber = (long) 0;
    public Ringtone ringtone;
    private Long sequenceNumber = (long)0;
    private String lastActiveStatus = "false";
    private Boolean isActive;

    public synchronized static DriverService singleton(Context context){
        if (mSingleton == null) mSingleton = new DriverService(context);
        return mSingleton;
    }

    private DriverService(Context context){
        lastContext = context;
        if(!(Thread.getDefaultUncaughtExceptionHandler() instanceof CustomExceptionHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler());
        }
        context.startService(new Intent(context, RegistrationIntentService.class));
        CookieSyncManager.createInstance(context);
//        connectWebSocket();

//        receiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                Log.d(TAG, "location received");
//                Double Latitude = intent.getDoubleExtra("Latitude", 0);
//                Log.d(TAG, "lat: "+ Latitude);
//                Double Longitude = intent.getDoubleExtra("Longitude", 0);
//                Log.d(TAG, "Longitude: "+ Longitude);
//                Float Accuracy = intent.getFloatExtra("Accuracy", 0);
//                Log.d(TAG, "Accuracy: "+ Accuracy);
//                String Provider = intent.getStringExtra("Provider");
//                Log.d(TAG, "Provider: "+ Provider);
//                checkWebSocketConnection();
//                WebSocket conn = mWebSocketClient.getConnection();
//                if (conn != null && conn.isOpen()) {
//                    try {
//                        JSONObject json = new JSONObject();
//                        json.put("type", "location");
//                        json.put("lat", Latitude);
//                        json.put("long", Longitude);
//                        json.put("accy", Accuracy);
//                        json.put("prov", Provider);
//                        json.put("time_ms", Calendar.getInstance().getTimeInMillis());
//                        json.put("session_id", sessionId);
//                        mWebSocketClient.send(json.toString());
//                    } catch (JSONException e){};
//                } else {
//                    Log.e(TAG, "dropped location");
//                }
//            }
//        };
//        LocalBroadcastManager.getInstance(null).registerReceiver((receiver),
//                new IntentFilter("Location")
//        );
    }

    public void sendLocation(Location location){
        checkWebSocketConnection();
        WebSocket conn = mWebSocketClient.getConnection();
        if (conn != null && conn.isOpen()) {
//            Intent intent2 = new Intent("UIUpdate");
//            intent2.putExtra("active", "true");
//            LocalBroadcastManager.getInstance(lastContext).sendBroadcast(intent2);
            try {
                JSONObject json = new JSONObject();
                json.put("type", "location");
                json.put("lat", location.getLatitude());
                json.put("long", location.getLongitude());
                json.put("accy", location.getAccuracy());
                json.put("prov", location.getProvider());
                json.put("time_ms", Calendar.getInstance().getTimeInMillis());
                json.put("session_id", sessionId);
                mWebSocketClient.send(json.toString());
            } catch (JSONException e){};

            SharedPreferences settings = lastContext.getApplicationContext().getSharedPreferences("SessionPrefs", 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putLong("lastSessionNumber", sessionNumber);
            editor.putLong("lastSessionTime", Calendar.getInstance().getTimeInMillis()/1000);
            editor.apply();

        } else {
            Log.e(TAG, "dropped location");
        }

    }

    public void sendActivity(DetectedActivity activity){
        WebSocket conn = mWebSocketClient.getConnection();
        if (conn != null && conn.isOpen()) {
//            Intent intent2 = new Intent("UIUpdate");
//            intent2.putExtra("active", "true");
//            LocalBroadcastManager.getInstance(lastContext).sendBroadcast(intent2);
            try {
                JSONObject json = new JSONObject();
                json.put("type", "android_activity");
                json.put("activity_code", activity.getType());
                json.put("confidence", activity.getConfidence());
                json.put("time_ms", Calendar.getInstance().getTimeInMillis());
                json.put("session_id", sessionId);
                mWebSocketClient.send(json.toString());
            } catch (JSONException e){};
        } else {
            Log.e(TAG, "dropped location");
        }
    }

    public synchronized void onChange(Context context){
        lastContext = context;
        boolean power = isACPowerConnected(context);
        isActive = power
                && isDriverModeEnabled(context);
        if ( isActive)
            start(context);
        else
            stop(context);
//        Intent intent = new Intent("LocationService");
//        intent.putExtra("isActive", isActive);
//        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        Intent intent2 = new Intent("UIUpdate");
        if ( isInternetConnected(context) ){
            intent2.putExtra("internet", isConnected() ? "true" : "maybe" );
        } else {
            intent2.putExtra("internet", "false" );
        }
        intent2.putExtra("power", String.valueOf(power));
        intent2.putExtra("active", lastActiveStatus);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent2);

    }

    public Boolean isConnected(){
        if (mWebSocketClient == null) return false;
        WebSocket conn = mWebSocketClient.getConnection();
        return (conn != null && conn.isOpen());
    }
    public synchronized void start(final Context context){
//        context.startService(new Intent(context, LocationService.class));
//        context.startService(new Intent(context, LocationUpdateService.class));
        if (!isWorking) {
            SharedPreferences settings = lastContext.getApplicationContext().getSharedPreferences("SessionPrefs", 0);
            Long lastSessionNumber = settings.getLong("lastSessionNumber", (long)0 );
            Long lastSessionTime = settings.getLong("lastSessionTime", (long)0 );
            Long onLine = Calendar.getInstance().getTimeInMillis()/1000;
            if (onLine - lastSessionTime > 600){
                sessionNumber = onLine;
            } else{
                sessionNumber = lastSessionNumber;
            }

            Log.e(TAG, "location reconnet 1");
//            Toast.makeText(context, "listening", Toast.LENGTH_LONG).show();

            try {
                SmartLocation.with(context).location()
                        .config(LocationParams.NAVIGATION)
                        .provider(new LocationGooglePlayServicesWithFallbackProvider(context))
                        .start(new OnLocationUpdatedListener() {
                            public void onLocationUpdated(Location location) {
                                Log.e(TAG, "location sending");
//                                Toast.makeText(context, "smartlocation acy:" + location.getAccuracy() + ",v:" + location.getSpeed(), Toast.LENGTH_LONG).show();
//                                try {
//                                ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
//                                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 1000);
//                                } catch (java.lang.RuntimeException e){};
                                sendLocation(location);
                            }
                        });
                SmartLocation.with(context).activityRecognition()
                        .start(new OnActivityUpdatedListener() {
                            public void onActivityUpdated(DetectedActivity activity) {
                                Log.e(TAG, "activity " + activity.toString());
                                sendActivity(activity);
//                                Toast.makeText(context, "a" + activity.toString(), Toast.LENGTH_LONG).show();
//                                try {
//                                    ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
//                                    toneGenerator.startTone(ToneGenerator.TONE_SUP_RINGTONE, 1000);
//                                } catch (java.lang.RuntimeException e){};
                            }
                        });
            } catch (java.lang.IllegalArgumentException e){
                Log.e(TAG, "exception " + e.toString());

            }

//        connectWebSocket();
            isWorking = true;
            Intent intent2 = new Intent("UIUpdate");
            intent2.putExtra("text", "" );
            LocalBroadcastManager.getInstance(lastContext).sendBroadcast(intent2);
            checkWebSocketConnection();
        }
    }

    public synchronized void stop(Context context){
        if (isWorking) {
            SmartLocation.with(context).location().stop();
            SmartLocation.with(context).activityRecognition().stop();
//        context.stopService(new Intent(context, LocationService.class));
            mWebSocketClient.close();
            isWorking = false;
        }
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }


    // Websocket section

    private Boolean checkWebSocketConnection(){
            if (!isActive){
                // do nothing
                return false;
            }
            else if (mWebSocketClient == null) {
                Log.e(TAG, "new websocket");
                connectWebSocket();
                return false;
            } else {
                WebSocket conn = mWebSocketClient.getConnection();
                if (conn == null || !(conn.isOpen() || conn.isConnecting())){
                    Log.e(TAG, "websocket reconnet");
                    connectWebSocket();
                    return false;
                } else {
                    return true;
                }
            }
    }

    private void connectWebSocket() {
        URI uri;
        try {
//            uri = new URI("ws://drider.dev:4000/chat");
            uri = new URI("ws://" + HOST + "/chat");
//            uri = new URI("ws://10.25.10.131:3000/chat");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        HashMap<String, String> header = new HashMap<String, String>();
        CookieManager cookieManager = CookieManager.getInstance();
        String cookie = cookieManager.getCookie(uri.getHost());
        header.put("cookie", cookie);
//        header =this.sessionCookie;//session cookie is obtained from https authentication
            mWebSocketClient = new WebSocketClient(uri, new Draft_10(), header, 0 ) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    Log.e(TAG, "Opened");
//                    mWebSocketClient.send("Hello from " + Build.MANUFACTURER + " " + Build.MODEL);
                    try {
                        String versionName;
                        int versionCode;
                        try {
                            versionName = lastContext.getPackageManager().getPackageInfo(lastContext.getPackageName(), 0).versionName;
                            versionCode = lastContext.getPackageManager().getPackageInfo(lastContext.getPackageName(), 0).versionCode;
                        } catch (PackageManager.NameNotFoundException  e){
                            versionName="";
                            versionCode=0;
                        }
                        JSONObject json = new JSONObject();
                        json.put("type", "handshake");
                        json.put("session_number", sessionNumber);
                        json.put("device_identifier", DeviceId());
                        json.put("time_ms", Calendar.getInstance().getTimeInMillis());
                        json.put("client_version", versionName);
                        json.put("client_version_code", versionCode);
                        json.put("client_os_version", Build.MANUFACTURER + " " + Build.MODEL + " " + Build.VERSION.SDK_INT);
                        json.put("android_manufacturer", Build.MANUFACTURER);
                        json.put("android_model", Build.MODEL);
                        json.put("android_sdk", Build.VERSION.SDK_INT);
                        json.put("is_location_enabled", SmartLocation.with(lastContext).location().state().locationServicesEnabled());
                        json.put("is_location_available", SmartLocation.with(lastContext).location().state().isAnyProviderAvailable());
                        json.put("is_gps_available", SmartLocation.with(lastContext).location().state().isGpsAvailable());
                        json.put("is_google_play_available", GooglePlayServicesUtil.isGooglePlayServicesAvailable(lastContext));

                        mWebSocketClient.send(json.toString());
                    } catch (JSONException e){};

                    Intent intent2 = new Intent("UIUpdate");
                    intent2.putExtra("internet", "true" );
                    lastActiveStatus = "maybe";
                    intent2.putExtra("active", lastActiveStatus);
                    LocalBroadcastManager.getInstance(lastContext).sendBroadcast(intent2);
                }

                @Override
                public void onMessage(String s) {
                    final String message = s;
                    Log.e(TAG, "onMessage");
                    Log.e(TAG, message);
                    processMessage(message);
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
////                        TextView textView = (TextView)findViewById(R.id.messages);
////                        textView.setText(textView.getText() + "\n" + message);
//                    }
//                });
                }

                @Override
                public void onClose(int i, String s, boolean b) {
                    Log.e(TAG, "Closed " + s);
                    Intent intent2 = new Intent("UIUpdate");
                    intent2.putExtra("internet", isInternetConnected(lastContext) ? "maybe" : "false" );
                    lastActiveStatus = "false";
                    intent2.putExtra("active", lastActiveStatus );
                    LocalBroadcastManager.getInstance(lastContext).sendBroadcast(intent2);
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error " + e.getMessage());
                }
            };
            mWebSocketClient.connect();
    }

    public void processMessage(String message){
        try {
            JSONObject json = new JSONObject(message);
            Long seq = json.optLong("sequence_number", -1);
            if (seq > 0){
                if (seq <= sequenceNumber) return;
                sequenceNumber = seq;
            }

            if (!json.optString("handshake_reply", "").isEmpty()){
                Intent intent2 = new Intent("UIUpdate");
                lastActiveStatus = "true";
                intent2.putExtra("active", lastActiveStatus );
                LocalBroadcastManager.getInstance(lastContext).sendBroadcast(intent2);
                setNotification();
            }
            String text = json.optString("text", "");
            if (!text.isEmpty()){
                Intent intent2 = new Intent("UIUpdate");
                intent2.putExtra("text", text );
                LocalBroadcastManager.getInstance(lastContext).sendBroadcast(intent2);
            }
            if (json.optBoolean("off_client", false)){
                SharedPreferences settings = lastContext.getApplicationContext().getSharedPreferences(MainActivity.PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("DriverActive", false);
                editor.commit();
                onChange(lastContext);
            }
            if (json.optBoolean("stop_client", false)){
                stop(lastContext);
            }
            String webview = json.optString("webview", "");
            if (!webview.isEmpty()){
                Log.e(TAG, "webview");
                Intent intent = new Intent(lastContext, WebViewActivity.class);
                intent.putExtra("url", webview);
                intent.putExtra("show_on_locked_screen", json.optBoolean("show_on_locked_screen"));
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                lastContext.startActivity(intent);
            }

            String sound = json.optString("play_sound", "");
            if (!sound.isEmpty()){
                if (ringtone != null){
                    ringtone.stop();
                }
                if (sound.equals("ringtone")){
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                    ringtone = RingtoneManager.getRingtone(lastContext, notification);
                    ringtone.play();
                } else
                if (sound.equals("notification")){
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    ringtone = RingtoneManager.getRingtone(lastContext, notification);
                    ringtone.play();
                } else
                if (sound.equals("alarm")){
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                    ringtone = RingtoneManager.getRingtone(lastContext, notification);
                    ringtone.play();
                }
            }
            String stop_sound = json.optString("stop_sound", "");
            if (!stop_sound.isEmpty()){
                if (ringtone != null){
                    ringtone.stop();
                }
            }

        } catch (JSONException e){
            ((CustomExceptionHandler)Thread.getDefaultUncaughtExceptionHandler()).reportException(e);
        };
    }


    private Boolean isInternetConnected(Context context){
        ConnectivityManager connectivityManager= (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private Boolean isACPowerConnected(Context context){
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.getApplicationContext().registerReceiver(null, ifilter);
        int chargePlug = -222;
        try {
            chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        } catch (NullPointerException e){

        }
        return chargePlug == BatteryManager.BATTERY_PLUGGED_USB || chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
    }

    private Boolean isDriverModeEnabled(Context context){
        SharedPreferences settings = context.getSharedPreferences(io.drider.car.MainActivity.PREFS_NAME, 0);
        return  settings.getBoolean("DriverActive", true);
    }

    private String DeviceId(){
        SharedPreferences settings = lastContext.getApplicationContext().getSharedPreferences(io.drider.car.MainActivity.PREFS_NAME, 0);
        String id = settings.getString("DriverID", null);
        if (null == id) {
            UUID uuid = UUID.randomUUID();
            id = uuid.toString();
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("DriverID", id);
            editor.commit();
        }
        return id;
    }


    public static String getCookie(String siteName,String CookieName){
        String CookieValue = null;

        CookieManager cookieManager = CookieManager.getInstance();
        String cookies = cookieManager.getCookie(siteName);
        if (cookies == null) {return "";}
        String[] temp=cookies.split("[;]");
        for (String ar1 : temp ){
            if(ar1.contains(CookieName)){
                String[] temp1=ar1.split("[=]");
                CookieValue = temp1[1];
            }
        }
        return CookieValue;
    }

    private void setNotification(){
        String message = "Пошук пасажира";
        int icon = R.mipmap.ic_stat_drider;
        long when = System.currentTimeMillis();
        NotificationManager notificationManager = (NotificationManager) lastContext.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(icon, message, when);
        String title = lastContext.getString(R.string.app_name);
        Intent notificationIntent = new Intent(lastContext, MainActivity.class);
        // set intent so it does not start a new activity
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent =
                PendingIntent.getActivity(lastContext, 0, notificationIntent, 0);
        notification.setLatestEventInfo(lastContext, title, message, intent);
//        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.flags |= Notification.FLAG_ONGOING_EVENT ;//| Notification.FLAG_FOREGROUND_SERVICE  ;
//        notification.flags |= Notification.FLAG_NO_CLEAR ;
//        notification.flags |=Notification.FLAG_FOREGROUND_SERVICE;
        notificationManager.notify(0, notification);
    }
}
