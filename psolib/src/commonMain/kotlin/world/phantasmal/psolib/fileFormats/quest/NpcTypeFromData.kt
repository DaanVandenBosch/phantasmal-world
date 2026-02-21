package world.phantasmal.psolib.fileFormats.quest

import world.phantasmal.psolib.Episode

// TODO: detect Mothmant, St. Rappy, Hallo Rappy, Egg Rappy, Death Gunner, Bulk and Recon.
fun npcTypeFromQuestNpc(npc: QuestNpc): NpcType {
    val episode = npc.episode
    val special = npc.special
    val skin = npc.skin
    // Use gameAreaId for NPC type detection (mapped from floor mapping, e.g., 17 for Tower)
    // This ensures correct type identification for quests using map_designate instructions
    val areaId = npc.gameAreaId

    return when (npc.typeId.toInt()) {
        0x004 -> NpcType.FemaleFat
        0x005 -> NpcType.FemaleMacho
        0x007 -> NpcType.FemaleTall
        0x00A -> NpcType.MaleDwarf
        0x00B -> NpcType.MaleFat
        0x00C -> NpcType.MaleMacho
        0x00D -> NpcType.MaleOld
        0x019 -> NpcType.BlueSoldier
        0x01A -> NpcType.RedSoldier
        0x01B -> NpcType.Principal
        0x01C -> NpcType.Tekker
        0x01D -> NpcType.GuildLady
        0x01E -> NpcType.Scientist
        0x01F -> NpcType.Nurse
        0x020 -> NpcType.Irene
        0x040 -> when (skin % 2) {
            0 -> if (episode == Episode.II) NpcType.Hildebear2 else NpcType.Hildebear
            else -> if (episode == Episode.II) NpcType.Hildeblue2 else NpcType.Hildeblue
        }
        0x041 -> when (skin % 2) {
            0 -> when (episode) {
                Episode.I -> NpcType.RagRappy
                Episode.II -> NpcType.RagRappy2
                Episode.IV -> NpcType.SandRappy
            }
            else -> when (episode) {
                Episode.I -> NpcType.AlRappy
                Episode.II -> NpcType.LoveRappy
                Episode.IV -> NpcType.DelRappy
            }
        }
        0x042 -> if (episode == Episode.II) NpcType.Monest2 else NpcType.Monest
        0x043 -> when (special) {
            true -> if (episode == Episode.II) NpcType.BarbarousWolf2 else NpcType.BarbarousWolf
            false -> if (episode == Episode.II) NpcType.SavageWolf2 else NpcType.SavageWolf
        }
        0x044 -> when (skin % 3) {
            0 -> NpcType.Booma
            1 -> NpcType.Gobooma
            else -> NpcType.Gigobooma
        }
        0x060 -> if (episode == Episode.II) NpcType.GrassAssassin2 else NpcType.GrassAssassin
        0x061 -> when {
            areaId == 17 -> NpcType.DelLily  // Tower (EP2 area 17)
            special -> if (episode == Episode.II) NpcType.NarLily2 else NpcType.NarLily
            else -> if (episode == Episode.II) NpcType.PoisonLily2 else NpcType.PoisonLily
        }
        0x062 -> NpcType.NanoDragon
        0x063 -> when (skin % 3) {
            0 -> NpcType.EvilShark
            1 -> NpcType.PalShark
            else -> NpcType.GuilShark
        }
        0x064 -> if (special) NpcType.PouillySlime else NpcType.PofuillySlime
        0x065 -> if (episode == Episode.II) NpcType.PanArms2 else NpcType.PanArms
        0x080 -> when (skin % 2) {
            0 -> if (episode == Episode.II) NpcType.Dubchic2 else NpcType.Dubchic
            else -> if (episode == Episode.II) NpcType.Gilchic2 else NpcType.Gilchic
        }
        0x081 -> if (episode == Episode.II) NpcType.Garanz2 else NpcType.Garanz
        0x082 -> if (special) NpcType.SinowGold else NpcType.SinowBeat
        0x083 -> NpcType.Canadine
        0x084 -> NpcType.Canane
        0x085 -> if (episode == Episode.II) NpcType.Dubswitch2 else NpcType.Dubswitch
        0x0A0 -> if (episode == Episode.II) NpcType.Delsaber2 else NpcType.Delsaber
        0x0A1 -> if (episode == Episode.II) NpcType.ChaosSorcerer2 else NpcType.ChaosSorcerer
        0x0A2 -> NpcType.DarkGunner
        0x0A4 -> NpcType.ChaosBringer
        0x0A5 -> if (episode == Episode.II) NpcType.DarkBelra2 else NpcType.DarkBelra
        0x0A6 -> when (skin % 3) {
            0 -> if (episode == Episode.II) NpcType.Dimenian2 else NpcType.Dimenian
            1 -> if (episode == Episode.II) NpcType.LaDimenian2 else NpcType.LaDimenian
            else -> if (episode == Episode.II) NpcType.SoDimenian2 else NpcType.SoDimenian
        }
        0x0A7 -> NpcType.Bulclaw
        0x0A8 -> NpcType.Claw
        0x0C0 -> if (episode == Episode.II) NpcType.GalGryphon else NpcType.Dragon
        0x0C1 -> NpcType.DeRolLe
        0x0C2 -> NpcType.VolOptPart1
        0x0C5 -> NpcType.VolOptPart2
        0x0C8 -> NpcType.DarkFalz
        0x0CA -> NpcType.OlgaFlow
        0x0CB -> NpcType.BarbaRay
        0x0CC -> NpcType.GolDragon
        0x0D4 -> when (skin % 2) {
            0 -> NpcType.SinowBerill
            else -> NpcType.SinowSpigell
        }
        0x0D5 -> when (skin % 2) {
            0 -> NpcType.Merillia
            else -> NpcType.Meriltas
        }
        0x0D6 -> when (skin % 3) {
            0 -> NpcType.Mericarol
            1 -> NpcType.Merikle
            else -> NpcType.Mericus
        }
        0x0D7 -> when (skin % 2) {
            0 -> NpcType.UlGibbon
            else -> NpcType.ZolGibbon
        }
        0x0D8 -> NpcType.Gibbles
        0x0D9 -> NpcType.Gee
        0x0DA -> NpcType.GiGue
        0x0DB -> NpcType.Deldepth
        0x0DC -> NpcType.Delbiter
        0x0DD -> when (skin % 2) {
            0 -> NpcType.Dolmolm
            else -> NpcType.Dolmdarl
        }
        0x0DE -> NpcType.Morfos
        0x0DF -> NpcType.Recobox
        0x0E0 -> when {
            areaId == 17 -> NpcType.Epsilon  // Tower (EP2 area 17)
            skin % 2 == 0 -> NpcType.SinowZoa
            else -> NpcType.SinowZele
        }
        0x0E1 -> NpcType.IllGill
        0x0F1 -> NpcType.ItemShop
        0x0FE -> NpcType.Nurse2
        0x110 -> NpcType.Astark
        0x111 -> if (special) NpcType.Yowie else NpcType.SatelliteLizard
        0x112 -> when (skin % 2) {
            0 -> NpcType.MerissaA
            else -> NpcType.MerissaAA
        }
        0x113 -> NpcType.Girtablulu
        0x114 -> when (skin % 2) {
            0 -> NpcType.Zu
            else -> NpcType.Pazuzu
        }
        0x115 -> when (skin % 3) {
            0 -> NpcType.Boota
            1 -> NpcType.ZeBoota
            else -> NpcType.BaBoota
        }
        0x116 -> when (skin % 2) {
            0 -> NpcType.Dorphon
            else -> NpcType.DorphonEclair
        }
        0x117 -> when (skin % 3) {
            0 -> NpcType.Goran
            1 -> NpcType.PyroGoran
            else -> NpcType.GoranDetonator
        }
        0x119 -> when {
            special -> NpcType.Kondrieu
            skin % 2 == 0 -> NpcType.SaintMilion
            else -> NpcType.Shambertin
        }
        else -> NpcType.Unknown
    }
}
