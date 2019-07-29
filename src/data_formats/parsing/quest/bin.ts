import Logger from "js-logger";
import { Endianness } from "../..";
import { ArrayBufferCursor } from "../../cursor/ArrayBufferCursor";
import { Cursor } from "../../cursor/Cursor";
import { ResizableBufferCursor } from "../../cursor/ResizableBufferCursor";
import { WritableCursor } from "../../cursor/WritableCursor";
import { ResizableBuffer } from "../../ResizableBuffer";
import { Opcode, OPCODES, Type } from "./opcodes";

export * from "./opcodes";

const logger = Logger.get("data_formats/parsing/quest/bin");

export class BinFile {
    constructor(
        readonly quest_id: number,
        readonly language: number,
        readonly quest_name: string,
        readonly short_description: string,
        readonly long_description: string,
        readonly object_code: Segment[],
        readonly shop_items: number[]
    ) {}
}

/**
 * Instruction invocation.
 */
export class Instruction {
    /**
     * Byte size of the argument list.
     */
    readonly arg_size: number = 0;
    /**
     * Byte size of the entire instruction, i.e. the sum of the opcode size and all argument sizes.
     */
    readonly size: number;
    /**
     * Maps each parameter by index to its arguments.
     */
    readonly param_to_args: Arg[][] = [];

    constructor(readonly opcode: Opcode, readonly args: Arg[]) {
        for (let i = 0; i < opcode.params.length; i++) {
            const type = opcode.params[i].type;
            const arg = args[i];
            this.param_to_args[i] = [];

            if (arg == undefined) {
                break;
            }

            switch (type) {
                case Type.U8Var:
                case Type.ILabelVar:
                    this.arg_size++;

                    for (let j = i; j < args.length; j++) {
                        this.param_to_args[i].push(args[j]);
                        this.arg_size += args[j].size;
                    }

                    break;
                default:
                    this.arg_size += arg.size;
                    this.param_to_args[i].push(arg);
                    break;
            }
        }

        this.size = opcode.size + this.arg_size;
    }
}

export enum SegmentType {
    Instructions,
    Data,
}

/**
 * Segment of object code.
 */
export type Segment = InstructionSegment | DataSegment;

export type InstructionSegment = {
    type: SegmentType.Instructions;
    label: number;
    instructions: Instruction[];
};

export type DataSegment = {
    type: SegmentType.Data;
    label: number;
    data: ArrayBuffer;
};

/**
 * Instruction argument.
 */
export type Arg = {
    value: any;
    size: number;
};

export function parse_bin(cursor: Cursor, lenient: boolean = false): BinFile {
    const object_code_offset = cursor.u32(); // Always 4652
    const label_offset_table_offset = cursor.u32(); // Relative offsets
    const size = cursor.u32();
    cursor.seek(4); // Always seems to be 0xFFFFFFFF
    const quest_id = cursor.u32();
    const language = cursor.u32();
    const quest_name = cursor.string_utf16(64, true, true);
    const short_description = cursor.string_utf16(256, true, true);
    const long_description = cursor.string_utf16(576, true, true);

    if (size !== cursor.size) {
        logger.warn(`Value ${size} in bin size field does not match actual size ${cursor.size}.`);
    }

    cursor.seek(4); // Skip padding.

    const shop_items = cursor.u32_array(932);

    const label_offset_count = Math.floor((cursor.size - label_offset_table_offset) / 4);
    cursor.seek_start(label_offset_table_offset);

    const label_offset_table = cursor.i32_array(label_offset_count);
    const offset_to_labels = new Map<number, number[]>();

    for (let label = 0; label < label_offset_table.length; label++) {
        const offset = label_offset_table[label];

        if (offset !== -1) {
            let labels = offset_to_labels.get(offset);

            if (!labels) {
                labels = [];
                offset_to_labels.set(offset, labels);
            }

            labels.push(label);
        }
    }

    const object_code = cursor
        .seek_start(object_code_offset)
        .take(label_offset_table_offset - object_code_offset);

    const segments = parse_object_code(object_code, offset_to_labels, lenient);

    // Sanity check parsed object code.
    let segments_size = 0;

    for (const segment of segments) {
        if (segment.type === SegmentType.Instructions) {
            for (const instruction of segment.instructions) {
                segments_size += instruction.size;
            }
        } else {
            segments_size += segment.data.byteLength;
        }
    }

    if (object_code.size !== segments_size) {
        const message = `Expected to parse ${object_code.size} bytes but parsed ${segments_size} instead.`;

        if (lenient) {
            logger.error(message);
        } else {
            throw new Error(message);
        }
    }

    // Verify labels.
    outer: for (let label = 0; label < label_offset_count; label++) {
        if (label_offset_table[label] !== -1) {
            for (const segment of segments) {
                if (segment.label === label) {
                    continue outer;
                }
            }

            logger.warn(
                `Label ${label} with offset ${label_offset_table[label]} does not point to anything.`
            );
        }
    }

    return new BinFile(
        quest_id,
        language,
        quest_name,
        short_description,
        long_description,
        segments,
        shop_items
    );
}

