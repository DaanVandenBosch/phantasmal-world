import { ResizableWidget } from "./ResizableWidget";
import { UnavailableView } from "../../quest_editor/gui/UnavailableView";
import "./ErrorView.css";
import { div } from "./dom";

export class ErrorView extends ResizableWidget {
    readonly element: HTMLElement;

    constructor(message: string) {
        super();

        this.element = div(
            { className: "core_ErrorView" },
            this.disposable(new UnavailableView(message)).element,
        );

        this.finalize_construction();
    }
}
