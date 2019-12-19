import { Kind, StackInteraction } from "../opcodes";
import { VirtualMachineIO } from "./io";
import { AsmToken, Instruction } from "../instructions";
import { Memory } from "./Memory";
import { Endianness } from "../../../core/data_formats/Endianness";

const ARG_STACK_SLOT_SIZE = 4;
const ARG_STACK_LENGTH = 8;

export class CallStackElement {
    constructor(public seg_idx: number, public inst_idx: number) {}
}

type ArgStackTypeList = [Kind, Kind, Kind, Kind, Kind, Kind, Kind, Kind];

export class Thread {
    private arg_stack = new Memory(ARG_STACK_LENGTH * ARG_STACK_SLOT_SIZE, Endianness.Little);
    private arg_stack_counter: number = 0;
    private arg_stack_types: ArgStackTypeList = Array(ARG_STACK_LENGTH).fill(
        Kind.Any,
    ) as ArgStackTypeList;

    /**
     * Call stack. The top element describes the instruction about to be executed.
     */
    readonly call_stack: CallStackElement[] = [];

    readonly variable_stack: number[] = [];
    /**
     * Global or floor-local?
     */
    readonly global: boolean;

    constructor(public io: VirtualMachineIO, next: CallStackElement, global: boolean) {
        this.call_stack = [next];
        this.global = global;
    }

    call_stack_top(): CallStackElement {
        return this.call_stack[this.call_stack.length - 1];
    }

    push_arg(data: number, type: Kind): void {
        if (this.arg_stack_counter >= ARG_STACK_LENGTH) {
            throw new Error("Argument stack: Stack overflow");
        }

        this.arg_stack.write_u32_at(this.arg_stack_counter * ARG_STACK_SLOT_SIZE, data);
        this.arg_stack_types[this.arg_stack_counter] = type;

        this.arg_stack_counter++;
    }

    fetch_args(inst: Instruction): number[] {
        if (inst.opcode.stack !== StackInteraction.Pop) return [];

        const args: number[] = [];
        const srcloc: AsmToken | undefined = inst.asm && inst.asm.mnemonic;

        if (inst.opcode.params.length !== this.arg_stack_counter) {
            this.io.warning("Argument stack: Argument count mismatch.", srcloc);
        }

        for (let i = 0; i < inst.opcode.params.length; i++) {
            const param = inst.opcode.params[i];

            this.check_arg_type(param.type.kind, this.arg_stack_types[i], srcloc);

            const arg_slot_offset = i * ARG_STACK_SLOT_SIZE;
            switch (param.type.kind) {
                case Kind.Byte:
                    args.push(this.arg_stack.u8_at(arg_slot_offset));
                    break;
                case Kind.Word:
                case Kind.ILabel:
                case Kind.DLabel:
                case Kind.SLabel:
                    args.push(this.arg_stack.u16_at(arg_slot_offset));
                    break;
                case Kind.DWord:
                case Kind.String:
                    args.push(this.arg_stack.u32_at(arg_slot_offset));
                    break;
                case Kind.RegTupRef:
                    if (param.type.register_tuples.length > 0) {
                        args.push(this.arg_stack.u8_at(arg_slot_offset));
                    }
                    break;
                default:
                    throw new Error(
                        `Argument stack: Unhandled parameter kind: ${Kind[param.type.kind]}.`,
                    );
            }
        }

        this.arg_stack_counter = 0;

        return args;
    }

    private check_arg_type(param_kind: Kind, stack_kind: Kind, srcloc?: AsmToken): void {
        let match: boolean;

        switch (param_kind) {
            case Kind.Any:
                match = true;
                break;
            case Kind.Byte:
            case Kind.Word:
            case Kind.DWord:
            case Kind.Float:
            case Kind.String:
            case Kind.Pointer:
                match = stack_kind === param_kind;
                break;
            case Kind.Label:
            case Kind.ILabel:
            case Kind.ILabelVar:
            case Kind.DLabel:
            case Kind.SLabel:
                match = stack_kind === Kind.Word;
                break;
            case Kind.RegRef:
            case Kind.RegTupRef:
            case Kind.RegRefVar:
                match = stack_kind === Kind.Byte;
                break;
        }

        if (!match) {
            this.io.warning(
                `Argument stack: Argument type mismatch, expected ${Kind[param_kind]} but received ${Kind[stack_kind]}.`,
                srcloc,
            );
        }
    }
}
