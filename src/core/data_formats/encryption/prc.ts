import { Endianness } from "../Endianness";
import { ArrayBufferCursor } from "../cursor/ArrayBufferCursor";
import { Cursor } from "../cursor/Cursor";

/**
 * Decrypts the bytes left in cursor.
 */
export function prc_decrypt(key: number, cursor: Cursor): Cursor {
    return new PrcDecryptor(key).decrypt(cursor);
}

class PrcDecryptor {
    private keys = new Uint32Array(56);
    private key_pos = 56;

    constructor(key: number) {
        this.construct_keys(key);
    }

    decrypt(cursor: Cursor): Cursor {
        // Size should be divisible by 4.
        const actual_size = cursor.bytes_left;
        const size = Math.ceil(actual_size / 4) * 4;
        const out_cursor = new ArrayBufferCursor(new ArrayBuffer(actual_size), cursor.endianness);

        for (let pos = 0; pos < size; pos += 4) {
            let u32;

            if (cursor.bytes_left >= 4) {
                u32 = cursor.u32();
                out_cursor.write_u32(this.decrypt_u32(u32));
            } else {
                // If the actual size of the cursor is not divisible by 4, "append" nul bytes until it is.
                const left_over = cursor.bytes_left;
                u32 = 0;

                // Pack left over bytes into a u32.
                for (let i = 0; i < left_over; i++) {
                    if (cursor.endianness === Endianness.Little) {
                        u32 |= cursor.u8() << (8 * i);
                    } else {
                        u32 |= cursor.u8() << (8 * (3 - i));
                    }
                }

                const u32_decrypted = this.decrypt_u32(u32);

                // Unpack the decrypted u32 into bytes again.
                for (let i = 0; i < left_over; i++) {
                    if (cursor.endianness === Endianness.Little) {
                        out_cursor.write_u8((u32_decrypted >>> (8 * i)) & 0xff);
                    } else {
                        out_cursor.write_u8((u32_decrypted >>> (8 * (3 - i))) & 0xff);
                    }
                }
            }
        }

        return out_cursor.seek_start(0);
    }

    private construct_keys(key: number): void {
        this.keys[55] = key;

        let idx;
        let tmp = 1;

        for (let i = 0x15; i <= 0x46e; i += 0x15) {
            idx = i % 55;
            key -= tmp;
            this.keys[idx] = tmp;
            tmp = key;
            key = this.keys[idx];
        }

        this.mix_keys();
        this.mix_keys();
        this.mix_keys();
        this.mix_keys();
    }

    private mix_keys(): void {
        let ptr = 1;

        for (let i = 24; i; --i, ++ptr) {
            this.keys[ptr] -= this.keys[ptr + 31];
        }

        ptr = 25;

        for (let i = 31; i; --i, ++ptr) {
            this.keys[ptr] -= this.keys[ptr - 24];
        }
    }

    private decrypt_u32(data: number): number {
        if (this.key_pos === 56) {
            this.mix_keys();
            this.key_pos = 1;
        }

        return data ^ this.keys[this.key_pos++];
    }
}
