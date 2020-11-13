package world.phantasmal.web.core.rendering

import mu.KotlinLogging
import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.web.externals.babylon.*
import world.phantasmal.webui.DisposableContainer

private val logger = KotlinLogging.logger {}

abstract class Renderer(
    val canvas: HTMLCanvasElement,
    val engine: Engine,
) : DisposableContainer() {
    private val light: HemisphericLight

    abstract val camera: Camera

    val scene = Scene(engine)

    init {
        with(scene) {
            useRightHandedSystem = true
            clearColor = Color4.FromInts(0x18, 0x18, 0x18, 0xFF)
        }

        light = HemisphericLight("Light", Vector3(-1.0, 1.0, 1.0), scene)
    }

    override fun internalDispose() {
        camera.dispose()
        light.dispose()
        scene.dispose()
        engine.dispose()
        super.internalDispose()
    }

    fun startRendering() {
        logger.trace { "${this::class.simpleName} - start rendering." }
        engine.runRenderLoop(::render)
    }

    fun stopRendering() {
        logger.trace { "${this::class.simpleName} - stop rendering." }
        engine.stopRenderLoop()
    }

    protected open fun render() {
        val lightDirection = Vector3(-1.0, 1.0, 1.0)
        lightDirection.rotateByQuaternionToRef(camera.absoluteRotation, lightDirection)
        light.direction = lightDirection
        scene.render()
    }
}
