package world.phantasmal.webui.widgets

import kotlinx.coroutines.launch
import org.w3c.dom.HTMLElement
import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.nullCell
import world.phantasmal.observable.cell.trueCell
import world.phantasmal.webui.dom.Icon
import world.phantasmal.webui.files.FileHandle
import world.phantasmal.webui.files.FileType
import world.phantasmal.webui.files.showOpenFilePicker

class FileButton(
    visible: Cell<Boolean> = trueCell(),
    enabled: Cell<Boolean> = trueCell(),
    tooltip: Cell<String?> = nullCell(),
    className: String? = null,
    text: String? = null,
    textCell: Cell<String>? = null,
    iconLeft: Icon? = null,
    iconRight: Icon? = null,
    private val types: List<FileType> = emptyList(),
    private val multiple: Boolean = false,
    private val filesSelected: ((List<FileHandle>?) -> Unit)? = null,
) : Button(visible, enabled, tooltip, className, text, textCell, iconLeft, iconRight) {
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
