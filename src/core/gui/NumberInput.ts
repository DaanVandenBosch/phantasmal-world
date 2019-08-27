import { property } from "../observable";
import { Property } from "../observable/Property";
import { Input } from "./Input";
import "./NumberInput.css";

export class NumberInput extends Input<number> {
    readonly preferred_label_position = "left";

    private readonly rounding_factor: number;
    private rounded_value: number = 0;

    constructor(
        value: number = 0,
        options: {
            label?: string;
            min?: number | Property<number>;
            max?: number | Property<number>;
            step?: number | Property<number>;
            width?: number;
            round_to?: number;
        } = {},
    ) {
        super(
            property(value),
            "core_NumberInput",
            "number",
            "core_NumberInput_inner",
            options.label,
        );

        const { min, max, step } = options;
        this.set_attr("min", min, String);
        this.set_attr("max", max, String);
        this.set_attr("step", step, String);

        if (options.round_to != undefined && options.round_to >= 0) {
            this.rounding_factor = Math.pow(10, options.round_to);
        } else {
            this.rounding_factor = 1;
        }

        this.element.style.width = `${options.width == undefined ? 54 : options.width}px`;
    }

    protected input_value_changed(): boolean {
        return this.input.valueAsNumber !== this.rounded_value;
    }

    protected get_input_value(): number {
        return this.input.valueAsNumber;
    }

    protected set_input_value(value: number): void {
        this.input.valueAsNumber = this.rounded_value =
            Math.round(this.rounding_factor * value) / this.rounding_factor;
    }
}
