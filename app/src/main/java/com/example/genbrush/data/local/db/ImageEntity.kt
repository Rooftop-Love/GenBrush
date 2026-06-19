package com.example.genbrush.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 图片记录的数据库实体。
 *
 * 字段在 v1 一次预留到位（size / negativePrompt / isFavorite / seed 等），
 * 后续功能提交直接使用，避免 Room Migration。
 * 旧数据（从 metadata.json 迁移过来的）这些新字段为 null / false。
 */
@Entity(
    tableName = "images",
    indices = [Index("timestamp"), Index("isFavorite")]
)
data class ImageEntity(
    @PrimaryKey
    val id: String,
    val fileName: String,
    val prompt: String,
    val model: String,
    val timestamp: Long,
    val type: String, // "text_to_image" or "image_edit"
    val size: String? = null,
    val negativePrompt: String? = null,
    val isFavorite: Boolean = false,
    val seed: Long? = null
)
