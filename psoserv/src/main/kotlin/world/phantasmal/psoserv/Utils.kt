package world.phantasmal.psoserv

/**
 * Rounds [n] up so that it's divisible by [align].
 */
fun alignToWidth(n: Int, align: Int): Int =
    n + (align - n % align) % align
