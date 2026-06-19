package com.example.genbrush.ui.gallery

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.genbrush.data.local.ImageEntry
import com.example.genbrush.data.repository.GenerationRepository
import com.example.genbrush.ui.localization.AppStrings
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
    val isLoading: Boolean = true,
    val pendingDelete: ImageEntry? = null,
    val showFavoritesOnly: Boolean = false
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

    /** 请求删除（弹出确认对话框） */
    fun requestDelete(entry: ImageEntry) {
        _state.update { it.copy(pendingDelete = entry) }
    }

    /** 确认删除 */
    fun confirmDelete() {
        val entry = _state.value.pendingDelete ?: return
        _state.update { it.copy(pendingDelete = null) }
        viewModelScope.launch {
            repository.deleteImage(entry)
            loadImages()
        }
    }

    /** 取消删除 */
    fun cancelDelete() {
        _state.update { it.copy(pendingDelete = null) }
    }

    /** 切换收藏状态 */
    fun toggleFavorite(entry: ImageEntry) {
        viewModelScope.launch {
            repository.setFavorite(entry.id, !entry.isFavorite)
            loadImages()
        }
    }

    /** 切换收藏筛选 */
    fun toggleFavoriteFilter() {
        _state.update { it.copy(showFavoritesOnly = !it.showFavoritesOnly) }
    }

    /** 当前展示的图片列表（受筛选影响） */
    fun displayedImages(): List<ImageEntry> {
        val s = _state.value
        return if (s.showFavoritesOnly) s.images.filter { it.isFavorite } else s.images
    }

    fun saveToDeviceGallery(entry: ImageEntry, context: Context) {
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) {
                val file = repository.getLocalImagePath(entry)
                if (!file.exists()) return@withContext false

                val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext false
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
                true
            }
            if (saved) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, AppStrings.ZH.commonSaved, Toast.LENGTH_SHORT).show()
                }
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
