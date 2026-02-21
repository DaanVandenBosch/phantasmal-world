package world.phantasmal.web.questEditor.widgets

import kotlinx.browser.window
import org.w3c.dom.Node
import world.phantasmal.cell.Cell
import world.phantasmal.cell.cell
import world.phantasmal.cell.map
import world.phantasmal.psolib.compatibility.CompatibilityResult
import world.phantasmal.psolib.compatibility.ProblemType
import world.phantasmal.web.questEditor.controllers.CompatibilityController
import world.phantasmal.web.questEditor.controllers.CompatibilityStatus
import world.phantasmal.webui.dom.*
import world.phantasmal.webui.widgets.Button
import world.phantasmal.webui.widgets.Dialog
import world.phantasmal.webui.widgets.Widget

class CompatibilityDialog(
    visible: Cell<Boolean>,
    private val ctrl: CompatibilityController,
    onDismiss: () -> Unit,
) : Dialog(
    visible = visible,
    title = cell("Compatibility Check"),
    description = cell("Check quest compatibility with different PSO versions"),
    content = {},
    footer = {},
    onDismiss = onDismiss,
) {
    init {
        // Override the dialog content
        val bodyElement = dialogElement.querySelector(".pw-dialog-body")
        bodyElement?.let { body ->
            body.innerHTML = ""

            // Create content widget and add to disposable chain
            val contentWidget = addDisposable(CompatibilityDialogContent(ctrl))
            body.appendChild(contentWidget.element)
        }

        // Override the footer
        val footerElement = dialogElement.querySelector(".pw-dialog-footer")
        footerElement?.let { footer ->
            footer.innerHTML = ""

            val closeButton = addDisposable(
                Button(
                text = "Close",
                onClick = { onDismiss() }
            ))
            footer.appendChild(closeButton.element)
        }

        // Set dialog size
        dialogElement.style.width = "600px"
        dialogElement.style.maxHeight = "600px"
    }

    companion object {
        init {
            @Suppress("CssUnusedSymbol", "CssUnresolvedCustomProperty")
            // language=css
            (style(
                """
                        .pw-compatibility-dialog-content {
                            display: flex;
                            flex-direction: column;
                            gap: 8px;
                            height: 400px;
                        }
        
                        .pw-compatibility-dialog-header {
                            display: flex;
                            gap: 8px;
                        }
        
                        .pw-compatibility-dialog-versions {
                            display: flex;
                            flex-direction: column;
                            gap: 4px;
                            max-height: 200px;
                            overflow-y: auto;
                        }
        
                        .pw-compatibility-dialog-version {
                            display: flex;
                            justify-content: space-between;
                            padding: 6px 8px;
                            border-radius: 4px;
                            cursor: pointer;
                            background-color: var(--pw-control-bg-color);
                            border: 1px solid var(--pw-control-border-color);
                        }
        
                        .pw-compatibility-dialog-version:hover {
                            background-color: var(--pw-control-bg-color-hover);
                        }
        
                        .pw-compatibility-dialog-version.compatible {
                            border-left: 3px solid hsl(120, 60%, 40%);
                        }
        
                        .pw-compatibility-dialog-version.warning {
                            border-left: 3px solid hsl(45, 80%, 50%);
                        }
        
                        .pw-compatibility-dialog-version.incompatible {
                            border-left: 3px solid hsl(0, 70%, 50%);
                        }
        
                        .pw-compatibility-dialog-version.not-checked {
                            border-left: 3px solid hsl(0, 0%, 50%);
                        }
        
                        .pw-compatibility-dialog-version-name {
                            font-weight: bold;
                        }
        
                        .pw-compatibility-dialog-version-status {
                            color: var(--pw-text-color-secondary);
                            font-size: 12px;
                        }
        
                        .pw-compatibility-dialog-details {
                            flex: 1;
                            overflow: auto;
                            background-color: var(--pw-bg-color);
                            border: 1px solid var(--pw-control-border-color);
                            border-radius: 4px;
                            padding: 8px;
                        }
        
                        .pw-compatibility-dialog-details-header {
                            display: flex;
                            justify-content: space-between;
                            align-items: center;
                            margin-bottom: 8px;
                        }
        
                        .pw-compatibility-dialog-details-header h4 {
                            margin: 0;
                        }
        
                        .pw-compatibility-dialog-details h5 {
                            margin: 12px 0 4px 0;
                        }
        
                        .pw-compatibility-dialog-no-selection {
                            color: var(--pw-text-color-secondary);
                            font-style: italic;
                        }
        
                        .pw-compatibility-dialog-success {
                            color: hsl(120, 60%, 50%);
                            font-weight: bold;
                        }
        
                        .pw-compatibility-dialog-errors li,
                        .pw-compatibility-dialog-warnings li {
                            margin: 4px 0;
                            font-size: 12px;
                        }
        
                        .pw-compatibility-dialog-errors {
                            color: hsl(0, 70%, 60%);
                        }
        
                        .pw-compatibility-dialog-warnings {
                            color: hsl(45, 80%, 60%);
                        }
        
                        .pw-compatibility-problem-type {
                            font-weight: bold;
                            margin-right: 4px;
                            color: var(--pw-text-color-secondary);
                        }
                    """.trimIndent()
            ))
        }
    }
}

