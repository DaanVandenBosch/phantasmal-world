package world.phantasmal.web.questEditor.rendering.input.state

import world.phantasmal.psolib.fileFormats.quest.NpcType
import world.phantasmal.psolib.fileFormats.quest.ObjectType
import world.phantasmal.psolib.fileFormats.quest.QuestNpc
import world.phantasmal.psolib.fileFormats.quest.QuestObject
import world.phantasmal.web.externals.three.Vector2
import world.phantasmal.web.externals.three.Vector3
import world.phantasmal.web.questEditor.models.*
import world.phantasmal.web.questEditor.rendering.input.*

class CreationState(
    private val ctx: StateContext,
    event: EntityDragEnterEvt,
    private val quest: QuestModel,
    area: AreaModel,
) : State() {
    private val entity: QuestEntityModel<*, *> =
        when (event.entityType) {
            is NpcType -> {
                val wave = ctx.wave.value
                val waveId = wave?.id ?: 0
                val npc = QuestNpc(
                    event.entityType,
                    quest.episode,
                    area.id,
                    waveId.toShort(),
                )
                wave?.let {
                    npc.sectionId = wave.sectionId.toShort()
                }
                QuestNpcModel(npc, waveId).also {
                    it.setSectionInitialized()
                    quest.addNpc(it)
                }
            }
            is ObjectType -> {
                QuestObjectModel(
                    QuestObject(event.entityType, area.id)
                ).also {
                    it.setSectionInitialized()
                    quest.addObject(it)
                }
            }
            else -> error("Unsupported entity type ${event.entityType::class}.")
        }

    private val dragAdjust = Vector3(.0, .0, .0)
    private val pointerDevicePosition = Vector2()
    private var shouldTranslate = false
    private var shouldTranslateVertically = false
    private var shouldAdjustSection = false

    init {
        event.allowDrop()
        event.hideDragElement()

        ctx.translateEntityHorizontally(
            entity,
            ZERO_VECTOR,
            ZERO_VECTOR,
            event.pointerDevicePosition,
            adjustSection = true,
        )

        ctx.setSelectedEntity(entity)
    }

    override fun processEvent(event: Evt): State =
        when (event) {
            is EntityDragOverEvt -> {
                event.allowDrop()
                pointerDevicePosition.copy(event.pointerDevicePosition)
                shouldTranslate = true
                shouldTranslateVertically = event.shiftKey
                shouldAdjustSection = !event.ctrlKey
                this
            }

            is EntityDragLeaveEvt -> {
                event.showDragElement()
                ctx.removeEntity(quest, entity)
                IdleState(ctx, entityManipulationEnabled = true)
            }

            is EntityDropEvt -> {
                ctx.finalizeEntityCreation(quest, entity)
                IdleState(ctx, entityManipulationEnabled = true)
            }

            else -> this
        }

    override fun beforeRender() {
        if (shouldTranslate) {
            if (shouldTranslateVertically) {
                ctx.translateEntityVertically(
                    entity,
                    dragAdjust,
                    ZERO_VECTOR,
                    pointerDevicePosition,
                )
            } else {
                ctx.translateEntityHorizontally(
                    entity,
                    dragAdjust,
                    ZERO_VECTOR,
                    pointerDevicePosition,
                    shouldAdjustSection,
                )
            }

            shouldTranslate = false
        }
    }

    override fun cancel() {
        ctx.removeEntity(quest, entity)
    }

    companion object {
        private val ZERO_VECTOR = Vector3(.0, .0, .0)
    }
}
