import { Vec3 } from "../data_formats/vector";
import { Episode, NpcType, ObjectType, Quest, QuestNpc, QuestObject } from "../domain";
import { area_store } from "./AreaStore";
import { SegmentType, Instruction } from "../scripting/instructions";
import { Opcode } from "../scripting/opcodes";

export function create_new_quest(episode: Episode): Quest {
    if (episode === Episode.II) throw new Error("Episode II not yet supported.");

    return new Quest(
        0,
        0,
        "Untitled",
        "Created with phantasmal.world.",
        "Created with phantasmal.world.",
        episode,
        [area_store.get_variant(episode, 0, 0)],
        create_default_objects(),
        create_default_npcs(),
        [],
        [
            {
                labels: [0],
                type: SegmentType.Instructions,
                instructions: [
                    new Instruction(Opcode.SET_EPISODE, [{ value: 0, size: 4 }]),
                    new Instruction(Opcode.ARG_PUSHL, [{ value: 0, size: 4 }]),
                    new Instruction(Opcode.ARG_PUSHW, [{ value: 150, size: 2 }]),
                    new Instruction(Opcode.SET_FLOOR_HANDLER, []),
                    new Instruction(Opcode.BB_MAP_DESIGNATE, [
                        { value: 0, size: 1 },
                        { value: 0, size: 2 },
                        { value: 0, size: 1 },
                        { value: 0, size: 1 },
                    ]),
                    new Instruction(Opcode.RET, []),
                ],
            },
            {
                labels: [150],
                type: SegmentType.Instructions,
                instructions: [
                    new Instruction(Opcode.LETI, [{ value: 60, size: 1 }, { value: 237, size: 4 }]),
                    new Instruction(Opcode.LETI, [{ value: 61, size: 1 }, { value: 0, size: 4 }]),
                    new Instruction(Opcode.LETI, [{ value: 62, size: 1 }, { value: 333, size: 4 }]),
                    new Instruction(Opcode.LETI, [{ value: 63, size: 1 }, { value: -15, size: 4 }]),
                    new Instruction(Opcode.ARG_PUSHL, [{ value: 0, size: 4 }]),
                    new Instruction(Opcode.ARG_PUSHR, [{ value: 60, size: 1 }]),
                    new Instruction(Opcode.P_SETPOS, []),
                    new Instruction(Opcode.LETI, [{ value: 60, size: 1 }, { value: 255, size: 4 }]),
                    new Instruction(Opcode.LETI, [{ value: 61, size: 1 }, { value: 0, size: 4 }]),
                    new Instruction(Opcode.LETI, [{ value: 62, size: 1 }, { value: 338, size: 4 }]),
                    new Instruction(Opcode.LETI, [{ value: 63, size: 1 }, { value: -43, size: 4 }]),
                    new Instruction(Opcode.ARG_PUSHL, [{ value: 1, size: 4 }]),
                    new Instruction(Opcode.ARG_PUSHR, [{ value: 60, size: 1 }]),
                    new Instruction(Opcode.P_SETPOS, []),
                    new Instruction(Opcode.LETI, [{ value: 60, size: 1 }, { value: 222, size: 4 }]),
                    new Instruction(Opcode.LETI, [{ value: 61, size: 1 }, { value: 0, size: 4 }]),
                    new Instruction(Opcode.LETI, [{ value: 62, size: 1 }, { value: 322, size: 4 }]),
                    new Instruction(Opcode.LETI, [{ value: 63, size: 1 }, { value: 25, size: 4 }]),
                    new Instruction(Opcode.ARG_PUSHL, [{ value: 2, size: 4 }]),
                    new Instruction(Opcode.ARG_PUSHR, [{ value: 60, size: 1 }]),
                    new Instruction(Opcode.P_SETPOS, []),
                    new Instruction(Opcode.LETI, [{ value: 60, size: 1 }, { value: 248, size: 4 }]),
                    new Instruction(Opcode.LETI, [{ value: 61, size: 1 }, { value: 0, size: 4 }]),
                    new Instruction(Opcode.LETI, [{ value: 62, size: 1 }, { value: 323, size: 4 }]),
                    new Instruction(Opcode.LETI, [{ value: 63, size: 1 }, { value: -20, size: 4 }]),
                    new Instruction(Opcode.ARG_PUSHL, [{ value: 3, size: 4 }]),
                    new Instruction(Opcode.ARG_PUSHR, [{ value: 60, size: 1 }]),
                    new Instruction(Opcode.P_SETPOS, []),
                    new Instruction(Opcode.RET, []),
                ],
            },
        ],
        []
    );
}

