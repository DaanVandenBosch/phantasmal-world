import { action } from 'mobx';
import { Object3D } from 'three';
import { ArrayBufferCursor } from '../../data/ArrayBufferCursor';
import { getAreaSections } from '../../data/loading/areas';
import { getNpcGeometry, getObjectGeometry } from '../../data/loading/entities';
import { parseNj, parseXj } from '../../data/parsing/ninja';
import { parseQuest } from '../../data/parsing/quest';
import { AreaVariant, Section, Vec3, VisibleQuestEntity } from '../../domain';
import { createNpcMesh, createObjectMesh } from '../../rendering/entities';
import { createModelMesh } from '../../rendering/models';
import { setModel, setQuest } from './questEditor';

export function loadFile(file: File) {
    const reader = new FileReader();
    reader.addEventListener('loadend', () => { loadend(file, reader) });
    reader.readAsArrayBuffer(file);
}

// TODO: notify user of problems.
async function loadend(file: File, reader: FileReader) {
    if (!(reader.result instanceof ArrayBuffer)) {
        console.error('Couldn\'t read file.');
        return;
    }

    if (file.name.endsWith('.nj')) {
        setModel(createModelMesh(parseNj(new ArrayBufferCursor(reader.result, true))));
    } else if (file.name.endsWith('.xj')) {
        setModel(createModelMesh(parseXj(new ArrayBufferCursor(reader.result, true))));
    } else {
        const quest = parseQuest(new ArrayBufferCursor(reader.result, true));
        setQuest(quest);

        if (quest) {
            // Load section data.
            for (const variant of quest.areaVariants) {
                const sections = await getAreaSections(quest.episode, variant.area.id, variant.id);
                setSectionsOnAreaVariant(variant, sections);

                // Generate object geometry.
                for (const object of quest.objects.filter(o => o.areaId === variant.area.id)) {
                    try {
                        const geometry = await getObjectGeometry(object.type);
                        setSectionOnVisibleQuestEntity(object, sections);
                        setObject3dOnVisibleQuestEntity(object, createObjectMesh(object, geometry));
                    } catch (e) {
                        console.error(e);
                    }
                }

                // Generate NPC geometry.
                for (const npc of quest.npcs.filter(npc => npc.areaId === variant.area.id)) {
                    try {
                        const geometry = await getNpcGeometry(npc.type);
                        setSectionOnVisibleQuestEntity(npc, sections);
                        setObject3dOnVisibleQuestEntity(npc, createNpcMesh(npc, geometry));
                    } catch (e) {
                        console.error(e);
                    }
                }
            }
        } else {
            console.error('Couldn\'t parse quest file.');
        }
    }
}

const setSectionsOnAreaVariant = action('setSectionsOnAreaVariant',
    (variant: AreaVariant, sections: Section[]) => {
        variant.sections = sections;
    }
);

const setSectionOnVisibleQuestEntity = action('setSectionOnVisibleQuestEntity',
    (entity: VisibleQuestEntity, sections: Section[]) => {
        let { x, y, z } = entity.position;

        const section = sections.find(s => s.id === entity.sectionId);
        entity.section = section;

        if (section) {
            const { x: secX, y: secY, z: secZ } = section.position;
            const rotX = section.cosYAxisRotation * x + section.sinYAxisRotation * z;
            const rotZ = -section.sinYAxisRotation * x + section.cosYAxisRotation * z;
            x = rotX + secX;
            y += secY;
            z = rotZ + secZ;
        } else {
            console.warn(`Section ${entity.sectionId} not found.`);
        }

        entity.position = new Vec3(x, y, z);
    }
);

const setObject3dOnVisibleQuestEntity = action('setObject3dOnVisibleQuestEntity',
    (entity: VisibleQuestEntity, object3d: Object3D) => {
        entity.object3d = object3d;
    }
);
