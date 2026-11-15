package com.dobleg.g2dash;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInstaller;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Checks public GitHub releases every 12 hours while G² Dash is open. */
public final class G2UpdateManager {
    private static final String RELEASE_API =
            "https://api.github.com/repos/2hpj76sd6z-cpu/g2-dash/releases/latest";
    private static final String RELEASE_ASSET_PREFIX =
            "https://github.com/2hpj76sd6z-cpu/g2-dash/releases/download/";
    private static final String APK_MIME = "application/vnd.android.package-archive";
    private static final long CHECK_INTERVAL_MS = 12L * 60L * 60L * 1000L;
    private static final long DOWNLOAD_POLL_MS = 900L;
    private static final String PREFS = "g2dash.auto_updates";
    private static final String PREF_LAST_CHECK = "last_check_ms";
    private static final String PREF_DOWNLOAD_ID = "download_id";
    private static final String PREF_DOWNLOAD_VERSION = "download_version";
    private static final String PREF_PERMISSION_PENDING = "permission_pending";
    private static final String PREF_PENDING_URL = "pending_url";
    private static final String PREF_PENDING_VERSION = "pending_version";
    private static final String PREF_AVAILABLE_URL = "available_url";
    private static final String PREF_AVAILABLE_VERSION = "available_version";
    private static final Pattern VERSION_PATTERN =
            Pattern.compile("^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:[-+].*)?$");

    private final Activity activity;
    private final WebView webView;
    private final SharedPreferences preferences;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean checking;
    private boolean started;
    private volatile String latestAssetUrl;
    private volatile String latestVersion;
    private long downloadId = -1L;

    private final Runnable scheduledCheck = new Runnable() {
        @Override public void run() {
            checkForUpdates(false);
        }
    };
    private final Runnable pollDownload = this::pollDownload;

    public G2UpdateManager(Activity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
        this.preferences = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.downloadId = preferences.getLong(PREF_DOWNLOAD_ID, -1L);
        this.latestAssetUrl = preferences.getString(PREF_AVAILABLE_URL, null);
        this.latestVersion = preferences.getString(PREF_AVAILABLE_VERSION, null);
    }

    /** Called after the local dashboard has loaded, so its JS callback is ready. */
    public void start() {
        if (started) return;
        started = true;
        if (downloadId > 0L) {
            scheduleDownloadPoll(0L);
            emit("downloading", preferences.getString(PREF_DOWNLOAD_VERSION, latestVersion),
                    "currentVersion", BuildConfig.VERSION_NAME);
        } else if (latestVersion != null && latestAssetUrl != null) {
            if (compareVersions(latestVersion, BuildConfig.VERSION_NAME) > 0) {
                emit("available", latestVersion, "currentVersion", BuildConfig.VERSION_NAME);
            } else {
                clearAvailableRelease();
            }
        }
        scheduleNextCheck();
        onResume();
    }

    public void onResume() {
        if (!started) return;
        if (preferences.getBoolean(PREF_PERMISSION_PENDING, false)) {
            boolean allowed = canInstallPackages();
            String url = preferences.getString(PREF_PENDING_URL, null);
            String version = preferences.getString(PREF_PENDING_VERSION, null);
            preferences.edit().putBoolean(PREF_PERMISSION_PENDING, false).apply();
            if (allowed && url != null && version != null) {
                beginDownload(url, version);
            } else if (!allowed) {
                emit("permission_required", latestVersion, "currentVersion", BuildConfig.VERSION_NAME,
                        "message", "Autoriza la instalación desde G² Dash para continuar.");
            }
        }
        if (downloadId > 0L) scheduleDownloadPoll(0L);
        checkForUpdates(false);
    }

