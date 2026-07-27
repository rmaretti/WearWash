package com.wearwash.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wearwash.app.data.local.dao.WashableItemDao
import com.wearwash.app.data.local.dao.CategoryDao
import com.wearwash.app.data.local.dao.FutureEventDao
import com.wearwash.app.data.local.entity.CategoryEntity
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
        CategoryEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class WearWashDatabase : RoomDatabase() {
    abstract fun washableItemDao(): WashableItemDao
    abstract fun categoryDao(): CategoryDao
    abstract fun futureEventDao(): FutureEventDao

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .addCallback(
                        object : RoomDatabase.Callback() {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                MIGRATION_3_4.migrate(db)
                            }
                        },
                    )
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

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                systemKey TEXT,
                customName TEXT,
                isPredefined INTEGER NOT NULL,
                washingCriteriaType TEXT NOT NULL,
                washingUsageThreshold INTEGER,
                washingDayThreshold INTEGER,
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL
            )
            """.trimIndent(),
        )
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_categories_systemKey ON categories(systemKey)",
        )
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_categories_customName ON categories(customName)",
        )
        val now = "2026-07-25T00:00:00Z"
        val predefined = listOf(
            Triple("tops", "ByUsage", "2,NULL"),
            Triple("bottoms", "ByUsage", "3,NULL"),
            Triple("underwear", "ByUsage", "1,NULL"),
            Triple("socks", "ByUsage", "1,NULL"),
            Triple("activewear", "ByUsage", "1,NULL"),
            Triple("sleepwear", "ByUsage", "3,NULL"),
            Triple("dresses", "ByUsage", "2,NULL"),
            Triple("outerwear", "ByUsage", "5,NULL"),
            Triple("bedding", "ByDate", "NULL,7"),
            Triple("towels", "ByUsageOrDate", "3,7"),
            Triple("curtains", "ByDate", "NULL,90"),
            Triple("other", "Manual", "NULL,NULL"),
        )
        predefined.forEach { (key, type, thresholds) ->
            database.execSQL(
                """
                INSERT OR IGNORE INTO categories(
                    systemKey, customName, isPredefined, washingCriteriaType,
                    washingUsageThreshold, washingDayThreshold, createdAt, updatedAt
                ) VALUES ('$key', NULL, 1, '$type', $thresholds, '$now', '$now')
                """.trimIndent(),
            )
        }
        database.execSQL(
            """
            INSERT OR IGNORE INTO categories(
                customName, systemKey, isPredefined, washingCriteriaType,
                washingUsageThreshold, washingDayThreshold, createdAt, updatedAt
            )
            SELECT DISTINCT TRIM(categoryName), NULL, 0, 'ByUsage', 3, NULL, '$now', '$now'
            FROM items
            WHERE categoryName IS NOT NULL AND TRIM(categoryName) != ''
            """.trimIndent(),
        )
        database.execSQL(
            """
            UPDATE items
            SET categoryId = (
                SELECT categories.id FROM categories
                WHERE categories.customName = TRIM(items.categoryName)
                LIMIT 1
            )
            WHERE categoryName IS NOT NULL AND TRIM(categoryName) != ''
            """.trimIndent(),
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE future_event_items_new (
                eventId INTEGER NOT NULL,
                itemId INTEGER NOT NULL,
                addedAt TEXT NOT NULL,
                PRIMARY KEY(eventId, itemId),
                FOREIGN KEY(eventId) REFERENCES future_events(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(itemId) REFERENCES items(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            INSERT OR IGNORE INTO future_event_items_new(eventId, itemId, addedAt)
            SELECT eventId, itemId, MIN(addedAt)
            FROM future_event_items
            GROUP BY eventId, itemId
            """.trimIndent(),
        )
        database.execSQL("DROP TABLE future_event_items")
        database.execSQL("ALTER TABLE future_event_items_new RENAME TO future_event_items")
        database.execSQL(
            "CREATE INDEX index_future_event_items_eventId ON future_event_items(eventId)",
        )
        database.execSQL(
            "CREATE INDEX index_future_event_items_itemId ON future_event_items(itemId)",
        )
    }
}
