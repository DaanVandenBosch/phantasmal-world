package world.phantasmal.lib.fileFormats.quest

import mu.KotlinLogging
import world.phantasmal.lib.Endianness
import world.phantasmal.lib.buffer.Buffer
import world.phantasmal.lib.cursor.Cursor
import world.phantasmal.lib.cursor.WritableCursor
import world.phantasmal.lib.cursor.cursor
import kotlin.math.max

private val logger = KotlinLogging.logger {}

private const val EVENT_ACTION_SPAWN_NPCS: Byte = 0x8
private const val EVENT_ACTION_UNLOCK: Byte = 0xA
private const val EVENT_ACTION_LOCK: Byte = 0xB
private const val EVENT_ACTION_TRIGGER_EVENT: Byte = 0xC

const val OBJECT_BYTE_SIZE = 68
const val NPC_BYTE_SIZE = 72

class DatFile(
    val objs: MutableList<DatEntity>,
    val npcs: MutableList<DatEntity>,
    val events: MutableList<DatEvent>,
    val unknowns: MutableList<DatUnknown>,
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
    var areaId: Int,
    var unknown: Short,
)

sealed class DatEventAction {
    class SpawnNpcs(
        var sectionId: Short,
        var appearFlag: Short,
    ) : DatEventAction()

    class Unlock(
        var doorId: Short,
    ) : DatEventAction()

    class Lock(
        var doorId: Short,
    ) : DatEventAction()

    class TriggerEvent(
        var eventId: Int,
    ) : DatEventAction()
}

class DatUnknown(
    var entityType: Int,
    var totalSize: Int,
    var areaId: Int,
    var entitiesSize: Int,
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
        when (val type = cursor.byte()) {
            (1).toByte() -> break@outer

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

fun writeDat(dat: DatFile): Buffer {
    val buffer = Buffer.withCapacity(
        dat.objs.size * (16 + OBJECT_BYTE_SIZE) +
                dat.npcs.size * (16 + NPC_BYTE_SIZE) +
                dat.unknowns.sumBy { it.totalSize },
        endianness = Endianness.Little,
    )
    val cursor = buffer.cursor()

    writeEntities(cursor, dat.objs, 1, OBJECT_BYTE_SIZE)
    writeEntities(cursor, dat.npcs, 2, NPC_BYTE_SIZE)
    writeEvents(cursor, dat.events)

    for (unknown in dat.unknowns) {
        cursor.writeInt(unknown.entityType)
        cursor.writeInt(unknown.totalSize)
        cursor.writeInt(unknown.areaId)
        cursor.writeInt(unknown.entitiesSize)
        cursor.writeByteArray(unknown.data)
    }

    // Final header.
    cursor.writeInt(0)
    cursor.writeInt(0)
    cursor.writeInt(0)
    cursor.writeInt(0)

    return buffer
}

private fun writeEntities(
    cursor: WritableCursor,
    entities: List<DatEntity>,
    entityType: Int,
    entitySize: Int,
) {
    val groupedEntities = entities.groupBy { it.areaId }

    for ((areaId, areaEntities) in groupedEntities.entries) {
        val entitiesSize = areaEntities.size * entitySize
        cursor.writeInt(entityType)
        cursor.writeInt(16 + entitiesSize)
        cursor.writeInt(areaId)
        cursor.writeInt(entitiesSize)
        val startPos = cursor.position

        for (entity in areaEntities) {
            require(entity.data.size == entitySize) {
                "Malformed entity in area $areaId, data buffer was of size ${
                    entity.data.size
                } instead of expected $entitySize."
            }

            cursor.writeCursor(entity.data.cursor())
        }

        check(cursor.position == startPos + entitiesSize) {
            "Wrote ${
                cursor.position - startPos
            } bytes of entity data instead of expected $entitiesSize bytes for area $areaId."
        }
    }
}

private fun writeEvents(cursor: WritableCursor, events: List<DatEvent>) {
    val groupedEvents = events.groupBy { it.areaId }

    for ((areaId, areaEvents) in groupedEvents.entries) {
        // Standard header.
        cursor.writeInt(3) // Entity type
        val totalSizeOffset = cursor.position
        cursor.writeInt(0) // Placeholder for the total size.
        cursor.writeInt(areaId)
        val entitiesSizeOffset = cursor.position
        cursor.writeInt(0) // Placeholder for the entities size.

        // Event header.
        val startPos = cursor.position
        // TODO: actual event size is dependent on the event type (challenge mode).
        // Absolute offset.
        val actionsOffset = startPos + 16 + 20 * areaEvents.size
        cursor.size = max(actionsOffset, cursor.size)

        cursor.writeInt(actionsOffset - startPos)
        cursor.writeInt(0x10)
        cursor.writeInt(areaEvents.size)
        cursor.writeInt(0) // TODO: write event type (challenge mode).

        // Relative offset.
        var eventActionsOffset = 0

        for (event in areaEvents) {
            cursor.writeInt(event.id)
            cursor.writeInt(0x10000)
            cursor.writeShort(event.sectionId)
            cursor.writeShort(event.wave)
            cursor.writeShort(event.delay)
            cursor.writeShort(event.unknown)
            cursor.writeInt(eventActionsOffset)
            val nextEventPos = cursor.position

            cursor.seekStart(actionsOffset + eventActionsOffset)

            for (action in event.actions) {
                when (action) {
                    is DatEventAction.SpawnNpcs -> {
                        cursor.writeByte(EVENT_ACTION_SPAWN_NPCS)
                        cursor.writeShort(action.sectionId)
                        cursor.writeShort(action.appearFlag)
                    }
                    is DatEventAction.Unlock -> {
                        cursor.writeByte(EVENT_ACTION_UNLOCK)
                        cursor.writeShort(action.doorId)
                    }
                    is DatEventAction.Lock -> {
                        cursor.writeByte(EVENT_ACTION_LOCK)
                        cursor.writeShort(action.doorId)
                    }
                    is DatEventAction.TriggerEvent -> {
                        cursor.writeByte(EVENT_ACTION_TRIGGER_EVENT)
                        cursor.writeInt(action.eventId)
                    }
                }
            }

            // End of event actions.
            cursor.writeByte(1)

            eventActionsOffset = cursor.position - actionsOffset

            cursor.seekStart(nextEventPos)
        }

        cursor.seekStart(actionsOffset + eventActionsOffset)

        while ((cursor.position - actionsOffset) % 4 != 0) {
            cursor.writeByte(-1)
        }

        val endPos = cursor.position

        cursor.seekStart(totalSizeOffset)
        cursor.writeInt(16 + endPos - startPos)

        cursor.seekStart(entitiesSizeOffset)
        cursor.writeInt(endPos - startPos)

        cursor.seekStart(endPos)
    }
}
