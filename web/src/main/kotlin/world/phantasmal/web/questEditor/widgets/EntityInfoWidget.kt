package world.phantasmal.web.questEditor.widgets

import kotlinx.coroutines.launch
import org.w3c.dom.Node
import world.phantasmal.observable.value.Val
import world.phantasmal.web.core.widgets.UnavailableWidget
import world.phantasmal.web.questEditor.controllers.EntityInfoController
import world.phantasmal.webui.dom.*
import world.phantasmal.webui.widgets.DoubleInput
import world.phantasmal.webui.widgets.IntInput
import world.phantasmal.webui.widgets.Widget

class EntityInfoWidget(private val ctrl: EntityInfoController) : Widget(enabled = ctrl.enabled) {
    override fun Node.createElement() =
        div {
            className = "pw-quest-editor-entity-info"
            tabIndex = -1

            addEventListener("focus", { ctrl.focused() }, true)

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
                    val sectionInput = IntInput(
                        enabled = ctrl.enabled,
                        value = ctrl.sectionId,
                        onChange = { scope.launch { ctrl.setSectionId(it) } },
                        label = "Section:",
                        min = 0,
                        step = 0,
                    )
                    th { addChild(sectionInput.label!!) }
                    td { addChild(sectionInput) }
                }
                tr {
                    hidden(ctrl.waveHidden)

                    val waveInput = IntInput(
                        enabled = ctrl.enabled,
                        value = ctrl.waveId,
                        onChange = ctrl::setWaveId,
                        label = "Wave:",
                        min = 0,
                        step = 0,
                    )
                    th { addChild(waveInput.label!!) }
                    td { addChild(waveInput) }
                }
                tr {
                    th { colSpan = 2; textContent = "Position:" }
                }
                createCoordRow("X:", ctrl.posX, ctrl::setPosX)
                createCoordRow("Y:", ctrl.posY, ctrl::setPosY)
                createCoordRow("Z:", ctrl.posZ, ctrl::setPosZ)
                tr {
                    th { colSpan = 2; textContent = "Rotation:" }
                }
                createCoordRow("X:", ctrl.rotX, ctrl::setRotX)
                createCoordRow("Y:", ctrl.rotY, ctrl::setRotY)
                createCoordRow("Z:", ctrl.rotZ, ctrl::setRotZ)
            }
            addChild(UnavailableWidget(
                visible = ctrl.unavailable,
                message = "No entity selected.",
            ))
        }

    private fun Node.createCoordRow(label: String, value: Val<Double>, onChange: (Double) -> Unit) {
        tr {
            className = COORD_CLASS

            val input = DoubleInput(
                enabled = ctrl.enabled,
                value = value,
                onChange = onChange,
                label = label,
                roundTo = 3,
            )
            th {
                addChild(input.label!!)
            }
            td {
                addChild(input)
            }
        }
    }

    companion object {
        @Suppress("CssInvalidHtmlTagReference")
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

                .pw-quest-editor-entity-info .$COORD_CLASS th {
                    padding-left: 10px;
                }

                .pw-quest-editor-entity-info .$COORD_CLASS .pw-number-input {
                    width: 100%;
                }

                .pw-quest-editor-entity-info table.pw-quest-editor-entity-info-specific-props {
                    margin-top: -2px;
                }
            """.trimIndent())
        }
    }
}
