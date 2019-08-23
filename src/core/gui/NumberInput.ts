import { property } from "../observable";
import { Property } from "../observable/Property";
import { Input } from "./Input";
import "./NumberInput.css"

export class NumberInput extends Input<number> {
    readonly preferred_label_position = "left";

    constructor(
        value: number = 0,
        options?: {
            label?: string;
            min?: number | Property<number>;
            max?: number | Property<number>;
            step?: number | Property<number>;
        },
    ) {
        super(
            property(value),
            "core_NumberInput",
            "number",
            "core_NumberInput_inner",
            options && options.label,
        );

        if (options) {
            const { min, max, step } = options;
            this.set_attr("min", min, String);
            this.set_attr("max", max, String);
            this.set_attr("step", step, String);
        }

        this.element.style.width = "54px";
    }

    protected get_input_value(): number {
        return this.input.valueAsNumber;
    }

    protected set_input_value(value: number): void {
        this.input.valueAsNumber = value;
    }
}