    public void checkForUpdates(boolean manual) {
        if (checking) {
            emit("checking", latestVersion, "currentVersion", BuildConfig.VERSION_NAME);
            return;
        }
        long now = System.currentTimeMillis();
        long lastCheck = preferences.getLong(PREF_LAST_CHECK, 0L);
        if (!manual && lastCheck > 0L && now - lastCheck < CHECK_INTERVAL_MS) {
            scheduleNextCheck();
            return;
        }

        checking = true;
        preferences.edit().putLong(PREF_LAST_CHECK, now).apply();
        scheduleNextCheck();
        emit("checking", latestVersion, "currentVersion", BuildConfig.VERSION_NAME);
        executor.execute(() -> {
            try {
                Release release = fetchLatestRelease();
                if (release == null) {
                    clearAvailableRelease();
                    emit("no_release", null, "currentVersion", BuildConfig.VERSION_NAME);
                } else if (compareVersions(release.version, BuildConfig.VERSION_NAME) <= 0) {
                    clearAvailableRelease();
                    emit("current", null, "currentVersion", BuildConfig.VERSION_NAME);
                } else {
                    latestAssetUrl = release.assetUrl;
                    latestVersion = release.version;
                    preferences.edit()
                            .putString(PREF_AVAILABLE_URL, latestAssetUrl)
                            .putString(PREF_AVAILABLE_VERSION, latestVersion)
                            .apply();
                    emit("available", release.version, "currentVersion", BuildConfig.VERSION_NAME);
                }
            } catch (Exception ex) {
                if (latestAssetUrl != null && latestVersion != null &&
                        compareVersions(latestVersion, BuildConfig.VERSION_NAME) > 0) {
                    emit("available", latestVersion, "currentVersion", BuildConfig.VERSION_NAME);
                } else {
                    emit("error", null, "currentVersion", BuildConfig.VERSION_NAME,
                            "message", "No se pudo revisar la actualización. Verifica Internet e inténtalo de nuevo.");
                }
            } finally {
                checking = false;
            }
        });
    }

    private void clearAvailableRelease() {
        latestAssetUrl = null;
        latestVersion = null;
        preferences.edit().remove(PREF_AVAILABLE_URL).remove(PREF_AVAILABLE_VERSION).apply();
    }

    private void scheduleNextCheck() {
        mainHandler.removeCallbacks(scheduledCheck);
        long lastCheck = preferences.getLong(PREF_LAST_CHECK, 0L);
        long delay = lastCheck == 0L ? 0L :
                Math.max(0L, CHECK_INTERVAL_MS - (System.currentTimeMillis() - lastCheck));
        mainHandler.postDelayed(scheduledCheck, delay);
    }

