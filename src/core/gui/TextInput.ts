import { Input } from "./Input";
import { Property } from "../observable/Property";
import { property } from "../observable";

export class TextInput extends Input<string> {
    readonly preferred_label_position = "left";

    constructor(
        value = "",
        options?: {
            label?: string;
            max_length?: number | Property<number>;
        },
    ) {
        super(
            property(value),
            "core_TextInput",
            "text",
            "core_TextInput_inner",
            options && options.label,
        );

        if (options) {
            const { max_length } = options;
            this.set_attr("maxLength", max_length);
        }
    }

    protected get_input_value(): string {
        return this.input.value;
    }

    protected set_input_value(value: string): void {
        this.input.value = value;
    }
}
