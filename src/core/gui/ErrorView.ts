import { ResizableWidget } from "./ResizableWidget";
import { el } from "./dom";
import { UnavailableView } from "../../quest_editor/gui/UnavailableView";
import "./ErrorView.css";

export class ErrorView extends ResizableWidget {
    readonly element: HTMLElement;

    constructor(message: string) {
        super();

        this.element = el.div(
            {
                class: "core_ErrorView",
            },
            this.disposable(new UnavailableView(message)).element,
        );
    }
}
