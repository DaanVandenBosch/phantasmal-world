import { Endianness } from "./Endianness";
import { AbstractWritableBlock } from "./AbstractWritableBlock";

export class ArrayBufferBlock extends AbstractWritableBlock {
    get size(): number {
        return this.data_view.byteLength;
    }

    protected readonly buffer: ArrayBuffer;
    protected readonly data_view: DataView;

    constructor(size: number, endianness: Endianness) {
        super(endianness);

        this.buffer = new ArrayBuffer(size);
        this.data_view = new DataView(this.buffer);
    }
}
