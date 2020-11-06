package world.phantasmal.web.viewer.widgets

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.w3c.dom.Node
import world.phantasmal.web.viewer.controller.ViewerToolbarController
import world.phantasmal.webui.dom.Icon
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.FileButton
import world.phantasmal.webui.widgets.Toolbar
import world.phantasmal.webui.widgets.Widget

class ViewerToolbar(
    scope: CoroutineScope,
    private val ctrl: ViewerToolbarController,
) : Widget(scope) {
    override fun Node.createElement() =
        div {
            className = "pw-viewer-toolbar"

            addChild(Toolbar(
                scope,
                children = listOf(
                    FileButton(
                        scope,
                        text = "Open file...",
                        iconLeft = Icon.File,
                        accept = ".afs, .nj, .njm, .xj, .xvm",
                        multiple = true,
                        filesSelected = { files -> scope.launch { ctrl.openFiles(files) } }
                    )
                )
            ))
        }
}
