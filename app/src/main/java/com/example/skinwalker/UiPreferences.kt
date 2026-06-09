package com.example.skinwalker

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object UiPreferences {
    private const val PREFS = "skinwalker_ui"
    private const val KEY_NIGHT_MODE = "night_mode"

    fun savedNightMode(context: Context): Int {
        val value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_NO)
        return if (value == AppCompatDelegate.MODE_NIGHT_YES) value else AppCompatDelegate.MODE_NIGHT_NO
    }

    fun saveNightMode(context: Context, mode: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_NIGHT_MODE, mode)
            .apply()
    }
}
