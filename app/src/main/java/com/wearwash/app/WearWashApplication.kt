package com.wearwash.app

import android.app.Application
import com.wearwash.app.data.AppContainer
import com.wearwash.app.data.DefaultAppContainer

class WearWashApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = DefaultAppContainer(this)
    }
}
