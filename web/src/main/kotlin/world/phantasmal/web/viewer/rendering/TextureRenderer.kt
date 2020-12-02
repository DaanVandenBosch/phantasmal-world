package world.phantasmal.web.viewer.rendering

import mu.KotlinLogging
import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.lib.fileFormats.ninja.XvrTexture
import world.phantasmal.web.core.rendering.*
import world.phantasmal.web.core.rendering.Renderer
import world.phantasmal.web.core.rendering.conversion.xvrTextureToThree
import world.phantasmal.web.externals.three.*
import world.phantasmal.web.viewer.store.ViewerStore
import world.phantasmal.webui.obj
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

private val logger = KotlinLogging.logger {}

class TextureRenderer(
    store: ViewerStore,
    createThreeRenderer: (HTMLCanvasElement) -> DisposableThreeRenderer,
) : Renderer() {
    private var meshes = listOf<Mesh>()

    override val context = addDisposable(RenderContext(
        createCanvas(),
        OrthographicCamera(
            left = -400.0,
            right = 400.0,
            top = 300.0,
            bottom = -300.0,
            near = 1.0,
            far = 10.0,
        )
    ))

    override val threeRenderer = addDisposable(createThreeRenderer(context.canvas)).renderer

    override val inputManager = addDisposable(OrbitalCameraInputManager(
        context.canvas,
        context.camera,
        Vector3(0.0, 0.0, 5.0),
        screenSpacePanning = true,
        enableRotate = false,
    ))

    init {
        observe(store.currentTextures, ::texturesChanged)
    }

    private fun texturesChanged(textures: List<XvrTexture>) {
        meshes.forEach { mesh ->
            disposeObject3DResources(mesh)
            context.scene.remove(mesh)
        }

        inputManager.resetCamera()

        // Lay textures out in a square grid of "cells".
        var cellWidth = -1
        var cellHeight = -1

        textures.forEach {
            cellWidth = max(cellWidth, SPACING + it.width)
            cellHeight = max(cellHeight, SPACING + it.height)
        }

        val cellsPerRow = ceil(sqrt(textures.size.toDouble())).toInt()
        val cellsPerCol = ceil(textures.size.toDouble() / cellsPerRow).toInt()

        // Start at the center of the first cell because the texture quads are placed at the center
        // of the given coordinates.
        val startX = -(cellsPerRow * cellWidth) / 2 + cellWidth / 2
        var x = startX
        var y = (cellsPerCol * cellHeight) / 2 - cellHeight / 2
        var cell = 0

        meshes = textures.map { xvr ->
            val texture =
                try {
                    xvrTextureToThree(xvr, filter = NearestFilter)
                } catch (e: Exception) {
                    logger.error(e) { "Couldn't convert XVR texture." }
                    null
                }

            val quad = Mesh(
                createQuad(x, y, xvr.width, xvr.height),
                MeshBasicMaterial(obj {
                    if (texture == null) {
                        color = Color(0xFF00FF)
                    } else {
                        map = texture
                        transparent = true
                    }
                })
            )
            context.scene.add(quad)

            x += cellWidth

            if (++cell % cellsPerRow == 0) {
                x = startX
                y -= cellHeight
            }

            quad
        }
    }

    private fun createQuad(x: Int, y: Int, width: Int, height: Int): PlaneGeometry {
        val quad = PlaneGeometry(
            width.toDouble(),
            height.toDouble(),
            widthSegments = 1.0,
            heightSegments = 1.0,
        )
        quad.faceVertexUvs = arrayOf(
            arrayOf(
                arrayOf(Vector2(0.0, 0.0), Vector2(0.0, 1.0), Vector2(1.0, 0.0)),
                arrayOf(Vector2(0.0, 1.0), Vector2(1.0, 1.0), Vector2(1.0, 0.0)),
            )
        )
        quad.translate(x.toDouble(), y.toDouble(), -5.0)
        return quad
    }

    companion object {
        private const val SPACING = 10
    }
}
