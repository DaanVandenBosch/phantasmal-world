package world.phantasmal.web.core.models

/**
 * Represents a PSO private server.
 */
enum class Server(
    /** Display name shown to the user. */
    val uiName: String,
    /** Used in URLs, do not change these. */
    val slug: String,
) {
    Ephinea(uiName = "Ephinea", slug = "ephinea")
}
