package world.phantasmal.web.core.rendering

import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.core.disposable.TrackedDisposable
import world.phantasmal.web.externals.three.*

open class RenderContext(
    val canvas: HTMLCanvasElement,
    val camera: Camera,
) : TrackedDisposable() {
    private val light = HemisphereLight(
        skyColor = 0xffffff,
        groundColor = 0x505050,
        intensity = 1.0
    )
    private val lightHolder = Group().add(light)

    var width = 0.0
    var height = 0.0

    val scene: Scene =
        Scene().apply {
            background = Color(0x181818)
            add(lightHolder)
        }

    fun pointerPosToDeviceCoords(pos: Vector2) {
        pos.set((pos.x / width) * 2 - 1, (pos.y / height) * -2 + 1)
    }
}
