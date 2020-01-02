import {
    Arg,
    DataSegment,
    Instruction,
    InstructionSegment,
    new_arg,
    new_instruction,
    Segment,
    SegmentType,
    StringSegment,
} from "../../asm/instructions";
import { Cursor } from "../../cursor/Cursor";
import { ControlFlowGraph } from "../../asm/data_flow_analysis/ControlFlowGraph";
import { Kind, OP_JMP, OP_RET, Opcode, OPCODES, StackInteraction } from "../../asm/opcodes";
import { get_register_value } from "../../asm/data_flow_analysis/get_register_value";
import { get_stack_value } from "../../asm/data_flow_analysis/get_stack_value";
import { ArrayBufferCursor } from "../../cursor/ArrayBufferCursor";
import { Endianness } from "../../Endianness";
import { LogManager } from "../../../Logger";
import { ResizableBufferCursor } from "../../cursor/ResizableBufferCursor";
import { ResizableBuffer } from "../../ResizableBuffer";

const logger = LogManager.get("core/data_formats/parsing/quest/object_code");

const SEGMENT_PRIORITY: number[] = [];
SEGMENT_PRIORITY[SegmentType.Instructions] = 2;
SEGMENT_PRIORITY[SegmentType.String] = 1;
SEGMENT_PRIORITY[SegmentType.Data] = 0;

export function parse_object_code(
    object_code: ArrayBuffer,
    label_offsets: readonly number[],
    entry_labels: readonly number[],
    lenient: boolean,
    dc_gc_format: boolean,
): Segment[] {
    return internal_parse_object_code(
        new ArrayBufferCursor(object_code, Endianness.Little),
        new LabelHolder(label_offsets),
        entry_labels,
        lenient,
        dc_gc_format,
    );
}

