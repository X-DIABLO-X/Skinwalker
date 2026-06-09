package com.example.skinwalker

object MissingVirtualEngine : VirtualEngine {
    override val isAvailable: Boolean = false
    override val name: String = "Virtual engine missing"

    override fun ensureProfile(userId: Int): EngineResult {
        return EngineResult(false, "Virtual engine is not linked yet, so this profile cannot be prepared.")
    }

    override fun installInstalledPackage(packageName: String, userId: Int): EngineResult {
        return EngineResult(false, "Virtual engine is not linked yet, so $packageName cannot be installed into an isolated space.")
    }

    override fun launch(packageName: String, userId: Int): EngineResult {
        return EngineResult(false, "Virtual engine is not linked yet, so $packageName was not launched. The original app was left untouched.")
    }

    override fun isInstalled(packageName: String, userId: Int): Boolean = false
}
