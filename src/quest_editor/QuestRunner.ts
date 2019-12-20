import { ExecutionResult, VirtualMachine } from "./scripting/vm/VirtualMachine";
import { QuestModel } from "./model/QuestModel";
import { VirtualMachineIO } from "./scripting/vm/io";
import { AsmToken } from "./scripting/instructions";
import { WritableProperty } from "../core/observable/property/WritableProperty";
import { list_property, property } from "../core/observable";
import { Property } from "../core/observable/property/Property";
import { log_store } from "./stores/LogStore";
import { Breakpoint, Debugger } from "./scripting/vm/Debugger";
import { WritableListProperty } from "../core/observable/property/list/WritableListProperty";
import { ListProperty } from "../core/observable/property/list/ListProperty";
import { AreaVariantModel } from "./model/AreaVariantModel";
import { Episode } from "../core/data_formats/parsing/quest/Episode";
import { area_store } from "./stores/AreaStore";
import { QuestNpcModel } from "./model/QuestNpcModel";
import { QuestObjectModel } from "./model/QuestObjectModel";
import { defined } from "../core/util";

export enum QuestRunnerState {
    /**
     * No quest is loading or loaded quest is not running.
     */
    Stopped,
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
    /**
     * Maps area ids to function labels.
     */
    readonly floor_handlers: Map<number, number>;
    /**
     * Maps area ids to area variants.
     */
    readonly area_variants: Map<number, AreaVariantModel>;
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
    private startup = true;
    private readonly _state: WritableProperty<QuestRunnerState> = property(
        QuestRunnerState.Stopped,
    );

    private initial_area_id = 0;

    private readonly _breakpoints: WritableListProperty<Breakpoint> = list_property();
    private readonly _pause_location: WritableProperty<number | undefined> = property(undefined);

    private readonly debugger: Debugger;

    private readonly _game_state = {
        episode: Episode.I,
        floor_handlers: new Map<number, number>(),
        area_variants: new Map<number, AreaVariantModel>(),
        current_area_variant: property<AreaVariantModel | undefined>(undefined),
        npcs: list_property<QuestNpcModel>(),
        objects: list_property<QuestObjectModel>(),

        reset() {
            this.episode = Episode.I;
            this.floor_handlers = new Map<number, number>();
            this.area_variants = new Map<number, AreaVariantModel>();
            this.current_area_variant.val = undefined;
            this.npcs.clear();
            this.objects.clear();
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
        state => state === QuestRunnerState.Paused,
    );
    readonly breakpoints: ListProperty<Breakpoint> = this._breakpoints;
    readonly pause_location: Property<number | undefined> = this._pause_location;

    readonly game_state: GameState = this._game_state;

    constructor() {
        this.vm = new VirtualMachine(this.create_vm_io());
        this.debugger = new Debugger(this.vm);
    }

    run(quest: QuestModel): void {
        if (this.animation_frame != undefined) {
            cancelAnimationFrame(this.animation_frame);
        }

        this.quest = quest;
        this.startup = true;
        this._game_state.reset();

        this.vm.load_object_code(quest.object_code, quest.episode);
        this.vm.start_thread(0);
        this.debugger.reset();

        this._state.val = QuestRunnerState.Running;

        this.schedule_frame();
    }

    resume(): void {
        this.debugger.resume();
        this.schedule_frame();
    }

    step_over(): void {
        this.debugger.step_over();
        this.schedule_frame();
    }

    step_into(): void {
        this.debugger.step_in();
        this.schedule_frame();
    }

    step_out(): void {
        this.debugger.step_out();
        this.schedule_frame();
    }

    stop(): void {
        this.vm.halt();
        this.debugger.reset();
        this._state.val = QuestRunnerState.Stopped;
        this._pause_location.val = undefined;
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
        if (this.animation_frame == undefined) {
            this.animation_frame = requestAnimationFrame(this.execution_loop);
        }
    }

    /**
     * Executes instructions until all threads have yielded or a breakpoint is hit.
     */
    private execution_loop = (): void => {
        this.animation_frame = undefined;

        this.vm.vsync();

        const result = this.vm.execute();

        let pause_location: number | undefined;

        switch (result) {
            case ExecutionResult.Suspended:
                this._state.val = QuestRunnerState.Running;
                break;

            case ExecutionResult.Paused:
                this._state.val = QuestRunnerState.Paused;

                pause_location = this.vm.get_current_instruction_pointer()?.source_location
                    ?.line_no;

                break;

            case ExecutionResult.WaitingVsync:
                this._state.val = QuestRunnerState.Running;
                this.schedule_frame();
                break;

            case ExecutionResult.WaitingInput:
                // TODO: implement input from gui
                this._state.val = QuestRunnerState.Running;
                this.schedule_frame();
                break;

            case ExecutionResult.WaitingSelection:
                // TODO: implement input from gui
                this.vm.list_select(0);
                this._state.val = QuestRunnerState.Running;
                this.schedule_frame();
                break;

            case ExecutionResult.Halted:
                this.stop();
                break;
        }

        this._pause_location.val = pause_location;

        if (this.startup && this._state.val === QuestRunnerState.Running) {
            this.startup = false;
            // At this point we know function 0 has run. All area variants have been designated and
            // all floor handlers have been registered.
            this.run_floor_handler(
                this._game_state.area_variants.get(this.initial_area_id) ||
                    area_store.get_variant(this._game_state.episode, this.initial_area_id, 0),
            );
        }
    };

    private create_vm_io = (): VirtualMachineIO => {
        function srcloc_to_string(srcloc?: AsmToken): string {
            return srcloc ? ` [${srcloc.line_no}:${srcloc.col}]` : " ";
        }

        return {
            bb_map_designate: (
                area_id: number,
                map_number: number,
                area_variant_id: number,
            ): void => {
                this._game_state.area_variants.set(
                    area_id,
                    area_store.get_variant(this._game_state.episode, area_id, area_variant_id),
                );
            },

            set_floor_handler: (area_id: number, label: number) => {
                this._game_state.floor_handlers.set(area_id, label);
            },

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

            p_dead_v3: (): boolean => {
                // Players never die.
                return false;
            },

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

    private run_floor_handler(area_variant: AreaVariantModel): void {
        const quest = this.quest;
        defined(quest);

        const area_id = area_variant.area.id;

        this._game_state.current_area_variant.val = area_variant;
        this._game_state.objects.push(...quest.objects.val.filter(obj => obj.area_id === area_id));

        const label = this._game_state.floor_handlers.get(area_id);

        if (label == undefined) {
            this.quest_logger.debug(`No floor handler registered for floor ${area_id}.`);
        } else {
            this.vm.start_thread(label);
            this.schedule_frame();
        }
    }
}
