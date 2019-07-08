import * as fs from "fs";
import { ObjectType, Quest } from "../../../domain";
import { parse_quest, write_quest_qst } from "../quest";
import { Endianness } from "../..";
import { BufferCursor } from "../../cursor/BufferCursor";
import { ArrayBufferCursor } from "../../cursor/ArrayBufferCursor";

test("parse Towards the Future", () => {
    const buffer = fs.readFileSync("test/resources/quest118_e.qst");
    const cursor = new BufferCursor(buffer, Endianness.Little);
    const quest = parse_quest(cursor)!;

    expect(quest.name).toBe("Towards the Future");
    expect(quest.short_description).toBe("Challenge the\nnew simulator.");
    expect(quest.long_description).toBe(
        "Client: Principal\nQuest: Wishes to have\nhunters challenge the\nnew simulator\nReward: ??? Meseta"
    );
    expect(quest.episode).toBe(1);
    expect(quest.objects.length).toBe(277);
    expect(quest.objects[0].type).toBe(ObjectType.MenuActivation);
    expect(quest.objects[4].type).toBe(ObjectType.PlayerSet);
    expect(quest.npcs.length).toBe(216);
    expect(testable_area_variants(quest)).toEqual([
        [0, 0],
        [2, 0],
        [11, 0],
        [5, 4],
        [12, 0],
        [7, 4],
        [13, 0],
        [8, 4],
        [10, 4],
        [14, 0],
    ]);
});

/**
 * Roundtrip test.
 * Parse a QST file, write the resulting Quest object to QST again, then parse that again.
 * Then check whether the two Quest objects are equal.
 */
test("parse_quest and write_quest_qst", () => {
    const buffer = fs.readFileSync("test/resources/tethealla_v0.143_quests/solo/ep1/02.qst");
    const cursor = new BufferCursor(buffer, Endianness.Little);
    const orig_quest = parse_quest(cursor)!;
    const test_quest = parse_quest(
        new ArrayBufferCursor(write_quest_qst(orig_quest, "02.qst"), Endianness.Little)
    )!;

    expect(test_quest.name).toBe(orig_quest.name);
    expect(test_quest.short_description).toBe(orig_quest.short_description);
    expect(test_quest.long_description).toBe(orig_quest.long_description);
    expect(test_quest.episode).toBe(orig_quest.episode);
    expect(testable_objects(test_quest)).toEqual(testable_objects(orig_quest));
    expect(testable_npcs(test_quest)).toEqual(testable_npcs(orig_quest));
    expect(testable_area_variants(test_quest)).toEqual(testable_area_variants(orig_quest));
});

function testable_objects(quest: Quest): any[][] {
    return quest.objects.map(object => [
        object.area_id,
        object.section_id,
        object.position,
        object.type,
    ]);
}

function testable_npcs(quest: Quest): any[][] {
    return quest.npcs.map(npc => [npc.area_id, npc.section_id, npc.position, npc.type]);
}

function testable_area_variants(quest: Quest): any[][] {
    return quest.area_variants.map(av => [av.area.id, av.id]);
}
