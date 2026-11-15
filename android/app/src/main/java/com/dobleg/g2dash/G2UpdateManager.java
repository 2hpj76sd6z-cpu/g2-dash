package com.dobleg.g2dash;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks public GitHub Releases when G² Dash resumes. Download and installation
 * are started only after the user accepts; Android controls the final install.
 */
final class G2UpdateManager {
    private static final String RELEASE_API =
            "https://api.github.com/repos/2hpj76sd6z-cpu/g2-dash/releases/latest";
    private static final String RELEASE_ASSET_PREFIX =
            "https://github.com/2hpj76sd6z-cpu/g2-dash/releases/download/";
    private static final String PREFS = "g2dash_updates";
    private static final String LAST_CHECK = "last_check";
    private static final String VERSION_PATTERN =
            "^[vV]?(\\d+)\\.(\\d+)\\.(\\d+)(?:[-+].*)?$";
    private static final long CHECK_INTERVAL_MS = 12L * 60L * 60L * 1000L;
    private static final long MAX_APK_BYTES = 100L * 1024L * 1024L;
    private static final int INSTALL_PERMISSION_REQUEST = 22;

    private final Activity activity;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean checking = new AtomicBoolean(false);
    private File pendingApk;
    private boolean waitingForInstallPermission;

    G2UpdateManager(Activity activity) {
        this.activity = activity;
    }

