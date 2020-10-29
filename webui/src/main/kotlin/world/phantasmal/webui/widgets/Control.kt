package world.phantasmal.webui.widgets

import kotlinx.coroutines.CoroutineScope
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.falseVal

/**
 * Represents all widgets that allow for user interaction such as buttons, text inputs, combo boxes,
 * etc. Controls are typically leaf nodes and thus typically don't have children.
 */
abstract class Control(
    scope: CoroutineScope,
    hidden: Val<Boolean> = falseVal(),
    disabled: Val<Boolean> = falseVal(),
) : Widget(scope, hidden, disabled)