private class CompatibilityDialogContent(private val ctrl: CompatibilityController) : Widget() {
    override fun Node.createElement() =
        div {
            className = "pw-compatibility-dialog-content"

            div {
                className = "pw-compatibility-dialog-header"

                addChild(
                    Button(
                    text = "Check All Versions",
                    enabled = ctrl.unavailable.map { !it },
                    onClick = { ctrl.checkAllVersions() }
                ))

                addChild(
                    Button(
                    text = "Clear",
                    enabled = ctrl.results.map { it.isNotEmpty() },
                    onClick = { ctrl.clearResults() }
                ))
            }

            div {
                className = "pw-compatibility-dialog-versions"

                observe(ctrl.versionSummaries) { summaries ->
                    innerHTML = ""
                    summaries.forEach { summary ->
                        div {
                            className = "pw-compatibility-dialog-version"
                            classList.toggle("compatible", summary.status == CompatibilityStatus.COMPATIBLE)
                            classList.toggle("warning", summary.status == CompatibilityStatus.WARNING)
                            classList.toggle("incompatible", summary.status == CompatibilityStatus.INCOMPATIBLE)
                            classList.toggle("not-checked", summary.status == CompatibilityStatus.NOT_CHECKED)

                            onclick = { ctrl.selectVersion(summary.version) }

                            span {
                                className = "pw-compatibility-dialog-version-name"
                                textContent = summary.version.displayName
                            }

                            span {
                                className = "pw-compatibility-dialog-version-status"
                                textContent = when (summary.status) {
                                    CompatibilityStatus.NOT_CHECKED -> "Not checked"
                                    CompatibilityStatus.COMPATIBLE -> "Compatible"
                                    CompatibilityStatus.WARNING -> "${summary.warningCount} warning(s)"
                                    CompatibilityStatus.INCOMPATIBLE -> "${summary.errorCount} error(s), ${summary.warningCount} warning(s)"
                                }
                            }
                        }
                    }
                }
            }

            div {
                className = "pw-compatibility-dialog-details"

                observe(ctrl.selectedResult) { result ->
                    innerHTML = ""

                    if (result == null) {
                        span {
                            className = "pw-compatibility-dialog-no-selection"
                            textContent = "Select a version to view details"
                        }
                    } else {
                        div {
                            className = "pw-compatibility-dialog-details-header"
                            h4 { textContent = result.version.displayName }

                            if (result.errors.isNotEmpty() || result.warnings.isNotEmpty()) {
                                addChild(
                                    Button(
                                    text = "Copy",
                                    onClick = { copyResultToClipboard(result) }
                                ))
                            }
                        }

                        if (result.errors.isEmpty() && result.warnings.isEmpty()) {
                            p {
                                className = "pw-compatibility-dialog-success"
                                textContent = "Fully compatible!"
                            }
                        } else {
                            if (result.errors.isNotEmpty()) {
                                h5 { textContent = "Errors (${result.errors.size})" }
                                ul {
                                    className = "pw-compatibility-dialog-errors"
                                    result.errors.forEach { error ->
                                        li {
                                            span {
                                                className = "pw-compatibility-problem-type"
                                                textContent = formatProblemType(error.type)
                                            }
                                            span { textContent = error.message }
                                        }
                                    }
                                }
                            }

                            if (result.warnings.isNotEmpty()) {
                                h5 { textContent = "Warnings (${result.warnings.size})" }
                                ul {
                                    className = "pw-compatibility-dialog-warnings"
                                    result.warnings.forEach { warning ->
                                        li {
                                            span {
                                                className = "pw-compatibility-problem-type"
                                                textContent = formatProblemType(warning.type)
                                            }
                                            span { textContent = warning.message }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    private fun copyResultToClipboard(result: CompatibilityResult) {
        val lines = mutableListOf<String>()
        lines.add(result.version.displayName)

        if (result.errors.isNotEmpty()) {
            lines.add("")
            lines.add("Errors:")
            result.errors.forEach { error ->
                lines.add("${formatProblemType(error.type)} ${error.message}")
            }
        }

        if (result.warnings.isNotEmpty()) {
            lines.add("")
            lines.add("Warnings:")
            result.warnings.forEach { warning ->
                lines.add("${formatProblemType(warning.type)} ${warning.message}")
            }
        }

        val text = lines.joinToString("\n")
        window.navigator.clipboard.writeText(text)
    }

    private fun formatProblemType(type: ProblemType): String = when (type) {
        ProblemType.MISSING_LABEL_0 -> "[Script]"
        ProblemType.UNKNOWN_OPCODE -> "[Script]"
        ProblemType.OPCODE_VERSION_MISMATCH -> "[Script]"
        ProblemType.EPISODE_NOT_SUPPORTED -> "[Episode]"
        ProblemType.INVALID_ARGUMENT -> "[Script]"
        ProblemType.LABEL_NOT_FOUND -> "[Script]"
        ProblemType.SWITCH_ARRAY_MISMATCH -> "[Script]"
        ProblemType.CONVERSION_WARNING -> "[Script]"
        ProblemType.SPECIAL_OPCODE_WARNING -> "[Script]"
        ProblemType.NPC_ACTION_LABEL_NOT_FOUND -> "[NPC]"
        ProblemType.NPC_SCRIPT_LABEL_NOT_SUPPORTED -> "[NPC]"
        ProblemType.SKIN_NOT_SUPPORTED -> "[NPC]"
        ProblemType.SKIN_51_INVALID_SUBTYPE -> "[NPC]"
        ProblemType.MONSTER_FLOOR_MISMATCH -> "[Monster]"
        ProblemType.TOO_MANY_MONSTERS -> "[Monster]"
        ProblemType.OBJECT_FLOOR_MISMATCH -> "[Object]"
        ProblemType.TOO_MANY_OBJECTS -> "[Object]"
        ProblemType.UNUSED_DATA_LABEL -> "[Data]"
        ProblemType.MISSING_BIN_FILE -> "[File]"
        ProblemType.MISSING_DAT_FILE -> "[File]"
        ProblemType.PVR_NOT_SUPPORTED -> "[File]"
    }
}