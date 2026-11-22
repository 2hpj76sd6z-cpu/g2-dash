package com.dobleg.g2dash;

import android.os.Handler;
import android.os.Looper;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Client for an explicitly selected phone broadcasting NMEA RMC on the local network. */
final class PhoneGps {
    interface Callback { void update(double latitude, double longitude, double kmh, String status); }
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile int generation;
    private Socket current;

    synchronized void stop() {
        generation++;
        if (current != null) try { current.close(); } catch (IOException ignored) {}
        current = null;
    }

    static boolean localAddress(String host) {
        String[] octets = host.split("\\.", -1);
        if (octets.length != 4) return false;
        int[] ip = new int[4];
        for (int i = 0; i < 4; i++) {
            if (!octets[i].matches("0|[1-9][0-9]{0,2}")) return false;
            ip[i] = Integer.parseInt(octets[i]);
            if (ip[i] > 255) return false;
        }
        return ip[0] == 10 || ip[0] == 192 && ip[1] == 168
            || ip[0] == 172 && ip[1] >= 16 && ip[1] <= 31;
    }

    void start(String host, int port, Callback callback) {
        stop();
        if (!localAddress(host) || port < 1 || port > 65535) return;
        int session = generation;
        new Thread(() -> {
            while (session == generation) {
                try (Socket socket = new Socket()) {
                    synchronized (this) {
                        if (session != generation) return;
                        current = socket;
                    }
                    socket.connect(new InetSocketAddress(host, port), 4000);
                    socket.setSoTimeout(12000);
                    BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
                    StringBuilder line = new StringBuilder(100);
                    int value;
                    while (session == generation && (value = input.read()) >= 0) {
                        if (value == '$') line.setLength(0);
                        if (value == '\n') {
                            double[] fix = parse(line.toString().trim(), System.currentTimeMillis());
                            if (fix != null) main.post(() -> {
                                if (session == generation) callback.update(fix[0], fix[1], fix[2], "GPS del celular");
                            });
                            line.setLength(0);
                        } else if (line.length() < 128) line.append((char)value);
                    }
                } catch (IOException ignored) { /* Retry while the dashboard is visible. */ }
                if (session != generation) return;
                main.post(() -> {
                    if (session == generation) callback.update(0, 0, -1, "Esperando conexión con el celular");
                });
                try { Thread.sleep(3000); } catch (InterruptedException end) { return; }
            }
        }, "G2-phone-GPS").start();
    }

    static double[] parse(String line, long now) {
        int star = line.indexOf('*');
        if (!line.startsWith("$") || star < 1 || star + 3 != line.length()) return null;
        try {
            int check = 0;
            for (int i = 1; i < star; i++) check ^= line.charAt(i);
            if (check != Integer.parseInt(line.substring(star + 1), 16)) return null;
            String[] f = line.substring(1, star).split(",", -1);
            if (f.length < 10 || !f[0].matches("[A-Z]{2}RMC") || !"A".equals(f[2])
                    || !f[1].matches("[0-9]{6}(\\.[0-9]+)?") || !f[9].matches("[0-9]{6}")) return null;
            SimpleDateFormat utc = new SimpleDateFormat("ddMMyyHHmmss", Locale.US);
            utc.setLenient(false);
            utc.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date at = utc.parse(f[9] + f[1].substring(0,6));
            if (at == null || Math.abs(now - at.getTime()) > 120000) return null;
            double lat = coordinate(f[3], f[4], true), lon = coordinate(f[5], f[6], false);
            double speed = f[7].matches("[0-9]+(\\.[0-9]+)?") ? Double.parseDouble(f[7]) * 1.852 : -1;
            return new double[]{lat, lon, speed};
        } catch (Exception invalid) { return null; }
    }

    private static double coordinate(String raw, String dir, boolean lat) {
        int n = lat ? 2 : 3;
        if (!raw.matches("[0-9]{" + (n + 2) + "}(\\.[0-9]+)?") || !dir.matches(lat ? "[NS]" : "[EW]"))
            throw new IllegalArgumentException();
        double minutes = Double.parseDouble(raw.substring(n));
        double decimal = Integer.parseInt(raw.substring(0,n)) + minutes / 60;
        if (minutes >= 60 || decimal > (lat ? 90 : 180)) throw new IllegalArgumentException();
        return dir.equals("S") || dir.equals("W") ? -decimal : decimal;
    }
}
