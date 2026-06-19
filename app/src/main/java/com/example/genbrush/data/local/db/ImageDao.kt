package com.example.genbrush.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ImageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ImageEntity>)

    @Update
    suspend fun update(entity: ImageEntity)

    @Delete
    suspend fun delete(entity: ImageEntity)

    @Query("DELETE FROM images WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM images WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("SELECT COUNT(*) FROM images")
    suspend fun count(): Int

    /**
     * 全量图片流（按时间倒序）。收藏/搜索/筛选等由 VM 层基于此 Flow 组合，
     * 避免在 DAO 写一堆分支 query。
     */
    @Query("SELECT * FROM images ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ImageEntity>>

    @Query("SELECT * FROM images ORDER BY timestamp DESC")
    suspend fun getAll(): List<ImageEntity>

    @Query("SELECT * FROM images WHERE id = :id")
    suspend fun getById(id: String): ImageEntity?

    @Query("SELECT * FROM images WHERE id = :id")
    fun observeById(id: String): Flow<ImageEntity?>

    @Query("SELECT * FROM images WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun observeFavorites(): Flow<List<ImageEntity>>

    @Query("UPDATE images SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("UPDATE images SET isFavorite = :favorite WHERE id IN (:ids)")
    suspend fun setFavoriteForIds(ids: List<String>, favorite: Boolean)

    @Query("SELECT * FROM images WHERE prompt LIKE '%' || :keyword || '%' ORDER BY timestamp DESC")
    fun searchByPrompt(keyword: String): Flow<List<ImageEntity>>
}
