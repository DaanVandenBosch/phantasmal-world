package world.phantasmal.web.questEditor.widgets

import org.w3c.dom.Node
import world.phantasmal.web.core.widgets.DockWidget
import world.phantasmal.web.questEditor.controllers.QuestEditorController
import world.phantasmal.web.questEditor.controllers.QuestEditorController.Companion.ASM_WIDGET_ID
import world.phantasmal.web.questEditor.controllers.QuestEditorController.Companion.ENTITY_INFO_WIDGET_ID
import world.phantasmal.web.questEditor.controllers.QuestEditorController.Companion.EVENTS_WIDGET_ID
import world.phantasmal.web.questEditor.controllers.QuestEditorController.Companion.NPC_COUNTS_WIDGET_ID
import world.phantasmal.web.questEditor.controllers.QuestEditorController.Companion.NPC_LIST_WIDGET_ID
import world.phantasmal.web.questEditor.controllers.QuestEditorController.Companion.OBJECT_LIST_WIDGET_ID
import world.phantasmal.web.questEditor.controllers.QuestEditorController.Companion.QUEST_INFO_WIDGET_ID
import world.phantasmal.web.questEditor.controllers.QuestEditorController.Companion.QUEST_RENDERER_WIDGET_ID
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.Widget

/**
 * Takes ownership of the widgets created by the given creation functions.
 */
class QuestEditorWidget(
    private val ctrl: QuestEditorController,
    private val createToolbar: () -> QuestEditorToolbarWidget,
    private val createQuestInfoWidget: () -> QuestInfoWidget,
    private val createNpcCountsWidget: () -> NpcCountsWidget,
    private val createEntityInfoWidget: () -> EntityInfoWidget,
    private val createQuestRendererWidget: () -> QuestRendererWidget,
    private val createAsmWidget: () -> AsmWidget,
    private val createNpcListWidget: () -> EntityListWidget,
    private val createObjectListWidget: () -> EntityListWidget,
) : Widget() {
    override fun Node.createElement() =
        div {
            className = "pw-quest-editor-quest-editor"

            addChild(createToolbar())
            addChild(DockWidget(
                ctrl = ctrl,
                createWidget = { id ->
                    when (id) {
                        QUEST_INFO_WIDGET_ID -> createQuestInfoWidget()
                        NPC_COUNTS_WIDGET_ID -> createNpcCountsWidget()
                        ENTITY_INFO_WIDGET_ID -> createEntityInfoWidget()
                        QUEST_RENDERER_WIDGET_ID -> createQuestRendererWidget()
                        ASM_WIDGET_ID -> createAsmWidget()
                        NPC_LIST_WIDGET_ID -> createNpcListWidget()
                        OBJECT_LIST_WIDGET_ID -> createObjectListWidget()
                        EVENTS_WIDGET_ID -> null // TODO: EventsWidget.
                        else -> null
                    }
                },
            ))
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-quest-editor-quest-editor {
                    display: flex;
                    flex-direction: column;
                    overflow: hidden;
                }
                .pw-quest-editor-quest-editor > * {
                    flex-grow: 1;
                }
            """.trimIndent())
        }
    }
}
