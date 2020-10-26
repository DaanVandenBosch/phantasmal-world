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
    var id: UInt,
    var sectionId: UShort,
    var wave: UShort,
    var delay: UShort,
    val actions: MutableList<DatEventAction>,
    val areaId: Int,
    val unknown: UShort,
)

sealed class DatEventAction {
    class SpawnNpcs(
        val sectionId: UShort,
        val appearFlag: UShort,
    ) : DatEventAction()

    class Unlock(
        val doorId: UShort,
    ) : DatEventAction()

    class Lock(
        val doorId: UShort,
    ) : DatEventAction()

    class TriggerEvent(
        val eventId: UInt,
    ) : DatEventAction()
}

class DatUnknown(
    val entityType: Int,
    val totalSize: Int,
    val areaId: Int,
    val entitiesSize: Int,
    val data: UByteArray,
)

fun parseDat(cursor: Cursor): DatFile {
    val objs = mutableListOf<DatEntity>()
    val npcs = mutableListOf<DatEntity>()
    val events = mutableListOf<DatEvent>()
    val unknowns = mutableListOf<DatUnknown>()

    while (cursor.hasBytesLeft()) {
        val entityType = cursor.i32()
        val totalSize = cursor.i32()
        val areaId = cursor.i32()
        val entitiesSize = cursor.i32()

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
                        data = cursor.u8Array(entitiesSize),
                    ))
                }
            }

            require(!entitiesCursor.hasBytesLeft()) {
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
    val actionsOffset = cursor.i32()
    cursor.seek(4) // Always 0x10
    val eventCount = cursor.i32()
    cursor.seek(3) // Always 0
    val eventType = cursor.u8()

    require(eventType != (0x32u).toUByte()) {
        "Can't parse challenge mode quests yet."
    }

    cursor.seekStart(actionsOffset)
    val actionsCursor = cursor.take(cursor.bytesLeft)
    cursor.seekStart(16)

    repeat(eventCount) {
        val id = cursor.u32()
        cursor.seek(4) // Always 0x100
        val sectionId = cursor.u16()
        val wave = cursor.u16()
        val delay = cursor.u16()
        val unknown = cursor.u16() // "wavesetting"?
        val eventActionsOffset = cursor.i32()

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

    var lastU8: UByte = 0xffu

    while (actionsCursor.hasBytesLeft()) {
        lastU8 = actionsCursor.u8()

        if (lastU8 != (0xffu).toUByte()) {
            break
        }
    }

    if (lastU8 != (0xffu).toUByte()) {
        actionsCursor.seek(-1)
    }

    // Make sure the cursor position represents the amount of bytes we've consumed.
    cursor.seekStart(actionsOffset + actionsCursor.position)
}

private fun parseEventActions(cursor: Cursor): MutableList<DatEventAction> {
    val actions = mutableListOf<DatEventAction>()

    outer@ while (cursor.hasBytesLeft()) {
        when (val type = cursor.u8().toInt()) {
            1 -> break@outer

            EVENT_ACTION_SPAWN_NPCS ->
                actions.add(DatEventAction.SpawnNpcs(
                    sectionId = cursor.u16(),
                    appearFlag = cursor.u16(),
                ))

            EVENT_ACTION_UNLOCK ->
                actions.add(DatEventAction.Unlock(
                    doorId = cursor.u16(),
                ))

            EVENT_ACTION_LOCK ->
                actions.add(DatEventAction.Lock(
                    doorId = cursor.u16(),
                ))

            EVENT_ACTION_TRIGGER_EVENT ->
                actions.add(DatEventAction.TriggerEvent(
                    eventId = cursor.u32(),
                ))

            else -> {
                logger.warn { "Unexpected event action type ${type}." }
                break@outer
            }
        }
    }

    return actions
}
