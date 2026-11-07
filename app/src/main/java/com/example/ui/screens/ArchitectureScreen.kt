package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ArchitectureScreen(
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableIntStateOf(0) }
    val sections = listOf(
        "System Diagram",
        "Project Structure",
        "VpnService & WireGuard",
        "Backend & DB Schema",
        "AdMob & Remote Config"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.linearGradient(listOf(NeonCyan, ElectricBlue))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountTree,
                    contentDescription = null,
                    tint = DarkBackground,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "System Architecture Blueprint",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Production Architectural Deliverables & Handshake Specifications",
                    fontSize = 11.sp,
                    color = NeonCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedSection,
            containerColor = DarkBackground,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSection]),
                    color = NeonCyan
                )
            }
        ) {
            sections.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSection == index,
                    onClick = { selectedSection = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedSection == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedSection == index) NeonCyan else TextSecondary
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (selectedSection) {
            0 -> SystemDiagramView()
            1 -> ProjectStructureView()
            2 -> WireGuardHandshakeView()
            3 -> BackendDatabaseSchemaView()
            4 -> AdMobAndRemoteConfigView()
        }
    }
}

@Composable
private fun SystemDiagramView() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        CodeBlockCard(
            title = "HIGH-LEVEL SYSTEM ARCHITECTURE",
            content = """
┌────────────────────────────────────────────────────────────────────────┐
│                        ANDROID MOBILE CLIENT                           │
│  ┌─────────────────────────┐         ┌──────────────────────────────┐  │
│  │   Jetpack Compose UI    │◄───────►│  VpnViewModel & Repository   │  │
│  │  Home • Servers • Admin │         │  DataStore • Telemetry Flow  │  │
│  └────────────┬────────────┘         └──────────────┬───────────────┘  │
│               │                                     │                  │
│               ▼                                     ▼                  │
│  ┌─────────────────────────┐         ┌──────────────────────────────┐  │
│  │    Google Mobile Ads    │         │     WireGuardVpnService      │  │
│  │  Rewarded (+30m) & Int  │         │   TUN Device (10.200.0.2)    │  │
│  └─────────────────────────┘         └──────────────┬───────────────┘  │
└─────────────────────────────────────────────────────┼──────────────────┘
                                                      │ Encrypted UDP
                         TLS / HTTPS (REST API)       │ (Port 51820)
                         ┌────────────────────────────┼──────────────────┐
                         │                            │                  │
                         ▼                            ▼                  │
┌──────────────────────────────────┐        ┌──────────────────────────┐ │
│      BACKEND & ADMIN API         │        │    WIREGUARD SERVERS     │ │
│   FastAPI (Python) / Node.js     │        │    High-Speed Clusters   │ │
│   ┌───────────────────────────┐  │        │  ┌────────────────────┐  │ │
│   │ Node Health & IP Manager  │  │        │  │ US-East (NYC)      │  │ │
│   ├───────────────────────────┤  │        │  ├────────────────────┤  │ │
│   │ JWT Auth & User Tiers     │  │        │  │ EU-Central (FRA)   │  │ │
│   ├───────────────────────────┤  │        │  ├────────────────────┤  │ │
│   │ Firebase Remote Config    │  │        │  │ Asia-Pacific (TYO) │  │ │
│   ├───────────────────────────┤  │        │  └────────────────────┘  │ │
│   │ FCM Push Notifications    │  │        │  ChaCha20-Poly1305 Auth  │ │
│   └───────────────────────────┘  │        │  Zero-Log Kernel Tunnel  │ │
└──────────────────────────────────┘        └──────────────────────────┘ │
                         ▲                                               │
                         │                                               │
┌────────────────────────┴───────────────────────────────────────────────┴┐
│             ADMIN WEB DASHBOARD (Next.js / Vue / Fastify)               │
│  • Node CRUD & Ping Latency Jitter Monitoring                           │
│  • User Management: Ban/Unban, Subscriptions, Bandwidth Analytics      │
│  • Remote Config Dynamic Over-The-Air Toggles (Maintenance / Ads)       │
└─────────────────────────────────────────────────────────────────────────┘
            """.trimIndent()
        )
    }
}

