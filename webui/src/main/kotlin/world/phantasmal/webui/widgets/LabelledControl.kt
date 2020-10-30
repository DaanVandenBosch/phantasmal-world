package world.phantasmal.webui.widgets

import kotlinx.coroutines.CoroutineScope
import world.phantasmal.observable.value.Val

enum class LabelPosition {
    Before,
    After
}

abstract class LabelledControl(
    scope: CoroutineScope,
    hidden: Val<Boolean>,
    disabled: Val<Boolean>,
    tooltip: String? = null,
    label: String?,
    labelVal: Val<String>?,
    val preferredLabelPosition: LabelPosition,
) : Control(scope, hidden, disabled, tooltip) {
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
