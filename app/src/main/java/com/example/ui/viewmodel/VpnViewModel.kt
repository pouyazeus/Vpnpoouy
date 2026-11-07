package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NexusVpnApplication
import com.example.data.model.AdminUser
import com.example.data.model.RemoteConfigData
import com.example.data.model.TrafficStats
import com.example.data.model.VpnProtocol
import com.example.data.model.VpnServer
import com.example.data.model.VpnStatus
import com.example.vpn.VpnManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppScreen {
    HOME,
    SERVERS,
    SETTINGS,
    ADMIN,
    ARCHITECTURE
}

enum class ServerFilter {
    ALL,
    RECOMMENDED,
    FREE,
    PREMIUM
}

data class VpnUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val selectedFilter: ServerFilter = ServerFilter.ALL,
    val searchQuery: String = "",
    val showRewardedAdDialog: Boolean = false,
    val showInterstitialAdDialog: Boolean = false,
    val showAddServerModal: Boolean = false,
    val rewardGrantNotification: String? = null,
    val adminSuccessToast: String? = null
)

class VpnViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NexusVpnApplication
    private val repository = app.vpnRepository

    val servers: StateFlow<List<VpnServer>> = repository.servers
    val selectedServer: StateFlow<VpnServer> = repository.selectedServer
    val vpnStatus: StateFlow<VpnStatus> = repository.vpnStatus
    val trafficStats: StateFlow<TrafficStats> = repository.trafficStats
    val remoteConfig: StateFlow<RemoteConfigData> = repository.remoteConfig
    val adminUsers: StateFlow<List<AdminUser>> = repository.adminUsers
    val adminBroadcast: StateFlow<String?> = repository.adminNotificationBroadcast

    val protocol: StateFlow<VpnProtocol> = repository.protocolFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VpnProtocol.WIREGUARD)

    val killSwitch: StateFlow<Boolean> = repository.killSwitchFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val customDns: StateFlow<String> = repository.customDnsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "1.1.1.1, 1.0.0.1")

    val mtu: StateFlow<Int> = repository.mtuFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1420)

    val remainingSessionSeconds: StateFlow<Long> = repository.remainingSecondsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1800L)

    private val _uiState = MutableStateFlow(VpnUiState())
    val uiState: StateFlow<VpnUiState> = _uiState.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun setFilter(filter: ServerFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectServer(server: VpnServer, context: Context) {
        viewModelScope.launch {
            val wasConnected = vpnStatus.value == VpnStatus.CONNECTED
            repository.selectServer(server)
            if (wasConnected) {
                // Seamlessly reconnect to the new server
                VpnManager.stopVpn(context)
                delay(300)
                VpnManager.startVpn(context, server)
            }
        }
    }

    fun toggleVpnConnection(context: Context, onRequireVpnPermission: () -> Unit) {
        val currentStatus = vpnStatus.value
        if (currentStatus == VpnStatus.CONNECTED || currentStatus == VpnStatus.CONNECTING) {
            // Disconnecting
            repository.setVpnStatus(VpnStatus.DISCONNECTING)
            VpnManager.stopVpn(context)
            viewModelScope.launch {
                delay(400)
                repository.setVpnStatus(VpnStatus.DISCONNECTED)
                // Check if AdMob interstitial should show on disconnect per remote config
                if (remoteConfig.value.enableAds) {
                    _uiState.update { it.copy(showInterstitialAdDialog = true) }
                }
            }
        } else {
            // Check Android VPN permission first
            val prepareIntent = VpnManager.getPrepareIntent(context)
            if (prepareIntent != null) {
                onRequireVpnPermission()
            } else {
                startVpnTunnel(context)
            }
        }
    }

    fun startVpnTunnel(context: Context) {
        repository.setVpnStatus(VpnStatus.CONNECTING)
        viewModelScope.launch {
            delay(600) // Realistic handshake negotiation
            val server = selectedServer.value
            VpnManager.startVpn(context, server)
        }
    }

    fun openRewardedAd() {
        _uiState.update { it.copy(showRewardedAdDialog = true) }
    }

    fun dismissRewardedAd() {
        _uiState.update { it.copy(showRewardedAdDialog = false) }
    }

    fun grantRewardedAdSessionExtension(minutes: Int = 30) {
        viewModelScope.launch {
            repository.addRewardSessionMinutes(minutes)
            _uiState.update {
                it.copy(
                    showRewardedAdDialog = false,
                    rewardGrantNotification = "+$minutes minutes of WireGuard time added!"
                )
            }
            delay(3500)
            _uiState.update { it.copy(rewardGrantNotification = null) }
        }
    }

    fun dismissInterstitialAd() {
        _uiState.update { it.copy(showInterstitialAdDialog = false) }
    }

    fun updateProtocol(protocol: VpnProtocol) {
        viewModelScope.launch { repository.setProtocol(protocol) }
    }

    fun toggleKillSwitch(enabled: Boolean) {
        viewModelScope.launch { repository.setKillSwitch(enabled) }
    }

    fun updateCustomDns(dns: String) {
        viewModelScope.launch { repository.setCustomDns(dns) }
    }

    fun updateMtu(mtu: Int) {
        viewModelScope.launch { repository.setMtu(mtu) }
    }

    // Admin Panel Actions
    fun adminAddServer(server: VpnServer) {
        repository.addServerNode(server)
        showAdminToast("Node '${server.name}' added to cluster")
    }

    fun adminDeleteServer(serverId: String) {
        repository.deleteServerNode(serverId)
        showAdminToast("Server node removed")
    }

    fun adminToggleUserBan(uid: String) {
        repository.toggleUserBan(uid)
        showAdminToast("User status updated")
    }

    fun adminExtendUser(uid: String, days: Int) {
        repository.extendUserSubscription(uid, days)
        showAdminToast("Extended subscription by $days days")
    }

    fun adminUpdateRemoteConfig(
        maintenance: Boolean? = null,
        enableAds: Boolean? = null,
        updateRequired: Boolean? = null,
        announcement: String? = null
    ) {
        repository.updateRemoteConfig(maintenance, enableAds, updateRequired, announcement)
        showAdminToast("Remote Config propagated")
    }

    fun adminSendPush(message: String) {
        repository.broadcastAdminPushNotification(message)
        showAdminToast("Broadcast notification sent!")
    }

    fun dismissPushNotification() {
        repository.clearAdminPushNotification()
    }

    private fun showAdminToast(msg: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(adminSuccessToast = msg) }
            delay(2500)
            _uiState.update { it.copy(adminSuccessToast = null) }
        }
    }
}
