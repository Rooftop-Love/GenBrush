package com.example.genbrush.data.local

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageSaver {

    suspend fun saveToGallery(context: Context, file: java.io.File, desc: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                if (!file.exists()) return@withContext Result.failure(Exception("File not found"))

                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, options)
                val maxDim = maxOf(options.outWidth, options.outHeight)
                var sampleSize = 1
                while (maxDim / (sampleSize * 2) >= 2048) sampleSize *= 2
                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                    ?: return@withContext Result.failure(Exception("Decode failed"))

                val fileName = "GenBrush_${System.currentTimeMillis()}.jpg"
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
                        desc
                    )
                    saved = true
                }
                bitmap.recycle()
                if (saved) Result.success(Unit)
                else Result.failure(Exception("Insert failed"))
            } catch (e: OutOfMemoryError) {
                Result.failure(Exception("Out of memory"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
