import { Kind, Opcode } from "./opcodes";
import { array_buffers_equal, arrays_equal } from "../../core/util";

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
        const len = Math.min(opcode.params.length, args.length);

        for (let i = 0; i < len; i++) {
            const type = opcode.params[i].type;
            const arg = args[i];
            this.param_to_args[i] = [];

            switch (type.kind) {
                case Kind.ILabelVar:
                case Kind.RegRefVar:
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

/**
 * Instruction argument.
 */
export type Arg = {
    value: any;
    size: number;
};

export enum SegmentType {
    Instructions,
    Data,
    String,
}

/**
 * Segment of object code.
 */
export type Segment = InstructionSegment | DataSegment | StringSegment;

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

export type StringSegment = {
    type: SegmentType.String;
    labels: number[];
    value: string;
};

export function object_code_equal(a: Segment[], b: Segment[]): boolean {
    return arrays_equal(a, b, segments_equal);
}

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

function instructions_equal(a: Instruction, b: Instruction): boolean {
    return a.opcode.code === b.opcode.code && arrays_equal(a.args, b.args, args_equal);
}

function args_equal(a: Arg, b: Arg): boolean {
    return a.value === b.value && a.size === b.size;
}
