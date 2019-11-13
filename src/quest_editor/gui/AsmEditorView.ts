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

/**
 * Merge Monaco decorations into one.
 */
function merge_monaco_decorations(
    ...decos: readonly editor.IModelDeltaDecoration[]
): editor.IModelDeltaDecoration {
    if (decos.length === 0) {
        throw new Error("At least 1 argument is required.");
    }

    const merged: any = Object.assign({}, decos[0]);
    merged.options = Object.assign({}, decos[0].options);

    if (decos.length === 1) {
        return merged;
    }

    for (let i = 1; i < decos.length; i++) {
        const deco = decos[i];
        for (const key of Object.keys(deco.options)) {
            if (deco.options.hasOwnProperty(key)) {
                const val = (deco.options as any)[key];

                switch (typeof val) {
                    case "object":
                    case "boolean":
                    case "number":
                    case "string":
                        merged.options[key] = val;
                        break;
                    default:
                        break;
                }
            }
        }
    }

    return merged;
}
/**
 * Monaco doesn't normally support having more than one decoration per line.
 * This function enables multiple decorations per line by merging them.
 */
function update_monaco_decorations(
    editor: IStandaloneCodeEditor,
    deco_opts: editor.IModelDecorationOptions,
    ...line_nums: readonly number[]
): void {
    const old_decos: string[] = [];
    const delta_decos: editor.IModelDeltaDecoration[] = [];

    if (line_nums.length < 1) {
        return;
    }

    for (const line_num of line_nums) {
        const update_deco = {
            range: new Range(line_num, 0, line_num, 0),
            options: deco_opts,
        };

        const cur_decos = editor.getLineDecorations(line_num);
        if (cur_decos) {
            // save current decos for replacement
            for (const deco of cur_decos) {
                old_decos.push(deco.id);
            }

            // merge current and new decos
            delta_decos.push(merge_monaco_decorations(...cur_decos, update_deco));
        } else {
            // nothing to update on this line
            delta_decos.push(update_deco);
        }
    }

    // commit changes
    editor.deltaDecorations(old_decos, delta_decos);
}

export class AsmEditorView extends ResizableWidget {
    private readonly tool_bar_view = this.disposable(new AsmEditorToolBar());
    private readonly editor: IStandaloneCodeEditor;
    private readonly history: EditorHistory;

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
                    // remove
                    update_monaco_decorations(
                        this.editor,
                        {
                            glyphMarginClassName: null,
                            glyphMarginHoverMessage: null,
                        },
                        ...change.removed,
                    );

                    // add
                    update_monaco_decorations(
                        this.editor,
                        {
                            glyphMarginClassName: "quest_editor_AsmEditorView_breakpoint-enabled",
                            glyphMarginHoverMessage: {
                                value: "Breakpoint",
                            },
                        },
                        ...change.inserted,
                    );
                }
            }),

            asm_editor_store.execution_location.observe(e => {
                const old_line_num = e.old_value;
                const new_line_num = e.value;

                // unset old
                if (old_line_num !== undefined) {
                    update_monaco_decorations(
                        this.editor,
                        {
                            className: null,
                            isWholeLine: false,
                        },
                        old_line_num,
                    );
                }

                // set new
                if (new_line_num !== undefined) {
                    update_monaco_decorations(
                        this.editor,
                        {
                            className: "quest_editor_AsmEditorView_execution-location",
                            isWholeLine: true,
                        },
                        new_line_num,
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
