import { ExecutionResult, VirtualMachine } from "./scripting/vm";
import { QuestModel } from "./model/QuestModel";
import { VirtualMachineIO } from "./scripting/vm/io";
import { AsmToken } from "./scripting/instructions";
import { WritableProperty } from "../core/observable/property/WritableProperty";
import { list_property, property } from "../core/observable";
import { Property } from "../core/observable/property/Property";
import Logger from "js-logger";
import { log_store } from "./stores/LogStore";
import { Debugger } from "./scripting/vm/Debugger";
import { WritableListProperty } from "../core/observable/property/list/WritableListProperty";
import { ListProperty } from "../core/observable/property/list/ListProperty";

const logger = Logger.get("quest_editor/QuestRunner");

/**
 * Orchestrates everything related to emulating a quest run. Delegates to {@link VirtualMachine}
 * and {@link Debugger}.
 */
export class QuestRunner {
    private quest_logger = log_store.get_logger("quest_editor/QuestRunner");
    private quest?: QuestModel;
    private animation_frame?: number;

    private readonly _running: WritableProperty<boolean> = property(false);
    private readonly _paused: WritableProperty<boolean> = property(false);
    private readonly _breakpoints: WritableListProperty<number> = list_property();
    private readonly _breakpoint_location: WritableProperty<number | undefined> = property(
        undefined,
    );
    /**
     * Have we executed since last advancing the instruction pointer?
     */
    private executed_since_advance = true;

    private execution_counter = 0;
    private readonly execution_max_count = 100_000;
    private readonly debugger: Debugger;

    // TODO: make vm private again.
    readonly vm: VirtualMachine;
    /**
     * There is a quest loaded and it is currently running.
     */
    readonly running: Property<boolean> = this._running;
    /**
     * A quest is running but the execution is currently paused.
     */
    readonly paused: Property<boolean> = this._paused;
    readonly breakpoints: ListProperty<number> = this._breakpoints;
    readonly breakpoint_location: Property<number | undefined> = this._breakpoint_location;

    constructor() {
        this.vm = new VirtualMachine(this.create_vm_io());
        this.debugger = new Debugger(this.vm);
    }

    run(quest: QuestModel): void {
        if (this.animation_frame != undefined) {
            cancelAnimationFrame(this.animation_frame);
        }

        this.quest = quest;

        this.vm.load_object_code(quest.object_code);
        this.vm.start_thread(0);

        this._running.val = true;
        this._paused.val = false;
        this.executed_since_advance = true;
        this.execution_counter = 0;

        this.schedule_frame();
    }

    resume(): void {
        this.schedule_frame();
    }

    step_over(): void {
        this.debugger.step_over();
        this.schedule_frame();
    }

    step_in(): void {
        this.debugger.step_in();
        this.schedule_frame();
    }

    step_out(): void {
        this.debugger.step_out();
        this.schedule_frame();
    }

    stop(): void {
        this.vm.halt();
        this._running.val = false;
        this._paused.val = false;
        this._breakpoint_location.val = undefined;
    }

    set_breakpoint(line_no: number): void {
        this.debugger.set_breakpoint(line_no);
        this._breakpoints.splice(0, Infinity, ...this.debugger.breakpoints);
    }

    remove_breakpoint(line_no: number): void {
        this.debugger.remove_breakpoint(line_no);
        this._breakpoints.splice(0, Infinity, ...this.debugger.breakpoints);
    }

    toggle_breakpoint(line_no: number): void {
        this.debugger.toggle_breakpoint(line_no);
        this._breakpoints.splice(0, Infinity, ...this.debugger.breakpoints);
    }

    clear_breakpoints(): void {
        this.debugger.clear_breakpoints();
        this._breakpoints.splice(0, Infinity, ...this.debugger.breakpoints);
    }

    private schedule_frame(): void {
        this.animation_frame = requestAnimationFrame(this.execution_loop);
    }

    private execution_loop = (): void => {
        let result: ExecutionResult;

        this._paused.val = false;

        exec_loop: while (true) {
            if (this.executed_since_advance) {
                this.vm.advance();

                this.executed_since_advance = false;

                if (this.vm.halted) {
                    this.stop();
                    break;
                }

                const srcloc = this.vm.get_current_source_location();

                if (srcloc) {
                    // check if need to break
                    const hit_breakpoint = this.debugger.breakpoint_hit(srcloc);

                    if (hit_breakpoint) {
                        this._breakpoint_location.val = srcloc.line_no;
                        break;
                    }
                }
            }

            result = this.vm.execute(false);
            this.executed_since_advance = true;

            // limit execution to prevent the browser from freezing
            if (++this.execution_counter >= this.execution_max_count) {
                this.stop();
                logger.error("Terminated: Maximum execution count reached.");
                break;
            }

            switch (result) {
                case ExecutionResult.WaitingVsync:
                    this.vm.vsync();
                    this.schedule_frame();
                    break exec_loop;
                case ExecutionResult.WaitingInput:
                    // TODO: implement input from gui
                    this.schedule_frame();
                    break exec_loop;
                case ExecutionResult.WaitingSelection:
                    // TODO: implement input from gui
                    this.vm.list_select(0);
                    this.schedule_frame();
                    break exec_loop;
                case ExecutionResult.Halted:
                    this.stop();
                    break exec_loop;
            }
        }

        this._paused.val = true;
        this.execution_counter = 0;
    };

    private create_vm_io = (): VirtualMachineIO => {
        function srcloc_to_string(srcloc?: AsmToken): string {
            return srcloc ? ` [${srcloc.line_no}:${srcloc.col}]` : " ";
        }

        return {
            window_msg: (msg: string): void => {
                this.quest_logger.info(`window_msg "${msg}"`);
            },

            message: (msg: string): void => {
                this.quest_logger.info(`message "${msg}"`);
            },

            add_msg: (msg: string): void => {
                this.quest_logger.info(`add_msg "${msg}"`);
            },

            winend: (): void => {},

            mesend: (): void => {},

            list: (list_items: string[]): void => {
                this.quest_logger.info(`list "[${list_items}]"`);
            },

            warning: (msg: string, srcloc?: AsmToken): void => {
                this.quest_logger.warning(msg + srcloc_to_string(srcloc));
            },

            error: (err: Error, srcloc?: AsmToken): void => {
                this.quest_logger.error(err + srcloc_to_string(srcloc));
            },
        };
    };
}
