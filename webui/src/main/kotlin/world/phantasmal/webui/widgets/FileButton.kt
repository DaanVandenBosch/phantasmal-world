package world.phantasmal.webui.widgets

import org.w3c.dom.HTMLElement
import org.w3c.files.File
import world.phantasmal.core.disposable.Scope
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.falseVal
import world.phantasmal.webui.openFiles

class FileButton(
    scope: Scope,
    hidden: Val<Boolean> = falseVal(),
    disabled: Val<Boolean> = falseVal(),
    text: String? = null,
    textVal: Val<String>? = null,
    private val accept: String = "",
    private val multiple: Boolean = false,
    private val filesSelected: ((List<File>) -> Unit)? = null,
) : Button(scope, hidden, disabled, text, textVal) {
    override fun interceptElement(element: HTMLElement) {
        element.classList.add("pw-file-button")

        if (filesSelected != null) {
            element.onclick = {
                openFiles(accept, multiple, filesSelected)
            }
        }
    }
}