package com.example.movie_booking_app

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory

@HiltAndroidApp
class MyApplication : Application(), SingletonImageLoader.Factory {

    companion object {
        private lateinit var instance: MyApplication
        fun getContext(): Context = instance.applicationContext
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun newImageLoader(context: coil3.PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory())
            }
            .build()
    }
}
