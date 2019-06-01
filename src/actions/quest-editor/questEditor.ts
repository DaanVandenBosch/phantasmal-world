import { writeQuestQst } from '../../data/parsing/quest';
import { QuestEntity, Quest } from '../../domain';
import { questEditorStore } from '../../stores/QuestEditorStore';
import { action } from 'mobx';
import { Object3D } from 'three';

/**
 * Reset application state, then set the current model.
 */
export const setModel = action('setModel', (model?: Object3D) => {
    resetModelAndQuestState();
    questEditorStore.currentModel = model;
});

/**
 * Reset application state, then set current quest and area.
 */
export const setQuest = action('setQuest', (quest?: Quest) => {
    resetModelAndQuestState();
    questEditorStore.currentQuest = quest;

    if (quest && quest.areaVariants.length) {
        questEditorStore.currentArea = quest.areaVariants[0].area;
    }
});

function resetModelAndQuestState() {
    questEditorStore.currentQuest = undefined;
    questEditorStore.currentArea = undefined;
    questEditorStore.selectedEntity = undefined;
    questEditorStore.currentModel = undefined;
}

export const setSelectedEntity = action('setSelectedEntity', (entity?: QuestEntity) => {
    questEditorStore.selectedEntity = entity;
});

export const setCurrentAreaId = action('setCurrentAreaId', (areaId?: number) => {
    questEditorStore.selectedEntity = undefined;

    if (areaId == null) {
        questEditorStore.currentArea = undefined;
    } else if (questEditorStore.currentQuest) {
        const areaVariant = questEditorStore.currentQuest.areaVariants.find(
            variant => variant.area.id === areaId);
        questEditorStore.currentArea = areaVariant && areaVariant.area;
    }
});

export const saveCurrentQuestToFile = (fileName: string) => {
    if (questEditorStore.currentQuest) {
        const cursor = writeQuestQst(questEditorStore.currentQuest, fileName);

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
};
