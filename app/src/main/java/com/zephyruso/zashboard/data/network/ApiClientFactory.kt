package com.zephyruso.zashboard.data.network

import com.zephyruso.zashboard.data.model.Backend
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object ApiClientFactory {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun create(backend: Backend): ClashApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(backend.password))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(backend.toBaseUrl())
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ClashApiService::class.java)
    }

    private fun Backend.toBaseUrl(): HttpUrl {
        val builder = HttpUrl.Builder()
            .scheme(protocol)
            .host(host.trim().removePrefix("[").removeSuffix("]"))
            .port(port.toInt())

        secondaryPath
            .trim()
            .trim('/')
            .split('/')
            .filter { it.isNotBlank() }
            .forEach(builder::addPathSegment)

        builder.addPathSegment("")
        return builder.build()
    }
}

private class AuthInterceptor(
    private val password: String,
) : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $password")
            .build()
        return chain.proceed(request)
    }
}
