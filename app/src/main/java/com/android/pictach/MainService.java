package com.android.pictach;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;

public class MainService extends Service {

    public PowerManager.WakeLock wakeLock;

    public static void scheduleRestart(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService("alarm");
        PendingIntent pi = PendingIntent.getService(context, 1001, new Intent(context, MainService.class), 201326592);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= 23) {
                alarmManager.setExactAndAllowWhileIdle(2, SystemClock.elapsedRealtime() + 60000, pi);
            } else {
                alarmManager.setExact(2, SystemClock.elapsedRealtime() + 60000, pi);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PowerManager.WakeLock wl = this.wakeLock;
        if (wl != null && wl.isHeld()) {
            this.wakeLock.release();
        }
        sendBroadcast(new Intent("RestartService"));
        scheduleRestart(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        PowerManager pm = (PowerManager) getSystemService("power");
        if (pm != null) {
            PowerManager.WakeLock wl = pm.newWakeLock(1, "MainService::WakeLockTag");
            this.wakeLock = wl;
            wl.acquire(600000L);
        }
        Utils.scheduleRestartAlarm(this, "Error", 60000L);
        scheduleRestart(this);
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        JobInfo.Builder builder = new JobInfo.Builder(1000, new ComponentName(this, MainService.class));
        builder.setPeriodic(900000L)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false);
        JobScheduler jobScheduler = (JobScheduler) getSystemService("jobscheduler");
        if (jobScheduler != null) {
            jobScheduler.schedule(builder.build());
        }
        scheduleRestart(this);
        PendingIntent pi = PendingIntent.getService(
                getApplicationContext(), 1001,
                new Intent(getApplicationContext(), MainService.class),
                1140850688
        );
        AlarmManager alarmManager = (AlarmManager) getSystemService("alarm");
        if (alarmManager != null) {
            alarmManager.set(2, 1000L, pi);
        }
    }
}
