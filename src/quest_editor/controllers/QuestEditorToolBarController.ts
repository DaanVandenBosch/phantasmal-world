import { GuiStore, GuiTool } from "../../core/stores/GuiStore";
import { AreaStore } from "../stores/AreaStore";
import { QuestEditorStore } from "../stores/QuestEditorStore";
import { AreaModel } from "../model/AreaModel";
import { list_property, map, property } from "../../core/observable";
import { Property } from "../../core/observable/property/Property";
import { undo_manager } from "../../core/undo/UndoManager";
import { Controller } from "../../core/controllers/Controller";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import { open_files, read_file } from "../../core/files";
import {
    parse_bin_dat_to_quest,
    parse_qst_to_quest,
    write_quest_qst,
} from "../../core/data_formats/parsing/quest";
import { ArrayBufferCursor } from "../../core/data_formats/block/cursor/ArrayBufferCursor";
import { Endianness } from "../../core/data_formats/block/Endianness";
import { convert_quest_from_model, convert_quest_to_model } from "../stores/model_conversion";
import { LogManager } from "../../core/logging";
import { basename } from "../../core/util";
import { Version } from "../../core/data_formats/parsing/quest/Version";
import { WritableProperty } from "../../core/observable/property/WritableProperty";
import { failure, problem, Result } from "../../core/Result";
import { Severity } from "../../core/Severity";
import { Quest } from "../../core/data_formats/parsing/quest/Quest";
import { QuestLoader } from "../loading/QuestLoader";

const logger = LogManager.get("quest_editor/controllers/QuestEditorToolBarController");

export type AreaAndLabel = { readonly area: AreaModel; readonly label: string };

export class QuestEditorToolBarController extends Controller {
    private readonly _result_dialog_visible = property(false);
    private readonly _result: WritableProperty<Result<unknown> | undefined> = property(undefined);
    private readonly _result_problems_message = property("");
    private readonly _result_error_message = property("");

    private _save_as_dialog_visible = property(false);
    private _filename = property("");
    private _version = property(Version.BB);

    readonly result_dialog_visible: Property<boolean> = this._result_dialog_visible;
    readonly result: Property<Result<unknown> | undefined> = this._result;
    readonly result_problems_message: Property<string> = this._result_problems_message;
    readonly result_error_message: Property<string> = this._result_error_message;

    readonly areas: Property<readonly AreaAndLabel[]>;
    readonly current_area: Property<AreaAndLabel>;
    readonly can_save: Property<boolean>;
    readonly can_undo: Property<boolean>;
    readonly can_redo: Property<boolean>;
    readonly can_select_area: Property<boolean>;
    readonly save_as_dialog_visible: Property<boolean> = this._save_as_dialog_visible;
    readonly filename: Property<string> = this._filename;
    readonly version: Property<Version> = this._version;

