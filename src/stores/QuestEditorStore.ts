import Logger from "js-logger";
import { action, flow, observable } from "mobx";
import { Endianness } from "../data_formats";
import { ArrayBufferCursor } from "../data_formats/cursor/ArrayBufferCursor";
import { parse_quest, write_quest_qst } from "../data_formats/parsing/quest";
import { Vec3 } from "../data_formats/vector";
import { Area, Episode, Quest, QuestEntity, Section } from "../domain";
import { read_file } from "../read_file";
import { UndoStack } from "../undo";
import { area_store } from "./AreaStore";
import { create_new_quest } from "./quest_creation";

const logger = Logger.get("stores/QuestEditorStore");

class QuestEditorStore {
    readonly undo_stack = new UndoStack();

    @observable current_quest_filename?: string;
    @observable current_quest?: Quest;
    @observable current_area?: Area;
    @observable selected_entity?: QuestEntity;

    @observable save_dialog_filename?: string;
    @observable save_dialog_open: boolean = false;

    @action
    set_selected_entity = (entity?: QuestEntity) => {
        if (entity) {
            this.set_current_area_id(entity.area_id);
        }

        this.selected_entity = entity;
    };

    @action
    set_current_area_id = (area_id?: number) => {
        this.selected_entity = undefined;

        if (area_id == null) {
            this.current_area = undefined;
        } else if (this.current_quest) {
            const area_variant = this.current_quest.area_variants.find(
                variant => variant.area.id === area_id
            );
            this.current_area = area_variant && area_variant.area;
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
            this.current_quest_filename = filename;
            this.set_quest(quest);
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
        if (this.current_quest) {
            const buffer = write_quest_qst(this.current_quest, file_name);

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
    private set_quest = flow(function* set_quest(this: QuestEditorStore, quest?: Quest) {
        if (quest !== this.current_quest) {
            this.undo_stack.clear();
            this.selected_entity = undefined;
            this.current_quest = quest;

            if (quest && quest.area_variants.length) {
                this.current_area = quest.area_variants[0].area;
            } else {
                this.current_area = undefined;
            }

            if (quest) {
                // Load section data.
                for (const variant of quest.area_variants) {
                    const sections = yield area_store.get_area_sections(
                        quest.episode,
                        variant.area.id,
                        variant.id
                    );
                    variant.sections = sections;

                    for (const object of quest.objects.filter(o => o.area_id === variant.area.id)) {
                        try {
                            this.set_section_on_visible_quest_entity(object, sections);
                        } catch (e) {
                            logger.error(e);
                        }
                    }

                    for (const npc of quest.npcs.filter(npc => npc.area_id === variant.area.id)) {
                        try {
                            this.set_section_on_visible_quest_entity(npc, sections);
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

    private set_section_on_visible_quest_entity = (entity: QuestEntity, sections: Section[]) => {
        let { x, y, z } = entity.position;

        const section = sections.find(s => s.id === entity.section_id);

        if (section) {
            const { x: sec_x, y: sec_y, z: sec_z } = section.position;
            const rot_x = section.cos_y_axis_rotation * x + section.sin_y_axis_rotation * z;
            const rot_z = -section.sin_y_axis_rotation * x + section.cos_y_axis_rotation * z;
            x = rot_x + sec_x;
            y += sec_y;
            z = rot_z + sec_z;
        } else {
            logger.warn(`Section ${entity.section_id} not found.`);
        }

        entity.set_position_and_section(new Vec3(x, y, z), section);
    };
}

export const quest_editor_store = new QuestEditorStore();
