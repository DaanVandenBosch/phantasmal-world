import { AssemblyError, AssemblyWarning } from "./assembly";
import { Segment } from "./instructions";
import { Opcode } from "./opcodes";

export enum InputMessageType {
    NewAssembly,
    AssemblyChange,
    SignatureHelp,
}

export type AssemblyWorkerInput = NewAssemblyInput | AssemblyChangeInput | SignatureHelpInput;

export type NewAssemblyInput = {
    readonly type: InputMessageType.NewAssembly;
    readonly assembly: string[];
};

export type AssemblyChangeInput = {
    readonly type: InputMessageType.AssemblyChange;
    readonly changes: {
        start_line_no: number;
        start_col: number;
        end_line_no: number;
        end_col: number;
        new_text: string;
    }[];
};

export type SignatureHelpInput = {
    readonly type: InputMessageType.SignatureHelp;
    readonly id: number;
    readonly line_no: number;
    readonly col: number;
};

export enum OutputMessageType {
    NewObjectCode,
    SignatureHelp,
}

export type AssemblyWorkerOutput = NewObjectCodeOutput | SignatureHelpOutput;

export type NewObjectCodeOutput = {
    readonly type: OutputMessageType.NewObjectCode;
    readonly object_code: Segment[];
    readonly map_designations: Map<number, number>;
    readonly warnings: AssemblyWarning[];
    readonly errors: AssemblyError[];
};

export type SignatureHelpOutput = {
    readonly type: OutputMessageType.SignatureHelp;
    readonly id: number;
    readonly opcode?: Opcode;
    readonly active_param: number;
};