@Composable
private fun ProjectStructureView() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        CodeBlockCard(
            title = "ANDROID PROJECT PACKAGE STRUCTURE",
            content = """
com.example/
├── NexusVpnApplication.kt          // App startup, DataStore & AdMob initialization
├── MainActivity.kt                 // Main activity, Navigation & VPN Permission flow
│
├── data/
│   ├── model/
│   │   └── VpnModels.kt            // VpnServer, VpnStatus, TrafficStats, AdminUser, RemoteConfigData
│   ├── local/
│   │   └── VpnPreferencesDataStore.kt // Jetpack DataStore: token, server, protocol, session timer
│   └── repository/
│       └── VpnRepository.kt        // Reactive flows for servers, ping latency, telemetry, admin CRUD
│
├── vpn/
│   ├── WireGuardVpnService.kt      // Extends VpnService, creates TUN interface, notification lifecycle
│   └── VpnManager.kt               // Singleton helper for VpnService.prepare and service intents
│
├── monetization/
│   └── AdMobController.kt          // Rewarded ad (+30 min) and Interstitial ad handlers
│
└── ui/
    ├── theme/
    │   ├── Color.kt                // Cybersecurity NeonCyan, EmeraldGreen, DeepBackground
    │   ├── Theme.kt                // VpnDarkColorScheme
    │   └── Type.kt                 // Modern typography
    ├── components/
    │   ├── VpnComponents.kt        // PulsePowerButton, LiveTelemetryCard, ServerRowItem
    │   └── AdDialogs.kt            // RewardedAdDialog (5s countdown) & InterstitialAdDialog
    ├── screens/
    │   ├── HomeScreen.kt           // Live speedometer, pulsating button, session countdown
    │   ├── ServerListScreen.kt     // Filterable global endpoints with ping/load
    │   ├── SettingsScreen.kt       // Protocol switch, kill switch, MTU slider, DNS
    │   ├── AdminPanelScreen.kt     // Interactive live node, user, remote config, and push console
    │   └── ArchitectureScreen.kt   // Interactive architectural deliverables viewer
    └── viewmodel/
        └── VpnViewModel.kt         // UI state, connection lifecycle, and admin events
            """.trimIndent()
        )
    }
}

@Composable
private fun WireGuardHandshakeView() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        CodeBlockCard(
            title = "STEP-BY-STEP VPNSERVICE & WIREGUARD HANDSHAKE",
            content = """
1. PERMISSION PREPARATION
   • Client checks `VpnService.prepare(activity)`. If null is returned, permission is already granted.
   • If an Intent is returned, prompt user with Android OS standard VPN consent dialog.

2. TUN INTERFACE INITIALIZATION
   • In `WireGuardVpnService`, instantiate `VpnService.Builder()`:
     - `setSession("NexusVPN-WireGuard")`
     - `addAddress("10.200.0.2", 32)`: Assign virtual client IP.
     - `addRoute("0.0.0.0", 0)`: Route all IPv4 traffic through TUN.
     - `addDnsServer("1.1.1.1")`: Prevent DNS leaks.
     - `setMtu(1420)`: Allocate 80-byte header room for WireGuard encapsulation.
     - `vpnInterface = builder.establish()`: Generates raw file descriptor (`ParcelFileDescriptor`).

3. WIREGUARD 1-RTT CRYPTOGRAPHIC HANDSHAKE
   • Client generates ephemeral Curve25519 keypair.
   • Packet 1: Type 0x01 (Initiation) sent to Server UDP port 51820:
     - Sender Index (4 bytes)
     - Ephemeral Public Key (32 bytes)
     - Encrypted Static Public Key (48 bytes, AEAD ChaCha20-Poly1305)
     - Encrypted Timestamp (28 bytes)
     - MAC1 & MAC2 (Anti-DoS cookies, 32 bytes)
   • Packet 2: Type 0x02 (Response) returned by Server within ~30ms:
     - Sender Index & Receiver Index
     - Ephemeral Public Key (32 bytes)
     - Encrypted Empty Buffer with Key Confirmation
     - MAC1 & MAC2
   • Derives symmetric transport keys via HKDF (Hash-based Key Derivation Function).

4. PACKET ENCAPSULATION & ROUTING LOOP
   • Protect Socket: Call `VpnService.protect(udpSocket)` to prevent routing loops.
   • Read raw IP packets from TUN `FileInputStream`.
   • Encapsulate into WireGuard Transport Data (Type 0x04) with 64-bit counter nonce.
   • Send via protected UDP socket to remote server endpoint.
   • Reverse: Decrypt incoming UDP packets and write raw IP packets back to TUN `FileOutputStream`.
            """.trimIndent()
        )
    }
}

