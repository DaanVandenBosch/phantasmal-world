package world.phantasmal.web.questEditor.rendering.input.state

import mu.KotlinLogging
import world.phantasmal.core.asJsArray
import world.phantasmal.lib.fileFormats.ninja.XjObject
import world.phantasmal.observable.value.Val
import world.phantasmal.web.core.dot
import world.phantasmal.web.core.minusAssign
import world.phantasmal.web.core.plusAssign
import world.phantasmal.web.core.rendering.OrbitalCameraInputManager
import world.phantasmal.web.core.rendering.conversion.AreaObjectUserData
import world.phantasmal.web.core.rendering.conversion.fingerPrint
import world.phantasmal.web.externals.three.*
import world.phantasmal.web.questEditor.actions.CreateEntityAction
import world.phantasmal.web.questEditor.actions.DeleteEntityAction
import world.phantasmal.web.questEditor.actions.RotateEntityAction
import world.phantasmal.web.questEditor.actions.TranslateEntityAction
import world.phantasmal.web.questEditor.loading.AreaUserData
import world.phantasmal.web.questEditor.models.*
import world.phantasmal.web.questEditor.rendering.QuestRenderContext
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import kotlin.math.PI
import kotlin.math.atan2

private val logger = KotlinLogging.logger {}

class StateContext(
    private val questEditorStore: QuestEditorStore,
    val renderContext: QuestRenderContext,
    val cameraInputManager: OrbitalCameraInputManager,
) {
    /**
     * Highlighted mesh with its original colors.
     */
    private var highlightedMesh: Pair<Mesh, List<Color>>? = null

    val devMode: Val<Boolean> = questEditorStore.devMode
    val quest: Val<QuestModel?> = questEditorStore.currentQuest
    val area: Val<AreaModel?> = questEditorStore.currentArea
    val wave: Val<WaveModel?> = questEditorStore.selectedEvent.flatMapNull { it?.wave }
    val selectedEntity: Val<QuestEntityModel<*, *>?> = questEditorStore.selectedEntity

    fun setHighlightedEntity(entity: QuestEntityModel<*, *>?) {
        questEditorStore.setHighlightedEntity(entity)
    }

    fun setSelectedEntity(entity: QuestEntityModel<*, *>?) {
        questEditorStore.setSelectedEntity(entity)
    }

    /**
     * If the drag-adjusted pointer is over the ground, translate an entity horizontally across the
     * ground. Otherwise translate the entity over the horizontal plane that intersects its origin.
     */
    fun translateEntityHorizontally(
        entity: QuestEntityModel<*, *>,
        dragAdjust: Vector3,
        grabOffset: Vector3,
        pointerPosition: Vector2,
        adjustSection: Boolean,
    ) {
        val pick = pickGround(pointerPosition, dragAdjust)

        if (pick == null) {
            // If the pointer is not over the ground, we translate the entity across the horizontal
            // plane in which the entity's origin lies.
            plane.set(UP_VECTOR, -entity.worldPosition.value.y + grabOffset.y)

            intersectPlane(pointerPosition, plane, tmpVec0)?.let { pointerPosOnPlane ->
                entity.setWorldPosition(Vector3(
                    pointerPosOnPlane.x + grabOffset.x,
                    entity.worldPosition.value.y,
                    pointerPosOnPlane.z + grabOffset.z,
                ))
            }
        } else {
            if (adjustSection) {
                pick.`object`.userData.unsafeCast<AreaUserData>().section?.let { section ->
                    entity.setSection(section)
                }
            }

            entity.setWorldPosition(
                Vector3(
                    pick.point.x,
                    pick.point.y + grabOffset.y - dragAdjust.y,
                    pick.point.z,
                )
            )
        }
    }

    fun translateEntityVertically(
        entity: QuestEntityModel<*, *>,
        dragAdjust: Vector3,
        grabOffset: Vector3,
        pointerPosition: Vector2,
    ) {
        // Intersect with a plane that's oriented towards the camera and that's coplanar with the
        // point where the entity was grabbed.
        val planeNormal = renderContext.camera.getWorldDirection(tmpVec0)
        planeNormal.negate()
        planeNormal.y = 0.0
        planeNormal.normalize()

        val entityPos = entity.worldPosition.value

        val grabPoint = tmpVec1.copy(entityPos)
        grabPoint -= grabOffset
        plane.setFromNormalAndCoplanarPoint(planeNormal, grabPoint)

        intersectPlane(pointerPosition, plane, tmpVec2)?.let { pointerPosOnPlane ->
            val y = pointerPosOnPlane.y + grabOffset.y
            val yDelta = y - entityPos.y
            dragAdjust.y -= yDelta
            entity.setWorldPosition(Vector3(
                entityPos.x,
                y,
                entityPos.z,
            ))
        }
    }

    fun finalizeEntityTranslation(
        entity: QuestEntityModel<*, *>,
        newSection: SectionModel?,
        oldSection: SectionModel?,
        newPosition: Vector3,
        oldPosition: Vector3,
    ) {
        questEditorStore.executeAction(TranslateEntityAction(
            ::setSelectedEntity,
            { questEditorStore.setEntitySection(entity, it) },
            entity,
            newSection?.id,
            oldSection?.id,
            newPosition,
            oldPosition,
        ))
    }

    /**
     * @param pointerPosition pointer position in normalized device space
     */
    fun rotateEntity(
        entity: QuestEntityModel<*, *>,
        initialRotation: Euler,
        grabPoint: Vector3,
        pointerPosition: Vector2,
    ) {
        // Intersect with a plane that's oriented along the entity's y-axis and that's coplanar with
        // the point where the entity was grabbed.
        val planeNormal = tmpVec0.copy(UP_VECTOR)
        planeNormal.applyEuler(entity.worldRotation.value)

        plane.setFromNormalAndCoplanarPoint(planeNormal, grabPoint)

        intersectPlane(pointerPosition, plane, tmpVec1)?.let { pointerPosOnPlane ->
            val yIntersect = plane.projectPoint(entity.worldPosition.value, tmpVec2)

            // Calculate vector from the entity's y-axis to the original grab point.
            val axisToGrab = tmpVec3.subVectors(yIntersect, grabPoint)

            // Calculate vector from the entity's y-axis to the new pointer position.
            val axisToPointer = tmpVec4.subVectors(yIntersect, pointerPosOnPlane)

            // Calculate the angle between the two vectors and rotate the entity around its y-axis
            // by that angle.
            val cos = axisToGrab dot axisToPointer
            val sin = planeNormal dot axisToGrab.cross(axisToPointer)
            val angle = atan2(sin, cos)

            entity.setWorldRotation(
                Euler(
                    initialRotation.x,
                    (initialRotation.y + angle) % PI2,
                    initialRotation.z,
                    "ZXY",
                ),
            )
        }
    }

    fun finalizeEntityRotation(
        entity: QuestEntityModel<*, *>,
        newRotation: Euler,
        oldRotation: Euler,
    ) {
        questEditorStore.executeAction(RotateEntityAction(
            ::setSelectedEntity,
            entity,
            newRotation,
            oldRotation,
            world = true,
        ))
    }

    fun finalizeEntityCreation(quest: QuestModel, entity: QuestEntityModel<*, *>) {
        questEditorStore.pushAction(CreateEntityAction(
            ::setSelectedEntity,
            quest,
            entity,
        ))
    }

    fun deleteEntity(quest: QuestModel, entity: QuestEntityModel<*, *>) {
        questEditorStore.executeAction(DeleteEntityAction(
            ::setSelectedEntity,
            quest,
            entity,
        ))
    }

    /**
     * @param origin position in normalized device space.
     */
    fun pickGround(origin: Vector2, dragAdjust: Vector3 = ZERO_VECTOR_3): Intersection? =
        intersectObject(origin, renderContext.collisionGeometry, dragAdjust) { intersection ->
            // Don't allow entities to be placed on very steep terrain. E.g. walls.
            // TODO: make use of the flags field in the collision data.
            intersection.face?.normal?.let { n -> n.y > 0.75 } ?: false
        }

    inline fun intersectObject(
        origin: Vector3,
        direction: Vector3,
        obj3d: Object3D,
        predicate: (Intersection) -> Boolean = { true },
    ): Intersection? {
        raycaster.set(origin, direction)
        raycasterIntersections.asJsArray().splice(0)
        raycaster.intersectObject(obj3d, recursive = true, raycasterIntersections)
        return raycasterIntersections.find(predicate)
    }

    /**
     * The ray's direction is determined by the camera.
     *
     * @param origin ray origin in normalized device space.
     * @param translateOrigin vector by which to translate the ray's origin after construction from
     * the camera.
     */
    inline fun intersectObject(
        origin: Vector2,
        obj3d: Object3D,
        translateOrigin: Vector3 = ZERO_VECTOR_3,
        predicate: (Intersection) -> Boolean = { true },
    ): Intersection? {
        raycaster.setFromCamera(origin, renderContext.camera)
        raycaster.ray.origin += translateOrigin
        raycasterIntersections.asJsArray().splice(0)
        raycaster.intersectObject(obj3d, recursive = true, raycasterIntersections)
        return raycasterIntersections.find(predicate)
    }

    fun setHighlightedMesh(mesh: Mesh?) {
        highlightedMesh?.let { (prevMesh, prevColors) ->
            prevMesh.material.unsafeCast<Array<MeshBasicMaterial>>().forEachIndexed { i, mat ->
                mat.color.set(prevColors[i])
            }
        }

        highlightedMesh = null

        if (mesh != null) {
            logger.info {
                val userData = mesh.userData.unsafeCast<AreaObjectUserData>()

                val areaObj = userData.areaObject
                val textureIds = mutableSetOf<Int>()

                fun getAllTextureIds(xjObject: XjObject) {
                    xjObject.model?.meshes?.forEach { it.material.textureId?.let(textureIds::add) }
                    xjObject.children.forEach(::getAllTextureIds)
                }

                getAllTextureIds(areaObj.xjObject)

                buildString {
                    append("Section ")
                    append(userData.sectionId)
                    append(" (finger print: ")
                    append(areaObj.fingerPrint())
                    append(", texture IDs: ")
                    textureIds.joinTo(this)
                    append(')')
                }
            }

            val origColors = mutableListOf<Color>()

            mesh.material.unsafeCast<Array<MeshBasicMaterial>>().forEach {
                origColors.add(it.color.clone())
                it.color.set(0xB0FF00)
            }

            highlightedMesh = Pair(mesh, origColors)
        }
    }

    private fun intersectPlane(
        origin: Vector2,
        plane: Plane,
        intersectionPoint: Vector3,
    ): Vector3? {
        raycaster.setFromCamera(origin, renderContext.camera)
        return raycaster.ray.intersectPlane(plane, intersectionPoint)
    }

    companion object {
        private const val PI2: Double = 2 * PI
        private val UP_VECTOR = Vector3(0.0, 1.0, 0.0)
        val ZERO_VECTOR_3 = Vector3(0.0, 0.0, 0.0)

        private val plane = Plane()
        private val tmpVec0 = Vector3()
        private val tmpVec1 = Vector3()
        private val tmpVec2 = Vector3()
        private val tmpVec3 = Vector3()
        private val tmpVec4 = Vector3()
        val raycaster = Raycaster()
        val raycasterIntersections = arrayOf<Intersection>()
    }
}
