export class NpcType {
    id: number;
    code: string;
    name: string;
    enemy: boolean;

    constructor(id: number, code: string, name: string, enemy: boolean) {
        if (!Number.isInteger(id) || id < 1)
            throw new Error(`Expected id to be an integer greater than or equal to 1, got ${id}.`);
        if (!code) throw new Error('code is required.');
        if (!name) throw new Error('name is required.');
        if (typeof enemy !== 'boolean') throw new Error('enemy is required.');

        this.id = id;
        this.code = code;
        this.name = name;
        this.enemy = enemy;
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
    static ChaosBringer: NpcType;
    static DarkBelra: NpcType;
    static Dimenian: NpcType;
    static LaDimenian: NpcType;
    static SoDimenian: NpcType;
    static Bulclaw: NpcType;
    static Claw: NpcType;
    static DarkFalz: NpcType;

    // Episode II VR Temple

    static Hildebear2: NpcType;
    static Hildeblue2: NpcType;
    static RagRappy2: NpcType;
    static LoveRappy: NpcType;
    static Monest2: NpcType;
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

    NpcType.Unknown = new NpcType(id++, 'Unknown', 'Unknown', false);

    //
    // Friendly NPCs
    //

    NpcType.FemaleFat = new NpcType(id++, 'FemaleFat', 'Female Fat', false);
    NpcType.FemaleMacho = new NpcType(id++, 'FemaleMacho', 'Female Macho', false);
    NpcType.FemaleTall = new NpcType(id++, 'FemaleTall', 'Female Tall', false);
    NpcType.MaleDwarf = new NpcType(id++, 'MaleDwarf', 'Male Dwarf', false);
    NpcType.MaleFat = new NpcType(id++, 'MaleFat', 'Male Fat', false);
    NpcType.MaleMacho = new NpcType(id++, 'MaleMacho', 'Male Macho', false);
    NpcType.MaleOld = new NpcType(id++, 'MaleOld', 'Male Old', false);
    NpcType.BlueSoldier = new NpcType(id++, 'BlueSoldier', 'Blue Soldier', false);
    NpcType.RedSoldier = new NpcType(id++, 'RedSoldier', 'Red Soldier', false);
    NpcType.Principal = new NpcType(id++, 'Principal', 'Principal', false);
    NpcType.Tekker = new NpcType(id++, 'Tekker', 'Tekker', false);
    NpcType.GuildLady = new NpcType(id++, 'GuildLady', 'Guild Lady', false);
    NpcType.Scientist = new NpcType(id++, 'Scientist', 'Scientist', false);
    NpcType.Nurse = new NpcType(id++, 'Nurse', 'Nurse', false);
    NpcType.Irene = new NpcType(id++, 'Irene', 'Irene', false);
    NpcType.ItemShop = new NpcType(id++, 'ItemShop', 'Item Shop', false);
    NpcType.Nurse2 = new NpcType(id++, 'Nurse2', 'Nurse (Episode II)', false);

    //
    // Enemy NPCs
    //

    // Episode I Forest

    NpcType.Hildebear = new NpcType(id++, 'Hildebear', 'Hildebear', true);
    NpcType.Hildeblue = new NpcType(id++, 'Hildeblue', 'Hildeblue', true);
    NpcType.RagRappy = new NpcType(id++, 'RagRappy', 'Rag Rappy', true);
    NpcType.AlRappy = new NpcType(id++, 'AlRappy', 'Al Rappy', true);
    NpcType.Monest = new NpcType(id++, 'Monest', 'Monest', true);
    NpcType.SavageWolf = new NpcType(id++, 'SavageWolf', 'Savage Wolf', true);
    NpcType.BarbarousWolf = new NpcType(id++, 'BarbarousWolf', 'Barbarous Wolf', true);
    NpcType.Booma = new NpcType(id++, 'Booma', 'Booma', true);
    NpcType.Gobooma = new NpcType(id++, 'Gobooma', 'Gobooma', true);
    NpcType.Gigobooma = new NpcType(id++, 'Gigobooma', 'Gigobooma', true);
    NpcType.Dragon = new NpcType(id++, 'Dragon', 'Dragon', true);

    // Episode I Caves

    NpcType.GrassAssassin = new NpcType(id++, 'GrassAssassin', 'Grass Assassin', true);
    NpcType.PoisonLily = new NpcType(id++, 'PoisonLily', 'Poison Lily', true);
    NpcType.NarLily = new NpcType(id++, 'NarLily', 'Nar Lily', true);
    NpcType.NanoDragon = new NpcType(id++, 'NanoDragon', 'Nano Dragon', true);
    NpcType.EvilShark = new NpcType(id++, 'EvilShark', 'Evil Shark', true);
    NpcType.PalShark = new NpcType(id++, 'PalShark', 'Pal Shark', true);
    NpcType.GuilShark = new NpcType(id++, 'GuilShark', 'Guil Shark', true);
    NpcType.PofuillySlime = new NpcType(id++, 'PofuillySlime', 'Pofuilly Slime', true);
    NpcType.PouillySlime = new NpcType(id++, 'PouillySlime', 'Pouilly Slime', true);
    NpcType.PanArms = new NpcType(id++, 'PanArms', 'Pan Arms', true);
    NpcType.DeRolLe = new NpcType(id++, 'DeRolLe', 'De Rol Le', true);

    // Episode I Mines

    NpcType.Dubchic = new NpcType(id++, 'Dubchic', 'Dubchic', true);
    NpcType.Gilchic = new NpcType(id++, 'Gilchic', 'Gilchic', true);
    NpcType.Garanz = new NpcType(id++, 'Garanz', 'Garanz', true);
    NpcType.SinowBeat = new NpcType(id++, 'SinowBeat', 'Sinow Beat', true);
    NpcType.SinowGold = new NpcType(id++, 'SinowGold', 'Sinow Gold', true);
    NpcType.Canadine = new NpcType(id++, 'Canadine', 'Canadine', true);
    NpcType.Canane = new NpcType(id++, 'Canane', 'Canane', true);
    NpcType.Dubswitch = new NpcType(id++, 'Dubswitch', 'Dubswitch', true);
    NpcType.VolOpt = new NpcType(id++, 'VolOpt', 'Vol Opt', true);

    // Episode I Ruins

    NpcType.Delsaber = new NpcType(id++, 'Delsaber', 'Delsaber', true);
    NpcType.ChaosSorcerer = new NpcType(id++, 'ChaosSorcerer', 'Chaos Sorcerer', true);
    NpcType.DarkGunner = new NpcType(id++, 'DarkGunner', 'Dark Gunner', true);
    NpcType.ChaosBringer = new NpcType(id++, 'ChaosBringer', 'Chaos Bringer', true);
    NpcType.DarkBelra = new NpcType(id++, 'DarkBelra', 'Dark Belra', true);
    NpcType.Dimenian = new NpcType(id++, 'Dimenian', 'Dimenian', true);
    NpcType.LaDimenian = new NpcType(id++, 'LaDimenian', 'La Dimenian', true);
    NpcType.SoDimenian = new NpcType(id++, 'SoDimenian', 'So Dimenian', true);
    NpcType.Bulclaw = new NpcType(id++, 'Bulclaw', 'Bulclaw', true);
    NpcType.Claw = new NpcType(id++, 'Claw', 'Claw', true);
    NpcType.DarkFalz = new NpcType(id++, 'DarkFalz', 'Dark Falz', true);

    // Episode II VR Temple

    NpcType.Hildebear2 = new NpcType(id++, 'Hildebear2', 'Hildebear (Ep. II)', true);
    NpcType.Hildeblue2 = new NpcType(id++, 'Hildeblue2', 'Hildeblue (Ep. II)', true);
    NpcType.RagRappy2 = new NpcType(id++, 'RagRappy2', 'Rag Rappy (Ep. II)', true);
    NpcType.LoveRappy = new NpcType(id++, 'LoveRappy', 'Love Rappy', true);
    NpcType.Monest2 = new NpcType(id++, 'Monest2', 'Monest (Ep. II)', true);
    NpcType.PoisonLily2 = new NpcType(id++, 'PoisonLily2', 'Poison Lily (Ep. II)', true);
    NpcType.NarLily2 = new NpcType(id++, 'NarLily2', 'Nar Lily (Ep. II)', true);
    NpcType.GrassAssassin2 = new NpcType(id++, 'GrassAssassin2', 'Grass Assassin (Ep. II)', true);
    NpcType.Dimenian2 = new NpcType(id++, 'Dimenian2', 'Dimenian (Ep. II)', true);
    NpcType.LaDimenian2 = new NpcType(id++, 'LaDimenian2', 'La Dimenian (Ep. II)', true);
    NpcType.SoDimenian2 = new NpcType(id++, 'SoDimenian2', 'So Dimenian (Ep. II)', true);
    NpcType.DarkBelra2 = new NpcType(id++, 'DarkBelra2', 'Dark Belra (Ep. II)', true);
    NpcType.BarbaRay = new NpcType(id++, 'BarbaRay', 'Barba Ray', true);

    // Episode II VR Spaceship

    NpcType.SavageWolf2 = new NpcType(id++, 'SavageWolf2', 'Savage Wolf (Ep. II)', true);
    NpcType.BarbarousWolf2 = new NpcType(id++, 'BarbarousWolf2', 'Barbarous Wolf (Ep. II)', true);
    NpcType.PanArms2 = new NpcType(id++, 'PanArms2', 'Pan Arms (Ep. II)', true);
    NpcType.Dubchic2 = new NpcType(id++, 'Dubchic2', 'Dubchic (Ep. II)', true);
    NpcType.Gilchic2 = new NpcType(id++, 'Gilchic2', 'Gilchic (Ep. II)', true);
    NpcType.Garanz2 = new NpcType(id++, 'Garanz2', 'Garanz (Ep. II)', true);
    NpcType.Dubswitch2 = new NpcType(id++, 'Dubswitch2', 'Dubswitch (Ep. II)', true);
    NpcType.Delsaber2 = new NpcType(id++, 'Delsaber2', 'Delsaber (Ep. II)', true);
    NpcType.ChaosSorcerer2 = new NpcType(id++, 'ChaosSorcerer2', 'Chaos Sorcerer (Ep. II)', true);
    NpcType.GolDragon = new NpcType(id++, 'GolDragon', 'Gol Dragon', true);

    // Episode II Central Control Area

    NpcType.SinowBerill = new NpcType(id++, 'SinowBerill', 'Sinow Berill', true);
    NpcType.SinowSpigell = new NpcType(id++, 'SinowSpigell', 'Sinow Spigell', true);
    NpcType.Merillia = new NpcType(id++, 'Merillia', 'Merillia', true);
    NpcType.Meriltas = new NpcType(id++, 'Meriltas', 'Meriltas', true);
    NpcType.Mericarol = new NpcType(id++, 'Mericarol', 'Mericarol', true);
    NpcType.Mericus = new NpcType(id++, 'Mericus', 'Mericus', true);
    NpcType.Merikle = new NpcType(id++, 'Merikle', 'Merikle', true);
    NpcType.UlGibbon = new NpcType(id++, 'UlGibbon', 'Ul Gibbon', true);
    NpcType.ZolGibbon = new NpcType(id++, 'ZolGibbon', 'Zol Gibbon', true);
    NpcType.Gibbles = new NpcType(id++, 'Gibbles', 'Gibbles', true);
    NpcType.Gee = new NpcType(id++, 'Gee', 'Gee', true);
    NpcType.GiGue = new NpcType(id++, 'GiGue', 'Gi Gue', true);
    NpcType.GalGryphon = new NpcType(id++, 'GalGryphon', 'Gal Gryphon', true);

    // Episode II Seabed

    NpcType.Deldepth = new NpcType(id++, 'Deldepth', 'Deldepth', true);
    NpcType.Delbiter = new NpcType(id++, 'Delbiter', 'Delbiter', true);
    NpcType.Dolmolm = new NpcType(id++, 'Dolmolm', 'Dolmolm', true);
    NpcType.Dolmdarl = new NpcType(id++, 'Dolmdarl', 'Dolmdarl', true);
    NpcType.Morfos = new NpcType(id++, 'Morfos', 'Morfos', true);
    NpcType.Recobox = new NpcType(id++, 'Recobox', 'Recobox', true);
    NpcType.Epsilon = new NpcType(id++, 'Epsilon', 'Epsilon', true);
    NpcType.SinowZoa = new NpcType(id++, 'SinowZoa', 'Sinow Zoa', true);
    NpcType.SinowZele = new NpcType(id++, 'SinowZele', 'Sinow Zele', true);
    NpcType.IllGill = new NpcType(id++, 'IllGill', 'Ill Gill', true);
    NpcType.DelLily = new NpcType(id++, 'DelLily', 'Del Lily', true);
    NpcType.OlgaFlow = new NpcType(id++, 'OlgaFlow', 'Olga Flow', true);

    // Episode IV

    NpcType.SandRappy = new NpcType(id++, 'SandRappy', 'Sand Rappy', true);
    NpcType.DelRappy = new NpcType(id++, 'DelRappy', 'Del Rappy', true);
    NpcType.Astark = new NpcType(id++, 'Astark', 'Astark', true);
    NpcType.SatelliteLizard = new NpcType(id++, 'SatelliteLizard', 'Satellite Lizard', true);
    NpcType.Yowie = new NpcType(id++, 'Yowie', 'Yowie', true);
    NpcType.MerissaA = new NpcType(id++, 'MerissaA', 'Merissa A', true);
    NpcType.MerissaAA = new NpcType(id++, 'MerissaAA', 'Merissa AA', true);
    NpcType.Girtablulu = new NpcType(id++, 'Girtablulu', 'Girtablulu', true);
    NpcType.Zu = new NpcType(id++, 'Zu', 'Zu', true);
    NpcType.Pazuzu = new NpcType(id++, 'Pazuzu', 'Pazuzu', true);
    NpcType.Boota = new NpcType(id++, 'Boota', 'Boota', true);
    NpcType.ZeBoota = new NpcType(id++, 'ZeBoota', 'Ze Boota', true);
    NpcType.BaBoota = new NpcType(id++, 'BaBoota', 'Ba Boota', true);
    NpcType.Dorphon = new NpcType(id++, 'Dorphon', 'Dorphon', true);
    NpcType.DorphonEclair = new NpcType(id++, 'DorphonEclair', 'Dorphon Eclair', true);
    NpcType.Goran = new NpcType(id++, 'Goran', 'Goran', true);
    NpcType.PyroGoran = new NpcType(id++, 'PyroGoran', 'Pyro Goran', true);
    NpcType.GoranDetonator = new NpcType(id++, 'GoranDetonator', 'Goran Detonator', true);
    NpcType.SaintMillion = new NpcType(id++, 'SaintMillion', 'Saint-Million', true);
    NpcType.Shambertin = new NpcType(id++, 'Shambertin', 'Shambertin', true);
    NpcType.Kondrieu = new NpcType(id++, 'Kondrieu', 'Kondrieu', true);
} ());
