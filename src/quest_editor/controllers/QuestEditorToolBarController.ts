import { GuiStore, GuiTool } from "../../core/stores/GuiStore";
import { AreaStore } from "../stores/AreaStore";
import { QuestEditorStore } from "../stores/QuestEditorStore";
import { AreaModel } from "../model/AreaModel";
import { list_property, map } from "../../core/observable";
import { Property } from "../../core/observable/property/Property";
import { undo_manager } from "../../core/undo/UndoManager";
import { Controller } from "../../core/controllers/Controller";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import { create_new_quest } from "../stores/quest_creation";
import { read_file } from "../../core/read_file";
import {
    parse_bin_dat_to_quest,
    parse_qst_to_quest,
    Quest,
    write_quest_qst,
} from "../../core/data_formats/parsing/quest";
import { ArrayBufferCursor } from "../../core/data_formats/cursor/ArrayBufferCursor";
import { Endianness } from "../../core/data_formats/Endianness";
import { convert_quest_from_model, convert_quest_to_model } from "../stores/model_conversion";
import { LogManager } from "../../core/Logger";
import { input } from "../../core/gui/dom";
import { basename } from "../../core/util";

const logger = LogManager.get("quest_editor/controllers/QuestEditorToolBarController");

export type AreaAndLabel = { readonly area: AreaModel; readonly label: string };

export class QuestEditorToolBarController extends Controller {
    private quest_filename?: string;

    readonly vm_feature_active: boolean;
    readonly areas: Property<readonly AreaAndLabel[]>;
    readonly current_area: Property<AreaAndLabel>;
    readonly can_save: Property<boolean>;
    readonly can_undo: Property<boolean>;
    readonly can_redo: Property<boolean>;
    readonly can_select_area: Property<boolean>;
    readonly can_debug: Property<boolean>;
    readonly can_step: Property<boolean>;
    readonly can_stop: Property<boolean>;

    constructor(
        gui_store: GuiStore,
        private readonly area_store: AreaStore,
        private readonly quest_editor_store: QuestEditorStore,
    ) {
        super();

        this.vm_feature_active = gui_store.feature_active("vm");

        // Ensure the areas list is updated when entities are added or removed (the count in the
        // label should update).
        this.areas = quest_editor_store.current_quest.flat_map(quest => {
            if (quest) {
                return quest?.entities_per_area.flat_map(entities_per_area => {
                    return list_property<AreaAndLabel>(
                        undefined,
                        ...area_store.get_areas_for_episode(quest.episode).map(area => {
                            const entity_count = entities_per_area.get(area.id);
                            return {
                                area,
                                label: area.name + (entity_count ? ` (${entity_count})` : ""),
                            };
                        }),
                    );
                });
            } else {
                return list_property<AreaAndLabel>();
            }
        });

        this.current_area = map(
            (areas, area) => areas.find(al => al.area == area)!,
            this.areas,
            quest_editor_store.current_area,
        );

        const quest_loaded = quest_editor_store.current_quest.map(q => q != undefined);
        this.can_save = quest_loaded;
        this.can_select_area = quest_loaded;
        this.can_debug = quest_loaded;

        this.can_undo = map(
            (c, r) => c && !r,
            undo_manager.can_undo,
            quest_editor_store.quest_runner.running,
        );

        this.can_redo = map(
            (c, r) => c && !r,
            undo_manager.can_redo,
            quest_editor_store.quest_runner.running,
        );

        this.can_step = quest_editor_store.quest_runner.paused;

        this.can_stop = quest_editor_store.quest_runner.running;

        this.disposables(
            quest_editor_store.current_quest.observe(() => {
                this.quest_filename = undefined;
            }),

            gui_store.on_global_keydown(GuiTool.QuestEditor, "Ctrl-O", () => {
                const input_element = input();
                input_element.type = "file";
                input_element.multiple = true;
                input_element.onchange = () => {
                    if (input_element.files && input_element.files.length) {
                        this.open_files(Array.prototype.slice.apply(input_element.files));
                    }
                };
                input_element.click();
            }),

            gui_store.on_global_keydown(GuiTool.QuestEditor, "Ctrl-Shift-S", this.save_as),

            gui_store.on_global_keydown(GuiTool.QuestEditor, "Ctrl-Z", () => {
                undo_manager.undo();
            }),

            gui_store.on_global_keydown(GuiTool.QuestEditor, "Ctrl-Shift-Z", () => {
                undo_manager.redo();
            }),

            gui_store.on_global_keydown(GuiTool.QuestEditor, "F5", this.debug),

            gui_store.on_global_keydown(GuiTool.QuestEditor, "Shift-F5", this.stop),

            gui_store.on_global_keydown(GuiTool.QuestEditor, "F6", this.resume),

            gui_store.on_global_keydown(GuiTool.QuestEditor, "F8", this.step_over),

            gui_store.on_global_keydown(GuiTool.QuestEditor, "F7", this.step_in),

            gui_store.on_global_keydown(GuiTool.QuestEditor, "Shift-F8", this.step_out),
        );
    }

    create_new_quest = async (episode: Episode): Promise<void> =>
        this.quest_editor_store.set_current_quest(create_new_quest(this.area_store, episode));

    // TODO: notify user of problems.
    open_files = async (files: File[]): Promise<void> => {
        try {
            let quest: Quest | undefined;

            const qst = files.find(f => f.name.toLowerCase().endsWith(".qst"));

            if (qst) {
                const buffer = await read_file(qst);
                quest = parse_qst_to_quest(new ArrayBufferCursor(buffer, Endianness.Little));
                this.quest_filename = qst.name;
            } else {
                const bin = files.find(f => f.name.toLowerCase().endsWith(".bin"));
                const dat = files.find(f => f.name.toLowerCase().endsWith(".dat"));

                if (bin && dat) {
                    const bin_buffer = await read_file(bin);
                    const dat_buffer = await read_file(dat);
                    quest = parse_bin_dat_to_quest(
                        new ArrayBufferCursor(bin_buffer, Endianness.Little),
                        new ArrayBufferCursor(dat_buffer, Endianness.Little),
                    );
                    this.quest_filename = bin.name || dat.name;
                }
            }

            if (!quest) {
                logger.error("Couldn't parse quest file.");
            }

            await this.quest_editor_store.set_current_quest(
                quest && convert_quest_to_model(this.area_store, quest),
            );
        } catch (e) {
            logger.error("Couldn't read file.", e);
        }
    };

    set_area = ({ area }: AreaAndLabel): void => {
        this.quest_editor_store.set_current_area(area);
    };

    save_as = (): void => {
        const quest = this.quest_editor_store.current_quest.val;
        if (!quest) return;

        const default_file_name = this.quest_filename && basename(this.quest_filename);

        let file_name = prompt("File name:", default_file_name);
        if (!file_name) return;

        const buffer = write_quest_qst(convert_quest_from_model(quest), file_name);

        if (!file_name.endsWith(".qst")) {
            file_name += ".qst";
        }

        const a = document.createElement("a");
        a.href = URL.createObjectURL(new Blob([buffer], { type: "application/octet-stream" }));
        a.download = file_name;
        document.body.appendChild(a);
        a.click();
        URL.revokeObjectURL(a.href);
        document.body.removeChild(a);
    };

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
}
