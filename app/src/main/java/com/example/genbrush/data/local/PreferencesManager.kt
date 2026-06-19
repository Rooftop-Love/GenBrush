package com.example.genbrush.data.local

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
        "GenBrush_prefs",
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

    var backend: String
        get() = prefs.getString(KEY_BACKEND, BACKEND_DASHSCOPE) ?: BACKEND_DASHSCOPE
        set(value) = prefs.edit().putString(KEY_BACKEND, value).apply()

    var sdServerUrl: String
        get() = prefs.getString(KEY_SD_SERVER_URL, DEFAULT_SD_URL) ?: DEFAULT_SD_URL
        set(value) = prefs.edit().putString(KEY_SD_SERVER_URL, value).apply()

    /** 被禁用的预置生成模型（默认全部启用，只存被禁用的） */
    var disabledGenerationModels: Set<String>
        get() {
            val raw = prefs.getString(KEY_DISABLED_GEN_MODELS, "") ?: ""
            return if (raw.isBlank()) emptySet()
            else raw.split(MODEL_SEP).filter { it.isNotBlank() }.toSet()
        }
        set(value) = prefs.edit().putString(KEY_DISABLED_GEN_MODELS, value.joinToString(MODEL_SEP)).apply()

    /** 被禁用的预置编辑模型 */
    var disabledEditModels: Set<String>
        get() {
            val raw = prefs.getString(KEY_DISABLED_EDIT_MODELS, "") ?: ""
            return if (raw.isBlank()) emptySet()
            else raw.split(MODEL_SEP).filter { it.isNotBlank() }.toSet()
        }
        set(value) = prefs.edit().putString(KEY_DISABLED_EDIT_MODELS, value.joinToString(MODEL_SEP)).apply()

    /** 用户自定义的生成模型列表 */
    var customGenerationModels: List<String>
        get() {
            val raw = prefs.getString(KEY_CUSTOM_GEN_MODELS, "") ?: ""
            return if (raw.isBlank()) emptyList()
            else raw.split(MODEL_SEP).filter { it.isNotBlank() }
        }
        set(value) = prefs.edit().putString(KEY_CUSTOM_GEN_MODELS, value.joinToString(MODEL_SEP)).apply()

    /** 用户自定义的编辑模型列表 */
    var customEditModels: List<String>
        get() {
            val raw = prefs.getString(KEY_CUSTOM_EDIT_MODELS, "") ?: ""
            return if (raw.isBlank()) emptyList()
            else raw.split(MODEL_SEP).filter { it.isNotBlank() }
        }
        set(value) = prefs.edit().putString(KEY_CUSTOM_EDIT_MODELS, value.joinToString(MODEL_SEP)).apply()

    /** 导出所有设置为 Map（用于数据迁移） */
    fun exportAll(): Map<String, String> = mapOf(
        "api_key" to apiKey,
        "default_model" to defaultModel,
        "default_size" to defaultSize,
        "language" to language,
        "backend" to backend,
        "sd_server_url" to sdServerUrl,
        "disabled_gen_models" to (disabledGenerationModels.joinToString(MODEL_SEP)),
        "disabled_edit_models" to (disabledEditModels.joinToString(MODEL_SEP)),
        "custom_gen_models" to (customGenerationModels.joinToString(MODEL_SEP)),
        "custom_edit_models" to (customEditModels.joinToString(MODEL_SEP))
    )

    /** 从导入的 Map 恢复所有设置 */
    fun applyImported(settings: Map<String, String>) {
        settings["api_key"]?.let { if (it.isNotEmpty()) apiKey = it }
        settings["default_model"]?.let { if (it.isNotEmpty()) defaultModel = it }
        settings["default_size"]?.let { if (it.isNotEmpty()) defaultSize = it }
        settings["language"]?.let { if (it.isNotEmpty()) language = it }
        settings["backend"]?.let { if (it.isNotEmpty()) backend = it }
        settings["sd_server_url"]?.let { sdServerUrl = it }
        settings["disabled_gen_models"]?.let {
            disabledGenerationModels = if (it.isBlank()) emptySet()
            else it.split(MODEL_SEP).filter { s -> s.isNotBlank() }.toSet()
        }
        settings["disabled_edit_models"]?.let {
            disabledEditModels = if (it.isBlank()) emptySet()
            else it.split(MODEL_SEP).filter { s -> s.isNotBlank() }.toSet()
        }
        settings["custom_gen_models"]?.let {
            customGenerationModels = if (it.isBlank()) emptyList()
            else it.split(MODEL_SEP).filter { s -> s.isNotBlank() }
        }
        settings["custom_edit_models"]?.let {
            customEditModels = if (it.isBlank()) emptyList()
            else it.split(MODEL_SEP).filter { s -> s.isNotBlank() }
        }
    }

    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_DEFAULT_MODEL = "default_model"
        private const val KEY_DEFAULT_SIZE = "default_size"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_BACKEND = "backend_type"
        private const val KEY_SD_SERVER_URL = "sd_server_url"
        private const val KEY_DISABLED_GEN_MODELS = "disabled_gen_models"
        private const val KEY_DISABLED_EDIT_MODELS = "disabled_edit_models"
        private const val KEY_CUSTOM_GEN_MODELS = "custom_gen_models"
        private const val KEY_CUSTOM_EDIT_MODELS = "custom_edit_models"
        private const val MODEL_SEP = "|||"
        const val DEFAULT_MODEL = "qwen-image-2.0-pro"
        const val DEFAULT_SIZE = "1024*1024"
        const val DEFAULT_SD_URL = ""
        const val LANGUAGE_SYSTEM = "system"
        const val LANGUAGE_ZH = "zh"
        const val LANGUAGE_EN = "en"
        const val BACKEND_DASHSCOPE = "dashscope"
        const val BACKEND_SD_WEBUI = "sd_webui"
    }
}