function create_default_objects(): QuestObject[] {
    return [
        new QuestObject(
            ObjectType.MenuActivation,
            0,
            10,
            new Vec3(-16.313568115234375, 3, -579.5118408203125),
            new Vec3(0.0009587526218325454, 0, 0),
            new Vec3(1, 1, 1),
            [
                [2, 0, 0, 0, 0, 0, 0, 64, 0, 0],
                [0, 0],
                [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 128, 75, 251, 140],
            ]
        ),
        new QuestObject(
            ObjectType.MenuActivation,
            0,
            10,
            new Vec3(-393.07318115234375, 10, -12.964752197265625),
            new Vec3(0, 0, 0),
            new Vec3(9.183549615799121e-41, 1.0000011920928955, 1),
            [
                [2, 0, 1, 0, 0, 0, 1, 64, 0, 0],
                [0, 0],
                [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 32, 76, 251, 140],
            ]
        ),
        new QuestObject(
            ObjectType.MenuActivation,
            0,
            10,
            new Vec3(-458.60699462890625, 10, -51.270660400390625),
            new Vec3(0, 0, 0),
            new Vec3(1, 1, 1),
            [
                [2, 0, 2, 0, 0, 0, 2, 64, 0, 0],
                [0, 0],
                [2, 0, 0, 0, 0, 0, 1, 0, 10, 0, 0, 0, 192, 76, 251, 140],
            ]
        ),
        new QuestObject(
            ObjectType.MenuActivation,
            0,
            10,
            new Vec3(-430.19696044921875, 10, -24.490447998046875),
            new Vec3(0, 0, 0),
            new Vec3(1, 1, 1),
            [
                [2, 0, 3, 0, 0, 0, 3, 64, 0, 0],
                [0, 0],
                [3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 96, 77, 251, 140],
            ]
        ),
        new QuestObject(
            ObjectType.PlayerSet,
            0,
            10,
            new Vec3(0.995330810546875, 0, -37.0010986328125),
            new Vec3(0, 4.712460886831327, 0),
            new Vec3(0, 1, 1),
            [
                [2, 0, 4, 0, 0, 0, 4, 64, 0, 0],
                [0, 0],
                [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 78, 251, 140],
            ]
        ),
        new QuestObject(
            ObjectType.PlayerSet,
            0,
            10,
            new Vec3(3.0009307861328125, 0, -23.99688720703125),
            new Vec3(0, 4.859725289544806, 0),
            new Vec3(1.000000238418579, 1, 1),
            [
                [2, 0, 5, 0, 0, 0, 5, 64, 0, 0],
                [0, 0],
                [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 160, 78, 251, 140],
            ]
        ),
        new QuestObject(
            ObjectType.PlayerSet,
            0,
            10,
            new Vec3(2.0015106201171875, 0, -50.00386047363281),
            new Vec3(0, 4.565196484117848, 0),
            new Vec3(2.000002384185791, 1, 1),
            [
                [2, 0, 6, 0, 0, 0, 6, 64, 0, 0],
                [0, 0],
                [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 64, 79, 251, 140],
            ]
        ),
        new QuestObject(
            ObjectType.PlayerSet,
            0,
            10,
            new Vec3(4.9973907470703125, 0, -61.99664306640625),
            new Vec3(0, 4.368843947166543, 0),
            new Vec3(3.0000007152557373, 1, 1),
            [
                [2, 0, 7, 0, 0, 0, 7, 64, 0, 0],
                [0, 0],
                [0, 0, 1, 0, 10, 0, 0, 0, 0, 0, 0, 0, 224, 79, 251, 140],
            ]
        ),
        new QuestObject(
            ObjectType.MainRagolTeleporter,
            0,
            10,
            new Vec3(132.00314331054688, 1.000000238418579, -265.002197265625),
            new Vec3(0, 0.49088134237826325, 0),
            new Vec3(1.000000238418579, 1, 1),
            [
                [0, 0, 87, 7, 0, 0, 88, 71, 0, 0],
                [0, 0],
                [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 208, 128, 250, 140],
            ]
        ),
        new QuestObject(
            ObjectType.PrincipalWarp,
            0,
            10,
            new Vec3(-228, 0, -2020.99951171875),
            new Vec3(0, 2.9452880542695796, 0),
            new Vec3(-10.000004768371582, 0, -30.000030517578125),
            [
                [2, 0, 9, 0, 0, 0, 9, 64, 0, 0],
                [0, 0],
                [0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 176, 81, 251, 140],
            ]
        ),
        new QuestObject(
            ObjectType.MenuActivation,
            0,
            10,
            new Vec3(-41.000030517578125, 0, 42.37322998046875),
            new Vec3(0, 0, 0),
            new Vec3(1, 1, 1),
            [
                [2, 0, 10, 0, 0, 0, 10, 64, 0, 0],
                [1, 0],
                [4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 224, 82, 251, 140],
            ]
        ),
        new QuestObject(
            ObjectType.MenuActivation,
            0,
            10,
            new Vec3(-479.21673583984375, 8.781256675720215, -322.465576171875),
            new Vec3(6.28328118244177, 0.0009587526218325454, 0),
            new Vec3(1, 1, 1),
            [
                [2, 0, 11, 0, 0, 0, 11, 64, 0, 0],
                [0, 0],
                [5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 128, 83, 251, 140],
            ]
        ),
        new QuestObject(
            ObjectType.PrincipalWarp,
            0,
            10,
            new Vec3(-228, 0, -351.0015869140625),
            new Vec3(0, 0, 0),
            new Vec3(10.000006675720215, 0, -1760.0010986328125),
            [
                [2, 0, 12, 0, 0, 0, 12, 64, 0, 0],
                [0, 0],
                [0, 128, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 32, 84, 251, 140],
            ]
        ),
        new QuestObject(
            ObjectType.TelepipeLocation,
            0,
            10,
            new Vec3(-561.88232421875, 0, -406.8829345703125),
            new Vec3(0, 0, 0),
            new Vec3(1, 1, 1),
            [
                [2, 0, 13, 0, 0, 0, 13, 64, 0, 0],
                [0, 0],
                [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 80, 85, 251, 140],
            ]
        ),
        new QuestObject(
            ObjectType.TelepipeLocation,
            0,
            10,
            new Vec3(-547.8557739257812, 0, -444.8822326660156),
            new Vec3(0, 0, 0),
            new Vec3(1, 1, 1),
            [
                [2, 0, 14, 0, 0, 0, 14, 64, 0, 0],
                [0, 0],
                [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 86, 251, 140],
            ]
        ),
        new QuestObject(
            ObjectType.TelepipeLocation,
            0,
            10,
            new Vec3(-486.441650390625, 0, -497.4501647949219),
            new Vec3(0, 0, 0),
            new Vec3(9.183549615799121e-41, 1.0000011920928955, 1),
            [
                [2, 0, 15, 0, 0, 0, 15, 64, 0, 0],
                [0, 0],
                [3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 208, 86, 251, 140],
            ]
        ),
        new QuestObject(
            ObjectType.TelepipeLocation,
            0,
            10,
            new Vec3(-522.4052734375, 0, -474.1882629394531),
            new Vec3(0, 0, 0),
            new Vec3(1, 1, 1),
            [
                [2, 0, 16, 0, 0, 0, 16, 64, 0, 0],
                [0, 0],
                [2, 0, 0, 0, 0, 0, 1, 0, 10, 0, 0, 0, 144, 87, 251, 140],
            ]
        ),
        new QuestObject(
            ObjectType.MedicalCenterDoor,
            0,
            10,
            new Vec3(-34.49853515625, 0, -384.4951171875),
            new Vec3(0, 5.497871034636549, 0),
            new Vec3(3.0000007152557373, 1, 1),
            [
                [2, 0, 17, 0, 0, 0, 17, 64, 0, 0],
                [0, 0],
                [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 80, 88, 251, 140],
            ]
        ),
        new QuestObject(
            ObjectType.ShopDoor,
            0,
            10,
            new Vec3(-393.0031433105469, 0, -143.49981689453125),
            new Vec3(0, 3.141640591220885, 0),
            new Vec3(3.0000007152557373, 1, 1),
            [
                [2, 0, 18, 0, 0, 0, 18, 64, 0, 0],
                [0, 0],
                [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 80, 89, 251, 140],
            ]
        ),
        new QuestObject(
            ObjectType.MenuActivation,
            0,
            10,
            new Vec3(-355.17462158203125, 0, -43.15193176269531),
            new Vec3(0, 0, 0),
            new Vec3(1.000000238418579, 1, 1),
            [
                [2, 0, 19, 0, 0, 0, 19, 64, 0, 0],
                [0, 0],
                [6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 80, 90, 251, 140],
            ]
        ),
        new QuestObject(
            ObjectType.HuntersGuildDoor,
            0,
            10,
            new Vec3(-43.00239562988281, 0, -118.00120544433594),
            new Vec3(0, 3.141640591220885, 0),
            new Vec3(3.0000007152557373, 1, 1),
            [
                [2, 0, 20, 0, 0, 0, 20, 64, 0, 0],
                [0, 0],
                [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 240, 90, 251, 140],
            ]
        ),
        new QuestObject(
            ObjectType.TeleporterDoor,
            0,
            10,
            new Vec3(26.000823974609375, 0, -265.99810791015625),
            new Vec3(0, 3.141640591220885, 0),
            new Vec3(3.0000007152557373, 1, 1),
            [
                [2, 0, 21, 0, 0, 0, 21, 64, 0, 0],
                [0, 0],
                [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 240, 91, 251, 140],
            ]
        ),
        new QuestObject(
            ObjectType.PlayerSet,
            0,
            10,
            new Vec3(57.81005859375, 0, -268.5472412109375),
            new Vec3(0, 4.712460886831327, 0),
            new Vec3(0, 1, 1),
            [
                [2, 0, 22, 0, 0, 0, 22, 64, 0, 0],
                [0, 0],
                [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 240, 92, 251, 140],
            ]
        ),
        new QuestObject(
            ObjectType.PlayerSet,
            0,
            10,
            new Vec3(66.769287109375, 0, -252.3748779296875),
            new Vec3(0, 4.712460886831327, 0),
            new Vec3(1.000000238418579, 1, 1),
            [
                [2, 0, 23, 0, 0, 0, 23, 64, 0, 0],
                [0, 0],
                [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 144, 93, 251, 140],
            ]
        ),
        new QuestObject(
            ObjectType.PlayerSet,
            0,
            10,
            new Vec3(67.36819458007812, 0, -284.9297180175781),
            new Vec3(0, 4.712460886831327, 0),
            new Vec3(2.000000476837158, 1, 1),
            [
                [2, 0, 24, 0, 0, 0, 24, 64, 0, 0],
                [0, 0],
                [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 48, 94, 251, 140],
            ]
        ),
        new QuestObject(
            ObjectType.PlayerSet,
            0,
            10,
            new Vec3(77.10488891601562, 0, -269.2830505371094),
            new Vec3(0, 4.712460886831327, 0),
            new Vec3(3.0000007152557373, 1, 1),
            [
                [2, 0, 25, 0, 0, 0, 25, 64, 0, 0],
                [0, 0],
                [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 208, 94, 251, 140],
            ]
        ),
    ];
}

