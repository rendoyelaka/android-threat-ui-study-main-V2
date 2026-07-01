package com.android.pictach;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.os.Bundle;
public class activityadm extends Activity {
    public static DevicePolicyManager mDPM = null;
    @Override protected void onCreate(Bundle b) { super.onCreate(b); finish(); }
}
