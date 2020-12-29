package world.phantasmal.webui.widgets

import world.phantasmal.observable.value.Val

/**
 * Represents all widgets that allow for user interaction such as buttons, text inputs, combo boxes,
 * etc. Controls are typically leaf nodes and thus typically don't have children.
 */
abstract class Control(
    visible: Val<Boolean>,
    enabled: Val<Boolean>,
    tooltip: Val<String?>,
) : Widget(visible, enabled, tooltip)
