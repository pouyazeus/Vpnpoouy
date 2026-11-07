# NexusVPN: Production Android VPN Architecture & Administration Plan

## 1. High-Level System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          ANDROID MOBILE CLIENT (Compose)                    │
│                                                                             │
│  ┌─────────────────────────┐               ┌─────────────────────────────┐  │
│  │   Jetpack Compose UI    │◄─────────────►│    VpnViewModel / State     │  │
│  │  Home • Servers • Admin │               │  DataStore • Telemetry Flow │  │
│  └────────────┬────────────┘               └──────────────┬──────────────┘  │
│               │                                           │                 │
│               ▼                                           ▼                 │
│  ┌─────────────────────────┐               ┌─────────────────────────────┐  │
│  │    Google Mobile Ads    │               │     WireGuardVpnService     │  │
│  │  Rewarded (+30m) & Int  │               │   TUN Device (10.200.0.2)   │  │
│  └─────────────────────────┘               └──────────────┬──────────────┘  │
└───────────────────────────────────────────────────────────┼─────────────────┘
                                                            │ Encrypted UDP
                               TLS / HTTPS (REST API)       │ (Port 51820)
                               ┌────────────────────────────┼─────────────────┐
                               │                            │                 │
                               ▼                            ▼                 │
┌─────────────────────────────────────────┐      ┌──────────────────────────┐ │
│        BACKEND & ADMIN API (FastAPI)    │      │    WIREGUARD SERVERS     │ │
│  ┌───────────────────────────────────┐  │      │  High-Throughput Nodes   │ │
│  │ Node Health & IP Cluster Registry │  │      │  ┌────────────────────┐  │ │
│  ├───────────────────────────────────┤  │      │  │ US-East (NYC)      │  │ │
│  │ JWT Authentication & User Tiers   │  │      │  ├────────────────────┤  │ │
│  ├───────────────────────────────────┤  │      │  │ EU-Central (FRA)   │  │ │
│  │ Firebase Remote Config (OTA Flags)│  │      │  ├────────────────────┤  │ │
│  ├───────────────────────────────────┤  │      │  │ Asia-Pacific (TYO) │  │ │
│  │ FCM Targeted Push Notifications   │  │      │  └────────────────────┘  │ │
│  └───────────────────────────────────┘  │      │  ChaCha20-Poly1305 Auth  │ │
└─────────────────────────────────────────┘      │  Kernel/Go TUN Backend   │ │
                                                 └──────────────────────────┘ │
                                                                              │
┌─────────────────────────────────────────────────────────────────────────────┴┐
│             ADMIN WEB DASHBOARD (Next.js / Vue / Fastify)                    │
│  • Node CRUD & Ping Latency Jitter Telemetry Monitoring                      │
│  • User Management: Ban/Unban, Subscriptions, Bandwidth Analytics           │
│  • Remote Config Dynamic Over-The-Air Toggles (Maintenance, Ad Controls)    │
│  • Targeted Push Broadcast Dispatcher (FCM HTTP v1)                         │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Android Project Structure

```
com.example/
├── NexusVpnApplication.kt             # Application init, DataStore, AdMob bootstrap
├── MainActivity.kt                    # Single-activity, edge-to-edge Compose navigation & VPN intent
│
├── data/
│   ├── model/
│   │   └── VpnModels.kt               # VpnServer, VpnStatus, TrafficStats, AdminUser, RemoteConfigData
│   ├── local/
│   │   └── VpnPreferencesDataStore.kt # Jetpack DataStore: token, server, session countdown, kill switch
│   └── repository/
│       └── VpnRepository.kt           # Central reactive repository, telemetry ticker, admin mutations
│
├── vpn/
│   ├── WireGuardVpnService.kt         # VpnService subclass: TUN interface, MTU, DNS, routes, notifications
│   └── VpnManager.kt                  # Lifecycle helper: prepare(context), startVpn, stopVpn
│
├── monetization/
│   └── AdMobController.kt             # Production Google Mobile Ads integration & sandbox simulation
│
└── ui/
    ├── theme/
    │   ├── Color.kt                   # Cybersecurity NeonCyan, EmeraldGreen, DeepBackground
    │   ├── Theme.kt                   # Material 3 VpnDarkColorScheme
    │   └── Type.kt                    # Typography configuration
    ├── components/
    │   ├── VpnComponents.kt           # PulsePowerButton, LiveTelemetryCard, ServerRowItem
    │   └── AdDialogs.kt               # RewardedAdDialog (+30m countdown) & InterstitialAdDialog
    ├── screens/
    │   ├── HomeScreen.kt              # Home screen with glowing connect button & live speedometer
    │   ├── ServerListScreen.kt        # Filterable server list with ping latency & load bars
    │   ├── SettingsScreen.kt          # Protocol selector, Kill switch, custom DNS, MTU slider
    │   ├── AdminPanelScreen.kt        # Live interactive node, user, config, and push alert console
    │   └── ArchitectureScreen.kt      # In-app architecture & deliverables viewer
    └── viewmodel/
        └── VpnViewModel.kt            # MVVM StateFlow coordinator for all screens & dialogs
```

