import { ArrayBufferCursor } from "../ArrayBufferCursor";

export type ItemPmt = {
    statBoosts: PmtStatBoost[],
    armors: PmtArmor[],
    shields: PmtShield[],
    units: PmtUnit[],
    tools: PmtTool[][],
    weapons: PmtWeapon[][],
}

export type PmtStatBoost = {
    stat1: number,
    stat2: number,
    amount1: number,
    amount2: number,
}

export type PmtWeapon = {
    id: number,
    type: number,
    skin: number,
    teamPoints: number,
    class: number,
    reserved1: number,
    minAtp: number,
    maxAtp: number,
    reqAtp: number,
    reqMst: number,
    reqAta: number,
    mst: number,
    maxGrind: number,
    photon: number,
    special: number,
    ata: number,
    statBoost: number,
    projectile: number,
    photonTrail1X: number,
    photonTrail1Y: number,
    photonTrail2X: number,
    photonTrail2Y: number,
    photonType: number,
    unknown1: number[],
    techBoost: number,
    comboType: number,
}

export type PmtArmor = {
    id: number,
    type: number,
    skin: number,
    teamPoints: number,
    dfp: number,
    evp: number,
    blockParticle: number,
    blockEffect: number,
    class: number,
    reserved1: number,
    requiredLevel: number,
    efr: number,
    eth: number,
    eic: number,
    edk: number,
    elt: number,
    dfpRange: number,
    evpRange: number,
    statBoost: number,
    techBoost: number,
    unknown1: number,
}

export type PmtShield = PmtArmor

export type PmtUnit = {
    id: number,
    type: number,
    skin: number,
    teamPoints: number,
    stat: number,
    statAmount: number,
    plusMinus: number,
    reserved: number[]
}

export type PmtTool = {
    id: number,
    type: number,
    skin: number,
    teamPoints: number,
    amount: number,
    tech: number,
    cost: number,
    itemFlag: number,
    reserved: number[],
}

export function parseItemPmt(cursor: ArrayBufferCursor): ItemPmt {
    cursor.seekEnd(32);
    const mainTableOffset = cursor.u32();
    const mainTableSize = cursor.u32();
    // const mainTableCount = cursor.u32(); // Should be 1.

    cursor.seekStart(mainTableOffset);

    const compactTableOffsets = cursor.u16Array(mainTableSize);
    const tableOffsets: { offset: number, size: number }[] = [];
    let expandedOffset: number = 0;

    for (const compactOffset of compactTableOffsets) {
        expandedOffset = expandedOffset + 4 * compactOffset;
        cursor.seekStart(expandedOffset - 4);
        const size = cursor.u32();
        const offset = cursor.u32();
        tableOffsets.push({ offset, size });
    }

    const itemPmt: ItemPmt = {
        // This size (65268) of this table seems wrong, so we pass in a hard-coded value.
        statBoosts: parseStatBoosts(cursor, tableOffsets[305].offset, 52),
        armors: parseArmors(cursor, tableOffsets[7].offset, tableOffsets[7].size),
        shields: parseShields(cursor, tableOffsets[8].offset, tableOffsets[8].size),
        units: parseUnits(cursor, tableOffsets[9].offset, tableOffsets[9].size),
        tools: [],
        weapons: [],
    };

    for (let i = 11; i <= 37; i++) {
        itemPmt.tools.push(parseTools(cursor, tableOffsets[i].offset, tableOffsets[i].size));
    }

    for (let i = 38; i <= 275; i++) {
        itemPmt.weapons.push(
            parseWeapons(cursor, tableOffsets[i].offset, tableOffsets[i].size)
        );
    }

    return itemPmt;
}

function parseStatBoosts(cursor: ArrayBufferCursor, offset: number, size: number): PmtStatBoost[] {
    cursor.seekStart(offset);
    const statBoosts: PmtStatBoost[] = [];

    for (let i = 0; i < size; i++) {
        statBoosts.push({
            stat1: cursor.u8(),
            stat2: cursor.u8(),
            amount1: cursor.i16(),
            amount2: cursor.i16(),
        });
    }

    return statBoosts;
}

function parseWeapons(cursor: ArrayBufferCursor, offset: number, size: number): PmtWeapon[] {
    cursor.seekStart(offset);
    const weapons: PmtWeapon[] = [];

    for (let i = 0; i < size; i++) {
        weapons.push({
            id: cursor.u32(),
            type: cursor.i16(),
            skin: cursor.i16(),
            teamPoints: cursor.i32(),
            class: cursor.u8(),
            reserved1: cursor.u8(),
            minAtp: cursor.i16(),
            maxAtp: cursor.i16(),
            reqAtp: cursor.i16(),
            reqMst: cursor.i16(),
            reqAta: cursor.i16(),
            mst: cursor.i16(),
            maxGrind: cursor.u8(),
            photon: cursor.i8(),
            special: cursor.u8(),
            ata: cursor.u8(),
            statBoost: cursor.u8(),
            projectile: cursor.u8(),
            photonTrail1X: cursor.i8(),
            photonTrail1Y: cursor.i8(),
            photonTrail2X: cursor.i8(),
            photonTrail2Y: cursor.i8(),
            photonType: cursor.i8(),
            unknown1: cursor.u8Array(5),
            techBoost: cursor.u8(),
            comboType: cursor.u8(),
        });
    }

    return weapons;
}

function parseArmors(cursor: ArrayBufferCursor, offset: number, size: number): PmtArmor[] {
    cursor.seekStart(offset);
    const armors: PmtArmor[] = [];

    for (let i = 0; i < size; i++) {
        armors.push({
            id: cursor.u32(),
            type: cursor.i16(),
            skin: cursor.i16(),
            teamPoints: cursor.i32(),
            dfp: cursor.i16(),
            evp: cursor.i16(),
            blockParticle: cursor.u8(),
            blockEffect: cursor.u8(),
            class: cursor.u8(),
            reserved1: cursor.u8(),
            requiredLevel: cursor.u8(),
            efr: cursor.u8(),
            eth: cursor.u8(),
            eic: cursor.u8(),
            edk: cursor.u8(),
            elt: cursor.u8(),
            dfpRange: cursor.u8(),
            evpRange: cursor.u8(),
            statBoost: cursor.u8(),
            techBoost: cursor.u8(),
            unknown1: cursor.i16(),
        });
    }

    return armors;
}

function parseShields(cursor: ArrayBufferCursor, offset: number, size: number): PmtShield[] {
    return parseArmors(cursor, offset, size);
}

function parseUnits(cursor: ArrayBufferCursor, offset: number, size: number): PmtUnit[] {
    cursor.seekStart(offset);
    const units: PmtUnit[] = [];

    for (let i = 0; i < size; i++) {
        units.push({
            id: cursor.u32(),
            type: cursor.i16(),
            skin: cursor.i16(),
            teamPoints: cursor.i32(),
            stat: cursor.i16(),
            statAmount: cursor.i16(),
            plusMinus: cursor.u8(),
            reserved: cursor.u8Array(3),
        });
    }

    return units;
}

function parseTools(cursor: ArrayBufferCursor, offset: number, size: number): PmtTool[] {
    cursor.seekStart(offset);
    const tools: PmtTool[] = [];

    for (let i = 0; i < size; i++) {
        tools.push({
            id: cursor.u32(),
            type: cursor.i16(),
            skin: cursor.i16(),
            teamPoints: cursor.i32(),
            amount: cursor.i16(),
            tech: cursor.i16(),
            cost: cursor.i32(),
            itemFlag: cursor.u8(),
            reserved: cursor.u8Array(3),
        });
    }

    return tools;
}
