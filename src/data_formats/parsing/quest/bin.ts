import Logger from "js-logger";
import { Endianness } from "../..";
import { Cursor } from "../../cursor/Cursor";
import { WritableCursor } from "../../cursor/WritableCursor";
import { WritableResizableBufferCursor } from "../../cursor/WritableResizableBufferCursor";
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
        /**
         * Map of labels to instruction indices.
         */
        readonly labels: Map<number, number>,
        readonly instructions: Instruction[],
        readonly shop_items: number[]
    ) {}

    get_label_instructions(label: number): Instruction[] | undefined {
        const index = this.labels.get(label);

        if (index == null || index > this.instructions.length) return undefined;

        const instructions: Instruction[] = [];

        for (let i = index; i < this.instructions.length; i++) {
            const instruction = this.instructions[i];
            instructions.push(instruction);

            if (instruction.opcode === Opcode.ret) {
                break;
            }
        }

        return instructions;
    }
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

            if (arg == null) {
                break;
            }

            switch (type) {
                case Type.U8Var:
                case Type.U16Var:
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

        this.size = opcode.code_size + this.arg_size;
    }
}

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

    const object_code = cursor
        .seek_start(object_code_offset)
        .take(label_offset_table_offset - object_code_offset);

    const instructions = parse_object_code(object_code, lenient);

    let instruction_size = 0;

    for (const instruction of instructions) {
        instruction_size += instruction.size;
    }

    if (object_code.size !== instruction_size) {
        throw new Error(
            `Expected to parse ${object_code.size} bytes but parsed ${instruction_size} instead.`
        );
    }

    const label_offset_count = Math.floor((cursor.size - label_offset_table_offset) / 4);
    cursor.seek_start(label_offset_table_offset);

    const labels = new Map<number, number>();

    for (let label = 0; label < label_offset_count; ++label) {
        const offset = cursor.i32();

        if (offset >= 0) {
            let size = 0;
            let index = 0;

            for (const instruction of instructions) {
                if (offset === size) {
                    break;
                } else if (offset < size) {
                    logger.warn(
                        `Label ${label} offset ${offset} does not point to the start of an instruction.`
                    );
                    break;
                }

                size += instruction.size;
                index++;
            }

            if (index >= instructions.length) {
                logger.warn(`Label ${label} offset ${offset} is too large.`);
            } else {
                labels.set(label, index);
            }
        }
    }

    return new BinFile(
        quest_id,
        language,
        quest_name,
        short_description,
        long_description,
        labels,
        instructions,
        shop_items
    );
}

export function write_bin(bin: BinFile): ArrayBuffer {
    const labels: number[] = [...bin.labels.entries()].reduce((ls, [l, i]) => {
        ls[l] = i;
        return ls;
    }, new Array<number>());

    const object_code_offset = 4652;
    const buffer = new ResizableBuffer(
        object_code_offset + 10 * bin.instructions.length + 4 * labels.length
    );
    const cursor = new WritableResizableBufferCursor(buffer, Endianness.Little);

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

    const object_code_size = write_object_code(cursor, bin.instructions);

    for (let label = 0; label < labels.length; label++) {
        const index = labels[label];

        if (index == null) {
            cursor.write_i32(-1);
        } else {
            let offset = 0;

            for (let j = 0; j < bin.instructions.length; j++) {
                const instruction = bin.instructions[j];

                if (j === index) {
                    break;
                } else {
                    offset += instruction.size;
                }
            }

            cursor.write_i32(offset);
        }
    }

    const file_size = cursor.position;

    cursor.seek_start(4);
    cursor.write_u32(object_code_offset + object_code_size);
    cursor.write_u32(file_size);

    return cursor.seek_start(0).array_buffer(file_size);
}

function parse_object_code(cursor: Cursor, lenient: boolean): Instruction[] {
    const instructions: Instruction[] = [];

    try {
        while (cursor.bytes_left) {
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

            try {
                const args = parse_instruction_arguments(cursor, opcode);
                instructions.push(new Instruction(opcode, args));
            } catch (e) {
                logger.warn(
                    `Exception occurred while parsing arguments for instruction ${opcode.mnemonic}.`,
                    e
                );
                instructions.push(new Instruction(opcode, []));
            }
        }
    } catch (e) {
        if (lenient) {
            logger.error("Couldn't fully parse object code.", e);
        } else {
            throw e;
        }
    }

    return instructions;
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
            case Type.U8Var:
                {
                    const arg_size = cursor.u8();
                    args.push(...cursor.u8_array(arg_size).map(value => ({ value, size: 1 })));
                }
                break;
            case Type.U16Var:
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

function write_object_code(cursor: WritableCursor, instructions: Instruction[]): number {
    const start_pos = cursor.position;

    for (const instruction of instructions) {
        const opcode = instruction.opcode;

        if (opcode.code_size === 2) {
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
                case Type.U8Var:
                    cursor.write_u8(args.length);
                    cursor.write_u8_array(args.map(arg => arg.value));
                    break;
                case Type.U16Var:
                    cursor.write_u8(args.length);
                    cursor.write_u16_array(args.map(arg => arg.value));
                    break;
                case Type.String:
                    cursor.write_string_utf16(arg.value, arg.size);
                    break;
                default:
                    throw new Error(
                        `Parameter type ${Type[param.type]} (${param.type}) not implemented.`
                    );
            }
        }
    }

    return cursor.position - start_pos;
}
