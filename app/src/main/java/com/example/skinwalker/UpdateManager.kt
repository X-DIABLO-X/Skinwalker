package com.example.skinwalker

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class UpdateManager(private val context: Context) {

    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("skinwalker_updates", Context.MODE_PRIVATE)

    fun currentVersionText(): String {
        return "${currentVersionName()} (${currentVersionCode()})"
    }

    fun currentVersionCode(): Long {
        val info = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    }

    fun currentVersionName(): String {
        val info = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
        return info.versionName ?: "0.0.0"
    }

    fun repoSlug(): String = preferences.getString(KEY_REPO_SLUG, DEFAULT_REPO_SLUG).orEmpty()

    fun saveRepoSlug(value: String) {
        preferences.edit().putString(KEY_REPO_SLUG, value.trim()).apply()
    }

    fun githubToken(): String = preferences.getString(KEY_GITHUB_TOKEN, "").orEmpty()

    fun saveGithubToken(value: String) {
        preferences.edit().putString(KEY_GITHUB_TOKEN, value.trim()).apply()
    }

    fun repoDisplay(): String = repoSlug().ifBlank { "Not configured" }

    fun tokenDisplay(): String = if (githubToken().isBlank()) "Not configured" else "Configured"

    fun checkForUpdate(): UpdateCheckResult {
        val slug = repoSlug()
        if (slug.isBlank() || !slug.contains("/")) {
            return UpdateCheckResult.Error("Configure a GitHub repo like owner/repo first.")
        }

        return runCatching {
            val json = githubGetJson("https://api.github.com/repos/$slug/releases/latest")
            val release = JSONObject(json)
            val assets = release.optJSONArray("assets")
            val asset = (0 until (assets?.length() ?: 0))
                .mapNotNull { index -> assets?.optJSONObject(index) }
                .firstOrNull { candidate ->
                    candidate.optString("name").endsWith(".apk", ignoreCase = true)
                } ?: error("Latest release does not contain an APK asset.")

            val notes = release.optString("body")
            val versionCode = parseVersionCode(release, asset)
            val versionName = parseVersionName(release, asset)
            val downloadUrl = asset.optString("url")
            require(downloadUrl.isNotBlank()) { "APK asset download URL is missing." }

            val info = UpdateInfo(
                versionCode = versionCode,
                versionName = versionName,
                assetName = asset.optString("name"),
                downloadUrl = downloadUrl,
                notes = notes
            )
            if (versionCode > currentVersionCode()) {
                UpdateCheckResult.Available(info)
            } else {
                UpdateCheckResult.UpToDate(info)
            }
        }.getOrElse {
            UpdateCheckResult.Error(it.message ?: "Update check failed.")
        }
    }

    fun downloadApk(update: UpdateInfo): File {
        val targetDir = File(appContext.cacheDir, "updates").apply { mkdirs() }
        val target = File(targetDir, update.assetName.ifBlank { "skinwalker-${update.versionCode}.apk" })
        val connection = openGithubConnection(update.downloadUrl, acceptBinary = true)
        try {
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } finally {
            connection.disconnect()
        }
        return target
    }

    fun installApk(apk: File) {
        val uri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            apk
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        appContext.startActivity(intent)
    }

    private fun githubGetJson(url: String): String {
        val connection = openGithubConnection(url, acceptBinary = false)
        try {
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun openGithubConnection(url: String, acceptBinary: Boolean): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("Accept", if (acceptBinary) APK_ASSET_ACCEPT else JSON_ACCEPT)
        connection.setRequestProperty("X-GitHub-Api-Version", GITHUB_API_VERSION)
        val token = githubToken()
        if (token.isNotBlank()) {
            connection.setRequestProperty("Authorization", "Bearer $token")
        }
        return connection
    }

    private fun parseVersionCode(release: JSONObject, asset: JSONObject): Long {
        val assetName = asset.optString("name")
        val releaseName = release.optString("name")
        val tagName = release.optString("tag_name")
        return extractNumber(assetName)
            ?: extractNumber(releaseName)
            ?: extractNumber(tagName)
            ?: error("Could not determine versionCode from the latest release.")
    }

    private fun parseVersionName(release: JSONObject, asset: JSONObject): String {
        return release.optString("name").ifBlank {
            release.optString("tag_name").ifBlank {
                asset.optString("name").substringBeforeLast(".apk").ifBlank {
                    currentVersionName()
                }
            }
        }
    }

    private fun extractNumber(text: String): Long? {
        val match = VERSION_CODE_REGEX.find(text) ?: return null
        return match.value.toLongOrNull()
    }

    private companion object {
        const val KEY_REPO_SLUG = "github_repo_slug"
        const val KEY_GITHUB_TOKEN = "github_token"
        const val DEFAULT_REPO_SLUG = ""
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        const val JSON_ACCEPT = "application/vnd.github+json"
        const val APK_ASSET_ACCEPT = "application/octet-stream"
        const val GITHUB_API_VERSION = "2022-11-28"
        const val TIMEOUT_MS = 15000
        val VERSION_CODE_REGEX = Regex("""\d+""")
    }
}

data class UpdateInfo(
    val versionCode: Long,
    val versionName: String,
    val assetName: String,
    val downloadUrl: String,
    val notes: String
)

sealed class UpdateCheckResult {
    data class Available(val update: UpdateInfo) : UpdateCheckResult()
    data class UpToDate(val latest: UpdateInfo) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}
