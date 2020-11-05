package world.phantasmal.core

/**
 * Returns the given filename without the file extension.
 */
fun basename(filename: String): String {
    val dotIdx = filename.lastIndexOf(".")

    // < 0 means filename doesn't contain any "."
    // Also skip index 0 because that would mean the basename is empty.
    if (dotIdx > 1) {
        return filename.substring(0, dotIdx)
    }

    return filename
}
