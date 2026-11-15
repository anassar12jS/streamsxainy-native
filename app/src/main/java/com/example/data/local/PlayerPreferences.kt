package com.example.data.local

import android.content.Context

class PlayerPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("sx_player_prefs", Context.MODE_PRIVATE)

    fun getPreferredProviderId(): String? =
        prefs.getString(KEY_PROVIDER_ID, null)

    fun setPreferredProviderId(id: String) {
        prefs.edit().putString(KEY_PROVIDER_ID, id).apply()
    }

    companion object {
        private const val KEY_PROVIDER_ID = "preferred_provider_id"
    }
}
