package world.phantasmal.web.questEditor.widgets

import kotlinx.coroutines.launch
import org.w3c.dom.Node
import world.phantasmal.cell.map
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.fileFormats.quest.EntityType
import world.phantasmal.psolib.fileFormats.quest.isBossArea
import world.phantasmal.psolib.fileFormats.quest.isPioneer2OrLab
import world.phantasmal.web.questEditor.controllers.EntityListController
import world.phantasmal.web.questEditor.rendering.EntityImageRenderer
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.dom.img
import world.phantasmal.webui.dom.span
import world.phantasmal.webui.widgets.Widget

class EntityListWidget(
    private val ctrl: EntityListController,
    private val entityImageRenderer: EntityImageRenderer,
    private val store: QuestEditorStore? = null,
    private val isNpcList: Boolean = false,
) : Widget(enabled = ctrl.enabled) {
    override fun Node.createElement() =
        div {
            className = "pw-quest-editor-entity-list"

            // Add omnispawn checkbox for NPC list only in regular areas
            if (isNpcList && store != null) {
                // Check if current area should show omnispawn
                val showOmnispawn = map(store.currentQuest, store.currentArea) { quest, area ->
                    val episode = quest?.episode ?: Episode.I
                    val areaId = area?.id ?: 0

                    // Check if this is Pioneer2/Lab area
                    val isPioneer2OrLab = isPioneer2OrLab(episode, areaId)

                    // Check if this is boss area
                    val isBoss = isBossArea(episode, areaId)
                    
                    // Only show omnispawn in regular areas
                    !isPioneer2OrLab && !isBoss
                }

                // Create the controls div conditionally
                div {
                    className = "pw-quest-editor-entity-list-controls"
                    style.display = "none"  // Hidden by default

                    observe(showOmnispawn) { show ->
                        style.display = if (show) "flex" else "none"
                    }

                    // Custom toggle switch for omnispawn
                    div {
                        className = "pw-omnispawn-toggle"

                        div {
                            className = "pw-toggle-switch"

                            observe(store.omnispawn) { isChecked ->
                                if (isChecked) {
                                    classList.add("pw-toggle-switch-on")
                                } else {
                                    classList.remove("pw-toggle-switch-on")
                                }
                            }

                            onclick = { store.setOmnispawn(!store.omnispawn.value) }

                            div {
                                className = "pw-toggle-switch-handle"
                            }
                        }

                        div {
                            className = "pw-toggle-label"
                            textContent = "Omnispawn"

                            div {
                                className = "pw-toggle-description"
                                textContent = "Show all monsters from all episodes"
                            }
                        }
                    }
                }
            }

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

                .pw-quest-editor-entity-list-controls {
                    padding: 12px 16px;
                    border-bottom: 1px solid var(--pw-control-border-color, #ddd);
                    background: linear-gradient(135deg, var(--pw-control-bg-color, #f8f9fa) 0%, var(--pw-control-bg-color-hover, #e9ecef) 100%);
                    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
                    display: flex;
                    align-items: center;
                    gap: 8px;
                }

                .pw-omnispawn-toggle {
                    display: flex;
                    align-items: center;
                    gap: 12px;
                    cursor: pointer;
                    padding: 6px;
                    border-radius: 6px;
                    transition: background-color 0.2s ease;
                }

                .pw-omnispawn-toggle:hover {
                    background-color: var(--pw-control-bg-color-hover, rgba(0, 0, 0, 0.05));
                }

                .pw-toggle-switch {
                    position: relative;
                    width: 44px;
                    height: 24px;
                    background-color: var(--pw-control-bg-color, #ccc);
                    border: 1px solid var(--pw-control-border-color, #999);
                    border-radius: 12px;
                    cursor: pointer;
                    transition: all 0.3s ease;
                    box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.2);
                }

                .pw-toggle-switch:hover {
                    border-color: var(--pw-control-border-color-hover, #666);
                }

                .pw-toggle-switch-on {
                    background-color: #4CAF50;
                    border-color: #45a049;
                    box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.2), 0 0 0 2px rgba(76, 175, 80, 0.2);
                }

                .pw-toggle-switch-handle {
                    position: absolute;
                    top: 1px;
                    left: 1px;
                    width: 20px;
                    height: 20px;
                    background: linear-gradient(135deg, #ffffff 0%, #f0f0f0 100%);
                    border: 1px solid var(--pw-control-border-color, #ddd);
                    border-radius: 50%;
                    transition: all 0.3s ease;
                    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
                }

                .pw-toggle-switch-on .pw-toggle-switch-handle {
                    transform: translateX(20px);
                    background: linear-gradient(135deg, #ffffff 0%, #f8f8f8 100%);
                    border-color: #fff;
                    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
                }

                .pw-toggle-label {
                    display: flex;
                    flex-direction: column;
                    gap: 2px;
                    font-family: var(--pw-font-family), sans-serif;
                    font-size: 13px;
                    font-weight: 600;
                    color: var(--pw-control-text-color, #333);
                    line-height: 1.2;
                }

                .pw-toggle-description {
                    font-size: 11px;
                    font-weight: 400;
                    color: var(--pw-control-text-color-muted, #666);
                    line-height: 1.3;
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
