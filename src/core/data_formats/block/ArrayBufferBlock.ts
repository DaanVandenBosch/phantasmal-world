import { Endianness } from "./Endianness";
import { AbstractWritableBlock } from "./AbstractWritableBlock";

export class ArrayBufferBlock extends AbstractWritableBlock {
    get size(): number {
        return this.data_view.byteLength;
    }

    protected readonly buffer: ArrayBuffer;
    protected readonly data_view: DataView;

    get backing_buffer(): ArrayBuffer {
        return this.buffer;
    }

    constructor(buffer_or_size: ArrayBuffer | number, endianness: Endianness) {
        super(endianness);

        this.buffer =
            typeof buffer_or_size === "number" ? new ArrayBuffer(buffer_or_size) : buffer_or_size;

        this.data_view = new DataView(this.buffer);
    }
}
