package world.phantasmal.webui.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.Node
import world.phantasmal.core.Failure
import world.phantasmal.core.PwResult
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.trueVal
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.dom.dom
import world.phantasmal.webui.dom.li
import world.phantasmal.webui.dom.ul

/**
 * Shows the details of a result if the result failed or succeeded with problems. Shows a "Dismiss"
 * button in the footer which triggers [onDismiss].
 */
class ResultDialog(
    scope: CoroutineScope,
    visible: Val<Boolean> = trueVal(),
    enabled: Val<Boolean> = trueVal(),
    result: Val<PwResult<*>?>,
    message: Val<String>,
    onDismiss: () -> Unit = {},
) : Dialog(
    scope,
    visible,
    enabled,
    title = result.map(::resultToTitle),
    description = message,
    content = result.map(::resultToContent),
    onDismiss,
) {
    override fun addFooterContent(footer: Node) {
        footer.addChild(Button(
            scope,
            visible,
            enabled,
            text = "Dismiss",
            onClick = { onDismiss() }
        ))
    }

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

private fun resultToTitle(result: PwResult<*>?): String =
    when {
        result is Failure -> "Error"
        result?.problems?.isNotEmpty() == true -> "Problems"
        else -> ""
    }

private fun resultToContent(result: PwResult<*>?): Node =
    dom {
        div {
            className = "pw-result-dialog-result"

            result?.let {
                ul {
                    className = "pw-result-dialog-problems"

                    result.problems.map {
                        li { textContent = it.uiMessage }
                    }
                }
            }
        }
    }