export function write_object_code(
    segments: readonly Segment[],
): { object_code: ArrayBuffer; label_offsets: number[] } {
    const cursor = new ResizableBufferCursor(
        new ResizableBuffer(100 * segments.length),
        Endianness.Little,
    );
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

                        switch (param.type.kind) {
                            case Kind.Byte:
                                if (arg.value >= 0) {
                                    cursor.write_u8(arg.value);
                                } else {
                                    cursor.write_i8(arg.value);
                                }
                                break;
                            case Kind.Word:
                                if (arg.value >= 0) {
                                    cursor.write_u16(arg.value);
                                } else {
                                    cursor.write_i16(arg.value);
                                }
                                break;
                            case Kind.DWord:
                                if (arg.value >= 0) {
                                    cursor.write_u32(arg.value);
                                } else {
                                    cursor.write_i32(arg.value);
                                }
                                break;
                            case Kind.Float:
                                cursor.write_f32(arg.value);
                                break;
                            case Kind.Label:
                            case Kind.ILabel:
                            case Kind.DLabel:
                            case Kind.SLabel:
                                cursor.write_u16(arg.value);
                                break;
                            case Kind.String:
                                cursor.write_string_utf16(arg.value, arg.size);
                                break;
                            case Kind.ILabelVar:
                                cursor.write_u8(args.length);
                                cursor.write_u16_array(args.map(arg => arg.value));
                                break;
                            case Kind.RegRef:
                            case Kind.RegTupRef:
                                cursor.write_u8(arg.value);
                                break;
                            case Kind.RegRefVar:
                                cursor.write_u8(args.length);
                                cursor.write_u8_array(args.map(arg => arg.value));
                                break;
                            default:
                                // TYPE_ANY, TYPE_VALUE and TYPE_POINTER cannot be serialized.
                                throw new Error(
                                    `Parameter type ${Kind[param.type.kind]} not implemented.`,
                                );
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

    for (let i = 0; i < label_offsets.length; i++) {
        if (label_offsets[i] == undefined) {
            label_offsets[i] = -1;
        }
    }

    return { object_code: cursor.seek_start(0).array_buffer(), label_offsets };
}

function internal_parse_object_code(
    cursor: Cursor,
    label_holder: LabelHolder,
    entry_labels: readonly number[],
    lenient: boolean,
    dc_gc_format: boolean,
): Segment[] {
    const offset_to_segment = new Map<number, Segment>();

    find_and_parse_segments(
        cursor,
        label_holder,
        entry_labels.reduce((m, l) => m.set(l, SegmentType.Instructions), new Map()),
        offset_to_segment,
        lenient,
        dc_gc_format,
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
                    `Next offset ${end_offset} was smaller than or equal to current offset ${offset}.`,
                );
                break;
            }

            // Should never happen either.
            if (!segment) {
                logger.error(`Couldn't create segment for offset ${offset}.`);
                break;
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
    lenient: boolean,
    dc_gc_format: boolean,
): void {
    let start_segment_count: number;

    // Iteratively parse segments from label references.
    do {
        start_segment_count = offset_to_segment.size;

        for (const [label, type] of labels) {
            parse_segment(
                offset_to_segment,
                label_holder,
                cursor,
                label,
                type,
                lenient,
                dc_gc_format,
            );
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

                    switch (param.type.kind) {
                        case Kind.ILabel:
                            get_arg_label_values(
                                cfg,
                                labels,
                                instruction,
                                i,
                                SegmentType.Instructions,
                            );
                            break;
                        case Kind.ILabelVar:
                            // Never on the stack.
                            // Eat all remaining arguments.
                            for (; i < instruction.args.length; i++) {
                                labels.set(instruction.args[i].value, SegmentType.Instructions);
                            }

                            break;
                        case Kind.DLabel:
                            get_arg_label_values(cfg, labels, instruction, i, SegmentType.Data);
                            break;
                        case Kind.SLabel:
                            get_arg_label_values(cfg, labels, instruction, i, SegmentType.String);
                            break;
                        case Kind.RegTupRef:
                            {
                                // Never on the stack.
                                const arg = instruction.args[i];

                                for (let j = 0; j < param.type.register_tuples.length; j++) {
                                    const reg_tup = param.type.register_tuples[j];

                                    if (reg_tup.type.kind === Kind.ILabel) {
                                        const label_values = get_register_value(
                                            cfg,
                                            instruction,
                                            arg.value + j,
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
    segment_type: SegmentType,
): void {
    if (instruction.opcode.stack === StackInteraction.Pop) {
        const stack_values = get_stack_value(
            cfg,
            instruction,
            instruction.opcode.params.length - param_idx - 1,
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
    lenient: boolean,
    dc_gc_format: boolean,
): void {
    try {
        const info = label_holder.get_info(label);

        if (info == undefined) {
            logger.warn(`Label ${label} is not registered in the label table.`);
            return;
        }

        // Check whether we've already parsed this segment and reparse it if necessary.
        const segment = offset_to_segment.get(info.offset);
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
                    lenient,
                    dc_gc_format,
                );
                break;
            case SegmentType.Data:
                parse_data_segment(offset_to_segment, cursor, end_offset, labels);
                break;
            case SegmentType.String:
                parse_string_segment(offset_to_segment, cursor, end_offset, labels, dc_gc_format);
                break;
            default:
                throw new Error(`Segment type ${SegmentType[type]} not implemented.`);
        }
    } catch (e) {
        if (lenient) {
            logger.error("Couldn't fully parse object code segment.", e);
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
    lenient: boolean,
    dc_gc_format: boolean,
): void {
    const instructions: Instruction[] = [];

    const segment: InstructionSegment = {
        type: SegmentType.Instructions,
        labels,
        instructions,
        asm: { labels: [] },
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

        const opcode = OPCODES[opcode_index];

        // Parse the arguments.
        try {
            const args = parse_instruction_arguments(cursor, opcode, dc_gc_format);
            instructions.push(new_instruction(opcode, args));
        } catch (e) {
            if (lenient) {
                logger.error(
                    `Exception occurred while parsing arguments for instruction ${opcode.mnemonic}.`,
                    e,
                );
                instructions.push(new_instruction(opcode, []));
            } else {
                throw e;
            }
        }
    }

    // Recurse on label drop-through.
    if (next_label != undefined) {
        // Find the first ret or jmp.
        let drop_through = true;

        for (let i = instructions.length - 1; i >= 0; i--) {
            const opcode = instructions[i].opcode;

            if (opcode.code === OP_RET.code || opcode.code === OP_JMP.code) {
                drop_through = false;
                break;
            }
        }

        if (drop_through) {
            parse_segment(
                offset_to_segment,
                label_holder,
                cursor,
                next_label,
                SegmentType.Instructions,
                lenient,
                dc_gc_format,
            );
        }
    }
}

function parse_data_segment(
    offset_to_segment: Map<number, Segment>,
    cursor: Cursor,
    end_offset: number,
    labels: number[],
): void {
    const start_offset = cursor.position;
    const segment: DataSegment = {
        type: SegmentType.Data,
        labels,
        data: cursor.array_buffer(end_offset - start_offset),
        asm: { labels: [] },
    };
    offset_to_segment.set(start_offset, segment);
}

function parse_string_segment(
    offset_to_segment: Map<number, Segment>,
    cursor: Cursor,
    end_offset: number,
    labels: number[],
    dc_gc_format: boolean,
): void {
    const start_offset = cursor.position;
    const segment: StringSegment = {
        type: SegmentType.String,
        labels,
        value: dc_gc_format
            ? cursor.string_ascii(end_offset - start_offset, true, true)
            : cursor.string_utf16(end_offset - start_offset, true, true),
        asm: { labels: [] },
    };
    offset_to_segment.set(start_offset, segment);
}

function parse_instruction_arguments(cursor: Cursor, opcode: Opcode, dc_gc_format: boolean): Arg[] {
    const args: Arg[] = [];

    if (opcode.stack !== StackInteraction.Pop) {
        for (const param of opcode.params) {
            switch (param.type.kind) {
                case Kind.Byte:
                    args.push(new_arg(cursor.u8(), 1));
                    break;
                case Kind.Word:
                    args.push(new_arg(cursor.u16(), 2));
                    break;
                case Kind.DWord:
                    args.push(new_arg(cursor.i32(), 4));
                    break;
                case Kind.Float:
                    args.push(new_arg(cursor.f32(), 4));
                    break;
                case Kind.Label:
                case Kind.ILabel:
                case Kind.DLabel:
                case Kind.SLabel:
                    args.push(new_arg(cursor.u16(), 2));
                    break;
                case Kind.String:
                    {
                        const start_pos = cursor.position;
                        const max_bytes = Math.min(4096, cursor.bytes_left);
                        args.push(
                            new_arg(
                                dc_gc_format
                                    ? cursor.string_ascii(max_bytes, true, false)
                                    : cursor.string_utf16(max_bytes, true, false),
                                cursor.position - start_pos,
                            ),
                        );
                    }
                    break;
                case Kind.ILabelVar:
                    {
                        const arg_size = cursor.u8();
                        args.push(...cursor.u16_array(arg_size).map(value => new_arg(value, 2)));
                    }
                    break;
                case Kind.RegRef:
                case Kind.RegTupRef:
                    args.push(new_arg(cursor.u8(), 1));
                    break;
                case Kind.RegRefVar:
                    {
                        const arg_size = cursor.u8();
                        args.push(...cursor.u8_array(arg_size).map(value => new_arg(value, 1)));
                    }
                    break;
                default:
                    throw new Error(`Parameter type ${Kind[param.type.kind]} not implemented.`);
            }
        }
    }

    return args;
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

    constructor(label_offsets: readonly number[]) {
        // Populate the main label list.
        for (let label = 0; label < label_offsets.length; label++) {
            const offset = label_offsets[label];

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
        label: number,
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
