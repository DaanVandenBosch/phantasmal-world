import { ArrayBufferCursor } from './data/ArrayBufferCursor';
import { applicationState } from './store';
import { parseQuest, writeQuestQst } from './data/parsing/quest';
import { parseNj, parseXj} from './data/parsing/ninja';
import { getAreaSections } from './data/loading/areas';
import { getNpcGeometry, getObjectGeometry } from './data/loading/entities';
import { createObjectMesh, createNpcMesh } from './rendering/entities';
import { createModelMesh } from './rendering/models';
import { VisibleQuestEntity } from './domain';

export function entity_selected(entity?: VisibleQuestEntity) {
    applicationState.selectedEntity = entity;
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
            applicationState.currentModel = createModelMesh(parseNj(new ArrayBufferCursor(reader.result, true)));
        } else if (file.name.endsWith('.xj')) {
            // Reset application state, then set the current model.
            // Might want to do this in a MobX transaction.
            reset_model_and_quest_state();
            applicationState.currentModel = createModelMesh(parseXj(new ArrayBufferCursor(reader.result, true)));
        } else {
            const quest = parseQuest(new ArrayBufferCursor(reader.result, true));

            if (quest) {
                // Reset application state, then set current quest and area in the correct order.
                // Might want to do this in a MobX transaction.
                reset_model_and_quest_state();
                applicationState.currentQuest = quest;

                if (quest.areaVariants.length) {
                    applicationState.currentArea = quest.areaVariants[0].area;
                }

                // Load section data.
                for (const variant of quest.areaVariants) {
                    const sections = await getAreaSections(quest.episode, variant.area.id, variant.id)
                    variant.sections = sections;

                    // Generate object geometry.
                    for (const object of quest.objects.filter(o => o.areaId === variant.area.id)) {
                        try {
                            const geometry = await getObjectGeometry(object.type);
                            object.object3d = createObjectMesh(object, sections, geometry);
                        } catch (e) {
                            console.error(e);
                        }
                    }

                    // Generate NPC geometry.
                    for (const npc of quest.npcs.filter(npc => npc.areaId === variant.area.id)) {
                        try {
                            const geometry = await getNpcGeometry(npc.type);
                            npc.object3d = createNpcMesh(npc, sections, geometry);
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
    applicationState.selectedEntity = undefined;

    if (area_id == null) {
        applicationState.currentArea = undefined;
    } else if (applicationState.currentQuest) {
        const area_variant = applicationState.currentQuest.areaVariants.find(
            variant => variant.area.id === area_id);
        applicationState.currentArea = area_variant && area_variant.area;
    }
}

export function save_current_quest_to_file(file_name: string) {
    if (applicationState.currentQuest) {
        const cursor = writeQuestQst(applicationState.currentQuest, file_name);

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
    applicationState.currentQuest = undefined;
    applicationState.currentArea = undefined;
    applicationState.selectedEntity = undefined;
    applicationState.currentModel = undefined;
}
