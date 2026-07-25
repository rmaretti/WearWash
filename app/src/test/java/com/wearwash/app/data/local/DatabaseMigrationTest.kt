package com.wearwash.app.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseMigrationTest {
    private lateinit var context: Context
    private lateinit var helper: SupportSQLiteOpenHelper

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DATABASE_NAME)
        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(DATABASE_NAME)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: SupportSQLiteDatabase) = Unit
                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                )
                .build(),
        )
    }

    @After
    fun tearDown() {
        helper.close()
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun `migration 1 to 2 adds custom text lookup columns`() {
        val database = helper.writableDatabase
        database.execSQL("CREATE TABLE items (id INTEGER PRIMARY KEY NOT NULL)")

        MIGRATION_1_2.migrate(database)

        val columns = mutableSetOf<String>()
        database.query("PRAGMA table_info(items)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) columns += cursor.getString(nameIndex)
        }
        assertTrue(columns.containsAll(listOf("categoryName", "colorName", "fabricName", "seasonName")))
    }

    @Test
    fun `migration 2 to 3 deduplicates basket rows and creates unique index`() {
        val database = helper.writableDatabase
        database.execSQL(
            """
            CREATE TABLE laundry_basket_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                itemId INTEGER NOT NULL,
                addedAt TEXT NOT NULL,
                reason TEXT,
                comment TEXT
            )
            """.trimIndent(),
        )
        database.execSQL(
            "CREATE INDEX index_laundry_basket_entries_itemId " +
                "ON laundry_basket_entries(itemId)",
        )
        database.execSQL(
            "INSERT INTO laundry_basket_entries(itemId, addedAt) VALUES (7, 'first')",
        )
        database.execSQL(
            "INSERT INTO laundry_basket_entries(itemId, addedAt) VALUES (7, 'duplicate')",
        )

        MIGRATION_2_3.migrate(database)

        database.query(
            "SELECT COUNT(*) FROM laundry_basket_entries WHERE itemId = 7",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        val uniqueIndexFound = database.query(
            "PRAGMA index_list(laundry_basket_entries)",
        ).use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val uniqueIndex = cursor.getColumnIndexOrThrow("unique")
            var found = false
            while (cursor.moveToNext()) {
                if (
                    cursor.getString(nameIndex) == "index_laundry_basket_entries_itemId" &&
                    cursor.getInt(uniqueIndex) == 1
                ) {
                    found = true
                }
            }
            found
        }
        assertTrue(uniqueIndexFound)
    }

    @Test
    fun `migration 3 to 4 seeds predefined categories and preserves existing values`() {
        val database = helper.writableDatabase
        database.execSQL(
            """
            CREATE TABLE items (
                id INTEGER PRIMARY KEY NOT NULL,
                categoryId INTEGER,
                categoryName TEXT
            )
            """.trimIndent(),
        )
        database.execSQL(
            "INSERT INTO items(id, categoryName) VALUES (1, 'My special category')",
        )

        MIGRATION_3_4.migrate(database)

        database.query("SELECT COUNT(*) FROM categories WHERE isPredefined = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(12, cursor.getInt(0))
        }
        database.query(
            """
            SELECT categories.customName
            FROM items
            JOIN categories ON categories.id = items.categoryId
            WHERE items.id = 1
            """.trimIndent(),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("My special category", cursor.getString(0))
        }
    }

    private companion object {
        const val DATABASE_NAME = "wear-wash-migration-test.db"
    }
}
