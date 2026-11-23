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
    private final StereoMusic stereo;
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
        stereo=new StereoMusic(activity);
        this.webView = webView;
        sessions = (MediaSessionManager) activity.getSystemService(Context.MEDIA_SESSION_SERVICE);
        listenerComponent = new ComponentName(activity, G2MediaListener.class);
    }

    void start() {
        stereo.start();
        previousPayload = "";
        visible = true;
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    void stop() {
        stereo.stop();
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
        String selected=activity.getPreferences(0).getString("carlinkApp","");
        List<MediaController> all = new java.util.ArrayList<>(controllers());
        MediaController bluetooth=stereo.controller(selected);
        if(bluetooth!=null)all.add(bluetooth);
        android.service.notification.StatusBarNotification notice=accessGranted()?G2MediaListener.media():null;
        if(notice!=null)try{
            android.media.session.MediaSession.Token token=notice.getNotification().extras.getParcelable(android.app.Notification.EXTRA_MEDIA_SESSION);
            if(token!=null){
                MediaController attached=new MediaController(activity,token);
                if(notice.getPackageName().equals(attached.getPackageName()))all.add(attached);
            }
        }catch(RuntimeException retired){/* Receiver replaced its session. */}
        MediaController best=null;
        int bestScore=-1;
        for(MediaController item:all){
            if(item==null)continue;
            String source=item.getPackageName();
            if(item!=bluetooth && !source.equals(selected) && !G2MediaListener.CARLINK.equals(source)
                    && !"com.android.bluetooth".equals(source))continue;
            MediaMetadata m=item.getMetadata();
            boolean named=m!=null && !safeText(m.getDescription().getTitle(),"").trim().isEmpty();
            PlaybackState state=item.getPlaybackState();
            boolean playing=state!=null&&state.getState()==PlaybackState.STATE_PLAYING;
            String pkg=item.getPackageName();
            int score=(named?16:0)+(playing?8:0)+(pkg.equals(selected)||G2MediaListener.CARLINK.equals(pkg)?4:0)+(pkg.equals(lastSource)?2:0);
            if(score>bestScore){best=item;bestScore=score;}
        }
        return best;
    }

    void control(String action) {
        MediaController item = preferredSession();
        if (item==null && !accessGranted() && stereo.track()==null) { showAccessSettings(); return; }
        if(item==null){
            Toast.makeText(activity,"El receptor aún no comparte controles de Spotify",Toast.LENGTH_SHORT).show();
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
        if (positionMs < 0) return;
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

    private static Bitmap notificationArt(android.app.Notification n,Activity activity) {
        if(n==null||n.getLargeIcon()==null)return null;
        try{
            if(Build.VERSION.SDK_INT<28)return n.largeIcon;
            android.graphics.drawable.Icon icon=n.getLargeIcon();
            if(Build.VERSION.SDK_INT>=28 && icon.getType()!=android.graphics.drawable.Icon.TYPE_BITMAP
                && icon.getType()!=android.graphics.drawable.Icon.TYPE_ADAPTIVE_BITMAP)return null;
            android.graphics.drawable.Drawable drawable=icon.loadDrawable(activity);
            if(drawable instanceof android.graphics.drawable.BitmapDrawable)
                return ((android.graphics.drawable.BitmapDrawable)drawable).getBitmap();
        }catch(RuntimeException ignored){}
        return null;
    }

    private String albumArt(MediaMetadata meta,android.app.Notification note,Bitmap received,String key) {
        Bitmap bitmap=null;
        if(meta!=null){
            bitmap=meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
            if(bitmap==null)bitmap=meta.getBitmap(MediaMetadata.METADATA_KEY_ART);
            if(bitmap==null)bitmap=meta.getDescription().getIconBitmap();
        }
        if(bitmap==null)bitmap=notificationArt(note,activity);
        if(bitmap==null)bitmap=received;
        if(bitmap!=null)key+="|"+bitmap.getGenerationId();
        else if(meta!=null)key+="|"+meta.getDescription().getIconUri();
        if(key.equals(lastArtworkId)&&!lastArtworkData.isEmpty())return lastArtworkData;
        if(bitmap==null&&meta!=null)bitmap=readLocalCover(meta);
        lastArtworkId=key;
        lastArtworkData=bitmap==null?"":encode(bitmap);
        return lastArtworkData;
    }

    static String encode(Bitmap bitmap) {
        try {
            int target = 320;
            float factor = Math.min(1f, Math.min((float) target / bitmap.getWidth(), (float) target / bitmap.getHeight()));
            int w = Math.max(1, Math.round(bitmap.getWidth() * factor));
            int h = Math.max(1, Math.round(bitmap.getHeight() * factor));
            Bitmap small = Bitmap.createScaledBitmap(bitmap, w, h, true);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            small.compress(Bitmap.CompressFormat.JPEG, 82, output);
            if (small != bitmap) small.recycle();
            return "data:image/jpeg;base64," + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP);
        } catch (Exception ex) {
            return "";
        }
    }

    void emit() {
        if (webView == null || !visible) return;
        boolean granted = accessGranted();
        if(granted)G2MediaListener.synchronize(listenerComponent);
        JSONObject data = new JSONObject();
        try {
            data.put("granted", granted);
            data.put("native", true);
            boolean share = granted && activity.getPreferences(0).getBoolean("shareTurns", false);
            data.put("turn", share ? G2MediaListener.instruction() : "");
            data.put("turnSource", share ? G2MediaListener.source() : "");
            MediaController current=preferredSession();
            MediaMetadata meta=current==null?null:current.getMetadata();
            String pkg=current==null?"":current.getPackageName();
            String title="",artist="",album="",channel="";
            if(meta!=null){
                title=safeText(meta.getText(MediaMetadata.METADATA_KEY_TITLE),safeText(meta.getDescription().getTitle(),""));
                artist=safeText(meta.getText(MediaMetadata.METADATA_KEY_ARTIST),safeText(meta.getDescription().getSubtitle(),""));
                album=safeText(meta.getText(MediaMetadata.METADATA_KEY_ALBUM),"");
            }
            if(!title.isEmpty())channel="Sesión multimedia";
            android.service.notification.StatusBarNotification posted=granted?G2MediaListener.media():null;
            android.app.Notification note=posted==null?null:posted.getNotification();
            if(note!=null){
                String name=safeText(note.extras.getCharSequence(android.app.Notification.EXTRA_TITLE),"");
                boolean matches=(pkg.isEmpty()||pkg.equals(posted.getPackageName())||title.isEmpty())
                    && (title.isEmpty()||title.equalsIgnoreCase(name));
                if(!matches || name.isEmpty())note=null;
                else{
                    if(title.isEmpty()){
                        if(!pkg.equals(posted.getPackageName())){current=null;meta=null;artist="";album="";}
                        title=name;pkg=posted.getPackageName();channel="Notificación musical";
                    }
                    if(artist.isEmpty())artist=safeText(note.extras.getCharSequence(android.app.Notification.EXTRA_TEXT),"");
                }
            }
            StereoMusic.Track track=stereo.track();
            if(title.isEmpty() && track!=null){
                title=track.title;artist=track.artist;album=track.album;channel="Bluetooth del estéreo";
                current=null;meta=null;pkg="";note=null;
            }
            boolean active=!title.isEmpty();
            PlaybackState state=current==null?null:current.getPlaybackState();
            boolean playing=state!=null&&state.getState()==PlaybackState.STATE_PLAYING;
            long actions=state==null?0:state.getActions();
            long duration=meta==null?0:Math.max(0,meta.getLong(MediaMetadata.METADATA_KEY_DURATION));
            long position=state==null?0:Math.max(0,state.getPosition());
            if(playing && state.getLastPositionUpdateTime()>0)
                position+=Math.round(Math.max(0,android.os.SystemClock.elapsedRealtime()-state.getLastPositionUpdateTime())*state.getPlaybackSpeed());
            boolean isCar=G2MediaListener.CARLINK.equals(pkg)||!pkg.isEmpty()&&pkg.equals(activity.getPreferences(0).getString("carlinkApp",""));
            String art=active?albumArt(meta,note,channel.equals("Bluetooth del estéreo")?track.art:null,pkg+"|"+title+"|"+artist+"|"+album):"";
            String cover=art.isEmpty()?"Sin imagen recibida":"Imagen recibida";
            data.put("granted",granted||active);
            data.put("access",granted);
            data.put("channel",channel);
            data.put("coverStatus",cover);
            data.put("title",active?title:granted?"Sin música activa":"Conecta tu música");
            data.put("artist",artist.isEmpty()?"Artista no disponible":artist);
            data.put("album",album.isEmpty()?"Álbum no disponible":album);
            data.put("albumArt",art);
            data.put("playing",playing); data.put("stateKnown",state!=null);
            data.put("package",pkg);data.put("carLink",isCar);
            if(active)lastSource=pkg;
            String mediaId=meta==null?"":safeText(meta.getString(MediaMetadata.METADATA_KEY_MEDIA_ID),"");
            if(channel.equals("Bluetooth del estéreo") || pkg.contains("bluetooth"))data.put("sourceLabel","BLUETOOTH");
            if(mediaId.startsWith("spotify:track:")||mediaId.startsWith("spotify:episode:"))data.put("sourceLabel","SPOTIFY");
            data.put("durationMs",duration);data.put("positionMs",duration>0?Math.min(duration,position):position);
            data.put("canSeek",duration>0&&(actions&PlaybackState.ACTION_SEEK_TO)!=0);
            data.put("canNext",(actions&PlaybackState.ACTION_SKIP_TO_NEXT)!=0);
            data.put("canPrevious",(actions&PlaybackState.ACTION_SKIP_TO_PREVIOUS)!=0);
            data.put("canPlay",(actions&(PlaybackState.ACTION_PLAY_PAUSE|(playing?PlaybackState.ACTION_PAUSE:PlaybackState.ACTION_PLAY)))!=0);
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
