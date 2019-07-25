import { editor } from "monaco-editor";
import { Instruction } from "../data_formats/parsing/quest/bin";
import { AssemblyError } from "./assembly";

export type ScriptWorkerInput = NewAssemblyInput | AssemblyChangeInput;

export type NewAssemblyInput = {
    readonly type: "new_assembly_input";
    readonly assembly: string[];
};

export type AssemblyChangeInput = {
    readonly type: "assembly_change_input";
    readonly changes: editor.IModelContentChange[];
};

export type ScriptWorkerOutput = NewErrorsOutput;

export type NewErrorsOutput = {
    readonly type: "new_errors_output";
    readonly instructions: Instruction[];
    readonly labels: Map<number, number>;
    readonly errors: AssemblyError[];
};
