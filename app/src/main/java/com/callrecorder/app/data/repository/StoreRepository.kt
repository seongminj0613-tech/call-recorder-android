package com.callrecorder.app.data.repository

import com.callrecorder.app.data.api.ApiService
import com.callrecorder.app.data.local.TokenStore
import com.callrecorder.app.data.model.CreateStoreRequest
import com.callrecorder.app.data.model.Store

class StoreRepository(
    private val api: ApiService,
    private val tokenStore: TokenStore,
) {
    suspend fun list(): Result<List<Store>> = runCatching { api.listStores().stores }

    suspend fun create(name: String, category: String, phone: String?, address: String?): Result<Store> =
        runCatching {
            val s = api.createStore(CreateStoreRequest(name, category, phone, address))
            // 첫 가게라면 활성 가게로 설정
            if (tokenStore.getActiveStore() == null) tokenStore.setActiveStore(s.id)
            s
        }

    suspend fun setActive(storeId: String) = tokenStore.setActiveStore(storeId)
    suspend fun activeStoreId(): String? = tokenStore.getActiveStore()
}