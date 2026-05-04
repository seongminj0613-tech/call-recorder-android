package com.callrecorder.app.data.api

import com.callrecorder.app.BuildConfig
import com.callrecorder.app.data.local.TokenStore
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object ApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true     // ← 추가: null이 들어와도 default값으로 처리
        explicitNulls = false        // ← 추가: null 필드 보내지 않음 (선택적이지만 안전)
    }

    fun create(tokenStore: TokenStore): ApiService {
        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            // S3 업로드 요청에는 Authorization 붙이지 않음
            val isS3 = original.url.host.contains("s3.")
                    || original.url.host.endsWith(".amazonaws.com") && original.url.encodedPath.contains("/")
                    && original.header("X-Amz-Date") != null
            val token = runBlocking { tokenStore.getAccessToken() }
            val req = if (!isS3 && !token.isNullOrBlank()) {
                original.newBuilder().addHeader("Authorization", "Bearer $token").build()
            } else original
            chain.proceed(req)
        }

        // 401 만료 시 토큰 클리어 → 다음 진입 시 로그인 화면으로
        val unauthorizedInterceptor = Interceptor { chain ->
            val resp = chain.proceed(chain.request())
            if (resp.code == 401 && !chain.request().url.host.contains("s3.")) {
                runBlocking { tokenStore.clear() }
            }
            resp
        }

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)  // 큰 파일 업로드 대비
            .addInterceptor(authInterceptor)
            .addInterceptor(unauthorizedInterceptor)
            .addInterceptor(logging)
            .build()

        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(ApiService::class.java)
    }
}