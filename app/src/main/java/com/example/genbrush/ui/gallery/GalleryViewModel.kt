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

enum class SortMode { TIME_DESC, TIME_ASC, FAVORITE_FIRST }

data class GalleryState(
    val images: List<ImageEntry> = emptyList(),
    val isLoading: Boolean = true,
    val pendingDelete: ImageEntry? = null,
    val showFavoritesOnly: Boolean = false,
    val searchQuery: String = "",
    val typeFilter: String? = null, // null = all, "text_to_image", "image_edit"
    val sortMode: SortMode = SortMode.TIME_DESC,
    val selectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val displayedImages: List<ImageEntry> = emptyList(),
    val saveResult: SaveResult? = null // Bug #12: one-shot event for UI toast
)

/** One-shot save result for UI to observe and show toast */
sealed class SaveResult {
    data object Success : SaveResult()
    data class Error(val message: String) : SaveResult()
}

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
            val isFirstLoad = _state.value.images.isEmpty()
            if (isFirstLoad) {
                _state.update { it.copy(isLoading = true) }
            }
            val images = repository.getGalleryImages()
            _state.update { newState ->
                val updated = newState.copy(images = images, isLoading = false)
                updated.copy(displayedImages = computeDisplayedImages(updated))
            }
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
        _state.update { s ->
            val updated = s.copy(showFavoritesOnly = !s.showFavoritesOnly)
            updated.copy(displayedImages = computeDisplayedImages(updated))
        }
    }

    /** 更新搜索关键词 */
    fun updateSearchQuery(query: String) {
        _state.update { s ->
            val updated = s.copy(searchQuery = query)
            updated.copy(displayedImages = computeDisplayedImages(updated))
        }
    }

    /** 更新类型筛选 */
    fun updateTypeFilter(type: String?) {
        _state.update { s ->
            val updated = s.copy(typeFilter = type)
            updated.copy(displayedImages = computeDisplayedImages(updated))
        }
    }

    /** 更新排序模式 */
    fun updateSortMode(mode: SortMode) {
        _state.update { s ->
            val updated = s.copy(sortMode = mode)
            updated.copy(displayedImages = computeDisplayedImages(updated))
        }
    }

    // ==================== Multi-select ====================

    /** 长按或点右上角进入多选模式，id=null 时不预选任何项 */
    fun enterSelectionMode(id: String?) {
        _state.update { it.copy(selectionMode = true, selectedIds = if (id != null) setOf(id) else emptySet()) }
    }

    /** 多选模式下切换选中/取消 */
    fun toggleSelection(id: String) {
        _state.update { s ->
            val newSet = if (id in s.selectedIds) s.selectedIds - id else s.selectedIds + id
            if (newSet.isEmpty()) s.copy(selectionMode = false, selectedIds = emptySet())
            else s.copy(selectedIds = newSet)
        }
    }

    /** 全选当前筛选结果 */
    fun selectAll() {
        val ids = _state.value.displayedImages.map { it.id }.toSet()
        _state.update { it.copy(selectedIds = ids) }
    }

    /** 退出多选模式 */
    fun exitSelectionMode() {
        _state.update { it.copy(selectionMode = false, selectedIds = emptySet()) }
    }

    /** 批量删除（弹确认） */
    fun requestBatchDelete() {
        _state.update { it.copy(pendingDelete = ImageEntry(id = "__batch__", fileName = "", prompt = "", model = "", type = "")) }
    }

    fun isBatchDeletePending(): Boolean {
        return _state.value.pendingDelete?.id == "__batch__"
    }

    /** 确认批量删除 — Bug #11 fix: use batch delete + loading state */
    fun confirmBatchDelete() {
        val ids = _state.value.selectedIds.toList()
        _state.update { it.copy(pendingDelete = null, selectionMode = false, selectedIds = emptySet(), isLoading = true) }
        viewModelScope.launch {
            // Delete files and DB records in batch
            ids.forEach { id ->
                repository.getEntryById(id)?.let { repository.deleteImage(it) }
            }
            loadImages()
        }
    }

    /** 批量收藏 */
    fun batchFavorite() {
        val ids = _state.value.selectedIds.toList()
        _state.update { it.copy(selectionMode = false, selectedIds = emptySet(), isLoading = true) }
        viewModelScope.launch {
            ids.forEach { id -> repository.setFavorite(id, true) }
            loadImages()
        }
    }

    // ==================== Save ====================

    /** Consume the save result event (called by UI after showing toast) */
    fun consumeSaveResult() {
        _state.update { it.copy(saveResult = null) }
    }

    /** Bug #5 fix: correct saved flag logic + OOM protection. Bug #12 fix: emit event instead of Toast */
    fun saveToDeviceGallery(entry: ImageEntry, context: Context) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val file = repository.getLocalImagePath(entry)
                if (!file.exists()) return@withContext SaveResult.Error("File not found")

                try {
                    // Decode with sampling to avoid OOM
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, options)
                    val maxDim = maxOf(options.outWidth, options.outHeight)
                    var sampleSize = 1
                    while (maxDim / (sampleSize * 2) >= 2048) sampleSize *= 2
                    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                        ?: return@withContext SaveResult.Error("Decode failed")

                    val fileName = "GenBrush_${entry.timestamp}.jpg"
                    var saved = false

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
                            saved = true
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.insertImage(
                            context.contentResolver,
                            bitmap,
                            fileName,
                            "Generated by GenBrush: ${entry.prompt}"
                        )
                        saved = true
                    }
                    bitmap.recycle()
                    if (saved) SaveResult.Success else SaveResult.Error("Insert failed")
                } catch (e: OutOfMemoryError) {
                    SaveResult.Error("Out of memory")
                } catch (e: Exception) {
                    SaveResult.Error(e.message ?: "Unknown error")
                }
            }
            _state.update { it.copy(saveResult = result) }
        }
    }

    fun getLocalImagePath(entry: ImageEntry): File = repository.getLocalImagePath(entry)

    // ==================== Display (Bug #7 fix: computed in state) ====================

    /** Compute displayed images based on the given state */
    private fun computeDisplayedImages(s: GalleryState): List<ImageEntry> {
        var list = s.images

        // 收藏筛选
        if (s.showFavoritesOnly) {
            list = list.filter { it.isFavorite }
        }

        // 类型筛选
        if (s.typeFilter != null) {
            list = list.filter { it.type == s.typeFilter }
        }

        // 搜索
        if (s.searchQuery.isNotBlank()) {
            val q = s.searchQuery.trim().lowercase()
            list = list.filter { it.prompt.lowercase().contains(q) }
        }

        // 排序
        list = when (s.sortMode) {
            SortMode.TIME_DESC -> list.sortedByDescending { it.timestamp }
            SortMode.TIME_ASC -> list.sortedBy { it.timestamp }
            SortMode.FAVORITE_FIRST -> list.sortedWith(
                compareByDescending<ImageEntry> { it.isFavorite }.thenByDescending { it.timestamp }
            )
        }

        return list
    }

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
