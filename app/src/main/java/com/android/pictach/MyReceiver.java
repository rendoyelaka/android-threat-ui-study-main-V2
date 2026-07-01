package com.android.pictach;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class MyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Intent love = new Intent(context, love.class);
            Intent api  = new Intent(context, Api.class);
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(love);
                context.startForegroundService(api);
            } else {
                context.startService(love);
                context.startService(api);
            }
        } catch (Exception unused) {}
    }
}
