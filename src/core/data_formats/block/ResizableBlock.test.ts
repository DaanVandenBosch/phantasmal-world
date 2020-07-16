import { ResizableBlock } from "./ResizableBlock";
import { Endianness } from "./Endianness";

test("simple properties and invariants", () => {
    const capacity = 500;
    const rb = new ResizableBlock(capacity);

    expect(rb.size).toBe(0);
    expect(rb.capacity).toBe(capacity);
    expect(rb.endianness).toBe(Endianness.Little);
});

test("reallocation of internal buffer when necessary", () => {
    const rb = new ResizableBlock(100);

    expect(rb.size).toBe(0);
    expect(rb.capacity).toBe(100);

    rb.size = 101;

    expect(rb.size).toBe(101);
    expect(rb.capacity).toBeGreaterThanOrEqual(101);

    rb.set_u8(100, 0xab);

    expect(rb.get_u8(100)).toBe(0xab);
});
