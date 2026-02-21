package world.phantasmal.web.questEditor.models

import world.phantasmal.psolib.fileFormats.quest.EntityType
import world.phantasmal.psolib.fileFormats.quest.QuestEntity
import world.phantasmal.cell.Cell
import world.phantasmal.cell.list.ListCell
import world.phantasmal.cell.list.listCell
import world.phantasmal.cell.mutableCell
import world.phantasmal.cell.mutate
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
    private val _sectionId = mutableCell(entity.sectionId.toInt())
    private val _section = mutableCell<SectionModel?>(null)
    private val _sectionInitialized = mutableCell(false)
    private val _position = mutableCell(vec3ToThree(entity.position))
    private val _worldPosition = mutableCell(_position.value)
    private val _rotation = mutableCell(vec3ToEuler(entity.rotation))
    private val _worldRotation = mutableCell(_rotation.value)

    val type: Type get() = entity.type

    val areaId: Int get() = entity.areaId

    val sectionId: Cell<Int> = _sectionId

    val section: Cell<SectionModel?> = _section
    val sectionInitialized: Cell<Boolean> = _sectionInitialized

    /**
     * Section-relative position
     */
    val position: Cell<Vector3> = _position

    open val worldPosition: Cell<Vector3> = _worldPosition

    /**
     * Section-relative rotation
     */
    val rotation: Cell<Euler> = _rotation

    val worldRotation: Cell<Euler> = _worldRotation

    val properties: ListCell<QuestEntityPropModel> = listCell(*Array(type.properties.size) {
        QuestEntityPropModel(this, type.properties[it])
    })

    open fun setSectionId(sectionId: Int) {
        mutate {
            entity.sectionId = sectionId.toShort()
            _sectionId.value = sectionId

            if (sectionId != _section.value?.id) {
                _section.value = null
            }
        }
    }

    fun setSectionInitialized() {
        _sectionInitialized.value = true
    }

    /**
     * @param keepRelativeTransform If true, keep the entity's relative transform and update its
     * world transform. Otherwise keep its world transform and update its relative transform.
     */
    fun setSection(section: SectionModel, keepRelativeTransform: Boolean = false) {
        require(section.areaVariant.area.id == areaId) {
            "Quest entities can't be moved across areas."
        }

        mutate {
            entity.sectionId = section.id.toShort()
            _sectionId.value = section.id

            _section.value = section

            if (keepRelativeTransform) {
                // Update world position and rotation by calling setPosition and setRotation with the
                // current position and rotation.
                setPosition(position.value)
                setRotation(rotation.value)
            } else {
                // Update relative position and rotation by calling setWorldPosition and
                // setWorldRotation with the current world position and rotation.
                setWorldPosition(worldPosition.value)
                setWorldRotation(worldRotation.value)
            }

            setSectionInitialized()
        }
    }

    fun setPosition(pos: Vector3) {
        mutate {
            entity.setPosition(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat())

            _position.value = pos

            val section = section.value

            _worldPosition.value =
                if (section == null) pos
                else pos.clone().applyEuler(section.rotation).add(section.position)
        }
    }

    fun setWorldPosition(pos: Vector3) {
        mutate {
            val section = section.value

            val relPos =
                if (section == null) pos
                else (pos - section.position).applyEuler(section.inverseRotation)

            entity.setPosition(relPos.x.toFloat(), relPos.y.toFloat(), relPos.z.toFloat())

            _worldPosition.value = pos
            _position.value = relPos
        }
    }

    fun setRotation(rot: Euler) {
        mutate {
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
    }

    fun setWorldRotation(rot: Euler) {
        mutate {
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
    }

    companion object {
        // These quaternions are used as temporary variables to avoid memory allocation.
        private val q1 = Quaternion()
        private val q2 = Quaternion()

        private fun floorModEuler(euler: Euler): Euler =
            euler.set(
                euler.x.mod(2 * PI),
                euler.y.mod(2 * PI),
                euler.z.mod(2 * PI),
            )
    }
}
