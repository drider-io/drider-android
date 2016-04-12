package io.drider.car;

/**
 * Created by devel on 7/26/15.
 */
import android.webkit.CookieManager;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides a synchronization point between the webview cookie store and OkHttpClient cookie store
 *
 * @author Justin Thomas
 */
public final class WebviewCookieHandler extends CookieHandler {
    private CookieManager webviewCookieManager = CookieManager.getInstance();

    @Override
    public Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders) throws IOException {
        String url = uri.toString();
        String cookieValue = this.webviewCookieManager.getCookie(url);

        Map<String, List<String>> cookies = new HashMap<>();
        if (cookieValue!=null) {
            cookies.put("Cookie", Arrays.asList(cookieValue));
        }

        return cookies;
    }

    @Override
    public void put(URI uri, Map<String, List<String>> responseHeaders) throws IOException {
        String url = uri.toString();

        for(String header : responseHeaders.keySet()){
            if(header.equalsIgnoreCase("Set-Cookie") || header.equalsIgnoreCase("Set-Cookie2")){
                for(String value : responseHeaders.get(header)){
                    this.webviewCookieManager.setCookie(url, value);
                }
            }
        }
    }
}