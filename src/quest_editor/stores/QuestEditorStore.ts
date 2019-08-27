import { property } from "../../core/observable";
import { QuestModel } from "../model/QuestModel";
import { Property, PropertyChangeEvent } from "../../core/observable/Property";
import { read_file } from "../../core/read_file";
import { parse_quest } from "../../core/data_formats/parsing/quest";
import { ArrayBufferCursor } from "../../core/data_formats/cursor/ArrayBufferCursor";
import { Endianness } from "../../core/data_formats/Endianness";
import { WritableProperty } from "../../core/observable/WritableProperty";
import { QuestObjectModel } from "../model/QuestObjectModel";
import { QuestNpcModel } from "../model/QuestNpcModel";
import { AreaModel } from "../model/AreaModel";
import { area_store } from "./AreaStore";
import { SectionModel } from "../model/SectionModel";
import { QuestEntityModel } from "../model/QuestEntityModel";
import { Vec3 } from "../../core/data_formats/vector";
import { Disposable } from "../../core/observable/Disposable";
import { Disposer } from "../../core/observable/Disposer";
import { gui_store, GuiTool } from "../../core/stores/GuiStore";
import { UndoStack } from "../../core/undo/UndoStack";
import { TranslateEntityAction } from "../actions/TranslateEntityAction";
import { EditShortDescriptionAction } from "../actions/EditShortDescriptionAction";
import { EditLongDescriptionAction } from "../actions/EditLongDescriptionAction";
import { EditNameAction } from "../actions/EditNameAction";
import { EditIdAction } from "../actions/EditIdAction";
import Logger = require("js-logger");

const logger = Logger.get("quest_editor/gui/QuestEditorStore");

export class QuestEditorStore implements Disposable {
    readonly debug: WritableProperty<boolean> = property(false);

    readonly undo = new UndoStack();

    private readonly _current_quest_filename = property<string | undefined>(undefined);
    readonly current_quest_filename: Property<string | undefined> = this._current_quest_filename;

    private readonly _current_quest = property<QuestModel | undefined>(undefined);
    readonly current_quest: Property<QuestModel | undefined> = this._current_quest;

    private readonly _current_area = property<AreaModel | undefined>(undefined);
    readonly current_area: Property<AreaModel | undefined> = this._current_area;

    private readonly _selected_entity = property<QuestEntityModel | undefined>(undefined);
    readonly selected_entity: Property<QuestEntityModel | undefined> = this._selected_entity;

    private readonly disposer = new Disposer();

    constructor() {
        this.disposer.add(
            gui_store.tool.observe(
                ({ value: tool }) => {
                    if (tool === GuiTool.QuestEditor) {
                        this.undo.make_current();
                    }
                },
                { call_now: true },
            ),
        );
    }

    dispose(): void {
        this.disposer.dispose();
    }

    set_current_area_id = (area_id?: number) => {
        this._selected_entity.val = undefined;

        if (area_id == undefined) {
            this._current_area.val = undefined;
        } else if (this.current_quest.val) {
            this._current_area.val = area_store.get_area(this.current_quest.val.episode, area_id);
        }
    };

    set_selected_entity = (entity?: QuestEntityModel) => {
        if (entity && this.current_quest.val) {
            this._current_area.val = area_store.get_area(
                this.current_quest.val.episode,
                entity.area_id,
            );
        }

        this._selected_entity.val = entity;
    };

    // TODO: notify user of problems.
    open_file = async (file: File) => {
        try {
            const buffer = await read_file(file);
            const quest = parse_quest(new ArrayBufferCursor(buffer, Endianness.Little));
            this.set_quest(
                quest &&
                    new QuestModel(
                        quest.id,
                        quest.language,
                        quest.name,
                        quest.short_description,
                        quest.long_description,
                        quest.episode,
                        quest.map_designations,
                        quest.objects.map(
                            obj =>
                                new QuestObjectModel(
                                    obj.type,
                                    obj.id,
                                    obj.group_id,
                                    obj.area_id,
                                    obj.section_id,
                                    obj.position,
                                    obj.rotation,
                                    obj.properties,
                                    obj.unknown,
                                ),
                        ),
                        quest.npcs.map(
                            npc =>
                                new QuestNpcModel(
                                    npc.type,
                                    npc.pso_type_id,
                                    npc.npc_id,
                                    npc.script_label,
                                    npc.roaming,
                                    npc.area_id,
                                    npc.section_id,
                                    npc.position,
                                    npc.rotation,
                                    npc.scale,
                                    npc.unknown,
                                ),
                        ),
                        quest.dat_unknowns,
                        quest.object_code,
                        quest.shop_items,
                    ),
                file.name,
            );
        } catch (e) {
            logger.error("Couldn't read file.", e);
        }
    };

    push_edit_id_action = (event: PropertyChangeEvent<number>) => {
        if (this.current_quest.val) {
            this.undo.push(new EditIdAction(this.current_quest.val, event)).redo();
        }
    };

    push_edit_name_action = (event: PropertyChangeEvent<string>) => {
        if (this.current_quest.val) {
            this.undo.push(new EditNameAction(this.current_quest.val, event)).redo();
        }
    };

    push_edit_short_description_action = (event: PropertyChangeEvent<string>) => {
        if (this.current_quest.val) {
            this.undo.push(new EditShortDescriptionAction(this.current_quest.val, event)).redo();
        }
    };

    push_edit_long_description_action = (event: PropertyChangeEvent<string>) => {
        if (this.current_quest.val) {
            this.undo.push(new EditLongDescriptionAction(this.current_quest.val, event)).redo();
        }
    };

    push_translate_entity_action = (
        entity: QuestEntityModel,
        old_section: SectionModel | undefined,
        new_section: SectionModel | undefined,
        old_position: Vec3,
        new_position: Vec3,
        world: boolean,
    ) => {
        this.undo
            .push(
                new TranslateEntityAction(
                    entity,
                    old_section,
                    new_section,
                    old_position,
                    new_position,
                    world,
                ),
            )
            .redo();
    };

    private async set_quest(quest?: QuestModel, filename?: string): Promise<void> {
        this.undo.reset();

        this._current_area.val = undefined;
        this._selected_entity.val = undefined;

        this._current_quest_filename.val = filename;
        this._current_quest.val = quest;

        if (quest) {
            this._current_area.val = area_store.get_area(quest.episode, 0);

            // Load section data.
            for (const variant of quest.area_variants.val) {
                const sections = await area_store.get_area_sections(
                    quest.episode,
                    variant.area.id,
                    variant.id,
                );
                variant.sections.val.splice(0, Infinity, ...sections);

                for (const object of quest.objects.val.filter(o => o.area_id === variant.area.id)) {
                    try {
                        this.set_section_on_quest_entity(object, sections);
                    } catch (e) {
                        logger.error(e);
                    }
                }

                for (const npc of quest.npcs.val.filter(npc => npc.area_id === variant.area.id)) {
                    try {
                        this.set_section_on_quest_entity(npc, sections);
                    } catch (e) {
                        logger.error(e);
                    }
                }
            }
        } else {
            logger.error("Couldn't parse quest file.");
        }
    }

    private set_section_on_quest_entity = (entity: QuestEntityModel, sections: SectionModel[]) => {
        const section = sections.find(s => s.id === entity.section_id.val);

        if (section) {
            entity.set_section(section);
        } else {
            logger.warn(`Section ${entity.section_id.val} not found.`);
        }
    };
}

export const quest_editor_store = new QuestEditorStore();
