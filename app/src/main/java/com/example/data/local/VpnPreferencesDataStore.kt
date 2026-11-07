package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.VpnProtocol
import com.example.data.model.VpnServer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nexus_vpn_prefs")

class VpnPreferencesDataStore(private val context: Context) {

    companion object {
        private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_SERVER_ID = stringPreferencesKey("server_id")
        private val KEY_SERVER_NAME = stringPreferencesKey("server_name")
        private val KEY_SERVER_COUNTRY = stringPreferencesKey("server_country")
        private val KEY_SERVER_CODE = stringPreferencesKey("server_code")
        private val KEY_SERVER_FLAG = stringPreferencesKey("server_flag")
        private val KEY_SERVER_IP = stringPreferencesKey("server_ip")
        private val KEY_SERVER_PORT = intPreferencesKey("server_port")
        private val KEY_SERVER_PUBLIC_KEY = stringPreferencesKey("server_pubkey")
        private val KEY_SERVER_IS_PREMIUM = booleanPreferencesKey("server_is_premium")

        private val KEY_PROTOCOL = stringPreferencesKey("vpn_protocol")
        private val KEY_KILL_SWITCH = booleanPreferencesKey("vpn_kill_switch")
        private val KEY_CUSTOM_DNS = stringPreferencesKey("vpn_custom_dns")
        private val KEY_MTU = intPreferencesKey("vpn_mtu")
        private val KEY_SESSION_TIME_LEFT = longPreferencesKey("session_time_left")
        private val KEY_LAST_REWARD_TIMESTAMP = longPreferencesKey("last_reward_timestamp")

        // Remote Config Local Cache
        private val KEY_RC_MAINTENANCE = booleanPreferencesKey("rc_maintenance")
        private val KEY_RC_ENABLE_ADS = booleanPreferencesKey("rc_enable_ads")
        private val KEY_RC_UPDATE_REQUIRED = booleanPreferencesKey("rc_update_required")
        private val KEY_RC_ANNOUNCEMENT = stringPreferencesKey("rc_announcement")
    }

    val authTokenFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTH_TOKEN]
    }

    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTH_TOKEN] = token
        }
    }

    val selectedServerFlow: Flow<VpnServer?> = context.dataStore.data.map { prefs ->
        val id = prefs[KEY_SERVER_ID] ?: return@map null
        VpnServer(
            id = id,
            name = prefs[KEY_SERVER_NAME] ?: "Fastest Server",
            countryName = prefs[KEY_SERVER_COUNTRY] ?: "United States",
            countryCode = prefs[KEY_SERVER_CODE] ?: "US",
            flagEmoji = prefs[KEY_SERVER_FLAG] ?: "🇺🇸",
            ipAddress = prefs[KEY_SERVER_IP] ?: "149.154.167.91",
            port = prefs[KEY_SERVER_PORT] ?: 51820,
            publicKey = prefs[KEY_SERVER_PUBLIC_KEY] ?: "xT98wKx9lQW/6V4qFkQc5X2+b8K4A5Jk0Z8=",
            isPremium = prefs[KEY_SERVER_IS_PREMIUM] ?: false
        )
    }

    suspend fun saveSelectedServer(server: VpnServer) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SERVER_ID] = server.id
            prefs[KEY_SERVER_NAME] = server.name
            prefs[KEY_SERVER_COUNTRY] = server.countryName
            prefs[KEY_SERVER_CODE] = server.countryCode
            prefs[KEY_SERVER_FLAG] = server.flagEmoji
            prefs[KEY_SERVER_IP] = server.ipAddress
            prefs[KEY_SERVER_PORT] = server.port
            prefs[KEY_SERVER_PUBLIC_KEY] = server.publicKey
            prefs[KEY_SERVER_IS_PREMIUM] = server.isPremium
        }
    }

    val protocolFlow: Flow<VpnProtocol> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_PROTOCOL] ?: VpnProtocol.WIREGUARD.name
        runCatching { VpnProtocol.valueOf(raw) }.getOrDefault(VpnProtocol.WIREGUARD)
    }

    suspend fun setProtocol(protocol: VpnProtocol) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PROTOCOL] = protocol.name
        }
    }

    val killSwitchFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_KILL_SWITCH] ?: false
    }

    suspend fun setKillSwitch(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_KILL_SWITCH] = enabled
        }
    }

    val customDnsFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_DNS] ?: "1.1.1.1, 1.0.0.1"
    }

    suspend fun setCustomDns(dns: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CUSTOM_DNS] = dns
        }
    }

    val mtuFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_MTU] ?: 1420
    }

    suspend fun setMtu(mtu: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MTU] = mtu
        }
    }

    val remainingSessionSecondsFlow: Flow<Long> = context.dataStore.data.map { prefs ->
        // Default initial free allocation: 30 minutes (1800 seconds)
        prefs[KEY_SESSION_TIME_LEFT] ?: 1800L
    }

    suspend fun addSessionSeconds(seconds: Long) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_SESSION_TIME_LEFT] ?: 1800L
            prefs[KEY_SESSION_TIME_LEFT] = current + seconds
            prefs[KEY_LAST_REWARD_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    suspend fun decrementSessionSecond() {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_SESSION_TIME_LEFT] ?: 1800L
            if (current > 0) {
                prefs[KEY_SESSION_TIME_LEFT] = current - 1
            }
        }
    }

    suspend fun setSessionSeconds(seconds: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SESSION_TIME_LEFT] = seconds
        }
    }
}
