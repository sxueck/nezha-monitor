package com.sxueck.monitor.data.network

import android.util.Log
import com.sxueck.monitor.data.model.LoginRequest
import com.sxueck.monitor.data.store.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TokenManager {
    private const val TAG = "TokenManager"
    private const val EXPIRY_BUFFER_SECONDS = 300 // 提前5分钟认为token过期

    suspend fun getValidToken(preferences: AppPreferences): String? {
        val config = preferences.getConfigOnce()
        val currentToken = config.apiToken

        if (currentToken.isBlank()) {
            Log.d(TAG, "No token stored")
            return null
        }

        // Check if token is about to expire
        val expireAt = preferences.getTokenExpireAt()
        val now = System.currentTimeMillis() / 1000

        if (expireAt > 0 && now >= expireAt - EXPIRY_BUFFER_SECONDS) {
            Log.d(TAG, "Token expired or about to expire, attempting re-login")
            return refreshToken(preferences)
        }

        Log.d(TAG, "Token is still valid")
        return currentToken
    }

    private suspend fun refreshToken(preferences: AppPreferences): String? {
        return withContext(Dispatchers.IO) {
            try {
                val (username, password) = preferences.getCredentials()

                if (username.isBlank() || password.isBlank()) {
                    Log.w(TAG, "No stored credentials, cannot refresh token")
                    return@withContext null
                }

                val config = preferences.getConfigOnce()
                if (config.baseUrl.isBlank()) {
                    Log.w(TAG, "No base URL configured")
                    return@withContext null
                }

                Log.d(TAG, "Re-logging in with stored credentials")
                val api = NezhaNetwork.createApi(config.baseUrl, "")
                val response = api.login(LoginRequest(username, password))

                if (response.isSuccessful && response.body()?.success == true) {
                    val loginData = response.body()?.data
                    val newToken = loginData?.token

                    if (!newToken.isNullOrBlank()) {
                        val expireAt = parseExpireTime(loginData.expire)
                        preferences.saveTokenWithExpiry(newToken, expireAt)
                        Log.d(TAG, "Token refreshed successfully, expires at: $expireAt")
                        return@withContext newToken
                    }
                }

                Log.e(TAG, "Failed to refresh token: ${response.errorBody()?.string()}")
                null
            } catch (e: Exception) {
                Log.e(TAG, "Exception during token refresh", e)
                null
            }
        }
    }

    private fun parseExpireTime(expire: String): Long {
        return when {
            expire.isBlank() -> 0L
            expire.toLongOrNull() != null -> {
                val ts = expire.toLong()
                if (ts > 1_000_000_000_000L) ts / 1000 else ts
            }
            else -> {
                try {
                    java.time.Instant.parse(expire).epochSecond
                } catch (_: Exception) {
                    0L
                }
            }
        }
    }
}
