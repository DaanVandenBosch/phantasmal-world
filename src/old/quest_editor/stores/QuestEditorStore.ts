import Logger from "js-logger";
import { action, flow, observable } from "mobx";
import { Endianness } from "../../../core/data_formats/Endianness";
import { ArrayBufferCursor } from "../../../core/data_formats/cursor/ArrayBufferCursor";
import { parse_quest, write_quest_qst } from "../../../core/data_formats/parsing/quest";
import { Vec3 } from "../../../core/data_formats/vector";
import { read_file } from "../../../core/read_file";
import { SimpleUndo, UndoStack } from "../../core/undo";
import { area_store } from "./AreaStore";
import { create_new_quest } from "./quest_creation";
import { Episode } from "../../../core/data_formats/parsing/quest/Episode";
import { entity_data } from "../../../core/data_formats/parsing/quest/entities";
import { ObservableQuest } from "../domain/ObservableQuest";
import { ObservableArea } from "../domain/ObservableArea";
import { Section } from "../domain/Section";
import {
    ObservableQuestEntity,
    ObservableQuestNpc,
    ObservableQuestObject,
} from "../domain/observable_quest_entities";

const logger = Logger.get("stores/QuestEditorStore");

class QuestEditorStore {
    @observable debug = false;

    readonly undo = new UndoStack();
    readonly script_undo = new SimpleUndo("Text edits", () => {}, () => {});

    @observable current_quest_filename?: string;
    @observable current_quest?: ObservableQuest;
    @observable current_area?: ObservableArea;

    @observable selected_entity?: ObservableQuestEntity;

    @observable save_dialog_filename?: string;
    @observable save_dialog_open: boolean = false;

    constructor() {
        // application_store.on_global_keyup("quest_editor", "Ctrl-Z", () => {
        //     // Let Monaco handle its own key bindings.
        //     if (undo_manager.current !== this.script_undo) {
        //         undo_manager.undo();
        //     }
        // });
        // application_store.on_global_keyup("quest_editor", "Ctrl-Shift-Z", () => {
        //     // Let Monaco handle its own key bindings.
        //     if (undo_manager.current !== this.script_undo) {
        //         undo_manager.redo();
        //     }
        // });
        // application_store.on_global_keyup("quest_editor", "Ctrl-Alt-D", this.toggle_debug);
    }

    @action
    toggle_debug = () => {
        this.debug = !this.debug;
    };

    @action
    set_selected_entity = (entity?: ObservableQuestEntity) => {
        if (entity) {
            this.set_current_area_id(entity.area_id);
        }

        this.selected_entity = entity;
    };

    @action
    set_current_area_id = (area_id?: number) => {
        this.selected_entity = undefined;

        if (area_id == undefined) {
            this.current_area = undefined;
        } else if (this.current_quest) {
            this.current_area = area_store.get_area(this.current_quest.episode, area_id);
        }
    };

    @action
    new_quest = (episode: Episode) => {
        this.set_quest(create_new_quest(episode));
    };

