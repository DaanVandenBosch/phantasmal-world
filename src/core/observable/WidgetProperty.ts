import { SimpleProperty } from "./SimpleProperty";
import { Widget } from "../gui/Widget";

export class WidgetProperty<T> extends SimpleProperty<T> {
    constructor(private widget: Widget, val: T, private set_value: (this: Widget, val: T) => void) {
        super(val);
    }

    set_val(val: T, options?: { silent?: boolean }): void {
        this.set_value.call(this.widget, val);
        super.set_val(val, options);
    }
}
