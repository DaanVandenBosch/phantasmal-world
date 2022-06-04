package world.phantasmal.web.questEditor.models

import world.phantasmal.cell.Cell
import world.phantasmal.cell.mutableCell

sealed class QuestEventActionModel {
    abstract val shortName: String

    class SpawnNpcs(sectionId: Int, appearFlag: Int) : QuestEventActionModel() {
        private val _sectionId = mutableCell(sectionId)
        private val _appearFlag = mutableCell(appearFlag)

        override val shortName = SHORT_NAME
        val sectionId: Cell<Int> = _sectionId
        val appearFlag: Cell<Int> = _appearFlag

        fun setSectionId(sectionId: Int) {
            _sectionId.value = sectionId
        }

        fun setAppearFlag(appearFlag: Int) {
            _appearFlag.value = appearFlag
        }

        companion object {
            const val SHORT_NAME = "Spawn"
        }
    }

    sealed class Door(doorId: Int) : QuestEventActionModel() {
        private val _doorId = mutableCell(doorId)

        val doorId: Cell<Int> = _doorId

        fun setDoorId(doorId: Int) {
            _doorId.value = doorId
        }

        class Unlock(doorId: Int) : Door(doorId) {
            override val shortName = SHORT_NAME

            companion object {
                const val SHORT_NAME = "Unlock"
            }
        }

        class Lock(doorId: Int) : Door(doorId) {
            override val shortName = SHORT_NAME

            companion object {
                const val SHORT_NAME = "Lock"
            }
        }
    }

    class TriggerEvent(eventId: Int) : QuestEventActionModel() {
        private val _eventId = mutableCell(eventId)

        override val shortName = SHORT_NAME
        val eventId: Cell<Int> = _eventId

        fun setEventId(eventId: Int) {
            _eventId.value = eventId
        }

        companion object {
            const val SHORT_NAME = "Event"
        }
    }
}
