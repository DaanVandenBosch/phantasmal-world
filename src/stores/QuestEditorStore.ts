import Logger from "js-logger";
import { action, observable, runInAction } from "mobx";
import { Endianness } from "../data_formats";
import { ArrayBufferCursor } from "../data_formats/cursor/ArrayBufferCursor";
import { parse_quest, write_quest_qst } from "../data_formats/parsing/quest";
import { Vec3 } from "../data_formats/vector";
import { Area, Quest, QuestEntity, Section } from "../domain";
import { read_file } from "../read_file";
import { area_store } from "./AreaStore";

const logger = Logger.get("stores/QuestEditorStore");

class QuestEditorStore {
    @observable current_quest?: Quest;
    @observable current_area?: Area;
    @observable selected_entity?: QuestEntity;

    set_quest = action("set_quest", (quest?: Quest) => {
        this.reset_quest_state();
        this.current_quest = quest;

        if (quest && quest.area_variants.length) {
            this.current_area = quest.area_variants[0].area;
        }
    });

    private reset_quest_state(): void {
        this.current_quest = undefined;
        this.current_area = undefined;
        this.selected_entity = undefined;
    }

    set_selected_entity = (entity?: QuestEntity) => {
        this.selected_entity = entity;
    };

    set_current_area_id = action("set_current_area_id", (area_id?: number) => {
        this.selected_entity = undefined;

        if (area_id == null) {
            this.current_area = undefined;
        } else if (this.current_quest) {
            const area_variant = this.current_quest.area_variants.find(
                variant => variant.area.id === area_id
            );
            this.current_area = area_variant && area_variant.area;
        }
    });

    // TODO: notify user of problems.
    load_file = async (file: File) => {
        try {
            const buffer = await read_file(file);
            const quest = parse_quest(new ArrayBufferCursor(buffer, Endianness.Little));
            this.set_quest(quest);

            if (quest) {
                // Load section data.
                for (const variant of quest.area_variants) {
                    const sections = await area_store.get_area_sections(
                        quest.episode,
                        variant.area.id,
                        variant.id
                    );
                    variant.sections = sections;

                    // Generate object geometry.
                    for (const object of quest.objects.filter(o => o.area_id === variant.area.id)) {
                        try {
                            this.set_section_on_visible_quest_entity(object, sections);
                        } catch (e) {
                            logger.error(e);
                        }
                    }

                    // Generate NPC geometry.
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
        } catch (e) {
            logger.error("Couldn't read file.", e);
        }
    };

    private set_section_on_visible_quest_entity = async (
        entity: QuestEntity,
        sections: Section[]
    ) => {
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

        runInAction(() => {
            entity.section = section;
            entity.position = new Vec3(x, y, z);
        });
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
    };
}

export const quest_editor_store = new QuestEditorStore();
