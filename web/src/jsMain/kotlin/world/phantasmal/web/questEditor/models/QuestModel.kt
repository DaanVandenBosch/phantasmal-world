package world.phantasmal.web.questEditor.models

import world.phantasmal.cell.Cell
import world.phantasmal.cell.list.ListCell
import world.phantasmal.cell.list.dependingOnElements
import world.phantasmal.cell.list.mutableListCell
import world.phantasmal.cell.map
import world.phantasmal.cell.MutableCell
import world.phantasmal.cell.mutableCell
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.asm.BytecodeIr
import world.phantasmal.psolib.asm.dataFlowAnalysis.FloorMapping
import world.phantasmal.psolib.asm.dataFlowAnalysis.ParticleSpawn
import world.phantasmal.psolib.fileFormats.quest.BinFormat
import world.phantasmal.psolib.fileFormats.quest.DatCmConfigPool
import world.phantasmal.psolib.fileFormats.quest.DatCmMonsterMapping
import world.phantasmal.psolib.fileFormats.quest.DatCmRandomSpawn
import world.phantasmal.psolib.fileFormats.quest.DatUnknown
import world.phantasmal.psolib.fileFormats.quest.getAreasForEpisode
import world.phantasmal.psolib.fileFormats.quest.getQuestParticleSpawns

