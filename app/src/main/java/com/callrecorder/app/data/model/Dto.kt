package com.callrecorder.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ===== Auth =====
@Serializable
data class KakaoLoginRequest(
    @SerialName("access_token") val accessToken: String
)

@Serializable
data class AuthResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val user: User
)

@Serializable
data class User(
    val id: String,
    val nickname: String,
    val email: String? = null,
    @SerialName("profile_image") val profileImage: String? = null
)

// ===== Store =====
@Serializable
data class CreateStoreRequest(
    val name: String,
    val category: String,
    @SerialName("phone_number") val phoneNumber: String? = null,
    val address: String? = null
)

@Serializable
data class Store(
    val id: String,
    val name: String,
    val category: String? = null,                          // 백엔드 응답에 없을 수 있음 → nullable
    @SerialName("phone_number") val phoneNumber: String? = null,
    val address: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class StoreList(val stores: List<Store>)

// ===== Calls =====
@Serializable
data class UploadUrlRequest(
    @SerialName("store_id") val storeId: String,
    @SerialName("file_name") val fileName: String,
    @SerialName("file_size") val fileSize: Long,
    @SerialName("mime_type") val mimeType: String,
    @SerialName("call_started_at") val callStartedAt: String,  // ISO8601
    @SerialName("duration_seconds") val durationSeconds: Int,
    @SerialName("counterpart_number") val counterpartNumber: String? = null,
    @SerialName("caller_category") val callerCategory: String? = null
)

@Serializable
data class UploadUrlResponse(
    @SerialName("call_id") val callId: String,
    @SerialName("upload_url") val uploadUrl: String,
    @SerialName("upload_headers") val uploadHeaders: Map<String, String> = emptyMap()
)

@Serializable
data class ProcessCallRequest(
    @SerialName("language") val language: String = "ko-KR"
)

@Serializable
data class Call(
    val id: String,
    @SerialName("store_id") val storeId: String,
    @SerialName("caller_number") val callerNumber: String? = null,
    @SerialName("caller_category") val callerCategory: String? = null,
    val duration: Int? = null,
    val status: String = "UNKNOWN",
    @SerialName("created_at") val createdAt: String? = null,
    val summary: String? = null,
    val category: String? = null,
    val sentiment: String? = null,
    @SerialName("action_required") val actionRequired: Int? = null,
    @SerialName("is_read") val isRead: Int? = null,
    @SerialName("stt_result") val sttResult: String? = null,
)

@Serializable
data class CallList(val calls: List<Call>)

@Serializable
data class CallDetail(
    val call: Call,
    val transcript: String? = null,
    val summary: Summary? = null
)

@Serializable
data class Summary(
    val id: String,
    @SerialName("call_id") val callId: String,
    val title: String = "",                                           // 안전장치
    val gist: String = "",                                            // 안전장치
    @SerialName("action_items") val actionItems: List<String> = emptyList(),
    @SerialName("key_points") val keyPoints: List<String> = emptyList(),
    val sentiment: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)