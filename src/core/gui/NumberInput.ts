import { Property } from "../observable/property/Property";
import { Input, InputOptions } from "./Input";
import "./NumberInput.css";

export class NumberInput extends Input<number> {
    readonly preferred_label_position = "left";

    private readonly rounding_factor: number;

    constructor(
        value: number = 0,
        options: InputOptions & {
            label?: string;
            min?: number | Property<number>;
            max?: number | Property<number>;
            step?: number | Property<number>;
            width?: number;
            round_to?: number;
        } = {},
    ) {
        super(value, "core_NumberInput", "number", "core_NumberInput_inner", options);

        const { min, max, step } = options;
        this.set_attr("min", min, String);
        this.set_attr("max", max, String);
        this.input_element.step = "any";
        this.set_attr("step", step, String);

        if (options.round_to != undefined && options.round_to >= 0) {
            this.rounding_factor = Math.pow(10, options.round_to);
        } else {
            this.rounding_factor = 1;
        }

        this.element.style.width = `${options.width == undefined ? 54 : options.width}px`;

        this.set_value(value);

        this.finalize_construction();
    }

    protected get_value(): number {
        return this.input_element.valueAsNumber;
    }

    protected set_value(value: number): void {
        this.input_element.valueAsNumber =
            Math.round(this.rounding_factor * value) / this.rounding_factor;
    }
}
