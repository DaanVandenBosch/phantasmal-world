import "./NumberInput.css";
import "./Input.css";
import { create_el } from "./dom";
import { WritableProperty } from "../observable/WritableProperty";
import { property } from "../observable";
import { LabelledControl } from "./LabelledControl";
import { is_any_property, Property } from "../observable/Property";

export class NumberInput extends LabelledControl {
    readonly element: HTMLInputElement = create_el("input", "core_NumberInput core_Input");

    readonly value: WritableProperty<number> = property(0);

    readonly preferred_label_position = "left";

    constructor(
        value = 0,
        label?: string,
        min: number | Property<number> = -Infinity,
        max: number | Property<number> = Infinity,
        step: number | Property<number> = 1,
    ) {
        super(label);

        this.element.type = "number";
        this.element.valueAsNumber = value;
        this.element.style.width = "50px";

        this.set_prop("min", min);
        this.set_prop("max", max);
        this.set_prop("step", step);

        this.element.onchange = () => this.value.set(this.element.valueAsNumber);

        this.disposables(
            this.value.observe(value => (this.element.valueAsNumber = value)),

            this.enabled.observe(enabled => (this.element.disabled = !enabled)),
        );
    }

    private set_prop<T>(prop: "min" | "max" | "step", value: T | Property<T>): void {
        if (is_any_property(value)) {
            this.element[prop] = String(value.get());
            this.disposable(value.observe(v => (this.element[prop] = String(v))));
        } else {
            this.element[prop] = String(value);
        }
    }
}
