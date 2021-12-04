package world.phantasmal.web.viewer.rendering

import mu.KotlinLogging
import org.khronos.webgl.Float32Array
import org.khronos.webgl.Uint16Array
import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.psolib.fileFormats.ninja.XvrTexture
import world.phantasmal.web.core.rendering.*
import world.phantasmal.web.core.rendering.Renderer
import world.phantasmal.web.core.rendering.conversion.xvrTextureToThree
import world.phantasmal.web.externals.three.*
import world.phantasmal.web.viewer.stores.ViewerStore
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
        observeNow(store.currentTextures) {
            texturesChanged(it.filterNotNull())
        }
    }

    private fun texturesChanged(textures: List<XvrTexture>) {
        meshes.forEach { mesh ->
            context.scene.remove(mesh)
            disposeObject3DResources(mesh)
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

    private fun createQuad(x: Int, y: Int, width: Int, height: Int): BufferGeometry {
        val halfWidth = width / 2f
        val halfHeight = height / 2f

        val geom = BufferGeometry()

        geom.setAttribute(
            "position",
            Float32BufferAttribute(
                Float32Array(arrayOf(
                    -halfWidth, -halfHeight, 0f,
                    -halfWidth, halfHeight, 0f,
                    halfWidth, halfHeight, 0f,
                    halfWidth, -halfHeight, 0f,
                )),
                3,
            ),
        )
        geom.setAttribute(
            "normal",
            Float32BufferAttribute(
                Float32Array(arrayOf(
                    0f, 0f, 1f,
                    0f, 0f, 1f,
                    0f, 0f, 1f,
                    0f, 0f, 1f,
                )),
                3,
            ),
        )
        geom.setAttribute(
            "uv",
            Float32BufferAttribute(
                Float32Array(arrayOf(
                    0f, 1f,
                    0f, 0f,
                    1f, 0f,
                    1f, 1f,
                )),
                2,
            ),
        )
        geom.setIndex(Uint16BufferAttribute(
            Uint16Array(arrayOf(
                0, 2, 1,
                2, 0, 3,
            )),
            1,
        ))

        geom.translate(x.toDouble(), y.toDouble(), -5.0)

        return geom
    }

    companion object {
        private const val SPACING = 10
    }
}
