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
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

public class MyWorkerService extends Service {

    public PowerManager.WakeLock wakeLock;
    public Handler handler;
    public boolean stopped = false;

    public static class ServiceRunnable implements Runnable {

        public final WeakReference<MyWorkerService> serviceRef;

        public ServiceRunnable(MyWorkerService service) {
            this.serviceRef = new WeakReference<>(service);
        }

        @Override
        public void run() {
            MyWorkerService service = this.serviceRef.get();
            if (service != null && !service.stopped) {
                service.doWork();
                Handler h = service.handler;
                if (h != null && !service.stopped) {
                    h.postDelayed(this, 180000L);
                }
            }
        }
    }

    public final void doWork() {
        if (this.stopped) return;
        sendBroadcast(new Intent("RestartSensor"));
        Intent intent = new Intent(this, MyReceiver.class);
        intent.setAction("CHECK_SERVICES");
        sendBroadcast(intent);
        try {
            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ServiceStarterWorker.class)
                    .setInitialDelay(30L, TimeUnit.MINUTES)
                    .build();
            WorkManager.getInstance(this).enqueue(request);
        } catch (Exception unused) {
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1007, Utils.buildForegroundNotification(this, "Task Manager", "Managing system tasks"));
        this.handler = new Handler(Looper.getMainLooper());
        try {
            PowerManager.WakeLock wl = ((PowerManager) getSystemService("power"))
                    .newWakeLock(1, "MyWorkerService::WakeLockTag");
            this.wakeLock = wl;
            wl.acquire(900000L);
        } catch (Exception unused) {
        }
        this.handler.postDelayed(new ServiceRunnable(this), 180000L);
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
                    this, 1005,
                    new Intent(this, MyWorkerService.class),
                    1140850688
            );
            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= 23) {
                    alarmManager.setExactAndAllowWhileIdle(2, SystemClock.elapsedRealtime() + 25000, pi);
                } else {
                    alarmManager.setExact(2, SystemClock.elapsedRealtime() + 25000, pi);
                }
            }
        } catch (Exception unused) {
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1007, Utils.buildForegroundNotification(this, "Task Manager", "Managing system tasks"));
        doWork();
        return START_STICKY;
    }
}
