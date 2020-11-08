package world.phantasmal.web.questEditor.models

import world.phantasmal.core.math.floorMod
import world.phantasmal.lib.fileFormats.quest.EntityType
import world.phantasmal.lib.fileFormats.quest.QuestEntity
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.web.core.plusAssign
import world.phantasmal.web.core.rendering.conversion.babylonToVec3
import world.phantasmal.web.core.rendering.conversion.vec3ToBabylon
import world.phantasmal.web.core.timesAssign
import world.phantasmal.web.externals.babylon.Quaternion
import world.phantasmal.web.externals.babylon.Vector3
import kotlin.math.PI

abstract class QuestEntityModel<Type : EntityType, Entity : QuestEntity<Type>>(
    private val entity: Entity,
) {
    private val _sectionId = mutableVal(entity.sectionId)
    private val _section = mutableVal<SectionModel?>(null)
    private val _sectionInitialized = mutableVal(false)
    private val _position = mutableVal(vec3ToBabylon(entity.position))
    private val _worldPosition = mutableVal(_position.value)
    private val _rotation = mutableVal(vec3ToBabylon(entity.rotation))
    private val _worldRotation = mutableVal(_rotation.value)

    val type: Type get() = entity.type

    val areaId: Int get() = entity.areaId

    val sectionId: Val<Int> = _sectionId

    val section: Val<SectionModel?> = _section
    val sectionInitialized: Val<Boolean> = _sectionInitialized

    /**
     * Section-relative position
     */
    val position: Val<Vector3> = _position

    val worldPosition: Val<Vector3> = _worldPosition

    /**
     * Section-relative rotation
     */
    val rotation: Val<Vector3> = _rotation

    val worldRotation: Val<Vector3> = _worldRotation

    fun setSection(section: SectionModel) {
        require(section.areaVariant.area.id == areaId) {
            "Quest entities can't be moved across areas."
        }

        entity.sectionId = section.id

        _section.value = section
        _sectionId.value = section.id

        setPosition(position.value)
        setRotation(rotation.value)

        setSectionInitialized()
    }

    fun setSectionInitialized() {
        _sectionInitialized.value = true
    }

    fun setPosition(pos: Vector3) {
        entity.position = babylonToVec3(pos)

        _position.value = pos

        val section = section.value

        _worldPosition.value =
            if (section == null) pos
            else Vector3.Zero().also { worldPos ->
                pos.rotateByQuaternionToRef(section.rotationQuaternion, worldPos)
                worldPos += section.position
            }
    }

    fun setRotation(rot: Vector3) {
        floorModEuler(rot)

        entity.rotation = babylonToVec3(rot)

        val section = section.value

        if (section == null) {
            _worldRotation.value = rot
        } else {
            Quaternion.FromEulerAnglesToRef(rot.x, rot.y, rot.z, q1)
            Quaternion.FromEulerAnglesToRef(
                section.rotation.x,
                section.rotation.y,
                section.rotation.z,
                q2
            )
            q1 *= q2
            val worldRot = q1.toEulerAngles()
            floorModEuler(worldRot)
            _worldRotation.value = worldRot
        }
    }

    private fun floorModEuler(euler: Vector3) {
        euler.set(
            floorMod(euler.x, 2 * PI),
            floorMod(euler.y, 2 * PI),
            floorMod(euler.z, 2 * PI),
        )
    }

    companion object {
        // These quaternions are used as temporary variables to avoid memory allocation.
        private val q1 = Quaternion()
        private val q2 = Quaternion()
    }
}
