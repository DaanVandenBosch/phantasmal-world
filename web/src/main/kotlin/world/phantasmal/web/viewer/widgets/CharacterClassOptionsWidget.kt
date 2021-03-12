package world.phantasmal.web.viewer.widgets

import kotlinx.coroutines.launch
import org.w3c.dom.Node
import world.phantasmal.observable.value.value
import world.phantasmal.web.shared.dto.SectionId
import world.phantasmal.web.viewer.controller.CharacterClassOptionsController
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.dom.table
import world.phantasmal.webui.dom.td
import world.phantasmal.webui.dom.tr
import world.phantasmal.webui.widgets.Select
import world.phantasmal.webui.widgets.Widget

class CharacterClassOptionsWidget(private val ctrl: CharacterClassOptionsController) : Widget() {
    override fun Node.createElement() =
        div {
            className = "pw-viewer-character-class-options"

            table {
                tr {
                    val sectionIdSelect = Select(
                        enabled = ctrl.enabled,
                        className = "pw-viewer-character-class-options-section-id",
                        label = "Section ID:",
                        items = value(SectionId.VALUES_LIST),
                        selected = ctrl.currentSectionId,
                        onSelect = { sectionId ->
                            scope.launch { ctrl.setCurrentSectionId(sectionId) }
                        },
                        itemToString = { it.uiName },
                    )
                    td { addChild(sectionIdSelect.label!!) }
                    td { addChild(sectionIdSelect) }
                }
                tr {
                    val bodySelect = Select(
                        enabled = ctrl.enabled,
                        className = "pw-viewer-character-class-options-body",
                        label = "Body:",
                        items = ctrl.currentBodyOptions,
                        selected = ctrl.currentBody,
                        onSelect = { body ->
                            scope.launch { ctrl.setCurrentBody(body) }
                        },
                    )
                    td { addChild(bodySelect.label!!) }
                    td { addChild(bodySelect) }
                }
            }
        }

    companion object {
        init {
            @Suppress("CssUnresolvedCustomProperty", "CssUnusedSymbol")
            // language=css
            style("""
                .pw-viewer-character-class-options {
                    box-sizing: border-box;
                    border-left: var(--pw-border);
                    border-right: var(--pw-border);
                    padding: 0 0 0 4px;
                }

                .pw-viewer-character-class-options-section-id {
                    width: 120px;
                }

                .pw-viewer-character-class-options-body {
                    width: 60px;
                }
            """.trimIndent())
        }
    }
}
