package world.phantasmal.web.questEditor.widgets

import kotlinx.coroutines.launch
import org.w3c.dom.Node
import world.phantasmal.psolib.fileFormats.quest.EntityType
import world.phantasmal.web.questEditor.controllers.EntityListController
import world.phantasmal.web.questEditor.rendering.EntityImageRenderer
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.dom.img
import world.phantasmal.webui.dom.span
import world.phantasmal.webui.widgets.Widget

class EntityListWidget(
    private val ctrl: EntityListController,
    private val entityImageRenderer: EntityImageRenderer,
) : Widget(enabled = ctrl.enabled) {
    override fun Node.createElement() =
        div {
            className = "pw-quest-editor-entity-list"

            div {
                className = "pw-quest-editor-entity-list-inner"

                bindChildWidgetsTo(ctrl.entities) { entityType, _ ->
                    EntityListEntityWidget(entityType)
                }
            }
        }

    private inner class EntityListEntityWidget(private val entityType: EntityType) : Widget() {
        override fun Node.createElement() =
            div {
                className = "pw-quest-editor-entity-list-entity"
                draggable = true

                img {
                    width = 100
                    height = 100
                    style.visibility = "hidden"
                    style.asDynamic().pointerEvents = "none"

                    scope.launch {
                        src = entityImageRenderer.renderToImage(entityType)
                        style.visibility = ""

                        addDisposable(this@div.entityDndSource(entityType, src))
                    }
                }

                span {
                    textContent = entityType.simpleName
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
