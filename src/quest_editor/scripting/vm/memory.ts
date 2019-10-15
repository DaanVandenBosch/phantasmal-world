import { ArrayBufferCursor } from "../../../core/data_formats/cursor/ArrayBufferCursor";
import { Endianness } from "../../../core/data_formats/Endianness";
import { Range, ranges_overlap } from "./utils";

export class VirtualMachineMemoryBuffer extends ArrayBufferCursor {
    /**
     * The memory this buffer belongs to.
     */
    public readonly memory: VirtualMachineMemory;
    /**
     * The memory address of this buffer.
     */
    public readonly address: number;

    constructor(memory: VirtualMachineMemory, address: number, size: number) {
        super(new ArrayBuffer(size), Endianness.Little);
        this.memory = memory;
        this.address = address;
    }

    public get_offset(byte_offset: number): VirtualMachineMemorySlot | undefined {
        return this.memory.get(this.address + byte_offset);
    }

    public free(): void {
        this.memory.free(this.address);
    }

    public zero(): void {
        new Uint32Array(this.backing_buffer).fill(0);
    }
}

/**
 * Represents a single location in memory.
 */
export class VirtualMachineMemorySlot {
    /**
     * The memory this slot belongs to.
     */
    public readonly memory: VirtualMachineMemory;
    /**
     * The memory address this slots represents.
     */
    public readonly address: number;
    /**
     * The allocated buffer this slot is a part of.
     */
    public readonly buffer: VirtualMachineMemoryBuffer;
    /**
     * The offset that this slot represents in the buffer.
     */
    public readonly byte_offset: number;

    constructor(
        memory: VirtualMachineMemory,
        address: number,
        buffer: VirtualMachineMemoryBuffer,
        byte_offset: number,
    ) {
        this.memory = memory;
        this.address = address;
        this.buffer = buffer;
        this.byte_offset = byte_offset;
    }
}

/**
 * Maps memory addresses to buffers.
 */
export class VirtualMachineMemory {
    private allocated_ranges: Range[] = [];
    private ranges_sorted: boolean = true;
    private memory: Map<number, VirtualMachineMemorySlot> = new Map();

    private sort_ranges(): void {
        this.allocated_ranges.sort((a, b) => a[0] - b[0]);

        this.ranges_sorted = true;
    }

    /**
     * Would a buffer of the given size fit at the given address?
     */
    private will_fit(address: number, size: number): boolean {
        const fit_range: Range = [address, address + size - 1];

        if (!this.ranges_sorted) {
            this.sort_ranges();
        }

        // check if it would overlap any already allocated space
        for (const alloc_range of this.allocated_ranges) {
            if (ranges_overlap(alloc_range, fit_range)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns an address where a buffer of the given size would fit.
     */
    private find_free_space(size: number): number {
        let address = 0;

        // nothing yet allocated, we can place it wherever
        if (this.allocated_ranges.length < 1) {
            return address;
        }

        if (!this.ranges_sorted) {
            this.sort_ranges();
        }

        // check if buffer could fit in between allocated buffers
        for (const alloc_range of this.allocated_ranges) {
            if (!ranges_overlap(alloc_range, [address, address + size - 1])) {
                return address;
            }

            address = alloc_range[1] + 1;
        }

        // just place it at the end
        return address;
    }

    /**
     * Allocate a buffer of the given size at the given address.
     * If the address is omitted a suitable location is chosen.
     * @returns The allocated buffer.
     */
    public allocate(size: number, address?: number): VirtualMachineMemoryBuffer {
        if (size <= 0) {
            throw new Error("Allocation failed: The size of the buffer must be greater than 0");
        }

        // check if given address is good or find an address if none was given
        if (address === undefined) {
            address = this.find_free_space(size);
        } else {
            if (!this.will_fit(address, size)) {
                throw new Error(
                    "Allocation failed: Cannot fit a buffer of the given size at the given address",
                );
            }
        }

        // save the range of allocated memory
        this.allocated_ranges.push([address, address + size - 1]);
        this.ranges_sorted = false;

        // the actual buffer
        const buf = new VirtualMachineMemoryBuffer(this, address, size);

        // set addresses to correct buffer offsets
        for (let offset = 0; offset < size; offset++) {
            this.memory.set(
                address + offset,
                new VirtualMachineMemorySlot(this, address, buf, offset),
            );
        }

        return buf;
    }

    /**
     * Free the memory allocated for the buffer at the given address.
     */
    public free(address: number): void {
        // check if address is a valid allocated buffer
        let range: Range | undefined = undefined;
        let range_idx = -1;

        for (let i = 0; i < this.allocated_ranges.length; i++) {
            const cur = this.allocated_ranges[i];
            if (cur[0] === address) {
                range = cur;
                range_idx = i;
                break;
            }
        }

        if (range === undefined) {
            throw new Error("Free failed: Given address is not the start of an allocated buffer");
        }

        const [alloc_start, alloc_end] = range;

        // remove addresses
        for (let addr = alloc_start; addr <= alloc_end; addr++) {
            this.memory.delete(addr);
        }

        // remove range
        this.allocated_ranges.splice(range_idx, 1);
    }

    /**
     * Gets the memory at the given address. Returns undefined if
     * there is nothing allocated at the given address.
     */
    public get(address: number): VirtualMachineMemorySlot | undefined {
        if (this.memory.has(address)) {
            return this.memory.get(address)!;
        }

        return undefined;
    }
}
