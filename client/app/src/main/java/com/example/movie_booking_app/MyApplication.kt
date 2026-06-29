package com.example.movie_booking_app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
<<<<<<< HEAD

@HiltAndroidApp
class MyApplication : Application()
=======
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory

@HiltAndroidApp
class MyApplication : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: coil3.PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory())
            }
            .build()
    }
}
>>>>>>> origin/develop
