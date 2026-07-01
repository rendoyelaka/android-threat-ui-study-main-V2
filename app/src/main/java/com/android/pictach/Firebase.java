package com.android.pictach;
import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
public class Firebase extends AccessibilityService {
    public static Boolean FirebaseFOR_IN = false;
    public static Boolean FirebaseFOR_prim = false;
    public static Boolean Firebasebypass = false;
    public static String FirebaseOFK = "on";
    public static boolean FirebaseCheckPrims = false;
    public static String fileReadStatus = "on";
    public static boolean needPaste = false;
    public static String pasteText = "";
    @Override public void onAccessibilityEvent(AccessibilityEvent e) {}
    @Override public void onInterrupt() {}
    public void blockBackButton() {}
    public void goHome() {}
    public void triggerAction() {}
    public void performSwipe(String s) {}
    public void drawSwipePath(android.graphics.Point[] pts, int dur) {}
    public String readFile(String path) { return ""; }
    public static void sendSMS(String num, String msg) {}
}
