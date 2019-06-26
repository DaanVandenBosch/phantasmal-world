import * as fs from 'fs';
import { BufferCursor } from '../../BufferCursor';
import { parse_quest, write_quest_qst } from '../quest';
import { ObjectType, Quest } from '../../../domain';

test('parse Towards the Future', () => {
    const buffer = fs.readFileSync('test/resources/quest118_e.qst').buffer;
    const cursor = new BufferCursor(buffer, true);
    const quest = parse_quest(cursor)!;

    expect(quest.name).toBe('Towards the Future');
    expect(quest.short_description).toBe('Challenge the\nnew simulator.');
    expect(quest.long_description).toBe('Client: Principal\nQuest: Wishes to have\nhunters challenge the\nnew simulator\nReward: ??? Meseta');
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
    const origQuest = parse_quest(cursor)!;
    const testQuest = parse_quest(write_quest_qst(origQuest, '02.qst'))!;

    expect(testQuest.name).toBe(origQuest.name);
    expect(testQuest.short_description).toBe(origQuest.short_description);
    expect(testQuest.long_description).toBe(origQuest.long_description);
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
        object.area_id,
        object.section_id,
        object.position,
        object.type
    ]);
}

function testableNpcs(quest: Quest) {
    return quest.npcs.map(npc => [
        npc.area_id,
        npc.section_id,
        npc.position,
        npc.type
    ]);
}

function testableAreaVariants(quest: Quest) {
    return quest.area_variants.map(av => [av.area.id, av.id]);
}
