package world.phantasmal.web.questEditor.widgets

import org.w3c.dom.Node
import world.phantasmal.web.core.widgets.UnavailableWidget
import world.phantasmal.web.questEditor.controllers.EntityInfoController
import world.phantasmal.webui.dom.*
import world.phantasmal.webui.widgets.DoubleInput
import world.phantasmal.webui.widgets.Widget

class EntityInfoWidget(private val ctrl: EntityInfoController) : Widget(enabled = ctrl.enabled) {
    override fun Node.createElement() =
        div {
            className = "pw-quest-editor-entity-info"
            tabIndex = -1

            table {
                hidden(ctrl.unavailable)

                tr {
                    th { textContent = "Type:" }
                    td { text(ctrl.type) }
                }
                tr {
                    th { textContent = "Name:" }
                    td { text(ctrl.name) }
                }
                tr {
                    th { textContent = "Section:" }
                    td { text(ctrl.sectionId) }
                }
                tr {
                    hidden(ctrl.waveHidden)

                    th { textContent = "Wave:" }
                    td { text(ctrl.wave) }
                }
                tr {
                    th { colSpan = 2; textContent = "Position:" }
                }
                tr {
                    th { className = COORD_CLASS; textContent = "X:" }
                    td {
                        addChild(DoubleInput(
                            enabled = ctrl.enabled,
                            value = ctrl.posX,
                            onChange = ctrl::setPosX,
                            roundTo = 3,
                        ))
                    }
                }
                tr {
                    th { className = COORD_CLASS; textContent = "Y:" }
                    td {
                        addChild(DoubleInput(
                            enabled = ctrl.enabled,
                            value = ctrl.posY,
                            onChange = ctrl::setPosY,
                            roundTo = 3,
                        ))
                    }
                }
                tr {
                    th { className = COORD_CLASS; textContent = "Z:" }
                    td {
                        addChild(DoubleInput(
                            enabled = ctrl.enabled,
                            value = ctrl.posZ,
                            onChange = ctrl::setPosZ,
                            roundTo = 3,
                        ))
                    }
                }
                tr {
                    th { colSpan = 2; textContent = "Rotation:" }
                }
                tr {
                    th { className = COORD_CLASS; textContent = "X:" }
                    td {
                        addChild(DoubleInput(
                            enabled = ctrl.enabled,
                            value = ctrl.rotX,
                            onChange = ctrl::setRotX,
                            roundTo = 3,
                        ))
                    }
                }
                tr {
                    th { className = COORD_CLASS; textContent = "Y:" }
                    td {
                        addChild(DoubleInput(
                            enabled = ctrl.enabled,
                            value = ctrl.rotY,
                            onChange = ctrl::setRotY,
                            roundTo = 3,
                        ))
                    }
                }
                tr {
                    th { className = COORD_CLASS; textContent = "Z:" }
                    td {
                        addChild(DoubleInput(
                            enabled = ctrl.enabled,
                            value = ctrl.rotZ,
                            onChange = ctrl::setRotZ,
                            roundTo = 3,
                        ))
                    }
                }
            }
            addChild(UnavailableWidget(
                visible = ctrl.unavailable,
                message = "No entity selected.",
            ))
        }

    override fun focus() {
        super.focus()
        ctrl.focused()
    }

    companion object {
        private const val COORD_CLASS = "pw-quest-editor-entity-info-coord"

        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-quest-editor-entity-info {
                    outline: none;
                    box-sizing: border-box;
                    padding: 3px;
                    overflow: auto;
                }

                .pw-quest-editor-entity-info table {
                    table-layout: fixed;
                    width: 100%;
                    margin: 0 auto;
                }

                .pw-quest-editor-entity-info th {
                    text-align: left;
                }

                .pw-quest-editor-entity-info th.pw-quest-editor-entity-info-coord {
                    padding-left: 10px;
                }

                .pw-quest-editor-entity-info .pw-number-input {
                    width: 100%;
                }

                .pw-quest-editor-entity-info table.pw-quest-editor-entity-info-specific-props {
                    margin-top: -2px;
                }
            """.trimIndent())
        }
    }
}
