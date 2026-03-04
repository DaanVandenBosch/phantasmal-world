package world.phantasmal.web.questEditor.widgets

import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.Node
import world.phantasmal.cell.Cell
import world.phantasmal.cell.cell
import world.phantasmal.web.application.Application
import world.phantasmal.webui.dom.*
import world.phantasmal.webui.widgets.Button
import world.phantasmal.webui.widgets.Dialog
import world.phantasmal.webui.widgets.Widget

class AboutDialog(
    visible: Cell<Boolean>,
    onDismiss: () -> Unit,
) : Dialog(
    visible = visible,
    title = cell("About"),
    content = {},
    footer = {},
    onDismiss = onDismiss,
) {
    init {
        // Override the dialog content
        val bodyElement = dialogElement.querySelector(".pw-dialog-body")
        bodyElement?.let { body ->
            body.innerHTML = ""
            val contentWidget = addDisposable(AboutDialogContent())
            body.appendChild(contentWidget.element)
        }

        // Override the footer
        val footerElement = dialogElement.querySelector(".pw-dialog-footer")
        footerElement?.let { footer ->
            footer.innerHTML = ""
            val closeButton = addDisposable(Button(
                text = "Close",
                onClick = { onDismiss() }
            ))
            footer.appendChild(closeButton.element)
        }

        // Set dialog size
        dialogElement.style.width = "450px"
    }

    companion object {
        init {
            @Suppress("CssUnusedSymbol", "CssUnresolvedCustomProperty")
            // language=css
            style("""
                .pw-about-dialog-content {
                    user-select: text;
                    cursor: default;
                }

                .pw-about-dialog-header {
                    display: flex;
                    align-items: center;
                    gap: 16px;
                    padding: 16px;
                    margin: -10px -10px 0 -10px;
                    background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
                    border-radius: 4px 4px 0 0;
                }

                .pw-about-dialog-logo {
                    width: 56px;
                    height: 56px;
                    background: linear-gradient(135deg, #e94560 0%, #ff6b6b 100%);
                    border-radius: 12px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-size: 24px;
                    font-weight: bold;
                    color: white;
                    text-shadow: 0 2px 4px rgba(0,0,0,0.3);
                    box-shadow: 0 4px 12px rgba(233, 69, 96, 0.4);
                }

                .pw-about-dialog-title-section {
                    flex: 1;
                }

                .pw-about-dialog-title {
                    font-size: 22px;
                    font-weight: bold;
                    color: #ffffff;
                    margin-bottom: 2px;
                }

                .pw-about-dialog-version {
                    font-size: 12px;
                    color: #e94560;
                    font-weight: 500;
                    background: rgba(233, 69, 96, 0.2);
                    padding: 2px 8px;
                    border-radius: 10px;
                    display: inline-block;
                }

                .pw-about-dialog-tagline {
                    text-align: center;
                    color: var(--pw-text-color);
                    opacity: 0.8;
                    font-size: 13px;
                    margin: 16px 0;
                    font-style: italic;
                }

                .pw-about-dialog-features {
                    display: grid;
                    grid-template-columns: 1fr 1fr;
                    gap: 10px;
                    margin: 16px 0;
                }

                .pw-about-dialog-feature {
                    display: flex;
                    align-items: center;
                    gap: 10px;
                    padding: 10px;
                    background: var(--pw-control-bg-color);
                    border-radius: 8px;
                    border: 1px solid var(--pw-border-color);
                    transition: border-color 0.2s, background-color 0.2s;
                }

                .pw-about-dialog-feature:hover {
                    border-color: #e94560;
                    background: rgba(233, 69, 96, 0.05);
                }

                .pw-about-dialog-feature-icon {
                    width: 32px;
                    height: 32px;
                    background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
                    border-radius: 6px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    color: #e94560;
                }

                .pw-about-dialog-feature-text {
                    flex: 1;
                    min-width: 0;
                }

                .pw-about-dialog-feature-title {
                    font-size: 12px;
                    font-weight: 600;
                    color: var(--pw-text-color);
                }

                .pw-about-dialog-feature-desc {
                    font-size: 10px;
                    color: var(--pw-text-color);
                    opacity: 0.6;
                }

                .pw-about-dialog-tech {
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    gap: 8px;
                    margin: 16px 0;
                    flex-wrap: wrap;
                }

                .pw-about-dialog-tech-label {
                    font-size: 11px;
                    color: var(--pw-text-color);
                    opacity: 0.5;
                }

                .pw-about-dialog-tech-item {
                    font-size: 11px;
                    padding: 3px 10px;
                    background: var(--pw-control-bg-color);
                    border: 1px solid var(--pw-border-color);
                    border-radius: 12px;
                    color: var(--pw-text-color);
                }

                .pw-about-dialog-links {
                    display: flex;
                    justify-content: center;
                    gap: 12px;
                    margin: 16px 0;
                }

                .pw-about-dialog-link {
                    display: flex;
                    align-items: center;
                    gap: 6px;
                    padding: 8px 16px;
                    background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
                    border-radius: 6px;
                    color: #ffffff !important;
                    text-decoration: none !important;
                    font-size: 12px;
                    font-weight: 500;
                    transition: transform 0.2s, box-shadow 0.2s;
                }

                .pw-about-dialog-link:hover {
                    transform: translateY(-2px);
                    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
                }

                .pw-about-dialog-credits {
                    text-align: center;
                    padding-top: 12px;
                    border-top: 1px solid var(--pw-border-color);
                    font-size: 11px;
                    color: var(--pw-text-color);
                    opacity: 0.5;
                    line-height: 1.6;
                }
            """.trimIndent())
        }
    }
}

