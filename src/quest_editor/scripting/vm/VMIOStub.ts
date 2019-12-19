import { stub } from "../../../core/decorators";
import { AsmToken } from "../instructions";
import { VirtualMachineIO } from "./io";

/* eslint-disable */
/**
 * All methods of VirtualMachineIO implemented as stubs.
 */
export class VMIOStub implements VirtualMachineIO {
    @stub set_floor_handler(area_id: number, label: number): void {}
    @stub window_msg(msg: string): void {}
    @stub message(msg: string): void {}
    @stub add_msg(msg: string): void {}
    @stub winend(): void {}
    @stub mesend(): void {}
    @stub list(list_items: string[]): void {}
    @stub warning(msg: string, srcloc?: AsmToken): void {}
    @stub error(err: Error, srcloc?: AsmToken): void {}
}
/* eslint-enable */
