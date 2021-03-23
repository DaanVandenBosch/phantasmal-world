package world.phantasmal.web.viewer.widgets

import kotlinx.coroutines.launch
import org.w3c.dom.Node
import world.phantasmal.web.viewer.controllers.ViewerToolbarController
import world.phantasmal.webui.dom.Icon
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.*

class ViewerToolbar(private val ctrl: ViewerToolbarController) : Widget() {
    override fun Node.createElement() =
        div {
            className = "pw-viewer-toolbar"

            addChild(Toolbar(
                children = listOf(
                    FileButton(
                        text = "Open file...",
                        iconLeft = Icon.File,
                        accept = ".afs, .nj, .njm, .xj, .xvm",
                        multiple = true,
                        filesSelected = { files -> scope.launch { ctrl.openFiles(files) } },
                    ),
                    Checkbox(
                        label = "Show skeleton",
                        checked = ctrl.showSkeleton,
                        onChange = ctrl::setShowSkeleton,
                    ),
                )
            ))
            addDisposable(ResultDialog(
                visible = ctrl.resultDialogVisible,
                result = ctrl.result,
                message = ctrl.resultMessage,
                onDismiss = ctrl::dismissResultDialog,
            ))
        }
}
