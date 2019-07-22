import { editor, languages } from "monaco-editor";
import React, { Component, createRef, ReactNode } from "react";
import { AutoSizer } from "react-virtualized";
import { OPCODES } from "../../data_formats/parsing/quest/bin";
import { quest_editor_store } from "../../stores/QuestEditorStore";
import "./ScriptEditorComponent.less";
import { disassemble } from "../scripting/disassembly";
import { IReactionDisposer, autorun } from "mobx";

const ASM_SYNTAX: languages.IMonarchLanguage = {
    defaultToken: "invalid",

    tokenizer: {
        root: [
            // Identifiers.
            [/[a-z][\w=<>!]*/, "identifier"],

            // Labels.
            [/^\d+:/, "tag"],

            // Registers.
            [/r\d+/, "predefined"],

            // Whitespace.
            [/[ \t\r\n]+/, "white"],
            [/\/\*/, "comment", "@comment"],
            [/\/\/.*$/, "comment"],

            // Numbers.
            [/-?\d*\.\d+([eE][-+]?\d+)?/, "number.float"],
            [/-?0[xX][0-9a-fA-F]+/, "number.hex"],
            [/-?\d+/, "number"],

            // Delimiters.
            [/,/, "delimiter"],

            // Strings.
            [/"([^"\\]|\\.)*$/, "string.invalid"], // Unterminated string.
            [/"/, { token: "string.quote", bracket: "@open", next: "@string" }],
        ],

        comment: [
            [/[^/*]+/, "comment"],
            [/\/\*/, "comment", "@push"], // Nested comment.
            [/\*\//, "comment", "@pop"],
            [/[/*]/, "comment"],
        ],

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
            endColumn: position.column + 1,
        });
        const suggest = /^\s*([a-z][\w=<>!]*)?$/.test(value);

        return {
            suggestions: suggest ? INSTRUCTION_SUGGESTIONS : [],
            incomplete: false,
        };
    },
});
languages.setLanguageConfiguration("psoasm", {
    indentationRules: {
        increaseIndentPattern: /\d+:/,
        decreaseIndentPattern: /\d+/,
    },
    autoClosingPairs: [{ open: '"', close: '"' }],
    surroundingPairs: [{ open: '"', close: '"' }],
});

editor.defineTheme("phantasmal-world", {
    base: "vs-dark",
    inherit: true,
    rules: [{ token: "", background: "151c21" }],
    colors: {
        "editor.background": "#151c21",
        "editor.lineHighlightBackground": "#1a2228",
    },
});

export class ScriptEditorComponent extends Component<{ className?: string }> {
    render(): ReactNode {
        let className = "qe-ScriptEditorComponent";
        if (this.props.className) className += " " + this.props.className;

        return (
            <section className={className}>
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
    private disposer?: IReactionDisposer;

    render(): ReactNode {
        return <div ref={this.div_ref} />;
    }

    componentDidMount(): void {
        if (this.div_ref.current) {
            this.editor = editor.create(this.div_ref.current, {
                theme: "phantasmal-world",
                scrollBeyondLastLine: false,
                autoIndent: true,
            });

            this.disposer = autorun(() => {
                const quest = quest_editor_store.current_quest;
                const model = quest && editor.createModel(disassemble(quest), "psoasm");

                if (model && this.editor) {
                    // model.onDidChangeContent(e => {
                    // });

                    this.editor.setModel(model);
                }
            });
        }
    }

    componentWillUnmount(): void {
        if (this.editor) {
            const model = this.editor.getModel();
            if (model) model.dispose();

            this.editor.dispose();
        }

        if (this.disposer) this.disposer();
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
}
