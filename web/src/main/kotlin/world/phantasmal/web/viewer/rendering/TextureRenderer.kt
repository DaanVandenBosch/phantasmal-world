package world.phantasmal.web.viewer.rendering

import world.phantasmal.lib.fileFormats.ninja.XvrTexture
import world.phantasmal.web.core.rendering.DisposableThreeRenderer
import world.phantasmal.web.core.rendering.Renderer
import world.phantasmal.web.core.rendering.disposeObject3DResources
import world.phantasmal.web.externals.three.*
import world.phantasmal.web.viewer.store.ViewerStore
import world.phantasmal.webui.obj

class TextureRenderer(
    store: ViewerStore,
    createThreeRenderer: () -> DisposableThreeRenderer,
) : Renderer(
    createThreeRenderer,
    OrthographicCamera(
        left = -400.0,
        right = 400.0,
        top = 300.0,
        bottom = -300.0,
        near = 1.0,
        far = 10.0,
    )
) {
    private var meshes = listOf<Mesh>()

    init {
        initializeControls()
        observe(store.currentTextures, ::texturesChanged)
    }

    private fun texturesChanged(textures: List<XvrTexture>) {
        meshes.forEach { mesh ->
            disposeObject3DResources(mesh)
            scene.remove(mesh)
        }

        var x = 0.0

        meshes = textures.map { xvr ->
            val quad = Mesh(
                createQuad(x, 0.0, xvr.width, xvr.height),
                MeshBasicMaterial(obj {
                    map = xvrTextureToThree(xvr, filter = NearestFilter)
                    transparent = true
                })
            )
            scene.add(quad)

            x += xvr.width + 10.0

            quad
        }
    }

    private fun createQuad(x: Double, y: Double, width: Int, height: Int): PlaneGeometry {
        val quad = PlaneGeometry(
            width.toDouble(),
            height.toDouble(),
            widthSegments = 1.0,
            heightSegments = 1.0
        )
        quad.faceVertexUvs = arrayOf(
            arrayOf(
                arrayOf(Vector2(0.0, 0.0), Vector2(0.0, 1.0), Vector2(1.0, 0.0)),
                arrayOf(Vector2(0.0, 1.0), Vector2(1.0, 1.0), Vector2(1.0, 0.0)),
            )
        )
        quad.translate(x + width / 2, y + height / 2, -5.0)
        return quad
    }
}
