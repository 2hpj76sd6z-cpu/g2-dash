package com.dobleg.testreceiver;

import android.app.*;
import android.graphics.*;
import android.media.MediaMetadata;
import android.media.session.*;
import android.os.Bundle;

/** Only built for emulator checks. Publishes through the same Android APIs as a receiver. */
public class ReceiverActivity extends Activity {
    private MediaSession session;
    private long position = 12000;
    private boolean playing = true;
    private boolean second;
    private NotificationManager notifications;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        notifications = getSystemService(NotificationManager.class);
        notifications.createNotificationChannel(new NotificationChannel("route","Prueba",NotificationManager.IMPORTANCE_LOW));
        session = new MediaSession(this,"Receiver test");
        session.setCallback(new MediaSession.Callback() {
            @Override public void onPlay(){ playing=true; publish(); }
            @Override public void onPause(){ playing=false; publish(); }
            @Override public void onSeekTo(long ms){ position=ms; publish(); }
            @Override public void onSkipToNext(){ second=true; position=0; publish(); notifications.cancel(7); }
            @Override public void onSkipToPrevious(){ second=false; position=0; publish(); }
        });
        publish();
        session.setActive(true);
        notifications.notify(7,new Notification.Builder(this,"route")
            .setSmallIcon(android.R.drawable.ic_dialog_map).setCategory(Notification.CATEGORY_NAVIGATION)
            .setContentTitle("Gira a la derecha").setContentText("En 200 m").setOngoing(true).build());
    }

    private void publish() {
        Bitmap cover=Bitmap.createBitmap(360,300,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(cover);
        Paint ink=new Paint(Paint.ANTI_ALIAS_FLAG);
        ink.setShader(new LinearGradient(0,0,360,300,0xff092b35,0xff297155,Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,360,300,ink); ink.setShader(null);
        ink.setStyle(Paint.Style.STROKE); ink.setStrokeWidth(2); ink.setColor(0xff81c9a1);
        for(int n=0;n<12;n++) canvas.drawCircle(290,85,18+n*11,ink);
        ink.setStyle(Paint.Style.FILL); ink.setColor(0xffeff6e9); ink.setTextSize(45);
        canvas.drawText(second?"Sesión 02":"Sesión 01",22,253,ink);
        session.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,second?"Segunda pista":"Primera pista")
            .putString(MediaMetadata.METADATA_KEY_ARTIST,"Receptor de prueba")
            .putString(MediaMetadata.METADATA_KEY_ALBUM,"Sesiones de prueba")
            .putLong(MediaMetadata.METADATA_KEY_DURATION,210000).putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART,cover).build());
        long actions=PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_SEEK_TO
            | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS;
        session.setPlaybackState(new PlaybackState.Builder().setActions(actions)
            .setState(playing?PlaybackState.STATE_PLAYING:PlaybackState.STATE_PAUSED,position,playing?1f:0f).build());
    }

    @Override public void onDestroy() {
        notifications.cancel(7); session.release(); super.onDestroy();
    }
}
