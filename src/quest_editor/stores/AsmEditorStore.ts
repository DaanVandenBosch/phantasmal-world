import { editor, languages, MarkerSeverity, MarkerTag, Position } from "monaco-editor";
import { AssemblyAnalyser } from "../scripting/AssemblyAnalyser";
import { Disposable } from "../../core/observable/Disposable";
import { Disposer } from "../../core/observable/Disposer";
import { SimpleUndo } from "../../core/undo/SimpleUndo";
import { quest_editor_store } from "./QuestEditorStore";
import { ASM_SYNTAX } from "./asm_syntax";
import { AssemblyError, AssemblyWarning } from "../scripting/assembly";
import { Observable } from "../../core/observable/Observable";
import { emitter, property, list_property } from "../../core/observable";
import { WritableProperty } from "../../core/observable/property/WritableProperty";
import { Property } from "../../core/observable/property/Property";
import ITextModel = editor.ITextModel;
import CompletionList = languages.CompletionList;
import IMarkerData = editor.IMarkerData;
import SignatureHelpResult = languages.SignatureHelpResult;
import LocationLink = languages.LocationLink;
import { WritableListProperty } from "../../core/observable/property/list/WritableListProperty";
import { ListProperty } from "../../core/observable/property/list/ListProperty";

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

    async provideSignatureHelp(
        model: ITextModel,
        position: Position,
    ): Promise<SignatureHelpResult | undefined> {
        const value = await assembly_analyser.provide_signature_help(
            model.uri,
            position.lineNumber,
            position.column,
        );
        return (
            value && {
                value,
                dispose() {
                    // Do nothing.
                },
            }
        );
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

languages.registerDefinitionProvider("psoasm", {
    provideDefinition(model: ITextModel, position: Position): Promise<LocationLink[]> {
        return assembly_analyser.provide_definition(
            model.uri,
            position.lineNumber,
            position.column,
        );
    },
});

export class AsmEditorStore implements Disposable {
    private readonly disposer = new Disposer();
    private readonly model_disposer = this.disposer.add(new Disposer());
    private readonly _model: WritableProperty<ITextModel | undefined> = property(undefined);
    private readonly _did_undo = emitter<string>();
    private readonly _did_redo = emitter<string>();
    private readonly _inline_args_mode: WritableProperty<boolean> = property(true);
    private readonly _breakpoints: WritableListProperty<number> = list_property();
    private readonly _execution_location: WritableProperty<number | undefined> = property(
        undefined,
    );

    readonly model: Property<ITextModel | undefined> = this._model;
    readonly did_undo: Observable<string> = this._did_undo;
    readonly did_redo: Observable<string> = this._did_redo;
    readonly undo = new SimpleUndo(
        "Text edits",
        () => this._did_undo.emit({ value: "asm undo" }),
        () => this._did_redo.emit({ value: "asm undo" }),
    );
    readonly inline_args_mode: Property<boolean> = this._inline_args_mode;
    readonly has_issues: Property<boolean> = assembly_analyser.issues.map(
        issues => issues.warnings.length + issues.errors.length > 0,
    );
    readonly breakpoints: ListProperty<number> = this._breakpoints;
    readonly execution_location: Property<number | undefined> = this._execution_location;

    constructor() {
        this.disposer.add_all(
            quest_editor_store.current_quest.observe(this.quest_changed, {
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

    set_inline_args_mode = (inline_args_mode: boolean): void => {
        // don't allow changing inline args mode if there are issues
        if (!this.has_issues.val) {
            this._inline_args_mode.val = inline_args_mode;

            assembly_analyser.update_settings({
                manual_stack: !this.inline_args_mode.val,
            });

            this.update_model();
        }
    };

    private quest_changed = (): void => {
        this.update_model();
    };

    /**
     * Setup features for a given editor model.
     * Features include undo/redo history and reassembling on change.
     */
    private setup_editor_model_features(model: editor.ITextModel): void {
        const initial_version = model.getAlternativeVersionId();
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

                // update breakpoints
                for (const change of e.changes) {
                    // empty text means something was deleted
                    if (change.text === "") {
                        const num_removed_lines =
                            change.range.endLineNumber - change.range.startLineNumber;

                        if (num_removed_lines > 0) {
                            // if a line that has a decoration is removed
                            // monaco will automatically move the decoration
                            // to the line before the change.
                            // we need to reflect this in the state as well.
                            // move breakpoints that were in removed lines
                            // backwards by the number of removed lines.
                            for (
                                let line_num = change.range.startLineNumber + 1;
                                line_num <= change.range.endLineNumber;
                                line_num++
                            ) {
                                // line numbers can't go less than 1
                                const new_line_num = Math.max(line_num - num_removed_lines, 1);
                                const breakpoint_idx = this._breakpoints.val.indexOf(line_num);

                                // don't add breakpoint if one already exists
                                if (
                                    breakpoint_idx > -1 &&
                                    !this._breakpoints.val.includes(new_line_num)
                                ) {
                                    this._breakpoints.splice(breakpoint_idx, 1, new_line_num);
                                }
                            }

                            // move breakpoints that are after the affected
                            // lines backwards by the number of removed lines
                            for (let i = 0; i < this._breakpoints.val.length; i++) {
                                if (this._breakpoints.val[i] > change.range.endLineNumber) {
                                    this._breakpoints.splice(
                                        i,
                                        1,
                                        this._breakpoints.val[i] - num_removed_lines,
                                    );
                                }
                            }
                        }
                    } else {
                        const num_added_lines = change.text.split("\n").length - 1;

                        if (num_added_lines > 0) {
                            // move breakpoints that are after the affected lines
                            // forwards by the number of added lines
                            for (let i = 0; i < this.breakpoints.val.length; i++) {
                                if (this._breakpoints.val[i] > change.range.endLineNumber) {
                                    this._breakpoints.splice(
                                        i,
                                        1,
                                        this._breakpoints.val[i] + num_added_lines,
                                    );
                                }
                            }
                        }
                    }
                }
            }),
        );
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

    private update_model(): void {
        this.undo.reset();
        this.model_disposer.dispose_all();

        const quest = quest_editor_store.current_quest.val;

        if (quest) {
            const manual_stack = !this.inline_args_mode.val;

            const assembly = assembly_analyser.disassemble(quest, manual_stack);
            const model = this.model_disposer.add(
                editor.createModel(assembly.join("\n"), "psoasm"),
            );

            this.setup_editor_model_features(model);

            this._model.val = model;
        } else {
            this._model.val = undefined;
        }
    }

    public toggle_breakpoint(line_num: number): void {
        const i = this._breakpoints.val.indexOf(line_num);

        if (i === -1) {
            this._breakpoints.push(line_num);
        } else {
            this._breakpoints.splice(i, 1);
        }
    }

    public clear_breakpoints(): void {
        this._breakpoints.splice(0);
    }

    public set_execution_location(line_num: number): void {
        this._execution_location.val = line_num;
    }

    public unset_execution_location(): void {
        this._execution_location.val = undefined;
    }
}

export const asm_editor_store = new AsmEditorStore();
