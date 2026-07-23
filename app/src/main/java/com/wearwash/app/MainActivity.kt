package com.wearwash.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import com.wearwash.app.ui.screens.items.ItemsScreen
import com.wearwash.app.ui.theme.WearWashTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearWashTheme {
                val app = LocalContext.current.applicationContext as WearWashApplication
                ItemsScreen(itemRepository = app.appContainer.itemRepository)
            }
        }
    }
}
