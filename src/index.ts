import { ApplicationView } from "./application/gui/ApplicationView";
import { Disposable } from "./core/observable/Disposable";
import "./core/gui/index.css";
import { throttle } from "lodash";
import Logger from "js-logger";
import "@fortawesome/fontawesome-free/js/fontawesome";
import "@fortawesome/fontawesome-free/js/solid";
import "@fortawesome/fontawesome-free/js/regular";
import "@fortawesome/fontawesome-free/js/brands";

Logger.useDefaults({
    defaultLevel: (Logger as any)[process.env["LOG_LEVEL"] || "OFF"],
});

function initialize(): Disposable {
    // Disable native undo/redo.
    document.addEventListener("beforeinput", before_input);
    // Work-around for FireFox:
    document.addEventListener("keydown", keydown);

    // Disable native drag-and-drop.
    document.addEventListener("dragenter", dragenter);
    document.addEventListener("dragover", dragover);
    document.addEventListener("drop", drop);

    // Initialize view.
    const application_view = new ApplicationView();

    // Resize the view on window resize.
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

    // Dispose view and global event listeners when necessary.
    return {
        dispose(): void {
            window.removeEventListener("beforeinput", before_input);
            window.removeEventListener("keydown", keydown);
            window.removeEventListener("resize", resize);
            window.removeEventListener("dragenter", dragenter);
            window.removeEventListener("dragover", dragover);
            window.removeEventListener("drop", drop);
            application_view.dispose();
        },
    };
}

function before_input(e: Event): void {
    const ie = e as any;

    if (ie.inputType === "historyUndo" || ie.inputType === "historyRedo") {
        e.preventDefault();
    }
}

function keydown(e: Event): void {
    const kbe = e as KeyboardEvent;

    if (kbe.ctrlKey && !kbe.altKey && kbe.key.toUpperCase() === "Z") {
        kbe.preventDefault();
    }
}

function dragenter(e: DragEvent): void {
    e.preventDefault();

    if (e.dataTransfer) {
        e.dataTransfer.dropEffect = "none";
    }
}

function dragover(e: DragEvent): void {
    dragenter(e);
}

function drop(e: DragEvent): void {
    dragenter(e);
}

initialize();
