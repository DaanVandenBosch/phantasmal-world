/**
 * This code is based on the Sylverant PRS compression code written by Lawrence Sebald.
 */

import { ArrayBufferCursor } from '../../ArrayBufferCursor';

export function compress(src: ArrayBufferCursor): ArrayBufferCursor {
    const ctx = new Context(src);
    const hashTable = new HashTable();

    if (ctx.src.size <= 3) {
        // Make a literal copy of the input.
        while (ctx.src.bytesLeft) {
            ctx.setBit(1);
            ctx.copyLiteral();
        }
    } else {
        // Add the first two "strings" to the hash table.
        hashTable.put(hashTable.hash(ctx.src), 0);
        ctx.src.seek(1);
        hashTable.put(hashTable.hash(ctx.src), 1);
        ctx.src.seek(-1);

        // Copy the first two bytes as literals.
        ctx.setBit(1);
        ctx.copyLiteral();
        ctx.setBit(1);
        ctx.copyLiteral();

        while (ctx.src.bytesLeft > 1) {
            let [offset, mlen] = ctx.findLongestMatch(hashTable, false);

            if (mlen > 0) {
                ctx.src.seek(1);
                const [offset2, mlen2] = ctx.findLongestMatch(hashTable, true);
                ctx.src.seek(-1);

                // Did the "lazy match" produce something more compressed?
                if (mlen2 > mlen) {
                    let copyLiteral = true;
                    // Check if it is a good idea to switch from a short match to a long one.
                    if (mlen >= 2 && mlen <= 5 && offset2 < offset) {
                        if (offset >= -256 && offset2 < -256) {
                            if (mlen2 - mlen < 3) {
                                copyLiteral = false;
                            }
                        }
                    }

                    if (copyLiteral) {
                        ctx.setBit(1);
                        ctx.copyLiteral();
                        continue;
                    }
                }

                // What kind of match did we find?
                if (mlen >= 2 && mlen <= 5 && offset >= -256) {
                    // Short match.
                    ctx.setBit(0);
                    ctx.setBit(0);
                    ctx.setBit((mlen - 2) & 0x02);
                    ctx.setBit((mlen - 2) & 0x01);
                    ctx.writeLiteral(offset & 0xFF);
                    ctx.addIntermediates(hashTable, mlen);
                    continue;
                } else if (mlen >= 3 && mlen <= 9) {
                    // Long match, short length.
                    ctx.setBit(0);
                    ctx.setBit(1);
                    ctx.writeLiteral(((offset & 0x1F) << 3) | ((mlen - 2) & 0x07));
                    ctx.writeLiteral(offset >> 5);
                    ctx.addIntermediates(hashTable, mlen);
                    continue;
                } else if (mlen > 9) {
                    // Long match, long length.
                    if (mlen > 256) {
                        mlen = 256;
                    }

                    ctx.setBit(0);
                    ctx.setBit(1);
                    ctx.writeLiteral((offset & 0x1F) << 3);
                    ctx.writeLiteral(offset >> 5);
                    ctx.writeLiteral(mlen - 1);
                    ctx.addIntermediates(hashTable, mlen);
                    continue;
                }
            }

            // If we get here, we didn't find a suitable match, so just we just make a literal copy.
            ctx.setBit(1);
            ctx.copyLiteral();
        }

        // If there's a left over byte at the end, make a literal copy.
        if (ctx.src.bytesLeft) {
            ctx.setBit(1);
            ctx.copyLiteral();
        }
    }

    ctx.writeEof();

    return ctx.dst.seekStart(0);
}

const MAX_WINDOW = 0x2000;
const WINDOW_MASK = MAX_WINDOW - 1;
const HASH_SIZE = 1 << 8;

class Context {
    src: ArrayBufferCursor;
    dst: ArrayBufferCursor;
    flags: number;
    flagBitsLeft: number;
    flagOffset: number;

    constructor(cursor: ArrayBufferCursor) {
        this.src = cursor;
        this.dst = new ArrayBufferCursor(cursor.size, cursor.littleEndian);
        this.flags = 0;
        this.flagBitsLeft = 0;
        this.flagOffset = 0;
    }