@Composable
private fun BackendDatabaseSchemaView() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        CodeBlockCard(
            title = "FASTAPI / POSTGRESQL & FIRESTORE DATABASE SCHEMA",
            content = """
-- ==============================================================
-- 1. TABLE: vpn_servers (Node Topology & Health)
-- ==============================================================
CREATE TABLE vpn_servers (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    country_name VARCHAR(64) NOT NULL,
    country_code VARCHAR(4) NOT NULL,
    flag_emoji VARCHAR(8) NOT NULL,
    ip_address INET NOT NULL,
    port INTEGER NOT NULL DEFAULT 51820,
    public_key VARCHAR(64) NOT NULL,
    is_premium BOOLEAN DEFAULT FALSE,
    is_online BOOLEAN DEFAULT TRUE,
    current_load_pct INTEGER DEFAULT 0,
    last_ping_ms INTEGER DEFAULT 40,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ==============================================================
-- 2. TABLE: app_users (Accounts, Tier, Subscription)
-- ==============================================================
CREATE TABLE app_users (
    uid VARCHAR(128) PRIMARY KEY, -- Firebase Auth UID
    email VARCHAR(255) UNIQUE NOT NULL,
    tier VARCHAR(16) DEFAULT 'FREE', -- 'FREE', 'PREMIUM', 'VIP'
    is_banned BOOLEAN DEFAULT FALSE,
    subscription_expires_at TIMESTAMP WITH TIME ZONE,
    total_connected_hours NUMERIC(10,2) DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_login TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ==============================================================
-- 3. TABLE: dynamic_app_config (Remote Config Over-The-Air)
-- ==============================================================
CREATE TABLE dynamic_app_config (
    config_key VARCHAR(64) PRIMARY KEY,
    config_value JSONB NOT NULL,
    updated_by_admin VARCHAR(128),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Example row in dynamic_app_config:
-- {
--   "maintenance_mode": false,
--   "enable_ads": true,
--   "min_required_version": "1.0.0",
--   "reward_minutes": 30,
--   "announcement_message": "WireGuard European nodes upgraded to 10Gbps!"
-- }
            """.trimIndent()
        )
    }
}

@Composable
private fun AdMobAndRemoteConfigView() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        CodeBlockCard(
            title = "PRODUCTION ADMOB & FIREBASE REMOTE CONFIG SNIPPETS",
            content = """
// ==============================================================
// 1. Google AdMob Rewarded Ad Integration (Kotlin)
// ==============================================================
class AdMobManager(private val context: Context) {
    private var rewardedAd: RewardedAd? = null

    fun loadRewardedAd(adUnitId: String = "ca-app-pub-3940256099942544/5224354917") {
        val request = AdRequest.Builder().build()
        RewardedAd.load(context, adUnitId, request, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                rewardedAd = null
            }
        })
    }

    fun showRewardedAd(activity: Activity, onEarnedReward: (Int) -> Unit) {
        rewardedAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    loadRewardedAd() // Pre-load next
                }
            }
            ad.show(activity) { rewardItem ->
                // User earned +30 minutes of VPN tunnel time
                onEarnedReward(30)
            }
        }
    }
}

// ==============================================================
// 2. Firebase Remote Config Live Fetching
// ==============================================================
fun fetchRemoteConfig(onConfigUpdated: (RemoteConfigData) -> Unit) {
    val remoteConfig = Firebase.remoteConfig
    val configSettings = remoteConfigSettings {
        minimumFetchIntervalInSeconds = 3600 // 1 hr in prod, 0 in debug
    }
    remoteConfig.setConfigSettingsAsync(configSettings)
    remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

    remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val maintenance = remoteConfig.getBoolean("maintenance_mode")
            val enableAds = remoteConfig.getBoolean("enable_ads")
            val rewardMins = remoteConfig.getLong("reward_minutes").toInt()
            val announcement = remoteConfig.getString("announcement_message")

            onConfigUpdated(
                RemoteConfigData(
                    maintenanceMode = maintenance,
                    enableAds = enableAds,
                    rewardMinutes = rewardMins,
                    announcementMessage = announcement
                )
            )
        }
    }
}
            """.trimIndent()
        )
    }
}

@Composable
private fun CodeBlockCard(
    title: String,
    content: String
) {
    val horizontalScrollState = rememberScrollState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("code_block_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(DarkOutline, DarkSurfaceHighlight)))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF070B14))
                    .padding(12.dp)
                    .horizontalScroll(horizontalScrollState)
            ) {
                Text(
                    text = content,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFFC7D2FE),
                    lineHeight = 16.sp
                )
            }
        }
    }
}
