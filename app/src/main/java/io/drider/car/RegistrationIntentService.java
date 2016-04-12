package io.drider.car;

import android.app.IntentService;
import android.content.Intent;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;

/**
 * Created by devel on 7/26/15.
 */
public class RegistrationIntentService extends IntentService {
    private static final String TAG= RegistrationIntentService.class.getSimpleName();
    private final OkHttpClient client = new OkHttpClient();


    public RegistrationIntentService() {
        super(TAG);
        if(!(Thread.getDefaultUncaughtExceptionHandler() instanceof CustomExceptionHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler());
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
//        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            synchronized (TAG) {
                // Initially a network call, to retrieve the token, subsequent calls are local.
                InstanceID instanceID = InstanceID.getInstance(this);
                String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId), GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                Log.i(TAG, "GCM Registration Token: " + token);

                sendRegistrationToServer(token);

                // Subscribe to topic channels, if applicable.
                // e.g. for (String topic : TOPICS) {
                //          GcmPubSub pubSub = GcmPubSub.getInstance(this);
                //          pubSub.subscribe(token, "/topics/" + topic, null);
                //       }

//                sharedPreferences.edit().putBoolean(getString(R.string.pref_key_SENT_TOKEN_TO_SERVER), true).apply();
            }
        } catch (Exception e) {
            ((CustomExceptionHandler)Thread.getDefaultUncaughtExceptionHandler()).reportException(e);
            Log.d(TAG, "Failed to complete token refresh", e);
//            sharedPreferences.edit().putBoolean(getString(R.string.pref_key_SENT_TOKEN_TO_SERVER), false).apply();
        }
        // Notify UI that registration has completed, so the progress indicator can be hidden.
//        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(getString(R.string.intent_name_REGISTRATION_COMPLETE)));
    }

    private void sendRegistrationToServer(String token){
        WebviewCookieHandler cookieManager = new WebviewCookieHandler();
        client.setCookieHandler(cookieManager);
        RequestBody formBody = new FormEncodingBuilder()
                .add("token", token)
                .add("name", Build.MANUFACTURER + " " + Build.MODEL)
                .build();
        Request request = new Request.Builder()
                .url("http://drider.io/api/token/gcm")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException throwable) {
            }

            @Override
            public void onResponse(Response response) throws IOException {
            }
        });
    }

}
