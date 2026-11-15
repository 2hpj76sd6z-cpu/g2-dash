package com.dobleg.g2dash;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.webkit.WebViewAssetLoader;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * G² Dash: local/offline dashboard hosted by native Android.
 * May be chosen as HOME by the user; never changes HOME automatically. No telemetry.
 */
public class MainActivity extends Activity {
    private static final String LOCAL_ORIGIN = "https://appassets.androidplatform.net";
    private static final String HOME_URL = LOCAL_ORIGIN + "/assets/www/index.html";
    private static final int LOCATION_REQUEST = 20;
    private static final int FILE_REQUEST = 21;

    private WebView webView;
    private G2MediaBridge mediaBridge;
    private GeolocationPermissions.Callback pendingGeo;
    private String pendingGeoOrigin;
    private android.webkit.ValueCallback<Uri[]> uploadCallback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showImmersive();

        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView = new WebView(this);
        setContentView(webView);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setGeolocationEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(true);

        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.addJavascriptInterface(new AndroidBridge(), "G2Android");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
                return assetLoader.shouldInterceptRequest(req.getUrl());
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                if (LOCAL_ORIGIN.equals(u.getScheme() + "://" + u.getHost())) {
                    return false;
                }
                // External websites are NEVER loaded inside the privileged WebView.
                if ("https".equals(u.getScheme()) || "http".equals(u.getScheme())) {
                    openExternal(new Intent(Intent.ACTION_VIEW, u));
                }
                return true;
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(
                    String origin, GeolocationPermissions.Callback callback) {
                if (!LOCAL_ORIGIN.equals(origin)) {
                    callback.invoke(origin, false, false);
                    return;
                }
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    callback.invoke(origin, true, false);
                } else {
                    pendingGeo = callback;
                    pendingGeoOrigin = origin;
                    requestPermissions(
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_REQUEST);
                }
            }
            @Override
            public boolean onShowFileChooser(WebView view,
                    android.webkit.ValueCallback<Uri[]> callback,
                    FileChooserParams params) {
                if (uploadCallback != null) uploadCallback.onReceiveValue(null);
                uploadCallback = callback;
                try {
                    Intent picker = params.createIntent();
                    picker.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(picker, FILE_REQUEST);
                    return true;
                } catch (ActivityNotFoundException ex) {
                    uploadCallback = null;
                    callback.onReceiveValue(null);
                    Toast.makeText(MainActivity.this, "No hay selector de imágenes", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });
        mediaBridge = new G2MediaBridge(this, webView);
        webView.loadUrl(HOME_URL);
    }

    private void showImmersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    private void openExternal(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, "No se encontró una app compatible", Toast.LENGTH_SHORT).show();
        }
    }

    private void openMap(String destination) {
        String query = destination == null ? "" : destination.trim();
        Uri uri = Uri.parse(query.isEmpty()
                ? "https://www.google.com/maps/"
                : "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query));
        openExternal(new Intent(Intent.ACTION_VIEW, uri));
    }

    private void openTarget(String key) {
        switch (key) {
            case "phone":
                openExternal(new Intent(Intent.ACTION_DIAL));
                break;
            case "bluetooth":
                openExternal(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                break;
            case "music":
                try {
                    openExternal(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN,
                            Intent.CATEGORY_APP_MUSIC));
                } catch (Exception ex) {
                    showInstalledApps();
                }
                break;
            case "radio":
            case "apps":
                showInstalledApps();
                break;
            default:
                Toast.makeText(this, "Función pendiente", Toast.LENGTH_SHORT).show();
        }
    }


    private void showInstalledApps() {
        Intent filter = new Intent(Intent.ACTION_MAIN);
        filter.addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> raw = pm.queryIntentActivities(filter, 0);
        List<ResolveInfo> apps = new ArrayList<>();
        for (ResolveInfo info : raw) {
            if (info.activityInfo != null &&
                    !getPackageName().equals(info.activityInfo.packageName)) apps.add(info);
        }
        apps.sort(Comparator.comparing(a -> a.loadLabel(pm).toString().toLowerCase()));
        if (apps.isEmpty()) {
            Toast.makeText(this, "No se encontraron aplicaciones", Toast.LENGTH_SHORT).show();
            return;
        }
        CharSequence[] labels = new CharSequence[apps.size()];
        for (int i = 0; i < apps.size(); i++) labels[i] = apps.get(i).loadLabel(pm);
        new AlertDialog.Builder(this)
                .setTitle("Abrir aplicación")
                .setItems(labels, (dialog, index) -> {
                    ResolveInfo info = apps.get(index);
                    Intent launch = new Intent(Intent.ACTION_MAIN);
                    launch.addCategory(Intent.CATEGORY_LAUNCHER);
                    launch.setClassName(info.activityInfo.packageName, info.activityInfo.name);
                    openExternal(launch);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private class AndroidBridge {
        @JavascriptInterface
        public void openTarget(String key) {
            runOnUiThread(() -> MainActivity.this.openTarget(key));
        }
        @JavascriptInterface
        public void musicControl(String action) {
            runOnUiThread(() -> { if (mediaBridge != null) mediaBridge.control(action); });
        }
        @JavascriptInterface
        public void openMusicAccess() {
            runOnUiThread(() -> { if (mediaBridge != null) mediaBridge.showAccessSettings(); });
        }
        @JavascriptInterface
        public void openMap(String destination) {
            runOnUiThread(() -> MainActivity.this.openMap(destination));
        }
        @JavascriptInterface
        public void openHomeSettings() {
            runOnUiThread(() -> MainActivity.this.openExternal(
                    new Intent(Settings.ACTION_HOME_SETTINGS)));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] perms, int[] results) {
        super.onRequestPermissionsResult(requestCode, perms, results);
        if (requestCode == LOCATION_REQUEST && pendingGeo != null) {
            boolean granted = results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED;
            pendingGeo.invoke(pendingGeoOrigin, granted, false);
            pendingGeo = null;
            pendingGeoOrigin = null;
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == FILE_REQUEST && uploadCallback != null) {
            uploadCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result, data));
            uploadCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        webView.evaluateJavascript("(function(){const s=document.querySelector('#settingsVeil');" +
                "const v=document.querySelector('#vehicleVeil');" +
                "const c=document.querySelector('#conceptVeil');" +
                "if(c&&!c.hidden){if(window.G2Concept)window.G2Concept.close();else c.hidden=true;return true;}" +
                "if(s&&!s.hidden){s.hidden=true;return true;}" +
                "if(v&&!v.hidden){v.hidden=true;return true;}return false;})()",
                value -> {
                    if (!"true".equals(value)) MainActivity.super.onBackPressed();
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mediaBridge != null) mediaBridge.start();
    }

    @Override
    protected void onStop() {
        if (mediaBridge != null) mediaBridge.stop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mediaBridge != null) mediaBridge.destroy();
        if (webView != null) {
            webView.removeJavascriptInterface("G2Android");
            webView.destroy();
        }
        super.onDestroy();
    }
}
