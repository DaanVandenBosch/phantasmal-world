import { observable, action } from 'mobx';
import { Object3D } from 'three';
import { BufferCursor } from '../bin_data/BufferCursor';
import { getAreaSections } from '../bin_data/loading/areas';
import { getNpcGeometry, getObjectGeometry } from '../bin_data/loading/entities';
import { parseNj, parseXj } from '../bin_data/parsing/ninja';
import { parse_quest, write_quest_qst } from '../bin_data/parsing/quest';
import { Area, Quest, QuestEntity, Section, Vec3 } from '../domain';
import { createNpcMesh, createObjectMesh } from '../rendering/entities';
import { createModelMesh } from '../rendering/models';
import Logger from 'js-logger';

const logger = Logger.get('stores/QuestEditorStore');

class QuestEditorStore {
    @observable currentModel?: Object3D;
    @observable currentQuest?: Quest;
    @observable currentArea?: Area;
    @observable selectedEntity?: QuestEntity;

    setModel = action('setModel', (model?: Object3D) => {
        this.resetModelAndQuestState();
        this.currentModel = model;
    })

    setQuest = action('setQuest', (quest?: Quest) => {
        this.resetModelAndQuestState();
        this.currentQuest = quest;

        if (quest && quest.area_variants.length) {
            this.currentArea = quest.area_variants[0].area;
        }
    })

    private resetModelAndQuestState() {
        this.currentQuest = undefined;
        this.currentArea = undefined;
        this.selectedEntity = undefined;
        this.currentModel = undefined;
    }

    setSelectedEntity = (entity?: QuestEntity) => {
        this.selectedEntity = entity;
    }

    setCurrentAreaId = action('setCurrentAreaId', (areaId?: number) => {
        this.selectedEntity = undefined;

        if (areaId == null) {
            this.currentArea = undefined;
        } else if (this.currentQuest) {
            const areaVariant = this.currentQuest.area_variants.find(
                variant => variant.area.id === areaId
            );
            this.currentArea = areaVariant && areaVariant.area;
        }
    })

    loadFile = (file: File) => {
        const reader = new FileReader();
        reader.addEventListener('loadend', () => { this.loadend(file, reader) });
        reader.readAsArrayBuffer(file);
    }

    // TODO: notify user of problems.
    private loadend = async (file: File, reader: FileReader) => {
        if (!(reader.result instanceof ArrayBuffer)) {
            logger.error('Couldn\'t read file.');
            return;
        }

        if (file.name.endsWith('.nj')) {
            this.setModel(createModelMesh(parseNj(new BufferCursor(reader.result, true))));
        } else if (file.name.endsWith('.xj')) {
            this.setModel(createModelMesh(parseXj(new BufferCursor(reader.result, true))));
        } else {
            const quest = parse_quest(new BufferCursor(reader.result, true));
            this.setQuest(quest);

            if (quest) {
                // Load section data.
                for (const variant of quest.area_variants) {
                    const sections = await getAreaSections(quest.episode, variant.area.id, variant.id);
                    variant.sections = sections;

                    // Generate object geometry.
                    for (const object of quest.objects.filter(o => o.area_id === variant.area.id)) {
                        try {
                            const geometry = await getObjectGeometry(object.type);
                            this.setSectionOnVisibleQuestEntity(object, sections);
                            object.object3d = createObjectMesh(object, geometry);
                        } catch (e) {
                            logger.error(e);
                        }
                    }

                    // Generate NPC geometry.
                    for (const npc of quest.npcs.filter(npc => npc.area_id === variant.area.id)) {
                        try {
                            const geometry = await getNpcGeometry(npc.type);
                            this.setSectionOnVisibleQuestEntity(npc, sections);
                            npc.object3d = createNpcMesh(npc, geometry);
                        } catch (e) {
                            logger.error(e);
                        }
                    }
                }
            } else {
                logger.error('Couldn\'t parse quest file.');
            }
        }
    }

    private setSectionOnVisibleQuestEntity = async (entity: QuestEntity, sections: Section[]) => {
        let { x, y, z } = entity.position;

        const section = sections.find(s => s.id === entity.section_id);
        entity.section = section;

        if (section) {
            const { x: secX, y: secY, z: secZ } = section.position;
            const rotX = section.cos_y_axis_rotation * x + section.sin_y_axis_rotation * z;
            const rotZ = -section.sin_y_axis_rotation * x + section.cos_y_axis_rotation * z;
            x = rotX + secX;
            y += secY;
            z = rotZ + secZ;
        } else {
            logger.warn(`Section ${entity.section_id} not found.`);
        }

        entity.position = new Vec3(x, y, z);
    }

    saveCurrentQuestToFile = (fileName: string) => {
        if (this.currentQuest) {
            const cursor = write_quest_qst(this.currentQuest, fileName);

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
}

export const questEditorStore = new QuestEditorStore();
