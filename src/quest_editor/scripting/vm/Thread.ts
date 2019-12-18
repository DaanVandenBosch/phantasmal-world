import { Kind, StackInteraction } from "../opcodes";
import { VirtualMachineIO } from "./io";
import { AsmToken, Instruction } from "../instructions";

const ARG_STACK_SLOT_SIZE = 4;
const ARG_STACK_LENGTH = 8;

export class ExecutionLocation {
    constructor(public seg_idx: number, public inst_idx: number) {}
}

type ArgStackTypeList = [Kind, Kind, Kind, Kind, Kind, Kind, Kind, Kind];

export class Thread {
    private arg_stack = new DataView(new ArrayBuffer(ARG_STACK_LENGTH * ARG_STACK_SLOT_SIZE));
    private arg_stack_counter: number = 0;
    private arg_stack_types: ArgStackTypeList = Array(ARG_STACK_LENGTH).fill(
        Kind.Any,
    ) as ArgStackTypeList;

    /**
     * Call stack. The top element describes the instruction about to be executed.
     */
    readonly call_stack: ExecutionLocation[] = [];

    readonly variable_stack: number[] = [];
    /**
     * Global or floor-local?
     */
    readonly global: boolean;

    constructor(public io: VirtualMachineIO, next: ExecutionLocation, global: boolean) {
        this.call_stack = [next];
        this.global = global;
    }

    call_stack_top(): ExecutionLocation {
        return this.call_stack[this.call_stack.length - 1];
    }

    push_arg(data: number, type: Kind): void {
        if (this.arg_stack_counter >= ARG_STACK_LENGTH) {
            throw new Error("Argument stack: Stack overflow");
        }

        this.arg_stack.setUint32(this.arg_stack_counter * ARG_STACK_SLOT_SIZE, data, true);
        this.arg_stack_types[this.arg_stack_counter] = type;

        this.arg_stack_counter++;
    }

    fetch_args(inst: Instruction): number[] {
        if (inst.opcode.stack !== StackInteraction.Pop) return [];

        const args: number[] = [];
        const srcloc: AsmToken | undefined = inst.asm && inst.asm.mnemonic;

        if (inst.opcode.params.length !== this.arg_stack_counter) {
            this.io.warning("Argument stack: Argument count mismatch", srcloc);
        }

        for (let i = 0; i < inst.opcode.params.length; i++) {
            const param = inst.opcode.params[i];

            if (param.type.kind !== this.arg_stack_types[i]) {
                this.io.warning("Argument stack: Argument type mismatch", srcloc);
            }

            const arg_slot_offset = i * ARG_STACK_SLOT_SIZE;
            switch (param.type.kind) {
                case Kind.Byte:
                    args.push(this.arg_stack.getUint8(arg_slot_offset));
                    break;
                case Kind.Word:
                case Kind.ILabel:
                case Kind.DLabel:
                case Kind.SLabel:
                    args.push(this.arg_stack.getUint16(arg_slot_offset, true));
                    break;
                case Kind.DWord:
                case Kind.String:
                    args.push(this.arg_stack.getUint32(arg_slot_offset, true));
                    break;
                case Kind.RegTupRef:
                    if (param.type.register_tuples.length > 0) {
                        args.push(this.arg_stack.getUint8(arg_slot_offset));
                    }
                    break;
                default:
                    throw new Error(
                        `Argument stack: Unhandled param kind: Kind.${Kind[param.type.kind]}`,
                    );
            }
        }

        this.arg_stack_counter = 0;

        return args;
    }
}
