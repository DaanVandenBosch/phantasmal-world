export class NpcType {
    id: number;
    code: string;
    /**
     * Unique name. E.g. a Delsaber would have (Ep. II) appended to its name.
     */
    name: string;
    /**
     * Name used in the game.
     * Might conflict with other NPC names (e.g. Delsaber from ep. I and ep. II).
     */
    simpleName: string;
    ultimateName: string;
    episode?: number;
    enemy: boolean;

    constructor(
        id: number,
        code: string,
        name: string,
        simpleName: string,
        ultimateName: string,
        episode: number | undefined,
        enemy: boolean
    ) {
        if (!Number.isInteger(id) || id < 1)
            throw new Error(`Expected id to be an integer greater than or equal to 1, got ${id}.`);
        if (!code) throw new Error('code is required.');
        if (!name) throw new Error('name is required.');
        if (!simpleName) throw new Error('simpleName is required.');
        if (!ultimateName) throw new Error('ultimateName is required.');
        if (episode != null && episode !== 1 && episode !== 2 && episode !== 4)
            throw new Error(`episode should be undefined, 1, 2 or 4, got ${episode}.`);
        if (typeof enemy !== 'boolean') throw new Error('enemy is required.');

        this.id = id;
        this.code = code;
        this.simpleName = simpleName;
        this.ultimateName = ultimateName;
        this.name = name;
        this.episode = episode;
        this.enemy = enemy;

        if (episode) {
            const map = NpcType.byEpAndName[episode];

            if (map) {
                map.set(simpleName, this);
                map.set(ultimateName, this);
            }
        }
    }

    private static byEpAndName = [
        undefined, new Map<string, NpcType>(), new Map<string, NpcType>(), undefined, new Map<string, NpcType>()
    ];

    /**
     * Uniquely identifies an NPC. Tries to match on simpleName and ultimateName.
     */
    static byNameAndEpisode(name: string, episode: number): NpcType | undefined {
        const ep = this.byEpAndName[episode];
        if (!ep) throw new Error(`No NpcTypes for episode ${episode}.`);

        return ep.get(name);
    }

    //
    // Unknown NPCs
    //

    static Unknown: NpcType;

    //
    // Friendly NPCs
    //

    static FemaleFat: NpcType;
    static FemaleMacho: NpcType;
    static FemaleTall: NpcType;
    static MaleDwarf: NpcType;
    static MaleFat: NpcType;
    static MaleMacho: NpcType;
    static MaleOld: NpcType;
    static BlueSoldier: NpcType;
    static RedSoldier: NpcType;
    static Principal: NpcType;
    static Tekker: NpcType;
    static GuildLady: NpcType;
    static Scientist: NpcType;
    static Nurse: NpcType;
    static Irene: NpcType;
    static ItemShop: NpcType;
    static Nurse2: NpcType;

    //
    // Enemy NPCs
    //

    // Episode I Forest

    static Hildebear: NpcType;
    static Hildeblue: NpcType;
    static RagRappy: NpcType;
    static AlRappy: NpcType;
    static Monest: NpcType;
    static Mothmant: NpcType;
    static SavageWolf: NpcType;
    static BarbarousWolf: NpcType;
    static Booma: NpcType;
    static Gobooma: NpcType;
    static Gigobooma: NpcType;
    static Dragon: NpcType;

    // Episode I Caves

    static GrassAssassin: NpcType;
    static PoisonLily: NpcType;
    static NarLily: NpcType;
    static NanoDragon: NpcType;
    static EvilShark: NpcType;
    static PalShark: NpcType;
    static GuilShark: NpcType;
    static PofuillySlime: NpcType;
    static PouillySlime: NpcType;
    static PanArms: NpcType;
    static Migium: NpcType;
    static Hidoom: NpcType;
    static DeRolLe: NpcType;

    // Episode I Mines

    static Dubchic: NpcType;
    static Gilchic: NpcType;
    static Garanz: NpcType;
    static SinowBeat: NpcType;
    static SinowGold: NpcType;
    static Canadine: NpcType;
    static Canane: NpcType;
    static Dubswitch: NpcType;
    static VolOpt: NpcType;

    // Episode I Ruins

    static Delsaber: NpcType;
    static ChaosSorcerer: NpcType;
    static DarkGunner: NpcType;
    static DeathGunner: NpcType;
    static ChaosBringer: NpcType;
    static DarkBelra: NpcType;
    static Dimenian: NpcType;
    static LaDimenian: NpcType;
    static SoDimenian: NpcType;
    static Bulclaw: NpcType;
    static Bulk: NpcType;
    static Claw: NpcType;
    static DarkFalz: NpcType;

    // Episode II VR Temple

    static Hildebear2: NpcType;
    static Hildeblue2: NpcType;
    static RagRappy2: NpcType;
    static LoveRappy: NpcType;
    static StRappy: NpcType;
    static HalloRappy: NpcType;
    static EggRappy: NpcType;
    static Monest2: NpcType;
    static Mothmant2: NpcType;
    static PoisonLily2: NpcType;
    static NarLily2: NpcType;
    static GrassAssassin2: NpcType;
    static Dimenian2: NpcType;
    static LaDimenian2: NpcType;
    static SoDimenian2: NpcType;
    static DarkBelra2: NpcType;
    static BarbaRay: NpcType;

    // Episode II VR Spaceship

    static SavageWolf2: NpcType;
    static BarbarousWolf2: NpcType;
    static PanArms2: NpcType;
    static Migium2: NpcType;
    static Hidoom2: NpcType;
    static Dubchic2: NpcType;
    static Gilchic2: NpcType;
    static Garanz2: NpcType;
    static Dubswitch2: NpcType;
    static Delsaber2: NpcType;
    static ChaosSorcerer2: NpcType;
    static GolDragon: NpcType;

    // Episode II Central Control Area

    static SinowBerill: NpcType;
    static SinowSpigell: NpcType;
    static Merillia: NpcType;
    static Meriltas: NpcType;
    static Mericarol: NpcType;
    static Mericus: NpcType;
    static Merikle: NpcType;
    static UlGibbon: NpcType;
    static ZolGibbon: NpcType;
    static Gibbles: NpcType;
    static Gee: NpcType;
    static GiGue: NpcType;
    static GalGryphon: NpcType;

    // Episode II Seabed

    static Deldepth: NpcType;
    static Delbiter: NpcType;
    static Dolmolm: NpcType;
    static Dolmdarl: NpcType;
    static Morfos: NpcType;
    static Recobox: NpcType;
    static Recon: NpcType;
    static Epsilon: NpcType;
    static SinowZoa: NpcType;
    static SinowZele: NpcType;
    static IllGill: NpcType;
    static DelLily: NpcType;
    static OlgaFlow: NpcType;

    // Episode IV

    static SandRappy: NpcType;
    static DelRappy: NpcType;
    static Astark: NpcType;
    static SatelliteLizard: NpcType;
    static Yowie: NpcType;
    static MerissaA: NpcType;
    static MerissaAA: NpcType;
    static Girtablulu: NpcType;
    static Zu: NpcType;
    static Pazuzu: NpcType;
    static Boota: NpcType;
    static ZeBoota: NpcType;
    static BaBoota: NpcType;
    static Dorphon: NpcType;
    static DorphonEclair: NpcType;
    static Goran: NpcType;
    static PyroGoran: NpcType;
    static GoranDetonator: NpcType;
    static SaintMillion: NpcType;
    static Shambertin: NpcType;
    static Kondrieu: NpcType;
}

