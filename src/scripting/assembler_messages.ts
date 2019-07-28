import { editor } from "monaco-editor";
import { Segment } from "../data_formats/parsing/quest/bin";
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

export type ScriptWorkerOutput = NewObjectCodeOutput;

export type NewObjectCodeOutput = {
    readonly type: "new_object_code_output";
    readonly object_code: Segment[];
    readonly errors: AssemblyError[];
};
