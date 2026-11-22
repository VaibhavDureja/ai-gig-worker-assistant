package com.parkcast.mirror

import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object LocalAddress {
    /** Best-effort LAN address; VPNs and complex networks may require manual adjustment. */
    fun discover(): String? = runCatching {
        val candidates = Collections.list(NetworkInterface.getNetworkInterfaces())
            .filter { it.isUp && !it.isLoopback && !it.isVirtual }
            .flatMap { network ->
                Collections.list(network.inetAddresses)
                    .filterIsInstance<Inet4Address>()
                    .filter { !it.isLoopbackAddress && !it.isLinkLocalAddress }
                    .map { network.name.lowercase() to it.hostAddress }
            }
        candidates.sortedBy { (name, _) ->
            when {
                name.startsWith("wlan") || name.startsWith("wifi") -> 0
                name.startsWith("ap") || name.startsWith("swlan") -> 1
                name.startsWith("eth") -> 2
                else -> 3
            }
        }.firstOrNull()?.second
    }.getOrNull()
}
