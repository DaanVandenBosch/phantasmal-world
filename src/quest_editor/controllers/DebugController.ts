import { Controller } from "../../core/controllers/Controller";
import { Property } from "../../core/observable/property/Property";
import { ListProperty } from "../../core/observable/property/list/ListProperty";
import { QuestEditorStore } from "../stores/QuestEditorStore";
import { GuiStore, GuiTool } from "../../core/stores/GuiStore";
import { LogEntry } from "../../core/Logger";
import { LogStore } from "../stores/LogStore";
import { Severity } from "../../core/Severity";
import { property } from "../../core/observable";

export class DebugController extends Controller {
    readonly can_debug: Property<boolean>;
    readonly can_step: Property<boolean>;
    readonly can_stop: Property<boolean>;
    readonly thread_ids: ListProperty<number>;
    readonly debugging_thread_id: Property<number | undefined>;
    readonly active_thread_id: Property<number | undefined>;
    readonly can_select_thread: Property<boolean>;
    readonly log: ListProperty<LogEntry>;
    readonly severity: Property<Severity>;

    constructor(
        gui_store: GuiStore,
        private readonly quest_editor_store: QuestEditorStore,
        private readonly log_store: LogStore,
    ) {
        super();

        this.can_debug = quest_editor_store.current_quest.map(q => q != undefined);

        this.can_step = quest_editor_store.quest_runner.paused;

        this.can_stop = quest_editor_store.quest_runner.running;

        this.thread_ids = quest_editor_store.quest_runner.thread_ids;

        this.debugging_thread_id = quest_editor_store.quest_runner.debugging_thread_id;

        this.active_thread_id = quest_editor_store.quest_runner.active_thread_id;

        this.can_select_thread = quest_editor_store.quest_runner.thread_ids.map(
            ids => ids.length > 0 && quest_editor_store.quest_runner.running.val,
        );

        this.log = log_store.log;
        this.severity = log_store.severity;

        this.disposables(
            gui_store.on_global_keydown(GuiTool.QuestEditor, "F5", this.debug),

            gui_store.on_global_keydown(GuiTool.QuestEditor, "Shift-F5", this.stop),

            gui_store.on_global_keydown(GuiTool.QuestEditor, "F6", this.resume),

            gui_store.on_global_keydown(GuiTool.QuestEditor, "F8", this.step_over),

            gui_store.on_global_keydown(GuiTool.QuestEditor, "F7", this.step_in),

            gui_store.on_global_keydown(GuiTool.QuestEditor, "Shift-F8", this.step_out),
        );
    }

    debug = (): void => {
        const quest = this.quest_editor_store.current_quest.val;

        if (quest) {
            this.quest_editor_store.quest_runner.run(quest);
        }
    };

    resume = (): void => {
        this.quest_editor_store.quest_runner.resume();
    };

    step_over = (): void => {
        this.quest_editor_store.quest_runner.step_over();
    };

    step_in = (): void => {
        this.quest_editor_store.quest_runner.step_into();
    };

    step_out = (): void => {
        this.quest_editor_store.quest_runner.step_out();
    };

    stop = (): void => {
        this.quest_editor_store.quest_runner.stop();
    };

    set_severity = (severity: Severity): void => {
        this.log_store.set_severity(severity);
    };

    select_thread = (thread_id: number): void => {
        this.quest_editor_store.quest_runner.set_debugging_thread(thread_id);
    };
}
