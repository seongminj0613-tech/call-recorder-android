package com.callrecorder.app.data.api

import com.callrecorder.app.data.model.*
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // ===== Auth =====
    @POST("auth/kakao")
    suspend fun loginWithKakao(@Body body: KakaoLoginRequest): AuthResponse

    // ===== Stores =====
    @POST("stores")
    suspend fun createStore(@Body body: CreateStoreRequest): Store

    @GET("stores")
    suspend fun listStores(): StoreList

    // ===== Calls =====
    @POST("calls/upload")
    suspend fun requestUploadUrl(@Body body: UploadUrlRequest): UploadUrlResponse

    /**
     * S3 Presigned URL에 PUT 업로드.
     * @Url 로 절대 URL 사용. 헤더는 서버에서 받은 그대로 전달.
     */
    @PUT
    suspend fun uploadToS3(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @Body body: RequestBody
    ): Response<Unit>

    @POST("calls/{id}/process")
    suspend fun processCall(
        @Path("id") callId: String,
        @Body body: ProcessCallRequest = ProcessCallRequest()
    ): Response<Unit>

    @GET("calls")
    suspend fun listCalls(
        @Query("store_id") storeId: String? = null,
        @Query("limit") limit: Int = 50
    ): CallList

    @GET("calls/{id}")
    suspend fun getCall(@Path("id") callId: String): CallDetail

    @GET("summaries/{id}")
    suspend fun getSummary(@Path("id") callId: String): Summary
}