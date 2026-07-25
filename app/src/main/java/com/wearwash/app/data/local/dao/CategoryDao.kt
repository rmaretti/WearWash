package com.wearwash.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.wearwash.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY isPredefined DESC, systemKey, customName")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CategoryEntity?

    @Query(
        """
        SELECT COUNT(*) FROM categories
        WHERE customName IS NOT NULL
          AND customName = :name COLLATE NOCASE
          AND id != :excludingId
        """,
    )
    suspend fun countCustomName(name: String, excludingId: Long): Int

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("SELECT COUNT(*) FROM items WHERE categoryId = :categoryId")
    suspend fun itemCount(categoryId: Long): Int

    @Query("DELETE FROM categories WHERE id = :id AND isPredefined = 0")
    suspend fun deleteCustom(id: Long)
}
