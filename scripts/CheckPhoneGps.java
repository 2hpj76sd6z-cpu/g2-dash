package com.dobleg.g2dash;

import java.time.Instant;

public final class CheckPhoneGps {
    private static String sentence(String body) {
        int sum=0;
        for(char c:body.toCharArray()) sum^=c;
        return "$"+body+"*"+String.format("%02X",sum);
    }
    public static void main(String[] args) {
        long at=Instant.parse("2026-11-22T15:30:00Z").toEpochMilli();
        String rmc="GNRMC,153000.00,A,1930.000,N,09915.000,W,10.0,0,221126,,,A";
        String msg=sentence(rmc);
        double[] p=PhoneGps.parse(msg,at);
        if (p==null || p[0]!=19.5 || p[1]!=-99.25 || Math.abs(p[2]-18.52)>.001 ||
                PhoneGps.parse(msg,at+180000)!=null || PhoneGps.parse(msg+"0",at)!=null ||
                PhoneGps.parse(sentence(rmc.replace(",A,19",",V,19")),at)!=null ||
                PhoneGps.parse(sentence(rmc.replace("1930","1962")),at)!=null ||
                !PhoneGps.localAddress("192.168.43.1") || PhoneGps.localAddress("192.168.0.500"))
            throw new AssertionError("Recepción de coordenadas del celular");
    }
}
