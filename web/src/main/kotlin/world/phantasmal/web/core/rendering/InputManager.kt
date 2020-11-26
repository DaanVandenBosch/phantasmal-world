package world.phantasmal.web.core.rendering

/**
 * Manages user input such as pointer and keyboard events.
 */
interface InputManager {
    fun setSize(width: Double, height: Double)

    fun resetCamera()

    fun beforeRender()
}
