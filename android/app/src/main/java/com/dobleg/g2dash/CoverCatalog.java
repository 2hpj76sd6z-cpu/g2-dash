package com.dobleg.g2dash;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.*;
import android.util.LruCache;
import org.json.*;
import java.net.*;
import java.io.*;
import java.util.Locale;
import java.util.concurrent.*;

final class CoverCatalog {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final LruCache<String,String> cache=new LruCache<>(12);
    private String desired="",busy="",failed="";
    private long retryAt, lastRequest;
    boolean searching(){return !busy.isEmpty() && busy.equals(desired);}
    void clear(){desired="";}
    void close(){clear();worker.shutdownNow();}
    String find(String title,String artist,String album,Runnable refresh){
        if(title.isEmpty() || artist.isEmpty()){clear();return "";}
        String key=title+"\n"+artist+"\n"+album;
        desired=key;
        String hit=cache.get(key);
        if(hit!=null)return hit;
        long now=SystemClock.elapsedRealtime();
        if(!busy.isEmpty() || (key.equals(failed)&&now<retryAt) || now-lastRequest<1300 || worker.isShutdown())return "";
        busy=key;lastRequest=now;
        worker.execute(()->{
            String result="";
            try{
                String query="recording:"+phrase(title)+" AND artist:"+phrase(artist);
                byte[] json=load("https://musicbrainz.org/ws/2/recording/?fmt=json&limit=8&query="+URLEncoder.encode(query,"UTF-8"),650000);
                String release=release(new JSONObject(new String(json,"UTF-8")),title,artist,album);
                if(!release.isEmpty()){
                    byte[] bytes=load("https://coverartarchive.org/release/"+release+"/front-250",600000);
                    BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
                    BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);
                    if(bounds.outWidth>0 && bounds.outHeight>0 && Math.max(bounds.outWidth,bounds.outHeight)<=1500){
                        Bitmap art=BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                        if(art!=null){result=G2MediaBridge.encode(art);art.recycle();}
                    }
                }
            }catch(Exception unavailable){/* Offline, no matching release, or the archive has no cover. */}
            final String data=result;
            main.post(()->{
                busy="";
                if(worker.isShutdown()||!key.equals(desired))return;
                if(data.isEmpty()){failed=key;retryAt=SystemClock.elapsedRealtime()+300000;}
                else cache.put(key,data);
                refresh.run();
            });
        });
        return "";
    }
    private static String phrase(String s){return "\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"";}
    private static String norm(String s){return s.trim().toLowerCase(Locale.ROOT);}
    static String release(JSONObject response,String title,String artist,String album){
        JSONArray rows=response.optJSONArray("recordings");
        if(rows==null)return "";
        for(int i=0;i<rows.length();i++){
            JSONObject rec=rows.optJSONObject(i);
            if(rec==null || !norm(title).equals(norm(rec.optString("title"))))continue;
            JSONArray credits=rec.optJSONArray("artist-credit");if(credits==null)continue;
            StringBuilder names=new StringBuilder();
            for(int k=0;k<credits.length();k++){
                JSONObject c=credits.optJSONObject(k);if(c==null)continue;
                names.append(c.optString("name")).append(c.optString("joinphrase"));
            }
            if(!norm(artist).equals(norm(names.toString())))continue;
            JSONArray releases=rec.optJSONArray("releases");if(releases==null)continue;
            for(int k=0;k<releases.length();k++){
                JSONObject r=releases.optJSONObject(k);if(r==null)continue;
                if(!album.isEmpty()&&!norm(album).equals(norm(r.optString("title"))))continue;
                String id=r.optString("id");
                if(id.matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"))return id;
            }
        }
        return "";
    }
    private static byte[] load(String address,int limit) throws IOException {
        URL url=new URL(address);
        for(int i=0;i<5;i++){
            String host=url.getHost().toLowerCase(Locale.ROOT);
            if(!"https".equals(url.getProtocol()) || !(host.equals("musicbrainz.org")||host.equals("coverartarchive.org")||host.equals("archive.org")||host.endsWith(".archive.org")))throw new IOException("host");
            HttpURLConnection conn=(HttpURLConnection)url.openConnection();
            try{
                conn.setConnectTimeout(4500);conn.setReadTimeout(6000);conn.setInstanceFollowRedirects(false);
                conn.setRequestProperty("User-Agent","G2Dash/23 (https://github.com/2hpj76sd6z-cpu/g2-dash)");
                int code=conn.getResponseCode();
                if(code>=300&&code<400){String next=conn.getHeaderField("Location");if(next==null)break;url=new URL(url,next);continue;}
                if(code!=200||conn.getContentLength()>limit)throw new IOException("response");
                try(InputStream in=conn.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                    byte[] block=new byte[8192];int n;
                    while((n=in.read(block))!=-1){if(out.size()+n>limit)throw new IOException("size");out.write(block,0,n);}
                    return out.toByteArray();
                }
            }finally{conn.disconnect();}
        }
        throw new IOException("redirect");
    }
}
