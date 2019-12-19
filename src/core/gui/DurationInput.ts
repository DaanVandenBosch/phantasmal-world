import { Input, InputOptions } from "./Input";
import { Duration } from "luxon";
import "./DurationInput.css";

export type DurationInputOptions = InputOptions;

export class DurationInput extends Input<Duration> {
    readonly preferred_label_position = "left";

    constructor(value = Duration.fromMillis(0), options?: DurationInputOptions) {
        super(value, "core_DurationInput", "text", "core_DurationInput_inner", options);

        this.input_element.pattern = "(60|[0-5][0-9]):(60|[0-5][0-9])";

        this.set_value(value);

        this.finalize_construction();
    }

    protected get_value(): Duration {
        const str = this.input_element.value;

        if (this.input_element.validity.valid) {
            return Duration.fromObject({
                hours: parseInt(str.slice(0, 2), 10),
                minutes: parseInt(str.slice(3), 10),
            });
        } else {
            const colon_pos = str.indexOf(":");

            if (colon_pos === -1) {
                return Duration.fromObject({ minutes: parseInt(str, 10) });
            } else {
                return Duration.fromObject({
                    hours: parseInt(str.slice(0, colon_pos), 10),
                    minutes: parseInt(str.slice(colon_pos + 1), 10),
                });
            }
        }
    }

    protected set_value(value: Duration): void {
        this.input_element.value = value.toFormat("hh:mm");
    }
}
