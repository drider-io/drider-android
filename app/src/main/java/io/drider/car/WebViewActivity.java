package io.drider.car;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class WebViewActivity extends AppCompatActivity  {
    CallbackManager callbackManager;
    private static final String TAG = "WebViewActivity";
    public String intentUrl;
    public WebView webview;
    BroadcastReceiver receiver;
    MenuItem iInternet;
    MenuItem iPower;
    MenuItem iActive;
    MenuItem iStatus;
    Context mContext;
    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // getIntent() should always return the most recent
        setIntent(intent);
        webview.loadUrl(intentUrl);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        //Settings.getApplicationSignature(Context)
        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        // App code
                        Log.d(TAG, "1");
                        sendRegistrationToServer(loginResult.getAccessToken().getToken(), loginResult.getAccessToken().getUserId());
                    }

                    @Override
                    public void onCancel() {
                        // App code
                        Log.d(TAG, "1");
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        // App code
                        Log.d(TAG, "1");
                    }
                });
        setContentView(R.layout.activity_web_view);

//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
//                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
//                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
//                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        webview = (WebView) findViewById(R.id.webView);
        WebSettings webSettings = webview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webview.addJavascriptInterface(new MyJavaScriptInterface(this), "injectedAndroid");
        webview.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        webview.setWebViewClient(new WebViewClient(){
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
//                view.loadUrl("file:///android_asset/custom_error.html");
                Log.e(TAG, "error");

            }
            public void onPageFinished(WebView view, String url) {
                CookieSyncManager.getInstance().sync();
                // do your stuff here
                Log.e(TAG, "page loaded");
                if (url.equals("file:///android_asset/loading.html")){
                    Map<String, String> noCacheHeaders = new HashMap<String, String>(2);
//                    noCacheHeaders.put("Pragma", "no-cache");
//                    noCacheHeaders.put("Cache-Control", "no-cache");
//                    view.loadUrl(intentUrl,noCacheHeaders);
                }
            }
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
//                if (url != null && (url.startsWith("https://secure.xsolla.com") || url.startsWith("http://m.odnoklassniki.ru/group/52656150347875") )) {
//                    view.getContext().startActivity(
//                            new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
//                    return true;
//                } else
//                if(url != null && (url.contains(SERVER_URL) || url.contains("android_asset"))) {
//                    view.loadUrl(url);
//                    return false;
//                }
                Log.i("test","deny to load:"+url);
                return false;
            }

        });




        Intent intent = getIntent();
        intentUrl = intent.getStringExtra("url");
        if (null == intentUrl) intentUrl = "http://" + DriverService.HOST + "/account";
        webview.loadUrl(intentUrl);
        if (intent.getBooleanExtra("show_on_locked_screen", false)){
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }


//        webview.loadUrl("file:///android_asset/loading.html");


//        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile"));


    }

    @Override
    protected void onPause() {
        super.onPause();

        // Logs 'app deactivate' App Event.
        AppEventsLogger.deactivateApp(this);
    }

    @Override
    public void onResume() {
        super.onResume();

    // Logs 'install' and 'app activate' App Events.
        AppEventsLogger.activateApp(this);
    }

    @Override
    public void onBackPressed() {
        if(webview.canGoBack()){
            webview.goBack();
        }else{
            webview.reload();
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_web_view, menu);
        iPower = menu.findItem(R.id.menu_power);
        iInternet = menu.findItem(R.id.menu_internet);
        iActive = menu.findItem(R.id.menu_active);
        iStatus = menu.findItem(R.id.menu_status);
        setUIReceiver();
        DriverService.singleton(this).onChange(this);
        ActionBar ab = getSupportActionBar();
        if (null != ab){
            ab.setDisplayShowHomeEnabled(true);
            ab.setIcon(R.mipmap.drider);
            ab.setDisplayShowTitleEnabled(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu){

        return super.onPrepareOptionsMenu(menu);
    }


    private void setUIReceiver(){

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
                SharedPreferences settings = getApplicationContext().getSharedPreferences(MainActivity.PREFS_NAME, 0);
                Boolean isActive = settings.getBoolean("DriverActive", true);
                if (isActive)
                    iStatus.setTitle(R.string.drive_on);
                 else
                    iStatus.setTitle(R.string.drive_off);
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver),
                new IntentFilter("UIUpdate")
        );
    }

    private void setIndicator(MenuItem i, String status){
        if (null == i) return;
        if ("maybe".equals(status)){
            i.setIcon(R.mipmap.yellow);
        }
        else if (Boolean.valueOf(status))
            i.setIcon(R.mipmap.green);
        else
            i.setIcon(R.mipmap.red);
    }
    private void sendRegistrationToServer(String token, String uid){
        WebviewCookieHandler cookieManager = new WebviewCookieHandler();
        client.setCookieHandler(cookieManager);
        RequestBody formBody = new FormEncodingBuilder()
                .add("token", token)
                .add("uid", uid)
                .build();
        Request request = new Request.Builder()
                .url("http://" + DriverService.HOST + "/api/token/facebook")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException throwable) {
                Log.d(TAG, "1");
            }

            @Override
            public void onResponse(Response response) throws IOException {
                Log.d(TAG, "1");
                webview.post(new Runnable() {
                    @Override
                    public void run() {
                        webview.loadUrl("http://" + DriverService.HOST + "/account");
                    }
                });
                SharedPreferences settings = getApplicationContext().getSharedPreferences(MainActivity.PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("DriverActive", true);
                editor.commit();
                DriverService.singleton(mContext).onChange(mContext);

            }
        });
    }
}
