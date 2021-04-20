package world.phantasmal.core

/**
 * Returns the given filename without the file extension.
 */
fun filenameBase(filename: String): String? =
    when (val dotIdx = filename.lastIndexOf(".")) {
        // Empty basename.
        0 -> null
        // No extension.
        -1 -> filename
        // Has a basename and extension.
        else -> filename.substring(0, dotIdx)
    }

/**
 * Returns the extension of the given filename.
 */
fun filenameExtension(filename: String): String? =
    when (val dotIdx = filename.lastIndexOf(".")) {
        // No extension.
        -1 -> null
        // Empty extension.
        filename.lastIndex -> null
        // Has an extension.
        else -> filename.substring(dotIdx + 1)
    }

expect inline fun String.getCodePointAt(index: Int): Int
