package world.phantasmal.webui.widgets

import world.phantasmal.observable.value.Val

enum class LabelPosition {
    Before,
    After
}

abstract class LabelledControl(
    visible: Val<Boolean>,
    enabled: Val<Boolean>,
    tooltip: Val<String?>,
    label: String?,
    labelVal: Val<String>?,
    val preferredLabelPosition: LabelPosition,
) : Control(visible, enabled, tooltip) {
    val label: Label? by lazy {
        if (label == null && labelVal == null) {
            null
        } else {
            Label(visible, enabled, label, labelVal, htmlFor = getId())
        }
    }

    protected abstract fun getId(): String
}
