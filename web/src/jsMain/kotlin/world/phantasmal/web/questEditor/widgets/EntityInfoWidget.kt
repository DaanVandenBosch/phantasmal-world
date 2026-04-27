package world.phantasmal.web.questEditor.widgets

import kotlinx.browser.window
import kotlinx.coroutines.launch
import org.w3c.dom.Node
import world.phantasmal.cell.Cell
import world.phantasmal.cell.cell
import world.phantasmal.cell.mutableCell
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.web.core.widgets.UnavailableWidget
import world.phantasmal.web.questEditor.controllers.EntityInfoController
import world.phantasmal.web.questEditor.controllers.EntityInfoPropModel
import world.phantasmal.webui.dom.*
import world.phantasmal.webui.widgets.*

class EntityInfoWidget(private val ctrl: EntityInfoController) : Widget(enabled = ctrl.enabled) {

    override fun Node.createElement() =
        div {
            className = "pw-quest-editor-entity-info"
            tabIndex = -1

            addEventListener("focus", { ctrl.focused() }, true)

            table {
                hidden(ctrl.unavailable)

                tr {
                    th { textContent = "Skin:" }
                    td { text(ctrl.id) }
                }
                tr {
                    th { textContent = "Type:" }
                    td { text(ctrl.type) }
                }
                tr {
                    th { textContent = "Name:" }
                    td { text(ctrl.name) }
                }
                tr {
                    hidden(ctrl.appearFlagHidden)

                    th { textContent = "Appear Flag:" }
                    td { text(ctrl.appearFlag) }
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
                    th { colSpan = 2; textContent = "World Position:" }
                }
                createCoordRow("X:", ctrl.worldPosX, ctrl::setWorldPosX)
                createCoordRow("Y:", ctrl.worldPosY, ctrl::setWorldPosY)
                createCoordRow("Z:", ctrl.worldPosZ, ctrl::setWorldPosZ)
                tr {
                    th { colSpan = 2; textContent = "Rotation:" }
                }
                createCoordRow("X:", ctrl.rotX, ctrl::setRotX)
                createCoordRow("Y:", ctrl.rotY, ctrl::setRotY)
                createCoordRow("Z:", ctrl.rotZ, ctrl::setRotZ)
            }
            table {
                className = "pw-quest-editor-entity-info-specific-props"
                hidden(ctrl.unavailable)

                bindDisposableChildrenTo(ctrl.props) { prop, _ -> createPropRow(prop) }
            }
            addChild(
                UnavailableWidget(
                    visible = ctrl.unavailable,
                    message = "No entity selected.",
                )
            )
        }

