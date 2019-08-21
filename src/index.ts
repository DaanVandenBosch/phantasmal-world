import { ApplicationView } from "./application/gui/ApplicationView";
import { Disposable } from "./core/observable/Disposable";
import "./index.css";
import { throttle } from "lodash";
import Logger from "js-logger";

Logger.useDefaults({
    defaultLevel: (Logger as any)[process.env["LOG_LEVEL"] || "OFF"],
});

// Disable native undo/redo.
document.addEventListener("keydown", e => {
    const kbe = e as KeyboardEvent;

    if (kbe.ctrlKey && !kbe.altKey && kbe.key.toUpperCase() === "Z") {
        kbe.preventDefault();
    }
});
// This doesn't work in FireFox:
document.addEventListener("beforeinput", e => {
    const ie = e as any;

    if (ie.inputType === "historyUndo" || ie.inputType === "historyRedo") {
        e.preventDefault();
    }
});

function initialize(): Disposable {
    const application_view = new ApplicationView();

    const resize = throttle(
        () => {
            application_view.resize(window.innerWidth, window.innerHeight);
        },
        100,
        { leading: true, trailing: true },
    );

    resize();
    document.body.append(application_view.element);
    window.addEventListener("resize", resize);

    return {
        dispose(): void {
            window.removeEventListener("resize", resize);
            application_view.dispose();
        },
    };
}

initialize();
