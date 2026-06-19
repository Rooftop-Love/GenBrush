package com.example.genbrush.ui.gallery

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.genbrush.data.local.ImageEntry
import com.example.genbrush.data.repository.GenerationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class GalleryState(
    val images: List<ImageEntry> = emptyList(),
    val isLoading: Boolean = true
)

class GalleryViewModel(
    private val repository: GenerationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GalleryState())
    val state: StateFlow<GalleryState> = _state.asStateFlow()

    init {
        loadImages()
    }

    fun loadImages() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val images = repository.getGalleryImages()
            _state.update { it.copy(images = images, isLoading = false) }
        }
    }

    fun deleteImage(entry: ImageEntry) {
        viewModelScope.launch {
            repository.deleteImage(entry)
            loadImages()
        }
    }

    fun saveToDeviceGallery(entry: ImageEntry, context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val file = repository.getLocalImagePath(entry)
                if (!file.exists()) return@withContext

                val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext
                val fileName = "GenBrush_${entry.timestamp}.jpg"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/GenBrush")
                    }
                    val uri = context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )
                    uri?.let {
                        context.contentResolver.openOutputStream(it)?.use { out ->
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.insertImage(
                        context.contentResolver,
                        bitmap,
                        fileName,
                        "由 GenBrush 生成：${entry.prompt}"
                    )
                }
                bitmap.recycle()
            }
        }
    }

    fun getLocalImagePath(entry: ImageEntry): File = repository.getLocalImagePath(entry)

    companion object {
        fun factory(repository: GenerationRepository) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return GalleryViewModel(repository) as T
                }
            }
    }
}
