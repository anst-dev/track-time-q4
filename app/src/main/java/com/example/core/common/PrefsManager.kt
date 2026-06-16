package com.example.core.common

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("eisenhower_prefs", Context.MODE_PRIVATE)

    var sheetConnected: Boolean
        get() = prefs.getBoolean("sheet_connected", false)
        set(value) = prefs.edit().putBoolean("sheet_connected", value).apply()

    var sheetId: String
        get() = prefs.getString("sheet_id", "1AbcD2eFgHijKlMnOpQrstUvwxXz") ?: "1AbcD2eFgHijKlMnOpQrstUvwxXz"
        set(value) = prefs.edit().putString("sheet_id", value).apply()

    var syncIntervalMinutes: Int
        get() = prefs.getInt("sync_interval_mins", 15)
        set(value) = prefs.edit().putInt("sync_interval_mins", value).apply()
}
