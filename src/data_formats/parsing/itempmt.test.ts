import { parse_item_pmt } from "./itempmt";
import { readFileSync } from "fs";
import { BufferCursor } from "../cursor/BufferCursor";
import { Endianness } from "..";

test("parse_item_pmt", () => {
    const buf = readFileSync("test/resources/ItemPMT.bin");
    const item_pmt = parse_item_pmt(new BufferCursor(buf, Endianness.Little));

    const saber = item_pmt.weapons[1][0];

    expect(saber.id).toBe(177);
    expect(saber.min_atp).toBe(40);
    expect(saber.max_atp).toBe(55);
    expect(saber.ata).toBe(30);
    expect(saber.max_grind).toBe(35);
    expect(saber.req_atp).toBe(30);
});
