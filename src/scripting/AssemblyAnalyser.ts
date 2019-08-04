import { observable } from "mobx";
import { editor } from "monaco-editor";
import AssemblyWorker from "worker-loader!./assembly_worker";
import { AssemblyChangeInput, NewAssemblyInput, ScriptWorkerOutput } from "./assembler_messages";
import { AssemblyError } from "./assembly";
import { disassemble } from "./disassembly";
import { Segment } from "./instructions";

export class AssemblyAnalyser {
    @observable errors: AssemblyError[] = [];

    private worker = new AssemblyWorker();
    private object_code: Segment[] = [];

    constructor() {
        this.worker.onmessage = this.process_worker_message;
    }

    disassemble(object_code: Segment[]): string[] {
        this.object_code = object_code;
        const assembly = disassemble(object_code);
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

        if (message.type === "new_object_code_output") {
            this.object_code.splice(0, this.object_code.length, ...message.object_code);
            this.errors = message.errors;
        }
    };
}
