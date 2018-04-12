package com.example.inapp;

import android.app.Application;
import android.content.Context;

/**
 * Created by w on 2018/4/10.
 */

public class App extends Application {

    private static Context context;

    public static Context getContext() {
        return context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
    }
}
