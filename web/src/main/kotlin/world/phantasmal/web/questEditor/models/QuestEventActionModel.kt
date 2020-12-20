package world.phantasmal.web.questEditor.models

import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.mutableVal

sealed class QuestEventActionModel {
    abstract val shortName: String

    class SpawnNpcs(sectionId: Int, appearFlag: Int) : QuestEventActionModel() {
        private val _sectionId = mutableVal(sectionId)
        private val _appearFlag = mutableVal(appearFlag)

        override val shortName = SHORT_NAME
        val sectionId: Val<Int> = _sectionId
        val appearFlag: Val<Int> = _appearFlag

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
        private val _doorId = mutableVal(doorId)

        val doorId: Val<Int> = _doorId

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
        private val _eventId = mutableVal(eventId)

        override val shortName = SHORT_NAME
        val eventId: Val<Int> = _eventId

        fun setEventId(eventId: Int) {
            _eventId.value = eventId
        }

        companion object {
            const val SHORT_NAME = "Event"
        }
    }
}
