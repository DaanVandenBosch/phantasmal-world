import { Kind, Opcode } from "./opcodes";
import { array_buffers_equal, arrays_equal } from "../../core/util";

/**
 * Instruction invocation.
 */
export type Instruction = {
    readonly opcode: Opcode;
    readonly args: readonly Arg[];
    /**
     * Byte size of the argument list.
     */
    readonly arg_size: number;
    /**
     * Byte size of the entire instruction, i.e. the sum of the opcode size and all argument sizes.
     */
    readonly size: number;
    /**
     * Maps each parameter by index to its arguments.
     */
    readonly param_to_args: readonly Arg[][];
    readonly asm?: InstructionAsm;
};

export function new_instruction(opcode: Opcode, args: Arg[], asm?: InstructionAsm): Instruction {
    const len = Math.min(opcode.params.length, args.length);
    const param_to_args: Arg[][] = [];
    let arg_size = 0;

    for (let i = 0; i < len; i++) {
        const type = opcode.params[i].type;
        const arg = args[i];
        param_to_args[i] = [];

        switch (type.kind) {
            case Kind.ILabelVar:
            case Kind.RegRefVar:
                arg_size++;

                for (let j = i; j < args.length; j++) {
                    param_to_args[i].push(args[j]);
                    arg_size += args[j].size;
                }

                break;
            default:
                arg_size += arg.size;
                param_to_args[i].push(arg);
                break;
        }
    }

    return {
        opcode,
        args,
        arg_size,
        size: opcode.size + arg_size,
        param_to_args,
        asm,
    };
}

function instructions_equal(a: Instruction, b: Instruction): boolean {
    return a.opcode.code === b.opcode.code && arrays_equal(a.args, b.args, args_equal);
}

/**
 * Instruction argument.
 */
export type Arg = {
    readonly value: any;
    readonly size: number;
};

export function new_arg(value: any, size: number): Arg {
    return {
        value,
        size,
    };
}

function args_equal(a: Arg, b: Arg): boolean {
    return a.value === b.value && a.size === b.size;
}

/**
 * Position and length of related assembly code.
 */
export type AsmToken = {
    readonly line_no: number;
    readonly col: number;
    readonly len: number;
};

/**
 * Information about the related assembly code.
 */
export type InstructionAsm = {
    mnemonic?: AsmToken;
    args: AsmToken[];
    stack_args: (AsmToken & { value: number })[];
};

export enum SegmentType {
    Instructions,
    Data,
    String,
}

/**
 * Segment of object code. A segment starts with an instruction, byte or string character that is
 * referenced by one or more labels. The segment ends right before the next instruction, byte or
 * string character that is referenced by a label.
 */
export type Segment = InstructionSegment | DataSegment | StringSegment;

export type InstructionSegment = {
    type: SegmentType.Instructions;
    labels: number[];
    instructions: Instruction[];
    asm: {
        labels: AsmToken[];
    };
};

export type DataSegment = {
    type: SegmentType.Data;
    labels: number[];
    data: ArrayBuffer;
    asm: {
        labels: AsmToken[];
    };
};

export type StringSegment = {
    type: SegmentType.String;
    labels: number[];
    value: string;
    asm: {
        labels: AsmToken[];
    };
};

function segments_equal(a: Segment, b: Segment): boolean {
    if (a.type !== b.type || !arrays_equal(a.labels, b.labels)) return false;

    switch (a.type) {
        case SegmentType.Instructions:
            return arrays_equal(
                a.instructions,
                (b as InstructionSegment).instructions,
                instructions_equal,
            );

        case SegmentType.Data:
            return array_buffers_equal(a.data, (b as DataSegment).data);

        case SegmentType.String:
            return a.value === (b as StringSegment).value;
    }
}

export function segment_arrays_equal(a: Segment[], b: Segment[]): boolean {
    return arrays_equal(a, b, segments_equal);
}
