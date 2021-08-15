package world.phantasmal.psoserv.utils

fun Int.toHex(pad: Int = 8): String =
    "0x" + toString(16).uppercase().padStart(pad, '0')

fun UByte.toHex(pad: Int = 2): String =
    "0x" + toString(16).uppercase().padStart(pad, '0')
