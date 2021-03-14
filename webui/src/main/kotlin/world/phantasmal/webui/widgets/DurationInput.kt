package world.phantasmal.webui.widgets

import org.w3c.dom.HTMLInputElement
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.nullVal
import world.phantasmal.observable.value.trueVal
import world.phantasmal.webui.formatAsHoursAndMinutes
import kotlin.time.Duration
import kotlin.time.minutes

class DurationInput(
    visible: Val<Boolean> = trueVal(),
    enabled: Val<Boolean> = trueVal(),
    tooltip: Val<String?> = nullVal(),
    label: String? = null,
    labelVal: Val<String>? = null,
    preferredLabelPosition: LabelPosition = LabelPosition.Before,
    value: Val<Duration>,
    onChange: (Duration) -> Unit = {},
) : Input<Duration>(
    visible,
    enabled,
    tooltip,
    label,
    labelVal,
    preferredLabelPosition,
    className = "pw-duration-input",
    value,
    onChange,
) {
    override fun interceptInputElement(input: HTMLInputElement) {
        super.interceptInputElement(input)

        input.type = "text"
        input.classList.add("pw-duration-input-inner")
        input.pattern = "(60|[0-5][0-9]):(60|[0-5][0-9])"
    }

    override fun getInputValue(input: HTMLInputElement): Duration {
        if (':' in input.value) {
            val (hoursStr, minutesStr) = input.value.split(':', limit = 2)
            val hours = hoursStr.toIntOrNull()
            val minutes = minutesStr.toIntOrNull()

            if (hours != null && minutes != null) {
                return (hours * 60 + minutes).minutes
            }
        }

        return input.value.toIntOrNull()?.minutes ?: Duration.ZERO
    }

    override fun setInputValue(input: HTMLInputElement, value: Duration) {
        input.value = value.formatAsHoursAndMinutes()
    }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-duration-input-inner {
                    text-align: center;
                }
            """.trimIndent())
        }
    }
}
