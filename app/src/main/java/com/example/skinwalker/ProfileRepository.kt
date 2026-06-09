package com.example.skinwalker

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class ProfileRepository(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences("skinwalker_profiles", Context.MODE_PRIVATE)

    fun getProfiles(): List<SkinwalkerProfile> {
        val raw = preferences.getString(KEY_PROFILES, null)
        if (raw.isNullOrBlank()) {
            val seed = listOf(SkinwalkerProfile(0, "Harshit", "1234567890", System.currentTimeMillis()))
            saveProfiles(seed)
            setActiveProfileId(0)
            return seed
        }
        val array = JSONArray(raw)
        return (0 until array.length())
            .map { SkinwalkerProfile.fromJson(array.getJSONObject(it)) }
            .sortedBy { it.id }
    }

    fun activeProfile(): SkinwalkerProfile {
        val profiles = getProfiles()
        val activeId = preferences.getInt(KEY_ACTIVE_PROFILE, profiles.first().id)
        return profiles.firstOrNull { it.id == activeId } ?: profiles.first()
    }

    fun setActiveProfileId(profileId: Int) {
        preferences.edit().putInt(KEY_ACTIVE_PROFILE, profileId).apply()
    }

    fun upsertProfile(profile: SkinwalkerProfile) {
        val profiles = getProfiles().filterNot { it.id == profile.id } + profile
        saveProfiles(profiles.sortedBy { it.id })
    }

    fun createProfile(name: String, phone: String): SkinwalkerProfile {
        val profiles = getProfiles()
        val nextId = (profiles.maxOfOrNull { it.id } ?: 0) + 1
        val profile = SkinwalkerProfile(nextId, name.ifBlank { "Profile ${nextId + 1}" }, phone, System.currentTimeMillis())
        saveProfiles((profiles + profile).sortedBy { it.id })
        setActiveProfileId(profile.id)
        return profile
    }

    private fun saveProfiles(profiles: List<SkinwalkerProfile>) {
        val array = JSONArray()
        profiles.forEach { array.put(it.toJson()) }
        preferences.edit().putString(KEY_PROFILES, array.toString()).apply()
    }

    private companion object {
        const val KEY_PROFILES = "profiles"
        const val KEY_ACTIVE_PROFILE = "active_profile_id"
    }
}

data class SkinwalkerProfile(
    val id: Int,
    val name: String,
    val phone: String,
    val createdAt: Long
) {
    val displaySlot: String
        get() = "Profile ${id + 1}"

    fun toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("name", name)
            .put("phone", phone)
            .put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(json: JSONObject): SkinwalkerProfile {
            return SkinwalkerProfile(
                id = json.optInt("id", 0),
                name = json.optString("name", "Profile"),
                phone = json.optString("phone"),
                createdAt = json.optLong("createdAt")
            )
        }
    }
}
