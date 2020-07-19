import { InstructionSegment, SegmentType } from "../../asm/instructions";
import { OP_SET_EPISODE } from "../../asm/opcodes";
import { prs_compress } from "../../compression/prs/compress";
import { prs_decompress } from "../../compression/prs/decompress";
import { ArrayBufferCursor } from "../../block/cursor/ArrayBufferCursor";
import { Cursor } from "../../block/cursor/Cursor";
import { ResizableBlockCursor } from "../../block/cursor/ResizableBlockCursor";
import { Endianness } from "../../block/Endianness";
import { parse_bin, write_bin } from "./bin";
import { DatEntity, parse_dat, write_dat } from "./dat";
import { Quest, QuestEntity } from "./Quest";
import { Episode } from "./Episode";
import { parse_qst, QstContainedFile, write_qst } from "./qst";
import { LogManager } from "../../../Logger";
import { parse_object_code, write_object_code } from "./object_code";
import { get_map_designations } from "../../asm/data_flow_analysis/get_map_designations";
import { basename } from "../../../util";
import { version_to_bin_format } from "./BinFormat";
import { Version } from "./Version";
import { data_to_quest_npc, get_npc_script_label, QuestNpc } from "./QuestNpc";
import {
    data_to_quest_object,
    get_object_script_label,
    get_object_script_label_2,
    QuestObject,
} from "./QuestObject";

const logger = LogManager.get("core/data_formats/parsing/quest");

export function parse_bin_dat_to_quest(
    bin_cursor: Cursor,
    dat_cursor: Cursor,
    lenient: boolean = false,
): Quest | undefined {
    // Decompress and parse files.
    const bin_decompressed = prs_decompress(bin_cursor);
    const { bin, format } = parse_bin(bin_decompressed);

    const dat_decompressed = prs_decompress(dat_cursor);
    const dat = parse_dat(dat_decompressed);
    const objects = dat.objs.map(({ area_id, data }) => data_to_quest_object(area_id, data));
    // Initialize NPCs with random episode and correct it later.
    const npcs = dat.npcs.map(({ area_id, data }) => data_to_quest_npc(Episode.I, area_id, data));

    // Extract episode and map designations from object code.
    let episode = Episode.I;
    let map_designations: Map<number, number> = new Map();

    const object_code = parse_object_code(
        bin.object_code,
        bin.label_offsets,
        extract_script_entry_points(objects, npcs),
        lenient,
        format,
    );

    if (object_code.length) {
        const instruction_segments = object_code.filter(
            s => s.type === SegmentType.Instructions,
        ) as InstructionSegment[];

        let label_0_segment: InstructionSegment | undefined;

        for (const segment of instruction_segments) {
            if (segment.labels.includes(0)) {
                label_0_segment = segment;
                break;
            }
        }

        if (label_0_segment) {
            episode = get_episode(label_0_segment);

            for (const npc of npcs) {
                npc.episode = episode;
            }

            map_designations = get_map_designations(instruction_segments, label_0_segment);
        } else {
            logger.warn(`No instruction for label 0 found.`);
        }
    } else {
        logger.warn("File contains no instruction labels.");
    }

    return {
        id: bin.quest_id,
        language: bin.language,
        name: bin.quest_name,
        short_description: bin.short_description,
        long_description: bin.long_description,
        episode,
        objects,
        npcs,
        events: dat.events,
        dat_unknowns: dat.unknowns,
        object_code,
        shop_items: bin.shop_items,
        map_designations,
    };
}

export function parse_qst_to_quest(
    cursor: Cursor,
    lenient: boolean = false,
): { quest: Quest; version: Version; online: boolean } | undefined {
    // Extract contained .dat and .bin files.
    const qst = parse_qst(cursor);

    if (!qst) {
        return;
    }

    let dat_file: QstContainedFile | undefined;
    let bin_file: QstContainedFile | undefined;

    for (const file of qst.files) {
        const file_name = file.filename.trim().toLowerCase();

        if (file_name.endsWith(".dat")) {
            dat_file = file;
        } else if (file_name.endsWith(".bin")) {
            bin_file = file;
        }
    }

    if (!dat_file) {
        logger.error("File contains no DAT file.");
        return;
    }

    if (!bin_file) {
        logger.error("File contains no BIN file.");
        return;
    }

    const quest = parse_bin_dat_to_quest(
        new ArrayBufferCursor(bin_file.data, Endianness.Little),
        new ArrayBufferCursor(dat_file.data, Endianness.Little),
        lenient,
    );

    return quest && { quest, version: qst.version, online: qst.online };
}

export function write_quest_qst(
    quest: Quest,
    file_name: string,
    version: Version,
    online: boolean,
): ArrayBuffer {
    const dat = write_dat({
        objs: entities_to_dat_data(quest.objects),
        npcs: entities_to_dat_data(quest.npcs),
        events: quest.events,
        unknowns: quest.dat_unknowns,
    });

    const { object_code, label_offsets } = write_object_code(
        quest.object_code,
        version_to_bin_format(version),
    );

    const bin = write_bin(
        {
            quest_id: quest.id,
            language: quest.language,
            quest_name: quest.name,
            short_description: quest.short_description,
            long_description: quest.long_description,
            object_code,
            label_offsets,
            shop_items: quest.shop_items,
        },
        version_to_bin_format(version),
    );

    const base_file_name = basename(file_name).slice(0, 11);

    return write_qst({
        version,
        online,
        files: [
            {
                id: quest.id,
                filename: base_file_name + ".dat",
                quest_name: quest.name,
                data: prs_compress(new ResizableBlockCursor(dat)).array_buffer(),
            },
            {
                id: quest.id,
                filename: base_file_name + ".bin",
                quest_name: quest.name,
                data: prs_compress(new ArrayBufferCursor(bin, Endianness.Little)).array_buffer(),
            },
        ],
    });
}

/**
 * Defaults to episode I.
 */
function get_episode(func_0_segment: InstructionSegment): Episode {
    const set_episode = func_0_segment.instructions.find(
        instruction => instruction.opcode.code === OP_SET_EPISODE.code,
    );

    if (set_episode) {
        const episode = set_episode.args[0].value;

        switch (episode) {
            case 0:
                return Episode.I;
            case 1:
                return Episode.II;
            case 2:
                return Episode.IV;
            default:
                logger.warn(`Unknown episode ${episode} in function 0 set_episode instruction.`);
                return Episode.I;
        }
    } else {
        logger.debug("Function 0 has no set_episode instruction.");
        return Episode.I;
    }
}

function extract_script_entry_points(
    objects: readonly QuestObject[],
    npcs: readonly QuestNpc[],
): number[] {
    const entry_points = new Set([0]);

    for (const obj of objects) {
        const entry_point = get_object_script_label(obj);

        if (entry_point != undefined) {
            entry_points.add(entry_point);
        }

        const entry_point_2 = get_object_script_label_2(obj);

        if (entry_point_2 != undefined) {
            entry_points.add(entry_point_2);
        }
    }

    for (const npc of npcs) {
        entry_points.add(get_npc_script_label(npc));
    }

    return [...entry_points];
}

function entities_to_dat_data(entities: readonly QuestEntity[]): DatEntity[] {
    return entities.map(({ area_id, data }) => ({ area_id, data }));
}