(function () {
    let id = 1;

    //
    // Unknown NPCs
    //

    NpcType.Unknown = new NpcType(id++, 'Unknown', 'Unknown', 'Unknown', 'Unknown', undefined, false);

    //
    // Friendly NPCs
    //

    NpcType.FemaleFat = new NpcType(id++, 'FemaleFat', 'Female Fat', 'Female Fat', 'Female Fat', undefined, false);
    NpcType.FemaleMacho = new NpcType(id++, 'FemaleMacho', 'Female Macho', 'Female Macho', 'Female Macho', undefined, false);
    NpcType.FemaleTall = new NpcType(id++, 'FemaleTall', 'Female Tall', 'Female Tall', 'Female Tall', undefined, false);
    NpcType.MaleDwarf = new NpcType(id++, 'MaleDwarf', 'Male Dwarf', 'Male Dwarf', 'Male Dwarf', undefined, false);
    NpcType.MaleFat = new NpcType(id++, 'MaleFat', 'Male Fat', 'Male Fat', 'Male Fat', undefined, false);
    NpcType.MaleMacho = new NpcType(id++, 'MaleMacho', 'Male Macho', 'Male Macho', 'Male Macho', undefined, false);
    NpcType.MaleOld = new NpcType(id++, 'MaleOld', 'Male Old', 'Male Old', 'Male Old', undefined, false);
    NpcType.BlueSoldier = new NpcType(id++, 'BlueSoldier', 'Blue Soldier', 'Blue Soldier', 'Blue Soldier', undefined, false);
    NpcType.RedSoldier = new NpcType(id++, 'RedSoldier', 'Red Soldier', 'Red Soldier', 'Red Soldier', undefined, false);
    NpcType.Principal = new NpcType(id++, 'Principal', 'Principal', 'Principal', 'Principal', undefined, false);
    NpcType.Tekker = new NpcType(id++, 'Tekker', 'Tekker', 'Tekker', 'Tekker', undefined, false);
    NpcType.GuildLady = new NpcType(id++, 'GuildLady', 'Guild Lady', 'Guild Lady', 'Guild Lady', undefined, false);
    NpcType.Scientist = new NpcType(id++, 'Scientist', 'Scientist', 'Scientist', 'Scientist', undefined, false);
    NpcType.Nurse = new NpcType(id++, 'Nurse', 'Nurse', 'Nurse', 'Nurse', undefined, false);
    NpcType.Irene = new NpcType(id++, 'Irene', 'Irene', 'Irene', 'Irene', undefined, false);
    NpcType.ItemShop = new NpcType(id++, 'ItemShop', 'Item Shop', 'Item Shop', 'Item Shop', undefined, false);
    NpcType.Nurse2 = new NpcType(id++, 'Nurse2', 'Nurse (Ep. II)', 'Nurse', 'Nurse', 2, false);

    //
    // Enemy NPCs
    //

    // Episode I Forest

    NpcType.Hildebear = new NpcType(id++, 'Hildebear', 'Hildebear', 'Hildebear', 'Hildelt', 1, true);
    NpcType.Hildeblue = new NpcType(id++, 'Hildeblue', 'Hildeblue', 'Hildeblue', 'Hildetorr', 1, true);
    NpcType.RagRappy = new NpcType(id++, 'RagRappy', 'Rag Rappy', 'Rag Rappy', 'El Rappy', 1, true);
    NpcType.AlRappy = new NpcType(id++, 'AlRappy', 'Al Rappy', 'Al Rappy', 'Pal Rappy', 1, true);
    NpcType.Monest = new NpcType(id++, 'Monest', 'Monest', 'Monest', 'Mothvist', 1, true);
    NpcType.Mothmant = new NpcType(id++, 'Mothmant', 'Mothmant', 'Mothmant', 'Mothvert', 1, true);
    NpcType.SavageWolf = new NpcType(id++, 'SavageWolf', 'Savage Wolf', 'Savage Wolf', 'Gulgus', 1, true);
    NpcType.BarbarousWolf = new NpcType(id++, 'BarbarousWolf', 'Barbarous Wolf', 'Barbarous Wolf', 'Gulgus-Gue', 1, true);
    NpcType.Booma = new NpcType(id++, 'Booma', 'Booma', 'Booma', 'Bartle', 1, true);
    NpcType.Gobooma = new NpcType(id++, 'Gobooma', 'Gobooma', 'Gobooma', 'Barble', 1, true);
    NpcType.Gigobooma = new NpcType(id++, 'Gigobooma', 'Gigobooma', 'Gigobooma', 'Tollaw', 1, true);
    NpcType.Dragon = new NpcType(id++, 'Dragon', 'Dragon', 'Dragon', 'Sil Dragon', 1, true);

    // Episode I Caves

    NpcType.GrassAssassin = new NpcType(id++, 'GrassAssassin', 'Grass Assassin', 'Grass Assassin', 'Crimson Assassin', 1, true);
    NpcType.PoisonLily = new NpcType(id++, 'PoisonLily', 'Poison Lily', 'Poison Lily', 'Ob Lily', 1, true);
    NpcType.NarLily = new NpcType(id++, 'NarLily', 'Nar Lily', 'Nar Lily', 'Mil Lily', 1, true);
    NpcType.NanoDragon = new NpcType(id++, 'NanoDragon', 'Nano Dragon', 'Nano Dragon', 'Nano Dragon', 1, true);
    NpcType.EvilShark = new NpcType(id++, 'EvilShark', 'Evil Shark', 'Evil Shark', 'Vulmer', 1, true);
    NpcType.PalShark = new NpcType(id++, 'PalShark', 'Pal Shark', 'Pal Shark', 'Govulmer', 1, true);
    NpcType.GuilShark = new NpcType(id++, 'GuilShark', 'Guil Shark', 'Guil Shark', 'Melqueek', 1, true);
    NpcType.PofuillySlime = new NpcType(id++, 'PofuillySlime', 'Pofuilly Slime', 'Pofuilly Slime', 'Pofuilly Slime', 1, true);
    NpcType.PouillySlime = new NpcType(id++, 'PouillySlime', 'Pouilly Slime', 'Pouilly Slime', 'Pouilly Slime', 1, true);
    NpcType.PanArms = new NpcType(id++, 'PanArms', 'Pan Arms', 'Pan Arms', 'Pan Arms', 1, true);
    NpcType.Migium = new NpcType(id++, 'Migium', 'Migium', 'Migium', 'Migium', 1, true);
    NpcType.Hidoom = new NpcType(id++, 'Hidoom', 'Hidoom', 'Hidoom', 'Hidoom', 1, true);
    NpcType.DeRolLe = new NpcType(id++, 'DeRolLe', 'De Rol Le', 'De Rol Le', 'Dal Ra Lie', 1, true);

    // Episode I Mines

    NpcType.Dubchic = new NpcType(id++, 'Dubchic', 'Dubchic', 'Dubchic', 'Dubchich', 1, true);
    NpcType.Gilchic = new NpcType(id++, 'Gilchic', 'Gilchic', 'Gilchic', 'Gilchich', 1, true);
    NpcType.Garanz = new NpcType(id++, 'Garanz', 'Garanz', 'Garanz', 'Baranz', 1, true);
    NpcType.SinowBeat = new NpcType(id++, 'SinowBeat', 'Sinow Beat', 'Sinow Beat', 'Sinow Blue', 1, true);
    NpcType.SinowGold = new NpcType(id++, 'SinowGold', 'Sinow Gold', 'Sinow Gold', 'Sinow Red', 1, true);
    NpcType.Canadine = new NpcType(id++, 'Canadine', 'Canadine', 'Canadine', 'Canabin', 1, true);
    NpcType.Canane = new NpcType(id++, 'Canane', 'Canane', 'Canane', 'Canune', 1, true);
    NpcType.Dubswitch = new NpcType(id++, 'Dubswitch', 'Dubswitch', 'Dubswitch', 'Dubswitch', 1, true);
    NpcType.VolOpt = new NpcType(id++, 'VolOpt', 'Vol Opt', 'Vol Opt', 'Vol Opt ver.2', 1, true);

    // Episode I Ruins

    NpcType.Delsaber = new NpcType(id++, 'Delsaber', 'Delsaber', 'Delsaber', 'Delsaber', 1, true);
    NpcType.ChaosSorcerer = new NpcType(id++, 'ChaosSorcerer', 'Chaos Sorcerer', 'Chaos Sorcerer', 'Gran Sorcerer', 1, true);
    NpcType.DarkGunner = new NpcType(id++, 'DarkGunner', 'Dark Gunner', 'Dark Gunner', 'Dark Gunner', 1, true);
    NpcType.DeathGunner = new NpcType(id++, 'DeathGunner', 'Death Gunner', 'Death Gunner', 'Death Gunner', 1, true);
    NpcType.ChaosBringer = new NpcType(id++, 'ChaosBringer', 'Chaos Bringer', 'Chaos Bringer', 'Dark Bringer', 1, true);
    NpcType.DarkBelra = new NpcType(id++, 'DarkBelra', 'Dark Belra', 'Dark Belra', 'Indi Belra', 1, true);
    NpcType.Dimenian = new NpcType(id++, 'Dimenian', 'Dimenian', 'Dimenian', 'Arlan', 1, true);
    NpcType.LaDimenian = new NpcType(id++, 'LaDimenian', 'La Dimenian', 'La Dimenian', 'Merlan', 1, true);
    NpcType.SoDimenian = new NpcType(id++, 'SoDimenian', 'So Dimenian', 'So Dimenian', 'Del-D', 1, true);
    NpcType.Bulclaw = new NpcType(id++, 'Bulclaw', 'Bulclaw', 'Bulclaw', 'Bulclaw', 1, true);
    NpcType.Bulk = new NpcType(id++, 'Bulk', 'Bulk', 'Bulk', 'Bulk', 1, true);
    NpcType.Claw = new NpcType(id++, 'Claw', 'Claw', 'Claw', 'Claw', 1, true);
    NpcType.DarkFalz = new NpcType(id++, 'DarkFalz', 'Dark Falz', 'Dark Falz', 'Dark Falz', 1, true);

    // Episode II VR Temple

    NpcType.Hildebear2 = new NpcType(id++, 'Hildebear2', 'Hildebear (Ep. II)', 'Hildebear', 'Hildelt', 2, true);
    NpcType.Hildeblue2 = new NpcType(id++, 'Hildeblue2', 'Hildeblue (Ep. II)', 'Hildeblue', 'Hildetorr', 2, true);
    NpcType.RagRappy2 = new NpcType(id++, 'RagRappy2', 'Rag Rappy (Ep. II)', 'Rag Rappy', 'El Rappy', 2, true);
    NpcType.LoveRappy = new NpcType(id++, 'LoveRappy', 'Love Rappy', 'Love Rappy', 'Love Rappy', 2, true);
    NpcType.StRappy = new NpcType(id++, 'StRappy', 'St. Rappy', 'St. Rappy', 'St. Rappy', 2, true);
    NpcType.HalloRappy = new NpcType(id++, 'HalloRappy', 'Hallo Rappy', 'Hallo Rappy', 'Hallo Rappy', 2, true);
    NpcType.EggRappy = new NpcType(id++, 'EggRappy', 'Egg Rappy', 'Egg Rappy', 'Egg Rappy', 2, true);
    NpcType.Monest2 = new NpcType(id++, 'Monest2', 'Monest (Ep. II)', 'Monest', 'Mothvist', 2, true);
    NpcType.Mothmant2 = new NpcType(id++, 'Mothmant2', 'Mothmant', 'Mothmant', 'Mothvert', 2, true);
    NpcType.PoisonLily2 = new NpcType(id++, 'PoisonLily2', 'Poison Lily (Ep. II)', 'Poison Lily', 'Ob Lily', 2, true);
    NpcType.NarLily2 = new NpcType(id++, 'NarLily2', 'Nar Lily (Ep. II)', 'Nar Lily', 'Mil Lily', 2, true);
    NpcType.GrassAssassin2 = new NpcType(id++, 'GrassAssassin2', 'Grass Assassin (Ep. II)', 'Grass Assassin', 'Crimson Assassin', 2, true);
    NpcType.Dimenian2 = new NpcType(id++, 'Dimenian2', 'Dimenian (Ep. II)', 'Dimenian', 'Arlan', 2, true);
    NpcType.LaDimenian2 = new NpcType(id++, 'LaDimenian2', 'La Dimenian (Ep. II)', 'La Dimenian', 'Merlan', 2, true);
    NpcType.SoDimenian2 = new NpcType(id++, 'SoDimenian2', 'So Dimenian (Ep. II)', 'So Dimenian', 'Del-D', 2, true);
    NpcType.DarkBelra2 = new NpcType(id++, 'DarkBelra2', 'Dark Belra (Ep. II)', 'Dark Belra', 'Indi Belra', 2, true);
    NpcType.BarbaRay = new NpcType(id++, 'BarbaRay', 'Barba Ray', 'Barba Ray', 'Barba Ray', 2, true);

    // Episode II VR Spaceship

    NpcType.SavageWolf2 = new NpcType(id++, 'SavageWolf2', 'Savage Wolf (Ep. II)', 'Savage Wolf', 'Gulgus', 2, true);
    NpcType.BarbarousWolf2 = new NpcType(id++, 'BarbarousWolf2', 'Barbarous Wolf (Ep. II)', 'Barbarous Wolf', 'Gulgus-Gue', 2, true);
    NpcType.PanArms2 = new NpcType(id++, 'PanArms2', 'Pan Arms (Ep. II)', 'Pan Arms', 'Pan Arms', 2, true);
    NpcType.Migium2 = new NpcType(id++, 'Migium2', 'Migium (Ep. II)', 'Migium', 'Migium', 2, true);
    NpcType.Hidoom2 = new NpcType(id++, 'Hidoom2', 'Hidoom (Ep. II)', 'Hidoom', 'Hidoom', 2, true);
    NpcType.Dubchic2 = new NpcType(id++, 'Dubchic2', 'Dubchic (Ep. II)', 'Dubchic', 'Dubchich', 2, true);
    NpcType.Gilchic2 = new NpcType(id++, 'Gilchic2', 'Gilchic (Ep. II)', 'Gilchic', 'Gilchich', 2, true);
    NpcType.Garanz2 = new NpcType(id++, 'Garanz2', 'Garanz (Ep. II)', 'Garanz', 'Baranz', 2, true);
    NpcType.Dubswitch2 = new NpcType(id++, 'Dubswitch2', 'Dubswitch (Ep. II)', 'Dubswitch', 'Dubswitch', 2, true);
    NpcType.Delsaber2 = new NpcType(id++, 'Delsaber2', 'Delsaber (Ep. II)', 'Delsaber', 'Delsaber', 2, true);
    NpcType.ChaosSorcerer2 = new NpcType(id++, 'ChaosSorcerer2', 'Chaos Sorcerer (Ep. II)', 'Chaos Sorcerer', 'Gran Sorcerer', 2, true);
    NpcType.GolDragon = new NpcType(id++, 'GolDragon', 'Gol Dragon', 'Gol Dragon', 'Gol Dragon', 2, true);

    // Episode II Central Control Area

    NpcType.SinowBerill = new NpcType(id++, 'SinowBerill', 'Sinow Berill', 'Sinow Berill', 'Sinow Berill', 2, true);
    NpcType.SinowSpigell = new NpcType(id++, 'SinowSpigell', 'Sinow Spigell', 'Sinow Spigell', 'Sinow Spigell', 2, true);
    NpcType.Merillia = new NpcType(id++, 'Merillia', 'Merillia', 'Merillia', 'Merillia', 2, true);
    NpcType.Meriltas = new NpcType(id++, 'Meriltas', 'Meriltas', 'Meriltas', 'Meriltas', 2, true);
    NpcType.Mericarol = new NpcType(id++, 'Mericarol', 'Mericarol', 'Mericarol', 'Mericarol', 2, true);
    NpcType.Mericus = new NpcType(id++, 'Mericus', 'Mericus', 'Mericus', 'Mericus', 2, true);
    NpcType.Merikle = new NpcType(id++, 'Merikle', 'Merikle', 'Merikle', 'Merikle', 2, true);
    NpcType.UlGibbon = new NpcType(id++, 'UlGibbon', 'Ul Gibbon', 'Ul Gibbon', 'Ul Gibbon', 2, true);
    NpcType.ZolGibbon = new NpcType(id++, 'ZolGibbon', 'Zol Gibbon', 'Zol Gibbon', 'Zol Gibbon', 2, true);
    NpcType.Gibbles = new NpcType(id++, 'Gibbles', 'Gibbles', 'Gibbles', 'Gibbles', 2, true);
    NpcType.Gee = new NpcType(id++, 'Gee', 'Gee', 'Gee', 'Gee', 2, true);
    NpcType.GiGue = new NpcType(id++, 'GiGue', 'Gi Gue', 'Gi Gue', 'Gi Gue', 2, true);
    NpcType.GalGryphon = new NpcType(id++, 'GalGryphon', 'Gal Gryphon', 'Gal Gryphon', 'Gal Gryphon', 2, true);

    // Episode II Seabed

    NpcType.Deldepth = new NpcType(id++, 'Deldepth', 'Deldepth', 'Deldepth', 'Deldepth', 2, true);
    NpcType.Delbiter = new NpcType(id++, 'Delbiter', 'Delbiter', 'Delbiter', 'Delbiter', 2, true);
    NpcType.Dolmolm = new NpcType(id++, 'Dolmolm', 'Dolmolm', 'Dolmolm', 'Dolmolm', 2, true);
    NpcType.Dolmdarl = new NpcType(id++, 'Dolmdarl', 'Dolmdarl', 'Dolmdarl', 'Dolmdarl', 2, true);
    NpcType.Morfos = new NpcType(id++, 'Morfos', 'Morfos', 'Morfos', 'Morfos', 2, true);
    NpcType.Recobox = new NpcType(id++, 'Recobox', 'Recobox', 'Recobox', 'Recobox', 2, true);
    NpcType.Recon = new NpcType(id++, 'Recon', 'Recon', 'Recon', 'Recon', 2, true);
    NpcType.Epsilon = new NpcType(id++, 'Epsilon', 'Epsilon', 'Epsilon', 'Epsilon', 2, true);
    NpcType.SinowZoa = new NpcType(id++, 'SinowZoa', 'Sinow Zoa', 'Sinow Zoa', 'Sinow Zoa', 2, true);
    NpcType.SinowZele = new NpcType(id++, 'SinowZele', 'Sinow Zele', 'Sinow Zele', 'Sinow Zele', 2, true);
    NpcType.IllGill = new NpcType(id++, 'IllGill', 'Ill Gill', 'Ill Gill', 'Ill Gill', 2, true);
    NpcType.DelLily = new NpcType(id++, 'DelLily', 'Del Lily', 'Del Lily', 'Del Lily', 2, true);
    NpcType.OlgaFlow = new NpcType(id++, 'OlgaFlow', 'Olga Flow', 'Olga Flow', 'Olga Flow', 2, true);

    // Episode IV

    NpcType.SandRappy = new NpcType(id++, 'SandRappy', 'Sand Rappy', 'Sand Rappy', 'Sand Rappy', 4, true);
    NpcType.DelRappy = new NpcType(id++, 'DelRappy', 'Del Rappy', 'Del Rappy', 'Del Rappy', 4, true);
    NpcType.Astark = new NpcType(id++, 'Astark', 'Astark', 'Astark', 'Astark', 4, true);
    NpcType.SatelliteLizard = new NpcType(id++, 'SatelliteLizard', 'Satellite Lizard', 'Satellite Lizard', 'Satellite Lizard', 4, true);
    NpcType.Yowie = new NpcType(id++, 'Yowie', 'Yowie', 'Yowie', 'Yowie', 4, true);
    NpcType.MerissaA = new NpcType(id++, 'MerissaA', 'Merissa A', 'Merissa A', 'Merissa A', 4, true);
    NpcType.MerissaAA = new NpcType(id++, 'MerissaAA', 'Merissa AA', 'Merissa AA', 'Merissa AA', 4, true);
    NpcType.Girtablulu = new NpcType(id++, 'Girtablulu', 'Girtablulu', 'Girtablulu', 'Girtablulu', 4, true);
    NpcType.Zu = new NpcType(id++, 'Zu', 'Zu', 'Zu', 'Zu', 4, true);
    NpcType.Pazuzu = new NpcType(id++, 'Pazuzu', 'Pazuzu', 'Pazuzu', 'Pazuzu', 4, true);
    NpcType.Boota = new NpcType(id++, 'Boota', 'Boota', 'Boota', 'Boota', 4, true);
    NpcType.ZeBoota = new NpcType(id++, 'ZeBoota', 'Ze Boota', 'Ze Boota', 'Ze Boota', 4, true);
    NpcType.BaBoota = new NpcType(id++, 'BaBoota', 'Ba Boota', 'Ba Boota', 'Ba Boota', 4, true);
    NpcType.Dorphon = new NpcType(id++, 'Dorphon', 'Dorphon', 'Dorphon', 'Dorphon', 4, true);
    NpcType.DorphonEclair = new NpcType(id++, 'DorphonEclair', 'Dorphon Eclair', 'Dorphon Eclair', 'Dorphon Eclair', 4, true);
    NpcType.Goran = new NpcType(id++, 'Goran', 'Goran', 'Goran', 'Goran', 4, true);
    NpcType.PyroGoran = new NpcType(id++, 'PyroGoran', 'Pyro Goran', 'Pyro Goran', 'Pyro Goran', 4, true);
    NpcType.GoranDetonator = new NpcType(id++, 'GoranDetonator', 'Goran Detonator', 'Goran Detonator', 'Goran Detonator', 4, true);
    NpcType.SaintMillion = new NpcType(id++, 'SaintMillion', 'Saint-Million', 'Saint-Million', 'Saint-Million', 4, true);
    NpcType.Shambertin = new NpcType(id++, 'Shambertin', 'Shambertin', 'Shambertin', 'Shambertin', 4, true);
    NpcType.Kondrieu = new NpcType(id++, 'Kondrieu', 'Kondrieu', 'Kondrieu', 'Kondrieu', 4, true);
}());
