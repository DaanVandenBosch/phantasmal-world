import Logger from "js-logger";
import { Endianness } from "../..";
import { ControlFlowGraph } from "../../../scripting/data_flow_analysis/ControlFlowGraph";
import { register_value } from "../../../scripting/data_flow_analysis/register_value";
import {
    Arg,
    DataSegment,
    Instruction,
    InstructionSegment,
    Segment,
    SegmentType,
    StringSegment,
} from "../../../scripting/instructions";
import {
    Opcode,
    OPCODES,
    RegTupRefType,
    StackInteraction,
    TYPE_BYTE,
    TYPE_DWORD,
    TYPE_D_LABEL,
    TYPE_FLOAT,
    TYPE_I_LABEL,
    TYPE_I_LABEL_VAR,
    TYPE_LABEL,
    TYPE_REF,
    TYPE_REG_REF,
    TYPE_REG_REF_VAR,
    TYPE_STRING,
    TYPE_S_LABEL,
    TYPE_WORD,
} from "../../../scripting/opcodes";
import { ArrayBufferCursor } from "../../cursor/ArrayBufferCursor";
import { Cursor } from "../../cursor/Cursor";
import { ResizableBufferCursor } from "../../cursor/ResizableBufferCursor";
import { WritableCursor } from "../../cursor/WritableCursor";
import { ResizableBuffer } from "../../ResizableBuffer";
import { stack_value } from "../../../scripting/data_flow_analysis/stack_value";

// TODO: correctly deal with stack floats (they're pushed with arg_pushl)

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

const SEGMENT_PRIORITY: number[] = [];
SEGMENT_PRIORITY[SegmentType.Instructions] = 2;
SEGMENT_PRIORITY[SegmentType.String] = 1;
SEGMENT_PRIORITY[SegmentType.Data] = 0;

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

    find_and_parse_segments(
        cursor,
        label_holder,
        entry_labels.reduce((m, l) => m.set(l, SegmentType.Instructions), new Map()),
        offset_to_segment,
        lenient
    );

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
            case SegmentType.String:
                // String segments should be multiples of 4 bytes.
                offset += 4 * Math.ceil((segment.value.length + 1) / 2);
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

function find_and_parse_segments(
    cursor: Cursor,
    label_holder: LabelHolder,
    labels: Map<number, SegmentType>,
    offset_to_segment: Map<number, Segment>,
    lenient: boolean
) {
    let start_segment_count: number;

    // Iteratively parse segments from label references.
    do {
        start_segment_count = offset_to_segment.size;

        for (const [label, type] of labels) {
            parse_segment(offset_to_segment, label_holder, cursor, label, type, lenient);
        }

        // Find label references.
        const sorted_segments = [...offset_to_segment.entries()]
            .filter(([, s]) => s.type === SegmentType.Instructions)
            .sort(([a], [b]) => a - b)
            .map(([, s]) => s as InstructionSegment);

        const cfg = ControlFlowGraph.create(sorted_segments);

        labels = new Map();

        for (const segment of sorted_segments) {
            for (const instruction of segment.instructions) {
                for (let i = 0; i < instruction.opcode.params.length; i++) {
                    const param = instruction.opcode.params[i];

                    switch (param.type) {
                        case TYPE_I_LABEL:
                            get_arg_label_values(
                                cfg,
                                labels,
                                instruction,
                                i,
                                SegmentType.Instructions
                            );
                            break;
                        case TYPE_I_LABEL_VAR:
                            // Never on the stack.
                            // Eat all remaining arguments.
                            for (; i < instruction.args.length; i++) {
                                labels.set(instruction.args[i].value, SegmentType.Instructions);
                            }

                            break;
                        case TYPE_D_LABEL:
                            get_arg_label_values(cfg, labels, instruction, i, SegmentType.Data);
                            break;
                        case TYPE_S_LABEL:
                            get_arg_label_values(cfg, labels, instruction, i, SegmentType.String);
                            break;
                        default:
                            if (param.type instanceof RegTupRefType) {
                                // Never on the stack.
                                const arg = instruction.args[i];

                                for (let j = 0; j < param.type.register_tuples.length; j++) {
                                    const reg_tup = param.type.register_tuples[j];

                                    if (reg_tup.type === TYPE_I_LABEL) {
                                        const label_values = register_value(
                                            cfg,
                                            instruction,
                                            arg.value + j
                                        );

                                        if (label_values.size() <= 10) {
                                            for (const label of label_values) {
                                                labels.set(label, SegmentType.Instructions);
                                            }
                                        }
                                    }
                                }
                            }

                            break;
                    }
                }
            }
        }
    } while (offset_to_segment.size > start_segment_count);
}

/**
 * @returns immediate arguments or stack arguments.
 */
