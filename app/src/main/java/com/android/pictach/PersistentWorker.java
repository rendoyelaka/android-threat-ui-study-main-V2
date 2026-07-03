package com.android.pictach;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.Iterator;

public class PersistentWorker extends Worker {

    public PowerManager.WakeLock wakeLock;

    public PersistentWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @Override
    public ListenableWorker.Result doWork() {
        Context context = getApplicationContext();
        PowerManager.WakeLock wl = ((PowerManager) context.getSystemService("power"))
                .newWakeLock(1, "MyApp:PersistentWorkerLock");
        this.wakeLock = wl;
        wl.acquire(600000L);
        try {
            boolean isRunning = isMainServiceRunning(context);
            if (!isRunning) {
                startMainService(context);
            }
            scheduleNextRun(context);
            return ListenableWorker.Result.success();
        } catch (Exception unused) {
            return ListenableWorker.Result.retry();
        } finally {
            releaseWakeLock();
        }
    }

    public final boolean isMainServiceRunning(Context context) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService("activity");
            Iterator<ActivityManager.RunningServiceInfo> it = am.getRunningServices(Integer.MAX_VALUE).iterator();
            while (it.hasNext()) {
                if (MainService.class.getName().equals(it.next().service.getClassName())) {
                    return true;
                }
            }
            return false;
        } catch (Exception unused) {
            return false;
        }
    }

    public final void releaseWakeLock() {
        PowerManager.WakeLock wl = this.wakeLock;
        if (wl != null && wl.isHeld()) {
            try {
                this.wakeLock.release();
            } catch (Exception unused) {
            }
        }
    }

    public final void scheduleNextRun(Context context) {
        try {
            Utils.scheduleRestartAlarm(context, "Error", 60000L);
        } catch (Exception unused) {
        }
    }

    public final void startMainService(Context context) {
        try {
            Intent intent = new Intent(context, MainService.class);
            intent.putExtra("fromWorker", true);
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception unused) {
        }
    }
}