export function write_bin(bin: BinFile): ArrayBuffer {
    const object_code_offset = 4652;
    const buffer = new ResizableBuffer(object_code_offset + 100 * bin.object_code.length);
    const cursor = new ResizableBufferCursor(buffer, Endianness.Little);

    cursor.write_u32(object_code_offset);
    cursor.write_u32(0); // Placeholder for the labels offset.
    cursor.write_u32(0); // Placeholder for the file size.
    cursor.write_u32(0xffffffff);
    cursor.write_u32(bin.quest_id);
    cursor.write_u32(bin.language);
    cursor.write_string_utf16(bin.quest_name, 64);
    cursor.write_string_utf16(bin.short_description, 256);
    cursor.write_string_utf16(bin.long_description, 576);
    cursor.write_u32(0);

    if (bin.shop_items.length > 932) {
        throw new Error(`shop_items can't be larger than 932, was ${bin.shop_items.length}.`);
    }

    cursor.write_u32_array(bin.shop_items);

    for (let i = bin.shop_items.length; i < 932; i++) {
        cursor.write_u32(0);
    }

    while (cursor.position < object_code_offset) {
        cursor.write_u8(0);
    }

    const { size: object_code_size, label_offsets } = write_object_code(cursor, bin.object_code);

    for (let label = 0; label < label_offsets.length; label++) {
        const offset = label_offsets[label];

        if (offset == undefined) {
            cursor.write_i32(-1);
        } else {
            cursor.write_i32(offset);
        }
    }

    const file_size = cursor.position;

    cursor.seek_start(4);
    cursor.write_u32(object_code_offset + object_code_size);
    cursor.write_u32(file_size);

    return cursor.seek_start(0).array_buffer(file_size);
}

function parse_object_code(
    cursor: Cursor,
    offset_to_labels: Map<number, number[]>,
    lenient: boolean
): Segment[] {
    const segments: Segment[] = [];
    const data_labels = new Set<number>();

    try {
        let instructions: Instruction[] | undefined;

        while (cursor.bytes_left) {
            // See if this instruction and the ones following belong to a new label.
            const offset = cursor.position;
            const labels: number[] | undefined = offset_to_labels.get(offset);

            // Check whether we've encountered a data segment.
            // If a label that points to this segment is referred to from a data context we assume the segment is a data segment.
            if (labels && labels.some(label => data_labels.has(label))) {
                let last_label = -1;
                let data_segment_size = cursor.size - offset;

                // Get the next label's offset.
                for (let i = offset + 1; i < cursor.size; i++) {
                    if (offset_to_labels.has(i)) {
                        // We create empty segments for all but the last label.
                        // The data will be in the last label's segment.
                        for (let j = 0; j < labels.length - 1; j++) {
                            segments.push({
                                type: SegmentType.Data,
                                label: labels[j],
                                data: new ArrayBuffer(0),
                            });
                        }

                        last_label = labels[labels.length - 1];
                        data_segment_size = i - offset;
                        break;
                    }
                }

                segments.push({
                    type: SegmentType.Data,
                    label: last_label,
                    data: cursor.array_buffer(data_segment_size),
                });

                instructions = undefined;
            } else {
                // Parse as instruction.
                if (labels == undefined) {
                    if (instructions == undefined) {
                        logger.warn(`Unlabelled instructions at ${offset}.`);

                        instructions = [];

                        segments.push({
                            type: SegmentType.Instructions,
                            label: -1,
                            instructions,
                        });
                    }
                } else {
                    for (let i = 0; i < labels.length - 1; i++) {
                        segments.push({
                            type: SegmentType.Instructions,
                            label: labels[i],
                            instructions: [],
                        });
                    }

                    instructions = [];

                    segments.push({
                        type: SegmentType.Instructions,
                        label: labels[labels.length - 1],
                        instructions,
                    });
                }

                // Parse the opcode.
                const main_opcode = cursor.u8();
                let opcode_index;

                switch (main_opcode) {
                    case 0xf8:
                    case 0xf9:
                        opcode_index = (main_opcode << 8) | cursor.u8();
                        break;
                    default:
                        opcode_index = main_opcode;
                        break;
                }

                let opcode = OPCODES[opcode_index];

                // Parse the arguments.
                try {
                    const args = parse_instruction_arguments(cursor, opcode);
                    instructions.push(new Instruction(opcode, args));

                    // Check whether we can deduce a data segment label.
                    for (let i = 0; i < opcode.params.length; i++) {
                        const param_type = opcode.params[i].type;
                        const arg_value = args[i].value;

                        if (param_type === Type.DLabel) {
                            data_labels.add(arg_value);
                        }
                    }
                } catch (e) {
                    if (lenient) {
                        logger.error(
                            `Exception occurred while parsing arguments for instruction ${opcode.mnemonic}.`,
                            e
                        );
                        instructions.push(new Instruction(opcode, []));
                    } else {
                        throw e;
                    }
                }
            }
        }
    } catch (e) {
        if (lenient) {
            logger.error("Couldn't fully parse object code.", e);
        } else {
            throw e;
        }
    }

    return segments;
}