function get_arg_label_values(
    cfg: ControlFlowGraph,
    labels: Map<number, SegmentType>,
    instruction: Instruction,
    param_idx: number,
    segment_type: SegmentType
): void {
    if (instruction.opcode.stack === StackInteraction.Pop) {
        const stack_values = stack_value(
            cfg,
            instruction,
            instruction.opcode.params.length - param_idx - 1
        );

        if (stack_values.size() <= 10) {
            for (const value of stack_values) {
                const old_type = labels.get(value);

                if (
                    old_type == undefined ||
                    SEGMENT_PRIORITY[segment_type] > SEGMENT_PRIORITY[old_type]
                ) {
                    labels.set(value, segment_type);
                }
            }
        }
    } else {
        const value = instruction.args[param_idx].value;
        const old_type = labels.get(value);

        if (old_type == undefined || SEGMENT_PRIORITY[segment_type] > SEGMENT_PRIORITY[old_type]) {
            labels.set(value, segment_type);
        }
    }
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
            case SegmentType.String:
                parse_string_segment(offset_to_segment, cursor, end_offset, labels);
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

    // Recurse on label drop-through.
    if (next_label != undefined) {
        // Find the first non-nop.
        let last_opcode: Opcode | undefined;

        for (let i = instructions.length - 1; i >= 0; i--) {
            last_opcode = instructions[i].opcode;

            if (last_opcode !== Opcode.NOP) {
                break;
            }
        }

        if (last_opcode !== Opcode.RET && last_opcode !== Opcode.JMP) {
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

function parse_string_segment(
    offset_to_segment: Map<number, Segment>,
    cursor: Cursor,
    end_offset: number,
    labels: number[]
) {
    const start_offset = cursor.position;
    const segment: StringSegment = {
        type: SegmentType.String,
        labels,
        value: cursor.string_utf16(end_offset - start_offset, true, true),
    };
    offset_to_segment.set(start_offset, segment);
}

function parse_instruction_arguments(cursor: Cursor, opcode: Opcode): Arg[] {
    const args: Arg[] = [];

    if (opcode.stack !== StackInteraction.Pop) {
        for (const param of opcode.params) {
            switch (param.type) {
                case TYPE_BYTE:
                    args.push({ value: cursor.u8(), size: 1 });
                    break;
                case TYPE_WORD:
                    args.push({ value: cursor.u16(), size: 2 });
                    break;
                case TYPE_DWORD:
                    args.push({ value: cursor.i32(), size: 4 });
                    break;
                case TYPE_FLOAT:
                    args.push({ value: cursor.f32(), size: 4 });
                    break;
                case TYPE_LABEL:
                case TYPE_I_LABEL:
                case TYPE_D_LABEL:
                case TYPE_S_LABEL:
                    args.push({ value: cursor.u16(), size: 2 });
                    break;
                case TYPE_STRING:
                    {
                        const start_pos = cursor.position;
                        args.push({
                            value: cursor.string_utf16(
                                Math.min(4096, cursor.bytes_left),
                                true,
                                false
                            ),
                            size: cursor.position - start_pos,
                        });
                    }
                    break;
                case TYPE_I_LABEL_VAR:
                    {
                        const arg_size = cursor.u8();
                        args.push(...cursor.u16_array(arg_size).map(value => ({ value, size: 2 })));
                    }
                    break;
                case TYPE_REG_REF:
                    args.push({ value: cursor.u8(), size: 1 });
                    break;
                case TYPE_REG_REF_VAR:
                    {
                        const arg_size = cursor.u8();
                        args.push(...cursor.u8_array(arg_size).map(value => ({ value, size: 1 })));
                    }
                    break;
                default:
                    if (param.type instanceof RegTupRefType) {
                        args.push({ value: cursor.u8(), size: 1 });
                        break;
                    } else {
                        throw new Error(`Parameter type ${param.type} not implemented.`);
                    }
            }
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

                if (opcode.stack !== StackInteraction.Pop) {
                    for (let i = 0; i < opcode.params.length; i++) {
                        const param = opcode.params[i];
                        const args = instruction.param_to_args[i];
                        const [arg] = args;

                        switch (param.type) {
                            case TYPE_BYTE:
                                if (arg.value >= 0) {
                                    cursor.write_u8(arg.value);
                                } else {
                                    cursor.write_i8(arg.value);
                                }
                                break;
                            case TYPE_WORD:
                                if (arg.value >= 0) {
                                    cursor.write_u16(arg.value);
                                } else {
                                    cursor.write_i16(arg.value);
                                }
                                break;
                            case TYPE_DWORD:
                                if (arg.value >= 0) {
                                    cursor.write_u32(arg.value);
                                } else {
                                    cursor.write_i32(arg.value);
                                }
                                break;
                            case TYPE_FLOAT:
                                cursor.write_f32(arg.value);
                                break;
                            case TYPE_LABEL: // Abstract type
                            case TYPE_I_LABEL:
                            case TYPE_D_LABEL:
                            case TYPE_S_LABEL:
                                cursor.write_u16(arg.value);
                                break;
                            case TYPE_STRING:
                                cursor.write_string_utf16(arg.value, arg.size);
                                break;
                            case TYPE_I_LABEL_VAR:
                                cursor.write_u8(args.length);
                                cursor.write_u16_array(args.map(arg => arg.value));
                                break;
                            case TYPE_REF: // Abstract type
                            case TYPE_REG_REF:
                                cursor.write_u8(arg.value);
                                break;
                            case TYPE_REG_REF_VAR:
                                cursor.write_u8(args.length);
                                cursor.write_u8_array(args.map(arg => arg.value));
                                break;
                            default:
                                if (param.type instanceof RegTupRefType) {
                                    cursor.write_u8(arg.value);
                                } else {
                                    // TYPE_ANY and TYPE_POINTER cannot be serialized.
                                    throw new Error(
                                        `Parameter type ${param.type} not implemented.`
                                    );
                                }
                        }
                    }
                }
            }
        } else if (segment.type === SegmentType.String) {
            // String segments should be multiples of 4 bytes.
            const byte_length = 4 * Math.ceil((segment.value.length + 1) / 2);
            cursor.write_string_utf16(segment.value, byte_length);
        } else {
            cursor.write_cursor(new ArrayBufferCursor(segment.data, cursor.endianness));
        }
    }

    return { size: cursor.position - start_pos, label_offsets };
}
