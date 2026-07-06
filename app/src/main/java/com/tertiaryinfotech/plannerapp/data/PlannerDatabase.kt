package com.tertiaryinfotech.plannerapp.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerDao {
    /** Active (non-archived) items, appointments/dated first, newest undated last. */
    @Query("SELECT * FROM planner_items WHERE NOT isArchived ORDER BY date IS NULL, date ASC, createdAt DESC")
    fun activeItems(): Flow<List<PlannerItem>>

    @Query("SELECT * FROM planner_items WHERE isArchived ORDER BY completedAt DESC")
    fun archivedItems(): Flow<List<PlannerItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PlannerItem)

    @Update
    suspend fun update(item: PlannerItem)

    @Delete
    suspend fun delete(item: PlannerItem)

    @Query("DELETE FROM planner_items WHERE isArchived")
    suspend fun clearArchive()

    @Query("DELETE FROM planner_items WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Database(entities = [PlannerItem::class], version = 1, exportSchema = false)
abstract class PlannerDatabase : RoomDatabase() {
    abstract fun plannerDao(): PlannerDao

    companion object {
        @Volatile private var instance: PlannerDatabase? = null

        fun get(context: Context): PlannerDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PlannerDatabase::class.java,
                    "planner.db"
                ).build().also { instance = it }
            }
    }
}
