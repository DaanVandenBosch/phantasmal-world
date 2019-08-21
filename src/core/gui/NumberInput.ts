import "./NumberInput.css";
import "./Input.css";
import { create_el } from "./dom";
import { WritableProperty } from "../observable/WritableProperty";
import { property } from "../observable";
import { LabelledControl } from "./LabelledControl";
import { is_any_property, Property } from "../observable/Property";

export class NumberInput extends LabelledControl {
    readonly element = create_el("span", "core_NumberInput core_Input");

    readonly value: WritableProperty<number> = property(0);

    readonly preferred_label_position = "left";

    private readonly input: HTMLInputElement = create_el(
        "input",
        "core_NumberInput_inner core_Input_inner",
    );

    constructor(
        value = 0,
        label?: string,
        min: number | Property<number> = -Infinity,
        max: number | Property<number> = Infinity,
        step: number | Property<number> = 1,
    ) {
        super(label);

        this.input.type = "number";
        this.input.valueAsNumber = value;

        this.set_prop("min", min);
        this.set_prop("max", max);
        this.set_prop("step", step);

        this.input.onchange = () => this.value.set(this.input.valueAsNumber);

        this.element.append(this.input);

        this.disposables(
            this.value.observe(value => (this.input.valueAsNumber = value)),

            this.enabled.observe(enabled => {
                this.input.disabled = !enabled;

                if (enabled) {
                    this.element.classList.remove("disabled");
                } else {
                    this.element.classList.add("disabled");
                }
            }),
        );

        this.element.style.width = "50px";
    }

    private set_prop<T>(prop: "min" | "max" | "step", value: T | Property<T>): void {
        if (is_any_property(value)) {
            this.input[prop] = String(value.get());
            this.disposable(value.observe(v => (this.input[prop] = String(v))));
        } else {
            this.input[prop] = String(value);
        }
    }
}
