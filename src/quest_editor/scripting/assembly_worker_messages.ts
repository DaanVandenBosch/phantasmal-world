import { editor } from "monaco-editor";
import { AssemblyError, AssemblyWarning } from "./assembly";
import { Segment } from "./instructions";

export type AssemblyWorkerInput = NewAssemblyInput | AssemblyChangeInput;

export type NewAssemblyInput = {
    readonly type: "new_assembly_input";
    readonly assembly: string[];
};

export type AssemblyChangeInput = {
    readonly type: "assembly_change_input";
    readonly changes: editor.IModelContentChange[];
};

export type AssemblyWorkerOutput = NewObjectCodeOutput;

export type NewObjectCodeOutput = {
    readonly type: "new_object_code_output";
    readonly object_code: Segment[];
    readonly map_designations: Map<number, number>;
    readonly warnings: AssemblyWarning[];
    readonly errors: AssemblyError[];
};
