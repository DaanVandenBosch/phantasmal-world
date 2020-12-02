package world.phantasmal.webui.widgets

import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.nullVal
import world.phantasmal.observable.value.trueVal

/**
 * Represents all widgets that allow for user interaction such as buttons, text inputs, combo boxes,
 * etc. Controls are typically leaf nodes and thus typically don't have children.
 */
abstract class Control(
    visible: Val<Boolean> = trueVal(),
    enabled: Val<Boolean> = trueVal(),
    tooltip: Val<String?> = nullVal(),
) : Widget(visible, enabled, tooltip)
