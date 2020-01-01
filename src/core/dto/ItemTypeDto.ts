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
    min_atp: number;
    max_atp: number;
    ata: number;
    max_grind: number;
    required_atp: number;
};

export type ArmorItemTypeDto = {
    class: "armor";
    id: number;
    name: string;
    atp: number;
    ata: number;
    min_evp: number;
    max_evp: number;
    min_dfp: number;
    max_dfp: number;
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
    min_evp: number;
    max_evp: number;
    min_dfp: number;
    max_dfp: number;
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
