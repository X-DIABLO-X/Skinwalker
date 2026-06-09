package com.example.skinwalker

import android.content.Context
import java.io.File
import java.util.Locale

class StorageMeter(private val context: Context) {

    fun snapshot(): StorageSnapshot {
        val dataDir = File(context.applicationInfo.dataDir)
        val blackboxDir = File(dataDir, "blackbox")
        val blackboxBytes = directorySize(blackboxDir)
        val totalBytes = directorySize(dataDir)
        return StorageSnapshot(
            appBytes = (totalBytes - blackboxBytes).coerceAtLeast(0L),
            cloneBytes = blackboxBytes,
            totalBytes = totalBytes
        )
    }

    private fun directorySize(file: File?): Long {
        if (file == null || !file.exists()) return 0L
        if (file.isFile) return file.length()
        return file.listFiles()?.sumOf { directorySize(it) } ?: 0L
    }
}

data class StorageSnapshot(
    val appBytes: Long,
    val cloneBytes: Long,
    val totalBytes: Long
) {
    fun appText(): String = appBytes.toStorageText()
    fun cloneText(): String = cloneBytes.toStorageText()
    fun totalText(): String = totalBytes.toStorageText()
}

private fun Long.toStorageText(): String {
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex++
    }
    return if (unitIndex == 0) {
        "${toLong()} ${units[unitIndex]}"
    } else {
        String.format(Locale.US, "%.1f %s", value, units[unitIndex])
    }
}
