/* eslint-disable no-dupe-class-members */
import { LabelledControl, LabelledControlOptions } from "./LabelledControl";
import { create_element } from "./dom";
import { WritableProperty } from "../observable/WritableProperty";
import { is_any_property, Property } from "../observable/Property";
import "./Input.css";
import { WidgetProperty } from "../observable/WidgetProperty";

export type InputOptions = LabelledControlOptions;

export abstract class Input<T> extends LabelledControl {
    readonly element: HTMLElement;

    readonly value: WritableProperty<T>;

    protected readonly input: HTMLInputElement;

    private readonly _value: WidgetProperty<T>;
    private ignore_input_change = false;

    protected constructor(
        value: T,
        class_name: string,
        input_type: string,
        input_class_name: string,
        options?: InputOptions,
    ) {
        super(options);

        this._value = new WidgetProperty<T>(this, value, this.set_value);
        this.value = this._value;

        this.element = create_element("span", { class: `${class_name} core_Input` });

        this.input = create_element("input", {
            class: `${input_class_name} core_Input_inner`,
        });
        this.input.type = input_type;
        this.input.onchange = () => {
            this._value.val = this.get_value();
        };

        this.element.append(this.input);
    }

    protected set_enabled(enabled: boolean): void {
        super.set_enabled(enabled);
        this.input.disabled = !enabled;
    }

    protected abstract get_value(): T;

    protected abstract set_value(value: T): void;

    protected ignore_change(f: () => void): void {
        this.ignore_input_change = true;
        f();
    }

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
            this.disposable(value.observe(({ value }) => (input[attr] = cvt(value))));
        } else {
            input[attr] = cvt(value);
        }
    }
}

type InputAttrsOfType<T> = {
    [K in keyof HTMLInputElement]: T extends HTMLInputElement[K] ? K : never;
}[keyof HTMLInputElement];
