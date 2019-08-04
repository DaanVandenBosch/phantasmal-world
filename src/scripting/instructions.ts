import { TYPE_I_LABEL_VAR, TYPE_REG_REF_VAR, Opcode } from "./opcodes";

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

            switch (type) {
                case TYPE_I_LABEL_VAR:
                case TYPE_REG_REF_VAR:
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
