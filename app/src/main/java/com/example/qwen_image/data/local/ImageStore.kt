package com.example.qwen_image.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

data class ImageEntry(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val prompt: String,
    val model: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String // "text_to_image" or "image_edit"
)

class ImageStore(private val context: Context) {

    private val gson = Gson()
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val imageDir: File
        get() = File(context.filesDir, "generated_images").also { it.mkdirs() }

    private val metadataFile: File
        get() = File(imageDir, "metadata.json")

    suspend fun saveImage(bitmap: Bitmap, entry: ImageEntry): ImageEntry = withContext(Dispatchers.IO) {
        val file = File(imageDir, entry.fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        appendMetadata(entry)
        entry
    }

    suspend fun saveImageFromUrl(url: String, entry: ImageEntry): ImageEntry = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("下载图片失败: HTTP ${response.code}")
        }
        val body = response.body ?: throw Exception("下载图片失败: 响应体为空")
        val file = File(imageDir, entry.fileName)
        body.byteStream().use { input ->
            FileOutputStream(file).use { out ->
                input.copyTo(out)
            }
        }

        appendMetadata(entry)
        entry
    }

    suspend fun saveImageFromBytes(bytes: ByteArray, entry: ImageEntry): ImageEntry = withContext(Dispatchers.IO) {
        val file = File(imageDir, entry.fileName)
        FileOutputStream(file).use { out ->
            out.write(bytes)
        }
        appendMetadata(entry)
        entry
    }

    suspend fun getAllEntries(): List<ImageEntry> = withContext(Dispatchers.IO) {
        if (!metadataFile.exists()) return@withContext emptyList()
        val json = metadataFile.readText()
        if (json.isBlank()) return@withContext emptyList()
        val type = object : TypeToken<List<ImageEntry>>() {}.type
        val entries: List<ImageEntry> = gson.fromJson(json, type) ?: emptyList()
        entries.sortedByDescending { it.timestamp }
    }

    fun getImageFile(entry: ImageEntry): File = File(imageDir, entry.fileName)

    fun getImageFileById(id: String): File? {
        if (!metadataFile.exists()) return null
        val json = metadataFile.readText()
        if (json.isBlank()) return null
        val type = object : TypeToken<List<ImageEntry>>() {}.type
        val entries: List<ImageEntry> = gson.fromJson(json, type) ?: emptyList()
        val entry = entries.find { it.id == id } ?: return null
        val file = File(imageDir, entry.fileName)
        return if (file.exists()) file else null
    }

    fun getEntryById(id: String): ImageEntry? {
        if (!metadataFile.exists()) return null
        val json = metadataFile.readText()
        if (json.isBlank()) return null
        val type = object : TypeToken<List<ImageEntry>>() {}.type
        val entries: List<ImageEntry> = gson.fromJson(json, type) ?: emptyList()
        return entries.find { it.id == id }
    }

    suspend fun deleteImage(entry: ImageEntry) = withContext(Dispatchers.IO) {
        val file = File(imageDir, entry.fileName)
        if (file.exists()) file.delete()
        removeMetadata(entry.id)
    }

    private fun appendMetadata(entry: ImageEntry) {
        val entries = readMetadata().toMutableList()
        entries.add(entry)
        writeMetadata(entries)
    }

    private fun removeMetadata(id: String) {
        val entries = readMetadata().filter { it.id != id }
        writeMetadata(entries)
    }

    private fun readMetadata(): List<ImageEntry> {
        if (!metadataFile.exists()) return emptyList()
        val json = metadataFile.readText()
        if (json.isBlank()) return emptyList()
        val type = object : TypeToken<List<ImageEntry>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    private fun writeMetadata(entries: List<ImageEntry>) {
        metadataFile.writeText(gson.toJson(entries))
    }

    companion object {
        fun createFileName(type: String): String {
            val ts = System.currentTimeMillis()
            val uuid = UUID.randomUUID().toString().take(8)
            return "${ts}_${uuid}.jpg"
        }

        fun resizeBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int = 2048): Bitmap {
            val w = bitmap.width
            val h = bitmap.height
            if (w <= maxDimension && h <= maxDimension) return bitmap
            val scale = maxDimension.toFloat() / maxOf(w, h)
            val newW = (w * scale).toInt()
            val newH = (h * scale).toInt()
            return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        }
    }
}
