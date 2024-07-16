package world.phantasmal.web.viewer.rendering

import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.core.disposable.TrackedDisposable
import world.phantasmal.core.math.degToRad
import world.phantasmal.psolib.fileFormats.ninja.NinjaObject
import world.phantasmal.psolib.fileFormats.ninja.NjMotion
import world.phantasmal.psolib.fileFormats.ninja.NjObject
import world.phantasmal.web.core.boundingSphere
import world.phantasmal.web.core.isSkinnedMesh
import world.phantasmal.web.core.rendering.DisposableThreeRenderer
import world.phantasmal.web.core.rendering.OrbitalCameraInputManager
import world.phantasmal.web.core.rendering.RenderContext
import world.phantasmal.web.core.rendering.Renderer
import world.phantasmal.web.core.rendering.conversion.PSO_FRAME_RATE_DOUBLE
import world.phantasmal.web.core.rendering.conversion.collisionGeometryToGroup
import world.phantasmal.web.core.rendering.conversion.createAnimationClip
import world.phantasmal.web.core.rendering.conversion.ninjaObjectToMesh
import world.phantasmal.web.core.rendering.conversion.ninjaObjectToSkinnedMesh
import world.phantasmal.web.core.rendering.conversion.renderGeometryToGroup
import world.phantasmal.web.core.rendering.disposeObject3DResources
import world.phantasmal.web.core.times
import world.phantasmal.web.externals.three.AnimationAction
import world.phantasmal.web.externals.three.AnimationClip
import world.phantasmal.web.externals.three.AnimationMixer
import world.phantasmal.web.externals.three.Clock
import world.phantasmal.web.externals.three.LineBasicMaterial
import world.phantasmal.web.externals.three.Object3D
import world.phantasmal.web.externals.three.PerspectiveCamera
import world.phantasmal.web.externals.three.SkeletonHelper
import world.phantasmal.web.externals.three.Vector3
import world.phantasmal.web.shared.Throttle
import world.phantasmal.web.viewer.stores.NinjaGeometry
import world.phantasmal.web.viewer.stores.ViewerStore
import kotlin.math.roundToInt
import kotlin.math.tan

