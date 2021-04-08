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
                        accept = ".afs, .nj, .njm, .rel, .xj, .xvm",
                        multiple = true,
                        filesSelected = { files -> scope.launch { ctrl.openFiles(files) } },
                    ),
                    Checkbox(
                        label = "Show skeleton",
                        enabled = ctrl.showSkeletonEnabled,
                        checked = ctrl.showSkeleton,
                        onChange = ctrl::setShowSkeleton,
                    ),
                    Checkbox(
                        label = "Play animation",
                        enabled = ctrl.animationControlsEnabled,
                        checked = ctrl.playAnimation,
                        onChange = ctrl::setPlayAnimation,
                    ),
                    IntInput(
                        label = "Frame rate:",
                        enabled = ctrl.animationControlsEnabled,
                        value = ctrl.frameRate,
                        onChange = ctrl::setFrameRate,
                        min = 1,
                        max = 240,
                        step = 1,
                    ),
                    IntInput(
                        label = "Frame:",
                        enabled = ctrl.animationControlsEnabled,
                        value = ctrl.frame,
                        onChange = ctrl::setFrame,
                        step = 1,
                    ),
                    Label(
                        enabled = ctrl.animationControlsEnabled,
                        textVal = ctrl.maxFrame,
                    ),
                    Button(
                        className = "pw-viewer-toolbar-clear-animation",
                        text = "Clear animation",
                        enabled = ctrl.animationControlsEnabled,
                        onClick = { scope.launch { ctrl.clearCurrentAnimation() } },
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

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-viewer-toolbar > .pw-toolbar > .pw-viewer-toolbar-clear-animation {
                    margin-left: 6px;
                }
            """.trimIndent())
        }
    }
}
