import { SimpleProperty } from "./SimpleProperty";

test("flat_mapped property without observers.", () => {
    const property: SimpleProperty<{ x?: SimpleProperty<number> }> = new SimpleProperty({});
    const flat_mapped = property.flat_map(p => p.x ?? new SimpleProperty(13));

    expect(flat_mapped.val).toBe(13);

    property.val = { x: new SimpleProperty(17) };

    expect(flat_mapped.val).toBe(17);

    property.val.x!.val = 23;

    expect(flat_mapped.val).toBe(23);
});