    setBit(bit: number): void {
        if (!this.flagBitsLeft--) {
            // Write out the flags to their position in the file, and store the next flags byte position.
            const pos = this.dst.position;
            this.dst
                .seekStart(this.flagOffset)
                .writeU8(this.flags)
                .seekStart(pos)
                .writeU8(0); // Placeholder for the next flags byte.
            this.flagOffset = pos;
            this.flagBitsLeft = 7;
        }

        this.flags >>>= 1;

        if (bit) {
            this.flags |= 0x80;
        }
    }

    copyLiteral(): void {
        this.dst.writeU8(this.src.u8());
    }

    writeLiteral(value: number): void {
        this.dst.writeU8(value);
    }

    writeFinalFlags(): void {
        this.flags >>>= this.flagBitsLeft;
        const pos = this.dst.position;
        this.dst
            .seekStart(this.flagOffset)
            .writeU8(this.flags)
            .seekStart(pos);
    }

    writeEof(): void {
        this.setBit(0);
        this.setBit(1);

        this.writeFinalFlags();

        this.writeLiteral(0);
        this.writeLiteral(0);
    }

    matchLength(s2: number): number {
        const array = this.src.uint8ArrayView();
        let len = 0;
        let s1 = this.src.position;

        while (s1 < array.byteLength && array[s1] === array[s2]) {
            ++len;
            ++s1;
            ++s2;
        }

        return len;
    }

    findLongestMatch(hashTable: HashTable, lazy: boolean): [number, number] {
        if (!this.src.bytesLeft) {
            return [0, 0];
        }

        // Figure out where we're looking.
        const hash = hashTable.hash(this.src);

        // If there is nothing in the table at that point, bail out now.
        let entry = hashTable.get(hash);

        if (entry === null) {
            if (!lazy) {
                hashTable.put(hash, this.src.position);
            }

            return [0, 0];
        }

        // If we'd go outside the window, truncate the hash chain now. 
        if (this.src.position - entry > MAX_WINDOW) {
            hashTable.hashToOffset[hash] = null;

            if (!lazy) {
                hashTable.put(hash, this.src.position);
            }

            return [0, 0];
        }

        // Ok, we have something in the hash table that matches the hash value.
        // Follow the chain to see if we have an actual string match, and find the longest match.
        let longestLength = 0;
        let longestMatch = 0;

        while (entry != null) {
            const mlen = this.matchLength(entry);

            if (mlen > longestLength || mlen >= 256) {
                longestLength = mlen;
                longestMatch = entry;
            }

            // Follow the chain, making sure not to exceed a difference of MAX_WINDOW.
            let entry2 = hashTable.prev(entry);

            if (entry2 !== null) {
                // If we'd go outside the window, truncate the hash chain now.
                if (this.src.position - entry2 > MAX_WINDOW) {
                    hashTable.setPrev(entry, null);
                    entry2 = null;
                }
            }

            entry = entry2;
        }

        // Add our current string to the hash.
        if (!lazy) {
            hashTable.put(hash, this.src.position);
        }

        // Did we find a match?
        const offset = longestLength > 0 ? longestMatch - this.src.position : 0;
        return [offset, longestLength];
    }

    addIntermediates(hashTable: HashTable, len: number): void {
        this.src.seek(1);

        for (let i = 1; i < len; ++i) {
            const hash = hashTable.hash(this.src);
            hashTable.put(hash, this.src.position);
            this.src.seek(1);
        }
    }
}

class HashTable {
    hashToOffset: Array<number | null> = new Array(HASH_SIZE).fill(null);
    maskedOffsetToPrev: Array<number | null> = new Array(MAX_WINDOW).fill(null);

    hash(cursor: ArrayBufferCursor): number {
        let hash = cursor.u8();

        if (cursor.bytesLeft) {
            hash ^= cursor.u8();
            cursor.seek(-1);
        }

        cursor.seek(-1);
        return hash;
    }

    get(hash: number): number | null {
        return this.hashToOffset[hash];
    }

    put(hash: number, offset: number): void {
        this.setPrev(offset, this.hashToOffset[hash]);
        this.hashToOffset[hash] = offset;
    }

    prev(offset: number): number | null {
        return this.maskedOffsetToPrev[offset & WINDOW_MASK];
    }

    setPrev(offset: number, prevOffset: number | null): void {
        this.maskedOffsetToPrev[offset & WINDOW_MASK] = prevOffset;
    }
}
