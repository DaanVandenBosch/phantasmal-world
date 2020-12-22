package world.phantasmal.webui.widgets

import org.w3c.dom.Node
import world.phantasmal.core.Failure
import world.phantasmal.core.PwResult
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.emptyStringVal
import world.phantasmal.observable.value.trueVal
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.dom.li
import world.phantasmal.webui.dom.ul

/**
 * Shows the details of a result if the result failed or succeeded with problems. Shows a "Dismiss"
 * button in the footer which triggers onDismiss.
 */
class ResultDialog(
    visible: Val<Boolean> = trueVal(),
    enabled: Val<Boolean> = trueVal(),
    result: Val<PwResult<*>?>,
    message: Val<String> = emptyStringVal(),
    onDismiss: () -> Unit = {},
) : Widget(visible, enabled) {
    private val dialog = addDisposable(
        Dialog(
            visible,
            enabled,
            title = result.map { result ->
                when {
                    result is Failure -> "Error"
                    result?.problems?.isNotEmpty() == true -> "Problems"
                    else -> ""
                }
            },
            description = message,
            content = {
                div {
                    className = "pw-result-dialog-result"

                    ul {
                        className = "pw-result-dialog-problems"
                        hidden(result.isNull())

                        bindChildrenTo(result.map {
                            it?.problems ?: emptyList()
                        }) { problem, _ ->
                            li { textContent = problem.uiMessage }
                        }
                    }
                }
            },
            footer = {
                addChild(Button(
                    visible,
                    enabled,
                    text = "Dismiss",
                    onClick = { onDismiss() }
                ))
            },
            onDismiss = onDismiss,
        )
    )

    override fun Node.createElement() = dialog.element

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-result-dialog-result {
                    overflow: auto;
                    user-select: text;
                    cursor: text;
                    height: 100%;
                    max-height: 400px; /* Workaround for chrome bug. */
                }
            """.trimIndent())
        }
    }
}
