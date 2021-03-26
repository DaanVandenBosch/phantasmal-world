package world.phantasmal.web.viewer.rendering

import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.core.math.degToRad
import world.phantasmal.lib.fileFormats.ninja.NjMotion
import world.phantasmal.web.core.rendering.*
import world.phantasmal.web.core.rendering.Renderer
import world.phantasmal.web.core.rendering.conversion.createAnimationClip
import world.phantasmal.web.core.rendering.conversion.ninjaObjectToSkinnedMesh
import world.phantasmal.web.core.times
import world.phantasmal.web.externals.three.*
import world.phantasmal.web.viewer.stores.ViewerStore
import kotlin.math.tan

class MeshRenderer(
    private val viewerStore: ViewerStore,
    createThreeRenderer: (HTMLCanvasElement) -> DisposableThreeRenderer,
) : Renderer() {
    private val clock = Clock()

    private var mesh: Mesh? = null
    private var skeletonHelper: SkeletonHelper? = null
    private var animation: Animation? = null
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
    }

    override fun render() {
        animation?.mixer?.update(clock.getDelta())

        super.render()

        animation?.let {
            // TODO: Update current animation frame in store.
//            val action = it.mixer.clipAction(it.clip)
//
//            if (!action.paused) {
//            }
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

        animation?.let { animation ->
            val mixer = animation.mixer
            animationTime = mixer.existingAction(animation.clip).time
            mixer.stopAllAction()
            mixer.uncacheAction(animation.clip)
            this.animation = null
        }

        // Create a new mesh if necessary.
        if (ninjaObject != null) {
            val mesh = ninjaObjectToSkinnedMesh(ninjaObject, textures, boundingVolumes = true)

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

            // Add skeleton.
            val skeletonHelper = SkeletonHelper(mesh)
            skeletonHelper.visible = viewerStore.showSkeleton.value
            skeletonHelper.asDynamic().material.lineWidth = 3

            context.scene.add(skeletonHelper)
            this.skeletonHelper = skeletonHelper

            // Create a new animation mixer and clip.
            viewerStore.currentNinjaMotion.value?.let { nj_motion ->
                val mixer = AnimationMixer(mesh)
                // TODO: Set time scale.
//                mixer.timeScale = this.store.animation_frame_rate.val / PSO_FRAME_RATE;

                val clip = createAnimationClip(ninjaObject, nj_motion)

                animation = Animation(mixer, clip)

                val action = mixer.clipAction(clip, mesh)
                action.time = animationTime ?: .0
                action.play()
            }
        }
    }

    private fun ninjaMotionChanged(njMotion: NjMotion?) {
        var mixer: AnimationMixer? = null

        animation?.let {
            it.mixer.stopAllAction()
            it.mixer.uncacheAction(it.clip)
            mixer = it.mixer
            animation = null
        }

        val mesh = mesh
        val njObject = viewerStore.currentNinjaObject.value

        if (mesh == null || mesh !is SkinnedMesh || njObject == null || njMotion == null) {
            return
        }

        if (mixer == null) {
            mixer = AnimationMixer(mesh)
        }

        val clip = createAnimationClip(njObject, njMotion)

        animation = Animation(mixer!!, clip)

        clock.start()
        mixer!!.clipAction(clip).play()
    }

    private class Animation(val mixer: AnimationMixer, val clip: AnimationClip)

    companion object {
        private val CAMERA_POS = Vector3(1.0, 1.0, 2.0).normalize()
    }
}
