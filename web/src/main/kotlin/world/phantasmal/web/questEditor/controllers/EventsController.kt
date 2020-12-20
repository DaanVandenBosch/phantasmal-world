package world.phantasmal.web.questEditor.controllers

import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.eq
import world.phantasmal.observable.value.flatMap
import world.phantasmal.observable.value.list.emptyListVal
import world.phantasmal.web.questEditor.actions.CreateEventAction
import world.phantasmal.web.questEditor.actions.EditPropertyAction
import world.phantasmal.web.questEditor.models.QuestEventModel
import world.phantasmal.web.questEditor.models.WaveModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.controllers.Controller

class EventsController(private val store: QuestEditorStore) : Controller() {
    val unavailable: Val<Boolean> = store.currentQuest.isNull()
    val enabled: Val<Boolean> = store.questEditingEnabled
    val events: Val<List<QuestEventModel>> =
        flatMap(store.currentQuest, store.currentArea) { quest, area ->
            if (quest != null && area != null) {
                quest.events.filtered { it.areaId == area.id }
            } else {
                emptyListVal()
            }
        }

    fun clicked() {
        store.setSelectedWave(null)
    }

    fun focused() {
        store.makeMainUndoCurrent()
    }

    fun addEvent() {
        val quest = store.currentQuest.value
        val area = store.currentArea.value

        if (quest != null && area != null) {
            val selectedWave = store.selectedWave.value
            val index =
                if (selectedWave == null) events.value.size
                else events.value.indexOfFirst { it.wave.value == selectedWave } + 1
            val sectionId = 1

            store.executeAction(
                CreateEventAction(quest, index, QuestEventModel(
                    id = 0,
                    areaId = area.id,
                    sectionId,
                    wave = WaveModel(1, area.id, sectionId),
                    delay = 0,
                    unknown = 0, // TODO: what is a sensible value for event.unknown?
                    actions = mutableListOf(),
                ))
            )
        }
    }

    fun isSelected(event: QuestEventModel): Val<Boolean> =
        store.selectedWave eq event.wave

    fun eventClicked(event: QuestEventModel) {
        store.setSelectedWave(event.wave.value)
    }

    fun setId(event: QuestEventModel, id: Int) {
        store.executeAction(
            EditPropertyAction("Edit event ID", event::setId, id, event.id.value)
        )
    }

    fun setWaveId(event: QuestEventModel, waveId: Int) {
        store.executeAction(
            EditPropertyAction("Edit event wave", event::setWaveId, waveId, event.wave.value.id)
        )
    }

    fun setDelay(event: QuestEventModel, delay: Int) {
        store.executeAction(
            EditPropertyAction("Edit event delay", event::setDelay, delay, event.delay.value)
        )
    }
}
