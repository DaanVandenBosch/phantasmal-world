import { readFileSync } from "fs";
import { parse_area_collision_geometry } from "./area_collision_geometry";
import { BufferCursor } from "../cursor/BufferCursor";
import { Endianness } from "../Endianness";

test("parse_area_collision_geometry", () => {
    const buf = readFileSync("test/resources/map_forest01c.rel");
    const object = parse_area_collision_geometry(new BufferCursor(buf, Endianness.Little));

    expect(object.meshes.length).toBe(69);
    expect(object.meshes[0].vertices.length).toBe(11);
    expect(object.meshes[0].vertices[0].x).toBeCloseTo(-589.5195, 4);
    expect(object.meshes[0].vertices[0].y).toBeCloseTo(16.7166, 4);
    expect(object.meshes[0].vertices[0].z).toBeCloseTo(-218.6852, 4);
    expect(object.meshes[0].triangles.length).toBe(12);
    expect(object.meshes[0].triangles[0].flags).toBe(0b100000001);
    expect(object.meshes[0].triangles[0].indices).toEqual([5, 0, 7]);
    expect(object.meshes[0].triangles[0].normal.x).toBeCloseTo(0.0137, 4);
    expect(object.meshes[0].triangles[0].normal.y).toBeCloseTo(0.9994, 4);
    expect(object.meshes[0].triangles[0].normal.z).toBeCloseTo(-0.0307, 4);
});
