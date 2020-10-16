package world.phantasmal.webui.widgets

import world.phantasmal.core.disposable.Scope
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.falseVal

enum class LabelPosition {
    Before,
    After
}

abstract class LabelledControl(
    scope: Scope,
    style: () -> String,
    hidden: Val<Boolean> = falseVal(),
    disabled: Val<Boolean> = falseVal(),
    label: String? = null,
    labelVal: Val<String>? = null,
    val preferredLabelPosition: LabelPosition,
) : Control(scope, style, hidden, disabled) {
    val label: Label? by lazy {
        if (label == null && labelVal == null) {
            null
        } else {
            var id = element.id

            if (id.isBlank()) {
                id = uniqueId()
                element.id = id
            }

            Label(scope, hidden, disabled, label, labelVal, htmlFor = id)
        }
    }

    companion object {
        private var id = 0

        private fun uniqueId() = "pw-labelled-control-id-${id++}"
    }
}
