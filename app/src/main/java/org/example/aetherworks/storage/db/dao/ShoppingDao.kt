package org.example.aetherworks.storage.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.example.aetherworks.storage.db.entity.ShoppingItem

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_items ORDER BY isChecked ASC, createdAt DESC")
    fun getAllShoppingItems(): Flow<List<ShoppingItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertItem(item: ShoppingItem)

    @Update
    fun updateItem(item: ShoppingItem)

    @Delete
    fun deleteItem(item: ShoppingItem)

    @Query("UPDATE shopping_items SET isChecked = :checked WHERE id = :id")
    fun updateCheckedStatus(id: String, checked: Boolean)

    @Query("DELETE FROM shopping_items WHERE isChecked = 1")
    fun clearCompletedItems()
}
