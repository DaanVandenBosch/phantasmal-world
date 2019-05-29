import { writeQuestQst } from '../data/parsing/quest';
import { VisibleQuestEntity, Quest } from '../domain';
import { appStateStore } from '../stores/AppStateStore';
import { action } from 'mobx';
import { Object3D } from 'three';

/**
 * Reset application state, then set the current model.
 */
export const setModel = action('setModel', (model?: Object3D) => {
    resetModelAndQuestState();
    appStateStore.currentModel = model;
});

/**
 * Reset application state, then set current quest and area.
 */
export const setQuest = action('setQuest', (quest?: Quest) => {
    resetModelAndQuestState();
    appStateStore.currentQuest = quest;

    if (quest && quest.areaVariants.length) {
        appStateStore.currentArea = quest.areaVariants[0].area;
    }
});

function resetModelAndQuestState() {
    appStateStore.currentQuest = undefined;
    appStateStore.currentArea = undefined;
    appStateStore.selectedEntity = undefined;
    appStateStore.currentModel = undefined;
}

export const setSelectedEntity = action('setSelectedEntity', (entity?: VisibleQuestEntity) => {
    appStateStore.selectedEntity = entity;
});

export const setCurrentAreaId = action('setCurrentAreaId', (areaId?: number) => {
    appStateStore.selectedEntity = undefined;

    if (areaId == null) {
        appStateStore.currentArea = undefined;
    } else if (appStateStore.currentQuest) {
        const areaVariant = appStateStore.currentQuest.areaVariants.find(
            variant => variant.area.id === areaId);
        appStateStore.currentArea = areaVariant && areaVariant.area;
    }
});

export const saveCurrentQuestToFile = (fileName: string) => {
    if (appStateStore.currentQuest) {
        const cursor = writeQuestQst(appStateStore.currentQuest, fileName);

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
