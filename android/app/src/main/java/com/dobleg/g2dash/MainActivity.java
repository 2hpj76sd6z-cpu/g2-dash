package com.dobleg.g2dash;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;
import android.os.Bundle;
import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.view.Gravity;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.webkit.WebViewAssetLoader;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * G² Dash: local/offline dashboard hosted by native Android.
 * May be chosen as HOME by the user; never changes HOME automatically. No telemetry.
 */
public class MainActivity extends Activity {
    private static final String LOCAL_ORIGIN = "https://appassets.androidplatform.net";
    private static final String HOME_URL = LOCAL_ORIGIN + "/assets/www/index.html";
    private static final int LOCATION_REQUEST = 20;
    private static final int FILE_REQUEST = 21;
    private static final String AIRPLAY_PACKAGE = "io.github.jqssun.airplay";

    private WebView webView;
    private WebView mapWebView;
    private Button closeMapButton;
    private FrameLayout root;
    private G2MediaBridge mediaBridge;
    // Activity lifecycle, NOT the vehicle ignition/ACC signal.
    private boolean pageReady = false;
    private boolean restoredInstance = false;
    private boolean wasPaused = false;
    private int resumeCount = 0;
    private GeolocationPermissions.Callback pendingGeo;
    private String pendingGeoOrigin;
    private android.webkit.ValueCallback<Uri[]> uploadCallback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        restoredInstance = savedInstanceState != null;
        showImmersive();

        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView = new WebView(this);
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        root.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        mapWebView = new WebView(this);
        mapWebView.setVisibility(View.GONE);
        root.addView(mapWebView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        closeMapButton = new Button(this);
        closeMapButton.setText("×  Cerrar mapa");
        closeMapButton.setAllCaps(false);
        closeMapButton.setTextColor(Color.WHITE);
        closeMapButton.setTextSize(14f);
        closeMapButton.setBackgroundColor(Color.rgb(20, 32, 48));
        closeMapButton.setPadding(24, 8, 24, 8);
        closeMapButton.setVisibility(View.GONE);
        closeMapButton.setOnClickListener(v -> closeMapOverlay());
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        closeParams.gravity = Gravity.TOP | Gravity.END;
        closeParams.setMargins(0, 24, 24, 0);
        root.addView(closeMapButton, closeParams);
        setContentView(root);
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
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!HOME_URL.equals(url)) return;
                pageReady = true;
                emitLifecycle(restoredInstance ? "restored" : "cold_start");
                restoredInstance = false;
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(
                    String origin, GeolocationPermissions.Callback callback) {
                if (!LOCAL_ORIGIN.equals(origin) && !isMapOrigin(origin)) {
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
        WebSettings mapSettings = mapWebView.getSettings();
        mapSettings.setJavaScriptEnabled(true);
        mapSettings.setDomStorageEnabled(true);
        mapSettings.setAllowFileAccess(false);
        mapSettings.setAllowContentAccess(false);
        mapSettings.setSupportMultipleWindows(false);
        mapSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        mapSettings.setBuiltInZoomControls(false);
        mapSettings.setDisplayZoomControls(false);
        mapSettings.setGeolocationEnabled(true);
        mapSettings.setMediaPlaybackRequiresUserGesture(true);
        mapWebView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mapWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                if (isMapUrl(u)) return false;
                if ("https".equalsIgnoreCase(u.getScheme()) || "http".equalsIgnoreCase(u.getScheme())) {
                    openExternal(new Intent(Intent.ACTION_VIEW, u));
                }
                return true;
            }
        });
        mapWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(
                    String origin, GeolocationPermissions.Callback callback) {
                if (!isMapOrigin(origin)) {
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
        });

        mediaBridge = new G2MediaBridge(this, webView);
        webView.loadUrl(HOME_URL);
    }

    /** LOCAL event only: no ignition, ACC or vehicle power signal accessed. */
    private void emitLifecycle(String phase) {
        if (!pageReady || webView == null) return;
        String script = "window.G2AndroidLifecycle && window.G2AndroidLifecycle("
                + JSONObject.quote(phase) + "," + resumeCount + ",false);";
        webView.evaluateJavascript(script, null);
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

    private boolean isMapHost(String host) {
        if (host == null) return false;
        String h = host.toLowerCase(Locale.US);
        return h.equals("google.com") || h.endsWith(".google.com")
                || h.equals("googleusercontent.com") || h.endsWith(".googleusercontent.com")
                || h.equals("gstatic.com") || h.endsWith(".gstatic.com");
    }

    private boolean isMapOrigin(String origin) {
        try {
            Uri parsed = Uri.parse(origin);
            return "https".equalsIgnoreCase(parsed.getScheme()) && isMapHost(parsed.getHost());
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isMapUrl(Uri uri) {
        return uri != null && "https".equalsIgnoreCase(uri.getScheme())
                && isMapHost(uri.getHost());
    }

    private void showMapOverlay(String destination) {
        if (mapWebView == null) return;
        String query = destination == null ? "" : destination.trim();
        Uri uri = Uri.parse(query.isEmpty()
                ? "https://www.google.com/maps/"
                : "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query));
        webView.setVisibility(View.GONE);
        mapWebView.setVisibility(View.VISIBLE);
        closeMapButton.setVisibility(View.VISIBLE);
        mapWebView.loadUrl(uri.toString());
    }

    private void closeMapOverlay() {
        if (mapWebView == null || mapWebView.getVisibility() != View.VISIBLE) return;
        mapWebView.stopLoading();
        mapWebView.setVisibility(View.GONE);
        closeMapButton.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        webView.requestFocus();
    }

    private void openMap(String destination) {
        showMapOverlay(destination);
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

    /**
     * Apps solicitadas por el usuario: solo actividades visibles como LAUNCHER.
     * Los íconos se comprimen a 48 px y se entregan exclusivamente al WebView local.
     * Sin QUERY_ALL_PACKAGES ni conexión con servidores.
     */
    private String listInstalledAppsJson() {
        JSONArray output = new JSONArray();
        PackageManager pm = getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> results = pm.queryIntentActivities(query, 0);
        results.sort(Comparator.comparing(a -> a.loadLabel(pm).toString().toLowerCase()));
        Set<String> seen = new HashSet<>();
        int count = 0;
        for (ResolveInfo info : results) {
            if (info.activityInfo == null) continue;
            String pkg = info.activityInfo.packageName;
            String activity = info.activityInfo.name;
            if (pkg == null || activity == null || getPackageName().equals(pkg)
                    || !seen.add(pkg) || count >= 90) continue;
            try {
                JSONObject row = new JSONObject();
                row.put("name", info.loadLabel(pm).toString());
                row.put("packageName", pkg);
                row.put("activityName", activity);
                try {
                    Drawable drawable = info.loadIcon(pm);
                    if (drawable != null) {
                        Bitmap bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(bitmap);
                        drawable.setBounds(0, 0, 48, 48);
                        drawable.draw(canvas);
                        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes);
                        row.put("icon", "data:image/png;base64," + Base64.encodeToString(
                                bytes.toByteArray(), Base64.NO_WRAP));
                        bitmap.recycle();
                    }
                } catch (Exception ignored) {
                    // La app sigue accesible aunque el paquete no exponga ícono.
                }
                output.put(row);
                count++;
            } catch (Exception ignored) { /* Ignorar componente defectuoso. */ }
        }
        return output.toString();
    }

    private void launchValidatedApp(String pkg, String activity) {
        if (pkg == null || activity == null) return;
        Intent query = new Intent(Intent.ACTION_MAIN);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager pm = getPackageManager();
        for (ResolveInfo info : pm.queryIntentActivities(query, 0)) {
            if (info.activityInfo == null) continue;
            if (pkg.equals(info.activityInfo.packageName)
                    && activity.equals(info.activityInfo.name)) {
                Intent launch = new Intent(Intent.ACTION_MAIN);
                launch.addCategory(Intent.CATEGORY_LAUNCHER);
                launch.setClassName(pkg, activity);
                openExternal(launch);
                return;
            }
        }
        Toast.makeText(this, "Aplicación no disponible", Toast.LENGTH_SHORT).show();
    }

    // El receptor AirPlay es un APK independiente. La instalación y su activación
    // siempre las decide el usuario: nunca lo inicia en segundo plano.
    private boolean isAirPlayInstalled() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false;
        try {
            return getPackageManager().getLaunchIntentForPackage(AIRPLAY_PACKAGE) != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String getAirPlaySupportJson() {
        JSONObject info = new JSONObject();
        try {
            info.put("supported", Build.VERSION.SDK_INT >= Build.VERSION_CODES.N);
            info.put("installed", isAirPlayInstalled());
            info.put("androidVersion", Build.VERSION.RELEASE);
        } catch (org.json.JSONException ignored) { /* Solo se envían datos locales. */ }
        return info.toString();
    }

    private void openAirPlayReceiver() {
        if (!isAirPlayInstalled()) {
            Toast.makeText(this, "Primero instala un receptor AirPlay", Toast.LENGTH_LONG).show();
            return;
        }
        Intent launch = getPackageManager().getLaunchIntentForPackage(AIRPLAY_PACKAGE);
        if (launch != null) openExternal(launch);
    }

    private void openAirPlayInstall() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Toast.makeText(this, "AirPlay Receiver requiere Android 7 o posterior", Toast.LENGTH_LONG).show();
            return;
        }
        // Se abre la ficha oficial, NO se descarga ni instala el APK de manera automática.
        openExternal(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=" + AIRPLAY_PACKAGE)));
    }

    private class AndroidBridge {
        @JavascriptInterface
        public String getAirPlaySupport() {
            return getAirPlaySupportJson();
        }
        @JavascriptInterface
        public void openAirPlayReceiver() {
            runOnUiThread(() -> MainActivity.this.openAirPlayReceiver());
        }
        @JavascriptInterface
        public void openAirPlayInstall() {
            runOnUiThread(() -> MainActivity.this.openAirPlayInstall());
        }
        @JavascriptInterface
        public String getInstalledApps() {
            return listInstalledAppsJson();
        }
        @JavascriptInterface
        public void launchInstalledApp(String packageName, String activityName) {
            runOnUiThread(() -> MainActivity.this.launchValidatedApp(packageName, activityName));
        }
        @JavascriptInterface
        public void openTarget(String key) {
            runOnUiThread(() -> MainActivity.this.openTarget(key));
        }
        @JavascriptInterface
        public void musicControl(String action) {
            runOnUiThread(() -> { if (mediaBridge != null) mediaBridge.control(action); });
        }
        @JavascriptInterface
        public void musicSeek(long positionMs) {
            runOnUiThread(() -> { if (mediaBridge != null) mediaBridge.seekTo(positionMs); });
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
        if (mapWebView != null && mapWebView.getVisibility() == View.VISIBLE) {
            closeMapOverlay();
            return;
        }
        webView.evaluateJavascript("(function(){const s=document.querySelector('#settingsVeil');" +
                "const v=document.querySelector('#vehicleVeil');" +
                "const c=document.querySelector('#conceptVeil');" +
                "const ap=document.querySelector('#airplayVeil');" +
                "if(ap&&!ap.hidden){ap.hidden=true;return true;}" +
                "const m=document.querySelector('#musicDetailVeil');" +
                "if(m&&!m.hidden){m.hidden=true;return true;}" +
                "const a=document.querySelector('#appsVeil');" +
                "if(a&&!a.hidden){a.hidden=true;return true;}" +
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
    protected void onResume() {
        super.onResume();
        showImmersive();
        if (webView != null) webView.onResume();
        if (mapWebView != null) mapWebView.onResume();
        if (wasPaused) {
            wasPaused = false;
            resumeCount++;
            emitLifecycle("resume");
        }
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        if (mapWebView != null) mapWebView.onPause();
        wasPaused = true;
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) showImmersive();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        showImmersive();
        emitLifecycle("home");
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
        if (mapWebView != null) {
            mapWebView.destroy();
        }
        super.onDestroy();
    }
}
