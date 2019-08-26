import { autorun } from "mobx";
import { editor, languages, MarkerSeverity, MarkerTag, Position } from "monaco-editor";
import React, { Component, createRef, ReactNode } from "react";
import { AutoSizer } from "react-virtualized";
import { AssemblyAnalyser } from "../../../quest_editor/scripting/AssemblyAnalyser";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { Action } from "../../core/undo";
import styles from "./AssemblyEditorComponent.css";
import CompletionList = languages.CompletionList;
import ITextModel = editor.ITextModel;
import IStandaloneCodeEditor = editor.IStandaloneCodeEditor;
import SignatureHelp = languages.SignatureHelp;
import IMarkerData = editor.IMarkerData;

const ASM_SYNTAX: languages.IMonarchLanguage = {
    defaultToken: "invalid",

    tokenizer: {
        root: [
            // Strings.
            [/"([^"\\]|\\.)*$/, "string.invalid"], // Unterminated string.
            [/"/, { token: "string.quote", bracket: "@open", next: "@string" }],

            // Registers.
            [/r\d+/, "predefined"],

            // Labels.
            [/[^\s]+:/, "tag"],

            // Numbers.
            [/0x[0-9a-fA-F]+/, "number.hex"],
            [/-?\d+(\.\d+)?(e-?\d+)?/, "number.float"],
            [/-?[0-9]+/, "number"],

            // Section markers.
            [/\.[^\s]+/, "keyword"],

            // Identifiers.
            [/[a-z][a-z0-9_=<>!]*/, "identifier"],

            // Whitespace.
            [/[ \t\r\n]+/, "white"],
            // [/\/\*/, "comment", "@comment"],
            [/\/\/.*$/, "comment"],

            // Delimiters.
            [/,/, "delimiter"],
        ],

        // comment: [
        //     [/[^/*]+/, "comment"],
        //     [/\/\*/, "comment", "@push"], // Nested comment.
        //     [/\*\//, "comment", "@pop"],
        //     [/[/*]/, "comment"],
        // ],

        string: [
            [/[^\\"]+/, "string"],
            [/\\(?:[n\\"])/, "string.escape"],
            [/\\./, "string.escape.invalid"],
            [/"/, { token: "string.quote", bracket: "@close", next: "@pop" }],
        ],
    },
};

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

export class AssemblyEditorComponent extends Component {
    render(): ReactNode {
        return (
            <section id="qe-ScriptEditorComponent" className={styles.main}>
                <AutoSizer>
                    {({ width, height }) => <MonacoComponent width={width} height={height} />}
                </AutoSizer>
            </section>
        );
    }
}

type MonacoProps = {
    width: number;
    height: number;
};

class MonacoComponent extends Component<MonacoProps> {
    private div_ref = createRef<HTMLDivElement>();
    private editor?: IStandaloneCodeEditor;
    private disposers: (() => void)[] = [];

    render(): ReactNode {
        return <div ref={this.div_ref} />;
    }

    componentDidMount(): void {
        if (this.div_ref.current) {
            this.editor = editor.create(this.div_ref.current, {
                theme: "phantasmal-world",
                scrollBeyondLastLine: false,
                autoIndent: true,
                fontSize: 14,
                wordBasedSuggestions: false,
                wordWrap: "on",
                wrappingIndent: "indent",
            });

            this.disposers.push(
                this.dispose,
                autorun(this.update_model),
                autorun(this.update_model_markers),
            );
        }
    }

    componentWillUnmount(): void {
        for (const disposer of this.disposers.splice(0, this.disposers.length)) {
            disposer();
        }
    }

    shouldComponentUpdate(): boolean {
        return false;
    }

    UNSAFE_componentWillReceiveProps(props: MonacoProps): void {
        if (
            (this.props.width !== props.width || this.props.height !== props.height) &&
            this.editor
        ) {
            this.editor.layout(props);
        }
    }

    private update_model = () => {
        const quest = quest_editor_store.current_quest;

        if (quest && this.editor) {
            const assembly = assembly_analyser.disassemble(quest);
            const model = editor.createModel(assembly.join("\n"), "psoasm");

            quest_editor_store.script_undo.action = new Action(
                "Text edits",
                () => {
                    if (this.editor) {
                        this.editor.trigger("undo stack", "undo", undefined);
                    }
                },
                () => {
                    if (this.editor) {
                        this.editor.trigger("redo stack", "redo", undefined);
                    }
                },
            );

            let initial_version = model.getAlternativeVersionId();
            let current_version = initial_version;
            let last_version = initial_version;

            const disposable = model.onDidChangeContent(e => {
                const version = model.getAlternativeVersionId();

                if (version < current_version) {
                    // Undoing.
                    quest_editor_store.script_undo.can_redo = true;

                    if (version === initial_version) {
                        quest_editor_store.script_undo.can_undo = false;
                    }
                } else {
                    // Redoing.
                    if (version <= last_version) {
                        if (version === last_version) {
                            quest_editor_store.script_undo.can_redo = false;
                        }
                    } else {
                        quest_editor_store.script_undo.can_redo = false;

                        if (current_version > last_version) {
                            last_version = current_version;
                        }
                    }

                    quest_editor_store.script_undo.can_undo = true;
                }

                current_version = version;

                assembly_analyser.update_assembly(e.changes);
            });

            this.disposers.push(() => disposable.dispose());
            this.editor.setModel(model);
            this.editor.updateOptions({ readOnly: false });
        } else if (this.editor) {
            this.editor.updateOptions({ readOnly: true });
        }
    };

    private update_model_markers = () => {
        if (!this.editor) return;

        // Reference warnings and errors here to make sure we get mobx updates.
        assembly_analyser.warnings.length;
        assembly_analyser.errors.length;

        const model = this.editor.getModel();
        if (!model) return;

        editor.setModelMarkers(
            model,
            "psoasm",
            assembly_analyser.warnings
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
                    assembly_analyser.errors.map(
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
    };

    private dispose = () => {
        if (this.editor) {
            this.editor.dispose();
            const model = this.editor.getModel();
            if (model) model.dispose();
            this.editor = undefined;
        }
    };
}
