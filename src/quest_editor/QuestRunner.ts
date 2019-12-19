import { ExecutionResult, VirtualMachine } from "./scripting/vm/VirtualMachine";
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
import { AreaVariantModel } from "./model/AreaVariantModel";
import { Episode } from "../core/data_formats/parsing/quest/Episode";
import { area_store } from "./stores/AreaStore";
import { QuestNpcModel } from "./model/QuestNpcModel";
import { QuestObjectModel } from "./model/QuestObjectModel";
import { defined } from "../core/util";

const logger = Logger.get("quest_editor/QuestRunner");

export enum QuestRunnerState {
    /**
     * No quest is loading or loaded quest is not running.
     */
    Stopped,
    /**
     * Function 0 is running.
     */
    Startup,
    StartupPaused,
    /**
     * Quest has started up and is running nominally.
     */
    Running,
    /**
     * Quest has started up and is paused.
     */
    Paused,
}

export type GameState = {
    readonly episode: Episode;
    readonly current_area_variant: Property<AreaVariantModel | undefined>;
    readonly npcs: ListProperty<QuestNpcModel>;
    readonly objects: ListProperty<QuestObjectModel>;
};

/**
 * Orchestrates everything related to emulating a quest run. Drives a {@link VirtualMachine}
 * and delegates to {@link Debugger}.
 */
export class QuestRunner {
    private quest_logger = log_store.get_logger("quest_editor/QuestRunner");
    private quest?: QuestModel;
    private animation_frame?: number;
    private readonly _state: WritableProperty<QuestRunnerState> = property(
        QuestRunnerState.Stopped,
    );

    private initial_area_id = 0;
    private initial_area_variant_id = 0;

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

    private readonly _game_state = {
        episode: Episode.I,
        current_area_variant: property<AreaVariantModel | undefined>(undefined),
        npcs: list_property<QuestNpcModel>(),
        objects: list_property<QuestObjectModel>(),

        reset() {
            this.episode = Episode.I;
            this.current_area_variant.val = undefined;
            this.npcs = list_property();
            this.objects = list_property();
        },
    };

    // TODO: make vm private again.
    readonly vm: VirtualMachine;
    /**
     * There is a quest loaded and it is currently running or paused.
     */
    readonly running: Property<boolean> = this._state.map(
        state => state !== QuestRunnerState.Stopped,
    );
    /**
     * A quest is running but execution is currently paused.
     */
    readonly paused: Property<boolean> = this._state.map(
        state => state === QuestRunnerState.StartupPaused || state === QuestRunnerState.Paused,
    );
    readonly breakpoints: ListProperty<number> = this._breakpoints;
    readonly breakpoint_location: Property<number | undefined> = this._breakpoint_location;

    readonly game_state: GameState = this._game_state;

    constructor() {
        this.vm = new VirtualMachine(this.create_vm_io());
        this.debugger = new Debugger(this.vm);
    }

    run(quest: QuestModel): void {
        if (this.animation_frame != undefined) {
            cancelAnimationFrame(this.animation_frame);
        }

        this.debugger.reset();

        this.quest = quest;
        this._game_state.reset();

        this.vm.load_object_code(quest.object_code, quest.episode);
        this.vm.start_thread(0);

        this._state.val = QuestRunnerState.Startup;
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
        this._state.val = QuestRunnerState.Stopped;
        this._breakpoint_location.val = undefined;
        this._game_state.reset();
    }

    /**
     * @returns false if there already was a breakpoint.
     */
    set_breakpoint(line_no: number): boolean {
        const set = this.debugger.set_breakpoint(line_no);
        this._breakpoints.splice(0, Infinity, ...this.debugger.breakpoints);
        return set;
    }

    /**
     * @returns false if there was no breakpoint to remove.
     */
    remove_breakpoint(line_no: number): boolean {
        const removed = this.debugger.remove_breakpoint(line_no);
        this._breakpoints.splice(0, Infinity, ...this.debugger.breakpoints);
        return removed;
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

    /**
     * Executes instructions until all threads have yielded or a breakpoint is hit.
     */
    private execution_loop = (): void => {
        this.vm.vsync();

        const prev_state = this._state.val;

        exec_loop: while (true) {
            if (this.executed_since_advance) {
                this.vm.advance();

                this.executed_since_advance = false;

                if (this.vm.halted) {
                    this.stop();
                    break exec_loop;
                }

                const srcloc = this.vm.get_current_source_location();

                if (srcloc) {
                    // Check if need to break.
                    const hit_breakpoint = this.debugger.breakpoint_hit(srcloc);

                    if (hit_breakpoint) {
                        this._state.val =
                            prev_state === QuestRunnerState.Startup
                                ? QuestRunnerState.StartupPaused
                                : QuestRunnerState.Paused;
                        this._breakpoint_location.val = srcloc.line_no;
                        break exec_loop;
                    }
                }
            }

            const result = this.vm.execute(false);
            this.executed_since_advance = true;

            // Limit amount of instructions executed to prevent infinite loops.
            if (++this.execution_counter >= this.execution_max_count) {
                this.stop();
                logger.error(
                    "Terminated: Maximum execution count reached. The code probably contains an infinite loop.",
                );
                break exec_loop;
            }

            switch (result) {
                case ExecutionResult.WaitingVsync:
                    this._state.val = QuestRunnerState.Running;
                    this.schedule_frame();
                    break exec_loop;
                case ExecutionResult.WaitingInput:
                    // TODO: implement input from gui
                    this._state.val = QuestRunnerState.Running;
                    this.schedule_frame();
                    break exec_loop;
                case ExecutionResult.WaitingSelection:
                    // TODO: implement input from gui
                    this.vm.list_select(0);
                    this._state.val = QuestRunnerState.Running;
                    this.schedule_frame();
                    break exec_loop;
                case ExecutionResult.Halted:
                    this.stop();
                    break exec_loop;
            }
        }

        this.execution_counter = 0;

        if (
            (prev_state === QuestRunnerState.Startup ||
                prev_state === QuestRunnerState.StartupPaused) &&
            !(
                this._state.val === QuestRunnerState.Startup ||
                this._state.val === QuestRunnerState.StartupPaused
            )
        ) {
            const quest = this.quest;
            defined(quest);

            const area_variant = area_store.get_variant(
                this._game_state.episode,
                this.initial_area_id,
                this.initial_area_variant_id,
            );
            this._game_state.current_area_variant.val = area_variant;
            this._game_state.objects.push(
                ...quest.objects.val.filter(obj => obj.area_id === area_variant.area.id),
            );
        }
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
