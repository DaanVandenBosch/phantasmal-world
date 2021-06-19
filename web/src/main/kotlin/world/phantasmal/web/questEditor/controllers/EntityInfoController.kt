package world.phantasmal.web.questEditor.controllers

import world.phantasmal.core.math.degToRad
import world.phantasmal.core.math.radToDeg
import world.phantasmal.lib.fileFormats.quest.EntityPropType
import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.cell
import world.phantasmal.observable.cell.list.ListCell
import world.phantasmal.observable.cell.list.emptyListCell
import world.phantasmal.observable.cell.list.flatMapToList
import world.phantasmal.observable.cell.zeroIntCell
import world.phantasmal.web.core.euler
import world.phantasmal.web.externals.three.Euler
import world.phantasmal.web.externals.three.Vector3
import world.phantasmal.web.questEditor.actions.*
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.models.QuestEntityPropModel
import world.phantasmal.web.questEditor.models.QuestNpcModel
import world.phantasmal.web.questEditor.stores.AreaStore
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.controllers.Controller

sealed class EntityInfoPropModel(
    protected val store: QuestEditorStore,
    protected val prop: QuestEntityPropModel,
) {
    val label = prop.name + ":"

    protected fun setPropValue(prop: QuestEntityPropModel, value: Any) {
        store.selectedEntity.value?.let { entity ->
            store.executeAction(
                EditEntityPropAction(
                    setSelectedEntity = store::setSelectedEntity,
                    entity,
                    prop,
                    value,
                    prop.value.value,
                )
            )
        }
    }

    class I32(store: QuestEditorStore, prop: QuestEntityPropModel) :
        EntityInfoPropModel(store, prop) {

        @Suppress("UNCHECKED_CAST")
        val value: Cell<Int> = prop.value as Cell<Int>

        val showGoToEvent: Boolean = prop.name == "Event ID"

        val canGoToEvent: Cell<Boolean> = store.canGoToEvent(value)

        fun setValue(value: Int) {
            setPropValue(prop, value)
        }

        fun goToEvent() {
            store.goToEvent(value.value)
        }
    }

    class F32(store: QuestEditorStore, prop: QuestEntityPropModel) :
        EntityInfoPropModel(store, prop) {

        val value: Cell<Double> = prop.value.map { (it as Float).toDouble() }

        fun setValue(value: Double) {
            setPropValue(prop, value.toFloat())
        }
    }

    class Angle(store: QuestEditorStore, prop: QuestEntityPropModel) :
        EntityInfoPropModel(store, prop) {

        val value: Cell<Double> = prop.value.map { radToDeg((it as Float).toDouble()) }

        fun setValue(value: Double) {
            setPropValue(prop, degToRad(value).toFloat())
        }
    }
}

