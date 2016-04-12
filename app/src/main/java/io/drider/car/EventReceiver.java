package io.drider.car;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.util.Calendar;

/**
 * Created by devel on 6/4/15.
 */
public class EventReceiver extends BroadcastReceiver {
    static Long onPower;
    static Long offPower;

    // callbacks section
    public void onReceive(Context context, Intent intent) {
        DriverService.singleton(context).onChange(context);
    }



}