---

## 3. Step-by-Step VpnService & WireGuard Handshake Plan

### Step 1: User Consent & Preparation
1. Call `VpnService.prepare(activity)`. If null, permission is already granted.
2. If non-null, launch the Android system VPN permission dialog via `rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())`.

### Step 2: TUN Interface Allocation
1. In `WireGuardVpnService`, invoke `Builder()`:
   - `setSession("NexusVPN-WireGuard")`
   - `addAddress("10.200.0.2", 32)`: Virtual tunnel client IPv4 address.
   - `addRoute("0.0.0.0", 0)`: Divert all default IPv4 traffic into the TUN.
   - `addDnsServer("1.1.1.1")` and `addDnsServer("8.8.8.8")`: Encrypted DNS leak prevention.
   - `setMtu(1420)`: Allocates 80 bytes for IP (20/40), UDP (8), and WireGuard header (16/32).
   - `setBlocking(false)`
   - `establish()`: Returns a `ParcelFileDescriptor` providing direct file access to incoming/outgoing network packets.

### Step 3: WireGuard 1-RTT Handshake
1. Client generates an ephemeral Curve25519 keypair and creates **Type 0x01 Handshake Initiation**:
   - `sender_index` (4 bytes)
   - `unencrypted_ephemeral` (32 bytes)
   - `encrypted_static` (48 bytes: static public key authenticated with server public key via AEAD ChaCha20-Poly1305)
   - `encrypted_timestamp` (28 bytes: replay protection)
   - `mac1` and `mac2` (32 bytes: cookie-based DoS protection)
2. Server validates MAC and static key, then sends **Type 0x02 Handshake Response**:
   - `sender_index`, `receiver_index`
   - `unencrypted_ephemeral` (32 bytes)
   - `encrypted_nothing` (16 bytes: key confirmation)
   - `mac1`, `mac2`
3. Both sides perform Hash-based Key Derivation (`HKDF`) deriving symmetric sending and receiving transport keys.

### Step 4: Socket Protection & Tunneling Loop
1. The client opens a UDP socket bound to port 51820 and calls `VpnService.protect(udpSocket)`. This ensures encrypted UDP traffic bypasses the TUN interface and goes directly to the physical NIC.
2. Packets read from the TUN device are encapsulated into WireGuard transport packets (Type 0x04) using ChaCha20-Poly1305 with an incrementing 64-bit nonce counter.
3. Incoming UDP packets from the server are decrypted and written back into the TUN device.

---

## 4. Admin Panel & Backend Database Schema

### PostgreSQL (FastAPI Backend)

```sql
-- 1. VPN Server Nodes
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

-- 2. User Accounts & Subscriptions
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

-- 3. Dynamic App Remote Configuration
CREATE TABLE dynamic_app_config (
    config_key VARCHAR(64) PRIMARY KEY,
    config_value JSONB NOT NULL,
    updated_by_admin VARCHAR(128),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

### Firestore Document Structure (Alternative NoSQL)
- `/servers/{serverId}`: `{ name, ip, port, publicKey, pingMs, load, isPremium, isOnline }`
- `/users/{userId}`: `{ email, tier, isBanned, subscriptionExpiresAt, totalConnectedHours }`
- `/config/app_settings`: `{ maintenanceMode, enableAds, minVersion, rewardMinutes, announcement }`

---

## 5. AdMob & Remote Config Production Code Snippets

### AdMob Rewarded Ad Integration (Kotlin)
```kotlin
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

    fun showRewardedAd(activity: Activity, onUserEarnedReward: (Int) -> Unit) {
        rewardedAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    loadRewardedAd() // Preload next ad
                }
            }
            ad.show(activity) { rewardItem ->
                // Grant +30 minutes of VPN tunnel time
                onUserEarnedReward(rewardItem.amount)
            }
        }
    }
}
```

### Firebase Remote Config Live Fetching
```kotlin
fun fetchRemoteConfig(onConfigReady: (RemoteConfigData) -> Unit) {
    val remoteConfig = Firebase.remoteConfig
    val settings = remoteConfigSettings {
        minimumFetchIntervalInSeconds = 3600
    }
    remoteConfig.setConfigSettingsAsync(settings)

    remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val maintenance = remoteConfig.getBoolean("maintenance_mode")
            val enableAds = remoteConfig.getBoolean("enable_ads")
            val rewardMins = remoteConfig.getLong("reward_minutes").toInt()
            val announcement = remoteConfig.getString("announcement_message")

            onConfigReady(
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
```
