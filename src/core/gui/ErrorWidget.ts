import { ResizableWidget } from "./ResizableWidget";
import "./ErrorWidget.css";
import { div } from "./dom";
import { Widget } from "./Widget";
import { Label } from "./Label";

export class ErrorWidget extends ResizableWidget {
    private readonly label: Label;

    readonly element = div({ className: "core_ErrorWidget" });
    readonly children: readonly Widget[] = [];

    constructor(message: string) {
        super();

        this.label = this.disposable(new Label(message, { enabled: false }));

        this.element.append(this.label.element);

        this.finalize_construction();
    }
}
