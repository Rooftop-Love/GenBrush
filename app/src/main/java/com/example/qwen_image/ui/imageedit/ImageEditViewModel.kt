package com.example.qwen_image.ui.imageedit

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.qwen_image.data.local.PreferencesManager
import com.example.qwen_image.ui.common.mapError
import com.example.qwen_image.data.repository.GenerationRepository
import com.example.qwen_image.ui.components.EDIT_MODELS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ImageEditState(
    val sourceImageUri: Uri? = null,
    val prompt: String = "",
    val selectedModel: String = EDIT_MODELS.first(),
    val isGenerating: Boolean = false,
    val resultImageFile: File? = null,
    val resultPrompt: String = "",
    val resultModel: String = "",
    val error: String? = null
)

class ImageEditViewModel(
    private val repository: GenerationRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _state = MutableStateFlow(ImageEditState())
    val state: StateFlow<ImageEditState> = _state.asStateFlow()

    fun setSourceImage(uri: Uri?) {
        _state.update { it.copy(sourceImageUri = uri) }
    }

    fun updatePrompt(value: String) {
        _state.update { it.copy(prompt = value) }
    }

    fun selectModel(model: String) {
        _state.update { it.copy(selectedModel = model) }
    }

    fun generate(context: Context) {
        val currentState = _state.value
        if (currentState.sourceImageUri == null) {
            _state.update { it.copy(error = "请选择一张图片") }
            return
        }
        if (currentState.prompt.isBlank()) {
            _state.update { it.copy(error = "请输入编辑提示词") }
            return
        }

        _state.update { it.copy(isGenerating = true, error = null, resultImageFile = null) }

        viewModelScope.launch {
            val result = repository.generateImageEdit(
                imageUri = currentState.sourceImageUri,
                prompt = currentState.prompt,
                model = currentState.selectedModel,
                context = context
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
        _state.update { it.copy(resultImageFile = null) }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    companion object {
        fun factory(repository: GenerationRepository, prefs: PreferencesManager) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ImageEditViewModel(repository, prefs) as T
                }
            }
    }
}

