package com.example.skinwalker

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class CloneRepository(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences("skinwalker_clones", Context.MODE_PRIVATE)

    fun getClones(profileId: Int): List<CloneEntry> {
        val raw = preferences.getString(KEY_CLONES, "[]") ?: "[]"
        val array = JSONArray(raw)
        return (0 until array.length())
            .map { CloneEntry.fromJson(array.getJSONObject(it)) }
            .filter { it.profileId == profileId }
            .let(CloneOrdering::sorted)
    }

    private fun getAllClones(): List<CloneEntry> {
        val raw = preferences.getString(KEY_CLONES, "[]") ?: "[]"
        val array = JSONArray(raw)
        return (0 until array.length()).map { CloneEntry.fromJson(array.getJSONObject(it)) }
    }

    fun upsertClone(clone: CloneEntry) {
        val clones = getAllClones().filterNot {
            it.packageName == clone.packageName && it.profileId == clone.profileId
        } + clone
        save(clones)
    }

    fun updateStatus(packageName: String, profileId: Int, status: CloneStatus, message: String) {
        val clones = getAllClones().map {
            if (it.packageName == packageName && it.profileId == profileId) {
                it.copy(status = status, lastMessage = message)
            } else {
                it
            }
        }
        save(clones)
    }

    fun removeClone(packageName: String, profileId: Int) {
        save(getAllClones().filterNot { it.packageName == packageName && it.profileId == profileId })
    }

    fun markLaunched(packageName: String, profileId: Int) {
        val now = System.currentTimeMillis()
        val clones = getAllClones().map {
            if (it.packageName == packageName && it.profileId == profileId) {
                it.copy(status = CloneStatus.INSTALLED, lastLaunchAt = now, lastMessage = "Launched in this profile.")
            } else {
                it
            }
        }
        save(clones)
    }

    private fun save(clones: List<CloneEntry>) {
        val array = JSONArray()
        clones.forEach { array.put(it.toJson()) }
        preferences.edit().putString(KEY_CLONES, array.toString()).apply()
    }

    private companion object {
        const val KEY_CLONES = "clones"
    }
}

data class CloneEntry(
    val packageName: String,
    val displayName: String?,
    val createdAt: Long,
    val status: CloneStatus,
    val lastMessage: String,
    val lastLaunchAt: Long? = null,
    val profileId: Int = 0
) {
    fun toJson(): JSONObject {
        return JSONObject()
            .put("packageName", packageName)
            .put("displayName", displayName)
            .put("createdAt", createdAt)
            .put("status", status.name)
            .put("lastMessage", lastMessage)
            .put("lastLaunchAt", lastLaunchAt)
            .put("profileId", profileId)
    }

    companion object {
        fun fromJson(json: JSONObject): CloneEntry {
            return CloneEntry(
                packageName = json.getString("packageName"),
                displayName = json.optString("displayName").ifBlank { null },
                createdAt = json.optLong("createdAt"),
                status = CloneStatus.valueOf(json.optString("status", CloneStatus.REQUESTED.name)),
                lastMessage = json.optString("lastMessage"),
                lastLaunchAt = json.optLongOrNull("lastLaunchAt"),
                profileId = json.optInt("profileId", 0)
            )
        }
    }
}

enum class CloneStatus(val message: String) {
    REQUESTED("Clone requested"),
    INSTALLED("Installed in managed profile"),
    ENGINE_MISSING("Virtual engine missing"),
    FAILED("Needs attention")
}

private fun JSONObject.optLongOrNull(name: String): Long? {
    return if (has(name) && !isNull(name)) optLong(name) else null
}
