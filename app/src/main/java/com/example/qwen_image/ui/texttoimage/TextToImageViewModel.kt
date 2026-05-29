package com.example.qwen_image.ui.texttoimage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.qwen_image.data.local.PreferencesManager
import com.example.qwen_image.ui.common.mapError
import com.example.qwen_image.data.repository.GenerationRepository
import com.example.qwen_image.ui.components.GENERATION_MODELS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class TextToImageState(
    val prompt: String = "",
    val negativePrompt: String = "",
    val selectedModel: String = PreferencesManager.DEFAULT_MODEL,
    val selectedSize: String = PreferencesManager.DEFAULT_SIZE,
    val isGenerating: Boolean = false,
    val resultImageFile: File? = null,
    val resultImageUrl: String? = null,
    val resultPrompt: String = "",
    val resultModel: String = "",
    val error: String? = null
)

class TextToImageViewModel(
    private val repository: GenerationRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _state = MutableStateFlow(TextToImageState())
    val state: StateFlow<TextToImageState> = _state.asStateFlow()

    init {
        _state.update {
            it.copy(
                selectedModel = prefs.defaultModel,
                selectedSize = prefs.defaultSize
            )
        }
    }

    fun updatePrompt(value: String) {
        _state.update { it.copy(prompt = value) }
    }

    fun updateNegativePrompt(value: String) {
        _state.update { it.copy(negativePrompt = value) }
    }

    fun selectModel(model: String) {
        _state.update { it.copy(selectedModel = model) }
    }

    fun selectSize(size: String) {
        _state.update { it.copy(selectedSize = size) }
    }

    fun generate() {
        val currentState = _state.value
        if (currentState.prompt.isBlank()) {
            _state.update { it.copy(error = "请输入提示词") }
            return
        }

        _state.update {
            it.copy(isGenerating = true, error = null, resultImageFile = null)
        }

        viewModelScope.launch {
            val result = repository.generateTextToImage(
                prompt = currentState.prompt,
                negativePrompt = currentState.negativePrompt,
                model = currentState.selectedModel,
                size = currentState.selectedSize
            )
            result.fold(
                onSuccess = { entry ->
                    _state.update {
                        it.copy(
                            isGenerating = false,
                            resultImageFile = repository.getLocalImagePath(entry),
                            resultPrompt = entry.prompt,
                            resultModel = entry.model
                        )
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            isGenerating = false,
                            error = mapError(e)
                        )
                    }
                }
            )
        }
    }

    fun clearResult() {
        _state.update { it.copy(resultImageFile = null, resultImageUrl = null) }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    companion object {
        fun factory(repository: GenerationRepository, prefs: PreferencesManager) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TextToImageViewModel(repository, prefs) as T
                }
            }
    }
}

