import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { el } from "../../core/gui/dom";
import { editor } from "monaco-editor";
import { asm_editor_store } from "../stores/AsmEditorStore";
import IStandaloneCodeEditor = editor.IStandaloneCodeEditor;
import { AsmEditorToolBar } from "./AsmEditorToolBar";

editor.defineTheme("phantasmal-world", {
    base: "vs-dark",
    inherit: true,
    rules: [
        { token: "", foreground: "e0e0e0", background: "#181818" },
        { token: "tag", foreground: "99bbff" },
        { token: "keyword", foreground: "d0a0ff", fontStyle: "bold" },
        { token: "predefined", foreground: "bbffbb" },
        { token: "number", foreground: "ffffaa" },
        { token: "number.hex", foreground: "ffffaa" },
        { token: "string", foreground: "88ffff" },
        { token: "string.escape", foreground: "8888ff" },
    ],
    colors: {
        "editor.background": "#181818",
        "editor.lineHighlightBackground": "#202020",
    },
});

const DUMMY_MODEL = editor.createModel("", "psoasm");

export class AsmEditorView extends ResizableWidget {
    private readonly tool_bar_view = this.disposable(new AsmEditorToolBar());

    private readonly editor: IStandaloneCodeEditor;

    constructor() {
        super(el.div());

        this.element.append(this.tool_bar_view.element);

        this.editor = this.disposable(
            editor.create(this.element, {
                theme: "phantasmal-world",
                scrollBeyondLastLine: false,
                autoIndent: true,
                fontSize: 13,
                wordBasedSuggestions: false,
                wordWrap: "on",
                wrappingIndent: "indent",
                renderIndentGuides: false,
                folding: false,
            }),
        );

        this.disposables(
            asm_editor_store.did_undo.observe(({ value: source }) => {
                this.editor.trigger(source, "undo", undefined);
            }),

            asm_editor_store.did_redo.observe(({ value: source }) => {
                this.editor.trigger(source, "redo", undefined);
            }),

            asm_editor_store.model.observe(
                ({ value: model }) => {
                    this.editor.updateOptions({ readOnly: model == undefined });
                    this.editor.setModel(model || DUMMY_MODEL);
                },
                { call_now: true },
            ),

            this.editor.onDidFocusEditorWidget(() => asm_editor_store.undo.make_current()),
        );

        this.finalize_construction(AsmEditorView.prototype);
    }

    focus(): void {
        this.editor.focus();
    }

    resize(width: number, height: number): this {
        this.editor.layout({ width, height });
        return this;
    }
}
