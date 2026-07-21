package world.phantasmal.web.questEditor.rendering

import kotlinx.browser.document
import org.khronos.webgl.Float32Array
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.web.externals.three.*
import world.phantasmal.webui.obj

/**
 * Renders section ID labels as transparent circles with text for quest sections.
 */
class SectionIdRenderer {
    companion object {
        private const val CIRCLE_SEGMENTS = 32  // Segments for circle outline
        private const val CIRCLE_RADIUS = 60.0f  // Circle radius in world units (increased for better visibility)
        private const val CIRCLE_COLOR = 0x4CAF50  // Green color for visibility
        private const val CIRCLE_OPACITY = 0.4  // Slightly more opaque
        private const val SELECTED_CIRCLE_COLOR = 0x00FFFF  // Bright cyan color for selected section
        private const val SELECTED_CIRCLE_OPACITY = 0.8  // High opacity for selected
        private const val SELECTED_CIRCLE_RADIUS = 80.0f  // Slightly larger radius for selected
        private const val TEXT_COLOR = 0xFFFFFF  // White text
        private const val SELECTED_TEXT_COLOR = 0xFFFFFF  // White text for selected (bold and clear)
        private const val TEXT_SIZE = 32.0  // Text size (significantly increased for better visibility)
        private const val SELECTED_TEXT_SIZE = 38.0  // Slightly larger text for selected
        private const val PLAYBACK_LABEL_TEXT_SIZE = 28.0  // Playback action label text size
        private const val PLAYBACK_LABEL_HEIGHT_OFFSET = 60.0f  // Height above entity for playback labels
        private const val DOOR_LABEL_HEIGHT_OFFSET = 30.0f  // Height above door for persistent Door ID label
        const val DOOR_ID_COLOR = 0x00E5FF  // Bright cyan
        const val UNLOCK_COLOR = 0x00E5FF  // Bright cyan (unified style)
        const val LOCK_COLOR = 0x00E5FF  // Bright cyan (unified style)
        const val SPAWN_COLOR = 0x00E5FF  // Bright cyan (unified style)
    }

    /**
     * Creates a section ID display with transparent circle and text label showing custom text.
     */
    fun createSectionIdLabelWithText(
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        sectionIdText: String
    ): Group {
        val group = Group()

        // Create transparent circle outline (elevated above ground)
        val circle = createTransparentCircle(CIRCLE_RADIUS, CIRCLE_COLOR, CIRCLE_OPACITY)
        group.add(circle)

        // Create text label using a plane with canvas texture
        val textPlane = createTextPlane(sectionIdText, TEXT_COLOR, TEXT_SIZE)
        // Position text slightly above the circle for better visibility
        textPlane.position.y = 5.0
        group.add(textPlane)

        // Position the entire group at the section center, moderately elevated
        group.position.set(centerX.toDouble(), (centerY + 40.0f).toDouble(), centerZ.toDouble())  // Reasonable height
        group.name = "SectionIdLabel_${sectionIdText.hashCode()}"

        // Make sure it renders after other geometry and is always visible
        group.renderOrder = 9999  // Higher render order

        // Force frustum culling off to always render
        circle.frustumCulled = false
        textPlane.frustumCulled = false
        group.frustumCulled = false

        return group
    }

    /**
     * Creates a section ID display with transparent circle and text label showing the section number.
     */
    fun createSectionIdLabel(
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        sectionId: Int
    ): Group {
        return createSectionIdLabelWithText(centerX, centerY, centerZ, sectionId.toString())
    }