    public void installAvailableUpdate() {
        String url = latestAssetUrl;
        String version = latestVersion;
        if (url == null || version == null) {
            checkForUpdates(true);
            emit("error", null, "currentVersion", BuildConfig.VERSION_NAME,
                    "message", "Primero busca una actualización disponible.");
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !canInstallPackages()) {
            preferences.edit()
                    .putBoolean(PREF_PERMISSION_PENDING, true)
                    .putString(PREF_PENDING_URL, url)
                    .putString(PREF_PENDING_VERSION, version)
                    .apply();
            emit("permission_required", version, "currentVersion", BuildConfig.VERSION_NAME);
            try {
                Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + activity.getPackageName()));
                activity.startActivity(settings);
            } catch (Exception ex) {
                preferences.edit().putBoolean(PREF_PERMISSION_PENDING, false).apply();
                emit("error", version, "currentVersion", BuildConfig.VERSION_NAME,
                        "message", "Abre los ajustes de Android y permite instalar apps desde G² Dash.");
            }
            return;
        }
        beginDownload(url, version);
    }

    private boolean canInstallPackages() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                activity.getPackageManager().canRequestPackageInstalls();
    }

    private void beginDownload(String url, String version) {
        if (downloadId > 0L) {
            emit("downloading", version, "currentVersion", BuildConfig.VERSION_NAME);
            return;
        }
        if (url == null || !url.startsWith(RELEASE_ASSET_PREFIX)) {
            emit("error", version, "currentVersion", BuildConfig.VERSION_NAME,
                    "message", "La dirección de descarga no es una versión oficial de G² Dash.");
            return;
        }
        try {
            DownloadManager manager =
                    (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager == null) throw new IllegalStateException("DownloadManager unavailable");
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
                    .setTitle("G² Dash · " + version)
                    .setDescription("Descargando actualización")
                    .setMimeType(APK_MIME)
                    .setNotificationVisibility(
                            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(false)
                    .setDestinationInExternalFilesDir(activity, Environment.DIRECTORY_DOWNLOADS,
                            "G2-Dash-" + version + ".apk");
            downloadId = manager.enqueue(request);
            preferences.edit()
                    .putLong(PREF_DOWNLOAD_ID, downloadId)
                    .putString(PREF_DOWNLOAD_VERSION, version)
                    .apply();
            emit("downloading", version, "currentVersion", BuildConfig.VERSION_NAME);
            scheduleDownloadPoll(DOWNLOAD_POLL_MS);
        } catch (Exception ex) {
            downloadId = -1L;
            emit("error", version, "currentVersion", BuildConfig.VERSION_NAME,
                    "message", "No se pudo iniciar la descarga. Revisa la conexión del estéreo.");
        }
    }

    private void scheduleDownloadPoll(long delay) {
        mainHandler.removeCallbacks(pollDownload);
        mainHandler.postDelayed(pollDownload, delay);
    }

    private void pollDownload() {
        if (downloadId <= 0L) return;
        DownloadManager manager =
                (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager == null) return;
        Cursor cursor = null;
        try {
            cursor = manager.query(new DownloadManager.Query().setFilterById(downloadId));
            if (cursor == null || !cursor.moveToFirst()) {
                scheduleDownloadPoll(DOWNLOAD_POLL_MS);
                return;
            }
            int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                Uri apkUri = manager.getUriForDownloadedFile(downloadId);
                String version = preferences.getString(PREF_DOWNLOAD_VERSION, latestVersion);
                downloadId = -1L;
                preferences.edit().remove(PREF_DOWNLOAD_ID).remove(PREF_DOWNLOAD_VERSION).apply();
                if (apkUri == null) {
                    emit("error", version, "currentVersion", BuildConfig.VERSION_NAME,
                            "message", "La descarga terminó, pero no se pudo abrir el APK.");
                    return;
                }
                emit("install_prompt", version, "currentVersion", BuildConfig.VERSION_NAME);
                installDownloadedApk(apkUri, version);
            } else if (status == DownloadManager.STATUS_FAILED) {
                downloadId = -1L;
                preferences.edit().remove(PREF_DOWNLOAD_ID).remove(PREF_DOWNLOAD_VERSION).apply();
                emit("error", null, "currentVersion", BuildConfig.VERSION_NAME,
                        "message", "La descarga falló. Revisa Internet e inténtalo otra vez.");
            } else {
                scheduleDownloadPoll(DOWNLOAD_POLL_MS);
            }
        } catch (Exception ex) {
            scheduleDownloadPoll(DOWNLOAD_POLL_MS);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private void installDownloadedApk(Uri apkUri, String version) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            try {
                Intent installer = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                installer.setData(apkUri);
                installer.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                activity.startActivity(installer);
                emit("install_prompt", version, "currentVersion", BuildConfig.VERSION_NAME);
            } catch (Exception ex) {
                emit("error", version, "currentVersion", BuildConfig.VERSION_NAME,
                        "message", "Android no pudo abrir el instalador. Vuelve a intentarlo.");
            }
            return;
        }
        int sessionId = -1;
        try {
            PackageInstaller installer = activity.getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params =
                    new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            params.setAppPackageName(activity.getPackageName());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED);
            }
            sessionId = installer.createSession(params);
            try (PackageInstaller.Session session = installer.openSession(sessionId);
                 InputStream input = activity.getContentResolver().openInputStream(apkUri);
                 OutputStream output = session.openWrite("base.apk", 0, -1L)) {
                if (input == null) throw new IOException("Downloaded APK could not be opened");
                byte[] buffer = new byte[65536];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                session.fsync(output);
                int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) flags |= PendingIntent.FLAG_MUTABLE;
                Intent statusIntent = new Intent(activity, G2InstallReceiver.class);
                statusIntent.setAction(G2InstallReceiver.ACTION_INSTALL_STATUS);
                PendingIntent statusPendingIntent =
                        PendingIntent.getBroadcast(activity, sessionId, statusIntent, flags);
                session.commit(statusPendingIntent.getIntentSender());
            }
        } catch (Exception ex) {
            if (sessionId >= 0) {
                try { activity.getPackageManager().getPackageInstaller().abandonSession(sessionId); }
                catch (Exception ignored) { }
            }
            emit("error", version, "currentVersion", BuildConfig.VERSION_NAME,
                    "message", "Android no pudo preparar el instalador. Vuelve a intentarlo.");
        }
    }

    private Release fetchLatestRelease() throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(RELEASE_API).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(12000);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("User-Agent", "G2-Dash-Android");
            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_NOT_FOUND) return null;
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("GitHub release API returned " + status);
            }
            String body;
            try (InputStream input = connection.getInputStream()) {
                body = readAll(input);
            }
            JSONObject release = new JSONObject(body);
            if (release.optBoolean("draft") || release.optBoolean("prerelease")) return null;
            String tag = release.optString("tag_name", "");
            Matcher matcher = VERSION_PATTERN.matcher(tag);
            if (!matcher.matches()) throw new IOException("Release tag is not semantic version");
            String version = matcher.group(1) + "." + matcher.group(2) + "." + matcher.group(3);
            JSONArray assets = release.optJSONArray("assets");
            if (assets == null) throw new IOException("Release has no assets");
            String assetUrl = null;
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.optJSONObject(i);
                if (asset != null && "G2-Dash.apk".equals(asset.optString("name"))) {
                    assetUrl = asset.optString("browser_download_url", null);
                    break;
                }
            }
            if (assetUrl == null || !assetUrl.startsWith(RELEASE_ASSET_PREFIX)) {
                throw new IOException("Official APK asset is missing");
            }
            return new Release(version, assetUrl);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private String readAll(InputStream input) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
        }
        return body.toString();
    }

    private int compareVersions(String available, String installed) {
        Matcher a = VERSION_PATTERN.matcher(available);
        Matcher b = VERSION_PATTERN.matcher(installed);
        if (!a.find() || !b.find()) return -1;
        for (int i = 1; i <= 3; i++) {
            int left = Integer.parseInt(a.group(i));
            int right = Integer.parseInt(b.group(i));
            if (left != right) return Integer.compare(left, right);
        }
        return 0;
    }

    private void emit(String state, String version, String... extraPairs) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("state", state);
            payload.put("currentVersion", BuildConfig.VERSION_NAME);
            if (version != null) payload.put("version", version);
            for (int i = 0; i + 1 < extraPairs.length; i += 2) {
                payload.put(extraPairs[i], extraPairs[i + 1]);
            }
        } catch (Exception ignored) { }
        String script = "window.G2AndroidUpdateResult && window.G2AndroidUpdateResult("
                + payload.toString() + ");";
        mainHandler.post(() -> {
            if (webView != null && !activity.isFinishing()) webView.evaluateJavascript(script, null);
        });
    }

    public void destroy() {
        mainHandler.removeCallbacks(scheduledCheck);
        mainHandler.removeCallbacks(pollDownload);
        executor.shutdownNow();
    }

    private static final class Release {
        final String version;
        final String assetUrl;
        Release(String version, String assetUrl) {
            this.version = version;
            this.assetUrl = assetUrl;
        }
    }
}
