package com.example.data.repository

import com.example.data.local.VpnPreferencesDataStore
import com.example.data.model.AdminUser
import com.example.data.model.RemoteConfigData
import com.example.data.model.TrafficStats
import com.example.data.model.VpnProtocol
import com.example.data.model.VpnServer
import com.example.data.model.VpnStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class VpnRepository(
    private val preferencesDataStore: VpnPreferencesDataStore,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    // Initial high-performance global nodes
    private val initialServers = listOf(
        VpnServer(
            id = "us-east-1",
            name = "US East (New York)",
            countryName = "United States",
            countryCode = "US",
            flagEmoji = "🇺🇸",
            ipAddress = "142.250.190.46",
            port = 51820,
            publicKey = "bm90YXJlYWxwdWJsaWNrZXlmb3J3aXJlZ3VhcmQxMjM0NQ==",
            pingMs = 28,
            loadPercentage = 34,
            isPremium = false,
            isFavorite = true
        ),
        VpnServer(
            id = "us-west-1",
            name = "US West (San Francisco)",
            countryName = "United States",
            countryCode = "US",
            flagEmoji = "🇺🇸",
            ipAddress = "142.250.190.78",
            port = 51820,
            publicKey = "eG05OHdLeDlsUVcvNlY0cUZrUWM1WDIrYjhLNFRKazBaOD0=",
            pingMs = 45,
            loadPercentage = 52,
            isPremium = false
        ),
        VpnServer(
            id = "de-fra-1",
            name = "Europe (Frankfurt)",
            countryName = "Germany",
            countryCode = "DE",
            flagEmoji = "🇩🇪",
            ipAddress = "172.217.16.206",
            port = 51820,
            publicKey = "RkVBVFVSQVVSTVNZTlRPWENLUVlTQ0hFTUE0Mzg5Mjc0OQ==",
            pingMs = 38,
            loadPercentage = 41,
            isPremium = false,
            isFavorite = true
        ),
        VpnServer(
            id = "uk-lon-1",
            name = "UK (London - Fast Peer)",
            countryName = "United Kingdom",
            countryCode = "GB",
            flagEmoji = "🇬🇧",
            ipAddress = "216.58.214.238",
            port = 51820,
            publicKey = "VVBLRVlMSU1JVEVESU5URUdSQVRJT05ERVZFTFBPVEVSOTk=",
            pingMs = 32,
            loadPercentage = 68,
            isPremium = true
        ),
        VpnServer(
            id = "jp-tyo-1",
            name = "Asia East (Tokyo 10Gbps)",
            countryName = "Japan",
            countryCode = "JP",
            flagEmoji = "🇯🇵",
            ipAddress = "172.217.26.14",
            port = 51820,
            publicKey = "SkFQQU5XSVJFR1VBUkRQRUVSMTIyMzM0NDU1NjY3Nzg4OTk=",
            pingMs = 95,
            loadPercentage = 77,
            isPremium = true
        ),
        VpnServer(
            id = "sg-sin-1",
            name = "Southeast Asia (Singapore)",
            countryName = "Singapore",
            countryCode = "SG",
            flagEmoji = "🇸🇬",
            ipAddress = "216.58.200.110",
            port = 51820,
            publicKey = "U0lOR0FQT1JFVlBOTk9ERTg5NDcyMzk0ODczMjkwMTQ4MjM=",
            pingMs = 82,
            loadPercentage = 29,
            isPremium = false
        ),
        VpnServer(
            id = "nl-ams-1",
            name = "Netherlands (Amsterdam P2P)",
            countryName = "Netherlands",
            countryCode = "NL",
            flagEmoji = "🇳🇱",
            ipAddress = "142.250.180.14",
            port = 51820,
            publicKey = "QU1TVEVSREFNTkVUSEVSTEFORFNQMlBXSVIxMjM0NTY3OA==",
            pingMs = 42,
            loadPercentage = 46,
            isPremium = true
        )
    )

    private val _servers = MutableStateFlow<List<VpnServer>>(initialServers)
    val servers: StateFlow<List<VpnServer>> = _servers.asStateFlow()

    private val _selectedServer = MutableStateFlow<VpnServer>(initialServers.first())
    val selectedServer: StateFlow<VpnServer> = _selectedServer.asStateFlow()

    private val _vpnStatus = MutableStateFlow(VpnStatus.DISCONNECTED)
    val vpnStatus: StateFlow<VpnStatus> = _vpnStatus.asStateFlow()

    private val _trafficStats = MutableStateFlow(TrafficStats())
    val trafficStats: StateFlow<TrafficStats> = _trafficStats.asStateFlow()

    private val _remoteConfig = MutableStateFlow(RemoteConfigData())
    val remoteConfig: StateFlow<RemoteConfigData> = _remoteConfig.asStateFlow()

    private val _adminUsers = MutableStateFlow<List<AdminUser>>(
        listOf(
            AdminUser("usr_101", "alex.dev@gmail.com", "VIP", isBanned = false, remainingDays = 94, totalConnectedHours = 412.3f),
            AdminUser("usr_102", "sarah.crypto@proton.me", "Premium", isBanned = false, remainingDays = 28, totalConnectedHours = 89.1f),
            AdminUser("usr_103", "spammer99@tempmail.com", "Free", isBanned = true, remainingDays = 0, totalConnectedHours = 4.2f),
            AdminUser("usr_104", "john.doe@workplace.org", "Free", isBanned = false, remainingDays = 3, totalConnectedHours = 34.5f),
            AdminUser("usr_105", "elena.traveller@icloud.com", "Premium", isBanned = false, remainingDays = 180, totalConnectedHours = 278.0f)
        )
    )
    val adminUsers: StateFlow<List<AdminUser>> = _adminUsers.asStateFlow()

    private val _adminNotificationBroadcast = MutableStateFlow<String?>(null)
    val adminNotificationBroadcast: StateFlow<String?> = _adminNotificationBroadcast.asStateFlow()

    val protocolFlow: Flow<VpnProtocol> = preferencesDataStore.protocolFlow
    val killSwitchFlow: Flow<Boolean> = preferencesDataStore.killSwitchFlow
    val customDnsFlow: Flow<String> = preferencesDataStore.customDnsFlow
    val mtuFlow: Flow<Int> = preferencesDataStore.mtuFlow
    val remainingSecondsFlow: Flow<Long> = preferencesDataStore.remainingSessionSecondsFlow

    init {
        // Restore selected server from DataStore
        scope.launch {
            preferencesDataStore.selectedServerFlow.collect { saved ->
                if (saved != null) {
                    _selectedServer.value = saved
                }
            }
        }

        // Start background telemetry & session ticker
        scope.launch {
            monitorTrafficAndSession()
        }
    }

    suspend fun selectServer(server: VpnServer) {
        _selectedServer.value = server
        preferencesDataStore.saveSelectedServer(server)
    }

    fun setVpnStatus(status: VpnStatus) {
        _vpnStatus.value = status
        if (status == VpnStatus.DISCONNECTED) {
            _trafficStats.value = TrafficStats()
        }
    }

    suspend fun setProtocol(protocol: VpnProtocol) = preferencesDataStore.setProtocol(protocol)
    suspend fun setKillSwitch(enabled: Boolean) = preferencesDataStore.setKillSwitch(enabled)
    suspend fun setCustomDns(dns: String) = preferencesDataStore.setCustomDns(dns)
    suspend fun setMtu(mtu: Int) = preferencesDataStore.setMtu(mtu)
    suspend fun addRewardSessionMinutes(minutes: Int) {
        preferencesDataStore.addSessionSeconds(minutes * 60L)
    }

    // Admin Operations: Server Nodes
    fun addServerNode(server: VpnServer) {
        _servers.update { current -> listOf(server) + current }
    }

    fun updateServerNode(updated: VpnServer) {
        _servers.update { current ->
            current.map { if (it.id == updated.id) updated else it }
        }
    }

    fun deleteServerNode(serverId: String) {
        _servers.update { current -> current.filterNot { it.id == serverId } }
    }

    // Admin Operations: User Accounts
    fun toggleUserBan(uid: String) {
        _adminUsers.update { users ->
            users.map { if (it.uid == uid) it.copy(isBanned = !it.isBanned) else it }
        }
    }

    fun extendUserSubscription(uid: String, extraDays: Int) {
        _adminUsers.update { users ->
            users.map { if (it.uid == uid) it.copy(remainingDays = it.remainingDays + extraDays, isBanned = false) else it }
        }
    }

    // Admin Operations: Remote Config
    fun updateRemoteConfig(
        maintenanceMode: Boolean? = null,
        enableAds: Boolean? = null,
        updateRequired: Boolean? = null,
        announcement: String? = null
    ) {
        _remoteConfig.update { cur ->
            cur.copy(
                maintenanceMode = maintenanceMode ?: cur.maintenanceMode,
                enableAds = enableAds ?: cur.enableAds,
                updateRequired = updateRequired ?: cur.updateRequired,
                announcementMessage = announcement ?: cur.announcementMessage
            )
        }
    }

    fun broadcastAdminPushNotification(message: String) {
        _adminNotificationBroadcast.value = message
    }

    fun clearAdminPushNotification() {
        _adminNotificationBroadcast.value = null
    }

    private suspend fun monitorTrafficAndSession() {
        var duration = 0L
        while (scope.isActive) {
            delay(1000L)
            if (_vpnStatus.value == VpnStatus.CONNECTED) {
                duration++
                // Simulate realistic high-throughput WireGuard traffic
                val upKbps = Random.nextDouble(120.0, 1850.0).toFloat()
                val downKbps = Random.nextDouble(850.0, 12500.0).toFloat()
                val cur = _trafficStats.value
                val newTotalUp = cur.totalUploadBytes + (upKbps * 128).toLong()
                val newTotalDown = cur.totalDownloadBytes + (downKbps * 128).toLong()

                _trafficStats.value = TrafficStats(
                    uploadSpeedKbps = upKbps,
                    downloadSpeedKbps = downKbps,
                    totalUploadBytes = newTotalUp,
                    totalDownloadBytes = newTotalDown,
                    connectedDurationSeconds = duration
                )

                // Decrement session remaining seconds
                preferencesDataStore.decrementSessionSecond()
                val remaining = preferencesDataStore.remainingSessionSecondsFlow.first()
                if (remaining <= 0) {
                    // Session time ran out! Prompt disconnect
                    setVpnStatus(VpnStatus.DISCONNECTED)
                }
            } else {
                duration = 0L
            }
        }
    }
}
