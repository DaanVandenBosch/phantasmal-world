package world.phantasmal.web.questEditor.controllers

import world.phantasmal.core.math.degToRad
import world.phantasmal.core.math.radToDeg
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.list.emptyListVal
import world.phantasmal.observable.value.value
import world.phantasmal.observable.value.zeroIntVal
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

class EntityInfoController(
    private val areaStore: AreaStore,
    private val questEditorStore: QuestEditorStore,
) : Controller() {
    val unavailable: Val<Boolean> = questEditorStore.selectedEntity.isNull()
    val enabled: Val<Boolean> = questEditorStore.questEditingEnabled

    val type: Val<String> = questEditorStore.selectedEntity.map {
        it?.let { if (it is QuestNpcModel) "NPC" else "Object" } ?: ""
    }

    val name: Val<String> = questEditorStore.selectedEntity.map { it?.type?.simpleName ?: "" }

    val sectionId: Val<Int> = questEditorStore.selectedEntity
        .flatMap { it?.sectionId ?: zeroIntVal() }

    val waveId: Val<Int> = questEditorStore.selectedEntity
        .flatMap { entity ->
            if (entity is QuestNpcModel) {
                entity.wave.map { it.id }
            } else {
                zeroIntVal()
            }
        }

    val waveHidden: Val<Boolean> = questEditorStore.selectedEntity.map { it !is QuestNpcModel }

    private val pos: Val<Vector3> =
        questEditorStore.selectedEntity.flatMap { it?.position ?: DEFAULT_POSITION }
    val posX: Val<Double> = pos.map { it.x }
    val posY: Val<Double> = pos.map { it.y }
    val posZ: Val<Double> = pos.map { it.z }

    private val rot: Val<Euler> =
        questEditorStore.selectedEntity.flatMap { it?.rotation ?: DEFAULT_ROTATION }
    val rotX: Val<Double> = rot.map { radToDeg(it.x) }
    val rotY: Val<Double> = rot.map { radToDeg(it.y) }
    val rotZ: Val<Double> = rot.map { radToDeg(it.z) }

    val props: Val<List<QuestEntityPropModel>> =
        questEditorStore.selectedEntity.flatMap { it?.properties ?: emptyListVal() }

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

        questEditorStore.executeAction(TranslateEntityAction(
            setSelectedEntity = questEditorStore::setSelectedEntity,
            setEntitySection = { /* Won't be called. */ },
            entity,
            newSection = null,
            oldSection = null,
            newPosition = Vector3(x, y, z),
            oldPosition = entity.position.value,
        ))
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

        questEditorStore.executeAction(RotateEntityAction(
            setSelectedEntity = questEditorStore::setSelectedEntity,
            entity,
            euler(x, y, z),
            entity.rotation.value,
            false,
        ))
    }

    fun setPropValue(prop: QuestEntityPropModel, value: Any) {
        questEditorStore.selectedEntity.value?.let { entity ->
            questEditorStore.executeAction(EditEntityPropAction(
                setSelectedEntity = questEditorStore::setSelectedEntity,
                entity,
                prop,
                value,
                prop.value.value,
            ))
        }
    }

    companion object {
        private val DEFAULT_POSITION = value(Vector3(0.0, 0.0, 0.0))
        private val DEFAULT_ROTATION = value(euler(0.0, 0.0, 0.0))
    }
}
