package world.phantasmal.web.questEditor.models

import world.phantasmal.core.math.floorMod
import world.phantasmal.lib.fileFormats.quest.EntityType
import world.phantasmal.lib.fileFormats.quest.QuestEntity
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.observable.value.list.listVal
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.web.core.minus
import world.phantasmal.web.core.rendering.conversion.vec3ToEuler
import world.phantasmal.web.core.rendering.conversion.vec3ToThree
import world.phantasmal.web.core.timesAssign
import world.phantasmal.web.core.toEuler
import world.phantasmal.web.externals.three.Euler
import world.phantasmal.web.externals.three.Quaternion
import world.phantasmal.web.externals.three.Vector3
import kotlin.math.PI

abstract class QuestEntityModel<Type : EntityType, Entity : QuestEntity<Type>>(
    /**
     * Don't modify the underlying entity directly because most of those modifications will not be
     * reflected in this model's properties.
     */
    val entity: Entity,
) {
    private val _sectionId = mutableVal(entity.sectionId.toInt())
    private val _section = mutableVal<SectionModel?>(null)
    private val _sectionInitialized = mutableVal(false)
    private val _position = mutableVal(vec3ToThree(entity.position))
    private val _worldPosition = mutableVal(_position.value)
    private val _rotation = mutableVal(vec3ToEuler(entity.rotation))
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
    val rotation: Val<Euler> = _rotation

    val worldRotation: Val<Euler> = _worldRotation

    val properties: ListVal<QuestEntityPropModel> = listVal(*Array(type.properties.size) {
        QuestEntityPropModel(this, type.properties[it])
    })

    open fun setSectionId(sectionId: Int) {
        entity.sectionId = sectionId.toShort()
        _sectionId.value = sectionId
    }

    fun initializeSection(section: SectionModel) {
        require(!sectionInitialized.value) {
            "Section is already initialized."
        }
        require(section.areaVariant.area.id == areaId) {
            "Section should lie within the entity's area."
        }

        setSectionId(section.id)

        _section.value = section

        // Update world position and rotation by calling setPosition and setRotation with the
        // current position and rotation.
        setPosition(position.value)
        setRotation(rotation.value)

        setSectionInitialized()
    }

    fun setSectionInitialized() {
        _sectionInitialized.value = true
    }

    /**
     * Will update the entity's relative transformation but keep its world transformation constant.
     */
    fun setSection(section: SectionModel) {
        require(section.areaVariant.area.id == areaId) {
            "Quest entities can't be moved across areas."
        }

        setSectionId(section.id)

        _section.value = section

        // Update relative position and rotation by calling setWorldPosition and setWorldRotation
        // with the current world position and rotation.
        setWorldPosition(worldPosition.value)
        setWorldRotation(worldRotation.value)
    }

    fun setPosition(pos: Vector3) {
        entity.setPosition(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat())

        _position.value = pos

        val section = section.value

        _worldPosition.value =
            if (section == null) pos
            else pos.clone().applyEuler(section.rotation).add(section.position)
    }

    fun setWorldPosition(pos: Vector3) {
        val section = section.value

        val relPos =
            if (section == null) pos
            else (pos - section.position).applyEuler(section.inverseRotation)

        entity.setPosition(relPos.x.toFloat(), relPos.y.toFloat(), relPos.z.toFloat())

        _worldPosition.value = pos
        _position.value = relPos
    }

    fun setRotation(rot: Euler) {
        floorModEuler(rot)

        entity.setRotation(rot.x.toFloat(), rot.y.toFloat(), rot.z.toFloat())
        _rotation.value = rot

        val section = section.value

        if (section == null) {
            _worldRotation.value = rot
        } else {
            q1.setFromEuler(rot)
            q2.setFromEuler(section.rotation)
            q1 *= q2
            _worldRotation.value = floorModEuler(q1.toEuler())
        }
    }

    fun setWorldRotation(rot: Euler) {
        floorModEuler(rot)

        val section = section.value

        val relRot = if (section == null) {
            rot
        } else {
            q1.setFromEuler(rot)
            q2.setFromEuler(section.rotation)
            q2.invert()
            q1 *= q2
            floorModEuler(q1.toEuler())
        }

        entity.setRotation(relRot.x.toFloat(), relRot.y.toFloat(), relRot.z.toFloat())
        _worldRotation.value = rot
        _rotation.value = relRot
    }

    companion object {
        // These quaternions are used as temporary variables to avoid memory allocation.
        private val q1 = Quaternion()
        private val q2 = Quaternion()

        private fun floorModEuler(euler: Euler): Euler =
            euler.set(
                floorMod(euler.x, 2 * PI),
                floorMod(euler.y, 2 * PI),
                floorMod(euler.z, 2 * PI),
            )
    }
}
