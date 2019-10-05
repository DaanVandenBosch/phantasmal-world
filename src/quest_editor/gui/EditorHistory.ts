import { Disposable } from "../../core/observable/Disposable";
import { editor, IPosition, KeyCode, KeyMod } from "monaco-editor";
import { Disposer } from "../../core/observable/Disposer";
import IStandaloneCodeEditor = editor.IStandaloneCodeEditor;
import ICursorPositionChangedEvent = editor.ICursorPositionChangedEvent;
import IEditorMouseEvent = editor.IEditorMouseEvent;
import ScrollType = editor.ScrollType;

export class EditorHistory implements Disposable {
    private readonly history: IPosition[] = [];
    private history_index = -1;
    private capture_history = true;
    private readonly disposer = new Disposer();

    constructor(private readonly editor: IStandaloneCodeEditor) {
        this.disposer.add_all(
            this.editor.onDidChangeCursorPosition(this.did_change_cursor_position),

            this.editor.addAction({
                id: "phantasmal.action.back",
                label: "Back",
                keybindings: [KeyMod.Alt | KeyCode.LeftArrow],
                run: this.back,
            }),

            this.editor.addAction({
                id: "phantasmal.action.forward",
                label: "Forward",
                keybindings: [KeyMod.Alt | KeyCode.RightArrow],
                run: this.forward,
            }),

            this.editor.onMouseUp(this.mouse_up),
        );
    }

    dispose(): void {
        this.disposer.dispose();
    }

    reset(): void {
        this.history.splice(0, Infinity);
        this.history_index = -1;
    }

    private did_change_cursor_position = (e: ICursorPositionChangedEvent): void => {
        if (!this.capture_history) return;

        this.history.splice(this.history_index + 1, Infinity);

        if (
            e.source === "api" ||
            this.history_index === -1 ||
            Math.abs(e.position.lineNumber - this.history[this.history_index].lineNumber) >= 10
        ) {
            this.history.push(e.position);
            this.history_index++;
        } else {
            this.history[this.history_index] = e.position;
        }
    };

    private back = async (): Promise<void> => {
        if (this.history_index > 0) {
            this.set_position(this.history[--this.history_index]);
        }
    };

    private forward = async (): Promise<void> => {
        if (this.history_index + 1 < this.history.length) {
            this.set_position(this.history[++this.history_index]);
        }
    };

    private set_position = (position: IPosition): void => {
        this.capture_history = false;
        this.editor.setPosition(position);
        this.editor.revealPositionInCenterIfOutsideViewport(position, ScrollType.Immediate);
        this.capture_history = true;
    };

    private mouse_up = (e: IEditorMouseEvent): void => {
        const button = e.event.browserEvent.button;
        const buttons = e.event.browserEvent.buttons;

        if (button === 3) {
            if (buttons === 0) {
                e.event.preventDefault();
                this.back();
            }

            this.editor.focus();
        } else if (button === 4) {
            if (buttons === 0) {
                e.event.preventDefault();
                this.forward();
            }

            this.editor.focus();
        }
    };
}
