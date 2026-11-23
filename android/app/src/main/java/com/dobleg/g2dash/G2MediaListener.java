package com.dobleg.g2dash;

import android.app.Notification;
import android.content.SharedPreferences;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public final class G2MediaListener extends NotificationListenerService
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    static final String MAPS = "com.google.android.apps.maps";
    static final String CARLINK = "com.syu.carlink";
    private static volatile String instruction = "";
    private static volatile String source = "";
    private SharedPreferences preferences;
    private String currentKey;

    static String instruction() { return instruction; }
    static String source() { return source; }

    @Override public void onListenerConnected() {
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
        if (preferences == null || !preferences.getBoolean("shareTurns", false)) return;
        try {
            StatusBarNotification[] entries = getActiveNotifications();
            if (entries == null) return;
            java.util.Arrays.sort(entries,(a,b) -> Long.compare(b.getPostTime(),a.getPostTime()));
            for (StatusBarNotification entry : entries) if (show(entry)) break;
        } catch (SecurityException | IllegalStateException unavailable) { clear(); }
    }

    @Override public void onSharedPreferenceChanged(SharedPreferences prefs,String key) {
        if ("shareTurns".equals(key) || "carlinkApp".equals(key)) refresh();
    }
    @Override public void onNotificationPosted(StatusBarNotification entry) {
        if (!show(entry) && entry != null && entry.getKey().equals(currentKey)) refresh();
    }
    @Override public void onNotificationRemoved(StatusBarNotification entry) {
        if (entry != null && entry.getKey().equals(currentKey)) refresh();
    }
}
