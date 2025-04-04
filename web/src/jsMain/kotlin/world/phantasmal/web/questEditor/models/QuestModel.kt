package world.phantasmal.web.questEditor.models

import world.phantasmal.cell.Cell
import world.phantasmal.cell.list.ListCell
import world.phantasmal.cell.list.flatMapToList
import world.phantasmal.cell.list.listCell
import world.phantasmal.cell.list.mutableListCell
import world.phantasmal.cell.map
import world.phantasmal.cell.mutableCell
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.asm.BytecodeIr
import world.phantasmal.psolib.fileFormats.quest.DatUnknown

class QuestModel(
    id: Int,
    language: Int,
    name: String,
    shortDescription: String,
    longDescription: String,
    val episode: Episode,
    mapDesignations: Map<Int, Int>,
    npcs: MutableList<QuestNpcModel>,
    objects: MutableList<QuestObjectModel>,
    events: MutableList<QuestEventModel>,
    /**
     * (Partial) raw DAT data that can't be parsed yet by Phantasmal.
     */
    val datUnknowns: List<DatUnknown>,
    bytecodeIr: BytecodeIr,
    val shopItems: UIntArray,
    getVariant: (Episode, areaId: Int, variantId: Int) -> AreaVariantModel?,
) {
    private val _id = mutableCell(0)
    private val _language = mutableCell(0)
    private val _name = mutableCell("")
    private val _shortDescription = mutableCell("")
    private val _longDescription = mutableCell("")
    private val _mapDesignations = mutableCell(mapDesignations)
    private val _npcs = mutableListCell(npcs)
    private val _objects = mutableListCell(objects)
    private val _events = mutableListCell(events)

    val id: Cell<Int> = _id
    val language: Cell<Int> = _language
    val name: Cell<String> = _name
    val shortDescription: Cell<String> = _shortDescription
    val longDescription: Cell<String> = _longDescription

    /**
     * Map of area IDs to area variant IDs. One designation per area.
     */
    val mapDesignations: Cell<Map<Int, Int>> = _mapDesignations

    /**
     * Map of area IDs to entity counts.
     */
    val entitiesPerArea: Cell<Map<Int, Int>>

    /**
     * One variant per area.
     */
    val areaVariants: ListCell<AreaVariantModel>

    val npcs: ListCell<QuestNpcModel> = _npcs
    val objects: ListCell<QuestObjectModel> = _objects

    val events: ListCell<QuestEventModel> = _events

    var bytecodeIr: BytecodeIr = bytecodeIr
        private set

    init {
        setId(id)
        setLanguage(language)
        setName(name)
        setShortDescription(shortDescription)
        setLongDescription(longDescription)

        entitiesPerArea = map(this.npcs, this.objects) { ns, os ->
            val map = mutableMapOf<Int, Int>()

            for (npc in ns) {
                map[npc.areaId] = (map[npc.areaId] ?: 0) + 1
            }

            for (obj in os) {
                map[obj.areaId] = (map[obj.areaId] ?: 0) + 1
            }

            map
        }

        areaVariants =
            flatMapToList(entitiesPerArea, this.mapDesignations) { entitiesPerArea, mds ->
                val variants = mutableMapOf<Int, AreaVariantModel>()

                for (areaId in entitiesPerArea.keys) {
                    getVariant(episode, areaId, 0)?.let {
                        variants[areaId] = it
                    }
                }

                for ((areaId, variantId) in mds) {
                    getVariant(episode, areaId, variantId)?.let {
                        variants[areaId] = it
                    }
                }

                listCell(*variants.values.toTypedArray())
            }
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

    fun setMapDesignations(mapDesignations: Map<Int, Int>) {
        _mapDesignations.value = mapDesignations
    }

    fun addNpc(npc: QuestNpcModel) {
        _npcs.add(npc)
    }

    fun addObject(obj: QuestObjectModel) {
        _objects.add(obj)
    }

    fun removeEntity(entity: QuestEntityModel<*, *>) {
        when (entity) {
            is QuestNpcModel -> _npcs.remove(entity)
            is QuestObjectModel -> _objects.remove(entity)
        }
    }

    fun addEvent(index: Int, event: QuestEventModel) {
        _events.add(index, event)
    }

    fun removeEvent(event: QuestEventModel) {
        _events.remove(event)
    }

    fun setBytecodeIr(bytecodeIr: BytecodeIr) {
        this.bytecodeIr = bytecodeIr
    }
}
