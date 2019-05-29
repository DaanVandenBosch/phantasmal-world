import { observable } from 'mobx';
import { Object3D } from 'three';
import { Area, Quest, VisibleQuestEntity } from '../domain';

class QuestEditorStore {
    @observable currentModel?: Object3D;
    @observable currentQuest?: Quest;
    @observable currentArea?: Area;
    @observable selectedEntity?: VisibleQuestEntity;
}

export const questEditorStore = new QuestEditorStore();
