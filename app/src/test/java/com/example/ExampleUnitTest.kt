package com.example

import com.example.data.model.TrafficStats
import com.example.data.model.VpnProtocol
import com.example.data.model.VpnServer
import com.example.data.model.VpnStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `test traffic speed formatter formats kbps and mbps correctly`() {
        val lowStats = TrafficStats(uploadSpeedKbps = 640f, downloadSpeedKbps = 950f)
        assertEquals("640 KB/s", lowStats.formattedUploadSpeed)
        assertEquals("950 KB/s", lowStats.formattedDownloadSpeed)

        val highStats = TrafficStats(uploadSpeedKbps = 2048f, downloadSpeedKbps = 10240f)
        assertEquals("2.0 MB/s", highStats.formattedUploadSpeed)
        assertEquals("10.0 MB/s", highStats.formattedDownloadSpeed)
    }

    @Test
    fun `test traffic duration formatter formats minutes and hours correctly`() {
        val shortDuration = TrafficStats(connectedDurationSeconds = 125L) // 2m 5s
        assertEquals("02:05", shortDuration.formattedDuration)

        val longDuration = TrafficStats(connectedDurationSeconds = 3665L) // 1h 1m 5s
        assertEquals("01:01:05", longDuration.formattedDuration)
    }

    @Test
    fun `test server defaults and protocol port mappings`() {
        assertEquals(51820, VpnProtocol.WIREGUARD.defaultPort)
        assertEquals(1194, VpnProtocol.OPENVPN_UDP.defaultPort)
        assertEquals(443, VpnProtocol.OPENVPN_TCP.defaultPort)

        val server = VpnServer(
            id = "test-1",
            name = "Test Node",
            countryName = "Iceland",
            countryCode = "IS",
            flagEmoji = "🇮🇸",
            ipAddress = "192.168.1.1",
            publicKey = "samplepubkey=="
        )
        assertFalse(server.isPremium)
        assertEquals(51820, server.port)
        assertTrue(server.isOnline)
    }
}