    /**
     * Creates a selected section ID display with distinctive visual effects.
     */
    fun createSelectedSectionIdLabel(
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        sectionId: Int
    ): Group {
        val group = Group()

        // Create larger, more prominent circle for selected section
        val circle = createTransparentCircle(SELECTED_CIRCLE_RADIUS, SELECTED_CIRCLE_COLOR, SELECTED_CIRCLE_OPACITY)
        group.add(circle)

        // Create highlighted text label
        val textPlane = createTextPlane(sectionId.toString(), SELECTED_TEXT_COLOR, SELECTED_TEXT_SIZE)
        // Position text slightly above the circle for better visibility
        textPlane.position.y = 5.0
        group.add(textPlane)

        // Position the entire group at the section center, moderately elevated
        group.position.set(centerX.toDouble(), (centerY + 40.0f).toDouble(), centerZ.toDouble())  // Reasonable height
        group.name = "SelectedSectionIdLabel_${sectionId.hashCode()}"

        // Make sure it renders after other geometry and is always visible
        group.renderOrder = 9999  // Higher render order

        // Force frustum culling off to always render
        circle.frustumCulled = false
        textPlane.frustumCulled = false
        group.frustumCulled = false

        return group
    }

    private fun createTransparentCircle(radius: Float, color: Int, opacity: Double): Object3D {
        val ringThickness = 2.0f
        val geometry = createRingGeometry(
            innerRadius = radius - ringThickness,
            outerRadius = radius + ringThickness,
            segments = CIRCLE_SEGMENTS,
        )

        val material = MeshBasicMaterial(obj {
            this.color = Color(color)
            transparent = true
            this.opacity = opacity
            side = DoubleSide
        })
        material.asDynamic().depthTest = false
        material.asDynamic().depthWrite = false

        return Mesh(geometry, material).apply {
            name = "SectionCircle"
            renderOrder = 10000
            frustumCulled = false
        }
    }

