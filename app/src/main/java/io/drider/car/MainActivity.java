package io.drider.car;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class MainActivity extends AppCompatActivity  {

    public static final String PREFS_NAME = "UIPrefs";
    private static final String TAG = "MainActivity";
    private static final long INTERVAL = 1000 * 10;
    private static final long FASTEST_INTERVAL = 1000 * 5;
    Button btnFusedLocation;
    Switch mToggleButton;
    Button mButton;
    TextView mTextView;
    TextView tvLocation;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
//    Location mCurrentLocation;
    String mLastUpdateTime;
    BroadcastReceiver receiver;
    Context mContext;

    TextView iInternet;
    TextView iPower;
    TextView iActive;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        iInternet = (TextView) findViewById(R.id.indicatorInternet);
        iPower = (TextView) findViewById(R.id.indicatorACPower);
        iActive = (TextView) findViewById(R.id.indicatorActive);
        mToggleButton = (Switch) findViewById(R.id.toggleButton);
        mButton = (Button) findViewById(R.id.buttonStats);
        mTextView = (TextView) findViewById(R.id.textView);
        mTextView.setMovementMethod(LinkMovementMethod.getInstance());

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "update received");
                String s = intent.getStringExtra("internet");
                Log.d(TAG, "internet: "+ s);
                if (s != null){
                    setIndicator(iInternet,s);
                }
                s = intent.getStringExtra("power");
                Log.d(TAG, "power: "+ s);
                if (s != null){
                    setIndicator(iPower,s);
                }
                s = intent.getStringExtra("active");
                Log.d(TAG, "active: "+ s);
                if (s != null){
                    setIndicator(iActive,s);
                }

                s = intent.getStringExtra("text");
                Log.d(TAG, "text: "+ s);
                if (s != null){
                    mTextView.setText(Html.fromHtml(s));
                }


                // do something here.
                SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
                Boolean isActive = settings.getBoolean("DriverActive", true);
                mToggleButton.setChecked(isActive);
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver),
                new IntentFilter("UIUpdate")
        );

//        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate ...............................");
//        startService(new Intent(this,LocationService.class));
        //show error dialog if GoolglePlayServices not available


//        mToggleButton.setOnClickListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                updateUI();
//            }
//        });
        mToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG, "button pressed");
                Log.d(TAG, "value:"+isChecked);

                SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("DriverActive", isChecked);
                editor.commit();
                DriverService.singleton(mContext).onChange(mContext);

                // Save the state here
            }
        });
        SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        Boolean isActive = settings.getBoolean("DriverActive", true);
        mToggleButton.setChecked(isActive);

//        tvLocation = (TextView) findViewById(R.id.tvLocation);
//
//        btnFusedLocation = (Button) findViewById(R.id.btnShowLocation);
//        btnFusedLocation.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View arg0) {
//                updateUI();
//            }
//        });

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, WebViewActivity.class);
                intent.putExtra("url", "http://drider.io/");
                startActivity(intent);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart fired ..............");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop fired ..............");
//        mGoogleApiClient.disconnect();
    }

    @Override
    protected void onPause() {
        super.onPause();
//        stopLocationUpdates();
    }


    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void updateUI() {
        Log.d(TAG, "UI update initiated .............");
//        if (null != mCurrentLocation) {
//            String lat = String.valueOf(mCurrentLocation.getLatitude());
//            String lng = String.valueOf(mCurrentLocation.getLongitude());
//            String message = "At Time: " + mLastUpdateTime + "\n" +
//                    "Latitude: " + lat + "\n" +
//                    "Longitude: " + lng + "\n" +
//                    "Accuracy: " + mCurrentLocation.getAccuracy() + "\n" +
//                    "Provider: " + mCurrentLocation.getProvider();
//            tvLocation.setText(message);
//            Log.d(TAG, message);
////            WebSocket conn = mWebSocketClient.getConnection();
////            if (conn != null && conn.isOpen()){
////                mWebSocketClient.send(message);
////            }
//        } else {
//            Log.d(TAG, "location is null ...............");
//        }
    }

    private void setIndicator(TextView i, String status){
        if ("maybe".equals(status)){
            i.setBackgroundResource(R.color.colorMaybe);
        }
        else if (Boolean.valueOf(status))
            i.setBackgroundResource(R.color.colorOn);
        else
            i.setBackgroundResource(R.color.colorOff);
    }
}