class EntityInfoController(
    private val areaStore: AreaStore,
    private val questEditorStore: QuestEditorStore,
) : Controller() {
    val unavailable: Cell<Boolean> = questEditorStore.selectedEntity.isNull()
    val enabled: Cell<Boolean> = questEditorStore.questEditingEnabled

    val type: Cell<String> = questEditorStore.selectedEntity.map {
        it?.let { if (it is QuestNpcModel) "NPC" else "Object" } ?: ""
    }

    val name: Cell<String> = questEditorStore.selectedEntity.map { it?.type?.simpleName ?: "" }

    val sectionId: Cell<Int> = questEditorStore.selectedEntity
        .flatMap { it?.sectionId ?: zeroIntCell() }

    val waveId: Cell<Int> = questEditorStore.selectedEntity
        .flatMap { entity ->
            if (entity is QuestNpcModel) {
                entity.wave.map { it.id }
            } else {
                zeroIntCell()
            }
        }

    val waveHidden: Cell<Boolean> = questEditorStore.selectedEntity.map { it !is QuestNpcModel }

    private val pos: Cell<Vector3> =
        questEditorStore.selectedEntity.flatMap { it?.position ?: DEFAULT_POSITION }
    val posX: Cell<Double> = pos.map { it.x }
    val posY: Cell<Double> = pos.map { it.y }
    val posZ: Cell<Double> = pos.map { it.z }

    private val rot: Cell<Euler> =
        questEditorStore.selectedEntity.flatMap { it?.rotation ?: DEFAULT_ROTATION }
    val rotX: Cell<Double> = rot.map { radToDeg(it.x) }
    val rotY: Cell<Double> = rot.map { radToDeg(it.y) }
    val rotZ: Cell<Double> = rot.map { radToDeg(it.z) }

    val props: ListCell<EntityInfoPropModel> =
        questEditorStore.selectedEntity.flatMapToList { entity ->
            entity?.properties?.listMap { prop ->
                when (prop.type) {
                    EntityPropType.I32 -> EntityInfoPropModel.I32(questEditorStore, prop)
                    EntityPropType.F32 -> EntityInfoPropModel.F32(questEditorStore, prop)
                    EntityPropType.Angle -> EntityInfoPropModel.Angle(questEditorStore, prop)
                }
            } ?: emptyListCell()
        }

    fun focused() {
        questEditorStore.makeMainUndoCurrent()
    }

    suspend fun setSectionId(sectionId: Int) {
        questEditorStore.currentQuest.value?.let { quest ->
            questEditorStore.selectedEntity.value?.let { entity ->
                val section = areaStore.getSection(
                    quest.episode,
                    quest.areaVariants.value.first { it.area.id == entity.areaId },
                    sectionId,
                )
                questEditorStore.executeAction(
                    EditEntitySectionAction(
                        entity,
                        sectionId,
                        section,
                        entity.sectionId.value,
                        entity.section.value,
                    )
                )
            }
        }
    }

    fun setWaveId(waveId: Int) {
        (questEditorStore.selectedEntity.value as? QuestNpcModel)?.let { npc ->
            questEditorStore.executeAction(
                EditPropertyAction(
                    "Edit ${npc.type.simpleName} wave",
                    npc::setWaveId,
                    waveId,
                    npc.wave.value.id,
                )
            )
        }
    }

    fun setPosX(x: Double) {
        questEditorStore.selectedEntity.value?.let { entity ->
            val pos = entity.position.value
            setPos(entity, x, pos.y, pos.z)
        }
    }

    fun setPosY(y: Double) {
        questEditorStore.selectedEntity.value?.let { entity ->
            val pos = entity.position.value
            setPos(entity, pos.x, y, pos.z)
        }
    }

    fun setPosZ(z: Double) {
        questEditorStore.selectedEntity.value?.let { entity ->
            val pos = entity.position.value
            setPos(entity, pos.x, pos.y, z)
        }
    }

    private fun setPos(entity: QuestEntityModel<*, *>, x: Double, y: Double, z: Double) {
        if (!enabled.value) return

        questEditorStore.executeAction(
            TranslateEntityAction(
                setSelectedEntity = questEditorStore::setSelectedEntity,
                setEntitySection = { /* Won't be called. */ },
                entity,
                newSection = null,
                oldSection = null,
                newPosition = Vector3(x, y, z),
                oldPosition = entity.position.value,
            )
        )
    }

    fun setRotX(x: Double) {
        questEditorStore.selectedEntity.value?.let { entity ->
            val rot = entity.rotation.value
            setRot(entity, degToRad(x), rot.y, rot.z)
        }
    }

    fun setRotY(y: Double) {
        questEditorStore.selectedEntity.value?.let { entity ->
            val rot = entity.rotation.value
            setRot(entity, rot.x, degToRad(y), rot.z)
        }
    }

    fun setRotZ(z: Double) {
        questEditorStore.selectedEntity.value?.let { entity ->
            val rot = entity.rotation.value
            setRot(entity, rot.x, rot.y, degToRad(z))
        }
    }

    private fun setRot(entity: QuestEntityModel<*, *>, x: Double, y: Double, z: Double) {
        if (!enabled.value) return

        questEditorStore.executeAction(
            RotateEntityAction(
                setSelectedEntity = questEditorStore::setSelectedEntity,
                entity,
                euler(x, y, z),
                entity.rotation.value,
                false,
            )
        )
    }

    companion object {
        private val DEFAULT_POSITION = cell(Vector3(0.0, 0.0, 0.0))
        private val DEFAULT_ROTATION = cell(euler(0.0, 0.0, 0.0))
    }
}