private class AboutDialogContent : Widget() {
    override fun Node.createElement() =
        div {
            className = "pw-about-dialog-content"

            // Header with gradient background
            div {
                className = "pw-about-dialog-header"
                div {
                    className = "pw-about-dialog-logo"
                    textContent = "PW"
                }
                div {
                    className = "pw-about-dialog-title-section"
                    div {
                        className = "pw-about-dialog-title"
                        textContent = "Phantasmal World"
                    }
                    div {
                        className = "pw-about-dialog-version"
                        textContent = "v${Application.VERSION}"
                    }
                }
            }

            // Tagline
            div {
                className = "pw-about-dialog-tagline"
                textContent = "Professional Quest Editor & Tools for Phantasy Star Online"
            }

            // Features grid
            div {
                className = "pw-about-dialog-features"

                div {
                    className = "pw-about-dialog-feature"
                    div {
                        className = "pw-about-dialog-feature-icon"
                        icon(Icon.File)
                    }
                    div {
                        className = "pw-about-dialog-feature-text"
                        div {
                            className = "pw-about-dialog-feature-title"
                            textContent = "Quest Editor"
                        }
                        div {
                            className = "pw-about-dialog-feature-desc"
                            textContent = "Visual quest creation"
                        }
                    }
                }

                div {
                    className = "pw-about-dialog-feature"
                    div {
                        className = "pw-about-dialog-feature-icon"
                        icon(Icon.Eye)
                    }
                    div {
                        className = "pw-about-dialog-feature-text"
                        div {
                            className = "pw-about-dialog-feature-title"
                            textContent = "3D Viewer"
                        }
                        div {
                            className = "pw-about-dialog-feature-desc"
                            textContent = "Model & map preview"
                        }
                    }
                }

                div {
                    className = "pw-about-dialog-feature"
                    div {
                        className = "pw-about-dialog-feature-icon"
                        icon(Icon.Play)
                    }
                    div {
                        className = "pw-about-dialog-feature-text"
                        div {
                            className = "pw-about-dialog-feature-title"
                            textContent = "Script Tools"
                        }
                        div {
                            className = "pw-about-dialog-feature-desc"
                            textContent = "ASM editor & debugger"
                        }
                    }
                }

                div {
                    className = "pw-about-dialog-feature"
                    div {
                        className = "pw-about-dialog-feature-icon"
                        icon(Icon.ArrowRight)
                    }
                    div {
                        className = "pw-about-dialog-feature-text"
                        div {
                            className = "pw-about-dialog-feature-title"
                            textContent = "Hunt Optimizer"
                        }
                        div {
                            className = "pw-about-dialog-feature-desc"
                            textContent = "Drop rate analysis"
                        }
                    }
                }
            }

            // Tech stack
            div {
                className = "pw-about-dialog-tech"
                span {
                    className = "pw-about-dialog-tech-label"
                    textContent = "Built with"
                }
                span {
                    className = "pw-about-dialog-tech-item"
                    textContent = "Kotlin/JS"
                }
                span {
                    className = "pw-about-dialog-tech-item"
                    textContent = "Three.js"
                }
                span {
                    className = "pw-about-dialog-tech-item"
                    textContent = "Monaco"
                }
            }

            // Links
            div {
                className = "pw-about-dialog-links"
                appendHtmlEl<HTMLAnchorElement>("A") {
                    className = "pw-about-dialog-link"
                    href = "https://github.com/DaanVandenBosch/phantasmal-world"
                    target = "_blank"
                    icon(Icon.GitHub)
                    span { textContent = "Source Code" }
                }
            }

            // Credits
            div {
                className = "pw-about-dialog-credits"
                div {
                    textContent = "Originally created by DaanVandenBosch"
                }
                div {
                    textContent = "Maintained by the PSO community"
                }
            }
        }
}