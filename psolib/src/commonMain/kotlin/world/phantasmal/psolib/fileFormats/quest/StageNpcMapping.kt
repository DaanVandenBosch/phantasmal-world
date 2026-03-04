package world.phantasmal.psolib.fileFormats.quest

/**
 * Resolves a StageNPC (typeId=0x33) to its actual NpcType based on the NPC51Name table.
 * The NPC51Name table maps (mapId, subtype) to the monster that appears in-game.
 *
 * @param mapId The game-internal map ID (from Areas.kt)
 * @param subtype The subtype value stored at data offset 32 (rotation.x field)
 * @return The resolved NpcType, or null if unmappable (box, trap, environment, etc.)
 */
fun resolveStageNpc(mapId: Int, subtype: Int): NpcType? {
    if (subtype < 0 || subtype > 15) return null
    return NPC51_TABLE[mapId]?.get(subtype)
}

/**
 * NPC51Name lookup table mapping (mapId, subtype) to NpcType.
 * Based on the NPC51Name array in the PSO BB client.
 *
 * null entries represent non-monster entities (boxes, traps, environment objects)
 * or invalid/crash-inducing subtypes.
 *
 * EP2 VR Temple/Spaceship entries use EP1 NpcTypes because EntityAssetLoader
 * already handles EP2→EP1 model redirection.
 */
