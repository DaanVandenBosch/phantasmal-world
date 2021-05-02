package world.phantasmal.webui.widgets

import world.phantasmal.observable.cell.Cell

/**
 * Represents all widgets that allow for user interaction such as buttons, text inputs, combo boxes,
 * etc. Controls are typically leaf nodes and thus typically don't have children.
 */
abstract class Control(
    visible: Cell<Boolean>,
    enabled: Cell<Boolean>,
    tooltip: Cell<String?>,
) : Widget(visible, enabled, tooltip)
