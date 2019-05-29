import { observable } from 'mobx';
import { Object3D } from 'three';
import { Area, Quest, VisibleQuestEntity } from '../domain';

class AppStateStore {
    @observable currentModel?: Object3D;
    @observable currentQuest?: Quest;
    @observable currentArea?: Area;
    @observable selectedEntity?: VisibleQuestEntity;
}

export const appStateStore = new AppStateStore();
