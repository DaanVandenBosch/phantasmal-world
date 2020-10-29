package world.phantasmal.lib.fileFormats.quest

import mu.KotlinLogging
import world.phantasmal.lib.buffer.Buffer
import world.phantasmal.lib.cursor.Cursor

private val logger = KotlinLogging.logger {}

private const val EVENT_ACTION_SPAWN_NPCS = 0x8
private const val EVENT_ACTION_UNLOCK = 0xA
private const val EVENT_ACTION_LOCK = 0xB
private const val EVENT_ACTION_TRIGGER_EVENT = 0xC

const val OBJECT_BYTE_SIZE = 68
const val NPC_BYTE_SIZE = 72

class DatFile(
    val objs: List<DatEntity>,
    val npcs: List<DatEntity>,
    val events: List<DatEvent>,
    val unknowns: List<DatUnknown>,
)

class DatEntity(
    var areaId: Int,
    val data: Buffer,
)

class DatEvent(
    var id: Int,
    var sectionId: Short,
    var wave: Short,
    var delay: Short,
    val actions: MutableList<DatEventAction>,
    val areaId: Int,
    val unknown: Short,
)

sealed class DatEventAction {
    class SpawnNpcs(
        val sectionId: Short,
        val appearFlag: Short,
    ) : DatEventAction()

    class Unlock(
        val doorId: Short,
    ) : DatEventAction()

    class Lock(
        val doorId: Short,
    ) : DatEventAction()

    class TriggerEvent(
        val eventId: Int,
    ) : DatEventAction()
}

class DatUnknown(
    val entityType: Int,
    val totalSize: Int,
    val areaId: Int,
    val entitiesSize: Int,
    val data: ByteArray,
)

fun parseDat(cursor: Cursor): DatFile {
    val objs = mutableListOf<DatEntity>()
    val npcs = mutableListOf<DatEntity>()
    val events = mutableListOf<DatEvent>()
    val unknowns = mutableListOf<DatUnknown>()

    while (cursor.hasBytesLeft()) {
        val entityType = cursor.int()
        val totalSize = cursor.int()
        val areaId = cursor.int()
        val entitiesSize = cursor.int()

        if (entityType == 0) {
            break
        } else {
            require(entitiesSize == totalSize - 16) {
                "Malformed DAT file. Expected an entities size of ${totalSize - 16}, got ${entitiesSize}."
            }

            val entitiesCursor = cursor.take(entitiesSize)

            when (entityType) {
                1 -> parseEntities(entitiesCursor, areaId, objs, OBJECT_BYTE_SIZE)
                2 -> parseEntities(entitiesCursor, areaId, npcs, NPC_BYTE_SIZE)
                3 -> parseEvents(entitiesCursor, areaId, events)
                else -> {
                    // Unknown entity types 4 and 5 (challenge mode).
                    unknowns.add(DatUnknown(
                        entityType,
                        totalSize,
                        areaId,
                        entitiesSize,
                        data = cursor.byteArray(entitiesSize),
                    ))
                }
            }

            if (entitiesCursor.hasBytesLeft()) {
                logger.warn {
                    "Read ${entitiesCursor.position} bytes instead of expected ${entitiesCursor.size} for entity type ${entityType}."
                }
            }
        }
    }

    return DatFile(
        objs,
        npcs,
        events,
        unknowns
    )
}

private fun parseEntities(
    cursor: Cursor,
    areaId: Int,
    entities: MutableList<DatEntity>,
    entitySize: Int,
) {
    val entityCount = cursor.size / entitySize

    repeat(entityCount) {
        entities.add(DatEntity(
            areaId,
            data = cursor.buffer(entitySize),
        ))
    }
}

private fun parseEvents(cursor: Cursor, areaId: Int, events: MutableList<DatEvent>) {
    val actionsOffset = cursor.int()
    cursor.seek(4) // Always 0x10
    val eventCount = cursor.int()
    cursor.seek(3) // Always 0
    val eventType = cursor.byte()

    require(eventType.toInt() != 0x32) {
        "Can't parse challenge mode quests yet."
    }

    cursor.seekStart(actionsOffset)
    val actionsCursor = cursor.take(cursor.bytesLeft)
    cursor.seekStart(16)

    repeat(eventCount) {
        val id = cursor.int()
        cursor.seek(4) // Always 0x100
        val sectionId = cursor.short()
        val wave = cursor.short()
        val delay = cursor.short()
        val unknown = cursor.short() // "wavesetting"?
        val eventActionsOffset = cursor.int()

        val actions: MutableList<DatEventAction> =
            if (eventActionsOffset < actionsCursor.size) {
                actionsCursor.seekStart(eventActionsOffset)
                parseEventActions(actionsCursor)
            } else {
                logger.warn { "Invalid event actions offset $eventActionsOffset for event ${id}." }
                mutableListOf()
            }

        events.add(DatEvent(
            id,
            sectionId,
            wave,
            delay,
            actions,
            areaId,
            unknown,
        ))
    }

    if (cursor.position != actionsOffset) {
        logger.warn {
            "Read ${cursor.position - 16} bytes of event data instead of expected ${actionsOffset - 16}."
        }
    }

    var lastByte: Byte = -1

    while (actionsCursor.hasBytesLeft()) {
        lastByte = actionsCursor.byte()

        if (lastByte.toInt() != -1) {
            break
        }
    }

    if (lastByte.toInt() != -1) {
        actionsCursor.seek(-1)
    }

    // Make sure the cursor position represents the amount of bytes we've consumed.
    cursor.seekStart(actionsOffset + actionsCursor.position)
}

private fun parseEventActions(cursor: Cursor): MutableList<DatEventAction> {
    val actions = mutableListOf<DatEventAction>()

    outer@ while (cursor.hasBytesLeft()) {
        when (val type = cursor.uByte().toInt()) {
            1 -> break@outer

            EVENT_ACTION_SPAWN_NPCS ->
                actions.add(DatEventAction.SpawnNpcs(
                    sectionId = cursor.short(),
                    appearFlag = cursor.short(),
                ))

            EVENT_ACTION_UNLOCK ->
                actions.add(DatEventAction.Unlock(
                    doorId = cursor.short(),
                ))

            EVENT_ACTION_LOCK ->
                actions.add(DatEventAction.Lock(
                    doorId = cursor.short(),
                ))

            EVENT_ACTION_TRIGGER_EVENT ->
                actions.add(DatEventAction.TriggerEvent(
                    eventId = cursor.int(),
                ))

            else -> {
                logger.warn { "Unexpected event action type ${type}." }
                break@outer
            }
        }
    }

    return actions
}
