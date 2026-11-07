package com.example.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.core.content.ContextCompat
import com.example.data.model.VpnServer

object VpnManager {

    fun getPrepareIntent(context: Context): Intent? {
        return VpnService.prepare(context)
    }

    fun startVpn(context: Context, server: VpnServer) {
        val intent = Intent(context, WireGuardVpnService::class.java).apply {
            action = WireGuardVpnService.ACTION_CONNECT
            putExtra(WireGuardVpnService.EXTRA_SERVER_IP, server.ipAddress)
            putExtra(WireGuardVpnService.EXTRA_SERVER_PORT, server.port)
            putExtra(WireGuardVpnService.EXTRA_SERVER_NAME, server.name)
            putExtra(WireGuardVpnService.EXTRA_SERVER_PUBKEY, server.publicKey)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopVpn(context: Context) {
        val intent = Intent(context, WireGuardVpnService::class.java).apply {
            action = WireGuardVpnService.ACTION_DISCONNECT
        }
        context.startService(intent)
    }
}
