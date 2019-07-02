import { BufferCursor } from "../BufferCursor";

export type ItemPmt = {
    stat_boosts: PmtStatBoost[];
    armors: PmtArmor[];
    shields: PmtShield[];
    units: PmtUnit[];
    tools: PmtTool[][];
    weapons: PmtWeapon[][];
};

export type PmtStatBoost = {
    stat_1: number;
    stat_2: number;
    amount_1: number;
    amount_2: number;
};

export type PmtWeapon = {
    id: number;
    type: number;
    skin: number;
    team_points: number;
    class: number;
    reserved_1: number;
    min_atp: number;
    max_atp: number;
    req_atp: number;
    req_mst: number;
    req_ata: number;
    mst: number;
    max_grind: number;
    photon: number;
    special: number;
    ata: number;
    stat_boost: number;
    projectile: number;
    photon_trail_1_x: number;
    photon_trail_1_y: number;
    photon_trail_2_x: number;
    photon_trail_2_y: number;
    photon_type: number;
    unknown_1: number[];
    tech_boost: number;
    combo_type: number;
};

export type PmtArmor = {
    id: number;
    type: number;
    skin: number;
    team_points: number;
    dfp: number;
    evp: number;
    block_particle: number;
    block_effect: number;
    class: number;
    reserved_1: number;
    required_level: number;
    efr: number;
    eth: number;
    eic: number;
    edk: number;
    elt: number;
    dfp_range: number;
    evp_range: number;
    stat_boost: number;
    tech_boost: number;
    unknown_1: number;
};

export type PmtShield = PmtArmor;

export type PmtUnit = {
    id: number;
    type: number;
    skin: number;
    team_points: number;
    stat: number;
    stat_amount: number;
    plus_minus: number;
    reserved: number[];
};

export type PmtTool = {
    id: number;
    type: number;
    skin: number;
    team_points: number;
    amount: number;
    tech: number;
    cost: number;
    item_flag: number;
    reserved: number[];
};

export function parse_item_pmt(cursor: BufferCursor): ItemPmt {
    cursor.seek_end(32);
    const main_table_offset = cursor.u32();
    const main_table_size = cursor.u32();
    // const main_table_count = cursor.u32(); // Should be 1.

    cursor.seek_start(main_table_offset);

    const compact_table_offsets = cursor.u16_array(main_table_size);
    const table_offsets: { offset: number; size: number }[] = [];
    let expanded_offset = 0;

    for (const compact_offset of compact_table_offsets) {
        expanded_offset = expanded_offset + 4 * compact_offset;
        cursor.seek_start(expanded_offset - 4);
        const size = cursor.u32();
        const offset = cursor.u32();
        table_offsets.push({ offset, size });
    }

    const item_pmt: ItemPmt = {
        // This size (65268) of this table seems wrong, so we pass in a hard-coded value.
        stat_boosts: parse_stat_boosts(cursor, table_offsets[305].offset, 52),
        armors: parse_armors(cursor, table_offsets[7].offset, table_offsets[7].size),
        shields: parse_shields(cursor, table_offsets[8].offset, table_offsets[8].size),
        units: parse_units(cursor, table_offsets[9].offset, table_offsets[9].size),
        tools: [],
        weapons: [],
    };

    for (let i = 11; i <= 37; i++) {
        item_pmt.tools.push(parse_tools(cursor, table_offsets[i].offset, table_offsets[i].size));
    }

    for (let i = 38; i <= 275; i++) {
        item_pmt.weapons.push(
            parse_weapons(cursor, table_offsets[i].offset, table_offsets[i].size)
        );
    }

    return item_pmt;
}

function parse_stat_boosts(cursor: BufferCursor, offset: number, size: number): PmtStatBoost[] {
    cursor.seek_start(offset);
    const stat_boosts: PmtStatBoost[] = [];

    for (let i = 0; i < size; i++) {
        stat_boosts.push({
            stat_1: cursor.u8(),
            stat_2: cursor.u8(),
            amount_1: cursor.i16(),
            amount_2: cursor.i16(),
        });
    }

    return stat_boosts;
}

function parse_weapons(cursor: BufferCursor, offset: number, size: number): PmtWeapon[] {
    cursor.seek_start(offset);
    const weapons: PmtWeapon[] = [];

    for (let i = 0; i < size; i++) {
        weapons.push({
            id: cursor.u32(),
            type: cursor.i16(),
            skin: cursor.i16(),
            team_points: cursor.i32(),
            class: cursor.u8(),
            reserved_1: cursor.u8(),
            min_atp: cursor.i16(),
            max_atp: cursor.i16(),
            req_atp: cursor.i16(),
            req_mst: cursor.i16(),
            req_ata: cursor.i16(),
            mst: cursor.i16(),
            max_grind: cursor.u8(),
            photon: cursor.i8(),
            special: cursor.u8(),
            ata: cursor.u8(),
            stat_boost: cursor.u8(),
            projectile: cursor.u8(),
            photon_trail_1_x: cursor.i8(),
            photon_trail_1_y: cursor.i8(),
            photon_trail_2_x: cursor.i8(),
            photon_trail_2_y: cursor.i8(),
            photon_type: cursor.i8(),
            unknown_1: cursor.u8_array(5),
            tech_boost: cursor.u8(),
            combo_type: cursor.u8(),
        });
    }

    return weapons;
}

function parse_armors(cursor: BufferCursor, offset: number, size: number): PmtArmor[] {
    cursor.seek_start(offset);
    const armors: PmtArmor[] = [];

    for (let i = 0; i < size; i++) {
        armors.push({
            id: cursor.u32(),
            type: cursor.i16(),
            skin: cursor.i16(),
            team_points: cursor.i32(),
            dfp: cursor.i16(),
            evp: cursor.i16(),
            block_particle: cursor.u8(),
            block_effect: cursor.u8(),
            class: cursor.u8(),
            reserved_1: cursor.u8(),
            required_level: cursor.u8(),
            efr: cursor.u8(),
            eth: cursor.u8(),
            eic: cursor.u8(),
            edk: cursor.u8(),
            elt: cursor.u8(),
            dfp_range: cursor.u8(),
            evp_range: cursor.u8(),
            stat_boost: cursor.u8(),
            tech_boost: cursor.u8(),
            unknown_1: cursor.i16(),
        });
    }

    return armors;
}

function parse_shields(cursor: BufferCursor, offset: number, size: number): PmtShield[] {
    return parse_armors(cursor, offset, size);
}

function parse_units(cursor: BufferCursor, offset: number, size: number): PmtUnit[] {
    cursor.seek_start(offset);
    const units: PmtUnit[] = [];

    for (let i = 0; i < size; i++) {
        units.push({
            id: cursor.u32(),
            type: cursor.i16(),
            skin: cursor.i16(),
            team_points: cursor.i32(),
            stat: cursor.i16(),
            stat_amount: cursor.i16(),
            plus_minus: cursor.u8(),
            reserved: cursor.u8_array(3),
        });
    }

    return units;
}

function parse_tools(cursor: BufferCursor, offset: number, size: number): PmtTool[] {
    cursor.seek_start(offset);
    const tools: PmtTool[] = [];

    for (let i = 0; i < size; i++) {
        tools.push({
            id: cursor.u32(),
            type: cursor.i16(),
            skin: cursor.i16(),
            team_points: cursor.i32(),
            amount: cursor.i16(),
            tech: cursor.i16(),
            cost: cursor.i32(),
            item_flag: cursor.u8(),
            reserved: cursor.u8_array(3),
        });
    }

    return tools;
}
