import { AsmToken } from "../instructions";

/**
 * The virtual machine calls these methods when it requires input.
 */
export interface VirtualMachineInput {}

/**
 * The virtual machine calls these methods when it outputs something.
 */
export interface VirtualMachineOutput {
    window_msg(msg: string): void;
    message(msg: string): void;
    add_msg(msg: string): void;
    winend(): void;
    mesend(): void;
    list(list_items: string[]): void;
}

/**
 * Methods that are outside of the context of the game.
 */
export interface VirtualMachineMetaIO {
    /**
     * The virtual machine emits warning messages about suspicious execution
     * patterns that could possibly cause problems or have unintended effects.
     */
    warning(msg: string, srcloc?: AsmToken): void;
    error(err: Error, srcloc?: AsmToken): void;
}

/**
 * Handles input/output to/from the virtual machine.
 */
export interface VirtualMachineIO
    extends VirtualMachineInput,
        VirtualMachineOutput,
        VirtualMachineMetaIO {}
