package com.wearwash.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wearwash.app.data.local.dao.WashableItemDao
import com.wearwash.app.data.local.entity.FutureEventEntity
import com.wearwash.app.data.local.entity.FutureEventItemEntity
import com.wearwash.app.data.local.entity.LaundryBasketEntryEntity
import com.wearwash.app.data.local.entity.UsageEventEntity
import com.wearwash.app.data.local.entity.WashEventEntity
import com.wearwash.app.data.local.entity.WashableItemEntity

@Database(
    entities = [
        WashableItemEntity::class,
        UsageEventEntity::class,
        WashEventEntity::class,
        LaundryBasketEntryEntity::class,
        FutureEventEntity::class,
        FutureEventItemEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class WearWashDatabase : RoomDatabase() {
    abstract fun washableItemDao(): WashableItemDao

    companion object {
        @Volatile
        private var instance: WearWashDatabase? = null

        fun getDatabase(context: Context): WearWashDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WearWashDatabase::class.java,
                    "wear_wash.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE items ADD COLUMN categoryName TEXT")
        database.execSQL("ALTER TABLE items ADD COLUMN colorName TEXT")
        database.execSQL("ALTER TABLE items ADD COLUMN fabricName TEXT")
        database.execSQL("ALTER TABLE items ADD COLUMN seasonName TEXT")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            DELETE FROM laundry_basket_entries
            WHERE id NOT IN (
                SELECT MIN(id) FROM laundry_basket_entries GROUP BY itemId
            )
            """.trimIndent(),
        )
        database.execSQL("DROP INDEX IF EXISTS index_laundry_basket_entries_itemId")
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_laundry_basket_entries_itemId " +
                "ON laundry_basket_entries(itemId)",
        )
    }
}
