import { AsmToken } from "../instructions";
import Logger from "js-logger";

const logger = Logger.get("quest_editor/scripting/vm/io");

/**
 * The virtual machine calls these methods when it requires input.
 */
// eslint-disable-next-line @typescript-eslint/no-empty-interface
export interface VirtualMachineInput {
    // unimplemented
}

/**
 * The virtual machine calls these methods when it outputs something.
 */
export interface VirtualMachineOutput {
    bb_map_designate(
        area_id: number,
        map_number: number,
        area_variant_id: number,
        unknown: number,
    ): void;
    set_floor_handler(area_id: number, label: number): void;
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

export class DefaultVirtualMachineIO implements VirtualMachineIO {
    bb_map_designate(area_id: number, area_variant_id: number): void {
        logger.warn(`bb_map_designate(${area_id}, ${area_variant_id})`);
    }

    set_floor_handler(area_id: number, label: number): void {
        logger.warn(`set_floor_handler(${area_id}, ${label})`);
    }

    window_msg(msg: string): void {
        logger.warn(`window_msg("${msg}")`);
    }

    message(msg: string): void {
        logger.warn(`message("${msg}")`);
    }

    add_msg(msg: string): void {
        logger.warn(`add_msg("${msg}")`);
    }

    winend(): void {
        logger.warn("winend");
    }

    mesend(): void {
        logger.warn("mesend");
    }

    list(list_items: string[]): void {
        logger.warn(`list([${list_items.map(i => `"${i}"`).join(", ")}])`);
    }

    warning(msg: string, srcloc?: AsmToken): void {
        logger.warn(msg + this.srcloc_to_string(srcloc));
    }

    error(err: Error, srcloc?: AsmToken): void {
        logger.error(err + this.srcloc_to_string(srcloc));
    }

    private srcloc_to_string(srcloc?: AsmToken): string {
        return srcloc ? ` [${srcloc.line_no}:${srcloc.col}]` : " ";
    }
}
