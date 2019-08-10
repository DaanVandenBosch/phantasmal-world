import { action, observable } from "mobx";
import { editor } from "monaco-editor";
import AssemblyWorker from "worker-loader!./assembly_worker";
import {
    AssemblyChangeInput,
    AssemblyWorkerOutput,
    NewAssemblyInput,
} from "./assembly_worker_messages";
import { AssemblyError, AssemblyWarning } from "./assembly";
import { disassemble } from "./disassembly";
import { ObservableQuest } from "../domain/ObservableQuest";

export class AssemblyAnalyser {
    @observable warnings: AssemblyWarning[] = [];
    @observable errors: AssemblyError[] = [];

    private worker = new AssemblyWorker();
    private quest?: ObservableQuest;

    constructor() {
        this.worker.onmessage = this.process_worker_message;
    }

    disassemble(quest: ObservableQuest): string[] {
        this.quest = quest;
        const assembly = disassemble(quest.object_code);
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

    @action
    private process_worker_message = (e: MessageEvent): void => {
        const message: AssemblyWorkerOutput = e.data;

        if (message.type === "new_object_code_output" && this.quest) {
            this.quest.object_code.splice(0, this.quest.object_code.length, ...message.object_code);
            this.quest.set_map_designations(message.map_designations);
            this.warnings = message.warnings;
            this.errors = message.errors;
        }
    };
}
