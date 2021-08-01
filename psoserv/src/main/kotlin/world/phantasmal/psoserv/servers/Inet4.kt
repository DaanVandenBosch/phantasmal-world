package world.phantasmal.psoserv.servers

import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress

class Inet4Pair(addr: Inet4Address, port: Int) : InetSocketAddress(addr, port) {
    constructor(addr: ByteArray, port: Int) : this(inet4Address(addr), port)
    constructor(addr: String, port: Int) : this(inet4Address(addr), port)

    val address: Inet4Address get() = super.getAddress() as Inet4Address
}

fun inet4Address(addr: ByteArray): Inet4Address =
    InetAddress.getByAddress(addr) as Inet4Address

fun inet4Address(addr: String): Inet4Address =
    InetAddress.getByName(addr) as Inet4Address

fun inet4Loopback(): Inet4Address =
    InetAddress.getLoopbackAddress() as Inet4Address
