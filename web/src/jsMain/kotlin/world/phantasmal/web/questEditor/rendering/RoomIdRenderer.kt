package world.phantasmal.web.questEditor.rendering

import kotlinx.browser.document
import org.khronos.webgl.Float32Array
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.CanvasRenderingContext2D
import world.phantasmal.web.externals.three.*
import world.phantasmal.webui.obj
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * Renders room ID labels as transparent circles with text for quest sections.
 */
class RoomIdRenderer {
    companion object {
        private const val CIRCLE_SEGMENTS = 32  // Segments for circle outline
        private const val CIRCLE_RADIUS = 60.0f  // Circle radius in world units (increased for better visibility)
        private const val CIRCLE_COLOR = 0x4CAF50  // Green color for visibility
        private const val CIRCLE_OPACITY = 0.4  // Slightly more opaque
        private const val TEXT_COLOR = 0xFFFFFF  // White text
        private const val TEXT_SIZE = 32.0  // Text size (significantly increased for better visibility)
    }

    /**
     * Creates a room ID display with transparent circle and text label showing custom text.
     */
    fun createRoomIdLabelWithText(
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        roomIdText: String
    ): Group {
        val group = Group()
        
        // Create transparent circle outline (elevated above ground)
        val circle = createTransparentCircle(CIRCLE_RADIUS, CIRCLE_COLOR, CIRCLE_OPACITY)
        group.add(circle)
        
        // Create text label using a plane with canvas texture
        val textPlane = createTextPlane(roomIdText, TEXT_COLOR, TEXT_SIZE)
        // Position text slightly above the circle for better visibility
        textPlane.position.y = 5.0
        group.add(textPlane)
        
        // Position the entire group at the section center, moderately elevated
        group.position.set(centerX.toDouble(), (centerY + 40.0f).toDouble(), centerZ.toDouble())  // Reasonable height
        group.name = "RoomIdLabel_${roomIdText.hashCode()}"
        
        // Make sure it renders after other geometry and is always visible
        group.renderOrder = 9999  // Higher render order
        
        // Force frustum culling off to always render
        circle.frustumCulled = false
        textPlane.frustumCulled = false
        group.frustumCulled = false
        
        return group
    }

    /**
     * Creates a room ID display with transparent circle and text label showing the room number.
     */
    fun createRoomIdLabel(
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        roomId: Int
    ): Group {
        return createRoomIdLabelWithText(centerX, centerY, centerZ, roomId.toString())
    }
    
    /**
     * Creates a transparent circle using line segments.
     */
    private fun createTransparentCircle(radius: Float, color: Int, opacity: Double): Object3D {
        // Create circle vertices
        val vertices = mutableListOf<Float>()
        for (i in 0..CIRCLE_SEGMENTS) {
            val angle = (i * 2 * PI / CIRCLE_SEGMENTS)
            val x = (cos(angle) * radius).toFloat()
            val z = (sin(angle) * radius).toFloat()
            vertices.add(x)
            vertices.add(0.0f) // Y relative to group position
            vertices.add(z)
        }
        
        val geometry = BufferGeometry().apply {
            setAttribute("position", Float32BufferAttribute(Float32Array(vertices.toTypedArray()), 3))
        }
        
        val material = LineBasicMaterial(obj {
            this.color = Color(color)
            transparent = true
            this.opacity = opacity
            linewidth = 2.0
        })
        
        // Force material settings after creation
        material.asDynamic().depthTest = false
        material.asDynamic().depthWrite = false
        
        return Line(geometry, material).apply {
            name = "RoomCircle"
            // Set high render order for this line specifically
            renderOrder = 10000
            frustumCulled = false
        }
    }
    
    /**
     * Creates a text plane using canvas texture for displaying room ID numbers.
     */
    private fun createTextPlane(text: String, color: Int, size: Double): Mesh {
        // Create canvas for text rendering (larger to avoid text clipping)
        val canvas = document.createElement("CANVAS") as HTMLCanvasElement
        canvas.width = 256  // Increased width
        canvas.height = 128  // Increased height
        val context = canvas.getContext("2d") as CanvasRenderingContext2D
        
        // Configure text rendering with larger font
        context.fillStyle = "#${color.toString(16).padStart(6, '0')}"
        context.font = "bold ${size * 2.5}px Arial"  // Reduced multiplier to fit better in canvas
        context.asDynamic().textAlign = "center"
        context.asDynamic().textBaseline = "middle"
        
        // Add some padding and stroke for better visibility
        context.strokeStyle = "#000000"
        context.lineWidth = 3.0
        
        // Clear canvas and draw text (adjusted for larger canvas)
        context.clearRect(0.0, 0.0, 256.0, 128.0)
        
        // Draw text with stroke (outline) first, then fill
        context.strokeText(text, 128.0, 64.0)  // Black outline
        context.fillText(text, 128.0, 64.0)    // White fill
        
        // Create texture from canvas
        val texture = Texture()
        texture.asDynamic().image = canvas
        texture.needsUpdate = true
        
        // Create plane geometry (larger to ensure text is not clipped)
        val geometry = PlaneGeometry(size * 3.0, size * 1.8)  // Increased size to accommodate larger text
        
        // Create material with aggressive visibility settings
        val material = MeshBasicMaterial(obj {
            map = texture
            transparent = true
            alphaTest = 0.05  // Lower alpha test threshold
        })
        
        // Force material settings for absolute visibility
        material.asDynamic().depthTest = false
        material.asDynamic().depthWrite = false
        
        return Mesh(geometry, material).apply {
            name = "RoomText"
            // Make text always face the camera (billboard effect)
            // Don't set lookAt here, it will be handled dynamically
            
            // Set high render order for this mesh specifically
            renderOrder = 10000
            
            // Additional settings to ensure visibility
            frustumCulled = false
        }
    }
}