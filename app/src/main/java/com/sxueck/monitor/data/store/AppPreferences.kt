package com.sxueck.monitor.data.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sxueck.monitor.data.model.MonitorConfig
import com.sxueck.monitor.data.model.WidgetSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "nezha_prefs")

class AppPreferences(val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    private val keyBaseUrl = stringPreferencesKey("base_url")
    private val keyToken = stringPreferencesKey("api_token")
    private val keyTagsCsv = stringPreferencesKey("tags_csv")
    private val keyCarouselIndex = intPreferencesKey("carousel_index")
    private val keyOfflineNotifiedJson = stringPreferencesKey("offline_notified_json")
    private val keyLastSuccessAt = longPreferencesKey("last_success_at")
    private val keyWidgetSnapshotJson = stringPreferencesKey("widget_snapshot_json")
    private val keyUsername = stringPreferencesKey("username")
    private val keyPassword = stringPreferencesKey("password")
    private val keyTokenExpireAt = longPreferencesKey("token_expire_at")

    val configFlow: Flow<MonitorConfig> = context.dataStore.data.map { pref ->
        MonitorConfig(
            baseUrl = pref[keyBaseUrl].orEmpty(),
            apiToken = pref[keyToken].orEmpty(),
            tags = pref[keyTagsCsv]
                .orEmpty()
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        )
    }

    val snapshotFlow: Flow<WidgetSnapshot> = context.dataStore.data.map { pref ->
        val raw = pref[keyWidgetSnapshotJson]
        if (raw.isNullOrBlank()) {
            WidgetSnapshot()
        } else {
            runCatching { 
                android.util.Log.d("AppPreferences", "Parsing snapshot, raw length: ${raw.length}")
                val snapshot = json.decodeFromString(WidgetSnapshot.serializer(), raw)
                android.util.Log.d("AppPreferences", "Parsed snapshot with ${snapshot.servers.size} servers")
                snapshot
            }.getOrElse { e -> 
                android.util.Log.e("AppPreferences", "Failed to parse snapshot: ${e.message}")
                WidgetSnapshot(status = "error", message = "Snapshot parse failed: ${e.message}") 
            }
        }
    }

    suspend fun getConfigOnce(): MonitorConfig = configFlow.first()

    suspend fun getSnapshotOnce(): WidgetSnapshot = snapshotFlow.first()

    suspend fun getCarouselIndex(): Int = context.dataStore.data
        .map { it[keyCarouselIndex] ?: 0 }
        .first()

    suspend fun getOfflineNotifiedSet(): Set<String> = context.dataStore.data
        .map { pref ->
            val raw = pref[keyOfflineNotifiedJson].orEmpty()
            if (raw.isBlank()) {
                emptySet()
            } else {
                runCatching {
                    json.decodeFromString(ListSerializer(String.serializer()), raw).toSet()
                }.getOrElse { emptySet() }
            }
        }
        .first()

    suspend fun updateConfig(baseUrl: String, apiToken: String, tags: List<String>) {
        context.dataStore.edit { pref ->
            pref[keyBaseUrl] = baseUrl.trim()
            pref[keyToken] = apiToken.trim()
            pref[keyTagsCsv] = tags.joinToString(",")
        }
    }

    suspend fun saveCredentials(username: String, password: String) {
        context.dataStore.edit { pref ->
            pref[keyUsername] = username
            pref[keyPassword] = password
        }
    }

    suspend fun getCredentials(): Pair<String, String> {
        val prefs = context.dataStore.data.first()
        return Pair(prefs[keyUsername].orEmpty(), prefs[keyPassword].orEmpty())
    }

    suspend fun saveTokenWithExpiry(token: String, expireAt: Long) {
        context.dataStore.edit { pref ->
            pref[keyToken] = token
            pref[keyTokenExpireAt] = expireAt
        }
    }

    suspend fun getTokenExpireAt(): Long {
        return context.dataStore.data.map { it[keyTokenExpireAt] ?: 0L }.first()
    }

    suspend fun clearAuth() {
        context.dataStore.edit { pref ->
            pref[keyToken] = ""
            pref[keyUsername] = ""
            pref[keyPassword] = ""
            pref[keyTokenExpireAt] = 0L
        }
    }

    suspend fun saveSnapshot(snapshot: WidgetSnapshot) {
        android.util.Log.d("AppPreferences", "Saving snapshot with ${snapshot.servers.size} servers")
        context.dataStore.edit { pref ->
            val jsonString = json.encodeToString(snapshot)
            android.util.Log.d("AppPreferences", "Snapshot JSON length: ${jsonString.length}")
            pref[keyWidgetSnapshotJson] = jsonString
        }
    }

    suspend fun saveCarouselIndex(next: Int) {
        context.dataStore.edit { pref ->
            pref[keyCarouselIndex] = next
        }
    }

    suspend fun saveOfflineNotifiedSet(keys: Set<String>) {
        context.dataStore.edit { pref ->
            pref[keyOfflineNotifiedJson] = json.encodeToString(keys.toList().sorted())
        }
    }

    suspend fun saveLastSuccessAt(epochSec: Long) {
        context.dataStore.edit { pref ->
            pref[keyLastSuccessAt] = epochSec
        }
    }

    suspend fun getLastSuccessAt(): Long = context.dataStore.data
        .map { it[keyLastSuccessAt] ?: 0L }
        .first()
}
