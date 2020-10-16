package world.phantasmal.webui.widgets

import world.phantasmal.core.disposable.Scope
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.falseVal

/**
 * Represents all widgets that allow for user interaction such as buttons, text inputs, combo boxes,
 * etc. Controls are typically leaf nodes and thus typically don't have children.
 */
abstract class Control(
    scope: Scope,
    style: () -> String,
    hidden: Val<Boolean> = falseVal(),
    disabled: Val<Boolean> = falseVal(),
) : Widget(scope, style, hidden, disabled)