private val NPC51_TABLE: Map<Int, Array<NpcType?>> by lazy {
    val ep2Outdoor = arrayOf<NpcType?>(
        NpcType.RagRappy,        // 0: Rappy
        NpcType.LoveRappy,       // 1: Love Rappy
        NpcType.UlGibbon,        // 2
        NpcType.ZolGibbon,       // 3
        NpcType.Gee,             // 4
        null,                    // 5: CRASH
        NpcType.Merillia,        // 6
        null,                    // 7: CRASH
        NpcType.Meriltas,        // 8
        null,                    // 9: Gee Nest
        null,                    // 10: Small Rock
        null,                    // 11: Small Plant
        NpcType.GiGue,           // 12
        NpcType.Mericarol,       // 13
        NpcType.Gibbles,         // 14
        null,                    // 15: CRASH
    )

    val seabed = arrayOf<NpcType?>(
        null,                    // 0: Seabed Box
        NpcType.Recon,           // 1
        NpcType.Recobox,         // 2
        null,                    // 3: Dolphin
        NpcType.Dolmolm,         // 4
        NpcType.Delbiter,        // 5
        NpcType.Deldepth,        // 6
        null,                    // 7: Deldepth form 2
        NpcType.SinowZoa,        // 8
        NpcType.SinowZele,       // 9
        null,                    // 10: Sinow Zoa dead
        null,                    // 11: Sinow Zele dead
        NpcType.Morfos,          // 12
        NpcType.Dolmdarl,        // 13
        null,                    // 14: CRASH
        null,                    // 15: CRASH
    )

    mapOf(
        // EP1 Forest 1
        1 to arrayOf(
            null,                    // 0: Forest Box
            NpcType.Booma,           // 1
            NpcType.Gigobooma,       // 2
            NpcType.Gobooma,         // 3
            NpcType.RagRappy,        // 4
            NpcType.AlRappy,         // 5
            NpcType.Mothmant,        // 6
            NpcType.Monest,          // 7
            NpcType.BarbarousWolf,   // 8
            NpcType.SavageWolf,      // 9
            null,                    // 10: Chao NPC
            null,                    // 11: Crashed Probe
            null,                    // 12: Crashed Probe on side
            null,                    // 13: CRASH
            null,                    // 14: CRASH
            null,                    // 15: CRASH
        ),
        // EP1 Forest 2
        2 to arrayOf(
            null,                    // 0: Forest Box
            NpcType.Booma,           // 1
            NpcType.Gigobooma,       // 2
            NpcType.Gobooma,         // 3
            NpcType.RagRappy,        // 4
            NpcType.AlRappy,         // 5
            NpcType.Mothmant,        // 6
            NpcType.Monest,          // 7
            NpcType.BarbarousWolf,   // 8
            NpcType.SavageWolf,      // 9
            null,                    // 10: Chao NPC
            null,                    // 11: Mini Hildebear
            NpcType.Hildebear,       // 12
            NpcType.Hildeblue,       // 13
            null,                    // 14: Crashed Probe
            null,                    // 15: Crashed Probe on side
        ),
        // EP1 Cave 1
        3 to arrayOf(
            null,                    // 0: Caves Box
            NpcType.NanoDragon,      // 1
            NpcType.PanArms,         // 2
            NpcType.Hidoom,          // 3
            NpcType.Migium,          // 4
            NpcType.PalShark,        // 5
            NpcType.GuilShark,       // 6
            NpcType.EvilShark,       // 7
            NpcType.GrassAssassin,   // 8
            null,                    // 9: Mini Grass Assassin
            NpcType.PoisonLily,      // 10
            null,                    // 11: CRASH
            null,                    // 12: CRASH
            null,                    // 13: CRASH
            null,                    // 14: CRASH
            null,                    // 15: CRASH
        ),
        // EP1 Cave 2
        4 to arrayOf(
            null,                    // 0: Caves Box
            NpcType.NanoDragon,      // 1
            NpcType.PofuillySlime,   // 2: Slime
            NpcType.PouillySlime,    // 3: Rare Slime
            NpcType.PalShark,        // 4
            NpcType.GuilShark,       // 5
            NpcType.EvilShark,       // 6
            NpcType.GrassAssassin,   // 7
            null,                    // 8: Mini Grass Assassin
            NpcType.PoisonLily,      // 9
            null,                    // 10: CRASH
            null,                    // 11: CRASH
            null,                    // 12: CRASH
            null,                    // 13: CRASH
            null,                    // 14: CRASH
            null,                    // 15: CRASH
        ),
        // EP1 Cave 3
        5 to arrayOf(
            null,                    // 0: Caves Box
            NpcType.NanoDragon,      // 1
            NpcType.PanArms,         // 2
            NpcType.Hidoom,          // 3
            NpcType.Migium,          // 4
            NpcType.PofuillySlime,   // 5: Slime
            NpcType.PouillySlime,    // 6: Rare Slime
            NpcType.PalShark,        // 7
            NpcType.GuilShark,       // 8
            NpcType.EvilShark,       // 9
            NpcType.GrassAssassin,   // 10
            null,                    // 11: Mini Grass Assassin
            NpcType.PoisonLily,      // 12
            null,                    // 13: CRASH
            null,                    // 14: CRASH
            null,                    // 15: CRASH
        ),
        // EP1 Mine 1
        6 to arrayOf(
            null,                    // 0: Mine Box
            NpcType.Canadine,        // 1
            NpcType.Canane,          // 2
            NpcType.Gilchic,         // 3
            NpcType.Dubchic,         // 4
            NpcType.Garanz,          // 5
            null,                    // 6: Garanz (broke)
            NpcType.SinowBeat,       // 7: Sinow Blue
            NpcType.SinowGold,       // 8
            null,                    // 9: CRASH
            null,                    // 10: CRASH
            null,                    // 11: CRASH
            null,                    // 12: CRASH
            null,                    // 13: CRASH
            null,                    // 14: CRASH
            null,                    // 15: CRASH
        ),
        // EP1 Mine 2
        7 to arrayOf(
            null,                    // 0: Mine Box
            NpcType.Canadine,        // 1
            NpcType.Canane,          // 2
            NpcType.Gilchic,         // 3
            NpcType.Dubchic,         // 4
            NpcType.Garanz,          // 5
            null,                    // 6: Garanz (broke)
            NpcType.SinowBeat,       // 7: Sinow Blue
            NpcType.SinowGold,       // 8
            null,                    // 9: Little flying robot
            null,                    // 10: CRASH
            null,                    // 11: CRASH
            null,                    // 12: CRASH
            null,                    // 13: CRASH
            null,                    // 14: CRASH
            null,                    // 15: CRASH
        ),
        // EP1 Ruins 1
        8 to arrayOf(
            NpcType.Bulclaw,         // 0
            NpcType.Claw,            // 1
            NpcType.DarkBelra,       // 2
            NpcType.Delsaber,        // 3
            NpcType.SoDimenian,      // 4
            NpcType.LaDimenian,      // 5
            NpcType.Dimenian,        // 6
            NpcType.ChaosSorcerer,   // 7
            null,                    // 8: Pillar Trap
            null,                    // 9: Poison Bulb
            null,                    // 10: CRASH
            null,                    // 11: CRASH
            null,                    // 12: CRASH
            null,                    // 13: CRASH
            null,                    // 14: CRASH
            null,                    // 15: CRASH
        ),
        // EP1 Ruins 2
        9 to arrayOf(
            NpcType.ChaosBringer,    // 0
            NpcType.Delsaber,        // 1
            NpcType.SoDimenian,      // 2
            NpcType.LaDimenian,      // 3
            NpcType.Dimenian,        // 4
            null,                    // 5: Pillar Trap
            null,                    // 6: Poison Bulb
            null,                    // 7: CRASH
            null,                    // 8: CRASH
            null,                    // 9: CRASH
            null,                    // 10: CRASH
            null,                    // 11: CRASH
            null,                    // 12: CRASH
            null,                    // 13: CRASH
            null,                    // 14: CRASH
            null,                    // 15: CRASH
        ),
        // EP1 Ruins 3
        10 to arrayOf(
            NpcType.DarkBelra,       // 0
            NpcType.ChaosBringer,    // 1
            NpcType.SoDimenian,      // 2
            NpcType.LaDimenian,      // 3
            NpcType.Dimenian,        // 4
            NpcType.ChaosSorcerer,   // 5
            null,                    // 6: Pillar Trap
            null,                    // 7: Poison Bulb
            null,                    // 8: CRASH
            null,                    // 9: CRASH
            null,                    // 10: CRASH
            null,                    // 11: CRASH
            null,                    // 12: CRASH
            null,                    // 13: CRASH
            null,                    // 14: CRASH
            null,                    // 15: CRASH
        ),
        // EP2 VR Temple Alpha (uses EP1 NpcTypes)
        19 to arrayOf(
            NpcType.RagRappy,        // 0: Rappy
            NpcType.LoveRappy,       // 1: Love Rappy
            NpcType.Mothmant,        // 2
            NpcType.Monest,          // 3
            NpcType.DarkBelra,       // 4
            NpcType.SoDimenian,      // 5
            NpcType.LaDimenian,      // 6
            NpcType.Dimenian,        // 7
            null,                    // 8: Mini Hildebear
            NpcType.Hildebear,       // 9
            NpcType.Hildeblue,       // 10
            NpcType.GrassAssassin,   // 11
            null,                    // 12: Mini Grass Assassin
            NpcType.PoisonLily,      // 13
            null,                    // 14: Pillar Trap
            null,                    // 15: CRASH
        ),
        // EP2 VR Temple Beta (same as Alpha)
        20 to arrayOf(
            NpcType.RagRappy,        // 0: Rappy
            NpcType.LoveRappy,       // 1: Love Rappy
            NpcType.Mothmant,        // 2
            NpcType.Monest,          // 3
            NpcType.DarkBelra,       // 4
            NpcType.SoDimenian,      // 5
            NpcType.LaDimenian,      // 6
            NpcType.Dimenian,        // 7
            null,                    // 8: Mini Hildebear
            NpcType.Hildebear,       // 9
            NpcType.Hildeblue,       // 10
            NpcType.GrassAssassin,   // 11
            null,                    // 12: Mini Grass Assassin
            NpcType.PoisonLily,      // 13
            null,                    // 14: Pillar Trap
            null,                    // 15: CRASH
        ),
        // EP2 VR Spaceship Alpha (uses EP1 NpcTypes)
        21 to arrayOf(
            NpcType.BarbarousWolf,   // 0
            NpcType.SavageWolf,      // 1
            NpcType.Delsaber,        // 2
            NpcType.Gilchic,         // 3
            NpcType.Dubchic,         // 4
            NpcType.Garanz,          // 5
            null,                    // 6: Garanz (broke)
            NpcType.PanArms,         // 7
            NpcType.Hidoom,          // 8
            NpcType.Migium,          // 9
            null,                    // 10: Pillar Trap
            null,                    // 11: CRASH
            null,                    // 12: CRASH
            null,                    // 13: CRASH
            null,                    // 14: CRASH
            null,                    // 15: CRASH
        ),
        // EP2 VR Spaceship Beta (uses EP1 NpcTypes)
        22 to arrayOf(
            NpcType.BarbarousWolf,   // 0
            NpcType.SavageWolf,      // 1
            NpcType.Delsaber,        // 2
            NpcType.Gilchic,         // 3
            NpcType.Dubchic,         // 4
            NpcType.PanArms,         // 5
            NpcType.Hidoom,          // 6
            NpcType.Migium,          // 7
            NpcType.ChaosSorcerer,   // 8
            null,                    // 9: Pillar Trap
            null,                    // 10: CRASH
            null,                    // 11: CRASH
            null,                    // 12: CRASH
            null,                    // 13: CRASH
            null,                    // 14: CRASH
            null,                    // 15: CRASH
        ),
        // EP2 CCA
        23 to ep2Outdoor,
        // EP2 Jungle East
        24 to ep2Outdoor,
        // EP2 Jungle North
        25 to ep2Outdoor,
        // EP2 Mountain
        26 to ep2Outdoor,
        // EP2 Seaside
        27 to ep2Outdoor,
        // EP2 Seabed Upper Levels
        28 to seabed,
        // EP2 Seabed Lower Levels
        29 to seabed,
        // EP2 Seaside Area at Night
        34 to arrayOf(
            NpcType.RagRappy,        // 0: Rappy
            NpcType.LoveRappy,       // 1: Love Rappy
            NpcType.Recon,           // 2
            NpcType.Recobox,         // 3
            null,                    // 4: CRASH
            NpcType.ZolGibbon,       // 5
            NpcType.Gee,             // 6
            null,                    // 7: NiGHTS (sit)
            null,                    // 8: NiGHTS (fly)
            NpcType.Merillia,        // 9
            NpcType.Meriltas,        // 10
            null,                    // 11: Gee Nest
            null,                    // 12: Small Rock
            null,                    // 13: Small Plant
            NpcType.Dolmolm,         // 14
            NpcType.Dolmdarl,        // 15
        ),
        // EP2 Tower
        35 to arrayOf(
            NpcType.Recon,           // 0
            NpcType.Recobox,         // 1: Recon Box
            NpcType.DelLily,         // 2
            null,                    // 3: Gee Nest
            NpcType.GiGue,           // 4
            NpcType.Mericarol,       // 5
            NpcType.IllGill,         // 6
            NpcType.Gibbles,         // 7
            NpcType.Delbiter,        // 8
            NpcType.Epsilon,         // 9
            null,                    // 10: CRASH
            null,                    // 11: CRASH
            null,                    // 12: CRASH
            null,                    // 13: CRASH
            null,                    // 14: CRASH
            null,                    // 15: CRASH
        ),
    )
}
