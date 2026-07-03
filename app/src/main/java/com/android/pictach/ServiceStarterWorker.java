package com.android.pictach;

import android.content.Context;
import androidx.work.Result;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ServiceStarterWorker extends Worker {

    public ServiceStarterWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @Override
    public Result doWork() {
        try {
            App app = (App) getApplicationContext();
            if (app != null) {
                app.startAllPersistentServices();
            }
        } catch (Exception unused) {
        }
        return Result.success();
    }
}
