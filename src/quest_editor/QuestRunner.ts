import { ExecutionResult, VirtualMachine } from "./scripting/vm";
import { QuestModel } from "./model/QuestModel";
import { VirtualMachineIO } from "./scripting/vm/io";
import { AsmToken } from "./scripting/instructions";
import { quest_editor_store } from "./stores/QuestEditorStore";
import { asm_editor_store } from "./stores/AsmEditorStore";

const logger = quest_editor_store.get_logger("quest_editor/QuestRunner");

function srcloc_to_string(srcloc: AsmToken): string {
    return `[${srcloc.line_no}:${srcloc.col}]`;
}

export class QuestRunner {
    private readonly vm: VirtualMachine;
    private animation_frame?: number;

    constructor() {
        this.vm = new VirtualMachine(this.create_vm_io());
    }

    run(quest: QuestModel): void {
        if (this.animation_frame != undefined) {
            cancelAnimationFrame(this.animation_frame);
        }

        this.vm.load_object_code(quest.object_code);
        this.vm.start_thread(0);

        this.schedule_frame();
    }

    private schedule_frame(): void {
        this.animation_frame = requestAnimationFrame(this.execution_loop);
    }

    private execution_loop = (): void => {
        let result: ExecutionResult;

        exec_loop:
        while (true) {
            result = this.vm.execute();

            const srcloc = this.vm.get_current_source_location();
            if (srcloc && asm_editor_store.breakpoints.val.includes(srcloc.line_no)) {
                asm_editor_store.set_execution_location(srcloc.line_no);
                break exec_loop;
            }

            switch (result) {
                case ExecutionResult.WaitingVsync:
                    this.vm.vsync();
                    this.schedule_frame();
                    break;
                case ExecutionResult.WaitingInput:
                    // TODO: implement input from gui
                    this.schedule_frame();
                    break;
                case ExecutionResult.WaitingSelection:
                    // TODO: implement input from gui
                    this.vm.list_select(0);
                    this.schedule_frame();
                    break;
                case ExecutionResult.Halted:
                    asm_editor_store.unset_execution_location();
                    break exec_loop;
            }
        }
    };

    private create_vm_io = (): VirtualMachineIO => {
        return {
            window_msg: (msg: string): void => {
                logger.info(`window_msg "${msg}"`);
            },

            message: (msg: string): void => {
                logger.info(`message "${msg}"`);
            },

            add_msg: (msg: string): void => {
                logger.info(`add_msg "${msg}"`);
            },

            winend: (): void => {},

            mesend: (): void => {},

            list: (list_items: string[]): void => {
                logger.info(`list "[${list_items}]"`);
            },

            warning: (msg: string, srcloc?: AsmToken): void => {
                logger.warning(msg, srcloc && srcloc_to_string(srcloc));
            },

            error: (err: Error, srcloc?: AsmToken): void => {
                logger.error(err, srcloc && srcloc_to_string(srcloc));
            },
        };
    };
}
