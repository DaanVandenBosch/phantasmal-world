export type ItemTypeDto =
    | WeaponItemTypeDto
    | ArmorItemTypeDto
    | ShieldItemTypeDto
    | UnitItemTypeDto
    | ToolItemTypeDto;

export type WeaponItemTypeDto = {
    class: "weapon";
    id: number;
    name: string;
    minAtp: number;
    maxAtp: number;
    ata: number;
    maxGrind: number;
    requiredAtp: number;
};

export type ArmorItemTypeDto = {
    class: "armor";
    id: number;
    name: string;
    atp: number;
    ata: number;
    minEvp: number;
    maxEvp: number;
    minDfp: number;
    maxDfp: number;
    mst: number;
    hp: number;
    lck: number;
};

export type ShieldItemTypeDto = {
    class: "shield";
    id: number;
    name: string;
    atp: number;
    ata: number;
    minEvp: number;
    maxEvp: number;
    minDfp: number;
    maxDfp: number;
    mst: number;
    hp: number;
    lck: number;
};

export type UnitItemTypeDto = {
    class: "unit";
    id: number;
    name: string;
};

export type ToolItemTypeDto = {
    class: "tool";
    id: number;
    name: string;
};
