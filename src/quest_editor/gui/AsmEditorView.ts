import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { el } from "../../core/gui/dom";
import { editor, KeyCode, KeyMod, Range } from "monaco-editor";
import { asm_editor_store } from "../stores/AsmEditorStore";
import { AsmEditorToolBar } from "./AsmEditorToolBar";
import { EditorHistory } from "./EditorHistory";
import IStandaloneCodeEditor = editor.IStandaloneCodeEditor;
import "./AsmEditorView.css";
import { ListChangeType } from "../../core/observable/property/list/ListProperty";

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
    private readonly history: EditorHistory;
    private editor_decoration_ids: string[] = [];

    readonly element = el.div();

    constructor() {
        super();

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
                glyphMargin: true,
            }),
        );

        this.history = this.disposable(new EditorHistory(this.editor));

        // Commands and actions.
        this.editor.addCommand(KeyMod.CtrlCmd | KeyCode.KEY_Z, () => {});

        this.editor.addCommand(KeyMod.CtrlCmd | KeyMod.Shift | KeyCode.KEY_Z, () => {});

        const quick_command = this.editor.getAction("editor.action.quickCommand");

        this.disposables(
            this.editor.addAction({
                id: "editor.action.quickCommand",
                label: "Command Palette",
                keybindings: [KeyMod.CtrlCmd | KeyMod.Shift | KeyCode.KEY_P],
                run: () => quick_command.run(),
            }),
        );

        // Undo/Redo
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
                    this.history.reset();
                },
                { call_now: true },
            ),

            asm_editor_store.breakpoints.observe_list(change => {
                if (change.type === ListChangeType.ListChange) {
                    // update breakpoint icons
                    this.editor_decoration_ids = this.editor.deltaDecorations(
                        this.editor_decoration_ids,
                        asm_editor_store.breakpoints.val.map(line_num => ({
                            range: new Range(line_num, 0, line_num, 0),
                            options: {
                                glyphMarginClassName:
                                    "quest_editor_AsmEditorView_breakpoint-enabled",
                                glyphMarginHoverMessage: {
                                    value: "Breakpoint"
                                }
                            },
                        })),
                    );
                }
            }),

            this.editor.onDidFocusEditorWidget(() => asm_editor_store.undo.make_current()),

            this.editor.onMouseDown(e => {
                switch (e.target.type) {
                    case editor.MouseTargetType.GUTTER_GLYPH_MARGIN:
                        const pos = e.target.position;
                        if (!pos) {
                            return;
                        }
                        asm_editor_store.toggle_breakpoint(pos.lineNumber);
                        break;
                    default:
                        break;
                }
            }),
        );

        this.finalize_construction(AsmEditorView.prototype);
    }

    focus(): void {
        this.editor.focus();
    }

    resize(width: number, height: number): this {
        const editor_height = Math.max(0, height - this.tool_bar_view.height);
        this.editor.layout({ width, height: editor_height });
        return this;
    }
}
