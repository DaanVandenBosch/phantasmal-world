import { ResizableBuffer } from "./ResizableBuffer";

test("simple properties and invariants", () => {
    const capacity = 500;
    const rb = new ResizableBuffer(capacity);

    expect(rb.size).toBe(0);
    expect(rb.capacity).toBe(capacity);
    expect(rb.backing_buffer.byteLength).toBe(capacity);
    expect(rb.view.byteOffset).toBe(0);
    expect(rb.view.byteLength).toBe(capacity);
});

test("reallocation of internal buffer when necessary", () => {
    const rb = new ResizableBuffer(100);

    expect(rb.size).toBe(0);
    expect(rb.capacity).toBe(100);

    rb.size = 101;

    expect(rb.size).toBe(101);
    expect(rb.capacity).toBeGreaterThanOrEqual(101);
    expect(rb.view.byteLength).toBeGreaterThanOrEqual(101);
});
