import { ArrayBufferCursor } from './data/ArrayBufferCursor';
import { application_state } from './store';
import { parse_quest, write_quest_qst } from './data/parsing/quest';
import { parse_nj, parse_xj } from './data/parsing/ninja';
import { get_area_sections } from './data/loading/areas';
import { get_npc_geometry, get_object_geometry } from './data/loading/entities';
import { create_object_mesh, create_npc_mesh } from './rendering/entities';
import { create_model_mesh } from './rendering/models';
import { VisibleQuestEntity } from './domain';

export function entity_selected(entity?: VisibleQuestEntity) {
    application_state.selected_entity = entity;
}

export function load_file(file: File) {
    const reader = new FileReader();

    reader.addEventListener('loadend', async () => {
        if (!(reader.result instanceof ArrayBuffer)) {
            console.error('Couldn\'t read file.');
            return;
        }

        if (file.name.endsWith('.nj')) {
            // Reset application state, then set the current model.
            // Might want to do this in a MobX transaction.
            reset_model_and_quest_state();
            application_state.current_model = create_model_mesh(parse_nj(new ArrayBufferCursor(reader.result, true)));
        } else if (file.name.endsWith('.xj')) {
            // Reset application state, then set the current model.
            // Might want to do this in a MobX transaction.
            reset_model_and_quest_state();
            application_state.current_model = create_model_mesh(parse_xj(new ArrayBufferCursor(reader.result, true)));
        } else {
            const quest = parse_quest(new ArrayBufferCursor(reader.result, true));

            if (quest) {
                // Reset application state, then set current quest and area in the correct order.
                // Might want to do this in a MobX transaction.
                reset_model_and_quest_state();
                application_state.current_quest = quest;

                if (quest.area_variants.length) {
                    application_state.current_area = quest.area_variants[0].area;
                }

                // Load section data.
                for (const variant of quest.area_variants) {
                    const sections = await get_area_sections(quest.episode, variant.area.id, variant.id)
                    variant.sections = sections;

                    // Generate object geometry.
                    for (const object of quest.objects.filter(o => o.area_id === variant.area.id)) {
                        try {
                            const geometry = await get_object_geometry(object.type);
                            object.object3d = create_object_mesh(object, sections, geometry);
                        } catch (e) {
                            console.error(e);
                        }
                    }

                    // Generate NPC geometry.
                    for (const npc of quest.npcs.filter(npc => npc.area_id === variant.area.id)) {
                        try {
                            const geometry = await get_npc_geometry(npc.type);
                            npc.object3d = create_npc_mesh(npc, sections, geometry);
                        } catch (e) {
                            console.error(e);
                        }
                    }
                }
            }
        }
    });

    reader.readAsArrayBuffer(file);
}

export function current_area_id_changed(area_id?: number) {
    application_state.selected_entity = undefined;

    if (area_id == null) {
        application_state.current_area = undefined;
    } else if (application_state.current_quest) {
        const area_variant = application_state.current_quest.area_variants.find(
            variant => variant.area.id === area_id);
        application_state.current_area = area_variant && area_variant.area;
    }
}

export function save_current_quest_to_file(file_name: string) {
    if (application_state.current_quest) {
        const cursor = write_quest_qst(application_state.current_quest, file_name);

        if (!file_name.endsWith('.qst')) {
            file_name += '.qst';
        }

        const a = document.createElement('a');
        a.href = URL.createObjectURL(new Blob([cursor.buffer]));
        a.download = file_name;
        document.body.appendChild(a);
        a.click();
        URL.revokeObjectURL(a.href);
        document.body.removeChild(a);
    }
}

function reset_model_and_quest_state() {
    application_state.current_quest = undefined;
    application_state.current_area = undefined;
    application_state.selected_entity = undefined;
    application_state.current_model = undefined;
}
