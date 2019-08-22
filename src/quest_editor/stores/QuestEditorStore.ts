import { property } from "../../core/observable";
import { ObservableQuest } from "../domain/ObservableQuest";
import { Property } from "../../core/observable/Property";
import { read_file } from "../../core/read_file";
import { parse_quest } from "../../core/data_formats/parsing/quest";
import { ArrayBufferCursor } from "../../core/data_formats/cursor/ArrayBufferCursor";
import { Endianness } from "../../core/data_formats/Endianness";
import { SimpleUndo, UndoStack } from "../../old/core/undo";
import Logger = require("js-logger");

const logger = Logger.get("quest_editor/gui/QuestEditorStore");

export class QuestEditorStore {
    readonly undo = new UndoStack();
    readonly script_undo = new SimpleUndo("Text edits", () => {}, () => {});

    private readonly _current_quest = property<ObservableQuest | undefined>(undefined);
    readonly current_quest: Property<ObservableQuest | undefined> = this._current_quest;

    // TODO: notify user of problems.
    open_file = async (file: File) => {
        try {
            const buffer = await read_file(file);
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
                        // quest.map_designations,
                        // quest.objects.map(
                        //     obj =>
                        //         new ObservableQuestObject(
                        //             obj.type,
                        //             obj.id,
                        //             obj.group_id,
                        //             obj.area_id,
                        //             obj.section_id,
                        //             obj.position,
                        //             obj.rotation,
                        //             obj.properties,
                        //             obj.unknown,
                        //         ),
                        // ),
                        // quest.npcs.map(
                        //     npc =>
                        //         new ObservableQuestNpc(
                        //             npc.type,
                        //             npc.pso_type_id,
                        //             npc.npc_id,
                        //             npc.script_label,
                        //             npc.roaming,
                        //             npc.area_id,
                        //             npc.section_id,
                        //             npc.position,
                        //             npc.rotation,
                        //             npc.scale,
                        //             npc.unknown,
                        //         ),
                        // ),
                        // quest.dat_unknowns,
                        // quest.object_code,
                        // quest.shop_items,
                    ),
                file.name,
            );
        } catch (e) {
            logger.error("Couldn't read file.", e);
        }
    };

    private set_quest(quest?: ObservableQuest, filename?: string): void {
        // this.current_quest_filename = filename;
        this.undo.reset();
        this.script_undo.reset();

        // if (quest) {
        //     this.current_area = area_store.get_area(quest.episode, 0);
        // } else {
        //     this.current_area = undefined;
        // }

        if (quest) {
            // Load section data.
            // for (const variant of quest.area_variants) {
            //     const sections = yield area_store.get_area_sections(
            //         quest.episode,
            //         variant.area.id,
            //         variant.id,
            //     );
            //     variant.sections.replace(sections);
            //
            //     for (const object of quest.objects.filter(o => o.area_id === variant.area.id)) {
            //         try {
            //             this.set_section_on_quest_entity(object, sections);
            //         } catch (e) {
            //             logger.error(e);
            //         }
            //     }
            //
            //     for (const npc of quest.npcs.filter(npc => npc.area_id === variant.area.id)) {
            //         try {
            //             this.set_section_on_quest_entity(npc, sections);
            //         } catch (e) {
            //             logger.error(e);
            //         }
            //     }
            // }
        } else {
            logger.error("Couldn't parse quest file.");
        }

        // this.selected_entity = undefined;
        this._current_quest.val = quest;
    }
}

export const quest_editor_store = new QuestEditorStore();
