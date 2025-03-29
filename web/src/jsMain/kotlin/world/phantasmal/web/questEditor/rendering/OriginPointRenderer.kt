package world.phantasmal.web.questEditor.rendering

import kotlinx.browser.document
import org.khronos.webgl.Float32Array
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.web.externals.three.*
import world.phantasmal.webui.obj
import kotlin.math.PI

/**
 * Renders the world origin point (0,0,0) as a distinctive marker.
 */
class OriginPointRenderer {
    companion object {
        private const val MARKER_SIZE = 50.0f  // Size of the origin marker
        private const val AXIS_LENGTH = 100.0f  // Length of axis lines
        private const val X_AXIS_COLOR = 0xFF0000  // Red for X axis
        private const val Y_AXIS_COLOR = 0x00FF00  // Green for Y axis
        private const val Z_AXIS_COLOR = 0x0000FF  // Blue for Z axis
        private const val CENTER_COLOR = 0xFFFFFF  // White for center marker
        private const val TEXT_COLOR = 0xFFFFFF  // White text
        private const val TEXT_SIZE = 24.0  // Text size
    }

    /**
     * Creates the origin point visualization with coordinate axes and label.
     */
    fun createOriginPointVisualization(): Group {
        val group = Group()

        // Create coordinate axes (X, Y, Z)
        val axes = createCoordinateAxes()
        group.add(axes)

        // Create center marker
        val centerMarker = createCenterMarker()
        group.add(centerMarker)

        // Create text label (removed for cleaner display)
        // val textLabel = createOriginLabel()
        // textLabel.position.y = MARKER_SIZE.toDouble() + 20.0  // Position above the marker
        // group.add(textLabel)

        // Position at world origin
        group.position.set(0.0, 0.0, 0.0)
        group.name = "OriginPointVisualization"

        // Ensure high render order for visibility
        group.renderOrder = 9999
        group.frustumCulled = false

        return group
    }

    /**
     * Creates the coordinate axes (X=Red, Y=Green, Z=Blue).
     */
    private fun createCoordinateAxes(): Group {
        val axesGroup = Group()

        // X axis (Red)
        val xAxis = createAxisLine(
            Vector3(0.0, 0.0, 0.0),
            Vector3(AXIS_LENGTH.toDouble(), 0.0, 0.0),
            X_AXIS_COLOR
        )
        axesGroup.add(xAxis)

        // Y axis (Green)
        val yAxis = createAxisLine(
            Vector3(0.0, 0.0, 0.0),
            Vector3(0.0, AXIS_LENGTH.toDouble(), 0.0),
            Y_AXIS_COLOR
        )
        axesGroup.add(yAxis)

        // Z axis (Blue)
        val zAxis = createAxisLine(
            Vector3(0.0, 0.0, 0.0),
            Vector3(0.0, 0.0, AXIS_LENGTH.toDouble()),
            Z_AXIS_COLOR
        )
        axesGroup.add(zAxis)

        axesGroup.name = "CoordinateAxes"
        return axesGroup
    }

    /**
     * Creates a single axis line.
     */
    private fun createAxisLine(start: Vector3, end: Vector3, color: Int): LineSegments {
        val points = floatArrayOf(
            start.x.toFloat(), start.y.toFloat(), start.z.toFloat(),
            end.x.toFloat(), end.y.toFloat(), end.z.toFloat()
        )

        val geometry = BufferGeometry().apply {
            setAttribute("position", Float32BufferAttribute(Float32Array(points.toTypedArray()), 3))
        }

        val material = LineBasicMaterial(obj {
            this.color = Color(color)
            transparent = true
            opacity = 0.9
            linewidth = 4.0
        })

        return LineSegments(geometry, material).apply {
            name = "AxisLine_${color.toString(16)}"
            renderOrder = 10000
            frustumCulled = false
        }
    }

