import { editor, languages } from "monaco-editor";
import AssemblyWorker from "worker-loader!./assembly_worker";
import {
    AssemblyChangeInput,
    AssemblyWorkerOutput,
    InputMessageType,
    NewAssemblyInput,
    OutputMessageType,
    SignatureHelpInput,
} from "./assembly_worker_messages";
import { AssemblyError, AssemblyWarning } from "./assembly";
import { disassemble } from "./disassembly";
import { QuestModel } from "../model/QuestModel";
import { Kind, OPCODES } from "./opcodes";
import { Property } from "../../core/observable/Property";
import { property } from "../../core/observable";
import { WritableProperty } from "../../core/observable/WritableProperty";
import CompletionList = languages.CompletionList;
import CompletionItemKind = languages.CompletionItemKind;
import CompletionItem = languages.CompletionItem;
import IModelContentChange = editor.IModelContentChange;
import SignatureHelp = languages.SignatureHelp;
import ParameterInformation = languages.ParameterInformation;
import { Disposable } from "../../core/observable/Disposable";

const INSTRUCTION_SUGGESTIONS = OPCODES.filter(opcode => opcode != null).map(opcode => {
    return ({
        label: opcode.mnemonic,
        kind: CompletionItemKind.Function,
        insertText: opcode.mnemonic,
    } as any) as languages.CompletionItem;
});

const KEYWORD_SUGGESTIONS = [
    {
        label: ".code",
        kind: CompletionItemKind.Keyword,
        insertText: "code",
    },
    {
        label: ".data",
        kind: CompletionItemKind.Keyword,
        insertText: "data",
    },
    {
        label: ".string",
        kind: CompletionItemKind.Keyword,
        insertText: "string",
    },
] as CompletionItem[];

export class AssemblyAnalyser implements Disposable {
    readonly _issues: WritableProperty<{
        warnings: AssemblyWarning[];
        errors: AssemblyError[];
    }> = property({ warnings: [], errors: [] });

    readonly issues: Property<{
        warnings: AssemblyWarning[];
        errors: AssemblyError[];
    }> = this._issues;

    private worker = new AssemblyWorker();
    private quest?: QuestModel;

    private promises = new Map<
        number,
        { resolve: (result: any) => void; reject: (error: Error) => void }
    >();

    private message_id = 0;

    constructor() {
        this.worker.onmessage = this.process_worker_message;
    }

    disassemble(quest: QuestModel): string[] {
        this.quest = quest;
        const assembly = disassemble(quest.object_code);
        const message: NewAssemblyInput = { type: InputMessageType.NewAssembly, assembly };
        this.worker.postMessage(message);
        return assembly;
    }

    update_assembly(changes: IModelContentChange[]): void {
        const message: AssemblyChangeInput = {
            type: InputMessageType.AssemblyChange,
            changes: changes.map(change => ({
                start_line_no: change.range.startLineNumber,
                start_col: change.range.startColumn,
                end_line_no: change.range.endLineNumber,
                end_col: change.range.endColumn,
                new_text: change.text,
            })),
        };
        this.worker.postMessage(message);
    }

    provide_completion_items(text: string): CompletionList {
        const suggestions = /^\s*([a-z][a-z0-9_=<>!]*)?$/.test(text)
            ? INSTRUCTION_SUGGESTIONS
            : /^\s*\.[a-z]+$/.test(text)
            ? KEYWORD_SUGGESTIONS
            : [];

        return {
            suggestions,
            incomplete: false,
        };
    }

    async provide_signature_help(line_no: number, col: number): Promise<SignatureHelp | undefined> {
        const id = this.message_id++;

        return new Promise<SignatureHelp>((resolve, reject) => {
            this.promises.set(id, { resolve, reject });
            const message: SignatureHelpInput = {
                type: InputMessageType.SignatureHelp,
                id,
                line_no,
                col,
            };
            this.worker.postMessage(message);

            setTimeout(() => {
                if (this.promises.delete(id)) {
                    reject(new Error("Signature help timed out."));
                }
            }, 5_000);
        });
    }

    dispose(): void {
        this.worker.terminate();
    }

    private process_worker_message = (e: MessageEvent): void => {
        const message: AssemblyWorkerOutput = e.data;

        switch (message.type) {
            case OutputMessageType.NewObjectCode:
                if (this.quest) {
                    this.quest.object_code.splice(
                        0,
                        this.quest.object_code.length,
                        ...message.object_code,
                    );
                    this.quest.set_map_designations(message.map_designations);
                    this._issues.val = { warnings: message.warnings, errors: message.errors };
                }
                break;
            case OutputMessageType.SignatureHelp:
                {
                    const promise = this.promises.get(message.id);

                    if (promise) {
                        this.promises.delete(message.id);

                        if (message.opcode) {
                            let signature = message.opcode.mnemonic + " ";
                            const parameters: ParameterInformation[] = [];
                            let first = true;

                            for (const param of message.opcode.params) {
                                if (first) {
                                    first = false;
                                } else {
                                    signature += ", ";
                                }

                                let param_name: string;

                                switch (param.type.kind) {
                                    case Kind.ILabel:
                                        param_name = "FuncLabel";
                                        break;
                                    case Kind.DLabel:
                                        param_name = "DataLabel";
                                        break;
                                    case Kind.SLabel:
                                        param_name = "StringLabel";
                                        break;
                                    case Kind.ILabelVar:
                                        param_name = "...FuncLabel";
                                        break;
                                    case Kind.RegRef:
                                    case Kind.RegTupRef:
                                        param_name = "Register";
                                        break;
                                    case Kind.RegRefVar:
                                        param_name = "...Register";
                                        break;
                                    default:
                                        param_name = Kind[param.type.kind];
                                        break;
                                }

                                parameters.push({
                                    label: [signature.length, signature.length + param_name.length],
                                    documentation: param.doc,
                                });

                                signature += param_name;
                            }

                            const help: SignatureHelp = {
                                signatures: [
                                    {
                                        label: signature,
                                        documentation: message.opcode.doc,
                                        parameters,
                                    },
                                ],
                                activeSignature: 0,
                                activeParameter: message.active_param,
                            };
                            promise.resolve(help);
                        } else {
                            promise.resolve(undefined);
                        }
                    }
                }
                break;
        }
    };
}
