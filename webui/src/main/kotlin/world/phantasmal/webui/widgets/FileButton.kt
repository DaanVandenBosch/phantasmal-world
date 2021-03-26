package world.phantasmal.webui.widgets

import kotlinx.coroutines.launch
import org.w3c.dom.HTMLElement
import org.w3c.files.File
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.nullVal
import world.phantasmal.observable.value.trueVal
import world.phantasmal.webui.dom.Icon
import world.phantasmal.webui.selectFiles

class FileButton(
    visible: Val<Boolean> = trueVal(),
    enabled: Val<Boolean> = trueVal(),
    tooltip: Val<String?> = nullVal(),
    className: String? = null,
    text: String? = null,
    textVal: Val<String>? = null,
    iconLeft: Icon? = null,
    iconRight: Icon? = null,
    private val accept: String = "",
    private val multiple: Boolean = false,
    private val filesSelected: ((List<File>) -> Unit)? = null,
) : Button(visible, enabled, tooltip, className, text, textVal, iconLeft, iconRight) {
    override fun interceptElement(element: HTMLElement) {
        element.classList.add("pw-file-button")

        if (filesSelected != null) {
            element.onclick = {
                scope.launch {
                    filesSelected.invoke(selectFiles(accept, multiple))
                }
            }
        }
    }
}
