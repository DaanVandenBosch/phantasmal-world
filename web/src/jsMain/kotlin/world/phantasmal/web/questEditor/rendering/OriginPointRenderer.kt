package world.phantasmal.web.questEditor.rendering

import kotlinx.browser.document
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.web.externals.three.*
import world.phantasmal.webui.obj
import kotlin.math.PI

/**
 * Renders the world origin point (0,0,0) as a coordinate axes marker with arrows and labels.
 */
class OriginPointRenderer {
    companion object {
        private const val SHAFT_LENGTH = 80.0
        private const val SHAFT_RADIUS = 1.2
        private const val HEAD_LENGTH = 18.0
        private const val HEAD_RADIUS = 4.0
        private const val CENTER_RADIUS = 3.5
        private const val NEG_LENGTH = 25.0
        private const val NEG_RADIUS = 0.6

        private const val X_COLOR = 0xE05555  // Soft red
        private const val Y_COLOR = 0x55C055  // Soft green
        private const val Z_COLOR = 0x5588DD  // Soft blue
        private const val CENTER_COLOR = 0xDDDDDD
    }

    fun createOriginPointVisualization(): Group {
        val group = Group()

        // Center sphere
        group.add(createCenterMarker())

        // Positive axes with arrowheads
        group.add(createArrow(Axis.X))
        group.add(createArrow(Axis.Y))
        group.add(createArrow(Axis.Z))

        // Negative axis stubs (shorter, dimmer)
        group.add(createNegativeStub(Axis.X))
        group.add(createNegativeStub(Axis.Y))
        group.add(createNegativeStub(Axis.Z))

        // Axis labels
        val labelOffset = SHAFT_LENGTH + HEAD_LENGTH + 10.0
        group.add(createAxisLabel("X", X_COLOR, labelOffset, Axis.X))
        group.add(createAxisLabel("Y", Y_COLOR, labelOffset, Axis.Y))
        group.add(createAxisLabel("Z", Z_COLOR, labelOffset, Axis.Z))

        group.position.set(0.0, 0.0, 0.0)
        group.name = "OriginPointVisualization"
        group.renderOrder = 9999
        group.frustumCulled = false

        return group
    }

    private enum class Axis { X, Y, Z }

    /**
     * Creates a positive-direction arrow (cylinder shaft + cone head) along the given axis.
     */
    private fun createArrow(axis: Axis): Group {
        val color = when (axis) { Axis.X -> X_COLOR; Axis.Y -> Y_COLOR; Axis.Z -> Z_COLOR }

        val material = MeshBasicMaterial(obj {
            this.color = Color(color)
            transparent = true
            opacity = 0.85
        }).also {
            it.asDynamic().depthTest = false
            it.asDynamic().depthWrite = false
        }

        val arrow = Group()

        // Shaft (cylinder along +Y by default)
        val shaft = Mesh(CylinderGeometry(SHAFT_RADIUS, SHAFT_RADIUS, SHAFT_LENGTH, 8), material).apply {
            position.y = SHAFT_LENGTH / 2.0
            renderOrder = 10000
            frustumCulled = false
        }
        arrow.add(shaft)

        // Arrowhead (cone)
        val head = Mesh(ConeGeometry(HEAD_RADIUS, HEAD_LENGTH, 12), material).apply {
            position.y = SHAFT_LENGTH + HEAD_LENGTH / 2.0
            renderOrder = 10000
            frustumCulled = false
        }
        arrow.add(head)

        orientGroup(arrow, axis, positive = true)
        arrow.name = "Arrow_${axis.name}"
        return arrow
    }

    /**
     * Creates a shorter, dimmer stub for the negative axis direction.
     */
    private fun createNegativeStub(axis: Axis): Group {
        val color = when (axis) { Axis.X -> X_COLOR; Axis.Y -> Y_COLOR; Axis.Z -> Z_COLOR }

        val material = MeshBasicMaterial(obj {
            this.color = Color(color)
            transparent = true
            opacity = 0.3
        }).also {
            it.asDynamic().depthTest = false
            it.asDynamic().depthWrite = false
        }

        val stub = Group()
        val mesh = Mesh(CylinderGeometry(NEG_RADIUS, NEG_RADIUS, NEG_LENGTH, 6), material).apply {
            position.y = NEG_LENGTH / 2.0
            renderOrder = 10000
            frustumCulled = false
        }
        stub.add(mesh)

        orientGroup(stub, axis, positive = false)
        stub.name = "NegStub_${axis.name}"
        return stub
    }

    /**
     * Rotates a group so its local +Y direction points along the given world axis.
     */
    private fun orientGroup(group: Group, axis: Axis, positive: Boolean) {
        when (axis) {
            Axis.X -> group.rotation.z = if (positive) -PI / 2.0 else PI / 2.0
            Axis.Y -> if (!positive) group.rotation.z = PI
            Axis.Z -> group.rotation.x = if (positive) PI / 2.0 else -PI / 2.0
        }
    }

    private fun createCenterMarker(): Mesh {
        val material = MeshBasicMaterial(obj {
            color = Color(CENTER_COLOR)
            transparent = true
            opacity = 0.9
        }).also {
            it.asDynamic().depthTest = false
            it.asDynamic().depthWrite = false
        }

        return Mesh(SphereGeometry(CENTER_RADIUS, 16, 16), material).apply {
            name = "OriginCenterMarker"
            renderOrder = 10001
            frustumCulled = false
        }
    }

    /**
     * Creates a billboard text label for an axis, positioned at the tip of the arrow.
     */
    private fun createAxisLabel(text: String, color: Int, offset: Double, axis: Axis): Mesh {
        val canvasSize = 64
        val canvas = document.createElement("CANVAS") as HTMLCanvasElement
        canvas.width = canvasSize
        canvas.height = canvasSize
        val ctx = canvas.getContext("2d") as CanvasRenderingContext2D

        ctx.clearRect(0.0, 0.0, canvasSize.toDouble(), canvasSize.toDouble())

        // Draw letter with outline for readability
        ctx.font = "bold 48px Arial"
        ctx.asDynamic().textAlign = "center"
        ctx.asDynamic().textBaseline = "middle"
        ctx.strokeStyle = "#000000"
        ctx.lineWidth = 4.0
        ctx.strokeText(text, canvasSize / 2.0, canvasSize / 2.0)
        ctx.fillStyle = "#${color.toString(16).padStart(6, '0')}"
        ctx.fillText(text, canvasSize / 2.0, canvasSize / 2.0)

        val texture = Texture()
        texture.asDynamic().image = canvas
        texture.needsUpdate = true

        val planeSize = 18.0
        val material = MeshBasicMaterial(obj {
            map = texture
            transparent = true
            alphaTest = 0.05
        }).also {
            it.asDynamic().depthTest = false
            it.asDynamic().depthWrite = false
        }

        return Mesh(PlaneGeometry(planeSize, planeSize), material).apply {
            name = "AxisLabel_$text"
            renderOrder = 10002
            frustumCulled = false

            when (axis) {
                Axis.X -> position.set(offset, 0.0, 0.0)
                Axis.Y -> position.set(0.0, offset, 0.0)
                Axis.Z -> position.set(0.0, 0.0, offset)
            }

            userData = js("{}")
            userData.asDynamic().targetScreenSize = 24.0
        }
    }

    /**
     * Updates axis labels to face the camera (billboard effect) and maintain constant screen size.
     */
    fun updateLabels(camera: Camera, originGroup: Group) {
        originGroup.children.forEach { child ->
            if (child is Mesh && child.name.startsWith("AxisLabel_")) {
                updateBillboardScale(camera, child)
            }
        }
    }
}
