package world.phantasmal.web.questEditor.models

import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.mutableVal

sealed class QuestEventActionModel {
    class SpawnNpcs(sectionId: Int, appearFlag: Int) : QuestEventActionModel() {
        private val _sectionId = mutableVal(sectionId)
        private val _appearFlag = mutableVal(appearFlag)

        val sectionId: Val<Int> = _sectionId
        val appearFlag: Val<Int> = _appearFlag

        fun setSectionId(sectionId: Int) {
            _sectionId.value = sectionId
        }

        fun setAppearFlag(appearFlag: Int) {
            _appearFlag.value = appearFlag
        }
    }

    class Unlock(doorId: Int) : QuestEventActionModel() {
        private val _doorId = mutableVal(doorId)

        val doorId: Val<Int> = _doorId

        fun setDoorId(doorId: Int) {
            _doorId.value = doorId
        }
    }

    class Lock(doorId: Int) : QuestEventActionModel() {
        private val _doorId = mutableVal(doorId)

        val doorId: Val<Int> = _doorId

        fun setDoorId(doorId: Int) {
            _doorId.value = doorId
        }
    }

    class TriggerEvent(eventId: Int) : QuestEventActionModel() {
        private val _eventId = mutableVal(eventId)

        val eventId: Val<Int> = _eventId

        fun setEventId(eventId: Int) {
            _eventId.value = eventId
        }
    }
}
