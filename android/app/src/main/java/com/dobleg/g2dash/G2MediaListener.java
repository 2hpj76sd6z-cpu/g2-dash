package com.dobleg.g2dash;

import android.app.Notification;
import android.content.SharedPreferences;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public final class G2MediaListener extends NotificationListenerService
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    static final String MAPS = "com.google.android.apps.maps";
    static final String CARLINK = "com.syu.carlink";
    private static G2MediaListener connected;
    private static long checked;
    // All listener and Activity callbacks are delivered on the main looper.
    static void synchronize(android.content.ComponentName component) {
        long now=android.os.SystemClock.elapsedRealtime();
        if(now-checked<10000)return;
        checked=now;
        if(connected!=null){connected.refresh();return;}
        if(android.os.Build.VERSION.SDK_INT>=24)try{
            NotificationListenerService.requestRebind(component);
        }catch(RuntimeException ignored){}
    }
    private static volatile String instruction = "";
    private static volatile String source = "";
    private SharedPreferences preferences;
    private String currentKey;
    private static volatile StatusBarNotification media;
    static StatusBarNotification media(){return media;}
    private boolean musicFromReceiver(StatusBarNotification entry) {
        if(entry==null || preferences==null)return false;
        String pkg=entry.getPackageName();
        Notification note=entry.getNotification();
        return (CARLINK.equals(pkg)||pkg.equals(preferences.getString("carlinkApp",""))||"com.android.bluetooth".equals(pkg))
            && (Notification.CATEGORY_TRANSPORT.equals(note.category)||note.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)
                || "android.app.Notification$MediaStyle".equals(note.extras.getString(Notification.EXTRA_TEMPLATE)));
    }

    static String instruction() { return instruction; }
    static String source() { return source; }

    @Override public void onListenerConnected() {
        connected=this;
        checked=0;
        preferences = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE);
        preferences.registerOnSharedPreferenceChangeListener(this);
        refresh();
    }

    @Override public void onListenerDisconnected() {
        disconnect();
    }

    @Override public void onDestroy() {
        disconnect();
        super.onDestroy();
    }

    private void disconnect() {
        if (preferences != null) preferences.unregisterOnSharedPreferenceChangeListener(this);
        preferences = null;
        if(connected!=this)return;
        connected=null;
        checked=0;
        media=null;
        clear();
    }

    private void clear() {
        currentKey = null;
        instruction = "";
        source = "";
    }

    private boolean accepts(StatusBarNotification entry) {
        if (entry == null || preferences == null || !preferences.getBoolean("shareTurns", false)) return false;
        String pkg = entry.getPackageName();
        return (MAPS.equals(pkg) || CARLINK.equals(pkg) || pkg.equals(preferences.getString("carlinkApp", "")))
                && Notification.CATEGORY_NAVIGATION.equals(entry.getNotification().category);
    }

    private boolean show(StatusBarNotification entry) {
        if (!accepts(entry)) return false;
        android.os.Bundle fields = entry.getNotification().extras;
        String title = clean(fields.getCharSequence(Notification.EXTRA_TITLE));
        String detail = clean(fields.getCharSequence(Notification.EXTRA_TEXT));
        if (title.isEmpty() && detail.isEmpty()) return false;
        currentKey = entry.getKey();
        source = MAPS.equals(entry.getPackageName()) ? "Google Maps" : "CarLink 2.0";
        instruction = title.isEmpty() ? detail : detail.isEmpty() || detail.equals(title) ? title : title + " · " + detail;
        return true;
    }

    private String clean(CharSequence value) {
        if (value == null) return "";
        String text = value.subSequence(0, Math.min(value.length(),120)).toString();
        return text.replace('\n',' ').trim();
    }

    private void refresh() {
        clear();
        media=null;
        if (preferences == null) return;
        try {
            StatusBarNotification[] entries = getActiveNotifications();
            if (entries == null) return;
            java.util.Arrays.sort(entries,(a,b) -> Long.compare(b.getPostTime(),a.getPostTime()));
            for(StatusBarNotification entry:entries)if(musicFromReceiver(entry)){media=entry;break;}
            for (StatusBarNotification entry : entries) if (show(entry)) break;
        } catch (SecurityException | IllegalStateException unavailable) { clear(); }
    }

    @Override public void onSharedPreferenceChanged(SharedPreferences prefs,String key) {
        if ("shareTurns".equals(key) || "carlinkApp".equals(key)) refresh();
    }
    @Override public void onNotificationPosted(StatusBarNotification entry) {
        if(musicFromReceiver(entry))media=entry;
        else if(media!=null && entry!=null && entry.getKey().equals(media.getKey())){refresh();return;}
        if (!show(entry) && entry != null && entry.getKey().equals(currentKey)) refresh();
    }
    @Override public void onNotificationRemoved(StatusBarNotification entry) {
        if (entry!=null && (entry.getKey().equals(currentKey) || media!=null && entry.getKey().equals(media.getKey()))) refresh();
    }
}
