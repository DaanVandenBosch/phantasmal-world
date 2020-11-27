package world.phantasmal.web.questEditor.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.Node
import world.phantasmal.web.questEditor.controllers.EntityListController
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.dom.img
import world.phantasmal.webui.dom.span
import world.phantasmal.webui.widgets.Widget

class EntityListWidget(
    scope: CoroutineScope,
    private val ctrl: EntityListController,
) : Widget(scope, enabled = ctrl.enabled) {
    override fun Node.createElement() =
        div {
            className = "pw-quest-editor-entity-list"
            tabIndex = -1

            div {
                className = "pw-quest-editor-entity-list-inner"

                bindChildrenTo(ctrl.entities) { entityType, index ->
                    div {
                        className = "pw-quest-editor-entity-list-entity"

                        img {
                            width = 100
                            height = 100
                        }

                        span {
                            textContent = entityType.simpleName
                        }
                    }
                }
            }
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-quest-editor-entity-list {
                    outline: none;
                    overflow: auto;
                }

                .pw-quest-editor-entity-list-inner {
                    display: grid;
                    grid-template-columns: repeat(auto-fill, 100px);
                    grid-column-gap: 6px;
                    grid-row-gap: 6px;
                    justify-content: center;
                    margin: 6px;
                }

                .pw-quest-editor-entity-list-entity {
                    box-sizing: border-box;
                    display: flex;
                    flex-direction: column;
                    text-align: center;
                }
            """.trimIndent())
        }
    }
}