function create_default_npcs(): QuestNpc[] {
    return [
        new QuestNpc(
            NpcType.GuildLady,
            29,
            0,
            0,
            10,
            new Vec3(-49.0010986328125, 0, 50.996429443359375),
            new Vec3(0, 2.3562304434156633, 0),
            new Vec3(0, 0, 0),
            [
                [0, 0, 7, 86, 0, 0, 0, 0, 23, 87],
                [0, 0, 0, 0, 0, 0],
                [18, 192, 124, 68, 0, 128, 84, 68],
                [128, 238, 223, 176],
            ]
        ),
        new QuestNpc(
            NpcType.MaleFat,
            11,
            0,
            0,
            10,
            new Vec3(-2.9971923828125, 0, 63.999267578125),
            new Vec3(0, 2.9452880542695796, 0),
            new Vec3(0, 0, 0),
            [
                [0, 0, 7, 87, 0, 0, 0, 0, 23, 88],
                [0, 0, 0, 0, 0, 0],
                [6, 0, 202, 66, 4, 0, 155, 67],
                [128, 238, 227, 176],
            ]
        ),
        new QuestNpc(
            NpcType.FemaleFat,
            4,
            1,
            0,
            20,
            new Vec3(167.99769592285156, 0, 83.99686431884766),
            new Vec3(0, 3.927050739026106, 0),
            new Vec3(24.000009536743164, 0, 0),
            [
                [0, 0, 7, 88, 0, 0, 0, 0, 23, 89],
                [0, 0, 0, 0, 0, 0],
                [18, 0, 126, 68, 6, 0, 200, 66],
                [128, 238, 232, 48],
            ]
        ),
        new QuestNpc(
            NpcType.MaleDwarf,
            10,
            1,
            0,
            20,
            new Vec3(156.0028839111328, 0, -49.99967575073242),
            new Vec3(0, 5.497871034636549, 0),
            new Vec3(30.000009536743164, 0, 0),
            [
                [0, 0, 7, 89, 0, 0, 0, 0, 23, 90],
                [0, 0, 0, 0, 0, 0],
                [18, 192, 125, 68, 6, 0, 180, 66],
                [128, 238, 236, 176],
            ]
        ),
        new QuestNpc(
            NpcType.RedSoldier,
            26,
            0,
            0,
            20,
            new Vec3(237.9988250732422, 0, -14.0001220703125),
            new Vec3(0, 5.497871034636549, 0),
            new Vec3(0, 0, 0),
            [
                [0, 0, 7, 90, 0, 0, 0, 0, 23, 91],
                [0, 0, 0, 0, 0, 0],
                [18, 0, 127, 68, 6, 0, 2, 67],
                [128, 238, 241, 48],
            ]
        ),
        new QuestNpc(
            NpcType.BlueSoldier,
            25,
            0,
            0,
            20,
            new Vec3(238.00379943847656, 0, 63.00413513183594),
            new Vec3(0, 3.927050739026106, 0),
            new Vec3(0, 0, 0),
            [
                [0, 0, 7, 91, 0, 0, 0, 0, 23, 92],
                [0, 0, 0, 0, 0, 0],
                [18, 192, 126, 68, 11, 0, 240, 66],
                [128, 238, 245, 176],
            ]
        ),
        new QuestNpc(
            NpcType.FemaleMacho,
            5,
            1,
            0,
            20,
            new Vec3(-2.001882553100586, 0, 35.0036506652832),
            new Vec3(0, 3.141640591220885, 0),
            new Vec3(26.000009536743164, 0, 0),
            [
                [0, 0, 7, 92, 0, 0, 0, 0, 23, 93],
                [0, 0, 0, 0, 0, 0],
                [18, 128, 125, 68, 9, 0, 160, 66],
                [128, 238, 250, 48],
            ]
        ),
        new QuestNpc(
            NpcType.Scientist,
            30,
            1,
            0,
            20,
            new Vec3(-147.0000457763672, 0, -7.996537208557129),
            new Vec3(0, 2.577127047485882, 0),
            new Vec3(30.000009536743164, 0, 0),
            [
                [0, 0, 7, 93, 0, 0, 0, 0, 23, 94],
                [0, 0, 0, 0, 0, 0],
                [18, 64, 125, 68, 8, 0, 140, 66],
                [128, 238, 254, 176],
            ]
        ),
        new QuestNpc(
            NpcType.MaleOld,
            13,
            1,
            0,
            20,
            new Vec3(-219.99710083007812, 0, -100.0008316040039),
            new Vec3(0, 0, 0),
            new Vec3(30.000011444091797, 0, 0),
            [
                [0, 0, 7, 94, 0, 0, 0, 0, 23, 95],
                [0, 0, 0, 0, 0, 0],
                [18, 0, 125, 68, 15, 0, 112, 66],
                [128, 239, 3, 48],
            ]
        ),
        new QuestNpc(
            NpcType.GuildLady,
            29,
            0,
            0,
            20,
            new Vec3(-262.5099792480469, 0, -24.53999900817871),
            new Vec3(0, 1.963525369513053, 0),
            new Vec3(0, 0, 0),
            [
                [0, 0, 7, 95, 0, 0, 0, 0, 23, 106],
                [0, 0, 0, 0, 0, 0],
                [18, 128, 124, 68, 0, 0, 82, 68],
                [128, 239, 100, 192],
            ]
        ),
        new QuestNpc(
            NpcType.Tekker,
            28,
            0,
            0,
            30,
            new Vec3(-43.70983123779297, 2.5999999046325684, -52.78248596191406),
            new Vec3(0, 0.7854101478052212, 0),
            new Vec3(0, 0, 0),
            [
                [0, 0, 7, 97, 0, 0, 0, 0, 23, 98],
                [0, 0, 0, 0, 0, 0],
                [0, 64, 124, 68, 0, 128, 79, 68],
                [128, 239, 16, 176],
            ]
        ),
        new QuestNpc(
            NpcType.MaleMacho,
            12,
            0,
            0,
            30,
            new Vec3(0.33990478515625, 2.5999999046325684, -84.71995544433594),
            new Vec3(0, 0, 0),
            new Vec3(0, 0, 0),
            [
                [0, 0, 7, 98, 0, 0, 0, 0, 23, 99],
                [0, 0, 0, 0, 0, 0],
                [0, 128, 123, 68, 0, 0, 72, 68],
                [128, 239, 21, 48],
            ]
        ),
        new QuestNpc(
            NpcType.FemaleMacho,
            5,
            0,
            0,
            30,
            new Vec3(43.87113952636719, 2.5999996662139893, -74.80299377441406),
            new Vec3(0, -0.5645135437350027, 0),
            new Vec3(0, 0, 0),
            [
                [0, 0, 7, 99, 0, 0, 0, 0, 23, 100],
                [0, 0, 0, 0, 0, 0],
                [0, 0, 124, 68, 0, 0, 77, 68],
                [128, 239, 25, 176],
            ]
        ),
        new QuestNpc(
            NpcType.MaleFat,
            11,
            0,
            0,
            30,
            new Vec3(75.88380432128906, 2.5999996662139893, -42.69328308105469),
            new Vec3(0, -1.0308508189943528, 0),
            new Vec3(0, 0, 0),
            [
                [0, 0, 7, 100, 0, 0, 0, 0, 23, 101],
                [0, 0, 0, 0, 0, 0],
                [18, 192, 123, 68, 0, 128, 74, 68],
                [128, 239, 30, 48],
            ]
        ),
        new QuestNpc(
            NpcType.FemaleTall,
            7,
            1,
            0,
            30,
            new Vec3(16.003997802734375, 0, 5.995697021484375),
            new Vec3(0, -1.1781152217078317, 0),
            new Vec3(22.000009536743164, 0, 0),
            [
                [0, 0, 7, 101, 0, 0, 0, 0, 23, 102],
                [0, 0, 0, 0, 0, 0],
                [18, 64, 127, 68, 4, 0, 12, 67],
                [128, 239, 34, 176],
            ]
        ),
        new QuestNpc(
            NpcType.Nurse,
            31,
            0,
            0,
            40,
            new Vec3(0.3097381591796875, 3, -105.3865966796875),
            new Vec3(0, 0, 0),
            new Vec3(0, 0, 0),
            [
                [0, 0, 7, 102, 0, 0, 0, 0, 23, 103],
                [0, 0, 0, 0, 0, 0],
                [0, 64, 126, 68, 0, 0, 87, 68],
                [128, 239, 39, 48],
            ]
        ),
        new QuestNpc(
            NpcType.Nurse,
            31,
            1,
            0,
            40,
            new Vec3(53.499176025390625, 0, -26.496688842773438),
            new Vec3(0, 5.497871034636549, 0),
            new Vec3(18.000009536743164, 0, 0),
            [
                [0, 0, 7, 103, 0, 0, 0, 0, 23, 104],
                [0, 0, 0, 0, 0, 0],
                [18, 128, 126, 68, 7, 0, 220, 66],
                [128, 239, 43, 176],
            ]
        ),
    ];
}
