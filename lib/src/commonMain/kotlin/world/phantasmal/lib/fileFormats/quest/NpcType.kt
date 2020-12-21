package world.phantasmal.lib.fileFormats.quest

import world.phantasmal.lib.Episode

private val FRIENDLY_NPC_PROPERTIES = listOf(
    EntityProp(name = "Movement distance", offset = 44, type = EntityPropType.F32),
    EntityProp(name = "Hide register", offset = 52, type = EntityPropType.F32),
    EntityProp(name = "Character ID", offset = 56, type = EntityPropType.F32),
    EntityProp(name = "Script label", offset = 60, type = EntityPropType.F32),
    EntityProp(name = "Movement flag", offset = 64, type = EntityPropType.I32)
)

enum class NpcType(
    override val uniqueName: String,
    override val simpleName: String = uniqueName,
    val ultimateName: String = simpleName,
    val episode: Episode? = null,
    val enemy: Boolean = false,
    rareType: (() -> NpcType)? = null,
    /**
     * IDs of the areas this NPC can be found in.
     */
    val areaIds: List<Int>,
    /**
     * Type ID used by the game.
     */
    val typeId: Int? = null,
    /**
     * Skin value used by the game.
     */
    val skin: Int? = null,
    /**
     * Boolean specifying whether an NPC is the regular or special variant.
     * Sometimes signifies a variant (e.g. Barbarous Wolf), sometimes a rare variant (e.g. Pouilly
     * Slime).
     */
    val special: Boolean? = null,
    /**
     * NPC-specific properties.
     */
    override val properties: List<EntityProp> = emptyList(),
) : EntityType {
    //
    // Unknown NPCs
    //

    Unknown(
        uniqueName = "Unknown",
        areaIds = listOf(),
    ),

    //
    // Friendly NPCs
    //

    FemaleFat(
        uniqueName = "Female Fat",
        areaIds = listOf(0),
        typeId = 4,
        skin = 0,
        special = false,
        properties = FRIENDLY_NPC_PROPERTIES,
    ),
    FemaleMacho(
        uniqueName = "Female Macho",
        areaIds = listOf(0),
        typeId = 5,
        skin = 0,
        special = false,
        properties = FRIENDLY_NPC_PROPERTIES,
    ),
    FemaleTall(
        uniqueName = "Female Tall",
        areaIds = listOf(0),
        typeId = 7,
        skin = 0,
        special = false,
        properties = FRIENDLY_NPC_PROPERTIES,
    ),
    MaleDwarf(
        uniqueName = "Male Dwarf",
        areaIds = listOf(0),
        typeId = 10,
        skin = 0,
        special = false,
        properties = FRIENDLY_NPC_PROPERTIES,
    ),
    MaleFat(
        uniqueName = "Male Fat",
        areaIds = listOf(0),
        typeId = 11,
        skin = 0,
        special = false,
        properties = FRIENDLY_NPC_PROPERTIES,
    ),
    MaleMacho(
        uniqueName = "Male Macho",
        areaIds = listOf(0),
        typeId = 12,
        skin = 0,
        special = false,
        properties = FRIENDLY_NPC_PROPERTIES,
    ),
    MaleOld(
        uniqueName = "Male Old",
        areaIds = listOf(0),
        typeId = 13,
        skin = 0,
        special = false,
        properties = FRIENDLY_NPC_PROPERTIES,
    ),
    BlueSoldier(
        uniqueName = "Blue Soldier",
        areaIds = listOf(0),
        typeId = 25,
        skin = 0,
        special = false,
        properties = FRIENDLY_NPC_PROPERTIES,
    ),
    RedSoldier(
        uniqueName = "Red Soldier",
        areaIds = listOf(0),
        typeId = 26,
        skin = 0,
        special = false,
        properties = FRIENDLY_NPC_PROPERTIES,
    ),
    Principal(
        uniqueName = "Principal",
        areaIds = listOf(0),
        typeId = 27,
        skin = 0,
        special = false,
        properties = FRIENDLY_NPC_PROPERTIES,
    ),
    Tekker(
        uniqueName = "Tekker",
        areaIds = listOf(0),
        typeId = 28,
        skin = 0,
        special = false,
        properties = FRIENDLY_NPC_PROPERTIES,
    ),
    GuildLady(
        uniqueName = "Guild Lady",
        areaIds = listOf(0),
        typeId = 29,
        skin = 0,
        special = false,
        properties = FRIENDLY_NPC_PROPERTIES,
    ),
    Scientist(
        uniqueName = "Scientist",
        areaIds = listOf(0),
        typeId = 30,
        skin = 0,
        special = false,
        properties = FRIENDLY_NPC_PROPERTIES,
    ),
    Nurse(
        uniqueName = "Nurse",
        areaIds = listOf(0),
        typeId = 31,
        skin = 0,
        special = false,
        properties = FRIENDLY_NPC_PROPERTIES,
    ),
    Irene(
        uniqueName = "Irene",
        areaIds = listOf(0),
        typeId = 32,
        skin = 0,
        special = false,
        properties = FRIENDLY_NPC_PROPERTIES,
    ),
    ItemShop(
        uniqueName = "Item Shop",
        areaIds = listOf(0),
        typeId = 241,
        skin = 0,
        special = false,
        properties = FRIENDLY_NPC_PROPERTIES,
    ),
    Nurse2(
        uniqueName = "Nurse (Ep. II)",
        simpleName = "Nurse",
        episode = Episode.II,
        areaIds = listOf(0),
        typeId = 254,
        skin = 0,
        special = false,
        properties = FRIENDLY_NPC_PROPERTIES,
    ),

    //
    // Enemy NPCs
    //

    // Episode I Forest

    Hildebear(
        uniqueName = "Hildebear",
        ultimateName = "Hildelt",
        episode = Episode.I,
        enemy = true,
        rareType = { Hildeblue },
        areaIds = listOf(1, 2),
        typeId = 64,
        skin = 0,
        special = false,
    ),
    Hildeblue(
        uniqueName = "Hildeblue",
        ultimateName = "Hildetorr",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(1, 2),
        typeId = 64,
        skin = 1,
        special = false,
    ),
    RagRappy(
        uniqueName = "Rag Rappy",
        ultimateName = "El Rappy",
        episode = Episode.I,
        enemy = true,
        rareType = { AlRappy },
        areaIds = listOf(1, 2),
        typeId = 65,
        skin = 0,
        special = false,
    ),
    AlRappy(
        uniqueName = "Al Rappy",
        ultimateName = "Pal Rappy",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(1, 2),
        typeId = 65,
        skin = 1,
        special = false,
    ),
    Monest(
        uniqueName = "Monest",
        ultimateName = "Mothvist",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(1, 2),
        typeId = 66,
        skin = 0,
        special = false,
        properties = listOf(
            EntityProp(name = "State", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Start number", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "Total number", offset = 52, type = EntityPropType.F32)
        ),
    ),
    Mothmant(
        uniqueName = "Mothmant",
        ultimateName = "Mothvert",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(),
    ),
    SavageWolf(
        uniqueName = "Savage Wolf",
        ultimateName = "Gulgus",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(1, 2),
        typeId = 67,
        skin = 0,
        special = false,
        properties = listOf(EntityProp(name = "Group ID", offset = 44, type = EntityPropType.F32)),
    ),
    BarbarousWolf(
        uniqueName = "Barbarous Wolf",
        ultimateName = "Gulgus-Gue",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(1, 2),
        typeId = 67,
        skin = 0,
        special = true,
        properties = listOf(EntityProp(name = "Group ID", offset = 44, type = EntityPropType.F32)),
    ),
    Booma(
        uniqueName = "Booma",
        ultimateName = "Bartle",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(1, 2),
        typeId = 68,
        skin = 0,
        special = false,
        properties = listOf(
            EntityProp(name = "Idle distance", offset = 48, type = EntityPropType.F32)
        ),
    ),
    Gobooma(
        uniqueName = "Gobooma",
        ultimateName = "Barble",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(1, 2),
        typeId = 68,
        skin = 1,
        special = false,
        properties = listOf(
            EntityProp(name = "Idle distance", offset = 48, type = EntityPropType.F32)
        ),
    ),
    Gigobooma(
        uniqueName = "Gigobooma",
        ultimateName = "Tollaw",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(1, 2),
        typeId = 68,
        skin = 2,
        special = false,
        properties = listOf(
            EntityProp(name = "Idle distance", offset = 48, type = EntityPropType.F32)
        ),
    ),
    Dragon(
        uniqueName = "Dragon",
        ultimateName = "Sil Dragon",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(11),
        typeId = 192,
        skin = 0,
        special = false,
    ),

    // Episode I Caves

    GrassAssassin(
        uniqueName = "Grass Assassin",
        ultimateName = "Crimson Assassin",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(3, 4, 5),
        typeId = 96,
        skin = 0,
        special = false,
    ),
    PoisonLily(
        uniqueName = "Poison Lily",
        ultimateName = "Ob Lily",
        episode = Episode.I,
        enemy = true,
        rareType = { NarLily },
        areaIds = listOf(3, 4, 5),
        typeId = 97,
        skin = 0,
        special = false,
    ),
    NarLily(
        uniqueName = "Nar Lily",
        ultimateName = "Mil Lily",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(3, 4, 5),
        typeId = 97,
        skin = 1,
        special = true,
    ),
    NanoDragon(
        uniqueName = "Nano Dragon",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(3, 4, 5),
        typeId = 98,
        skin = 0,
        special = false,
        properties = listOf(
            EntityProp(name = "Spawn flag", offset = 64, type = EntityPropType.I32)
        ),
    ),
    EvilShark(
        uniqueName = "Evil Shark",
        ultimateName = "Vulmer",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(3, 4, 5),
        typeId = 99,
        skin = 0,
        special = false,
        properties = listOf(
            EntityProp(name = "Idle distance", offset = 48, type = EntityPropType.F32)
        ),
    ),
    PalShark(
        uniqueName = "Pal Shark",
        ultimateName = "Govulmer",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(3, 4, 5),
        typeId = 99,
        skin = 1,
        special = false,
        properties = listOf(
            EntityProp(name = "Idle distance", offset = 48, type = EntityPropType.F32)
        ),
    ),
    GuilShark(
        uniqueName = "Guil Shark",
        ultimateName = "Melqueek",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(3, 4, 5),
        typeId = 99,
        skin = 2,
        special = false,
        properties = listOf(
            EntityProp(name = "Idle distance", offset = 48, type = EntityPropType.F32)
        ),
    ),
    PofuillySlime(
        uniqueName = "Pofuilly Slime",
        episode = Episode.I,
        enemy = true,
        rareType = { PouillySlime },
        areaIds = listOf(3, 4, 5),
        typeId = 100,
        skin = 0,
        special = false,
    ),
    PouillySlime(
        uniqueName = "Pouilly Slime",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(3, 4, 5),
        typeId = 100,
        skin = 0,
        special = true,
    ),
    PanArms(
        uniqueName = "Pan Arms",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(3, 4, 5),
        typeId = 101,
        skin = 0,
        special = false,
    ),
    Migium(
        uniqueName = "Migium",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(),
    ),
    Hidoom(
        uniqueName = "Hidoom",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(),
    ),
    DeRolLe(
        uniqueName = "De Rol Le",
        ultimateName = "Dal Ra Lie",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(12),
        typeId = 193,
        skin = 0,
        special = false,
    ),

    // Episode I Mines

    Dubchic(
        uniqueName = "Dubchic",
        ultimateName = "Dubchich",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(6, 7),
        typeId = 128,
        skin = 0,
        special = false,
    ),
    Gilchic(
        uniqueName = "Gilchic",
        ultimateName = "Gilchich",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(6, 7),
        typeId = 128,
        skin = 1,
        special = false,
    ),
    Garanz(
        uniqueName = "Garanz",
        ultimateName = "Baranz",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(6, 7),
        typeId = 129,
        skin = 0,
        special = false,
    ),
    SinowBeat(
        uniqueName = "Sinow Beat",
        ultimateName = "Sinow Blue",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(6, 7),
        typeId = 130,
        skin = 0,
        special = false,
    ),
    SinowGold(
        uniqueName = "Sinow Gold",
        ultimateName = "Sinow Red",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(6, 7),
        typeId = 130,
        skin = 0,
        special = true,
    ),
    Canadine(
        uniqueName = "Canadine",
        ultimateName = "Canabin",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(6, 7),
        typeId = 131,
        skin = 0,
        special = false,
    ),
    Canane(
        uniqueName = "Canane",
        ultimateName = "Canune",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(6, 7),
        typeId = 132,
        skin = 0,
        special = false,
    ),
    Dubswitch(
        uniqueName = "Dubswitch",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(6, 7),
        typeId = 133,
        skin = 0,
        special = false,
    ),
    VolOptPart1(
        uniqueName = "Vol Opt (Part 1)",
        simpleName = "Vol Opt",
        ultimateName = "Vol Opt ver.2",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(13),
        typeId = 194,
        skin = 0,
        special = false,
    ),
    VolOptPart2(
        uniqueName = "Vol Opt (Part 2)",
        simpleName = "Vol Opt",
        ultimateName = "Vol Opt ver.2",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(13),
        typeId = 197,
        skin = 0,
        special = false,
    ),

    // Episode I Ruins

    Delsaber(
        uniqueName = "Delsaber",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(8, 9, 10),
        typeId = 160,
        skin = 0,
        special = false,
        properties = listOf(
            EntityProp(name = "Jump distance", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Block HP", offset = 48, type = EntityPropType.F32)
        ),
    ),
    ChaosSorcerer(
        uniqueName = "Chaos Sorcerer",
        ultimateName = "Gran Sorcerer",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(8, 9, 10),
        typeId = 161,
        skin = 0,
        special = false,
    ),
    DarkGunner(
        uniqueName = "Dark Gunner",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(8, 9, 10),
        typeId = 162,
        skin = 0,
        special = false,
    ),
    DeathGunner(
        uniqueName = "Death Gunner",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(),
    ),
    ChaosBringer(
        uniqueName = "Chaos Bringer",
        ultimateName = "Dark Bringer",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(8, 9, 10),
        typeId = 164,
        skin = 0,
        special = false,
    ),
    DarkBelra(
        uniqueName = "Dark Belra",
        ultimateName = "Indi Belra",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(8, 9, 10),
        typeId = 165,
        skin = 0,
        special = false,
    ),
    Dimenian(
        uniqueName = "Dimenian",
        ultimateName = "Arlan",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(8, 9, 10),
        typeId = 166,
        skin = 0,
        special = false,
        properties = listOf(
            EntityProp(name = "Idle distance", offset = 48, type = EntityPropType.F32)
        ),
    ),
    LaDimenian(
        uniqueName = "La Dimenian",
        ultimateName = "Merlan",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(8, 9, 10),
        typeId = 166,
        skin = 1,
        special = false,
        properties = listOf(
            EntityProp(name = "Idle distance", offset = 48, type = EntityPropType.F32)
        ),
    ),
    SoDimenian(
        uniqueName = "So Dimenian",
        ultimateName = "Del-D",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(8, 9, 10),
        typeId = 166,
        skin = 2,
        special = false,
        properties = listOf(
            EntityProp(name = "Idle distance", offset = 48, type = EntityPropType.F32)
        ),
    ),
    Bulclaw(
        uniqueName = "Bulclaw",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(8, 9, 10),
        typeId = 167,
        skin = 0,
        special = false,
    ),
    Bulk(
        uniqueName = "Bulk",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(),
    ),
    Claw(
        uniqueName = "Claw",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(8, 9, 10),
        typeId = 168,
        skin = 0,
        special = false,
    ),
    DarkFalz(
        uniqueName = "Dark Falz",
        episode = Episode.I,
        enemy = true,
        areaIds = listOf(14),
        typeId = 200,
        skin = 0,
        special = false,
    ),

    // Episode II VR Temple

    Hildebear2(
        uniqueName = "Hildebear (Ep. II)",
        simpleName = "Hildebear",
        ultimateName = "Hildelt",
        episode = Episode.II,
        enemy = true,
        rareType = { Hildeblue2 },
        areaIds = listOf(1, 2),
        typeId = 64,
        skin = 0,
        special = false,
    ),
    Hildeblue2(
        uniqueName = "Hildeblue (Ep. II)",
        simpleName = "Hildeblue",
        ultimateName = "Hildetorr",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(1, 2),
        typeId = 64,
        skin = 1,
        special = false,
    ),
    RagRappy2(
        uniqueName = "Rag Rappy (Ep. II)",
        simpleName = "Rag Rappy",
        ultimateName = "El Rappy",
        episode = Episode.II,
        enemy = true,
        rareType = { LoveRappy },
        areaIds = listOf(1, 2),
        typeId = 65,
        skin = 0,
        special = false,
    ),
    LoveRappy(
        uniqueName = "Love Rappy",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(1, 2),
        typeId = 65,
        skin = 1,
        special = false,
    ),
    StRappy(
        uniqueName = "St. Rappy",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(),
    ),
    HalloRappy(
        uniqueName = "Hallo Rappy",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(),
    ),
    EggRappy(
        uniqueName = "Egg Rappy",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(),
    ),
    Monest2(
        uniqueName = "Monest (Ep. II)",
        simpleName = "Monest",
        ultimateName = "Mothvist",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(1, 2),
        typeId = 66,
        skin = 0,
        special = false,
        properties = listOf(
            EntityProp(name = "State", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Start number", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "Total number", offset = 52, type = EntityPropType.F32)
        ),
    ),
    Mothmant2(
        uniqueName = "Mothmant",
        ultimateName = "Mothvert",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(),
    ),
    PoisonLily2(
        uniqueName = "Poison Lily (Ep. II)",
        simpleName = "Poison Lily",
        ultimateName = "Ob Lily",
        episode = Episode.II,
        enemy = true,
        rareType = { NarLily2 },
        areaIds = listOf(1, 2),
        typeId = 97,
        skin = 0,
        special = false,
    ),
    NarLily2(
        uniqueName = "Nar Lily (Ep. II)",
        simpleName = "Nar Lily",
        ultimateName = "Mil Lily",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(1, 2),
        typeId = 97,
        skin = 1,
        special = false,
    ),
    GrassAssassin2(
        uniqueName = "Grass Assassin (Ep. II)",
        simpleName = "Grass Assassin",
        ultimateName = "Crimson Assassin",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(1, 2),
        typeId = 96,
        skin = 0,
        special = false,
    ),
    Dimenian2(
        uniqueName = "Dimenian (Ep. II)",
        simpleName = "Dimenian",
        ultimateName = "Arlan",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(1, 2),
        typeId = 166,
        skin = 0,
        special = false,
        properties = listOf(
            EntityProp(name = "Idle distance", offset = 48, type = EntityPropType.F32)
        ),
    ),
    LaDimenian2(
        uniqueName = "La Dimenian (Ep. II)",
        simpleName = "La Dimenian",
        ultimateName = "Merlan",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(1, 2),
        typeId = 166,
        skin = 1,
        special = false,
        properties = listOf(
            EntityProp(name = "Idle distance", offset = 48, type = EntityPropType.F32)
        ),
    ),
    SoDimenian2(
        uniqueName = "So Dimenian (Ep. II)",
        simpleName = "So Dimenian",
        ultimateName = "Del-D",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(1, 2),
        typeId = 166,
        skin = 2,
        special = false,
        properties = listOf(
            EntityProp(name = "Idle distance", offset = 48, type = EntityPropType.F32)
        ),
    ),
    DarkBelra2(
        uniqueName = "Dark Belra (Ep. II)",
        simpleName = "Dark Belra",
        ultimateName = "Indi Belra",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(1, 2),
        typeId = 165,
        skin = 0,
        special = false,
    ),
    BarbaRay(
        uniqueName = "Barba Ray",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(14),
        typeId = 203,
        skin = 0,
        special = false,
    ),

    // Episode II VR Spaceship

    SavageWolf2(
        uniqueName = "Savage Wolf (Ep. II)",
        simpleName = "Savage Wolf",
        ultimateName = "Gulgus",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(3, 4),
        typeId = 67,
        skin = 0,
        special = false,
        properties = listOf(EntityProp(name = "Group ID", offset = 44, type = EntityPropType.F32)),
    ),
    BarbarousWolf2(
        uniqueName = "Barbarous Wolf (Ep. II)",
        simpleName = "Barbarous Wolf",
        ultimateName = "Gulgus-Gue",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(3, 4),
        typeId = 67,
        skin = 0,
        special = true,
        properties = listOf(EntityProp(name = "Group ID", offset = 44, type = EntityPropType.F32)),
    ),
    PanArms2(
        uniqueName = "Pan Arms (Ep. II)",
        simpleName = "Pan Arms",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(3, 4),
        typeId = 101,
        skin = 0,
        special = false,
    ),
    Migium2(
        uniqueName = "Migium (Ep. II)",
        simpleName = "Migium",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(),
    ),
    Hidoom2(
        uniqueName = "Hidoom (Ep. II)",
        simpleName = "Hidoom",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(),
    ),
    Dubchic2(
        uniqueName = "Dubchic (Ep. II)",
        simpleName = "Dubchic",
        ultimateName = "Dubchich",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(3, 4),
        typeId = 128,
        skin = 0,
        special = false,
    ),
    Gilchic2(
        uniqueName = "Gilchic (Ep. II)",
        simpleName = "Gilchic",
        ultimateName = "Gilchich",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(3, 4),
        typeId = 128,
        skin = 1,
        special = false,
    ),
    Garanz2(
        uniqueName = "Garanz (Ep. II)",
        simpleName = "Garanz",
        ultimateName = "Baranz",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(3, 4),
        typeId = 129,
        skin = 0,
        special = false,
    ),
    Dubswitch2(
        uniqueName = "Dubswitch (Ep. II)",
        simpleName = "Dubswitch",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(3, 4),
        typeId = 133,
        skin = 0,
        special = false,
    ),
    Delsaber2(
        uniqueName = "Delsaber (Ep. II)",
        simpleName = "Delsaber",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(3, 4),
        typeId = 160,
        skin = 0,
        special = false,
        properties = listOf(
            EntityProp(name = "Jump distance", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Block HP", offset = 48, type = EntityPropType.F32)
        ),
    ),
    ChaosSorcerer2(
        uniqueName = "Chaos Sorcerer (Ep. II)",
        simpleName = "Chaos Sorcerer",
        ultimateName = "Gran Sorcerer",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(3, 4),
        typeId = 161,
        skin = 0,
        special = false,
    ),
    GolDragon(
        uniqueName = "Gol Dragon",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(15),
        typeId = 204,
        skin = 0,
        special = false,
    ),

    // Episode II Central Control Area

    SinowBerill(
        uniqueName = "Sinow Berill",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(5, 6, 7, 8, 9, 16),
        typeId = 212,
        skin = 0,
        special = false,
    ),
    SinowSpigell(
        uniqueName = "Sinow Spigell",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(5, 6, 7, 8, 9, 16),
        typeId = 212,
        skin = 1,
        special = false,
    ),
    Merillia(
        uniqueName = "Merillia",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(5, 6, 7, 8, 9, 16),
        typeId = 213,
        skin = 0,
        special = false,
    ),
    Meriltas(
        uniqueName = "Meriltas",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(5, 6, 7, 8, 9, 16),
        typeId = 213,
        skin = 1,
        special = false,
    ),
    Mericarol(
        uniqueName = "Mericarol",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(5, 6, 7, 8, 9, 16, 17),
        typeId = 214,
        skin = 0,
        special = false,
    ),
    Mericus(
        uniqueName = "Mericus",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(5, 6, 7, 8, 9, 16, 17),
        typeId = 214,
        skin = 1,
        special = false,
    ),
    Merikle(
        uniqueName = "Merikle",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(5, 6, 7, 8, 9, 16, 17),
        typeId = 214,
        skin = 2,
        special = false,
    ),
    UlGibbon(
        uniqueName = "Ul Gibbon",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(5, 6, 7, 8, 9, 16),
        typeId = 215,
        skin = 0,
        special = false,
        properties = listOf(
            EntityProp(name = "Spot appear", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Jump appear", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "Back jump", offset = 52, type = EntityPropType.F32),
            EntityProp(name = "Run tech", offset = 56, type = EntityPropType.F32),
            EntityProp(name = "Back tech", offset = 60, type = EntityPropType.F32)
        ),
    ),
    ZolGibbon(
        uniqueName = "Zol Gibbon",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(5, 6, 7, 8, 9, 16),
        typeId = 215,
        skin = 1,
        special = false,
        properties = listOf(
            EntityProp(name = "Spot appear", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Jump appear", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "Back jump", offset = 52, type = EntityPropType.F32),
            EntityProp(name = "Run tech", offset = 56, type = EntityPropType.F32),
            EntityProp(name = "Back tech", offset = 60, type = EntityPropType.F32)
        ),
    ),
    Gibbles(
        uniqueName = "Gibbles",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(5, 6, 7, 8, 9, 16, 17),
        typeId = 216,
        skin = 0,
        special = false,
    ),
    Gee(
        uniqueName = "Gee",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(5, 6, 7, 8, 9, 16),
        typeId = 217,
        skin = 0,
        special = false,
    ),
    GiGue(
        uniqueName = "Gi Gue",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(5, 6, 7, 8, 9, 16, 17),
        typeId = 218,
        skin = 0,
        special = false,
    ),
    IllGill(
        uniqueName = "Ill Gill",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(17),
        typeId = 225,
        skin = 0,
        special = false,
    ),
    DelLily(
        uniqueName = "Del Lily",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(17),
        typeId = 97,
        skin = 0,
        special = false,
    ),
    Epsilon(
        uniqueName = "Epsilon",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(17),
        typeId = 224,
        skin = 0,
        special = false,
    ),
    GalGryphon(
        uniqueName = "Gal Gryphon",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(12),
        typeId = 192,
        skin = 0,
        special = false,
    ),

    // Episode II Seabed

    Deldepth(
        uniqueName = "Deldepth",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(10, 11),
        typeId = 219,
        skin = 0,
        special = false,
    ),
    Delbiter(
        uniqueName = "Delbiter",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(10, 11, 17),
        typeId = 220,
        skin = 0,
        special = false,
        properties = listOf(
            EntityProp(name = "Howl percent", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Confuse percent", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "Confuse distance", offset = 52, type = EntityPropType.F32),
            EntityProp(name = "Laser percent", offset = 56, type = EntityPropType.F32),
            EntityProp(name = "Charge percent", offset = 60, type = EntityPropType.F32),
            EntityProp(name = "Type", offset = 64, type = EntityPropType.I32)
        ),
    ),
    Dolmolm(
        uniqueName = "Dolmolm",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(10, 11),
        typeId = 221,
        skin = 0,
        special = false,
    ),
    Dolmdarl(
        uniqueName = "Dolmdarl",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(10, 11),
        typeId = 221,
        skin = 1,
        special = false,
    ),
    Morfos(
        uniqueName = "Morfos",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(10, 11),
        typeId = 222,
        skin = 0,
        special = false,
    ),
    Recobox(
        uniqueName = "Recobox",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(10, 11, 17),
        typeId = 223,
        skin = 0,
        special = false,
    ),
    Recon(
        uniqueName = "Recon",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(),
    ),
    SinowZoa(
        uniqueName = "Sinow Zoa",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(10, 11),
        typeId = 224,
        skin = 0,
        special = false,
    ),
    SinowZele(
        uniqueName = "Sinow Zele",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(10, 11),
        typeId = 224,
        skin = 1,
        special = false,
    ),
    OlgaFlow(
        uniqueName = "Olga Flow",
        episode = Episode.II,
        enemy = true,
        areaIds = listOf(13),
        typeId = 202,
        skin = 0,
        special = false,
    ),

    // Episode IV

    SandRappy(
        uniqueName = "Sand Rappy",
        episode = Episode.IV,
        enemy = true,
        rareType = { DelRappy },
        areaIds = listOf(1, 2, 3, 4, 5, 6, 7, 8),
        typeId = 65,
        skin = 0,
        special = false,
    ),
    DelRappy(
        uniqueName = "Del Rappy",
        episode = Episode.IV,
        enemy = true,
        areaIds = listOf(1, 2, 3, 4, 5, 6, 7, 8),
        typeId = 65,
        skin = 1,
        special = false,
    ),
    Astark(
        uniqueName = "Astark",
        episode = Episode.IV,
        enemy = true,
        areaIds = listOf(1, 2, 3, 4, 5, 6, 7, 8),
        typeId = 272,
        skin = 0,
        special = false,
    ),
    SatelliteLizard(
        uniqueName = "Satellite Lizard",
        episode = Episode.IV,
        enemy = true,
        areaIds = listOf(1, 2, 3, 4, 5, 6, 7, 8),
        typeId = 273,
        skin = 0,
        special = false,
    ),
    Yowie(
        uniqueName = "Yowie",
        episode = Episode.IV,
        enemy = true,
        areaIds = listOf(1, 2, 3, 4, 5, 6, 7, 8),
        typeId = 273,
        skin = 0,
        special = true,
    ),
    MerissaA(
        uniqueName = "Merissa A",
        episode = Episode.IV,
        enemy = true,
        rareType = { MerissaAA },
        areaIds = listOf(6, 7, 8),
        typeId = 274,
        skin = 0,
        special = false,
    ),
    MerissaAA(
        uniqueName = "Merissa AA",
        episode = Episode.IV,
        enemy = true,
        areaIds = listOf(6, 7, 8),
        typeId = 274,
        skin = 1,
        special = false,
    ),
    Girtablulu(
        uniqueName = "Girtablulu",
        episode = Episode.IV,
        enemy = true,
        areaIds = listOf(6, 7, 8),
        typeId = 275,
        skin = 0,
        special = false,
    ),
    Zu(
        uniqueName = "Zu",
        episode = Episode.IV,
        enemy = true,
        rareType = { Pazuzu },
        areaIds = listOf(1, 2, 3, 4, 5, 6, 7, 8),
        typeId = 276,
        skin = 0,
        special = false,
    ),
    Pazuzu(
        uniqueName = "Pazuzu",
        episode = Episode.IV,
        enemy = true,
        areaIds = listOf(1, 2, 3, 4, 5, 6, 7, 8),
        typeId = 276,
        skin = 1,
        special = false,
    ),
    Boota(
        uniqueName = "Boota",
        episode = Episode.IV,
        enemy = true,
        areaIds = listOf(1, 2, 3, 4, 5),
        typeId = 277,
        skin = 0,
        special = false,
    ),
    ZeBoota(
        uniqueName = "Ze Boota",
        episode = Episode.IV,
        enemy = true,
        areaIds = listOf(1, 2, 3, 4, 5),
        typeId = 277,
        skin = 1,
        special = false,
    ),
    BaBoota(
        uniqueName = "Ba Boota",
        episode = Episode.IV,
        enemy = true,
        areaIds = listOf(1, 2, 3, 4, 5),
        typeId = 277,
        skin = 2,
        special = false,
    ),
    Dorphon(
        uniqueName = "Dorphon",
        episode = Episode.IV,
        enemy = true,
        rareType = { DorphonEclair },
        areaIds = listOf(1, 2, 3, 4, 5),
        typeId = 278,
        skin = 0,
        special = false,
    ),
    DorphonEclair(
        uniqueName = "Dorphon Eclair",
        episode = Episode.IV,
        enemy = true,
        areaIds = listOf(1, 2, 3, 4, 5),
        typeId = 278,
        skin = 1,
        special = false,
    ),
    Goran(
        uniqueName = "Goran",
        episode = Episode.IV,
        enemy = true,
        areaIds = listOf(6, 7, 8),
        typeId = 279,
        skin = 0,
        special = false,
    ),
    PyroGoran(
        uniqueName = "Pyro Goran",
        episode = Episode.IV,
        enemy = true,
        areaIds = listOf(6, 7, 8),
        typeId = 279,
        skin = 1,
        special = false,
    ),
    GoranDetonator(
        uniqueName = "Goran Detonator",
        episode = Episode.IV,
        enemy = true,
        areaIds = listOf(6, 7, 8),
        typeId = 279,
        skin = 2,
        special = false,
    ),
    SaintMilion(
        uniqueName = "Saint-Milion",
        episode = Episode.IV,
        enemy = true,
        rareType = { Kondrieu },
        areaIds = listOf(9),
        typeId = 281,
        skin = 0,
        special = false,
    ),
    Shambertin(
        uniqueName = "Shambertin",
        episode = Episode.IV,
        enemy = true,
        rareType = { Kondrieu },
        areaIds = listOf(9),
        typeId = 281,
        skin = 1,
        special = false,
    ),
    Kondrieu(
        uniqueName = "Kondrieu",
        episode = Episode.IV,
        enemy = true,
        areaIds = listOf(9),
        typeId = 281,
        skin = 0,
        special = true,
    );

    /**
     * The type of this NPC's rare variant if it has one.
     */
    val rareType: NpcType? by lazy { rareType?.invoke() }

    companion object {
        /**
         * Use this instead of [values] to avoid unnecessary copying.
         */
        val VALUES: Array<NpcType> = values()
    }
}