    constructor(
        private readonly quest_loader: QuestLoader,
        gui_store: GuiStore,
        private readonly area_store: AreaStore,
        private readonly quest_editor_store: QuestEditorStore,
    ) {
        super();

        // Ensure the areas list is updated when entities are added or removed (the count in the
        // label should update).
        this.areas = quest_editor_store.current_quest.flat_map(quest => {
            if (quest) {
                return quest.entities_per_area.flat_map(entities_per_area => {
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

        this.disposables(
            gui_store.on_global_keydown(GuiTool.QuestEditor, "Ctrl-O", async () => {
                const files = await open_files({ accept: ".bin, .dat, .qst", multiple: true });
                await this.parse_files(files);
            }),

            gui_store.on_global_keydown(GuiTool.QuestEditor, "Ctrl-Shift-S", this.save_as_clicked),

            gui_store.on_global_keydown(GuiTool.QuestEditor, "Ctrl-Z", () => {
                undo_manager.undo();
            }),

            gui_store.on_global_keydown(GuiTool.QuestEditor, "Ctrl-Shift-Z", () => {
                undo_manager.redo();
            }),

            gui_store.on_global_keydown(GuiTool.QuestEditor, "Ctrl-Y", () => {
                undo_manager.redo();
            }),
        );
    }

    create_new_quest = async (episode: Episode): Promise<void> => {
        this.set_filename("");
        this.set_version(Version.BB);
        await this.quest_editor_store.set_current_quest(
            convert_quest_to_model(
                this.area_store,
                await this.quest_loader.load_default_quest(episode),
            ),
        );
    };

    parse_files = async (files: File[]): Promise<void> => {
        try {
            if (files.length === 0) return;

            let quest: Quest | undefined;

            const qst = files.find(f => f.name.toLowerCase().endsWith(".qst"));

            if (qst) {
                const buffer = await read_file(qst);
                const parse_result = parse_qst_to_quest(
                    new ArrayBufferCursor(buffer, Endianness.Little),
                );
                this.set_result(parse_result);

                if (parse_result.success) {
                    quest = parse_result.value.quest;
                    this.set_version(parse_result.value.version);
                    this.set_filename(basename(qst.name));
                }
            } else {
                const bin = files.find(f => f.name.toLowerCase().endsWith(".bin"));
                const dat = files.find(f => f.name.toLowerCase().endsWith(".dat"));

                if (bin && dat) {
                    const bin_buffer = await read_file(bin);
                    const dat_buffer = await read_file(dat);
                    const parse_result = parse_bin_dat_to_quest(
                        new ArrayBufferCursor(bin_buffer, Endianness.Little),
                        new ArrayBufferCursor(dat_buffer, Endianness.Little),
                    );
                    this.set_result(parse_result);

                    if (parse_result.success) {
                        quest = parse_result.value;
                        this.set_filename(basename(bin.name || dat.name));
                    }
                } else {
                    this.set_result(
                        failure(
                            problem(
                                Severity.Error,
                                "Please select a .qst file or one .bin and one .dat file.",
                            ),
                        ),
                    );
                }
            }

            if (quest) {
                await this.quest_editor_store.set_current_quest(
                    convert_quest_to_model(this.area_store, quest),
                );
            }
        } catch (e) {
            logger.error("Couldn't read file.", e);
            this.set_result(failure(problem(Severity.Error, e.message)));
        }
    };

    set_area = ({ area }: AreaAndLabel): void => {
        this.quest_editor_store.set_current_area(area);
    };

    save_as_clicked = (): void => {
        if (this.quest_editor_store.current_quest.val) {
            this._save_as_dialog_visible.val = true;
        }
    };

    save_as = (): void => {
        const quest = this.quest_editor_store.current_quest.val;
        if (!quest) return;

        const format = this.version.val;
        if (format === undefined) return;

        let filename = this.filename.val;
        const buffer = write_quest_qst(convert_quest_from_model(quest), filename, format, true);

        if (!filename.endsWith(".qst")) {
            filename += ".qst";
        }

        const a = document.createElement("a");
        a.href = URL.createObjectURL(new Blob([buffer], { type: "application/octet-stream" }));
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        URL.revokeObjectURL(a.href);
        document.body.removeChild(a);

        this.dismiss_save_as_dialog();
    };

    dismiss_save_as_dialog = (): void => {
        this._save_as_dialog_visible.val = false;
    };

    set_filename = (filename: string): void => {
        this._filename.val = filename;
    };

    set_version = (version: Version): void => {
        // We only support GC and BB at the moment.
        switch (version) {
            case Version.DC:
            case Version.GC:
                this._version.val = Version.GC;
                break;
            case Version.PC:
            case Version.BB:
                this._version.val = Version.BB;
                break;
        }
    };

    dismiss_result_dialog = (): void => {
        this._result_dialog_visible.val = false;
    };

    private set_result(result: Result<unknown>): void {
        this._result.val = result;

        if (result.problems.length) {
            this._result_dialog_visible.val = true;
        }
    }
}
