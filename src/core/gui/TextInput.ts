import { Input, InputOptions } from "./Input";
import { Property } from "../observable/Property";

export type TextInputOptions = InputOptions & {
    max_length?: number | Property<number>;
};

export class TextInput extends Input<string> {
    readonly preferred_label_position = "left";

    constructor(value = "", options?: TextInputOptions) {
        super(value, "core_TextInput", "text", "core_TextInput_inner", options);

        if (options) {
            const { max_length } = options;
            this.set_attr("maxLength", max_length);
        }

        this.set_value(value);
    }

    protected get_value(): string {
        return this.input.value;
    }

    protected set_value(value: string): void {
        this.input.value = value;
    }
}
