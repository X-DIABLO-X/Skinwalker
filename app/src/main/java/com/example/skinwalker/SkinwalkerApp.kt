package com.example.skinwalker

import android.app.Application
import android.content.Context

class SkinwalkerApp : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        VirtualEngineProvider.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        VirtualEngineProvider.onCreate(this)
    }
}
