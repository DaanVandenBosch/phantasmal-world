import { ArrayBufferCursor } from './data/ArrayBufferCursor';
import { applicationState } from './store';
import { parseQuest, writeQuestQst } from './data/parsing/quest';
import { parseNj, parseXj} from './data/parsing/ninja';
import { getAreaSections } from './data/loading/areas';
import { getNpcGeometry, getObjectGeometry } from './data/loading/entities';
import { createObjectMesh, createNpcMesh } from './rendering/entities';
import { createModelMesh } from './rendering/models';
import { VisibleQuestEntity } from './domain';

export function entitySelected(entity?: VisibleQuestEntity) {
    applicationState.selectedEntity = entity;
}

export function loadFile(file: File) {
    const reader = new FileReader();

    reader.addEventListener('loadend', async () => {
        if (!(reader.result instanceof ArrayBuffer)) {
            console.error('Couldn\'t read file.');
            return;
        }

        if (file.name.endsWith('.nj')) {
            // Reset application state, then set the current model.
            // Might want to do this in a MobX transaction.
            resetModelAndQuestState();
            applicationState.currentModel = createModelMesh(parseNj(new ArrayBufferCursor(reader.result, true)));
        } else if (file.name.endsWith('.xj')) {
            // Reset application state, then set the current model.
            // Might want to do this in a MobX transaction.
            resetModelAndQuestState();
            applicationState.currentModel = createModelMesh(parseXj(new ArrayBufferCursor(reader.result, true)));
        } else {
            const quest = parseQuest(new ArrayBufferCursor(reader.result, true));

            if (quest) {
                // Reset application state, then set current quest and area in the correct order.
                // Might want to do this in a MobX transaction.
                resetModelAndQuestState();
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

export function currentAreaIdChanged(areaId?: number) {
    applicationState.selectedEntity = undefined;

    if (areaId == null) {
        applicationState.currentArea = undefined;
    } else if (applicationState.currentQuest) {
        const areaVariant = applicationState.currentQuest.areaVariants.find(
            variant => variant.area.id === areaId);
        applicationState.currentArea = areaVariant && areaVariant.area;
    }
}

export function saveCurrentQuestToFile(fileName: string) {
    if (applicationState.currentQuest) {
        const cursor = writeQuestQst(applicationState.currentQuest, fileName);

        if (!fileName.endsWith('.qst')) {
            fileName += '.qst';
        }

        const a = document.createElement('a');
        a.href = URL.createObjectURL(new Blob([cursor.buffer]));
        a.download = fileName;
        document.body.appendChild(a);
        a.click();
        URL.revokeObjectURL(a.href);
        document.body.removeChild(a);
    }
}

function resetModelAndQuestState() {
    applicationState.currentQuest = undefined;
    applicationState.currentArea = undefined;
    applicationState.selectedEntity = undefined;
    applicationState.currentModel = undefined;
}
