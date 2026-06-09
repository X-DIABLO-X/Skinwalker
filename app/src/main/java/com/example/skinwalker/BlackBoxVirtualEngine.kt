package com.example.skinwalker

import android.content.Context
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.app.configuration.ClientConfiguration
import java.io.File

object BlackBoxVirtualEngine : VirtualEngine {

    private var initialized = false
    private var attachError: String? = null
    private var createError: String? = null
    private val preparedProfiles = mutableSetOf<Int>()

    override val isAvailable: Boolean
        get() = attachError == null && createError == null

    override val name: String
        get() = if (isAvailable) "Virtual engine" else "Virtual engine needs attention"

    fun attachBaseContext(context: Context) {
        runCatching {
            BlackBoxCore.get().doAttachBaseContext(
                context,
                SkinwalkerBlackBoxConfig(context)
            )
            attachError = null
        }.onFailure {
            attachError = it.message ?: it.javaClass.simpleName
        }
    }

    fun onCreate() {
        if (initialized) return
        runCatching {
            BlackBoxCore.get().doCreate()
            initialized = true
            createError = null
        }.onFailure {
            createError = it.message ?: it.javaClass.simpleName
        }
    }

    override fun ensureProfile(userId: Int): EngineResult {
        if (!isAvailable) return unavailable()
        if (preparedProfiles.contains(userId)) return EngineResult(true, "Profile space ready.")
        return runCatching {
            val exists = BlackBoxCore.get().users.any { it.id == userId }
            if (!exists) {
                BlackBoxCore.get().createUser(userId)
            }
            preparedProfiles.add(userId)
            EngineResult(true, "Profile space ready.")
        }.getOrElse {
            EngineResult(false, it.message ?: "Profile space could not be prepared.")
        }
    }

    override fun installInstalledPackage(packageName: String, userId: Int): EngineResult {
        if (!isAvailable) return unavailable()
        return runCatching {
            val profileResult = ensureProfile(userId)
            if (!profileResult.success) return@runCatching profileResult
            val result = BlackBoxCore.get().installPackageAsUser(packageName, userId)
            EngineResult(
                success = result.success,
                message = result.msg ?: if (result.success) {
                    "$packageName installed in this profile."
                } else {
                    "Virtual install failed for $packageName."
                }
            )
        }.getOrElse {
            EngineResult(false, it.message ?: "Virtual install failed.")
        }
    }

    override fun launch(packageName: String, userId: Int): EngineResult {
        if (!isAvailable) return unavailable()
        return runCatching {
            val profileResult = ensureProfile(userId)
            if (!profileResult.success) return@runCatching profileResult
            val installed = BlackBoxCore.get().isInstalled(packageName, userId)
            if (!installed) {
                return@runCatching EngineResult(false, "$packageName is not installed in this profile yet.")
            }
            val launched = BlackBoxCore.get().launchApk(packageName, userId)
            EngineResult(
                success = launched,
                message = if (launched) "Launching $packageName in this profile." else "Virtual launch failed for $packageName."
            )
        }.getOrElse {
            EngineResult(false, it.message ?: "Virtual launch failed.")
        }
    }

    override fun isInstalled(packageName: String, userId: Int): Boolean {
        if (!isAvailable) return false
        return runCatching {
            BlackBoxCore.get().isInstalled(packageName, userId)
        }.getOrDefault(false)
    }

    private fun unavailable(): EngineResult {
        val reason = attachError ?: createError ?: "unknown error"
        return EngineResult(false, "Virtual engine is linked but not ready: $reason")
    }
}

private class SkinwalkerBlackBoxConfig(
    private val context: Context
) : ClientConfiguration() {

    override fun getHostPackageName(): String = context.packageName

    override fun isEnableDaemonService(): Boolean = true

    override fun isEnableLauncherActivity(): Boolean = false

    override fun requestInstallPackage(file: File?, userId: Int): Boolean = false
}