    private fun Node.createCoordRow(
        label: String,
        value: Cell<Double>,
        onChange: (Double) -> Unit,
    ) {
        tr {
            className = COORD_CLASS

            val inputValue = mutableCell(value.value)
            var timeout = -1

            observeNow(value) {
                if (timeout == -1) {
                    timeout = window.setTimeout({
                        inputValue.value = value.value
                        timeout = -1
                    })
                }
            }

            val input = DoubleInput(
                enabled = ctrl.enabled,
                value = inputValue,
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

    private fun Node.createPropRow(prop: EntityInfoPropModel): Pair<Node, Disposable> {
        val disposer = Disposer()

        // Use a Select widget for Color properties on fence types.
        val colorSelect = when (prop) {
            is EntityInfoPropModel.I32 -> prop.colorOptions?.let { options ->
                disposer.add(
                    Select(
                        enabled = ctrl.enabled,
                        label = prop.label,
                        items = cell(options),
                        selected = prop.selectedColor,
                        onSelect = { option -> prop.setValue(option.value) },
                    )
                )
            }

            is EntityInfoPropModel.F32 -> prop.colorOptions?.let { options ->
                disposer.add(
                    Select(
                        enabled = ctrl.enabled,
                        label = prop.label,
                        items = cell(options),
                        selected = prop.selectedColor,
                        onSelect = { option -> prop.setValue(option.value.toDouble()) },
                    )
                )
            }

            else -> null
        }

        if (colorSelect != null) {
            val node = tr {
                th { addWidget(disposer.add(colorSelect.label!!), addToDisposer = false) }
                td { addWidget(colorSelect, addToDisposer = false) }
            }
            return Pair(node, disposer)
        }

        val input = disposer.add(
            when (prop) {
                is EntityInfoPropModel.I32 -> IntInput(
                    enabled = ctrl.enabled,
                    label = prop.label,
                    min = Int.MIN_VALUE,
                    max = Int.MAX_VALUE,
                    step = 1,
                    value = prop.value,
                    onChange = prop::setValue,
                )
                is EntityInfoPropModel.F32 -> DoubleInput(
                    enabled = ctrl.enabled,
                    label = prop.label,
                    roundTo = 3,
                    value = prop.value,
                    onChange = prop::setValue,
                )
                is EntityInfoPropModel.Angle -> DoubleInput(
                    enabled = ctrl.enabled,
                    label = prop.label,
                    roundTo = 1,
                    value = prop.value,
                    onChange = prop::setValue,
                )
            }
        )

        val node = tr {
            if (prop.isScriptLabel) {
                style.cursor = "pointer"
                title = "Double-click to go to script label"
                addEventListener("dblclick", {
                    val labelId = when (prop) {
                        is EntityInfoPropModel.I32 -> prop.value.value
                        is EntityInfoPropModel.F32 -> prop.value.value.toInt()
                        else -> return@addEventListener
                    }
                    ctrl.goToScriptLabel(labelId)
                })
            }

            th {
                addWidget(disposer.add(input.label!!), addToDisposer = false)
            }
            td {
                addWidget(input, addToDisposer = false)
            }

            if (prop is EntityInfoPropModel.I32 && prop.showGoToEvent) {
                td {
                    addWidget(
                        disposer.add(Button(
                            enabled = prop.canGoToEvent,
                            tooltip = cell("Go to event"),
                            iconLeft = Icon.ArrowRight,
                            onClick = { e ->
                                e.stopPropagation()
                                prop.goToEvent()
                            }
                        )),
                        addToDisposer = false,
                    )
                }
            }

            if (prop.isScriptLabel) {
                td {
                    addWidget(
                        disposer.add(Button(
                            tooltip = cell("Go to script label"),
                            iconLeft = Icon.ArrowRight,
                            onClick = { e ->
                                e.stopPropagation()
                                val labelId = when (prop) {
                                    is EntityInfoPropModel.I32 -> prop.value.value
                                    is EntityInfoPropModel.F32 -> prop.value.value.toInt()
                                    else -> return@Button
                                }
                                ctrl.goToScriptLabel(labelId)
                            }
                        )),
                        addToDisposer = false,
                    )
                }
            }
        }

        return Pair(node, disposer)
    }

    companion object {
        @Suppress("CssInvalidHtmlTagReference")
        private const val COORD_CLASS = "pw-quest-editor-entity-info-coord"

        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style(
                """
                .pw-quest-editor-entity-info {
                    outline: none;
                    box-sizing: border-box;
                    padding: 3px;
                    overflow: auto;
                }

                .pw-quest-editor-entity-info table {
                    width: 100%;
                    margin: 0 auto;
                }

                .pw-quest-editor-entity-info th {
                    text-align: left;
                    width: 50%;
                }

                .pw-quest-editor-entity-info .$COORD_CLASS th {
                    padding-left: 10px;
                }

                .pw-quest-editor-entity-info .$COORD_CLASS .pw-number-input {
                    width: 100%;
                }

                /* Using a selector with high specificity to ensure we override rule above. */
                .pw-quest-editor-entity-info table.pw-quest-editor-entity-info-specific-props {
                    margin-top: -2px;
                }

                .pw-quest-editor-entity-info-specific-props .pw-number-input {
                    width: 100%;
                }
            """.trimIndent()
            )
        }
    }
}
