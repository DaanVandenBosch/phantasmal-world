import { observable } from "mobx";
import { editor } from "monaco-editor";
import AssemblyWorker from "worker-loader!./assembly_worker";
import { Instruction } from "../data_formats/parsing/quest/bin";
import { AssemblyChangeInput, NewAssemblyInput, ScriptWorkerOutput } from "./assembler_messages";
import { AssemblyError } from "./assembly";
import { disassemble } from "./disassembly";

export class Assembler {
    @observable errors: AssemblyError[] = [];

    private worker = new AssemblyWorker();
    private instructions: Instruction[] = [];
    private labels: Map<number, number> = new Map();

    constructor() {
        this.worker.onmessage = this.process_worker_message;
    }

    disassemble(instructions: Instruction[], labels: Map<number, number>): string[] {
        this.instructions = instructions;
        this.labels = labels;
        const assembly = disassemble(instructions, labels);
        const message: NewAssemblyInput = { type: "new_assembly_input", assembly };
        this.worker.postMessage(message);
        return assembly;
    }

    update_assembly(changes: editor.IModelContentChange[]): void {
        const message: AssemblyChangeInput = { type: "assembly_change_input", changes };
        this.worker.postMessage(message);
    }

    dispose(): void {
        this.worker.terminate();
    }

    private process_worker_message = (e: MessageEvent): void => {
        const message: ScriptWorkerOutput = e.data;

        if (message.type === "new_errors_output") {
            this.instructions.splice(0, this.instructions.length, ...message.instructions);

            this.labels.clear();

            for (const [l, i] of message.labels) {
                this.labels.set(l, i);
            }

            this.errors = message.errors;
        }
    };
}
