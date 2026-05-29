package com.example.qwen_image.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PreferencesManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "qwen_image_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_KEY, value).apply()

    var defaultModel: String
        get() = prefs.getString(KEY_DEFAULT_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(value) = prefs.edit().putString(KEY_DEFAULT_MODEL, value).apply()

    var defaultSize: String
        get() = prefs.getString(KEY_DEFAULT_SIZE, DEFAULT_SIZE) ?: DEFAULT_SIZE
        set(value) = prefs.edit().putString(KEY_DEFAULT_SIZE, value).apply()

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, LANGUAGE_SYSTEM) ?: LANGUAGE_SYSTEM
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_DEFAULT_MODEL = "default_model"
        private const val KEY_DEFAULT_SIZE = "default_size"
        private const val KEY_LANGUAGE = "language"
        const val DEFAULT_MODEL = "qwen-image-2.0-pro"
        const val DEFAULT_SIZE = "1024*1024"
        const val LANGUAGE_SYSTEM = "system"
        const val LANGUAGE_ZH = "zh"
        const val LANGUAGE_EN = "en"
    }
}
