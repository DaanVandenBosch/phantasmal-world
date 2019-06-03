/**
 * This code is based on the Sylverant PRS decompression code written by Lawrence Sebald.
 */

/*eslint no-use-before-define: "off"*/
import { ArrayBufferCursor } from '../../ArrayBufferCursor';

export function decompress(cursor: ArrayBufferCursor) {
    const ctx = new Context(cursor);

    while (true) {
        if (ctx.readFlagBit() === 1) {
            // Single byte copy.
            ctx.copyU8();
        } else {
            // Multi byte copy.
            let length;
            let offset;

            if (ctx.readFlagBit() === 0) {
                // Short copy.
                length = ctx.readFlagBit() << 1;
                length |= ctx.readFlagBit();
                length += 2;

                offset = ctx.readU8() - 256;
            } else {
                // Long copy or end of file.
                offset = ctx.readU16();

                // Two zero bytes implies that this is the end of the file.
                if (offset === 0) {
                    break;
                }

                // Do we need to read a length byte, or is it encoded in what we already have?
                length = offset & 0b111;
                offset >>>= 3;

                if (length === 0) {
                    length = ctx.readU8();
                    length += 1;
                } else {
                    length += 2;
                }

                offset -= 8192;
            }

            ctx.offsetCopy(offset, length);
        }
    }

    return ctx.dst.seekStart(0);
}

class Context {
    src: ArrayBufferCursor;
    dst: ArrayBufferCursor;
    flags: number;
    flagBitsLeft: number;

    constructor(cursor: ArrayBufferCursor) {
        this.src = cursor;
        this.dst = new ArrayBufferCursor(4 * cursor.size, cursor.littleEndian);
        this.flags = 0;
        this.flagBitsLeft = 0;
    }

    readFlagBit() {
        // Fetch a new flag byte when the previous byte has been processed.
        if (this.flagBitsLeft === 0) {
            this.flags = this.readU8();
            this.flagBitsLeft = 8;
        }

        let bit = this.flags & 1;
        this.flags >>>= 1;
        this.flagBitsLeft -= 1;
        return bit;
    }

    copyU8() {
        this.dst.writeU8(this.readU8());
    }

    readU8() {
        return this.src.u8();
    }

    readU16() {
        return this.src.u16();
    }

    offsetCopy(offset: number, length: number) {
        if (offset < -8192 || offset > 0) {
            console.error(`offset was ${offset}, should be between -8192 and 0.`);
        }

        if (length < 1 || length > 256) {
            console.error(`length was ${length}, should be between 1 and 256.`);
        }

        // The length can be larger than -offset, in that case we copy -offset bytes size/-offset times.
        const bufSize = Math.min(-offset, length);

        this.dst.seek(offset);
        const buf = this.dst.take(bufSize);
        this.dst.seek(-offset - bufSize);

        for (let i = 0; i < Math.floor(length / bufSize); ++i) {
            this.dst.writeCursor(buf);
        }

        this.dst.writeCursor(buf.take(length % bufSize));
    }
}
