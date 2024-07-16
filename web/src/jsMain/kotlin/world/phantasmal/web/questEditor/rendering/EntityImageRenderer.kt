package world.phantasmal.web.questEditor.rendering

import kotlinx.browser.document
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.url.URL
import world.phantasmal.core.math.degToRad
import world.phantasmal.psolib.fileFormats.quest.EntityType
import world.phantasmal.web.core.rendering.DisposableThreeRenderer
import world.phantasmal.web.core.rendering.disposeObject3DResources
import world.phantasmal.web.core.timesAssign
import world.phantasmal.web.externals.three.*
import world.phantasmal.web.questEditor.loading.EntityAssetLoader
import world.phantasmal.web.core.loading.LoadingCache
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.obj
import kotlin.math.tan

class EntityImageRenderer(
    private val entityAssetLoader: EntityAssetLoader,
    createThreeRenderer: (HTMLCanvasElement) -> DisposableThreeRenderer,
) : DisposableContainer() {
    private val threeRenderer = addDisposable(
        createThreeRenderer(document.createElement("CANVAS") as HTMLCanvasElement)
    ).renderer.apply {
        setClearColor(Color(0x000000), alpha = 0.0)
        autoClearColor = false
        setSize(100.0, 100.0)
    }

    private val cache: LoadingCache<EntityType, String> = addDisposable(
        LoadingCache(::renderToDataUrl) { URL.revokeObjectURL(it) }
    )

    private val scene = Scene()

    private val light = HemisphereLight(0xffffff, 0x505050, 1.2)
    private val camera = PerspectiveCamera(fov = 30.0, aspect = 1.0, near = 10.0, far = 2000.0)
    private val cameraPos = Vector3(1.0, 1.0, 2.0).normalize()
    private val cameraDistFactor = 1.3 / tan(degToRad(camera.fov) / 2)

    suspend fun renderToImage(type: EntityType): String = cache.get(type)

    private suspend fun renderToDataUrl(type: EntityType): String {
        // First render a flat version of the model with the same color as the background. Then
        // render the final version on top of that. We do this to somewhat fix issues with
        // additive alpha blending on a transparent background.

        val mesh = entityAssetLoader.loadInstancedMesh(type, model = null)
        val origMaterial = mesh.material

        try {
            mesh.count = 1
            mesh.setMatrixAt(0, Matrix4())
            scene.clear()
            scene.add(light, mesh)

            // Compute camera position.
            val bSphere = mesh.geometry.boundingSphere!!
            camera.position.copy(cameraPos)
            camera.position *= bSphere.radius * cameraDistFactor
            camera.lookAt(bSphere.center)

            // Render the flat model.
            mesh.material = BACKGROUND_MATERIAL
            threeRenderer.clearColor()
            threeRenderer.render(scene, camera)

            // Render the textured model.
            mesh.material = origMaterial
            threeRenderer.render(scene, camera)

            return threeRenderer.domElement.toDataURL()
        } finally {
            // Ensure we dispose the original material and not the background material.
            mesh.material = origMaterial
            disposeObject3DResources(mesh)
        }
    }

    companion object {
        private val BACKGROUND_MATERIAL = MeshBasicMaterial(obj {
            color = Color(0x262626)
            side = DoubleSide
        })
    }
}
