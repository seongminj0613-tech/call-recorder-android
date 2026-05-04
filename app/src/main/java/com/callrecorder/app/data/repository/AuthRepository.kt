package com.callrecorder.app.data.repository

import android.content.Context
import com.callrecorder.app.data.api.ApiService
import com.callrecorder.app.data.local.TokenStore
import com.callrecorder.app.data.model.KakaoLoginRequest
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AuthRepository(
    private val api: ApiService,
    private val tokenStore: TokenStore,
) {
    /** 카카오 SDK로 로그인 → 우리 서버에 액세스토큰 전달 → 서버 토큰 저장 */
    suspend fun loginWithKakao(context: Context): Result<Unit> = runCatching {
        val kakaoToken = kakaoLogin(context)
        val resp = api.loginWithKakao(KakaoLoginRequest(kakaoToken.accessToken))
        tokenStore.saveTokens(
            access = resp.accessToken,
            refresh = resp.refreshToken,
            userId = resp.user.id,
            nickname = resp.user.nickname,
        )
    }

    private suspend fun kakaoLogin(context: Context): OAuthToken =
        suspendCancellableCoroutine { cont ->
            val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
                if (error != null) cont.resumeWithException(error)
                else if (token != null) cont.resume(token)
                else cont.resumeWithException(IllegalStateException("Kakao token null"))
            }
            val client = UserApiClient.instance
            if (client.isKakaoTalkLoginAvailable(context)) {
                client.loginWithKakaoTalk(context) { token, error ->
                    if (error != null) {
                        // 카카오톡 로그인 실패 시 카카오계정으로 폴백
                        client.loginWithKakaoAccount(context, callback = callback)
                    } else callback(token, null)
                }
            } else {
                client.loginWithKakaoAccount(context, callback = callback)
            }
        }

    suspend fun logout() {
        runCatching {
            suspendCancellableCoroutine<Unit> { cont ->
                UserApiClient.instance.logout { _ -> cont.resume(Unit) }
            }
        }
        tokenStore.clear()
    }
}