class QuestModel(
    id: Int,
    language: Int,
    name: String,
    shortDescription: String,
    longDescription: String,
    val episode: Episode,
    npcs: MutableList<QuestNpcModel>,
    objects: MutableList<QuestObjectModel>,
    events: MutableList<QuestEventModel>,
    /**
     * (Partial) raw DAT data that can't be parsed yet by Phantasmal.
     */
    val datUnknowns: List<DatUnknown>,
    /**
     * Challenge mode random spawn configurations.
     */
    cmRandomSpawns: MutableList<DatCmRandomSpawn>,
    /**
     * Challenge mode monster type mappings (Table 5B).
     */
    cmMonsterMappings: MutableList<DatCmMonsterMapping>,
    /**
     * Challenge mode config pool (Table 5A).
     */
    cmConfigPool: MutableList<DatCmConfigPool>,
    bytecodeIr: BytecodeIr,
    val shopItems: UIntArray,
    floorMappings: List<FloorMapping>,
    private val getVariant: (Episode, areaId: Int, variantId: Int) -> AreaVariantModel?,
    /** Whether DC/GC text fields use Shift-JIS encoding (Japanese). */
    val shiftJis: Boolean = false,
    /** Non-standard bytecode offset preserved from the original BIN, or null for the default. */
    val bytecodeOffset: Int? = null,
    /** Original BIN format from the loaded file, used to restore the correct version on save. */
    val binFormat: BinFormat = BinFormat.BB,
) {
    private val _id = mutableCell(0)
    private val _language = mutableCell(0)
    private val _name = mutableCell("")
    private val _shortDescription = mutableCell("")
    private val _longDescription = mutableCell("")
    private val _npcs = mutableListCell(npcs)
    private val _objects = mutableListCell(objects)
    private val _events = mutableListCell(events)
    private val _cmRandomSpawns = mutableListCell(cmRandomSpawns)
    private val _cmMonsterMappings = mutableListCell(cmMonsterMappings)
    private val _cmConfigPool = mutableListCell(cmConfigPool)
    private val _cmDataRevision: MutableCell<Int> = mutableCell(0)
    private val _floorMappingRevision: MutableCell<Int> = mutableCell(0)
    private val _bytecodeRevision: MutableCell<Int> = mutableCell(0)
    private val _areaVariants = mutableListCell<AreaVariantModel>()

    val id: Cell<Int> = _id
    val language: Cell<Int> = _language
    val name: Cell<String> = _name
    val shortDescription: Cell<String> = _shortDescription
    val longDescription: Cell<String> = _longDescription

    var floorMappings: List<FloorMapping> = floorMappings
        private set

    /**
     * Returns the set of floor IDs that map to the given area+variant, or null if this quest
     * has no floor mappings (i.e., a simple single-floor quest).
     */
    fun getFloorIdsForVariant(areaId: Int, variantId: Int): Set<Int>? {
        if (floorMappings.isEmpty()) return null
        return floorMappings
            .filter { it.areaId == areaId && it.variantId == variantId }
            .map { it.floorId }
            .toSet()
    }

    /**
     * Returns the set of floor IDs that map to the given area (any variant), or null if this
     * quest has no floor mappings.
     */
    fun getFloorIdsForArea(areaId: Int): Set<Int>? {
        if (floorMappings.isEmpty()) return null
        return floorMappings
            .filter { it.areaId == areaId }
            .map { it.floorId }
            .toSet()
    }

    /**
     * Checks whether an entity (identified by its areaId, which is a floor ID in multi-floor
     * quests) belongs to the given area and optional variant.
     *
     * Centralizes the floor-mapping lookup so callers don't need to branch on
     * [floorMappings].isEmpty() themselves.
     */
    fun entityBelongsToArea(entityAreaId: Int, areaId: Int, variantId: Int? = null): Boolean {
        if (floorMappings.isEmpty()) {
            return entityAreaId == areaId
        }
        return floorMappings.any {
            it.floorId == entityAreaId &&
                it.areaId == areaId &&
                (variantId == null || it.variantId == variantId)
        }
    }

    /**
     * Map of area IDs to entity counts.
     */
    val entitiesPerArea: Cell<Map<Int, Int>>

    /**
     * All area variants used by this quest. Multiple variants per area are supported.
     */
    val areaVariants: ListCell<AreaVariantModel> = _areaVariants

    /**
     * Map floor ID to area variant, for regular quests, this is empty.
     */
    var floorToVariantMap: Map<Int, AreaVariantModel>
        private set


    val npcs: ListCell<QuestNpcModel> = _npcs
    val objects: ListCell<QuestObjectModel> = _objects

    val events: ListCell<QuestEventModel> = _events

    val cmRandomSpawns: ListCell<DatCmRandomSpawn> = _cmRandomSpawns
    val cmMonsterMappings: ListCell<DatCmMonsterMapping> = _cmMonsterMappings
    val cmConfigPool: ListCell<DatCmConfigPool> = _cmConfigPool
    val cmDataRevision: Cell<Int> = _cmDataRevision

    var bytecodeIr: BytecodeIr = bytecodeIr
        private set

    /**
     * Ticks on every [setBytecodeIr] call. Downstream views derived from
     * the bytecode (trigger analysis, data-label typing, inline preview,
     * 3D trigger rings) depend on this so they refresh after ASM edits
     * settle into a new bytecodeIr — `currentQuest` only re-emits on
     * quest load, which isn't enough to catch in-place reassembles.
     */
    val bytecodeRevision: Cell<Int> = _bytecodeRevision

    /**
     * Fixed DAT-object and BIN-opcode particles derived from the current editable quest state.
     * Entity property dependencies cover particle IDs and DAT/NPC script entry labels.
     */
    val particleSpawns: Cell<List<ParticleSpawn>> = map(
        _objects.dependingOnElements { obj ->
            obj.properties.value.map { it.value }.toTypedArray()
        },
        _npcs.dependingOnElements { npc ->
            npc.properties.value.map { it.value }.toTypedArray()
        },
        _bytecodeRevision,
        _floorMappingRevision,
    ) { objects, npcs, _, _ ->
        getQuestParticleSpawns(
            bytecodeIr = this.bytecodeIr,
            objects = objects.map { it.entity },
            npcs = npcs.map { it.entity },
        )
    }

    init {
        setId(id)
        setLanguage(language)
        setName(name)
        setShortDescription(shortDescription)
        setLongDescription(longDescription)

        entitiesPerArea = map(this.npcs, this.objects, _floorMappingRevision) { ns, os, _ ->
            val floorToAreaId = this.floorMappings.associate { it.floorId to it.areaId }
            val map = mutableMapOf<Int, Int>()

            for (npc in ns) {
                // Map floor ID to area ID for correct counting
                val areaId = floorToAreaId[npc.areaId] ?: npc.areaId
                map[areaId] = (map[areaId] ?: 0) + 1
            }

            for (obj in os) {
                val areaId = floorToAreaId[obj.areaId] ?: obj.areaId
                map[areaId] = (map[areaId] ?: 0) + 1
            }

            map
        }

        floorToVariantMap = emptyMap()
        rebuildFloorVariants()
    }

    /**
     * Rebuilds the floor-to-variant mapping and the [areaVariants] list.
     *
     * With floorMappings (bb_map_designate quests): each mapping entry defines which
     * AreaVariantModel a floor uses. Multiple floors may map to the same area with different
     * variants (e.g., PW4 Tower).
     *
     * Without floorMappings (regular quests): every area in the episode gets variant 0,
     * ensuring the full floor list is always visible — including areas with 0 entities.
     */
    private fun rebuildFloorVariants() {
        val variants = mutableMapOf<Int, AreaVariantModel>()

        if (floorMappings.isNotEmpty()) {
            for (mapping in floorMappings) {
                getVariant(mapping.mapEpisode ?: episode, mapping.areaId, mapping.variantId)?.let { variant ->
                    variants[mapping.floorId] = variant
                }
            }
        } else {
            // For regular quests (no floor mappings), add variant 0 for every area in the
            // episode so the full floor list is always available, including areas with 0
            // entities. Do NOT filter out empty areas — users expect to see all floors.
            for (area in getAreasForEpisode(episode)) {
                getVariant(episode, area.id, 0)?.let { variant ->
                    variants[area.id] = variant
                }
            }
        }

        floorToVariantMap = if (floorMappings.isNotEmpty()) {
            variants.toMap()
        } else {
            emptyMap()
        }

        _areaVariants.replaceAll(variants.values.distinct())
    }

    fun setId(id: Int): QuestModel {
        require(id >= 0) { "id should be greater than or equal to 0, was ${id}." }

        _id.value = id
        return this
    }

    fun setLanguage(language: Int): QuestModel {
        require(language >= 0) { "language should be greater than or equal to 0, was ${language}." }

        _language.value = language
        return this
    }

    fun setName(name: String): QuestModel {
        require(name.length <= 32) { """name can't be longer than 32 characters, got "$name".""" }

        _name.value = name
        return this
    }

    fun setShortDescription(shortDescription: String): QuestModel {
        require(shortDescription.length <= 128) {
            """shortDescription can't be longer than 128 characters, got "$shortDescription"."""
        }

        _shortDescription.value = shortDescription
        return this
    }

    fun setLongDescription(longDescription: String): QuestModel {
        require(longDescription.length <= 288) {
            """longDescription can't be longer than 288 characters, got "$longDescription"."""
        }

        _longDescription.value = longDescription
        return this
    }

    fun addEntity(entity: QuestEntityModel<*, *>) {
        when (entity) {
            is QuestNpcModel -> addNpc(entity)
            is QuestObjectModel -> addObject(entity)
        }
    }

    fun setFloorMappings(floorMappings: List<FloorMapping>) {
        this.floorMappings = floorMappings
        // Re-propagate gameAreaId on all NPCs so NPC type detection stays accurate.
        val floorToAreaId = floorMappings.associate { it.floorId to it.areaId }
        _npcs.value.forEach { npcModel ->
            npcModel.entity.gameAreaId = floorToAreaId[npcModel.entity.areaId] ?: npcModel.entity.areaId
        }
        rebuildFloorVariants()
        _floorMappingRevision.value++
    }

    fun addNpc(npc: QuestNpcModel) {
        _npcs.add(npc)
        // Keep areaVariants in sync when a new area is introduced (no floor mappings path).
        if (floorMappings.isEmpty()) rebuildFloorVariants()
    }

    fun addObject(obj: QuestObjectModel) {
        _objects.add(obj)
        if (floorMappings.isEmpty()) rebuildFloorVariants()
    }

    fun removeEntity(entity: QuestEntityModel<*, *>) {
        when (entity) {
            is QuestNpcModel -> _npcs.remove(entity)
            is QuestObjectModel -> _objects.remove(entity)
        }
        if (floorMappings.isEmpty()) rebuildFloorVariants()
    }

    fun addEvent(index: Int, event: QuestEventModel) {
        _events.add(index, event)
    }

    fun removeEvent(event: QuestEventModel) {
        _events.remove(event)
    }

    fun setBytecodeIr(bytecodeIr: BytecodeIr) {
        this.bytecodeIr = bytecodeIr
        _bytecodeRevision.value++
    }

    fun addCmRandomSpawn(spawn: DatCmRandomSpawn) {
        _cmRandomSpawns.add(spawn)
        bumpCmRevision()
    }

    fun removeCmRandomSpawn(spawn: DatCmRandomSpawn) {
        _cmRandomSpawns.remove(spawn)
        bumpCmRevision()
    }

    fun addCmMonsterMapping(mapping: DatCmMonsterMapping) {
        _cmMonsterMappings.add(mapping)
        bumpCmRevision()
    }

    fun removeCmMonsterMapping(mapping: DatCmMonsterMapping) {
        _cmMonsterMappings.remove(mapping)
        bumpCmRevision()
    }

    fun addCmConfigPool(pool: DatCmConfigPool) {
        _cmConfigPool.add(pool)
        bumpCmRevision()
    }

    fun removeCmConfigPool(pool: DatCmConfigPool) {
        _cmConfigPool.remove(pool)
        bumpCmRevision()
    }

    fun bumpCmRevision() {
        _cmDataRevision.value++
    }
}
