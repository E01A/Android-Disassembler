package com.kyhsgeekcode.disassembler;

import android.app.Application;
import android.content.Context;
import android.util.Log;
//import android.support.multidex.*;

public class EnableMultidex extends /*MultiDex*/Application {
    public static Context context;
    private static EnableMultidex enableMultiDex;
    private final String TAG = "EnableMultiDex";

    public EnableMultidex() {
        enableMultiDex = this;
    }

    public static EnableMultidex getEnableMultiDexApp() {
        return enableMultiDex;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "OnCreate");
        context = getApplicationContext();

    }
}

