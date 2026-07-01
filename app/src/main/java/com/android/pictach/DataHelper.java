package com.android.pictach;

/**
 * DataHelper — Holds a reference to a dynamically loaded DEX module class.
 *
 * When the C2 server sends a DEX module, LoveApi0.loadDexModule() loads it
 * and returns the Class object. That Class is wrapped in DataHelper and
 * stored in love.loadedModules (love.L_love_cl list).
 *
 * Fields:
 *   className   — fully qualified class name of the loaded module
 *   moduleClass — the actual Class object loaded via DexClassLoader
 *
 * Used by Api.AsyncTaskC0189ta when the C2 sends an "invoke module" command:
 *   love.loadedModules.get(i).className.equals(targetClassName)
 *   → LoveApi0.invokeModuleMethod(..., love.loadedModules.get(i).moduleClass, ...)
 */
public class DataHelper {

    // Fully qualified class name of the loaded DEX module
    public String className;

    // The Class object of the loaded DEX module
    public Class<?> moduleClass;

    /**
     * Constructor.
     * classNameStr = class name string sent by C2 server
     * cls          = Class object returned by DexClassLoader.loadClass()
     *
     * CanBeCalc() always returns classNameStr unchanged
     * (str.length() > 0 && i == 33 is always true with "Intent" and 33).
     * It is dead obfuscation code — does nothing functional.
     */
    DataHelper(String classNameStr, Class<?> cls) {
        this.className  = resolveClassName("Intent", 33, classNameStr);
        this.moduleClass = cls;
    }

    /**
     * Dead obfuscation method — always returns str2 unchanged.
     * Condition: "Intent".length() > 0 && 33 == 33 → always true.
     * Original obfuscated name: CanBeCalc
     */
    private String resolveClassName(String str, int i, String str2) {
        if (str.length() > 0 && i == 33) {
            return str2;
        }
        return str2 + "";
    }
}