    // TODO: notify user of problems.
    open_file = flow(function* open_file(this: QuestEditorStore, filename: string, file: File) {
        try {
            const buffer = yield read_file(file);
            const quest = parse_quest(new ArrayBufferCursor(buffer, Endianness.Little));
            this.set_quest(
                quest &&
                    new ObservableQuest(
                        quest.id,
                        quest.language,
                        quest.name,
                        quest.short_description,
                        quest.long_description,
                        quest.episode,
                        quest.map_designations,
                        quest.objects.map(
                            obj =>
                                new ObservableQuestObject(
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
                                new ObservableQuestNpc(
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
                filename,
            );
        } catch (e) {
            logger.error("Couldn't read file.", e);
        }
    });

    @action
    open_save_dialog = () => {
        this.save_dialog_filename = this.current_quest_filename
            ? this.current_quest_filename.endsWith(".qst")
                ? this.current_quest_filename.slice(0, -4)
                : this.current_quest_filename
            : "";

        this.save_dialog_open = true;
    };

    @action
    close_save_dialog = () => {
        this.save_dialog_open = false;
    };

    @action
    set_save_dialog_filename = (filename: string) => {
        this.save_dialog_filename = filename;
    };

    save_current_quest_to_file = (file_name: string) => {
        const quest = this.current_quest;

        if (quest) {
            const buffer = write_quest_qst(
                {
                    id: quest.id,
                    language: quest.language,
                    name: quest.name,
                    short_description: quest.short_description,
                    long_description: quest.long_description,
                    episode: quest.episode,
                    objects: quest.objects.map(obj => ({
                        type: obj.type,
                        area_id: obj.area_id,
                        section_id: obj.section_id,
                        position: obj.position,
                        rotation: obj.rotation,
                        unknown: obj.unknown,
                        id: obj.id,
                        group_id: obj.group_id,
                        properties: obj.props(),
                    })),
                    npcs: quest.npcs.map(npc => ({
                        type: npc.type,
                        area_id: npc.area_id,
                        section_id: npc.section_id,
                        position: npc.position,
                        rotation: npc.rotation,
                        scale: npc.scale,
                        unknown: npc.unknown,
                        pso_type_id: npc.pso_type_id,
                        npc_id: npc.npc_id,
                        script_label: npc.script_label,
                        roaming: npc.roaming,
                    })),
                    dat_unknowns: quest.dat_unknowns,
                    object_code: quest.object_code,
                    shop_items: quest.shop_items,
                    map_designations: quest.map_designations,
                },
                file_name,
            );

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
        }

        this.save_dialog_open = false;
    };

    @action
    push_id_edit_action = (old_id: number, new_id: number) => {
        const quest = quest_editor_store.current_quest;
        if (quest) quest.set_id(new_id);

        this.undo.push_action(
            `Edit ID`,
            () => {
                const quest = quest_editor_store.current_quest;
                if (quest) quest.set_id(old_id);
            },
            () => {
                const quest = quest_editor_store.current_quest;
                if (quest) quest.set_id(new_id);
            },
        );
    };

    @action
    push_name_edit_action = (old_name: string, new_name: string) => {
        const quest = quest_editor_store.current_quest;
        if (quest) quest.set_name(new_name);

        this.undo.push_action(
            `Edit name`,
            () => {
                const quest = quest_editor_store.current_quest;
                if (quest) quest.set_name(old_name);
            },
            () => {
                const quest = quest_editor_store.current_quest;
                if (quest) quest.set_name(new_name);
            },
        );
    };

    @action
    push_short_description_edit_action = (
        old_short_description: string,
        new_short_description: string,
    ) => {
        const quest = quest_editor_store.current_quest;
        if (quest) quest.set_short_description(new_short_description);

        this.undo.push_action(
            `Edit short description`,
            () => {
                const quest = quest_editor_store.current_quest;
                if (quest) quest.set_short_description(old_short_description);
            },
            () => {
                const quest = quest_editor_store.current_quest;
                if (quest) quest.set_short_description(new_short_description);
            },
        );
    };

    @action
    push_long_description_edit_action = (
        old_long_description: string,
        new_long_description: string,
    ) => {
        const quest = quest_editor_store.current_quest;
        if (quest) quest.set_long_description(new_long_description);

        this.undo.push_action(
            `Edit long description`,
            () => {
                const quest = quest_editor_store.current_quest;
                if (quest) quest.set_long_description(old_long_description);
            },
            () => {
                const quest = quest_editor_store.current_quest;
                if (quest) quest.set_long_description(new_long_description);
            },
        );
    };

    @action
    push_entity_move_action = (
        entity: ObservableQuestEntity,
        old_position: Vec3,
        new_position: Vec3,
    ) => {
        this.undo.push_action(
            `Move ${entity_data(entity.type).name}`,
            () => {
                entity.world_position = old_position;
                quest_editor_store.set_selected_entity(entity);
            },
            () => {
                entity.world_position = new_position;
                quest_editor_store.set_selected_entity(entity);
            },
        );
    };

    @action
    private set_quest = flow(function* set_quest(
        this: QuestEditorStore,
        quest?: ObservableQuest,
        filename?: string,
    ) {
        this.current_quest_filename = filename;

        if (quest !== this.current_quest) {
            this.undo.reset();
            this.script_undo.reset();
            this.selected_entity = undefined;
            this.current_quest = quest;

            if (quest) {
                this.current_area = area_store.get_area(quest.episode, 0);
            } else {
                this.current_area = undefined;
            }

            if (quest) {
                // Load section data.
                for (const variant of quest.area_variants) {
                    const sections = yield area_store.get_area_sections(
                        quest.episode,
                        variant.area.id,
                        variant.id,
                    );
                    variant.sections.replace(sections);

                    for (const object of quest.objects.filter(o => o.area_id === variant.area.id)) {
                        try {
                            this.set_section_on_quest_entity(object, sections);
                        } catch (e) {
                            logger.error(e);
                        }
                    }

                    for (const npc of quest.npcs.filter(npc => npc.area_id === variant.area.id)) {
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
    });

    private set_section_on_quest_entity = (entity: ObservableQuestEntity, sections: Section[]) => {
        const section = sections.find(s => s.id === entity.section_id);

        if (section) {
            entity.section = section;
        } else {
            logger.warn(`Section ${entity.section_id} not found.`);
        }
    };
}

export const quest_editor_store = new QuestEditorStore();
