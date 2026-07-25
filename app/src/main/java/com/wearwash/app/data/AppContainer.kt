package com.wearwash.app.data

import android.content.Context
import com.wearwash.app.data.local.WearWashDatabase

interface AppContainer {
    val itemRepository: ItemRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val database = WearWashDatabase.getDatabase(context)

    override val itemRepository: ItemRepository by lazy {
        RoomItemRepository(database.washableItemDao(), database.categoryDao())
    }
}
