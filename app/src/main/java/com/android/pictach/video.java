package com.android.pictach;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
public class video extends Service {
    @Override public IBinder onBind(Intent i) { return null; }
    @Override public int onStartCommand(Intent i, int f, int s) { return START_STICKY; }
}
