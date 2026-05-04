package com.callrecorder.app.util

import android.util.Log
import com.callrecorder.app.BuildConfig

/**
 * SafeLog: masks sensitive info (phone numbers, file names) in logs.
 * Debug builds: full logs with masking.
 * Release builds: only warnings and errors are logged.
 */
object SafeLog {

    fun d(tag: String, msg: String) {
        if (BuildConfig.DEBUG) Log.d(tag, mask(msg))
    }

    fun i(tag: String, msg: String) {
        if (BuildConfig.DEBUG) Log.i(tag, mask(msg))
    }

    fun w(tag: String, msg: String, e: Throwable? = null) {
        if (e != null) Log.w(tag, mask(msg), e) else Log.w(tag, mask(msg))
    }

    fun e(tag: String, msg: String, e: Throwable? = null) {
        if (e != null) Log.e(tag, mask(msg), e) else Log.e(tag, mask(msg))
    }

    private fun mask(msg: String): String {
        var result = msg
        val phoneRegex = Regex("(\\d{3})[-\\s]?(\\d{3,4})[-\\s]?(\\d{4})")
        result = phoneRegex.replace(result) { m ->
            m.groupValues[1] + "-****-" + m.groupValues[3]
        }
        val koreanWord = "\uD1B5\uD654\u0020\uB179\uC74C"
        val nameRegex = Regex("(" + koreanWord + "\\s+)([^_/\\\\]+?)(_\\d{6,})")
        result = nameRegex.replace(result) { m ->
            m.groupValues[1] + "***" + m.groupValues[3]
        }
        return result
    }
}