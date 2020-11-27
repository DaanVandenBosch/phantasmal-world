package world.phantasmal.web.questEditor.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.Node
import world.phantasmal.web.core.widgets.DockWidget
import world.phantasmal.web.questEditor.controllers.QuestEditorController
import world.phantasmal.web.questEditor.controllers.QuestEditorController.Companion.ASSEMBLY_EDITOR_WIDGET_ID
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
    scope: CoroutineScope,
    private val ctrl: QuestEditorController,
    private val createToolbar: (CoroutineScope) -> QuestEditorToolbarWidget,
    private val createQuestInfoWidget: (CoroutineScope) -> QuestInfoWidget,
    private val createNpcCountsWidget: (CoroutineScope) -> NpcCountsWidget,
    private val createEntityInfoWidget: (CoroutineScope) -> EntityInfoWidget,
    private val createQuestRendererWidget: (CoroutineScope) -> QuestRendererWidget,
    private val createAssemblyEditorWidget: (CoroutineScope) -> AssemblyEditorWidget,
    private val createNpcListWidget: (CoroutineScope) -> EntityListWidget,
    private val createObjectListWidget: (CoroutineScope) -> EntityListWidget,
) : Widget(scope) {
    override fun Node.createElement() =
        div {
            className = "pw-quest-editor-quest-editor"

            addChild(createToolbar(scope))
            addChild(DockWidget(
                scope,
                ctrl = ctrl,
                createWidget = { scope, id ->
                    when (id) {
                        QUEST_INFO_WIDGET_ID -> createQuestInfoWidget(scope)
                        NPC_COUNTS_WIDGET_ID -> createNpcCountsWidget(scope)
                        ENTITY_INFO_WIDGET_ID -> createEntityInfoWidget(scope)
                        QUEST_RENDERER_WIDGET_ID -> createQuestRendererWidget(scope)
                        ASSEMBLY_EDITOR_WIDGET_ID -> createAssemblyEditorWidget(scope)
                        NPC_LIST_WIDGET_ID -> createNpcListWidget(scope)
                        OBJECT_LIST_WIDGET_ID -> createObjectListWidget(scope)
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
