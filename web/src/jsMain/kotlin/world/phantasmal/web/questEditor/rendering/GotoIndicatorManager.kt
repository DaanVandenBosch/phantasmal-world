package world.phantasmal.web.questEditor.rendering

import world.phantasmal.web.externals.three.Color
import world.phantasmal.web.externals.three.CylinderGeometry
import world.phantasmal.web.externals.three.DoubleSide
import world.phantasmal.web.externals.three.Group
import world.phantasmal.web.externals.three.Mesh
import world.phantasmal.web.externals.three.MeshBasicMaterial
import world.phantasmal.web.externals.three.RingGeometry
import world.phantasmal.web.externals.three.Vector3
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.obj
import kotlin.js.Date
import kotlin.math.PI
import kotlin.math.sqrt

/**
 * Renders an animated marker at the camera's last goto-position target so the user can see where
 * they were teleported. Persists until the next goto (or explicit clear).
 *
 * Visual: a thin vertical rod through the spot, a small dot at the spot, and an outer ring
 * lying flat on the "ground" that pulses outward — a classic "you-are-here" map marker with
 * just enough motion to draw the eye without being noisy.
 *
 * Frame-time animation is updated via [update] which the [QuestMeshManager] hooks into the
 * render loop's `beforeRender`.
 */
class GotoIndicatorManager(
    private val renderContext: QuestRenderContext,
) : DisposableContainer() {
    private val container: Group = Group().apply {
        name = "Goto Indicator"
        visible = false
        renderContext.helpers.add(this)
    }

    // Pulsing outer ring (animated each frame). Uses its own material so the opacity tween
    // doesn't bleed into the static parts.
    private val outerRingMaterial = MeshBasicMaterial(obj {
        color = COLOR
        transparent = true
        opacity = 1.0
        side = DoubleSide
    }).apply {
        // depthWrite isn't declared on MeshBasicMaterialParameters externals — set after construction.
        asDynamic().depthWrite = false
    }
    private val outerRing = Mesh(
        RingGeometry(
            innerRadius = OUTER_RING_INNER,
            outerRadius = OUTER_RING_OUTER,
            thetaSegments = 48,
        ),
        outerRingMaterial,
    ).apply {
        // RingGeometry is in the XY plane by default; rotate to lie flat on XZ ground.
        rotation.x = -PI / 2
    }

    init {
        // Static inner ring — a fixed visual anchor.
        val innerRing = Mesh(
            RingGeometry(
                innerRadius = INNER_RING_INNER,
                outerRadius = INNER_RING_OUTER,
                thetaSegments = 32,
            ),
            STATIC_MATERIAL,
        ).apply {
            rotation.x = -PI / 2
        }

        // Slim vertical rod — visible from above when the camera is high.
        val rod = Mesh(
            CylinderGeometry(
                radiusTop = 0.5,
                radiusBottom = 0.5,
                height = ROD_HEIGHT,
                radialSegments = 8,
            ),
            STATIC_MATERIAL,
        ).apply {
            position.y = ROD_HEIGHT / 2
        }

        // Tiny anchor dot at the exact target.
        val dot = Mesh(
            CylinderGeometry(
                radiusTop = 1.5,
                radiusBottom = 1.5,
                height = 0.4,
                radialSegments = 16,
            ),
            STATIC_MATERIAL,
        )

        container.add(innerRing)
        container.add(outerRing)
        container.add(rod)
        container.add(dot)
    }

    override fun dispose() {
        renderContext.helpers.remove(container)
        super.dispose()
    }

    fun setPosition(position: Vector3?) {
        if (position == null) {
            container.visible = false
        } else {
            container.position.set(position.x, position.y, position.z)
            container.visible = true
        }
    }

    /**
     * Drives the outer-ring pulse animation AND the camera-distance scaling so the marker stays
     * roughly the same on-screen size whether the camera is close or far. Should be called once
     * per frame from the render loop. No-op while the indicator is hidden.
     */
    fun update() {
        if (!container.visible) return

        // Constant-screen-size scaling: as the camera pulls back, scale the whole marker up so
        // it doesn't shrink to a dot; as the camera dives in, scale down so it doesn't dominate.
        // Same pattern used elsewhere in the editor for billboard text labels.
        val cam = renderContext.camera.position
        val cx = cam.x - container.position.x
        val cy = cam.y - container.position.y
        val cz = cam.z - container.position.z
        val distance = sqrt(cx * cx + cy * cy + cz * cz)
        val baseScale = (distance / BASE_DISTANCE).coerceIn(MIN_SCALE, MAX_SCALE)
        container.scale.set(baseScale, baseScale, baseScale)

        // Pulse animation runs in local space, multiplied on top of the base scale via outerRing.
        val now = Date.now() / 1000.0
        val phase = (now % PULSE_PERIOD) / PULSE_PERIOD
        val eased = 1.0 - (1.0 - phase) * (1.0 - phase)
        val pulseScale = 1.0 + eased * (PULSE_SCALE - 1.0)
        outerRing.scale.set(pulseScale, pulseScale, 1.0)
        outerRingMaterial.asDynamic().opacity = (1.0 - eased) * 0.9
    }

    companion object {
        private val COLOR = Color(0xFF7A1F) // warm orange — distinct from yellow markers

        private val STATIC_MATERIAL = MeshBasicMaterial(obj {
            color = COLOR
            transparent = true
            opacity = 0.9
            side = DoubleSide
        }).apply {
            asDynamic().depthWrite = false
        }

        private const val OUTER_RING_INNER = 14.0
        private const val OUTER_RING_OUTER = 16.0
        private const val INNER_RING_INNER = 8.0
        private const val INNER_RING_OUTER = 9.0
        private const val ROD_HEIGHT = 60.0

        private const val PULSE_PERIOD = 1.6
        private const val PULSE_SCALE = 2.0

        // Camera-distance scaling: at BASE_DISTANCE the indicator renders at 1× world size.
        // Closer than that → smaller scale; farther → larger scale. Clamps avoid the indicator
        // disappearing or completely dominating the viewport.
        private const val BASE_DISTANCE = 800.0
        private const val MIN_SCALE = 0.25
        private const val MAX_SCALE = 6.0
    }
}
