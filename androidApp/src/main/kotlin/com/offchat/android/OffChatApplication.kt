package com.offchat.android

import android.app.Application
import com.offchat.android.di.androidModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class OffChatApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@OffChatApplication)
            allowOverride(true)
            modules(androidModule)
        }
    }
}
