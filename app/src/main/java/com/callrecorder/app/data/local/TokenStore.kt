package com.callrecorder.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("auth_prefs")

class TokenStore(private val context: Context) {

    private val accessKey = stringPreferencesKey("access_token")
    private val refreshKey = stringPreferencesKey("refresh_token")
    private val userIdKey = stringPreferencesKey("user_id")          // Long → String
    private val nicknameKey = stringPreferencesKey("nickname")
    private val activeStoreKey = stringPreferencesKey("active_store_id")  // Long → String

    suspend fun saveTokens(access: String, refresh: String?, userId: String, nickname: String) {
        context.dataStore.edit { prefs ->
            prefs[accessKey] = access
            refresh?.let { prefs[refreshKey] = it }
            prefs[userIdKey] = userId
            prefs[nicknameKey] = nickname
        }
    }

    suspend fun getAccessToken(): String? =
        context.dataStore.data.first()[accessKey]

    val accessTokenFlow: Flow<String?> =
        context.dataStore.data.map { it[accessKey] }

    val nicknameFlow: Flow<String?> =
        context.dataStore.data.map { it[nicknameKey] }

    suspend fun setActiveStore(storeId: String) {
        context.dataStore.edit { it[activeStoreKey] = storeId }
    }

    suspend fun getActiveStore(): String? =
        context.dataStore.data.first()[activeStoreKey]

    val activeStoreFlow: Flow<String?> =
        context.dataStore.data.map { it[activeStoreKey] }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}