    void checkForUpdate() {
        long lastCheck = activity.getSharedPreferences(PREFS, 0)
                .getLong(LAST_CHECK, 0L);
        long now = System.currentTimeMillis();
        if (now - lastCheck < CHECK_INTERVAL_MS || !checking.compareAndSet(false, true)) return;

        activity.getSharedPreferences(PREFS, 0).edit().putLong(LAST_CHECK, now).apply();
        executor.execute(() -> {
            try {
                JSONObject release = getLatestRelease();
                String tag = release.optString("tag_name", "");
                if (!isNewer(tag, BuildConfig.VERSION_NAME)) return;

                String assetUrl = null;
                String digest = null;
                JSONArray assets = release.optJSONArray("assets");
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject asset = assets.optJSONObject(i);
                        if (asset == null) continue;
                        String name = asset.optString("name", "");
                        String candidate = asset.optString("browser_download_url", "");
                        if (name.toLowerCase(Locale.ROOT).endsWith(".apk")
                                && candidate.startsWith(RELEASE_ASSET_PREFIX)) {
                            assetUrl = candidate;
                            digest = asset.optString("digest", "");
                            break;
                        }
                    }
                }
                if (assetUrl == null) return;

                final String version = tag;
                final String downloadUrl = assetUrl;
                final String expectedDigest = digest;
                activity.runOnUiThread(() -> showUpdateDialog(version, downloadUrl, expectedDigest));
            } catch (Exception ignored) {
                // Sin conexión o sin Release: G² Dash sigue funcionando sin interrupciones.
            } finally {
                checking.set(false);
            }
        });
    }

    private JSONObject getLatestRelease() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(RELEASE_API).openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(18000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "G2-Dash-Android");
        try {
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("GitHub release check failed");
            }
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                    if (body.length() > 1500000) {
                        throw new IllegalStateException("Release response too large");
                    }
                }
            }
            return new JSONObject(body.toString());
        } finally {
            connection.disconnect();
        }
    }

    private void showUpdateDialog(String tag, String downloadUrl, String digest) {
        if (activity.isFinishing() || (Build.VERSION.SDK_INT >= 17 && activity.isDestroyed())) return;
        new AlertDialog.Builder(activity)
                .setTitle("Nueva versión de G² Dash")
                .setMessage(tag + " está disponible. Al continuar, se descargará la APK y "
                        + "Android abrirá el instalador para que confirmes la actualización.")
                .setPositiveButton("Descargar", (dialog, which) ->
                        downloadAndInstall(tag, downloadUrl, digest))
                .setNegativeButton("Después", (dialog, which) -> { })
                .show();
    }

    private void downloadAndInstall(String tag, String downloadUrl, String digest) {
        Toast.makeText(activity, "Descargando actualización…", Toast.LENGTH_LONG).show();
        executor.execute(() -> {
            try {
                File apk = downloadApk(tag, downloadUrl, digest);
                PackageInfo packageInfo = activity.getPackageManager()
                        .getPackageArchiveInfo(apk.getAbsolutePath(), 0);
                if (packageInfo == null
                        || !activity.getPackageName().equals(packageInfo.packageName)
                        || packageInfo.versionCode <= BuildConfig.VERSION_CODE) {
                    throw new IllegalStateException("APK inválida o versión no superior");
                }
                activity.runOnUiThread(() -> installOrRequestPermission(apk));
            } catch (Exception ex) {
                activity.runOnUiThread(() ->
                        Toast.makeText(activity, "No se pudo preparar la actualización.",
                                Toast.LENGTH_LONG).show());
            }
        });
    }

    private File downloadApk(String tag, String downloadUrl, String expectedDigest) throws Exception {
        if (!downloadUrl.startsWith(RELEASE_ASSET_PREFIX)) {
            throw new IllegalArgumentException("Unexpected download host");
        }
        HttpURLConnection connection = (HttpURLConnection) new URL(downloadUrl).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setRequestProperty("User-Agent", "G2-Dash-Android");
        connection.setInstanceFollowRedirects(true);
        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) throw new IllegalStateException("Download failed");
            long declaredSize = connection.getContentLength();
            if (declaredSize > MAX_APK_BYTES) throw new IllegalStateException("APK too large");

            File directory = new File(activity.getCacheDir(), "updates");
            if (!directory.exists() && !directory.mkdirs()) {
                throw new IllegalStateException("Could not create update directory");
            }
            String safeTag = tag.replaceAll("[^A-Za-z0-9._-]", "_");
            File partial = new File(directory, "G2-Dash-" + safeTag + ".apk.part");
            File apk = new File(directory, "G2-Dash-" + safeTag + ".apk");
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            long total = 0L;
            byte[] buffer = new byte[16384];
            try (InputStream input = new BufferedInputStream(connection.getInputStream());
                 FileOutputStream output = new FileOutputStream(partial)) {
                int count;
                while ((count = input.read(buffer)) != -1) {
                    total += count;
                    if (total > MAX_APK_BYTES) throw new IllegalStateException("APK too large");
                    sha256.update(buffer, 0, count);
                    output.write(buffer, 0, count);
                }
                output.getFD().sync();
            } catch (Exception ex) {
                partial.delete();
                throw ex;
            }

            if (expectedDigest != null && expectedDigest.startsWith("sha256:")) {
                String actual = toHex(sha256.digest());
                String expected = expectedDigest.substring("sha256:".length())
                        .trim().toLowerCase(Locale.ROOT);
                if (!actual.equals(expected)) {
                    partial.delete();
                    throw new SecurityException("APK checksum mismatch");
                }
            }
            if (apk.exists() && !apk.delete()) {
                partial.delete();
                throw new IllegalStateException("Could not replace cached APK");
            }
            if (!partial.renameTo(apk)) {
                partial.delete();
                throw new IllegalStateException("Could not finalize cached APK");
            }
            return apk;
        } finally {
            connection.disconnect();
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format(Locale.ROOT, "%02x", value & 0xff));
        return result.toString();
    }

    private void installOrRequestPermission(File apk) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !activity.getPackageManager().canRequestPackageInstalls()) {
            pendingApk = apk;
            waitingForInstallPermission = true;
            new AlertDialog.Builder(activity)
                    .setTitle("Permitir instalación de G² Dash")
                    .setMessage("Android necesita permiso para instalar actualizaciones de esta app.")
                    .setPositiveButton("Abrir ajustes", (dialog, which) -> {
                        try {
                            Intent settings = new Intent(
                                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                    Uri.parse("package:" + activity.getPackageName()));
                            activity.startActivityForResult(settings, INSTALL_PERMISSION_REQUEST);
                        } catch (Exception ex) {
                            waitingForInstallPermission = false;
                            pendingApk = null;
                            Toast.makeText(activity, "No se pudieron abrir los ajustes.",
                                    Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton("Cancelar", (dialog, which) -> {
                        waitingForInstallPermission = false;
                        pendingApk = null;
                    })
                    .show();
            return;
        }
        launchInstaller(apk);
    }

    boolean onActivityResult(int requestCode) {
        if (requestCode != INSTALL_PERMISSION_REQUEST) return false;
        continueAfterPermission();
        return true;
    }

    void onResume() {
        if (waitingForInstallPermission && canInstallPackages()) continueAfterPermission();
    }

    private boolean canInstallPackages() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                || activity.getPackageManager().canRequestPackageInstalls();
    }

    private void continueAfterPermission() {
        if (!waitingForInstallPermission) return;
        waitingForInstallPermission = false;
        File apk = pendingApk;
        pendingApk = null;
        if (apk != null && apk.exists() && canInstallPackages()) {
            launchInstaller(apk);
        } else if (apk != null && apk.exists()) {
            Toast.makeText(activity, "No se autorizó la instalación.", Toast.LENGTH_LONG).show();
        }
    }

    private void launchInstaller(File apk) {
        try {
            Uri uri = FileProvider.getUriForFile(activity,
                    activity.getPackageName() + ".fileprovider", apk);
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(uri, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(install);
        } catch (Exception ex) {
            Toast.makeText(activity, "Android no pudo abrir el instalador.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private static boolean isNewer(String candidate, String current) {
        int[] remote = versionParts(candidate);
        int[] installed = versionParts(current);
        if (remote == null || installed == null) return false;
        for (int i = 0; i < remote.length; i++) {
            if (remote[i] != installed[i]) return remote[i] > installed[i];
        }
        boolean remoteStable = isStable(candidate);
        boolean installedStable = isStable(current);
        return remoteStable && !installedStable;
    }

    private static int[] versionParts(String value) {
        if (value == null) return null;
        Matcher matcher = Pattern.compile(VERSION_PATTERN).matcher(value.trim());
        if (!matcher.matches()) return null;
        try {
            return new int[] {
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3))
            };
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static boolean isStable(String value) {
        return value != null && value.matches("^[vV]?\\d+\\.\\d+\\.\\d+$");
    }
}
