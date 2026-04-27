package world.phantasmal.web.questEditor.controllers

import world.phantasmal.cell.Cell
import world.phantasmal.cell.cell
import world.phantasmal.cell.flatMap
import world.phantasmal.cell.isNull
import world.phantasmal.cell.map
import world.phantasmal.cell.mutableCell
import world.phantasmal.cell.mutateDeferred
import world.phantasmal.psolib.fileFormats.quest.CHALLENGE_MODE_MONSTER_TYPE_IDS
import world.phantasmal.psolib.fileFormats.quest.DatCmConfigPool
import world.phantasmal.psolib.fileFormats.quest.DatCmConfigPoolEntry
import world.phantasmal.psolib.fileFormats.quest.DatCmMonsterMapping
import world.phantasmal.psolib.fileFormats.quest.DatCmMonsterMappingEntry
import world.phantasmal.psolib.fileFormats.quest.DatCmRandomSpawn
import world.phantasmal.psolib.fileFormats.quest.DatCmRandomSpawnEntry
import world.phantasmal.psolib.fileFormats.quest.getNpcTypeForChallengeMonsterIndex
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.controllers.Tab
import world.phantasmal.webui.controllers.TabContainerController

enum class MonsterRandomnessTab(override val title: String) : Tab {
    MonsterPosition("Monster position"),
    EnemyConfiguration("Enemy configuration"),
}

data class RoomInfo(val globalIndex: Int, val roomId: Int, val entryCount: Int)
data class IndexedSpawnEntry(val index: Int, val entry: DatCmRandomSpawnEntry)
data class IndexedConfigPoolEntry(val index: Int, val entry: DatCmConfigPoolEntry)
data class IndexedMappingEntry(val index: Int, val entry: DatCmMonsterMappingEntry)
data class MonsterTypeOption(val index: Int, val name: String)

