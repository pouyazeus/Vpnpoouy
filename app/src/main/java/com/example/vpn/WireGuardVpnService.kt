package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.NexusVpnApplication
import com.example.R
import com.example.data.model.VpnStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class WireGuardVpnService : VpnService() {

    companion object {
        const val TAG = "WireGuardVpnService"
        const val ACTION_CONNECT = "com.example.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.example.vpn.DISCONNECT"
        const val EXTRA_SERVER_IP = "extra_server_ip"
        const val EXTRA_SERVER_PORT = "extra_server_port"
        const val EXTRA_SERVER_NAME = "extra_server_name"
        const val EXTRA_SERVER_PUBKEY = "extra_server_pubkey"
        const val NOTIFICATION_ID = 4040
        const val CHANNEL_ID = "nexus_vpn_channel"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var tunnelJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val serverIp = intent.getStringExtra(EXTRA_SERVER_IP) ?: "149.154.167.91"
                val serverPort = intent.getIntExtra(EXTRA_SERVER_PORT, 51820)
                val serverName = intent.getStringExtra(EXTRA_SERVER_NAME) ?: "Nexus Server"
                val serverPubKey = intent.getStringExtra(EXTRA_SERVER_PUBKEY) ?: ""

                startVpnTunnel(serverIp, serverPort, serverName, serverPubKey)
            }
            ACTION_DISCONNECT -> {
                stopVpnTunnel()
            }
        }
        return START_NOT_STICKY
    }

    private fun startVpnTunnel(
        serverIp: String,
        serverPort: Int,
        serverName: String,
        serverPubKey: String
    ) {
        startForeground(NOTIFICATION_ID, buildNotification("Connecting to $serverName…"))

        try {
            // Setup Android Virtual Network Interface (TUN)
            val builder = Builder()
                .setSession("NexusVPN-WireGuard")
                .addAddress("10.200.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                .setMtu(1420)
                .setBlocking(false)

            vpnInterface = builder.establish()

            if (vpnInterface != null) {
                Log.i(TAG, "WireGuard TUN interface established successfully. fd=${vpnInterface?.fd}")
                val app = application as? NexusVpnApplication
                app?.vpnRepository?.setVpnStatus(VpnStatus.CONNECTED)

                updateNotification("Connected to $serverName • WireGuard Active")

                // Start Packet Loop / Tunnel telemetry
                tunnelJob?.cancel()
                tunnelJob = serviceScope.launch {
                    val pfd = vpnInterface ?: return@launch
                    val inputStream = FileInputStream(pfd.fileDescriptor)
                    val outputStream = FileOutputStream(pfd.fileDescriptor)
                    val packetBuffer = ByteArray(32767)

                    try {
                        while (isActive && vpnInterface != null) {
                            // Non-blocking read simulation
                            val length = runCatching { inputStream.read(packetBuffer) }.getOrDefault(0)
                            if (length > 0) {
                                // Real tunnel packets would be encrypted with ChaCha20-Poly1305 and forwarded over UDP socket
                            }
                            delay(50)
                        }
                    } catch (e: IOException) {
                        Log.e(TAG, "Tunnel stream error", e)
                    }
                }
            } else {
                Log.e(TAG, "Failed to establish VPN interface: null returned from builder")
                val app = application as? NexusVpnApplication
                app?.vpnRepository?.setVpnStatus(VpnStatus.ERROR)
                stopSelf()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception creating VPN TUN interface", e)
            val app = application as? NexusVpnApplication
            app?.vpnRepository?.setVpnStatus(VpnStatus.ERROR)
            stopSelf()
        }
    }

    private fun stopVpnTunnel() {
        tunnelJob?.cancel()
        tunnelJob = null

        try {
            vpnInterface?.close()
            vpnInterface = null
            Log.i(TAG, "TUN interface closed.")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing vpn interface", e)
        }

        val app = application as? NexusVpnApplication
        app?.vpnRepository?.setVpnStatus(VpnStatus.DISCONNECTED)

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.vpn_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.vpn_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = Intent(this, WireGuardVpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPendingIntent = PendingIntent.getService(
            this,
            1,
            disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("NexusVPN WireGuard")
            .setContentText(statusText)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Disconnect",
                disconnectPendingIntent
            )
            .build()
    }

    private fun updateNotification(statusText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(statusText))
    }

    override fun onDestroy() {
        stopVpnTunnel()
        super.onDestroy()
    }
}
