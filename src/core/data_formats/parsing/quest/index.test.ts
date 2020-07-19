import { readFileSync } from "fs";
import { Endianness } from "../../block/Endianness";
import { walk_qst_files } from "../../../../../test/src/utils";
import { ArrayBufferCursor } from "../../block/cursor/ArrayBufferCursor";
import { BufferCursor } from "../../block/cursor/BufferCursor";
import { parse_qst_to_quest, write_quest_qst } from "./index";
import { ObjectType } from "./object_types";
import {
    DataSegment,
    InstructionSegment,
    SegmentType,
    StringSegment,
} from "../../asm/instructions";
import { get_object_position, get_object_section_id, get_object_type } from "./QuestObject";
import { get_npc_position, get_npc_section_id, get_npc_type } from "./QuestNpc";

test("parse Towards the Future", () => {
    const buffer = readFileSync("test/resources/quest118_e.qst");
    const cursor = new BufferCursor(buffer, Endianness.Little);
    const { quest } = parse_qst_to_quest(cursor)!;

    expect(quest.name).toBe("Towards the Future");
    expect(quest.short_description).toBe("Challenge the\nnew simulator.");
    expect(quest.long_description).toBe(
        "Client: Principal\nQuest: Wishes to have\nhunters challenge the\nnew simulator\nReward: ??? Meseta",
    );
    expect(quest.episode).toBe(1);
    expect(quest.objects.length).toBe(277);
    expect(get_object_type(quest.objects[0])).toBe(ObjectType.MenuActivation);
    expect(get_object_type(quest.objects[4])).toBe(ObjectType.PlayerSet);
    expect(quest.npcs.length).toBe(216);
    expect(quest.map_designations).toEqual(
        new Map([
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
        ]),
    );
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

// GC quest.
round_trip_test(
    "test/resources/lost_heat_sword_gc.qst",
    "lost_heat_sword_gc.qst",
    readFileSync("test/resources/lost_heat_sword_gc.qst"),
);

function round_trip_test(path: string, file_name: string, contents: Buffer): void {
    test(`parse_quest and write_quest_qst ${path}`, () => {
        const { quest: orig_quest, version, online } = parse_qst_to_quest(
            new BufferCursor(contents, Endianness.Little),
        )!;
        const test_qst = write_quest_qst(orig_quest, file_name, version, online);
        const { quest: test_quest } = parse_qst_to_quest(
            new ArrayBufferCursor(test_qst, Endianness.Little),
        )!;

        expect(test_quest.name).toBe(orig_quest.name);
        expect(test_quest.short_description).toBe(orig_quest.short_description);
        expect(test_quest.long_description).toBe(orig_quest.long_description);
        expect(test_quest.episode).toBe(orig_quest.episode);
        expect(test_quest.objects.length).toBe(orig_quest.objects.length);

        for (let i = 0; i < orig_quest.objects.length; i++) {
            const orig_obj = orig_quest.objects[i];
            const test_obj = test_quest.objects[i];
            expect(test_obj.area_id).toBe(orig_obj.area_id);
            expect(get_object_section_id(test_obj)).toBe(get_object_section_id(orig_obj));
            expect(get_object_position(test_obj)).toEqual(get_object_position(orig_obj));
            expect(get_object_type(test_obj)).toBe(get_object_type(orig_obj));
        }

        expect(test_quest.npcs.length).toBe(orig_quest.npcs.length);

        for (let i = 0; i < orig_quest.npcs.length; i++) {
            const orig_npc = orig_quest.npcs[i];
            const test_npc = test_quest.npcs[i];
            expect(test_npc.area_id).toBe(orig_npc.area_id);
            expect(get_npc_section_id(test_npc)).toBe(get_npc_section_id(orig_npc));
            expect(get_npc_position(test_npc)).toEqual(get_npc_position(orig_npc));
            expect(get_npc_type(test_npc)).toBe(get_npc_type(orig_npc));
        }

        expect(test_quest.map_designations).toEqual(orig_quest.map_designations);

        expect(test_quest.object_code.length).toBe(orig_quest.object_code.length);

        for (let i = 0; i < orig_quest.object_code.length; i++) {
            const orig_segment = orig_quest.object_code[i];
            const test_segment = test_quest.object_code[i];

            expect(test_segment.type).toBe(orig_segment.type);
            expect(test_segment.labels).toEqual(orig_segment.labels);

            switch (orig_segment.type) {
                case SegmentType.Instructions:
                    expect((test_segment as InstructionSegment).instructions.length).toBe(
                        orig_segment.instructions.length,
                    );

                    for (let j = 0; j < orig_segment.instructions.length; j++) {
                        const orig_inst = orig_segment.instructions[j];
                        const test_inst = (test_segment as InstructionSegment).instructions[j];

                        expect(test_inst.opcode.code).toBe(orig_inst.opcode.code);
                        expect(test_inst.args).toEqual(orig_inst.args);
                    }

                    break;
                case SegmentType.Data:
                    expect((test_segment as DataSegment).data).toEqual(orig_segment.data);
                    break;
                case SegmentType.String:
                    expect((test_segment as StringSegment).value).toBe(orig_segment.value);
                    break;
            }

            expect(test_quest.object_code[i]).toEqual(orig_quest.object_code[i]);
        }
    });
}
