package com.example.qwen_image.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.qwen_image.data.local.ImageEntry
import com.example.qwen_image.data.local.ImageStore
import com.example.qwen_image.data.local.PreferencesManager
import com.example.qwen_image.data.remote.DashScopeApi
import com.example.qwen_image.data.remote.model.ContentItem
import com.example.qwen_image.data.remote.model.GenerationRequest
import com.example.qwen_image.data.remote.model.Input
import com.example.qwen_image.data.remote.model.Message
import com.example.qwen_image.data.remote.model.Parameters
import com.example.qwen_image.data.remote.model.TaskInput
import com.example.qwen_image.data.remote.model.TaskParameters
import com.example.qwen_image.data.remote.model.TaskSubmitRequest
import com.example.qwen_image.data.remote.model.isWanModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class GenerationRepository(
    private val api: DashScopeApi,
    private val imageStore: ImageStore,
    private val prefs: PreferencesManager
) {
    suspend fun generateTextToImage(
        prompt: String,
        negativePrompt: String,
        model: String,
        size: String
    ): Result<ImageEntry> {
        val apiKey = prefs.apiKey
        if (apiKey.isBlank()) {
            return Result.failure(Exception("请先在设置中配置 API Key"))
        }

        return if (isWanModel(model)) {
            generateTextToImageAsync(apiKey, prompt, model, size)
        } else {
            generateTextToImageSync(apiKey, prompt, negativePrompt, model, size)
        }
    }

    private suspend fun generateTextToImageSync(
        apiKey: String,
        prompt: String,
        negativePrompt: String,
        model: String,
        size: String
    ): Result<ImageEntry> {
        val content = mutableListOf(ContentItem(text = prompt))
        val request = GenerationRequest(
            model = model,
            input = Input(messages = listOf(Message(role = "user", content = content))),
            parameters = Parameters(
                size = size,
                n = 1,
                negative_prompt = negativePrompt.ifBlank { null },
                prompt_extend = true,
                watermark = false
            )
        )

        val response = api.generateImage(apiKey, request)
        return response.fold(
            onSuccess = { genResponse ->
                try {
                    val imageUrl = genResponse.output?.choices?.firstOrNull()?.message?.content?.firstOrNull()?.image
                        ?: throw Exception("响应中未包含图片")
                    val fileName = ImageStore.createFileName("text_to_image")
                    val entry = ImageEntry(
                        fileName = fileName,
                        prompt = prompt,
                        model = model,
                        type = "text_to_image"
                    )
                    Result.success(imageStore.saveImageFromUrl(imageUrl, entry))
                } catch (e: Exception) {
                    Result.failure(Exception("保存图片失败：${e.message}"))
                }
            },
            onFailure = { Result.failure(it) }
        )
    }

    private suspend fun generateTextToImageAsync(
        apiKey: String,
        prompt: String,
        model: String,
        size: String
    ): Result<ImageEntry> {
        val request = TaskSubmitRequest(
            model = model,
            input = TaskInput(prompt = prompt),
            parameters = TaskParameters(size = size, n = 1)
        )

        val submitResult = api.submitAsyncTask(apiKey, request)
        val taskId = submitResult.fold(
            onSuccess = { it.output?.task_id ?: return Result.failure(Exception("未获取到任务 ID")) },
            onFailure = { return Result.failure(it) }
        )

        val statusResult = api.pollTaskStatus(apiKey, taskId)
        return statusResult.fold(
            onSuccess = { statusResponse ->
                try {
                    val imageUrl = statusResponse.output?.results?.firstOrNull()?.url
                        ?: throw Exception("响应中未包含图片")
                    val fileName = ImageStore.createFileName("text_to_image")
                    val entry = ImageEntry(
                        fileName = fileName,
                        prompt = prompt,
                        model = model,
                        type = "text_to_image"
                    )
                    Result.success(imageStore.saveImageFromUrl(imageUrl, entry))
                } catch (e: Exception) {
                    Result.failure(Exception("保存图片失败：${e.message}"))
                }
            },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun generateImageEdit(
        imageUri: Uri,
        prompt: String,
        model: String,
        context: Context
    ): Result<ImageEntry> {
        val apiKey = prefs.apiKey
        if (apiKey.isBlank()) {
            return Result.failure(Exception("请先在设置中配置 API Key"))
        }

        val base64Image = encodeImageToBase64(imageUri, context)

        val content = listOf(
            ContentItem(image = "data:image/jpeg;base64,$base64Image"),
            ContentItem(text = prompt)
        )

        val request = GenerationRequest(
            model = model,
            input = Input(messages = listOf(Message(role = "user", content = content))),
            parameters = Parameters(
                size = prefs.defaultSize,
                n = 1,
                prompt_extend = true,
                watermark = false
            )
        )

        val response = api.generateImage(apiKey, request)
        return response.fold(
            onSuccess = { genResponse ->
                try {
                    val imageUrl = genResponse.output?.choices?.firstOrNull()?.message?.content?.firstOrNull()?.image
                        ?: throw Exception("响应中未包含图片")
                    val fileName = ImageStore.createFileName("image_edit")
                    val entry = ImageEntry(
                        fileName = fileName,
                        prompt = prompt,
                        model = model,
                        type = "image_edit"
                    )
                    Result.success(imageStore.saveImageFromUrl(imageUrl, entry))
                } catch (e: Exception) {
                    Result.failure(Exception("保存编辑后的图片失败：${e.message}"))
                }
            },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun getGalleryImages(): List<ImageEntry> = imageStore.getAllEntries()

    fun getLocalImagePath(entry: ImageEntry) = imageStore.getImageFile(entry)

    fun getLocalImagePathById(id: String) = imageStore.getImageFileById(id)

    fun getEntryById(id: String) = imageStore.getEntryById(id)

    suspend fun deleteImage(entry: ImageEntry) = imageStore.deleteImage(entry)

    private suspend fun encodeImageToBase64(uri: Uri, context: Context): String =
        withContext(Dispatchers.IO) {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw Exception("无法打开图片")
            val bytes = inputStream.readBytes()
            inputStream.close()

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

            val maxDim = maxOf(options.outWidth, options.outHeight)
            val sampleSize = if (maxDim > 2048) {
                var sample = 1
                while (maxDim / (sample * 2) >= 2048) sample *= 2
                sample
            } else 1

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                ?: throw Exception("解码图片失败")

            val resized = ImageStore.resizeBitmapIfNeeded(bitmap, 2048)

            val outputStream = ByteArrayOutputStream()
            resized.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, outputStream)
            if (resized !== bitmap) resized.recycle()
            bitmap.recycle()

            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        }
}
