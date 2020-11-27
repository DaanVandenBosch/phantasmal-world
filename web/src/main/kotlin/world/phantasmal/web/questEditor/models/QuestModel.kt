package world.phantasmal.web.questEditor.models

import world.phantasmal.lib.assembly.Segment
import world.phantasmal.lib.fileFormats.quest.Episode
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.observable.value.list.mutableListVal
import world.phantasmal.observable.value.map
import world.phantasmal.observable.value.mutableVal

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
    val bytecodeIr: List<Segment>,
    getVariant: (Episode, areaId: Int, variantId: Int) -> AreaVariantModel?,
) {
    private val _id = mutableVal(0)
    private val _language = mutableVal(0)
    private val _name = mutableVal("")
    private val _shortDescription = mutableVal("")
    private val _longDescription = mutableVal("")
    private val _mapDesignations = mutableVal(mapDesignations)
    private val _npcs = mutableListVal(npcs)
    private val _objects = mutableListVal(objects)

    val id: Val<Int> = _id
    val language: Val<Int> = _language
    val name: Val<String> = _name
    val shortDescription: Val<String> = _shortDescription
    val longDescription: Val<String> = _longDescription
    val mapDesignations: Val<Map<Int, Int>> = _mapDesignations

    /**
     * Map of area IDs to entity counts.
     */
    val entitiesPerArea: Val<Map<Int, Int>>

    /**
     * One variant per area.
     */
    val areaVariants: Val<List<AreaVariantModel>>

    val npcs: ListVal<QuestNpcModel> = _npcs
    val objects: ListVal<QuestObjectModel> = _objects

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

        areaVariants = map(entitiesPerArea, this.mapDesignations) { entitiesPerArea, mds ->
            val variants = mutableMapOf<Int, AreaVariantModel>()

            for (areaId in entitiesPerArea.values) {
                getVariant(episode, areaId, 0)?.let {
                    variants[areaId] = it
                }
            }

            for ((areaId, variantId) in mds) {
                getVariant(episode, areaId, variantId)?.let {
                    variants[areaId] = it
                }
            }

            variants.values.toList()
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
}
