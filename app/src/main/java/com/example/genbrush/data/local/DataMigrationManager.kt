package com.example.genbrush.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class DataMigrationManager(private val context: Context) {

    private val gson = Gson()
    private val imageStore = ImageStore(context)

    suspend fun exportData(outputStream: OutputStream): Int = withContext(Dispatchers.IO) {
        val imageDir = File(context.filesDir, "generated_images")
        val entries = imageStore.getAllEntries()

        ZipOutputStream(outputStream.buffered()).use { zip ->
            // 1. 导出设置
            val settingsJson = gson.toJson(PreferencesManager(context).exportAll())
            zip.putNextEntry(ZipEntry("settings.json"))
            zip.write(settingsJson.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 2. 导出图片元数据（保持 metadata.json 格式以兼容旧备份）
            if (entries.isNotEmpty()) {
                zip.putNextEntry(ZipEntry("metadata.json"))
                zip.write(gson.toJson(entries).toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                // 逐个导出图片文件
                entries.forEach { entry ->
                    val img = File(imageDir, entry.fileName)
                    if (img.exists()) {
                        zip.putNextEntry(ZipEntry("images/${img.name}"))
                        img.inputStream().use { input -> input.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
        }

        entries.size
    }

    suspend fun importData(inputStream: InputStream): ImportResult = withContext(Dispatchers.IO) {
        val imageDir = File(context.filesDir, "generated_images").also { it.mkdirs() }
        var imageCount = 0
        var settingsApplied = false

        // 解压 ZIP 到临时目录
        val tempDir = File(context.cacheDir, "import_temp_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        try {
            ZipInputStream(inputStream.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val outFile = File(tempDir, entry.name)
                    if (!entry.isDirectory) {
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { out -> zip.copyTo(out) }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            // 导入图片文件
            val imagesDir = File(tempDir, "images")
            if (imagesDir.exists()) {
                imagesDir.listFiles()?.forEach { img ->
                    val dest = File(imageDir, img.name)
                    if (!dest.exists()) {
                        img.copyTo(dest)
                    }
                }
            }

            // 合并元数据到数据库（跳过已存在的 id）
            val importedMeta = File(tempDir, "metadata.json")
            if (importedMeta.exists()) {
                val importedJson = importedMeta.readText()
                val type = object : TypeToken<List<ImageEntry>>() {}.type
                val importedEntries: List<ImageEntry> = gson.fromJson(importedJson, type) ?: emptyList()
                imageCount = imageStore.importEntries(importedEntries)
            }

            // 导入设置
            val settingsFile = File(tempDir, "settings.json")
            if (settingsFile.exists()) {
                val json = settingsFile.readText()
                val type = object : TypeToken<Map<String, String>>() {}.type
                val settings: Map<String, String> = gson.fromJson(json, type) ?: emptyMap()
                PreferencesManager(context).applyImported(settings)
                settingsApplied = true
            }
        } finally {
            tempDir.deleteRecursively()
        }

        ImportResult(imageCount = imageCount, settingsApplied = settingsApplied)
    }

    data class ImportResult(val imageCount: Int, val settingsApplied: Boolean)
}
