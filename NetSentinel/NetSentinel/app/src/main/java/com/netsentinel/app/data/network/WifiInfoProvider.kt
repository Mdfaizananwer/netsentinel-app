package com.netsentinel.app.data.network

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface

/**
 * Snapshot of everything the Wi-Fi Analyzer screen shows. Every field is nullable/explicit
 * about *why* it's missing (permission vs. unavailable vs. not-on-wifi) rather than silently
 * showing nothing, per the "handle permission denial and unavailable values gracefully" requirement.
 */
data class WifiSnapshot(
    val isWifiConnected: Boolean,
    val ssid: String?,
    val ssidUnavailableReason: String?,
    val bssid: String?,
    val localIpv4: String?,
    val localIpv6List: List<String>,
    val gatewayIpv4: String?,
    val subnetCidr: String?,
    val dnsServers: List<String>,
    val linkSpeedMbps: Int?,
    val signalStrengthDbm: Int?,
    val signalLevelOutOf: Pair<Int, Int>?, // (level, maxLevel) from WifiManager.calculateSignalLevel
    val frequencyMhz: Int?,
    val capturedAtMillis: Long,
)

class WifiInfoProvider(private val context: Context) {

    private val connectivityManager: ConnectivityManager
        get() = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val wifiManager: WifiManager
        get() = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Builds a full snapshot. This performs no network I/O of its own — everything comes from
     * system APIs (ConnectivityManager / WifiManager / NetworkInterface), so it's safe to call
     * frequently (e.g. on a refresh timer) without rate-limiting.
     */
    fun currentSnapshot(): WifiSnapshot {
        val now = System.currentTimeMillis()
        val activeNetwork: Network? = connectivityManager.activeNetwork
        val capabilities: NetworkCapabilities? = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
        val linkProperties: LinkProperties? = activeNetwork?.let { connectivityManager.getLinkProperties(it) }
        val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

        if (!isWifi) {
            return WifiSnapshot(
                isWifiConnected = false, ssid = null, ssidUnavailableReason = "Device is not connected to Wi-Fi",
                bssid = null, localIpv4 = null, localIpv6List = emptyList(), gatewayIpv4 = null, subnetCidr = null,
                dnsServers = emptyList(), linkSpeedMbps = null, signalStrengthDbm = null, signalLevelOutOf = null,
                frequencyMhz = null, capturedAtMillis = now
            )
        }

        val wifiInfo = wifiManager.connectionInfo
        val locationGranted = hasLocationPermission()

        // SSID/BSSID require location permission from Android 8.0+ and, on Android 10+, the
        // location *setting* must also be enabled system-wide, which this call can't detect
        // directly; WifiManager returns "<unknown ssid>" in that case, which we surface as-is.
        val (ssid, ssidReason) = when {
            !locationGranted -> null to "Location permission not granted (required by Android to read SSID)"
            wifiInfo.ssid.isNullOrEmpty() || wifiInfo.ssid == "<unknown ssid>" -> null to
                "SSID unavailable (device location setting may be off, or Android withheld it)"
            else -> wifiInfo.ssid.removeSurrounding("\"") to null
        }
        val bssid = if (locationGranted) wifiInfo.bssid?.takeIf { it != "02:00:00:00:00:00" } else null

        val linkSpeed = wifiInfo.linkSpeed.takeIf { it >= 0 }
        val rssi = wifiInfo.rssi.takeIf { it != Int.MIN_VALUE && it != 0 }
        val signalLevel = rssi?.let {
            val maxLevel = 4
            WifiManager.calculateSignalLevel(it, maxLevel + 1) to maxLevel
        }
        val frequency = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) wifiInfo.frequency.takeIf { it > 0 } else null

        val ipv4 = linkProperties?.linkAddresses
            ?.mapNotNull { it.address as? Inet4Address }
            ?.firstOrNull()?.hostAddress
        val ipv6List = linkProperties?.linkAddresses
            ?.mapNotNull { it.address as? Inet6Address }
            ?.map { it.hostAddress ?: "" }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

        val prefixLength = linkProperties?.linkAddresses
            ?.firstOrNull { it.address is Inet4Address }?.prefixLength

        val gateway = linkProperties?.routes
            ?.firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }
            ?.gateway?.hostAddress
            ?: wifiManager.dhcpInfo?.gateway?.let { intToIp(it) }

        val subnetCidr = if (ipv4 != null && prefixLength != null) {
            NetworkUtils.cidrNotation(NetworkUtils.deriveSubnet(ipv4, prefixLength).networkAddress, prefixLength)
        } else null

        val dnsServers = linkProperties?.dnsServers?.mapNotNull { it.hostAddress } ?: emptyList()

        return WifiSnapshot(
            isWifiConnected = true,
            ssid = ssid,
            ssidUnavailableReason = ssidReason,
            bssid = bssid,
            localIpv4 = ipv4,
            localIpv6List = ipv6List,
            gatewayIpv4 = gateway,
            subnetCidr = subnetCidr,
            dnsServers = dnsServers,
            linkSpeedMbps = linkSpeed,
            signalStrengthDbm = rssi,
            signalLevelOutOf = signalLevel,
            frequencyMhz = frequency,
            capturedAtMillis = now,
        )
    }

    /** Local Wi-Fi subnet prefix length, used by the scanner; falls back to /24 if unknown. */
    fun currentPrefixLengthOrDefault(): Int {
        val lp = connectivityManager.activeNetwork?.let { connectivityManager.getLinkProperties(it) }
        return lp?.linkAddresses?.firstOrNull { it.address is Inet4Address }?.prefixLength ?: 24
    }

    private fun intToIp(value: Int): String {
        return "${value and 0xFF}.${(value shr 8) and 0xFF}.${(value shr 16) and 0xFF}.${(value shr 24) and 0xFF}"
    }

    /** Lists this device's own interfaces/addresses, used as a fallback when ConnectivityManager yields nothing. */
    fun localInterfaceAddresses(): List<String> {
        return runCatching {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.asSequence() }
                .mapNotNull { it.hostAddress }
                .toList()
        }.getOrDefault(emptyList())
    }
}
