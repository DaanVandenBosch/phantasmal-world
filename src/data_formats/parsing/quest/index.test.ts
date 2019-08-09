import { readFileSync } from "fs";
import { Endianness } from "../../Endianness";
import { walk_qst_files } from "../../../../test/src/utils";
import { ArrayBufferCursor } from "../../cursor/ArrayBufferCursor";
import { BufferCursor } from "../../cursor/BufferCursor";
import { parse_quest, Quest, write_quest_qst } from "./index";
import { ObjectType } from "./object_types";

test("parse Towards the Future", () => {
    const buffer = readFileSync("test/resources/quest118_e.qst");
    const cursor = new BufferCursor(buffer, Endianness.Little);
    const quest = parse_quest(cursor)!;

    expect(quest.name).toBe("Towards the Future");
    expect(quest.short_description).toBe("Challenge the\nnew simulator.");
    expect(quest.long_description).toBe(
        "Client: Principal\nQuest: Wishes to have\nhunters challenge the\nnew simulator\nReward: ??? Meseta",
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
 * Round-trip tests.
 * Parse a QST file, write the resulting Quest object to QST again, then parse that again.
 * Then check whether the two Quest objects are equal.
 */
if (process.env["RUN_ALL_TESTS"] === "true") {
    walk_qst_files(round_trip_test);
} else {
    const file_name_1 = "quest118_e.qst";
    const path_1 = `test/resources/${file_name_1}`;
    const buffer_1 = readFileSync(path_1);
    round_trip_test(path_1, file_name_1, buffer_1);

    const file_name_2 = "quest27_e.qst";
    const path_2 = `test/resources/${file_name_2}`;
    const buffer_2 = readFileSync(path_2);
    round_trip_test(path_2, file_name_2, buffer_2);
}

function round_trip_test(path: string, file_name: string, contents: Buffer): void {
    test(`parse_quest and write_quest_qst ${path}`, () => {
        const orig_quest = parse_quest(new BufferCursor(contents, Endianness.Little))!;
        const test_bin = write_quest_qst(orig_quest, file_name);
        const test_quest = parse_quest(new ArrayBufferCursor(test_bin, Endianness.Little))!;

        expect(test_quest.name).toBe(orig_quest.name);
        expect(test_quest.short_description).toBe(orig_quest.short_description);
        expect(test_quest.long_description).toBe(orig_quest.long_description);
        expect(test_quest.episode).toBe(orig_quest.episode);
        expect(test_quest.objects.length).toBe(orig_quest.objects.length);

        for (let i = 0; i < orig_quest.objects.length; i++) {
            const orig_obj = orig_quest.objects[i];
            const test_obj = test_quest.objects[i];
            expect(test_obj.area_id).toBe(orig_obj.area_id);
            expect(test_obj.section_id).toBe(orig_obj.section_id);
            expect(test_obj.position).toEqual(orig_obj.position);
            expect(test_obj.type).toBe(orig_obj.type);
        }

        expect(test_quest.npcs.length).toBe(orig_quest.npcs.length);

        for (let i = 0; i < orig_quest.npcs.length; i++) {
            const orig_npc = orig_quest.npcs[i];
            const test_npc = test_quest.npcs[i];
            expect(test_npc.area_id).toBe(orig_npc.area_id);
            expect(test_npc.section_id).toBe(orig_npc.section_id);
            expect(test_npc.position).toEqual(orig_npc.position);
            expect(test_npc.type).toBe(orig_npc.type);
        }

        expect(test_quest.area_variants.length).toBe(orig_quest.area_variants.length);

        for (let i = 0; i < orig_quest.area_variants.length; i++) {
            const orig_area_variant = orig_quest.area_variants[i];
            const test_area_variant = test_quest.area_variants[i];
            expect(test_area_variant.id).toBe(orig_area_variant.id);
            expect(test_area_variant.area.id).toBe(orig_area_variant.area.id);
        }

        expect(test_quest.object_code.length).toBe(orig_quest.object_code.length);

        for (let i = 0; i < orig_quest.object_code.length; i++) {
            expect(test_quest.object_code[i]).toEqual(orig_quest.object_code[i]);
        }
    });
}

function testable_area_variants(quest: Quest): any[][] {
    return quest.area_variants.map(av => [av.area.id, av.id]);
}
