package world.phantasmal.web.questEditor.stores

import world.phantasmal.cell.Cell
import world.phantasmal.cell.MutableCell
import world.phantasmal.cell.mutableCell
import world.phantasmal.webui.stores.Store

class PlaybackVisualizationStore : Store() {
    private val _playbackActionText: MutableCell<String> = mutableCell("")
    private val _playbackDoorIds: MutableCell<Set<Int>> = mutableCell(emptySet())
    private val _playbackLockDoorIds: MutableCell<Set<Int>> = mutableCell(emptySet())
    private val _playbackSpawnSectionIds: MutableCell<Set<Int>> = mutableCell(emptySet())

    val playbackActionText: Cell<String> = _playbackActionText
    val playbackDoorIds: Cell<Set<Int>> = _playbackDoorIds
    val playbackLockDoorIds: Cell<Set<Int>> = _playbackLockDoorIds
    val playbackSpawnSectionIds: Cell<Set<Int>> = _playbackSpawnSectionIds

    fun setPlaybackActionText(text: String) {
        _playbackActionText.value = text
    }

    fun setPlaybackDoorIds(doorIds: Set<Int>) {
        _playbackDoorIds.value = doorIds
    }

    fun setPlaybackLockDoorIds(lockDoorIds: Set<Int>) {
        _playbackLockDoorIds.value = lockDoorIds
    }

    fun setPlaybackSpawnSectionIds(sectionIds: Set<Int>) {
        _playbackSpawnSectionIds.value = sectionIds
    }
}
