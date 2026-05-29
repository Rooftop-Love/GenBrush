package com.example.qwen_image.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.qwen_image.data.local.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsState(
    val apiKey: String = "",
    val isApiKeyVisible: Boolean = false,
    val defaultModel: String = PreferencesManager.DEFAULT_MODEL,
    val defaultSize: String = PreferencesManager.DEFAULT_SIZE,
    val language: String = PreferencesManager.LANGUAGE_ZH,
    val saveSuccess: Boolean = false
)

class SettingsViewModel(private val prefs: PreferencesManager) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        load()
    }

    fun updateApiKey(value: String) {
        _state.update { it.copy(apiKey = value, saveSuccess = false) }
    }

    fun toggleApiKeyVisibility() {
        _state.update { it.copy(isApiKeyVisible = !it.isApiKeyVisible) }
    }

    fun selectModel(model: String) {
        _state.update { it.copy(defaultModel = model, saveSuccess = false) }
    }

    fun selectSize(size: String) {
        _state.update { it.copy(defaultSize = size, saveSuccess = false) }
    }

    fun selectLanguage(lang: String) {
        _state.update { it.copy(language = lang) }
    }

    fun save() {
        prefs.apiKey = _state.value.apiKey
        prefs.defaultModel = _state.value.defaultModel
        prefs.defaultSize = _state.value.defaultSize
        _state.update { it.copy(saveSuccess = true) }
    }

    private fun load() {
        _state.update {
            it.copy(
                apiKey = prefs.apiKey,
                defaultModel = prefs.defaultModel,
                defaultSize = prefs.defaultSize,
                language = prefs.language
            )
        }
    }

    companion object {
        fun factory(prefs: PreferencesManager) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(prefs) as T
                }
            }
    }
}
