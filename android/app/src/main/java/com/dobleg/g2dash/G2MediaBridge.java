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
 * Las indicaciones de navegación se comparten solo cuando el usuario las habilita.
 * Los metadatos y las portadas no salen del equipo.
 */
final class G2MediaBridge {
    private final Activity activity;
    private final WebView webView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final MediaSessionManager sessions;
    private final ComponentName listenerComponent;
    private boolean visible;
    private String lastSource = "";
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
            .setMessage("Android requiere acceso a notificaciones para compartir las sesiones de música con G² Dash. Es un permiso amplio: Dash consulta títulos, artistas, portadas y controles. Si activas Indicaciones compartidas, también muestra las maniobras publicadas por Google Maps o el receptor seleccionado. Puedes revocarlo en Ajustes.")
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
        MediaController previous = null;
        // AirPlay has priority when genuinely playing: other paused sessions
        // must not steal the iPhone cover art or transport controls.
        for (MediaController item : all) {
            if (item == null) continue;
            PlaybackState state = item.getPlaybackState();
            if (("com.syu.carlink".equals(item.getPackageName()) || activity.getPreferences(0).getString("carlinkApp","").equals(item.getPackageName()))
                    && state != null && state.getState() == PlaybackState.STATE_PLAYING) return item;
        }
        for (MediaController item : all) {
            if (item == null) continue;
            PlaybackState state = item.getPlaybackState();
            if (state != null && state.getState() == PlaybackState.STATE_PLAYING) return item;
            if (item.getMetadata() != null) {
                if (lastSource.equals(item.getPackageName())) previous=item;
                if (fallback==null) fallback=item;
            }
        }
        return previous!=null ? previous : fallback;
    }

    void control(String action) {
        if (!accessGranted()) {
            showAccessSettings();
            return;
        }
        MediaController item = preferredSession();
        if (item == null) {
            android.media.AudioManager audio=(android.media.AudioManager)activity.getSystemService(Context.AUDIO_SERVICE);
            int key="toggle".equals(action)?android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    "next".equals(action)?android.view.KeyEvent.KEYCODE_MEDIA_NEXT:
                    "previous".equals(action)?android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS:0;
            if (audio!=null && key!=0) {
                audio.dispatchMediaKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,key));
                audio.dispatchMediaKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP,key));
            }
            return;
        }
        PlaybackState state = item.getPlaybackState();
        long actions = state == null ? 0 : state.getActions();
        MediaController.TransportControls transport = item.getTransportControls();
        switch (action) {
            case "toggle":
                if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                    if ((actions & (PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE)) != 0) transport.pause();
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
        if (value == null || value.length() == 0) return previous!=null ? previous : fallback;
        return value.toString();
    }

    private Bitmap readLocalCover(MediaMetadata meta) {
        android.net.Uri uri=meta.getDescription().getIconUri();
        if (uri==null || !"content".equals(uri.getScheme())) return null;
        try {
            android.graphics.BitmapFactory.Options opts=new android.graphics.BitmapFactory.Options();
            opts.inJustDecodeBounds=true;
            try(java.io.InputStream in=activity.getContentResolver().openInputStream(uri)) {
                android.graphics.BitmapFactory.decodeStream(in,null,opts);
            }
            if (opts.outWidth<=0 || opts.outHeight<=0) return null;
            opts.inSampleSize=1;
            while (Math.max(opts.outWidth,opts.outHeight)/opts.inSampleSize>640) opts.inSampleSize*=2;
            opts.inJustDecodeBounds=false;
            try(java.io.InputStream in=activity.getContentResolver().openInputStream(uri)) {
                return android.graphics.BitmapFactory.decodeStream(in,null,opts);
            }
        } catch(Exception unavailable) { return null; }
    }

    private String albumArt(MediaMetadata metadata, String key) {
        if (key.equals(lastArtworkId) && !lastArtworkData.isEmpty()) return lastArtworkData;
        lastArtworkId = key;
        lastArtworkData = "";
        if (metadata == null) return lastArtworkData;
        Bitmap bitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        if (bitmap == null) bitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
        if (bitmap == null) bitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
        if (bitmap == null) bitmap = metadata.getDescription().getIconBitmap();
        if (bitmap == null) bitmap = readLocalCover(metadata);
        if (bitmap == null || bitmap.getWidth() < 1 || bitmap.getHeight() < 1) return "";
        try {
            int target = 320;
            float factor = Math.min(1f, Math.min((float) target / bitmap.getWidth(), (float) target / bitmap.getHeight()));
            int w = Math.max(1, Math.round(bitmap.getWidth() * factor));
            int h = Math.max(1, Math.round(bitmap.getHeight() * factor));
            Bitmap small = Bitmap.createScaledBitmap(bitmap, w, h, true);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            small.compress(Bitmap.CompressFormat.JPEG, 82, output);
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
            boolean share = granted && activity.getPreferences(0).getBoolean("shareTurns", false);
            data.put("turn", share ? G2MediaListener.instruction() : "");
            data.put("turnSource", share ? G2MediaListener.source() : "");
            if (!granted) {
                data.put("title", "Conecta tu música");
                data.put("artist", "Autoriza el acceso en Android");
                data.put("album", "Álbum no disponible");
                data.put("playing", false);
                data.put("albumArt", "");
                data.put("durationMs", 0);
                data.put("positionMs", 0);
            } else {
                MediaController current = preferredSession();
                if (current == null) {
                    data.put("title", "Sin música activa");
                    data.put("artist", "Abre tu reproductor en este Android");
                    data.put("album", "Álbum no disponible");
                    data.put("playing", false);
                    data.put("albumArt", "");
                    data.put("durationMs", 0);
                    data.put("positionMs", 0);
                } else {
                    lastSource=current.getPackageName();
                    MediaMetadata meta = current.getMetadata();
                    PlaybackState state = current.getPlaybackState();
                    boolean playing = state != null && state.getState() == PlaybackState.STATE_PLAYING;
                    String title = meta == null ? "Sin título" :
                            safeText(meta.getText(MediaMetadata.METADATA_KEY_TITLE), "Sin título");
                    String artist = meta == null ? "Artista no disponible" :
                            safeText(meta.getText(MediaMetadata.METADATA_KEY_ARTIST), "Artista no disponible");
                    String album = meta == null ? "Álbum no disponible" :
                            safeText(meta.getText(MediaMetadata.METADATA_KEY_ALBUM), "Álbum no disponible");
                    long duration = meta == null ? 0 : Math.max(0, meta.getLong(MediaMetadata.METADATA_KEY_DURATION));
                    long position = state == null ? 0 : Math.max(0, state.getPosition());
                    if (playing && state.getLastPositionUpdateTime() > 0) {
                        long elapsed=android.os.SystemClock.elapsedRealtime()-state.getLastPositionUpdateTime();
                        if (elapsed >= 0) position+=Math.round(elapsed*state.getPlaybackSpeed());
                    }
                    if (duration > 0) position = Math.min(position, duration);
                    data.put("title", title);
                    data.put("artist", artist);
                    data.put("album", album);
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
                    data.put("carLink", "com.syu.carlink".equals(current.getPackageName())
                            || current.getPackageName().equals(activity.getPreferences(0).getString("carlinkApp","")));
                    data.put("albumArt", albumArt(meta, current.getPackageName() + "|" + title + "|" + artist + "|" + album));
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
