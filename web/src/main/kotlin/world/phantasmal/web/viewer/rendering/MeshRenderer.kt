package world.phantasmal.web.viewer.rendering

import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.core.disposable.TrackedDisposable
import world.phantasmal.core.math.degToRad
import world.phantasmal.lib.fileFormats.ninja.NinjaObject
import world.phantasmal.lib.fileFormats.ninja.NjMotion
import world.phantasmal.lib.fileFormats.ninja.NjObject
import world.phantasmal.web.core.rendering.*
import world.phantasmal.web.core.rendering.Renderer
import world.phantasmal.web.core.rendering.conversion.PSO_FRAME_RATE_DOUBLE
import world.phantasmal.web.core.rendering.conversion.createAnimationClip
import world.phantasmal.web.core.rendering.conversion.ninjaObjectToMesh
import world.phantasmal.web.core.rendering.conversion.ninjaObjectToSkinnedMesh
import world.phantasmal.web.core.times
import world.phantasmal.web.externals.three.*
import world.phantasmal.web.viewer.stores.ViewerStore
import kotlin.math.roundToInt
import kotlin.math.tan

class MeshRenderer(
    private val viewerStore: ViewerStore,
    createThreeRenderer: (HTMLCanvasElement) -> DisposableThreeRenderer,
) : Renderer() {
    private val clock = Clock()

    private var mesh: Mesh? = null
    private var skeletonHelper: SkeletonHelper? = null
    private var animation: Animation? = null
    private var updateAnimationTime = true
    private var charClassActive = false

    override val context = addDisposable(RenderContext(
        createCanvas(),
        PerspectiveCamera(
            fov = 45.0,
            aspect = 1.0,
            near = 10.0,
            far = 5_000.0,
        )
    ))

    override val threeRenderer = addDisposable(createThreeRenderer(context.canvas)).renderer

    override val inputManager = addDisposable(OrbitalCameraInputManager(
        context.canvas,
        context.camera,
        Vector3(),
        screenSpacePanning = true,
    ))

    init {
        observe(viewerStore.currentNinjaObject) { ninjaObjectOrXvmChanged() }
        observe(viewerStore.currentTextures) { ninjaObjectOrXvmChanged() }
        observe(viewerStore.currentNinjaMotion, ::ninjaMotionChanged)
        observe(viewerStore.showSkeleton) { skeletonHelper?.visible = it }
        observe(viewerStore.animationPlaying, ::animationPlayingChanged)
        observe(viewerStore.frameRate, ::frameRateChanged)
        observe(viewerStore.frame, ::frameChanged)
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

    private fun ninjaObjectOrXvmChanged() {
        // Remove the previous mesh.
        mesh?.let { mesh ->
            disposeObject3DResources(mesh)
            context.scene.remove(mesh)
        }

        // Remove the previous skeleton.
        skeletonHelper?.let {
            context.scene.remove(it)
            skeletonHelper = null
        }

        val ninjaObject = viewerStore.currentNinjaObject.value
        val textures = viewerStore.currentTextures.value

        // Stop and clean up previous animation and store animation time.
        var animationTime: Double? = null

        animation?.let {
            animationTime = it.action.time
            it.dispose()
            this.animation = null
        }

        // Create a new mesh if necessary.
        if (ninjaObject != null) {
            val mesh =
                if (ninjaObject is NjObject) {
                    ninjaObjectToSkinnedMesh(ninjaObject, textures, boundingVolumes = true)
                } else {
                    ninjaObjectToMesh(ninjaObject, textures, boundingVolumes = true)
                }

            // Determine whether camera needs to be reset. Resets should always happen when the
            // Ninja object changes except when we're switching between character class models.
            val charClassActive = viewerStore.currentCharacterClass.value != null
            val resetCamera = !charClassActive || !this.charClassActive
            this.charClassActive = charClassActive

            if (resetCamera) {
                // Compute camera position.
                val bSphere = mesh.geometry.boundingSphere!!
                val cameraDistFactor =
                    1.5 / tan(degToRad((context.camera as PerspectiveCamera).fov) / 2)
                val cameraPos = CAMERA_POS * (bSphere.radius * cameraDistFactor)
                inputManager.lookAt(cameraPos, bSphere.center)
            }

            context.scene.add(mesh)
            this.mesh = mesh

            if (mesh is SkinnedMesh) {
                // Add skeleton.
                val skeletonHelper = SkeletonHelper(mesh)
                skeletonHelper.visible = viewerStore.showSkeleton.value
                skeletonHelper.asDynamic().material.lineWidth = 3

                context.scene.add(skeletonHelper)
                this.skeletonHelper = skeletonHelper

                // Create a new animation mixer and clip.
                viewerStore.currentNinjaMotion.value?.let { njMotion ->
                    animation = Animation(ninjaObject, njMotion, mesh).also {
                        it.mixer.timeScale = viewerStore.frameRate.value / PSO_FRAME_RATE_DOUBLE
                        it.action.time = animationTime ?: .0
                        it.action.play()
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

        val mesh = mesh
        val njObject = viewerStore.currentNinjaObject.value

        if (mesh == null || mesh !is SkinnedMesh || njObject == null || njMotion == null) {
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
