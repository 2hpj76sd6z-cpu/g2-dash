package com.dobleg.g2dash;

import android.app.Activity;
import android.content.*;
import android.content.pm.*;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.os.*;
import java.util.*;

/** Published Bluetooth metadata on the head unit. Never launches the receiver UI. */
final class StereoMusic {
    static final class Track {
        final String title,artist,album;
        Track(String t,String a,String l){title=t;artist=a;album=l;}
    }
    private final Activity activity;
    private MediaBrowser browser;
    private MediaController controller;
    private String selected="";
    private long nextAttempt, received;
    private Track broadcast;
    private boolean listening;
    private final String[] actions={"com.syu.bt.music.id3","com.syu.ms.bt.id3","com.microntek.bt.report",
        "com.android.bluetooth.a2dpsink.action.METADATA_CHANGED","com.android.music.bluetooth.metachanged","com.clw.intent.MEDIA_INFO"};
    StereoMusic(Activity activity){this.activity=activity;}
    private static String field(Bundle b,String...keys){
        for(String key:keys){Object v=b.get(key);if(v instanceof CharSequence){String text=v.toString().trim();if(!text.isEmpty())return text.substring(0,Math.min(text.length(),180));}}
        return "";
    }
    private final BroadcastReceiver receiver=new BroadcastReceiver(){
        @Override public void onReceive(Context c,Intent intent){
            try {
                Bundle b=intent.getExtras();
                if(b==null)return;
                String title=field(b,"title","track","song","id3_title","music_title","Media_Title","TITLE","name");
                if(title.isEmpty())return;
                broadcast=new Track(title,field(b,"artist","singer","id3_artist","music_artist","Media_Artist","ARTIST"),
                    field(b,"album","id3_album","albumname","Media_Album","ALBUM"));
                received=SystemClock.elapsedRealtime();
            } catch(RuntimeException invalid){ /* A manufacturer's payload may be unreadable on this firmware. */ }
        }
    };
    void start(){
        if(listening)return;
        IntentFilter filter=new IntentFilter();
        for(String action:actions)filter.addAction(action);
        if(Build.VERSION.SDK_INT>=33)activity.registerReceiver(receiver,filter,Context.RECEIVER_EXPORTED);
        else activity.registerReceiver(receiver,filter);
        listening=true;
    }
    void stop(){
        if(listening)activity.unregisterReceiver(receiver);
        listening=false;broadcast=null;nextAttempt=0;disconnect();
    }
    private void disconnect(){
        MediaBrowser old=browser; browser=null; controller=null;
        if(old!=null)try{old.disconnect();}catch(RuntimeException ignored){}
    }
    Track track(){return SystemClock.elapsedRealtime()-received<180000 ? broadcast:null;}
    MediaController controller(String preferred){
        if(!listening)return null;
        if(!selected.equals(preferred)){selected=preferred;disconnect();nextAttempt=0;}
        if(controller!=null || SystemClock.elapsedRealtime()<nextAttempt)return controller;
        disconnect();
        nextAttempt=SystemClock.elapsedRealtime()+30000;
        List<ResolveInfo> services=activity.getPackageManager().queryIntentServices(new Intent("android.media.browse.MediaBrowserService"),0);
        List<ServiceInfo> found=new ArrayList<>();
        for(ResolveInfo r:services){
            ServiceInfo s=r.serviceInfo;
            if(s==null || !s.exported)continue;
            boolean system=(s.applicationInfo.flags&ApplicationInfo.FLAG_SYSTEM)!=0;
            if(s.packageName.equals(preferred))found.add(0,s);
            else if(system && s.name.endsWith(".avrcpcontroller.BluetoothMediaBrowserService"))found.add(s);
        }
        connect(found,0);
        return controller;
    }
    private void connect(List<ServiceInfo> candidates,int index){
        if(index>=candidates.size()||!listening)return;
        ServiceInfo service=candidates.get(index);
        final MediaBrowser[] pending=new MediaBrowser[1];
        pending[0]=new MediaBrowser(activity,new ComponentName(service.packageName,service.name),new MediaBrowser.ConnectionCallback(){
            public void onConnected(){
                if(browser!=pending[0])return;
                try{controller=new MediaController(activity,browser.getSessionToken());}catch(RuntimeException invalid){onConnectionFailed();}
            }
            public void onConnectionFailed(){if(browser==pending[0]){disconnect();connect(candidates,index+1);}}
            public void onConnectionSuspended(){if(browser==pending[0])disconnect();}
        },null);
        browser=pending[0];
        try{browser.connect();}catch(RuntimeException denied){disconnect();connect(candidates,index+1);}
    }
}