class MeshRenderer(
    private val viewerStore: ViewerStore,
    createThreeRenderer: (HTMLCanvasElement) -> DisposableThreeRenderer,
) : Renderer() {
    private val clock = Clock()
    private val throttleRebuildMesh = Throttle(wait = 10, leading = false, trailing = true)

    private var obj3d: Object3D? = null
    private var skeletonHelper: SkeletonHelper? = null
    private var animation: Animation? = null
    private var updateAnimationTime = true
    private var charClassActive = false
    private var resetCamera = true

    override val context = addDisposable(
        RenderContext(
            createCanvas(),
            PerspectiveCamera(
                fov = 45.0,
                aspect = 1.0,
                near = 10.0,
                far = 5_000.0,
            )
        )
    )

    override val threeRenderer = addDisposable(createThreeRenderer(context.canvas)).renderer

    override val inputManager = addDisposable(
        OrbitalCameraInputManager(
            context.canvas,
            context.camera,
            position = Vector3(.0, .0, .0),
            screenSpacePanning = true,
        )
    )

    init {
        observeNow(viewerStore.currentNinjaGeometry, viewerStore.currentTextures) { _, _ ->
            resetCamera = true
            rebuildMesh()
        }
        observeNow(viewerStore.applyTextures) { rebuildMesh() }
        observeNow(viewerStore.currentNinjaMotion, ::ninjaMotionChanged)
        observeNow(viewerStore.showSkeleton) { skeletonHelper?.visible = it }
        observeNow(viewerStore.animationPlaying, ::animationPlayingChanged)
        observeNow(viewerStore.frameRate, ::frameRateChanged)
        observeNow(viewerStore.frame, ::frameChanged)
    }

    override fun dispose() {
        animation?.dispose()
        super.dispose()
    }

    override fun render() {
        animation?.mixer?.update(clock.getDelta())

        context.lightHolder.quaternion.copy(context.camera.quaternion)

        super.render()

        animation?.let {
            if (!it.action.paused) {
                updateAnimationTime = false
                viewerStore.setFrame((it.action.time * PSO_FRAME_RATE_DOUBLE + 1).roundToInt())
                updateAnimationTime = true
            }
        }
    }

    private fun rebuildMesh() {
        throttleRebuildMesh {
            // Remove the previous mesh.
            obj3d?.let { mesh ->
                disposeObject3DResources(mesh)
                context.scene.remove(mesh)
            }

            // Remove the previous skeleton.
            skeletonHelper?.let {
                context.scene.remove(it)
                skeletonHelper = null
            }

            val ninjaGeometry = viewerStore.currentNinjaGeometry.value
            val textures =
                if (viewerStore.applyTextures.value) viewerStore.currentTextures.value
                else emptyList()

            // Stop and clean up previous animation and store animation time.
            var animationTime: Double? = null

            animation?.let {
                animationTime = it.action.time
                it.dispose()
                this.animation = null
            }

            // Create a new mesh if necessary.
            if (ninjaGeometry != null) {
                val obj3d = when (ninjaGeometry) {
                    is NinjaGeometry.Object -> {
                        val obj = ninjaGeometry.obj

                        if (obj is NjObject) {
                            ninjaObjectToSkinnedMesh(
                                obj,
                                textures,
                                boundingVolumes = true,
                                anisotropy = threeRenderer.capabilities.getMaxAnisotropy() / 2,
                            )
                        } else {
                            ninjaObjectToMesh(
                                obj,
                                textures,
                                boundingVolumes = true,
                                anisotropy = threeRenderer.capabilities.getMaxAnisotropy() / 2,
                            )
                        }
                    }

                    is NinjaGeometry.Render -> renderGeometryToGroup(
                        ninjaGeometry.geometry,
                        textures,
                        anisotropy = threeRenderer.capabilities.getMaxAnisotropy() / 2,
                    )

                    is NinjaGeometry.Collision -> collisionGeometryToGroup(ninjaGeometry.geometry)
                }

                // Determine whether camera needs to be reset. Resets should always happen when the
                // Ninja geometry changes except when we're switching between character class models.
                val charClassActive = viewerStore.currentCharacterClass.value != null
                val cameraResetNecessary = !charClassActive || !this.charClassActive
                this.charClassActive = charClassActive

                if (resetCamera && cameraResetNecessary) {
                    // Compute camera position.
                    val bSphere = boundingSphere(obj3d)
                    val cameraDistFactor =
                        1.5 / tan(degToRad((context.camera as PerspectiveCamera).fov) / 2)
                    val cameraPos = CAMERA_POS * (bSphere.radius * cameraDistFactor)
                    inputManager.lookAt(cameraPos, bSphere.center)
                    resetCamera = false
                }

                context.scene.add(obj3d)
                this.obj3d = obj3d

                if (obj3d.isSkinnedMesh() && ninjaGeometry is NinjaGeometry.Object) {
                    // Add skeleton.
                    val skeletonHelper = SkeletonHelper(obj3d)
                    skeletonHelper.visible = viewerStore.showSkeleton.value
                    skeletonHelper.material.unsafeCast<LineBasicMaterial>().linewidth = 3.0

                    context.scene.add(skeletonHelper)
                    this.skeletonHelper = skeletonHelper

                    // Create a new animation mixer and clip.
                    viewerStore.currentNinjaMotion.value?.let { njMotion ->
                        animation = Animation(ninjaGeometry.obj, njMotion, obj3d).also {
                            it.mixer.timeScale = viewerStore.frameRate.value / PSO_FRAME_RATE_DOUBLE
                            it.action.time = animationTime ?: .0
                            it.action.play()
                        }
                    }
                }
            }
        }
    }

    private fun ninjaMotionChanged(njMotion: NjMotion?) {
        animation?.let {
            it.dispose()
            animation = null
        }

        val mesh = obj3d
        val njObject = (viewerStore.currentNinjaGeometry.value as? NinjaGeometry.Object)?.obj

        if (mesh == null || !mesh.isSkinnedMesh() || njObject == null || njMotion == null) {
            return
        }

        animation = Animation(njObject, njMotion, mesh).also {
            it.mixer.timeScale = viewerStore.frameRate.value / PSO_FRAME_RATE_DOUBLE
            it.action.play()
        }

        clock.start()
    }

    private fun animationPlayingChanged(playing: Boolean) {
        animation?.let {
            it.action.paused = !playing

            if (playing) {
                clock.start()
            } else {
                clock.stop()
            }
        }
    }

    private fun frameRateChanged(frameRate: Int) {
        animation?.let {
            it.mixer.timeScale = frameRate / PSO_FRAME_RATE_DOUBLE
        }
    }

    private fun frameChanged(frame: Int) {
        if (updateAnimationTime) {
            animation?.let {
                it.action.time = (frame - 1) / PSO_FRAME_RATE_DOUBLE
            }
        }
    }

    private class Animation(
        njObject: NinjaObject<*, *>,
        njMotion: NjMotion,
        root: Object3D,
    ) : TrackedDisposable() {
        private val clip: AnimationClip = createAnimationClip(njObject, njMotion)

        val mixer = AnimationMixer(root)
        val action: AnimationAction = mixer.clipAction(clip)

        override fun dispose() {
            mixer.stopAllAction()
            mixer.uncacheAction(clip)
            super.dispose()
        }
    }

    companion object {
        private val CAMERA_POS = Vector3(1.0, 1.0, 2.0).normalize()
    }
}
