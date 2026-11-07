package com.example.data.model

enum class VpnStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}

enum class VpnProtocol(val displayName: String, val defaultPort: Int) {
    WIREGUARD("WireGuard (ChaCha20-Poly1305)", 51820),
    OPENVPN_UDP("OpenVPN UDP (AES-256-GCM)", 1194),
    OPENVPN_TCP("OpenVPN TCP (Stealth Mode)", 443)
}

data class VpnServer(
    val id: String,
    val name: String,
    val countryName: String,
    val countryCode: String,
    val flagEmoji: String,
    val ipAddress: String,
    val port: Int = 51820,
    val publicKey: String,
    val endpointDns: String? = null,
    val pingMs: Int = 45,
    val loadPercentage: Int = 35,
    val isPremium: Boolean = false,
    val isFavorite: Boolean = false,
    val isOnline: Boolean = true
)

data class TrafficStats(
    val uploadSpeedKbps: Float = 0f,
    val downloadSpeedKbps: Float = 0f,
    val totalUploadBytes: Long = 0L,
    val totalDownloadBytes: Long = 0L,
    val connectedDurationSeconds: Long = 0L
) {
    val formattedUploadSpeed: String
        get() = formatSpeed(uploadSpeedKbps)

    val formattedDownloadSpeed: String
        get() = formatSpeed(downloadSpeedKbps)

    val formattedTotalUpload: String
        get() = formatBytes(totalUploadBytes)

    val formattedTotalDownload: String
        get() = formatBytes(totalDownloadBytes)

    val formattedDuration: String
        get() {
            val hours = connectedDurationSeconds / 3600
            val minutes = (connectedDurationSeconds % 3600) / 60
            val seconds = connectedDurationSeconds % 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    private fun formatSpeed(kbps: Float): String {
        return if (kbps >= 1024f) {
            String.format("%.1f MB/s", kbps / 1024f)
        } else {
            String.format("%.0f KB/s", kbps)
        }
    }

    private fun formatBytes(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024.0) {
            String.format("%.2f GB", mb / 1024.0)
        } else {
            String.format("%.1f MB", mb)
        }
    }
}

data class RemoteConfigData(
    val maintenanceMode: Boolean = false,
    val enableAds: Boolean = true,
    val updateRequired: Boolean = false,
    val rewardMinutes: Int = 30,
    val announcementMessage: String = "✨ WireGuard high-speed nodes updated. Enjoy ultra-low latency streaming!",
    val minSupportedVersion: String = "1.0.0"
)

data class AdminUser(
    val uid: String,
    val email: String,
    val tier: String, // "Free", "Premium", "VIP"
    val isBanned: Boolean = false,
    val remainingDays: Int = 14,
    val totalConnectedHours: Float = 128.5f,
    val lastActive: String = "Just now"
)
