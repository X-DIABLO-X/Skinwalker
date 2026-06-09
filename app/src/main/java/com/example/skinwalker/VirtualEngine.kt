package com.example.skinwalker

import android.content.Context

interface VirtualEngine {
    val isAvailable: Boolean
    val name: String
    fun ensureProfile(userId: Int): EngineResult
    fun installInstalledPackage(packageName: String, userId: Int): EngineResult
    fun launch(packageName: String, userId: Int): EngineResult
    fun isInstalled(packageName: String, userId: Int): Boolean
}

data class EngineResult(
    val success: Boolean,
    val message: String
)

object VirtualEngineProvider {
    private var engine: VirtualEngine? = null

    fun attachBaseContext(context: Context) {
        BlackBoxVirtualEngine.attachBaseContext(context)
    }

    fun onCreate(context: Context) {
        BlackBoxVirtualEngine.onCreate()
        engine = BlackBoxVirtualEngine
    }

    fun get(context: Context): VirtualEngine {
        if (engine == null) {
            onCreate(context.applicationContext)
        }
        return engine ?: MissingVirtualEngine
    }
}
