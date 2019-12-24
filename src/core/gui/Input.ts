/* eslint-disable no-dupe-class-members */
import { LabelledControl, LabelledControlOptions } from "./LabelledControl";
import { create_element, el } from "./dom";
import { WritableProperty } from "../observable/property/WritableProperty";
import { is_any_property, Property } from "../observable/property/Property";
import "./Input.css";
import { WidgetProperty } from "../observable/property/WidgetProperty";

export type InputOptions = { readonly readonly?: boolean } & LabelledControlOptions;

export abstract class Input<T> extends LabelledControl {
    readonly element: HTMLElement;

    readonly value: WritableProperty<T>;

    protected readonly input_element: HTMLInputElement;

    private readonly _value: WidgetProperty<T>;

    protected constructor(
        value: T,
        class_name: string,
        input_type: string,
        input_class_name: string,
        options?: InputOptions,
    ) {
        super(options);

        this.element = el.span({ class: `${class_name} core_Input` });

        this._value = new WidgetProperty<T>(this, value, this.set_value);
        this.value = this._value;

        this.input_element = create_element("input", {
            class: `${input_class_name} core_Input_inner`,
        });
        this.input_element.type = input_type;
        this.input_element.addEventListener("change", () => {
            this._value.set_val(this.get_value(), { silent: false });
        });

        if (options) {
            if (options.readonly) {
                this.set_attr("readOnly", true);
            }
        }

        this.element.append(this.input_element);
    }

    protected set_enabled(enabled: boolean): void {
        super.set_enabled(enabled);
        this.input_element.disabled = !enabled;
    }

    protected abstract get_value(): T;

    protected abstract set_value(value: T): void;

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

        const input = this.input_element as any;
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
