package world.phantasmal.web.viewer.widgets

import kotlinx.coroutines.launch
import org.w3c.dom.Node
import world.phantasmal.web.viewer.controllers.ViewerController
import world.phantasmal.web.viewer.controllers.ViewerTab
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.TabContainer
import world.phantasmal.webui.widgets.Widget

class ViewerWidget(
    private val ctrl: ViewerController,
    private val createToolbar: () -> Widget,
    private val createCharacterClassOptionsWidget: () -> CharacterClassOptionsWidget,
    private val createMeshWidget: () -> Widget,
    private val createTextureWidget: () -> Widget,
) : Widget() {
    override fun Node.createElement() =
        div {
            className = "pw-viewer-viewer"

            addChild(createToolbar())

            div {
                className = "pw-viewer-viewer-content"

                addChild(SelectionWidget(
                    ctrl.characterClasses,
                    ctrl.currentCharacterClass,
                    { char -> scope.launch { ctrl.setCurrentCharacterClass(char) } },
                    { it.uiName },
                ))
                addChild(createCharacterClassOptionsWidget())
                addChild(TabContainer(ctrl = ctrl, createWidget = { tab ->
                    when (tab) {
                        ViewerTab.Mesh -> createMeshWidget()
                        ViewerTab.Texture -> createTextureWidget()
                    }
                }))
                addChild(SelectionWidget(
                    ctrl.animations,
                    ctrl.currentAnimation,
                    { animation -> scope.launch { ctrl.setCurrentAnimation(animation) } },
                    { it.name },
                    borderLeft = true,
                ))
            }
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-viewer-viewer {
                    display: flex;
                    flex-direction: column;
                }
                
                .pw-viewer-viewer-content {
                    flex-grow: 1;
                    display: flex;
                    flex-direction: row;
                    overflow: hidden;
                }
                
                .pw-viewer-viewer-content > .pw-tab-container {
                    flex-grow: 1;
                }
            """.trimIndent())
        }
    }
}
