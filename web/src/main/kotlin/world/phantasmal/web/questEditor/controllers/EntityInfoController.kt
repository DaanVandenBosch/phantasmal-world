package world.phantasmal.web.questEditor.controllers

import world.phantasmal.core.math.degToRad
import world.phantasmal.core.math.radToDeg
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.value
import world.phantasmal.web.core.euler
import world.phantasmal.web.externals.three.Euler
import world.phantasmal.web.externals.three.Vector3
import world.phantasmal.web.questEditor.actions.RotateEntityAction
import world.phantasmal.web.questEditor.actions.TranslateEntityAction
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.models.QuestNpcModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.controllers.Controller

class EntityInfoController(private val store: QuestEditorStore) : Controller() {
    val unavailable: Val<Boolean> = store.selectedEntity.isNull()
    val enabled: Val<Boolean> = store.questEditingEnabled

    val type: Val<String> = store.selectedEntity.map {
        it?.let { if (it is QuestNpcModel) "NPC" else "Object" } ?: ""
    }

    val name: Val<String> = store.selectedEntity.map { it?.type?.simpleName ?: "" }

    val sectionId: Val<String> = store.selectedEntity
        .flatMapNull { it?.sectionId }
        .map { it?.toString() ?: "" }

    val wave: Val<String> = store.selectedEntity
        .flatMapNull { entity -> (entity as? QuestNpcModel)?.wave?.flatMapNull { it?.id } }
        .map { it?.toString() ?: "" }

    val waveHidden: Val<Boolean> = store.selectedEntity.map { it !is QuestNpcModel }

    private val pos: Val<Vector3> =
        store.selectedEntity.flatMap { it?.position ?: DEFAULT_POSITION }
    val posX: Val<Double> = pos.map { it.x }
    val posY: Val<Double> = pos.map { it.y }
    val posZ: Val<Double> = pos.map { it.z }

    private val rot: Val<Euler> =
        store.selectedEntity.flatMap { it?.rotation ?: DEFAULT_ROTATION }
    val rotX: Val<Double> = rot.map { radToDeg(it.x) }
    val rotY: Val<Double> = rot.map { radToDeg(it.y) }
    val rotZ: Val<Double> = rot.map { radToDeg(it.z) }

    fun setPosX(x: Double) {
        store.selectedEntity.value?.let { entity ->
            val pos = entity.position.value
            setPos(entity, x, pos.y, pos.z)
        }
    }

    fun setPosY(y: Double) {
        store.selectedEntity.value?.let { entity ->
            val pos = entity.position.value
            setPos(entity, pos.x, y, pos.z)
        }
    }

    fun setPosZ(z: Double) {
        store.selectedEntity.value?.let { entity ->
            val pos = entity.position.value
            setPos(entity, pos.x, pos.y, z)
        }
    }

    private fun setPos(entity: QuestEntityModel<*, *>, x: Double, y: Double, z: Double) {
        if (!enabled.value) return

        store.executeAction(TranslateEntityAction(
            setSelectedEntity = store::setSelectedEntity,
            entity,
            entity.section.value,
            entity.section.value,
            Vector3(x, y, z),
            entity.position.value,
            false,
        ))
    }

    fun setRotX(x: Double) {
        store.selectedEntity.value?.let { entity ->
            val rot = entity.rotation.value
            setRot(entity, degToRad(x), rot.y, rot.z)
        }
    }

    fun setRotY(y: Double) {
        store.selectedEntity.value?.let { entity ->
            val rot = entity.rotation.value
            setRot(entity, rot.x, degToRad(y), rot.z)
        }
    }

    fun setRotZ(z: Double) {
        store.selectedEntity.value?.let { entity ->
            val rot = entity.rotation.value
            setRot(entity, rot.x, rot.y, degToRad(z))
        }
    }

    private fun setRot(entity: QuestEntityModel<*, *>, x: Double, y: Double, z: Double) {
        if (!enabled.value) return

        store.executeAction(RotateEntityAction(
            setSelectedEntity = store::setSelectedEntity,
            entity,
            euler(x, y, z),
            entity.rotation.value,
            false,
        ))
    }

    companion object {
        private val DEFAULT_POSITION = value(Vector3(0.0, 0.0, 0.0))
        private val DEFAULT_ROTATION = value(euler(0.0, 0.0, 0.0))
    }
}
