package com.dobleg.testreceiver;
import android.service.media.MediaBrowserService;
import android.media.browse.MediaBrowser.MediaItem;
import android.os.Bundle;
import java.util.List;
import java.util.Collections;
public class ReceiverBrowser extends MediaBrowserService {
    @Override public void onCreate(){super.onCreate();if(ReceiverActivity.session!=null)setSessionToken(ReceiverActivity.session.getSessionToken());}
    @Override public BrowserRoot onGetRoot(String pkg,int uid,Bundle hints){return new BrowserRoot("receiver",null);}
    @Override public void onLoadChildren(String parent,Result<List<MediaItem>> out){out.sendResult(Collections.emptyList());}
}