class MonsterRandomnessController(
    private val store: QuestEditorStore,
) : TabContainerController<MonsterRandomnessTab>() {

    override val tabs: List<MonsterRandomnessTab> = MonsterRandomnessTab.values().toList()

    private val _activeTab = mutableCell<MonsterRandomnessTab?>(MonsterRandomnessTab.MonsterPosition)
    override val activeTab: Cell<MonsterRandomnessTab?> = _activeTab

    override fun setActiveTab(tab: MonsterRandomnessTab?) {
        _activeTab.value = tab
    }

    val unavailable: Cell<Boolean> = store.currentQuest.isNull()
    val enabled: Cell<Boolean> = store.questEditingEnabled

    /**
     * Returns the set of floor IDs that map to the current area and variant.
     * When the quest has floor mappings, CM data uses floor IDs (not area IDs).
     * When there are no floor mappings, returns a set containing just the area ID.
     */
    private fun floorIdsForCurrentArea(): Set<Int> {
        val quest = store.currentQuest.value ?: return emptySet()
        val area = store.currentArea.value ?: return emptySet()
        val areaVariant = store.currentAreaVariant.value
        return if (quest.floorMappings.isNotEmpty()) {
            quest.floorMappings
                .filter { it.areaId == area.id && (areaVariant == null || it.variantId == areaVariant.id) }
                .map { it.floorId }
                .toSet()
        } else {
            setOf(area.id)
        }
    }

    val monsterTypeOptions: List<MonsterTypeOption> =
        CHALLENGE_MODE_MONSTER_TYPE_IDS.indices
            .filter { CHALLENGE_MODE_MONSTER_TYPE_IDS[it] != 0 }
            .map { idx ->
                val npcType = getNpcTypeForChallengeMonsterIndex(idx)
                val name = npcType?.simpleName ?: "Unknown(0x${CHALLENGE_MODE_MONSTER_TYPE_IDS[idx].toString(16).uppercase()})"
                MonsterTypeOption(idx, name)
            }

    val cmDataRevision: Cell<Int> =
        store.currentQuest.flatMap { it?.cmDataRevision ?: cell(0) }

    // === Monster Position tab ===

    /** Rooms filtered by the current area/variant. */
    val rooms: Cell<List<RoomInfo>> =
        map(store.currentQuest, store.currentArea, store.currentAreaVariant, cmDataRevision) { quest, area, _, _ ->
            if (quest == null || area == null) return@map emptyList()
            val floorIds = floorIdsForCurrentArea()
            quest.cmRandomSpawns.value.mapIndexedNotNull { globalIdx, spawn ->
                if (spawn.areaId in floorIds) RoomInfo(globalIdx, spawn.roomId, spawn.entries.size) else null
            }
        }

    private val _selectedRoomIndex = mutableCell(-1)
    val selectedRoomIndex: Cell<Int> = _selectedRoomIndex

    val selectedRoomEntries: Cell<List<IndexedSpawnEntry>> =
        map(store.currentQuest, rooms, selectedRoomIndex, cmDataRevision) { quest, roomList, selIdx, _ ->
            if (quest == null || selIdx < 0 || selIdx >= roomList.size) return@map emptyList()
            val globalIdx = roomList[selIdx].globalIndex
            quest.cmRandomSpawns.value[globalIdx].entries.mapIndexed { idx, entry ->
                IndexedSpawnEntry(idx, entry)
            }
        }

    // === Enemy Configuration tab ===

    /** Config pool entries (Table 5A) filtered by current area/variant. */
    val configPoolEntries: Cell<List<IndexedConfigPoolEntry>> =
        map(store.currentQuest, store.currentArea, store.currentAreaVariant, cmDataRevision) { quest, area, _, _ ->
            if (quest == null || area == null) return@map emptyList()
            val floorIds = floorIdsForCurrentArea()
            quest.cmConfigPool.value
                .filter { it.areaId in floorIds }
                .flatMap { it.entries }
                .mapIndexed { idx, entry -> IndexedConfigPoolEntry(idx, entry) }
        }

    /** Monster setting entries (Table 5B) filtered by current area/variant. */
    val monsterSettingEntries: Cell<List<IndexedMappingEntry>> =
        map(store.currentQuest, store.currentArea, store.currentAreaVariant, cmDataRevision) { quest, area, _, _ ->
            if (quest == null || area == null) return@map emptyList()
            val floorIds = floorIdsForCurrentArea()
            quest.cmMonsterMappings.value
                .filter { it.areaId in floorIds }
                .flatMap { it.entries }
                .mapIndexed { idx, entry -> IndexedMappingEntry(idx, entry) }
        }

    init {
        // Reset room selection when quest or floor changes.
        observe(store.currentQuest) {
            mutateDeferred { _selectedRoomIndex.value = -1 }
        }
        observe(store.currentArea) {
            mutateDeferred { _selectedRoomIndex.value = -1 }
        }
    }

    // === Monster Position actions ===

    fun selectRoom(index: Int) {
        _selectedRoomIndex.value = index
    }

    /** Resolves the global spawn index from the current room selection. Returns -1 if invalid. */
    private fun selectedGlobalSpawnIndex(): Int {
        val roomList = rooms.value
        val selIdx = _selectedRoomIndex.value
        if (selIdx < 0 || selIdx >= roomList.size) return -1
        return roomList[selIdx].globalIndex
    }

    fun addRoom() {
        val quest = store.currentQuest.value ?: return
        val floorIds = floorIdsForCurrentArea()
        if (floorIds.isEmpty()) return
        // Use the first matching floor ID as the areaId for the new spawn
        val floorId = floorIds.first()
        val areaSpawns = quest.cmRandomSpawns.value.filter { it.areaId in floorIds }
        val newRoomId = if (areaSpawns.isEmpty()) 0 else areaSpawns.maxOf { it.roomId } + 1
        quest.addCmRandomSpawn(DatCmRandomSpawn(floorId, newRoomId, mutableListOf()))
    }

    fun deleteRoom() {
        val quest = store.currentQuest.value ?: return
        val globalIdx = selectedGlobalSpawnIndex()
        if (globalIdx < 0) return
        quest.removeCmRandomSpawn(quest.cmRandomSpawns.value[globalIdx])
        val selIdx = _selectedRoomIndex.value
        _selectedRoomIndex.value = (selIdx - 1).coerceAtLeast(-1)
    }

    fun addSpawnEntry() {
        val quest = store.currentQuest.value ?: return
        val globalIdx = selectedGlobalSpawnIndex()
        if (globalIdx < 0) return
        quest.cmRandomSpawns.value[globalIdx].entries.add(
            DatCmRandomSpawnEntry(
                x = 0f, unknown1 = 0f, y = 0f, unknown2 = 0f,
                rotation = 0, unknown3 = 0, unknown4 = 0,
                sectionId = 0, unknown5 = 0,
            )
        )
        quest.bumpCmRevision()
    }

    fun deleteSpawnEntry(entryIndex: Int) {
        val quest = store.currentQuest.value ?: return
        val globalIdx = selectedGlobalSpawnIndex()
        if (globalIdx < 0) return
        val entries = quest.cmRandomSpawns.value[globalIdx].entries
        if (entryIndex < 0 || entryIndex >= entries.size) return
        entries.removeAt(entryIndex)
        quest.bumpCmRevision()
    }

    fun setSpawnField(entryIndex: Int, setter: (DatCmRandomSpawnEntry) -> Unit) {
        val quest = store.currentQuest.value ?: return
        val globalIdx = selectedGlobalSpawnIndex()
        if (globalIdx < 0) return
        val entries = quest.cmRandomSpawns.value[globalIdx].entries
        if (entryIndex < 0 || entryIndex >= entries.size) return
        setter(entries[entryIndex])
        quest.bumpCmRevision()
    }

    fun setRoomId(globalIndex: Int, roomId: Int) {
        val quest = store.currentQuest.value ?: return
        val spawns = quest.cmRandomSpawns.value
        if (globalIndex < 0 || globalIndex >= spawns.size) return
        spawns[globalIndex].roomId = roomId
        quest.bumpCmRevision()
    }

    // === Enemy Configuration actions ===

    private fun currentConfigPool(): DatCmConfigPool? {
        val quest = store.currentQuest.value ?: return null
        val floorIds = floorIdsForCurrentArea()
        return quest.cmConfigPool.value.find { it.areaId in floorIds }
    }

    private fun currentMonsterMapping(): DatCmMonsterMapping? {
        val quest = store.currentQuest.value ?: return null
        val floorIds = floorIdsForCurrentArea()
        return quest.cmMonsterMappings.value.find { it.areaId in floorIds }
    }

    fun setConfigPoolField(entryIndex: Int, setter: (DatCmConfigPoolEntry) -> Unit) {
        val quest = store.currentQuest.value ?: return
        val pool = currentConfigPool() ?: return
        if (entryIndex < 0 || entryIndex >= pool.entries.size) return
        setter(pool.entries[entryIndex])
        quest.bumpCmRevision()
    }

    fun addConfigPoolEntry() {
        val quest = store.currentQuest.value ?: return
        val floorIds = floorIdsForCurrentArea()
        if (floorIds.isEmpty()) return
        val pool = currentConfigPool() ?: run {
            val newPool = DatCmConfigPool(floorIds.min(), mutableListOf())
            quest.addCmConfigPool(newPool)
            newPool
        }
        val nextConfigId = if (pool.entries.isEmpty()) 1 else pool.entries.maxOf { it.configId } + 1
        pool.entries.add(
            DatCmConfigPoolEntry(
                baseX = 0f, baseZ = 0f, baseY = 0f, unknownFloat = 0f,
                unknownDword = 0, unknownWord1 = 0, unknownWord2 = 0,
                configId = nextConfigId, unknownWord3 = 0, padding = 0,
            )
        )
        quest.bumpCmRevision()
    }

    fun deleteConfigPoolEntry(entryIndex: Int) {
        val quest = store.currentQuest.value ?: return
        val pool = currentConfigPool() ?: return
        if (entryIndex < 0 || entryIndex >= pool.entries.size) return
        pool.entries.removeAt(entryIndex)
        quest.bumpCmRevision()
    }

    fun setMappingField(entryIndex: Int, setter: (DatCmMonsterMappingEntry) -> Unit) {
        val quest = store.currentQuest.value ?: return
        val mapping = currentMonsterMapping() ?: return
        if (entryIndex < 0 || entryIndex >= mapping.entries.size) return
        setter(mapping.entries[entryIndex])
        quest.bumpCmRevision()
    }

    fun addMappingEntry() {
        val quest = store.currentQuest.value ?: return
        val floorIds = floorIdsForCurrentArea()
        if (floorIds.isEmpty()) return
        val mapping = currentMonsterMapping() ?: run {
            val newMapping = DatCmMonsterMapping(floorIds.min(), mutableListOf())
            quest.addCmMonsterMapping(newMapping)
            newMapping
        }
        mapping.entries.add(
            DatCmMonsterMappingEntry(monsterTypeIndex = 0, configId = 0, spawnRatio = 0)
        )
        quest.bumpCmRevision()
    }

    fun deleteMappingEntry(entryIndex: Int) {
        val quest = store.currentQuest.value ?: return
        val mapping = currentMonsterMapping() ?: return
        if (entryIndex < 0 || entryIndex >= mapping.entries.size) return
        mapping.entries.removeAt(entryIndex)
        quest.bumpCmRevision()
    }

    fun focused() {
        store.makeMainUndoCurrent()
    }
}
