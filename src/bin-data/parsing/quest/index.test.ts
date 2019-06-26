import * as fs from 'fs';
import { BufferCursor } from '../../BufferCursor';
import { parseQuest, writeQuestQst } from '.';
import { ObjectType, Quest } from '../../../domain';

test('parse Towards the Future', () => {
    const buffer = fs.readFileSync('test/resources/quest118_e.qst').buffer;
    const cursor = new BufferCursor(buffer, true);
    const quest = parseQuest(cursor)!;

    expect(quest.name).toBe('Towards the Future');
    expect(quest.shortDescription).toBe('Challenge the\nnew simulator.');
    expect(quest.longDescription).toBe('Client: Principal\nQuest: Wishes to have\nhunters challenge the\nnew simulator\nReward: ??? Meseta');
    expect(quest.episode).toBe(1);
    expect(quest.objects.length).toBe(277);
    expect(quest.objects[0].type).toBe(ObjectType.MenuActivation);
    expect(quest.objects[4].type).toBe(ObjectType.PlayerSet);
    expect(quest.npcs.length).toBe(216);
    expect(testableAreaVariants(quest)).toEqual([
        [0, 0], [2, 0], [11, 0], [5, 4], [12, 0], [7, 4], [13, 0], [8, 4], [10, 4], [14, 0]
    ]);
});

/**
 * Parse a QST file, write the resulting Quest object to QST again, then parse that again.
 * Then check whether the two Quest objects are equal.
 */
test('parseQuest and writeQuestQst', () => {
    const buffer = fs.readFileSync('test/resources/tethealla_v0.143_quests/solo/ep1/02.qst').buffer;
    const cursor = new BufferCursor(buffer, true);
    const origQuest = parseQuest(cursor)!;
    const testQuest = parseQuest(writeQuestQst(origQuest, '02.qst'))!;

    expect(testQuest.name).toBe(origQuest.name);
    expect(testQuest.shortDescription).toBe(origQuest.shortDescription);
    expect(testQuest.longDescription).toBe(origQuest.longDescription);
    expect(testQuest.episode).toBe(origQuest.episode);
    expect(testableObjects(testQuest))
        .toEqual(testableObjects(origQuest));
    expect(testableNpcs(testQuest))
        .toEqual(testableNpcs(origQuest));
    expect(testableAreaVariants(testQuest))
        .toEqual(testableAreaVariants(origQuest));
});

function testableObjects(quest: Quest) {
    return quest.objects.map(object => [
        object.areaId,
        object.sectionId,
        object.position,
        object.type
    ]);
}

function testableNpcs(quest: Quest) {
    return quest.npcs.map(npc => [
        npc.areaId,
        npc.sectionId,
        npc.position,
        npc.type
    ]);
}

function testableAreaVariants(quest: Quest) {
    return quest.areaVariants.map(av => [av.area.id, av.id]);
}
