package io.drider.car;

import android.app.Activity;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.facebook.login.LoginManager;

import java.util.Arrays;

/**
 * Created by devel on 9/19/15.
 */
class MyJavaScriptInterface
{
    Activity context;
    public MyJavaScriptInterface(Activity c){
        context = c;
    }


    @JavascriptInterface
    public String login(){
        LoginManager.getInstance().logInWithReadPermissions(context, Arrays.asList("public_profile","email"));
        return "";
    }

}