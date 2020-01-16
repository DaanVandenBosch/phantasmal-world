import { Version } from "./Version";

export enum BinFormat {
    /**
     * Dreamcast/GameCube
     */
    DC_GC,
    /**
     * Desktop
     */
    PC,
    /**
     * BlueBurst
     */
    BB,
}

export function version_to_bin_format(version: Version): BinFormat {
    switch (version) {
        case Version.DC:
        case Version.GC:
            return BinFormat.DC_GC;
        case Version.PC:
            return BinFormat.PC;
        case Version.BB:
            return BinFormat.BB;
    }
}
