package world.phantasmal.web.questEditor.widgets

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.DragEvent
import org.w3c.dom.HTMLElement
import org.w3c.dom.Image
import org.w3c.dom.events.Event
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.TrackedDisposable
import world.phantasmal.psolib.fileFormats.quest.EntityType
import world.phantasmal.web.externals.three.Vector2
import world.phantasmal.webui.dom.disposableListener
import world.phantasmal.webui.dom.getRoot

private const val DATA_TYPE_PREFIX = "phantasmal-world-id-"

private val eventData: MutableMap<String, EventData> = mutableMapOf()
private var nextEventId = 0
private var dragSources = 0

// Store a references to dragEnd and dragOver because because KJS generates a new object every time
// you use :: at the moment. So e.g. ::dragEnd != ::dragEnd.
@Suppress("UNCHECKED_CAST")
private val dragEndReference: (Event) -> Unit = ::dragEnd as (Event) -> Unit

@Suppress("UNCHECKED_CAST")
private val dragOverReference: (Event) -> Unit = ::dragOver as (Event) -> Unit

class EntityDragEvent(private val data: EventData, private val event: DragEvent) {
    val entityType: EntityType = data.entityType
    val clientX = event.clientX
    val clientY = event.clientY
    val ctrlKey = event.ctrlKey || event.metaKey  // Support both Ctrl (Windows/Linux) and Cmd (macOS)
    val shiftKey = event.shiftKey

    fun allowDrop() {
        event.stopPropagation()
        event.preventDefault()
        event.dataTransfer?.dropEffect = "copy"
    }

    fun showDragElement() {
        data.dragElement.hidden = false
    }

    fun hideDragElement() {
        data.dragElement.hidden = true
    }
}

fun HTMLElement.entityDndSource(entityType: EntityType, imageUrl: String): Disposable =
    disposableListener("dragstart", { e: DragEvent ->
        dragStart(e, entityType, imageUrl)
    })

fun HTMLElement.observeEntityDragEnter(observer: (EntityDragEvent) -> Unit): Disposable =
    observeEntityEvent("dragenter", observer)

fun HTMLElement.observeEntityDragOver(observer: (EntityDragEvent) -> Unit): Disposable =
    observeEntityEvent("dragover", observer)

fun HTMLElement.observeEntityDragLeave(observer: (EntityDragEvent) -> Unit): Disposable =
    observeEntityEvent("dragleave", observer)

fun HTMLElement.observeEntityDrop(observer: (EntityDragEvent) -> Unit): Disposable =
    observeEntityEvent("drop", observer)

/**
 * Shouldn't be used outside of this file.
 */
class EventData(
    val id: String,
    val entityType: EntityType,
    imageUrl: String,
    val position: Vector2,
    private val grabPoint: Vector2,
) : TrackedDisposable() {
    val dragElement = Image(100, 100)

    init {
        dragElement.src = imageUrl
        dragElement.style.position = "fixed"
        (dragElement.style.asDynamic()).pointerEvents = "none"
        dragElement.style.zIndex = "500"
        dragElement.style.top = "0"
        dragElement.style.left = "0"

        updateTransform()

        getRoot().append(dragElement)
    }

    fun setPosition(x: Int, y: Int) {
        position.set(x.toDouble(), y.toDouble())
        updateTransform()
    }

    private fun updateTransform() {
        dragElement.style.transform =
            "translate(${position.x - grabPoint.x}px, ${position.y - grabPoint.y}px)"
    }

    override fun dispose() {
        dragElement.remove()
        super.dispose()
    }
}

private fun HTMLElement.observeEntityEvent(
    type: String,
    observer: (EntityDragEvent) -> Unit,
): Disposable =
    disposableListener(type, { e: DragEvent ->
        getEventData(e)?.let { data ->
            observer(EntityDragEvent(data, e))
        }

        focus()
    })

private fun dragStart(e: DragEvent, entityType: EntityType, imageUrl: String) {
    val dataTransfer = e.dataTransfer

    if (dataTransfer == null) {
        e.preventDefault()
        return
    }

    val eventId = (nextEventId++).toString()
    val position = Vector2(e.clientX.toDouble(), e.clientY.toDouble())
    val grabPoint = Vector2(e.offsetX, e.offsetY)

    eventData[eventId] = EventData(eventId, entityType, imageUrl, position, grabPoint)

    dataTransfer.effectAllowed = "copy"
    dataTransfer.setDragImage(document.createElement("div"), 0, 0)
    dataTransfer.setData(DATA_TYPE_PREFIX + eventId, eventId)
    dataTransfer.setData("text/plain", entityType.simpleName)

    if (++dragSources == 1) {
        window.addEventListener("dragover", dragOverReference)
        window.addEventListener("dragend", dragEndReference)
    }
}

private fun dragOver(e: DragEvent) {
    getEventData(e)?.setPosition(e.clientX, e.clientY)
}

private fun dragEnd(e: DragEvent) {
    if (--dragSources == 0) {
        window.removeEventListener("dragover", dragOverReference)
        window.removeEventListener("dragend", dragEndReference)
    }

    getEventData(e)?.let { data ->
        eventData.remove(data.id)
        data.dispose()
    }
}

private fun getEventData(e: DragEvent): EventData? {
    val pos = Vector2(e.clientX.toDouble(), e.clientY.toDouble())
    var data: EventData? = null

    if (e.type == "dragend") {
        // In this case, e.dataTransfer.types will be empty and we can't retrieve the id anymore.
        var closestDist = Double.POSITIVE_INFINITY

        for (d in eventData.values) {
            val dist = d.position.distanceTo(pos)

            if (dist < closestDist) {
                closestDist = dist
                data = d
            }
        }
    } else {
        data = getEventId(e)?.let { eventData[it] }
    }

    // Position is 0,0 in the last dragleave event before dragend.
    if (e.type != "dragleave") {
        data?.position?.copy(pos)
    }

    return data
}

private fun getEventId(e: DragEvent): String? =
    e.dataTransfer
        ?.types
        ?.find { it.startsWith(DATA_TYPE_PREFIX) }
        ?.drop(DATA_TYPE_PREFIX.length)
