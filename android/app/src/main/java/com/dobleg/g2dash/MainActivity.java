package com.dobleg.g2dash;

import android.Manifest;
import android.location.Address;
import android.location.Geocoder;
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
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
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
    private MapView nativeMapView;
    private GoogleMap nativeGoogleMap;
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
    private final PhoneGps phoneGps = new PhoneGps();
    private boolean foreground, expanded, mapStarted, preferWeb, tilesReady;
    private boolean followMap = true;
    private boolean webFailed, webReady;
    private int webLoadId;
    private android.widget.TextView webMapStatus;
    private LatLng lastFix, lastWebFix;
    private long lastWebAt;
    private long phoneAt;

    private com.google.android.gms.maps.model.Marker phoneMarker;
    private String mapQuery = "";
    private FrameLayout.LayoutParams inlineBounds;
    private boolean nativeLocationPending = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        restoredInstance = savedInstanceState != null;
        showImmersive();

        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true);
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

        if (isNativeMapsConfigured()) {
            nativeMapView = new MapView(this);
            nativeMapView.setVisibility(View.GONE);
            root.addView(nativeMapView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
            nativeMapView.onCreate(savedInstanceState);
            nativeMapView.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap map) {
                    nativeGoogleMap = map;
                    map.setOnMapLoadedCallback(() -> tilesReady = true);
                    map.setOnCameraMoveStartedListener(reason -> {
                        if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) followMap = false;
                    });
                    map.getUiSettings().setZoomControlsEnabled(true);
                    map.getUiSettings().setCompassEnabled(true);
                    map.getUiSettings().setMapToolbarEnabled(false);
                    if (!usingPhone() && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        try { map.setMyLocationEnabled(true); } catch (SecurityException ignored) { }
                    }
                    if (mapStarted && !preferWeb) {
                        renderMap();
                        if (!mapQuery.isEmpty()) centerNativeMap(mapQuery);
                        else if (lastFix != null) map.moveCamera(CameraUpdateFactory.newLatLngZoom(lastFix, 16));
                    }
                }
            });
        }

        webMapStatus = new android.widget.TextView(this);
        webMapStatus.setGravity(Gravity.CENTER);
        webMapStatus.setTextSize(15);
        webMapStatus.setTextColor(Color.WHITE);
        webMapStatus.setBackgroundColor(Color.rgb(25,42,51));
        webMapStatus.setPadding(16,8,16,8);
        webMapStatus.setVisibility(View.GONE);
        webMapStatus.setOnClickListener(v -> { if (webFailed) loadWebMap(); });
        root.addView(webMapStatus, new FrameLayout.LayoutParams(-1,-1));

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
                startPhone();
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
                if (hasMapLocation()) {
                    callback.invoke(origin, true, false);
                } else {
                    pendingGeo = callback;
                    pendingGeoOrigin = origin;
                    requestPermissions(
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
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
                    picker.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, params.getMode()==FileChooserParams.MODE_OPEN_MULTIPLE);
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
        mapWebView.setOnTouchListener((view,event) -> {
            if (event.getActionMasked() == android.view.MotionEvent.ACTION_MOVE) followMap = false;
            return false;
        });
        mapWebView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view,String url) {
                inspectGooglePage(webLoadId);
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest req,
                    android.webkit.WebResourceError error) {
                if (!req.isForMainFrame()) return;
                markWebFailure();
            }
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
                if (usingPhone() || !isMapOrigin(origin)) {
                    callback.invoke(origin, false, false);
                    return;
                }
                if (hasMapLocation()) {
                    callback.invoke(origin, true, false);
                } else {
                    pendingGeo = callback;
                    pendingGeoOrigin = origin;
                    requestPermissions(
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
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

    private boolean isNativeMapsConfigured() {
        return BuildConfig.GOOGLE_MAPS_API_KEY != null
                && !BuildConfig.GOOGLE_MAPS_API_KEY.trim().isEmpty();
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

    private boolean hasMapLocation() {
        return checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
            || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;
    }

    private boolean usingPhone() {
        return !getPreferences(MODE_PRIVATE).getString("gpsHost", "").isEmpty();
    }

    private void renderMap() {
        boolean visible = expanded || inlineBounds != null;
        boolean nativeMode = nativeMapView != null && !preferWeb;
        View surface = nativeMode ? nativeMapView : mapWebView;
        if (nativeMapView != null && !nativeMode) nativeMapView.setVisibility(View.GONE);
        if (nativeMode) mapWebView.setVisibility(View.GONE);
        closeMapButton.setVisibility(expanded ? View.VISIBLE : View.GONE);
        if (!visible) { surface.setVisibility(View.GONE); webMapStatus.setVisibility(View.GONE); return; }
        surface.setLayoutParams(expanded ? new FrameLayout.LayoutParams(-1, -1) : inlineBounds);
        surface.setVisibility(View.VISIBLE);
        webMapStatus.setLayoutParams(surface.getLayoutParams());
        webMapStatus.setVisibility(!nativeMode && !webReady ? View.VISIBLE : View.GONE);
        if (mapStarted) return;
        mapStarted = true;
        if (!nativeMode) loadWebMap();
        else root.postDelayed(() -> {
            if (!tilesReady && !isFinishing() && !isDestroyed()) {
                preferWeb = true;
                renderMap();
                loadWebMap();
            }
        }, 12000);
    }

    private void markWebFailure() {
        webFailed = true;
        webReady = false;
        webMapStatus.setText("Google Maps no cargó.\nToca para reintentar o usa Ruta del celular.");
        webMapStatus.setVisibility(mapWebView.getVisibility());
        webView.evaluateJavascript("window.G2LiveMap?.mapError()",null);
    }

    private void inspectGooglePage(int id) {
        if (id!=webLoadId || isFinishing() || isDestroyed()) return;
        String probe="(function(){var c=document.querySelectorAll('canvas,img[src*=tile],img[src*=mapsvt],img[src*=khms],"
                + "[role=application],#scene,[aria-label*=Zoom],form[action*=consent]');"
                + "for(var i=0;i<c.length;i++){var b=c[i].getBoundingClientRect();if(b.width>8&&b.height>8)return true;}"
                + "return location.hostname==='consent.google.com';})()";
        mapWebView.evaluateJavascript(probe, result -> {
            if (id!=webLoadId || isDestroyed() || !"true".equals(result)) return;
            webReady=true; webFailed=false;
            webMapStatus.setVisibility(View.GONE);
            webView.evaluateJavascript("window.G2LiveMap?.mapLoaded()",null);
        });
    }

    private void loadWebMap() {
        final int id=++webLoadId;
        boolean alreadyVisible=webReady;
        webReady=false;
        webMapStatus.setText("Cargando Google Maps…");
        webMapStatus.setVisibility(alreadyVisible ? View.GONE : mapWebView.getVisibility());
        for(int delay:new int[]{1500,4000,8500,14000,22000,35000}) {
            root.postDelayed(() -> { if (id==webLoadId && !webReady) inspectGooglePage(id); },delay);
        }
        root.postDelayed(() -> { if (id==webLoadId && !webReady && !isDestroyed()) markWebFailure(); },16500);
        String url = "https://www.google.com/maps/@?api=1&map_action=map";
        if (!mapQuery.isEmpty()) url = "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(mapQuery);
        else if (lastFix != null) url += "&zoom=16&center=" + lastFix.latitude + "," + lastFix.longitude;
        lastWebFix = lastFix;
        lastWebAt = android.os.SystemClock.elapsedRealtime();
        webFailed = false;
        mapWebView.loadUrl(url);
    }

    private void position(double lat, double lon, boolean center) {
        if (Double.isNaN(lat) || Double.isNaN(lon) || Math.abs(lat)>90 || Math.abs(lon)>180) return;
        boolean first = lastFix == null;
        lastFix = new LatLng(lat, lon);
        if (center) { followMap = true; mapQuery = ""; }
        if (nativeGoogleMap != null && !preferWeb) {
            if (usingPhone()) {
                if (phoneMarker == null) phoneMarker = nativeGoogleMap.addMarker(new MarkerOptions().position(lastFix).title("GPS del celular"));
                else phoneMarker.setPosition(lastFix);
            } else if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                nativeGoogleMap.setMyLocationEnabled(true);
            }
            if (followMap && mapQuery.isEmpty()) {
                if (first || center) nativeGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastFix, 16));
                else nativeGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(lastFix));
            }
        } else if ((nativeMapView == null || preferWeb) && mapStarted && mapQuery.isEmpty()
                && (first || center || (webReady && followMap && lastWebFix != null
                    && android.os.SystemClock.elapsedRealtime()-lastWebAt >= 8000
                    && (Math.abs(lastWebFix.latitude-lat)+Math.abs(lastWebFix.longitude-lon) > .00018)))) {
            loadWebMap();
        }
    }

    private void showMapOverlay(String destination) {
        String query = destination == null ? "" : destination.trim();
        if (query.isEmpty()) expanded = true;
        else { mapQuery = query; followMap = false; }
        renderMap();
        if (!query.isEmpty()) {
            if (nativeGoogleMap != null && !preferWeb) centerNativeMap(query);
            else loadWebMap();
        } else if (webFailed) loadWebMap();
    }

    private void closeMapOverlay() {
        expanded = false;
        renderMap();
        webView.requestFocus();
    }

    private void startPhone() {
        phoneGps.stop();
        if (!foreground || !pageReady || !usingPhone()) return;
        phoneGps.start(getPreferences(MODE_PRIVATE).getString("gpsHost",""),
            getPreferences(MODE_PRIVATE).getInt("gpsPort", 11123), (lat, lon, speed, status) -> {
                boolean valid = "GPS del celular".equals(status);
                if (valid) { phoneAt=android.os.SystemClock.elapsedRealtime(); position(lat, lon, false); }
                webView.evaluateJavascript("window.G2LiveMap?.phoneUpdate(" + valid + "," +
                        (Double.isNaN(speed) || Double.isInfinite(speed) ? -1 : speed) + "," + JSONObject.quote(status) + ")", null);
            });
    }

    private void locationSettings() {
        android.widget.EditText endpoint = new android.widget.EditText(this);
        endpoint.setSingleLine(true);
        endpoint.setHint("192.168.1.25:11123");
        if (usingPhone()) endpoint.setText(getPreferences(MODE_PRIVATE).getString("gpsHost","") + ":" +
                getPreferences(MODE_PRIVATE).getInt("gpsPort",11123));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Fuente de ubicación")
                .setMessage("Para usar el GPS del celular, activa un emisor NMEA TCP con mensajes RMC en el teléfono. Conecta ambos a la misma red privada e introduce la IP y el puerto que muestra el emisor.")
                .setView(endpoint).setPositiveButton("GPS del celular", null)
                .setNeutralButton("GPS del estéreo", (d,w) -> changeSource("",0))
                .setNegativeButton("Cancelar", null).create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String[] parts = endpoint.getText().toString().trim().split(":", -1);
            int port = 0;
            try { if (parts.length == 2) port = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}
            if (port < 1 || port > 65535 || !PhoneGps.localAddress(parts[0])) {
                endpoint.setError("Escribe una IPv4 privada y un puerto del 1 al 65535"); return;
            }
            changeSource(parts[0],port);
            dialog.dismiss();
        });
    }

    private void changeSource(String host, int port) {
        getPreferences(MODE_PRIVATE).edit().putString("gpsHost",host).putInt("gpsPort",port).apply();
        lastFix = null; followMap = true; mapQuery = "";
        if (phoneMarker != null) { phoneMarker.remove(); phoneMarker = null; }
        if (nativeGoogleMap != null) nativeGoogleMap.setMyLocationEnabled(false);
        webView.evaluateJavascript("window.G2LiveMap?.sourceChanged()",null);
        startPhone();
    }

    private void centerNativeMap(String query) {
        if (nativeGoogleMap == null) return;
        if (query == null || query.trim().isEmpty()) {
            nativeGoogleMap.clear();
            return;
        }
        final String term = query.trim();
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> matches = geocoder.getFromLocationName(term, 1);
                if (matches == null || matches.isEmpty()) return;
                Address address = matches.get(0);
                final double lat = address.getLatitude();
                final double lng = address.getLongitude();
                runOnUiThread(() -> {
                    if (nativeGoogleMap == null || !term.equals(mapQuery)) return;
                    LatLng point = new LatLng(lat, lng);
                    nativeGoogleMap.clear();
                    phoneMarker = null;
                    nativeGoogleMap.addMarker(new MarkerOptions().position(point).title(term));
                    nativeGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 15f));
                });
            } catch (Exception ignored) {
                runOnUiThread(() -> Toast.makeText(this,
                        "No se pudo centrar el destino; puedes mover el mapa",
                        Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void zoomNativeMap(float delta) {
        if (Float.isNaN(delta) || Float.isInfinite(delta) || nativeGoogleMap == null) return;
        float bounded = Math.max(-2f, Math.min(2f, delta));
        nativeGoogleMap.animateCamera(CameraUpdateFactory.zoomBy(bounded));
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
                openCarLink(false);
                break;
            case "choose-carlink":
                openCarLink(true);
                break;
            case "radio":
            case "apps":
                showInstalledApps();
                break;
            default:
                Toast.makeText(this, "Función pendiente", Toast.LENGTH_SHORT).show();
        }
    }


    private void openCarLink(boolean choose) {
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> apps = new ArrayList<>(pm.queryIntentActivities(query, 0));
        java.util.Iterator<ResolveInfo> iterator = apps.iterator();
        while (iterator.hasNext()) {
            ResolveInfo a = iterator.next();
            if (a.activityInfo == null || a.activityInfo.packageName.equals(getPackageName())) iterator.remove();
        }
        String saved = getPreferences(MODE_PRIVATE).getString("carlinkApp", "");
        ResolveInfo known = null;
        for (ResolveInfo info : apps) {
            String pkg = info.activityInfo.packageName;
            if (!choose && pkg.equals(saved)) { launchValidatedApp(pkg,info.activityInfo.name); return; }
            String label = info.loadLabel(pm).toString().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");
            if (pkg.equals("com.syu.carlink") || (known == null && label.matches("carlink(20|2)?"))) known = info;
        }
        if (!choose && known != null) { launchValidatedApp(known.activityInfo.packageName,known.activityInfo.name); return; }
        apps.sort(Comparator.comparing(a -> a.loadLabel(pm).toString()));
        CharSequence[] labels = new CharSequence[apps.size()];
        for (int i=0;i<apps.size();i++) labels[i]=apps.get(i).loadLabel(pm);
        new AlertDialog.Builder(this).setTitle("Elige la app CarLink de tu estéreo").setItems(labels,(d,index)->{
            ResolveInfo a=apps.get(index);
            getPreferences(MODE_PRIVATE).edit().putString("carlinkApp",a.activityInfo.packageName).apply();
            launchValidatedApp(a.activityInfo.packageName,a.activityInfo.name);
        }).setNegativeButton("Cancelar",null).show();
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
        @JavascriptInterface public void retryWidgetMap() {runOnUiThread(() -> {
            if (nativeMapView == null || preferWeb) loadWebMap();
        });}
        @JavascriptInterface public boolean usesPhoneGps() { return usingPhone(); }
        @JavascriptInterface public void chooseLocationSource() { runOnUiThread(() -> locationSettings()); }
        @JavascriptInterface public void centerFix(double lat, double lon, boolean center) {
            runOnUiThread(() -> { if (!usingPhone()) position(lat,lon,center); });
        }
        @JavascriptInterface public void recenterPhone() {
            runOnUiThread(() -> { if (usingPhone() && lastFix != null && android.os.SystemClock.elapsedRealtime()-phoneAt < 30000) position(lastFix.latitude,lastFix.longitude,true); });
        }
        @JavascriptInterface public void mapBounds(double x,double y,double w,double h) {
            runOnUiThread(() -> {
                inlineBounds = null;
                if (w > 0 && h > 0 && x >= 0 && y >= 0 && x+w <= 1.002 && y+h <= 1.002) {
                    int width = webView.getWidth(), height = webView.getHeight();
                    inlineBounds = new FrameLayout.LayoutParams(Math.max(1,(int)(w*width)), Math.max(1,(int)(h*height)));
                    inlineBounds.leftMargin = (int)Math.round(x*width);
                    inlineBounds.topMargin = (int)Math.round(y*height);
                }
                renderMap();
            });
        }
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
        public boolean hasNativeMaps() {
            return MainActivity.this.isNativeMapsConfigured();
        }
        @JavascriptInterface
        public void mapZoom(float delta) {
            runOnUiThread(() -> MainActivity.this.zoomNativeMap(delta));
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
        if (requestCode == LOCATION_REQUEST) {
            boolean granted = hasMapLocation();
            if (pendingGeo != null) {
                pendingGeo.invoke(pendingGeoOrigin, granted, false);
                pendingGeo = null;
                pendingGeoOrigin = null;
            }
            if (granted && nativeLocationPending && nativeGoogleMap != null) {
                try { nativeGoogleMap.setMyLocationEnabled(true); } catch (SecurityException ignored) { }
            }
            nativeLocationPending = false;
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
    protected void onSaveInstanceState(Bundle outState) {
        if (nativeMapView != null) nativeMapView.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (expanded) {
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
        foreground = true;
        startPhone();
        if (nativeMapView != null) nativeMapView.onStart();
        if (mediaBridge != null) mediaBridge.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        showImmersive();
        if (webView != null) webView.onResume();
        if (mapWebView != null) mapWebView.onResume();
        if (nativeMapView != null) nativeMapView.onResume();
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
        if (nativeMapView != null) nativeMapView.onPause();
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
        foreground = false;
        phoneGps.stop();
        if (nativeMapView != null) nativeMapView.onStop();
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
        if (nativeMapView != null) {
            nativeMapView.onDestroy();
        }
        super.onDestroy();
    }
}
