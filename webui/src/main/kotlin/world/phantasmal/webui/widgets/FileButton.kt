package world.phantasmal.webui.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.HTMLElement
import org.w3c.files.File
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.falseVal
import world.phantasmal.observable.value.nullVal
import world.phantasmal.observable.value.trueVal
import world.phantasmal.webui.dom.Icon
import world.phantasmal.webui.openFiles

class FileButton(
    scope: CoroutineScope,
    visible: Val<Boolean> = trueVal(),
    enabled: Val<Boolean> = trueVal(),
    tooltip: Val<String?> = nullVal(),
    text: String? = null,
    textVal: Val<String>? = null,
    iconLeft: Icon? = null,
    iconRight: Icon? = null,
    private val accept: String = "",
    private val multiple: Boolean = false,
    private val filesSelected: ((List<File>) -> Unit)? = null,
) : Button(scope, visible, enabled, tooltip, text, textVal, iconLeft, iconRight) {
    override fun interceptElement(element: HTMLElement) {
        element.classList.add("pw-file-button")

        if (filesSelected != null) {
            element.onclick = {
                openFiles(accept, multiple, filesSelected)
            }
        }
    }
}
