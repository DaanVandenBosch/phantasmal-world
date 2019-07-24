import { autorun } from "mobx";
import { editor, languages, MarkerSeverity } from "monaco-editor";
import React, { Component, createRef, ReactNode } from "react";
import { AutoSizer } from "react-virtualized";
import { OPCODES } from "../../data_formats/parsing/quest/bin";
import { Assembler } from "../../scripting/Assembler";
import { quest_editor_store } from "../../stores/QuestEditorStore";
import "./ScriptEditorComponent.less";
import { Action } from "../../undo";

const ASM_SYNTAX: languages.IMonarchLanguage = {
    defaultToken: "invalid",

    tokenizer: {
        root: [
            // Registers.
            [/r\d+/, "predefined"],

            // Identifiers.
            [/[a-z][a-z0-9_=<>!]*/, "identifier"],

            // Labels.
            [/\d+:/, "tag"],

            // Whitespace.
            [/[ \t\r\n]+/, "white"],
            // [/\/\*/, "comment", "@comment"],
            // [/\/\/.*$/, "comment"],

            // Numbers.
            [/-?\d*\.\d+([eE][-+]?\d+)?/, "number.float"],
            // [/-?0[xX][0-9a-fA-F]+/, "number.hex"],
            [/-?\d+/, "number"],

            // Delimiters.
            [/,/, "delimiter"],

            // Strings.
            [/"([^"\\]|\\.)*$/, "string.invalid"], // Unterminated string.
            [/"/, { token: "string.quote", bracket: "@open", next: "@string" }],
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

const INSTRUCTION_SUGGESTIONS = OPCODES.filter(opcode => opcode != null).map(opcode => {
    return ({
        label: opcode.mnemonic,
        kind: languages.CompletionItemKind.Function,
        insertText: opcode.mnemonic,
    } as any) as languages.CompletionItem;
});

languages.register({ id: "psoasm" });
languages.setMonarchTokensProvider("psoasm", ASM_SYNTAX);
languages.registerCompletionItemProvider("psoasm", {
    provideCompletionItems: (model, position) => {
        const value = model.getValueInRange({
            startLineNumber: position.lineNumber,
            endLineNumber: position.lineNumber,
            startColumn: 1,
            endColumn: position.column,
        });
        const suggestions = /^\s*([a-z][a-z0-9_=<>!]*)?$/.test(value)
            ? INSTRUCTION_SUGGESTIONS
            : [];

        return {
            suggestions,
            incomplete: false,
        };
    },
});
languages.setLanguageConfiguration("psoasm", {
    indentationRules: {
        increaseIndentPattern: /^\s*\d+:/,
        decreaseIndentPattern: /^\s*\d+/,
    },
    autoClosingPairs: [{ open: '"', close: '"' }],
    surroundingPairs: [{ open: '"', close: '"' }],
});

editor.defineTheme("phantasmal-world", {
    base: "vs-dark",
    inherit: true,
    rules: [
        { token: "", foreground: "e0e0e0", background: "#181818" },
        { token: "tag", foreground: "99bbff" },
        { token: "predefined", foreground: "bbffbb" },
        { token: "number", foreground: "ffffaa" },
        { token: "string", foreground: "88ffff" },
        { token: "string.escape", foreground: "8888ff" },
    ],
    colors: {
        "editor.background": "#181818",
        "editor.lineHighlightBackground": "#202020",
    },
});

export class ScriptEditorComponent extends Component {
    render(): ReactNode {
        return (
            <section id="qe-ScriptEditorComponent" className="qe-ScriptEditorComponent">
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
    private editor?: editor.IStandaloneCodeEditor;
    private assembler?: Assembler;
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

            this.assembler = new Assembler();

            this.disposers.push(
                this.dispose,
                autorun(this.update_model),
                autorun(this.update_model_markers)
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

        if (quest && this.editor && this.assembler) {
            const assembly = this.assembler.disassemble(quest.instructions, quest.labels);
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
                }
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

                if (!this.assembler) return;
                this.assembler.update_assembly(e.changes);
            });

            this.disposers.push(() => disposable.dispose());
            this.editor.setModel(model);
            this.editor.updateOptions({ readOnly: false });
        } else if (this.editor) {
            this.editor.updateOptions({ readOnly: true });
        }
    };

    private update_model_markers = () => {
        if (!this.editor || !this.assembler) return;

        // Reference errors here to make sure we get mobx updates.
        this.assembler.errors.length;

        const model = this.editor.getModel();
        if (!model) return;

        editor.setModelMarkers(
            model,
            "psoasm",
            this.assembler.errors.map(error => ({
                severity: MarkerSeverity.Error,
                message: error.message,
                startLineNumber: error.line_no,
                endLineNumber: error.line_no,
                startColumn: error.col,
                endColumn: error.col + error.length,
            }))
        );
    };

    private dispose = () => {
        if (this.editor) {
            this.editor.dispose();
            const model = this.editor.getModel();
            if (model) model.dispose();
            this.editor = undefined;
        }

        if (this.assembler) {
            this.assembler.dispose();
        }
    };
}
