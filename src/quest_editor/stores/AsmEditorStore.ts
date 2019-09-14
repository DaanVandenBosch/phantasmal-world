import { editor, languages, MarkerSeverity, MarkerTag, Position } from "monaco-editor";
import { AssemblyAnalyser } from "../scripting/AssemblyAnalyser";
import { Disposable } from "../../core/observable/Disposable";
import { Disposer } from "../../core/observable/Disposer";
import { SimpleUndo } from "../../core/undo/SimpleUndo";
import { QuestModel } from "../model/QuestModel";
import { quest_editor_store } from "./QuestEditorStore";
import { ASM_SYNTAX } from "./asm_syntax";
import { AssemblyError, AssemblyWarning } from "../scripting/assembly";
import { Observable } from "../../core/observable/Observable";
import { emitter, property } from "../../core/observable";
import { WritableProperty } from "../../core/observable/property/WritableProperty";
import { Property } from "../../core/observable/property/Property";
import SignatureHelp = languages.SignatureHelp;
import ITextModel = editor.ITextModel;
import CompletionList = languages.CompletionList;
import IMarkerData = editor.IMarkerData;

const assembly_analyser = new AssemblyAnalyser();

languages.register({ id: "psoasm" });

languages.setMonarchTokensProvider("psoasm", ASM_SYNTAX);

languages.registerCompletionItemProvider("psoasm", {
    provideCompletionItems(model, position): CompletionList {
        const text = model.getValueInRange({
            startLineNumber: position.lineNumber,
            endLineNumber: position.lineNumber,
            startColumn: 1,
            endColumn: position.column,
        });
        return assembly_analyser.provide_completion_items(text);
    },
});

languages.registerSignatureHelpProvider("psoasm", {
    signatureHelpTriggerCharacters: [" ", ","],

    signatureHelpRetriggerCharacters: [", "],

    provideSignatureHelp(
        _model: ITextModel,
        position: Position,
    ): Promise<SignatureHelp | undefined> {
        return assembly_analyser.provide_signature_help(position.lineNumber, position.column);
    },
});

languages.setLanguageConfiguration("psoasm", {
    indentationRules: {
        increaseIndentPattern: /^\s*\d+:/,
        decreaseIndentPattern: /^\s*(\d+|\.)/,
    },
    autoClosingPairs: [{ open: '"', close: '"' }],
    surroundingPairs: [{ open: '"', close: '"' }],
    comments: {
        lineComment: "//",
    },
});

export class AsmEditorStore implements Disposable {
    readonly model: Property<ITextModel | undefined>;
    readonly did_undo: Observable<string>;
    readonly did_redo: Observable<string>;
    readonly undo = new SimpleUndo(
        "Text edits",
        () => this._did_undo.emit({ value: "asm undo" }),
        () => this._did_redo.emit({ value: "asm undo" }),
    );

    private readonly disposer = new Disposer();
    private readonly model_disposer = this.disposer.add(new Disposer());
    private readonly _model: WritableProperty<ITextModel | undefined> = property(undefined);
    private readonly _did_undo = emitter<string>();
    private readonly _did_redo = emitter<string>();

    constructor() {
        this.model = this._model;
        this.did_undo = this._did_undo;
        this.did_redo = this._did_redo;

        this.disposer.add_all(
            quest_editor_store.current_quest.observe(({ value }) => this.quest_changed(value), {
                call_now: true,
            }),

            assembly_analyser.issues.observe(({ value }) => this.update_model_markers(value), {
                call_now: true,
            }),
        );
    }

    dispose(): void {
        this.disposer.dispose();
    }

    private quest_changed(quest?: QuestModel): void {
        this.undo.reset();
        this.model_disposer.dispose_all();

        if (quest) {
            const assembly = assembly_analyser.disassemble(quest);
            const model = this.model_disposer.add(
                editor.createModel(assembly.join("\n"), "psoasm"),
            );

            let initial_version = model.getAlternativeVersionId();
            let current_version = initial_version;
            let last_version = initial_version;

            this.model_disposer.add(
                model.onDidChangeContent(e => {
                    const version = model.getAlternativeVersionId();

                    if (version < current_version) {
                        // Undoing.
                        this.undo.can_redo.val = true;

                        if (version === initial_version) {
                            this.undo.can_undo.val = false;
                        }
                    } else {
                        // Redoing.
                        if (version <= last_version) {
                            if (version === last_version) {
                                this.undo.can_redo.val = false;
                            }
                        } else {
                            this.undo.can_redo.val = false;

                            if (current_version > last_version) {
                                last_version = current_version;
                            }
                        }

                        this.undo.can_undo.val = true;
                    }

                    current_version = version;

                    assembly_analyser.update_assembly(e.changes);
                }),
            );

            this._model.val = model;
        } else {
            this._model.val = undefined;
        }
    }

    private update_model_markers({
        warnings,
        errors,
    }: {
        warnings: AssemblyWarning[];
        errors: AssemblyError[];
    }): void {
        const model = this.model.val;
        if (!model) return;

        editor.setModelMarkers(
            model,
            "psoasm",
            warnings
                .map(
                    (warning): IMarkerData => ({
                        severity: MarkerSeverity.Hint,
                        message: warning.message,
                        startLineNumber: warning.line_no,
                        endLineNumber: warning.line_no,
                        startColumn: warning.col,
                        endColumn: warning.col + warning.length,
                        tags: [MarkerTag.Unnecessary],
                    }),
                )
                .concat(
                    errors.map(
                        (error): IMarkerData => ({
                            severity: MarkerSeverity.Error,
                            message: error.message,
                            startLineNumber: error.line_no,
                            endLineNumber: error.line_no,
                            startColumn: error.col,
                            endColumn: error.col + error.length,
                        }),
                    ),
                ),
        );
    }
}

export const asm_editor_store = new AsmEditorStore();