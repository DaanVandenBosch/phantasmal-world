package world.phantasmal.psoserv

/**
 * Rounds [n] up so that it's divisible by [blockSize].
 */
fun roundToBlockSize(n: Int, blockSize: Int): Int =
    n + (blockSize - n % blockSize) % blockSize
