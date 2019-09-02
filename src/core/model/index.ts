import { enum_values } from "../enums";

export const RARE_ENEMY_PROB = 1 / 512;
export const KONDRIEU_PROB = 1 / 10;

export enum ServerModel {
    Ephinea = "Ephinea",
}

export const ServerModels: ServerModel[] = enum_values(ServerModel);

export enum SectionIdModel {
    Viridia,
    Greenill,
    Skyly,
    Bluefull,
    Purplenum,
    Pinkal,
    Redria,
    Oran,
    Yellowboze,
    Whitill,
}

export const SectionIdModels: SectionIdModel[] = enum_values(SectionIdModel);

export enum DifficultyModel {
    Normal,
    Hard,
    VHard,
    Ultimate,
}

export const DifficultyModels: DifficultyModel[] = enum_values(DifficultyModel);
