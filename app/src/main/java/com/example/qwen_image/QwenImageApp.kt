package com.example.qwen_image

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import okio.Path.Companion.toPath
import com.example.qwen_image.data.local.ImageStore
import com.example.qwen_image.data.local.PreferencesManager
import com.example.qwen_image.data.remote.DashScopeApi
import com.example.qwen_image.data.repository.GenerationRepository
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class QwenImageApp : Application() {

    lateinit var repository: GenerationRepository
        private set
    lateinit var prefs: PreferencesManager
        private set

    private val _language = MutableStateFlow("")
    val language: StateFlow<String> = _language.asStateFlow()

    fun setLanguage(lang: String) {
        prefs.language = lang
        _language.value = lang
    }

    override fun onCreate() {
        super.onCreate()

        prefs = PreferencesManager(this)

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                }
            }
            .build()

        val gson = GsonBuilder().create()
        val api = DashScopeApi(okHttpClient, gson)
        val imageStore = ImageStore(this)

        repository = GenerationRepository(api, imageStore, prefs)

        _language.value = prefs.language

        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .components {
                    add(OkHttpNetworkFetcherFactory())
                }
                .memoryCachePolicy(CachePolicy.ENABLED)
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizePercent(context, 0.25)
                        .build()
                }
                .diskCachePolicy(CachePolicy.ENABLED)
                .diskCache {
                    DiskCache.Builder()
                        .directory(context.cacheDir.resolve("image_cache").absolutePath.toPath())
                        .maxSizePercent(0.02)
                        .build()
                }
                .build()
        }
    }
}
