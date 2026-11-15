package com.dobleg.g2dash;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Base64;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Solo observa sesiones de música de ESTE Android, con permiso explícito.
 * NO escucha el audio de otro teléfono por Wi-Fi ni lee notificaciones.
 * Los metadatos y las portadas no salen del equipo.
 */
final class G2MediaBridge {
    private final Activity activity;
    private final WebView webView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final MediaSessionManager sessions;
    private final ComponentName listenerComponent;
    private boolean visible;
    private String lastArtworkId = "";
    private String lastArtworkData = "";
    private String previousPayload = "";

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!visible) return;
            try { emit(); } catch (Exception ignored) {
                // Never crash a driving dashboard due to another app's media metadata.
            }
            handler.postDelayed(this, 2500);
        }
    };

    G2MediaBridge(Activity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
        sessions = (MediaSessionManager) activity.getSystemService(Context.MEDIA_SESSION_SERVICE);
        listenerComponent = new ComponentName(activity, G2MediaListener.class);
    }

    void start() {
        visible = true;
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    void stop() {
        visible = false;
        handler.removeCallbacks(tick);
    }

    void destroy() {
        stop();
        previousPayload = "";
        lastArtworkData = "";
        lastArtworkId = "";
    }

    boolean accessGranted() {
        String raw = Settings.Secure.getString(activity.getContentResolver(), "enabled_notification_listeners");
        if (raw == null || raw.isEmpty()) return false;
        for (String entry : raw.split(":")) {
            ComponentName enabled = ComponentName.unflattenFromString(entry);
            if (listenerComponent.equals(enabled)) return true;
        }
        return false;
    }

    void showAccessSettings() {
        // Notification access is broad; explain it before asking the user to enable it.
        new android.app.AlertDialog.Builder(activity)
            .setTitle("Conectar reproductor de música")
            .setMessage("Android requiere acceso a notificaciones para compartir las sesiones de música con G² Dash. Es un permiso amplio: esta app solo consultará títulos, artistas, portadas y controles de reproducción. Puedes revocarlo en Ajustes.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Continuar", (d, which) -> {
                try {
                    activity.startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                } catch (Exception ex) {
                    Toast.makeText(activity, "Esta versión de Android no abre ese ajuste", Toast.LENGTH_LONG).show();
                }
            })
            .show();
    }

    private List<MediaController> controllers() {
        if (!accessGranted() || sessions == null) return java.util.Collections.emptyList();
        try {
            return sessions.getActiveSessions(listenerComponent);
        } catch (SecurityException | IllegalStateException ex) {
            return java.util.Collections.emptyList();
        }
    }

    private MediaController preferredSession() {
        List<MediaController> all = controllers();
        MediaController fallback = null;
        for (MediaController item : all) {
            if (item == null) continue;
            PlaybackState state = item.getPlaybackState();
            if (state != null && state.getState() == PlaybackState.STATE_PLAYING) return item;
            if (fallback == null && item.getMetadata() != null) fallback = item;
        }
        return fallback;
    }

    void control(String action) {
        if (!accessGranted()) {
            showAccessSettings();
            return;
        }
        MediaController item = preferredSession();
        if (item == null) {
            Toast.makeText(activity, "Abre primero una app de música", Toast.LENGTH_SHORT).show();
            return;
        }
        PlaybackState state = item.getPlaybackState();
        long actions = state == null ? 0 : state.getActions();
        MediaController.TransportControls transport = item.getTransportControls();
        switch (action) {
            case "toggle":
                if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                    if ((actions & PlaybackState.ACTION_PAUSE) != 0) transport.pause();
                    else Toast.makeText(activity, "Pausar no disponible", Toast.LENGTH_SHORT).show();
                } else {
                    if ((actions & PlaybackState.ACTION_PLAY) != 0 || (actions & PlaybackState.ACTION_PLAY_PAUSE) != 0) transport.play();
                    else Toast.makeText(activity, "Reproducir no disponible", Toast.LENGTH_SHORT).show();
                }
                break;
            case "next":
                if ((actions & PlaybackState.ACTION_SKIP_TO_NEXT) != 0) transport.skipToNext();
                else Toast.makeText(activity, "Siguiente no disponible", Toast.LENGTH_SHORT).show();
                break;
            case "previous":
                if ((actions & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0) transport.skipToPrevious();
                else Toast.makeText(activity, "Anterior no disponible", Toast.LENGTH_SHORT).show();
                break;
            default:
                return;
        }
        handler.postDelayed(this::emit, 350);
    }

    /** Adelantar/retroceder solo cuando la MediaSession expone ACTION_SEEK_TO. */
    void seekTo(long positionMs) {
        if (!accessGranted() || positionMs < 0) return;
        MediaController item = preferredSession();
        if (item == null) return;
        PlaybackState state = item.getPlaybackState();
        MediaMetadata meta = item.getMetadata();
        long duration = meta == null ? 0 : Math.max(0,
                meta.getLong(MediaMetadata.METADATA_KEY_DURATION));
        if (state == null || duration == 0 ||
                (state.getActions() & PlaybackState.ACTION_SEEK_TO) == 0) return;
        try {
            item.getTransportControls().seekTo(Math.min(positionMs, duration));
            handler.postDelayed(this::emit, 300);
        } catch (Exception ignored) {
            Toast.makeText(activity,"Esta aplicación no permite adelantar",Toast.LENGTH_SHORT).show();
        }
    }

    private static String safeText(CharSequence value, String fallback) {
        if (value == null || value.length() == 0) return fallback;
        return value.toString();
    }

    private String albumArt(MediaMetadata metadata, String key) {
        if (key.equals(lastArtworkId)) return lastArtworkData;
        lastArtworkId = key;
        lastArtworkData = "";
        if (metadata == null) return lastArtworkData;
        Bitmap bitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        if (bitmap == null) bitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
        if (bitmap == null) bitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
        if (bitmap == null || bitmap.getWidth() < 1 || bitmap.getHeight() < 1) return "";
        try {
            int target = 180;
            float factor = Math.min(1f, Math.min((float) target / bitmap.getWidth(), (float) target / bitmap.getHeight()));
            int w = Math.max(1, Math.round(bitmap.getWidth() * factor));
            int h = Math.max(1, Math.round(bitmap.getHeight() * factor));
            Bitmap small = Bitmap.createScaledBitmap(bitmap, w, h, true);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            small.compress(Bitmap.CompressFormat.JPEG, 72, output);
            if (small != bitmap) small.recycle();
            lastArtworkData = "data:image/jpeg;base64," + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP);
        } catch (Exception ex) {
            lastArtworkData = "";
        }
        return lastArtworkData;
    }

    void emit() {
        if (webView == null || !visible) return;
        boolean granted = accessGranted();
        JSONObject data = new JSONObject();
        try {
            data.put("granted", granted);
            data.put("native", true);
            if (!granted) {
                data.put("title", "Conecta tu música");
                data.put("artist", "Autoriza el acceso en Android");
                data.put("playing", false);
                data.put("albumArt", "");
                data.put("durationMs", 0);
                data.put("positionMs", 0);
            } else {
                MediaController current = preferredSession();
                if (current == null) {
                    data.put("title", "Sin música activa");
                    data.put("artist", "Abre tu reproductor en este Android");
                    data.put("playing", false);
                    data.put("albumArt", "");
                    data.put("durationMs", 0);
                    data.put("positionMs", 0);
                } else {
                    MediaMetadata meta = current.getMetadata();
                    PlaybackState state = current.getPlaybackState();
                    boolean playing = state != null && state.getState() == PlaybackState.STATE_PLAYING;
                    String title = meta == null ? "Sin título" :
                            safeText(meta.getText(MediaMetadata.METADATA_KEY_TITLE), "Sin título");
                    String artist = meta == null ? "Artista no disponible" :
                            safeText(meta.getText(MediaMetadata.METADATA_KEY_ARTIST), "Artista no disponible");
                    long duration = meta == null ? 0 : Math.max(0, meta.getLong(MediaMetadata.METADATA_KEY_DURATION));
                    long position = state == null ? 0 : Math.max(0, state.getPosition());
                    if (duration > 0) position = Math.min(position, duration);
                    data.put("title", title);
                    data.put("artist", artist);
                    data.put("playing", playing);
                    long actions = state == null ? 0 : state.getActions();
                    data.put("canSeek", duration > 0 &&
                            (actions & PlaybackState.ACTION_SEEK_TO) != 0);
                    data.put("canPrevious",
                            (actions & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0);
                    data.put("canNext",
                            (actions & PlaybackState.ACTION_SKIP_TO_NEXT) != 0);
                    data.put("canPlay", playing
                            ? (actions & (PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE)) != 0
                            : (actions & (PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PLAY_PAUSE)) != 0);
                    data.put("package", current.getPackageName());
                    data.put("albumArt", albumArt(meta, current.getPackageName() + "|" + title + "|" + artist));
                    data.put("durationMs", duration);
                    data.put("positionMs", position);
                }
            }
            String payload = data.toString();
            if (!payload.equals(previousPayload)) {
                previousPayload = payload;
                webView.evaluateJavascript("window.G2MusicUpdate && window.G2MusicUpdate(" + payload + ");", null);
            }
        } catch (JSONException | IllegalStateException ex) {
            // WebView may already be destroyed or the remote session may be unavailable.
        }
    }
}