    /**
     * Creates a text plane using canvas texture for displaying section ID numbers.
     * The text will maintain constant screen size regardless of camera distance.
     */
    private fun createTextPlane(text: String, color: Int, size: Double): Mesh {
        // Create canvas for text rendering (larger to avoid text clipping)
        val canvas = document.createElement("CANVAS") as HTMLCanvasElement
        canvas.width = 256  // Increased width
        canvas.height = 128  // Increased height
        val context = canvas.getContext("2d") as CanvasRenderingContext2D

        // Configure text rendering with larger font and extra bold for selected sections
        context.fillStyle = "#${color.toString(16).padStart(6, '0')}"
        val fontWeight = if (color == SELECTED_TEXT_COLOR) "900" else "bold"  // Extra bold for selected
        context.font = "$fontWeight ${size * 2.5}px Arial"  // Reduced multiplier to fit better in canvas
        context.asDynamic().textAlign = "center"
        context.asDynamic().textBaseline = "middle"

        // Add thicker stroke for selected sections
        context.strokeStyle = "#000000"
        context.lineWidth = if (color == SELECTED_TEXT_COLOR) 4.0 else 3.0

        // Clear canvas and draw text (adjusted for larger canvas)
        context.clearRect(0.0, 0.0, 256.0, 128.0)

        // Draw text with stroke (outline) first, then fill
        context.strokeText(text, 128.0, 64.0)  // Black outline
        context.fillText(text, 128.0, 64.0)    // White fill

        // Create texture from canvas
        val texture = Texture()
        texture.asDynamic().image = canvas
        texture.needsUpdate = true

        // Create plane geometry with a base size that will be scaled later
        val geometry = PlaneGeometry(size * 2.0, size * 1.2)  // Base size for scaling

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
            name = "SectionText"

            // Set high render order for this mesh specifically
            renderOrder = 10000

            // Additional settings to ensure visibility
            frustumCulled = false

            // Custom property to track base scale for constant screen size
            userData = js("{}")
            userData.asDynamic().baseScale = 1.0
            userData.asDynamic().targetScreenSize = size // Target screen size in pixels
        }
    }

    /**
     * Creates a playback action label (for door unlock/lock/spawn events) at the given position.
     * Uses a wider canvas than section-ID labels because the text is longer (e.g. "Unlock #5").
     */
    fun createPlaybackActionLabel(
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        text: String,
        color: Int,
    ): Group {
        val group = Group()

        val textPlane = createWideTextPlane(text, color)
        textPlane.position.y = 5.0
        group.add(textPlane)

        // Position above the entity
        group.position.set(
            centerX.toDouble(),
            (centerY + PLAYBACK_LABEL_HEIGHT_OFFSET).toDouble(),
            centerZ.toDouble(),
        )
        group.name = "PlaybackActionLabel_${text.hashCode()}"

        group.renderOrder = 10001
        textPlane.frustumCulled = false
        group.frustumCulled = false

        return group
    }

    /**
     * Creates a label above a door object.  Callers supply the display text
     * ("Door 5", "Unlock 5", "Lock 5") and colour so the same helper serves
     * both the persistent annotation and the playback highlight.
     */
    fun createDoorLabel(
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        text: String,
        color: Int,
    ): Group {
        val group = Group()

        val textPlane = createWideTextPlane(text, color)
        textPlane.position.y = 5.0
        group.add(textPlane)

        group.position.set(
            centerX.toDouble(),
            (centerY + DOOR_LABEL_HEIGHT_OFFSET).toDouble(),
            centerZ.toDouble(),
        )
        group.name = "DoorLabel_${text.hashCode()}"

        group.renderOrder = 9998
        textPlane.frustumCulled = false
        group.frustumCulled = false

        return group
    }

    /**
     * Creates a text plane with a wider canvas suitable for multi-character labels
     * like "Unlock #5" or "Door 12".
     */
    private fun createWideTextPlane(text: String, color: Int): Mesh {
        val canvasWidth = 512
        val canvasHeight = 128
        val fontSize = 52.0

        val canvas = document.createElement("CANVAS") as HTMLCanvasElement
        canvas.width = canvasWidth
        canvas.height = canvasHeight
        val context = canvas.getContext("2d") as CanvasRenderingContext2D

        context.clearRect(0.0, 0.0, canvasWidth.toDouble(), canvasHeight.toDouble())

        // Dark rounded-rect background for contrast
        context.font = "bold ${fontSize}px Arial"
        val textWidth = context.measureText(text).width
        val padX = 18.0
        val padY = 10.0
        val bgW = textWidth + padX * 2
        val bgH = fontSize + padY * 2
        val bgX = (canvasWidth - bgW) / 2.0
        val bgY = (canvasHeight - bgH) / 2.0
        val r = 14.0
        context.beginPath()
        context.asDynamic().roundRect(bgX, bgY, bgW, bgH, r)
        context.fillStyle = "rgba(0,0,0,0.75)"
        context.fill()

        // Text
        context.asDynamic().textAlign = "center"
        context.asDynamic().textBaseline = "middle"
        context.strokeStyle = "#000000"
        context.lineWidth = 5.0
        context.fillStyle = "#${color.toString(16).padStart(6, '0')}"
        context.strokeText(text, canvasWidth / 2.0, canvasHeight / 2.0)
        context.fillText(text, canvasWidth / 2.0, canvasHeight / 2.0)

        val texture = Texture()
        texture.asDynamic().image = canvas
        texture.needsUpdate = true

        val planeWidth = PLAYBACK_LABEL_TEXT_SIZE * 4.0
        val planeHeight = PLAYBACK_LABEL_TEXT_SIZE * 1.2
        val geometry = PlaneGeometry(planeWidth, planeHeight)

        val material = MeshBasicMaterial(obj {
            map = texture
            transparent = true
            alphaTest = 0.05
        })
        material.asDynamic().depthTest = false
        material.asDynamic().depthWrite = false

        return Mesh(geometry, material).apply {
            name = "SectionText"
            renderOrder = 10001
            frustumCulled = false
            userData = js("{}")
            userData.asDynamic().baseScale = 1.0
            userData.asDynamic().targetScreenSize = PLAYBACK_LABEL_TEXT_SIZE
        }
    }

    /**
     * Creates SCL_TAMA visualization with circle, one radius line and value display.
     * Only shown when ObjSectionID object is selected.
     * Positioned close to ground level like EventCollision range circles.
     */
    fun createSclTamaVisualization(
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        sclTamaValue: Float
    ): Group {
        val group = Group()
        val calculatedRadius = sclTamaValue * 10.0f

        // Create transparent circle outline at ground level
        val circle = createGroundLevelCircle(calculatedRadius, CIRCLE_COLOR, CIRCLE_OPACITY)
        group.add(circle)

        // Create single radius line at ground level
        val radiusLine = createGroundLevelRadiusLine(calculatedRadius)
        group.add(radiusLine)

        // Create value text at the end of the line, slightly elevated above ground
        val valueText = createSimpleValueText(calculatedRadius)
        valueText.position.set(calculatedRadius.toDouble(), 2.0, 0.0) // Slightly above ground
        group.add(valueText)

        // Position the entire group at entity position with fixed height offset like sectionId labels
        val elevatedY = centerY + 15.0f // Same offset as range circles for consistency
        group.position.set(centerX.toDouble(), elevatedY.toDouble(), centerZ.toDouble())
        group.name = "SclTamaVisualization_${sclTamaValue.hashCode()}"

        // Rendering settings
        group.renderOrder = 9999
        group.frustumCulled = false

        return group
    }

    /**
     * Creates simple value text display (just the number).
     */
    private fun createSimpleValueText(radiusValue: Float): Mesh {
        val valueText = "${(radiusValue * 10).toInt() / 10.0f}" // Just the number, no "R:" prefix
        return createTextPlane(valueText, 0xFFFF00, (TEXT_SIZE * 0.8f).toDouble()).apply {
            name = "SclTamaSimpleText"
            frustumCulled = false
        }
    }

    /**
     * Creates a transparent circle at ground level using thick cylinder geometry for better visibility.
     */
    private fun createGroundLevelCircle(radius: Float, color: Int, opacity: Double): Object3D {
        // Create a thick ring using cylinder geometry
        val ringThickness = 2.0f  // Slightly thinner than section circles for ground level
        val outerRadius = radius + ringThickness
        val height = 1.0f  // Very thin height for ring appearance

        val geometry =
            CylinderGeometry(outerRadius.toDouble(), outerRadius.toDouble(), height.toDouble(), CIRCLE_SEGMENTS)

        val material = MeshBasicMaterial(obj {
            this.color = Color(color)
            transparent = true
            this.opacity = opacity
            side = DoubleSide  // Use proper DoubleSide constant
        })

        // Force material settings after creation
        material.asDynamic().depthTest = false
        material.asDynamic().depthWrite = false

        return Mesh(geometry, material).apply {
            name = "SclTamaGroundCircle"
            renderOrder = 1000 // Same as EventCollision range circles
            frustumCulled = false
        }
    }

    /**
     * Creates a single radius line at ground level.
     */
    private fun createGroundLevelRadiusLine(radius: Float): LineSegments {
        val points = FloatArray(6) // 1 line * 2 points * 3 coordinates
        var index = 0

        // Single line from center to east at ground level
        points[index++] = 0f; points[index++] = 0f; points[index++] = 0f
        points[index++] = radius; points[index++] = 0f; points[index++] = 0f

        val geometry = BufferGeometry().apply {
            setAttribute("position", Float32BufferAttribute(Float32Array(points.toTypedArray()), 3))
        }

        val material = LineBasicMaterial(obj {
            color = Color(1.0, 0.8, 0.0) // Golden color for radius line
            transparent = true
            opacity = 0.8
            linewidth = 3.0 // Thicker line for better visibility
        })

        return LineSegments(geometry, material).apply {
            name = "SclTamaGroundRadiusLine"
            renderOrder = 1000 // Same as EventCollision range circles
            frustumCulled = false
        }
    }

    /**
     * Updates the scale of section ID labels to maintain constant screen size.
     * Should be called when camera position or zoom changes.
     */
    fun updateTextScales(camera: Camera, sectionIdLabels: Collection<Group>) {
        sectionIdLabels.forEach { group ->
            group.children.forEach { child ->
                if (child is Mesh && child.name == "SectionText") {
                    updateTextMeshScale(camera, child)
                }
            }
        }
    }

    private fun updateTextMeshScale(camera: Camera, textMesh: Mesh) {
        updateBillboardScale(camera, textMesh)
    }
}