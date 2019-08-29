import { action, observable } from "mobx";
import { write_quest_qst } from "../../../core/data_formats/parsing/quest";

class QuestEditorStore {
    @observable current_quest_filename?: string;

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
}
