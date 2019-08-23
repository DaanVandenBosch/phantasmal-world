/* eslint-disable no-dupe-class-members */
import { LabelledControl } from "./LabelledControl";
import { create_element } from "./dom";
import { WritableProperty } from "../observable/WritableProperty";
import { is_any_property, Property } from "../observable/Property";
import "./Input.css";

export abstract class Input<T> extends LabelledControl {
    readonly element: HTMLElement;

    readonly value: WritableProperty<T>;

    protected readonly input: HTMLInputElement;

    protected constructor(
        value: WritableProperty<T>,
        class_name: string,
        input_type: string,
        input_class_name: string,
        label?: string,
    ) {
        super(label);

        this.value = value;

        this.element = create_element("span", { class: `${class_name} core_Input` });

        this.input = create_element("input", {
            class: `${input_class_name} core_Input_inner`,
        });
        this.input.type = input_type;
        this.input.onchange = () => (this.value.val = this.get_input_value());
        this.set_input_value(value.val);

        this.element.append(this.input);

        this.disposables(
            this.value.observe(value => this.set_input_value(value)),

            this.enabled.observe(enabled => {
                this.input.disabled = !enabled;

                if (enabled) {
                    this.element.classList.remove("disabled");
                } else {
                    this.element.classList.add("disabled");
                }
            }),
        );
    }

    protected abstract get_input_value(): T;

    protected abstract set_input_value(value: T): void;

    protected set_attr<T>(attr: InputAttrsOfType<T>, value?: T | Property<T>): void;
    protected set_attr<T, U>(
        attr: InputAttrsOfType<U>,
        value: T | Property<T> | undefined,
        convert: (value: T) => U,
    ): void;
    protected set_attr<T, U>(
        attr: InputAttrsOfType<U>,
        value?: T | Property<T>,
        convert?: (value: T) => U,
    ): void {
        if (value == undefined) return;

        const input = this.input as any;
        const cvt = convert ? convert : (v: T) => (v as any) as U;

        if (is_any_property(value)) {
            input[attr] = cvt(value.val);
            this.disposable(value.observe(v => (input[attr] = cvt(v))));
        } else {
            input[attr] = cvt(value);
        }
    }
}

type InputAttrsOfType<T> = {
    [K in keyof HTMLInputElement]: T extends HTMLInputElement[K] ? K : never;
}[keyof HTMLInputElement];