    /**
     * Creates the center marker at the origin.
     */
    private fun createCenterMarker(): Mesh {
        val geometry = SphereGeometry(MARKER_SIZE.toDouble() / 4.0, 16, 16)

        val material = MeshBasicMaterial(obj {
            color = Color(CENTER_COLOR)
            transparent = true
            opacity = 0.8
        })

        // Force visibility settings
        material.asDynamic().depthTest = false
        material.asDynamic().depthWrite = false

        return Mesh(geometry, material).apply {
            name = "OriginCenterMarker"
            renderOrder = 10000
            frustumCulled = false
        }
    }

    /**
     * Creates the text label for the origin point.
     */
    private fun createOriginLabel(): Mesh {
        return createTextPlane("Origin (0,0,0)", TEXT_COLOR, TEXT_SIZE)
    }

    /**
     * Creates a text plane using canvas texture for displaying text.
     */
    private fun createTextPlane(text: String, color: Int, size: Double): Mesh {
        // Create canvas for text rendering
        val canvas = document.createElement("CANVAS") as HTMLCanvasElement
        canvas.width = 256
        canvas.height = 128
        val context = canvas.getContext("2d") as CanvasRenderingContext2D

        // Configure text rendering
        context.fillStyle = "#${color.toString(16).padStart(6, '0')}"
        context.font = "bold ${size * 2.5}px Arial"
        context.asDynamic().textAlign = "center"
        context.asDynamic().textBaseline = "middle"

        // Add stroke for better visibility
        context.strokeStyle = "#000000"
        context.lineWidth = 3.0

        // Clear canvas and draw text
        context.clearRect(0.0, 0.0, 256.0, 128.0)

        // Draw text with stroke (outline) first, then fill
        context.strokeText(text, 128.0, 64.0)  // Black outline
        context.fillText(text, 128.0, 64.0)    // White fill

        // Create texture from canvas
        val texture = Texture()
        texture.asDynamic().image = canvas
        texture.needsUpdate = true

        // Create plane geometry
        val geometry = PlaneGeometry(size * 4.0, size * 2.0)

        // Create material
        val material = MeshBasicMaterial(obj {
            map = texture
            transparent = true
            alphaTest = 0.05
        })

        // Force visibility settings
        material.asDynamic().depthTest = false
        material.asDynamic().depthWrite = false

        return Mesh(geometry, material).apply {
            name = "OriginText"
            renderOrder = 10000
            frustumCulled = false

            // Custom property to track base scale for constant screen size
            userData = js("{}")
            userData.asDynamic().baseScale = 1.0
            userData.asDynamic().targetScreenSize = size
        }
    }

    /**
     * Updates the scale of origin label to maintain constant screen size.
     * Should be called when camera position or zoom changes.
     */
    fun updateTextScale(camera: Camera, originGroup: Group) {
        originGroup.children.forEach { child ->
            if (child is Mesh && child.name == "OriginText") {
                updateTextMeshScale(camera, child)
            }
        }
    }

    /**
     * Updates individual text mesh scale based on camera distance.
     */
    private fun updateTextMeshScale(camera: Camera, textMesh: Mesh) {
        val userData = textMesh.userData
        userData.asDynamic().targetScreenSize as? Double ?: TEXT_SIZE

        // Calculate distance from camera to text mesh
        val cameraPosition = camera.position
        val textWorldPosition = Vector3()
        textMesh.asDynamic().getWorldPosition(textWorldPosition)
        val distance = cameraPosition.distanceTo(textWorldPosition)

        // Calculate scale factor to maintain constant screen size
        val baseDistance = 1000.0
        val scaleFactor = (distance / baseDistance).coerceIn(0.1, 10.0)

        // Apply scale to maintain constant apparent size
        textMesh.scale.asDynamic().setScalar(scaleFactor)

        // Ensure text always faces camera (billboard effect)
        textMesh.asDynamic().lookAt(cameraPosition)
    }
}