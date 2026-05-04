package com.callrecorder.app.service

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.callrecorder.app.CallRecorderApp
import com.callrecorder.app.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * FCM 수신 서비스.
 * - 새 토큰 발급 시 백엔드에 등록 (서버 측 엔드포인트가 추가되면 활성화)
 * - 요약 완료 푸시 수신 시 사용자에게 알림 표시
 */
class FcmService : FirebaseMessagingService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO: 백엔드에 POST /devices/fcm-token 등으로 토큰 등록
        // 현재 백엔드에 해당 엔드포인트가 없으므로 로컬에 보관만 해둠
        scope.launch {
            CallRecorderApp.instance.container.tokenStore // placeholder
            // 추후: api.registerFcmToken(FcmTokenRequest(token))
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // data payload 우선, 없으면 notification payload
        val title = message.data["title"]
            ?: message.notification?.title
            ?: "통화 요약 완료"
        val body = message.data["body"]
            ?: message.notification?.body
            ?: "새로운 통화 요약이 도착했어요"
        val callId = message.data["call_id"]?.toLongOrNull()

        showNotification(title, body, callId)
    }

    private fun showNotification(title: String, body: String, callId: Long?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            callId?.let { putExtra("open_call_id", it) }
        }
        val pi = PendingIntent.getActivity(
            this, callId?.toInt() ?: 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val n = NotificationCompat.Builder(this, CallRecorderApp.CHANNEL_SUMMARY)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching {
            NotificationManagerCompat.from(this)
                .notify((callId ?: System.currentTimeMillis()).toInt(), n)
        }
    }
}
