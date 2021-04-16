package world.phantasmal.webui.widgets

import kotlinx.coroutines.launch
import org.w3c.dom.HTMLElement
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.nullVal
import world.phantasmal.observable.value.trueVal
import world.phantasmal.webui.dom.Icon
import world.phantasmal.webui.files.FileHandle
import world.phantasmal.webui.files.FileType
import world.phantasmal.webui.files.showOpenFilePicker

class FileButton(
    visible: Val<Boolean> = trueVal(),
    enabled: Val<Boolean> = trueVal(),
    tooltip: Val<String?> = nullVal(),
    className: String? = null,
    text: String? = null,
    textVal: Val<String>? = null,
    iconLeft: Icon? = null,
    iconRight: Icon? = null,
    private val types: List<FileType> = emptyList(),
    private val multiple: Boolean = false,
    private val filesSelected: ((List<FileHandle>?) -> Unit)? = null,
) : Button(visible, enabled, tooltip, className, text, textVal, iconLeft, iconRight) {
    override fun interceptElement(element: HTMLElement) {
        element.classList.add("pw-file-button")

        if (filesSelected != null) {
            element.onclick = {
                scope.launch {
                    filesSelected.invoke(showOpenFilePicker(types, multiple))
                }
            }
        }
    }
}