function parse_instruction_arguments(cursor: Cursor, opcode: Opcode): Arg[] {
    const args: Arg[] = [];

    for (const param of opcode.params) {
        switch (param.type) {
            case Type.U8:
                args.push({ value: cursor.u8(), size: 1 });
                break;
            case Type.U16:
                args.push({ value: cursor.u16(), size: 2 });
                break;
            case Type.U32:
                args.push({ value: cursor.u32(), size: 4 });
                break;
            case Type.I32:
                args.push({ value: cursor.i32(), size: 4 });
                break;
            case Type.F32:
                args.push({ value: cursor.f32(), size: 4 });
                break;
            case Type.Register:
                args.push({ value: cursor.u8(), size: 1 });
                break;
            case Type.ILabel:
                args.push({ value: cursor.u16(), size: 2 });
                break;
            case Type.DLabel:
                args.push({ value: cursor.u16(), size: 2 });
                break;
            case Type.U8Var:
                {
                    const arg_size = cursor.u8();
                    args.push(...cursor.u8_array(arg_size).map(value => ({ value, size: 1 })));
                }
                break;
            case Type.ILabelVar:
                {
                    const arg_size = cursor.u8();
                    args.push(...cursor.u16_array(arg_size).map(value => ({ value, size: 2 })));
                }
                break;
            case Type.String:
                {
                    const start_pos = cursor.position;
                    args.push({
                        value: cursor.string_utf16(Math.min(4096, cursor.bytes_left), true, false),
                        size: cursor.position - start_pos,
                    });
                }
                break;
            default:
                throw new Error(
                    `Parameter type ${Type[param.type]} (${param.type}) not implemented.`
                );
        }
    }

    return args;
}

function write_object_code(
    cursor: WritableCursor,
    segments: Segment[]
): { size: number; label_offsets: number[] } {
    const start_pos = cursor.position;
    // Keep track of label offsets.
    const label_offsets: number[] = [];

    // Write instructions first.
    for (const segment of segments) {
        if (segment.label !== -1) {
            label_offsets[segment.label] = cursor.position - start_pos;
        }

        if (segment.type === SegmentType.Instructions) {
            for (const instruction of segment.instructions) {
                const opcode = instruction.opcode;

                if (opcode.size === 2) {
                    cursor.write_u8(opcode.code >>> 8);
                }

                cursor.write_u8(opcode.code & 0xff);

                for (let i = 0; i < opcode.params.length; i++) {
                    const param = opcode.params[i];
                    const args = instruction.param_to_args[i];
                    const [arg] = args;

                    switch (param.type) {
                        case Type.U8:
                            cursor.write_u8(arg.value);
                            break;
                        case Type.U16:
                            cursor.write_u16(arg.value);
                            break;
                        case Type.U32:
                            cursor.write_u32(arg.value);
                            break;
                        case Type.I32:
                            cursor.write_i32(arg.value);
                            break;
                        case Type.F32:
                            cursor.write_f32(arg.value);
                            break;
                        case Type.Register:
                            cursor.write_u8(arg.value);
                            break;
                        case Type.ILabel:
                            cursor.write_u16(arg.value);
                            break;
                        case Type.DLabel:
                            cursor.write_u16(arg.value);
                            break;
                        case Type.U8Var:
                            cursor.write_u8(args.length);
                            cursor.write_u8_array(args.map(arg => arg.value));
                            break;
                        case Type.ILabelVar:
                            cursor.write_u8(args.length);
                            cursor.write_u16_array(args.map(arg => arg.value));
                            break;
                        case Type.String:
                            cursor.write_string_utf16(arg.value, arg.size);
                            break;
                        default:
                            throw new Error(
                                `Parameter type ${Type[param.type]} (${
                                    param.type
                                }) not implemented.`
                            );
                    }
                }
            }
        } else {
            cursor.write_cursor(new ArrayBufferCursor(segment.data, cursor.endianness));
        }
    }

    return { size: cursor.position - start_pos, label_offsets };
}
