package com.callrecorder.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Configuration
import com.callrecorder.app.di.AppContainer
import com.kakao.sdk.common.KakaoSdk

class CallRecorderApp : Application(), Configuration.Provider {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 카카오 SDK 초기화
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)

        // DI 컨테이너 (수동 DI - Hilt 없이 가벼움 유지)
        container = AppContainer(this)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_OBSERVER,
                "통화 감지",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "통화 녹음 파일을 감지합니다" }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_UPLOAD,
                "업로드",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "녹음 파일을 서버에 업로드합니다" }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SUMMARY,
                "요약 완료",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "통화 요약이 준비되었습니다" }
        )
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    companion object {
        lateinit var instance: CallRecorderApp
            private set

        const val CHANNEL_OBSERVER = "channel_observer"
        const val CHANNEL_UPLOAD = "channel_upload"
        const val CHANNEL_SUMMARY = "channel_summary"
    }
}
