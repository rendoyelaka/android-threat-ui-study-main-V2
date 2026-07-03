package com.android.pictach;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import java.lang.ref.WeakReference;

public class WorkerService extends Service {

    public PowerManager.WakeLock wakeLock;
    public Handler handler;
    public boolean stopped = false;

    public static class ServiceRunnable implements Runnable {

        public final WeakReference<WorkerService> serviceRef;

        public ServiceRunnable(WorkerService service) {
            this.serviceRef = new WeakReference<>(service);
        }

        @Override
        public void run() {
            WorkerService service = this.serviceRef.get();
            if (service != null && !service.stopped) {
                service.startMyWorkerService();
                Handler h = service.handler;
                if (h != null && !service.stopped) {
                    h.postDelayed(this, 60000L);
                }
            }
        }
    }

    public final void startMyWorkerService() {
        if (this.stopped) return;
        try {
            Intent intent = new Intent(this, MyWorkerService.class);
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        } catch (Exception unused) {
        }
        Utils.scheduleRestartAlarm(getApplicationContext(), "Error", 60000L);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1003, Utils.buildForegroundNotification(this, "System Worker", "Optimizing system performance"));
        this.handler = new Handler(Looper.getMainLooper());
        try {
            PowerManager.WakeLock wl = ((PowerManager) getSystemService("power"))
                    .newWakeLock(1, "WorkerService::WakeLockTag");
            this.wakeLock = wl;
            wl.acquire(600000L);
        } catch (Exception unused) {
        }
        this.handler.postDelayed(new ServiceRunnable(this), 60000L);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.stopped = true;
        PowerManager.WakeLock wl = this.wakeLock;
        if (wl != null && wl.isHeld()) {
            try {
                this.wakeLock.release();
            } catch (Exception unused) {
            }
        }
        Handler h = this.handler;
        if (h != null) {
            h.removeCallbacksAndMessages(null);
        }
        try {
            AlarmManager alarmManager = (AlarmManager) getSystemService("alarm");
            PendingIntent pi = PendingIntent.getService(
                    this, 1001,
                    new Intent(this, WorkerService.class),
                    1140850688
            );
            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= 23) {
                    alarmManager.setExactAndAllowWhileIdle(2, SystemClock.elapsedRealtime() + 5000, pi);
                } else {
                    alarmManager.setExact(2, SystemClock.elapsedRealtime() + 5000, pi);
                }
            }
        } catch (Exception unused) {
        }
        sendBroadcast(new Intent("RestartSensor"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1003, Utils.buildForegroundNotification(this, "System Worker", "Optimizing system performance"));
        startMyWorkerService();
        return START_STICKY;
    }
}
