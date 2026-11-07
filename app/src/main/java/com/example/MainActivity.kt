package com.example

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.InterstitialAdDialog
import com.example.ui.components.RewardedAdDialog
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.ArchitectureScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ServerListScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.VpnViewModel
import com.example.vpn.VpnManager

class MainActivity : ComponentActivity() {

    private val viewModel: VpnViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: VpnViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val servers by viewModel.servers.collectAsState()
    val selectedServer by viewModel.selectedServer.collectAsState()
    val vpnStatus by viewModel.vpnStatus.collectAsState()
    val trafficStats by viewModel.trafficStats.collectAsState()
    val remoteConfig by viewModel.remoteConfig.collectAsState()
    val adminUsers by viewModel.adminUsers.collectAsState()
    val adminBroadcast by viewModel.adminBroadcast.collectAsState()
    val protocol by viewModel.protocol.collectAsState()
    val killSwitch by viewModel.killSwitch.collectAsState()
    val customDns by viewModel.customDns.collectAsState()
    val mtu by viewModel.mtu.collectAsState()
    val remainingSeconds by viewModel.remainingSessionSeconds.collectAsState()

    // Android VPN Permission Launcher
    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.startVpnTunnel(context)
        }
    }

    // Android Notification Permission Launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission granted or denied */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Back handling: If on secondary tab, return to HOME
    BackHandler(enabled = uiState.currentScreen != AppScreen.HOME) {
        viewModel.navigateTo(AppScreen.HOME)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                modifier = Modifier
                    .height(68.dp)
                    .testTag("main_navigation_bar")
            ) {
                val items = listOf(
                    Triple(AppScreen.HOME, "Home", Icons.Default.PowerSettingsNew),
                    Triple(AppScreen.SERVERS, "Servers", Icons.Default.Public),
                    Triple(AppScreen.SETTINGS, "Settings", Icons.Default.Settings),
                    Triple(AppScreen.ADMIN, "Admin", Icons.Default.AdminPanelSettings),
                    Triple(AppScreen.ARCHITECTURE, "Blueprint", Icons.Default.AccountTree)
                )

                items.forEach { (screen, label, icon) ->
                    val isSelected = uiState.currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.navigateTo(screen) },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                modifier = Modifier.size(20.dp),
                                tint = if (isSelected) NeonCyan else TextMuted
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NeonCyan else TextMuted
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = DarkSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_${screen.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = uiState.currentScreen,
            modifier = Modifier.padding(innerPadding),
            label = "screen_transition"
        ) { screen ->
            when (screen) {
                AppScreen.HOME -> {
                    HomeScreen(
                        selectedServer = selectedServer,
                        vpnStatus = vpnStatus,
                        trafficStats = trafficStats,
                        remainingSeconds = remainingSeconds,
                        protocol = protocol,
                        remoteConfig = remoteConfig,
                        adminBroadcast = adminBroadcast,
                        rewardNotification = uiState.rewardGrantNotification,
                        onToggleConnection = {
                            viewModel.toggleVpnConnection(context) {
                                val prepareIntent = VpnManager.getPrepareIntent(context)
                                if (prepareIntent != null) {
                                    vpnPrepareLauncher.launch(prepareIntent)
                                }
                            }
                        },
                        onNavigateServers = { viewModel.navigateTo(AppScreen.SERVERS) },
                        onNavigateAdmin = { viewModel.navigateTo(AppScreen.ADMIN) },
                        onOpenRewardedAd = { viewModel.openRewardedAd() },
                        onDismissBroadcast = { viewModel.dismissPushNotification() }
                    )
                }

                AppScreen.SERVERS -> {
                    ServerListScreen(
                        servers = servers,
                        selectedServer = selectedServer,
                        selectedFilter = uiState.selectedFilter,
                        searchQuery = uiState.searchQuery,
                        onFilterChanged = { viewModel.setFilter(it) },
                        onSearchChanged = { viewModel.setSearchQuery(it) },
                        onSelectServer = { server ->
                            viewModel.selectServer(server, context)
                            viewModel.navigateTo(AppScreen.HOME)
                        }
                    )
                }

                AppScreen.SETTINGS -> {
                    SettingsScreen(
                        currentProtocol = protocol,
                        killSwitchEnabled = killSwitch,
                        customDns = customDns,
                        mtu = mtu,
                        remainingSeconds = remainingSeconds,
                        onProtocolChanged = { viewModel.updateProtocol(it) },
                        onKillSwitchToggled = { viewModel.toggleKillSwitch(it) },
                        onCustomDnsChanged = { viewModel.updateCustomDns(it) },
                        onMtuChanged = { viewModel.updateMtu(it) },
                        onOpenRewardedAd = { viewModel.openRewardedAd() }
                    )
                }

                AppScreen.ADMIN -> {
                    AdminPanelScreen(
                        servers = servers,
                        adminUsers = adminUsers,
                        remoteConfig = remoteConfig,
                        successToast = uiState.adminSuccessToast,
                        onAddServer = { viewModel.adminAddServer(it) },
                        onDeleteServer = { viewModel.adminDeleteServer(it) },
                        onToggleUserBan = { viewModel.adminToggleUserBan(it) },
                        onExtendUser = { uid, days -> viewModel.adminExtendUser(uid, days) },
                        onUpdateRemoteConfig = { maintenance, enableAds, updateReq, announcement ->
                            viewModel.adminUpdateRemoteConfig(maintenance, enableAds, updateReq, announcement)
                        },
                        onSendPushBroadcast = { viewModel.adminSendPush(it) }
                    )
                }

                AppScreen.ARCHITECTURE -> {
                    ArchitectureScreen()
                }
            }
        }

        // Rewarded Ad Dialog (+30m VPN free time)
        if (uiState.showRewardedAdDialog) {
            RewardedAdDialog(
                onDismiss = { viewModel.dismissRewardedAd() },
                onRewardEarned = { minutes ->
                    viewModel.grantRewardedAdSessionExtension(minutes)
                }
            )
        }

        // Interstitial Ad Dialog (on disconnect event)
        if (uiState.showInterstitialAdDialog) {
            InterstitialAdDialog(
                onDismiss = { viewModel.dismissInterstitialAd() }
            )
        }
    }
}
