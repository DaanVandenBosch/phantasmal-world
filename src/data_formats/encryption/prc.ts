import { BufferCursor } from "../BufferCursor";

/**
 * Decrypts the bytes left in cursor.
 */
export function decrypt(key: number, cursor: BufferCursor): BufferCursor {
    return new PrcDecryptor(key).decrypt(cursor);
}

class PrcDecryptor {
    private keys = new Uint32Array(56);
    private key_pos = 56;

    constructor(key: number) {
        this.construct_keys(key);
    }

    decrypt(cursor: BufferCursor): BufferCursor {
        // Size should be divisible by 4.
        const actual_size = cursor.bytes_left;
        const size = Math.ceil(actual_size / 4) * 4;
        const out_cursor = new BufferCursor(size, cursor.little_endian);

        for (let pos = 0; pos < size; pos += 4) {
            let u32;

            if (cursor.bytes_left >= 4) {
                u32 = cursor.u32();
            } else {
                // If the actual size of the cursor is not divisible by 4, "append" nul bytes until it is.
                const left_over = cursor.bytes_left;
                u32 = 0;

                for (let i = 0; i < left_over; i++) {
                    if (cursor.little_endian) {
                        u32 |= cursor.u8() << (8 * i);
                    } else {
                        u32 |= cursor.u8() << (8 * (3 - i));
                    }
                }
            }

            out_cursor.write_u32(this.decrypt_u32(u32));
        }

        out_cursor.position = 0;
        out_cursor.size = actual_size;
        return out_cursor;
    }

    private construct_keys(key: number) {
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

    private mix_keys() {
        let ptr = 1;

        for (let i = 24; i; --i, ++ptr) {
            this.keys[ptr] -= this.keys[ptr + 31];
        }

        ptr = 25;

        for (let i = 31; i; --i, ++ptr) {
            this.keys[ptr] -= this.keys[ptr - 24];
        }
    }

    private decrypt_u32(data: number) {
        if (this.key_pos === 56) {
            this.mix_keys();
            this.key_pos = 1;
        }

        return data ^ this.keys[this.key_pos++];
    }
}
