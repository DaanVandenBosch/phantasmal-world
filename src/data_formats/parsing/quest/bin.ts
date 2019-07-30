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

const SEGMENT_PRIORITY: number[] = [];
SEGMENT_PRIORITY[SegmentType.Instructions] = 1;
SEGMENT_PRIORITY[SegmentType.Data] = 0;

/**
 * Segment of object code.
 */
export type Segment = InstructionSegment | DataSegment;

export type InstructionSegment = {
    type: SegmentType.Instructions;
    labels: number[];
    instructions: Instruction[];
};

export type DataSegment = {
    type: SegmentType.Data;
    labels: number[];
    data: ArrayBuffer;
};

/**
 * Instruction argument.
 */
export type Arg = {
    value: any;
    size: number;
};

export function parse_bin(
    cursor: Cursor,
    entry_labels: number[] = [0],
    lenient: boolean = false
): BinFile {
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
    const label_holder = new LabelHolder(label_offset_table);

    const object_code = cursor
        .seek_start(object_code_offset)
        .take(label_offset_table_offset - object_code_offset);

    const segments = parse_object_code(object_code, label_holder, entry_labels, lenient);

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

class LabelHolder {
    /**
     * Labels and their offset sorted by offset and then label.
     */
    labels: { label: number; offset: number }[] = [];
    /**
     * Mapping of labels to their offset and index into labels.
     */
    private label_map: Map<number, { offset: number; index: number }> = new Map();
    /**
     * Mapping of offsets to lists of labels.
     */
    private offset_map: Map<number, number[]> = new Map();

    constructor(label_offset_table: number[]) {
        // Populate the main label list.
        for (let label = 0; label < label_offset_table.length; label++) {
            const offset = label_offset_table[label];

            if (offset !== -1) {
                this.labels.push({ label, offset });
            }
        }

        // Sort by offset, then label.
        this.labels.sort((a, b) => a.offset - b.offset || a.label - b.label);

        // Populate the label and offset maps.
        for (let index = 0; index < this.labels.length; index++) {
            const { label, offset } = this.labels[index];

            this.label_map.set(label, { offset, index });

            const labels = this.offset_map.get(offset) || [];
            labels.push(label);
            this.offset_map.set(offset, labels);
        }
    }

    get_labels(offset: number): number[] | undefined {
        return this.offset_map.get(offset);
    }

    get_info(
        label: number
    ): { offset: number; next?: { label: number; offset: number } } | undefined {
        const offset_and_index = this.label_map.get(label);

        if (offset_and_index == undefined) {
            return undefined;
        }

        // Find the next label with a different offset.
        let next: { label: number; offset: number } | undefined;

        for (let i = offset_and_index.index + 1; i < this.labels.length; i++) {
            next = this.labels[i];

            // Skip the label if it points to the same offset.
            if (next.offset > offset_and_index.offset) {
                break;
            } else {
                next = undefined;
            }
        }

        return {
            offset: offset_and_index.offset,
            next,
        };
    }
}

function parse_object_code(
    cursor: Cursor,
    label_holder: LabelHolder,
    entry_labels: number[],
    lenient: boolean
): Segment[] {
    const offset_to_segment = new Map<number, Segment>();

    // Recursively parse segments from the entry points.
    for (const entry_label of entry_labels) {
        parse_segment(
            offset_to_segment,
            label_holder,
            cursor,
            entry_label,
            SegmentType.Instructions,
            lenient
        );
    }

    const segments: Segment[] = [];

    // Put segments in an array and parse left-over segments as data.
    let offset = 0;

    while (offset < cursor.size) {
        let segment: Segment | undefined = offset_to_segment.get(offset);

        // If we have a segment, add it. Otherwise create a new data segment.
        if (!segment) {
            const labels = label_holder.get_labels(offset);
            let end_offset: number;

            if (labels) {
                const info = label_holder.get_info(labels[0])!;
                end_offset = info.next ? info.next.offset : cursor.size;
            } else {
                end_offset = cursor.size;

                for (const label of label_holder.labels) {
                    if (label.offset > offset) {
                        end_offset = label.offset;
                        break;
                    }
                }
            }

            cursor.seek_start(offset);
            parse_data_segment(offset_to_segment, cursor, end_offset, labels || []);

            segment = offset_to_segment.get(offset);

            // Should never happen.
            if (end_offset <= offset) {
                logger.error(
                    `Next offset ${end_offset} was smaller than or equal to current offset ${offset}.`
                );
                break;
            }

            // Should never happen either.
            if (!segment) {
                logger.error(`Couldn't create segment for offset ${offset}.`);
                continue;
            }
        }

        segments.push(segment);

        switch (segment.type) {
            case SegmentType.Instructions:
                for (const instruction of segment.instructions) {
                    offset += instruction.size;
                }

                break;
            case SegmentType.Data:
                offset += segment.data.byteLength;
                break;
            default:
                throw new Error(`${SegmentType[segment!.type]} not implemented.`);
        }
    }

    // Add unreferenced labels to their segment.
    for (const { label, offset } of label_holder.labels) {
        const segment = offset_to_segment.get(offset);

        if (segment) {
            if (!segment.labels.includes(label)) {
                segment.labels.push(label);
                segment.labels.sort((a, b) => a - b);
            }
        } else {
            logger.warn(`Label ${label} with offset ${offset} does not point to anything.`);
        }
    }

    // Sanity check parsed object code.
    if (cursor.size !== offset) {
        const message = `Expected to parse ${cursor.size} bytes but parsed ${offset} instead.`;

        if (lenient) {
            logger.error(message);
        } else {
            throw new Error(message);
        }
    }

    return segments;
}

function parse_segment(
    offset_to_segment: Map<number, Segment>,
    label_holder: LabelHolder,
    cursor: Cursor,
    label: number,
    type: SegmentType,
    lenient: boolean
) {
    try {
        const info = label_holder.get_info(label);

        if (info == undefined) {
            logger.warn(`Label ${label} is not registered in the label table.`);
            return;
        }

        // Check whether we've already parsed this segment and reparse it if necessary.
        let segment = offset_to_segment.get(info.offset);
        let labels: number[];

        if (segment) {
            if (!segment.labels.includes(label)) {
                segment.labels.push(label);
                segment.labels.sort((a, b) => a - b);
            }

            if (SEGMENT_PRIORITY[type] > SEGMENT_PRIORITY[segment.type]) {
                labels = segment.labels;
            } else {
                return;
            }
        } else {
            labels = [label];
        }

        const end_offset = info.next ? info.next.offset : cursor.size;
        cursor.seek_start(info.offset);

        switch (type) {
            case SegmentType.Instructions:
                parse_instructions_segment(
                    offset_to_segment,
                    label_holder,
                    cursor,
                    end_offset,
                    labels,
                    info.next && info.next.label,
                    lenient
                );
                break;
            case SegmentType.Data:
                parse_data_segment(offset_to_segment, cursor, end_offset, labels);
                break;
            default:
                throw new Error(`Segment type ${SegmentType[type]} not implemented.`);
        }
    } catch (e) {
        if (lenient) {
            logger.error("Couldn't fully parse object code.", e);
        } else {
            throw e;
        }
    }
}

function parse_instructions_segment(
    offset_to_segment: Map<number, Segment>,
    label_holder: LabelHolder,
    cursor: Cursor,
    end_offset: number,
    labels: number[],
    next_label: number | undefined,
    lenient: boolean
) {
    const instructions: Instruction[] = [];

    const segment: InstructionSegment = {
        type: SegmentType.Instructions,
        labels,
        instructions,
    };
    offset_to_segment.set(cursor.position, segment);

    while (cursor.position < end_offset) {
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

    // Recurse on label references.
    const stack: Arg[] = [];

    for (const instruction of instructions) {
        const params = instruction.opcode.params;
        const args = instruction.args;

        if (instruction.opcode.push_stack) {
            for (const arg of args) {
                stack.push(arg);
            }
        } else {
            for (let i = 0; i < params.length && i < args.length; i++) {
                const param_type = params[i].type;
                const label = args[i].value;
                let segment_type: SegmentType;

                switch (param_type) {
                    case Type.ILabel:
                        segment_type = SegmentType.Instructions;
                        break;
                    case Type.DLabel:
                        segment_type = SegmentType.Data;
                        break;
                    default:
                        continue;
                }

                parse_segment(
                    offset_to_segment,
                    label_holder,
                    cursor,
                    label,
                    segment_type,
                    lenient
                );
            }
        }

        const stack_params = instruction.opcode.stack_params;
        const stack_args = stack.splice(stack.length - stack_params.length, stack_params.length);

        for (let i = 0; i < stack_args.length; i++) {
            const param_type = stack_params[i].type;
            let label = stack_args[i].value;
            let segment_type: SegmentType;

            switch (param_type) {
                case Type.ILabel:
                    segment_type = SegmentType.Instructions;
                    break;
                case Type.DLabel:
                    segment_type = SegmentType.Data;
                    break;
                default:
                    continue;
            }

            if (!Number.isInteger(label)) {
                logger.error(`Expected label reference but got ${label}.`);
                continue;
            }

            parse_segment(offset_to_segment, label_holder, cursor, label, segment_type, lenient);
        }
    }

    // Recurse on label drop-through.
    if (
        next_label != undefined &&
        instructions.length &&
        instructions[instructions.length - 1].opcode !== Opcode.ret
    ) {
        parse_segment(
            offset_to_segment,
            label_holder,
            cursor,
            next_label,
            SegmentType.Instructions,
            lenient
        );
    }
}

function parse_data_segment(
    offset_to_segment: Map<number, Segment>,
    cursor: Cursor,
    end_offset: number,
    labels: number[]
) {
    const start_offset = cursor.position;
    const segment: DataSegment = {
        type: SegmentType.Data,
        labels,
        data: cursor.array_buffer(end_offset - start_offset),
    };
    offset_to_segment.set(start_offset, segment);
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

    for (const segment of segments) {
        for (const label of segment.labels) {
            label_offsets[label] = cursor.position - start_pos;
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
