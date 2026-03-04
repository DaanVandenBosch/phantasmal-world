package world.phantasmal.web.viewer.models

import world.phantasmal.psolib.fileFormats.quest.NpcType

sealed class ViewerModel {
    abstract val uiName: String
    abstract val slug: String

    data class Character(val characterClass: CharacterClass) : ViewerModel() {
        override val uiName: String = characterClass.uiName
        override val slug: String = characterClass.slug
    }

    data class Npc(val npcType: NpcType) : ViewerModel() {
        override val uiName: String = npcType.uniqueName
        override val slug: String = npcType.name
    }

    data class Group(val label: String, val items: List<ViewerModel>)

    companion object {
        val CHARACTERS: List<ViewerModel> = CharacterClass.VALUES_LIST.map(::Character)

        val EP1_ENEMIES: List<ViewerModel> = listOf(
            NpcType.Hildebear,
            NpcType.Hildeblue,
            NpcType.RagRappy,
            NpcType.AlRappy,
            NpcType.Monest,
            NpcType.Mothmant,
            NpcType.SavageWolf,
            NpcType.BarbarousWolf,
            NpcType.Booma,
            NpcType.Gobooma,
            NpcType.Gigobooma,
            NpcType.GrassAssassin,
            NpcType.PoisonLily,
            NpcType.NarLily,
            NpcType.EvilShark,
            NpcType.PalShark,
            NpcType.GuilShark,
            NpcType.PanArms,
            NpcType.Dubchic,
            NpcType.Gilchic,
            NpcType.Garanz,
            NpcType.SinowBeat,
            NpcType.SinowGold,
            NpcType.Canadine,
            NpcType.Canane,
            NpcType.Delsaber,
            NpcType.ChaosSorcerer,
            NpcType.DarkGunner,
            NpcType.DarkBelra,
            NpcType.Bulclaw,
            NpcType.Claw,
            NpcType.Dimenian,
            NpcType.LaDimenian,
            NpcType.SoDimenian,
            NpcType.ChaosBringer,
            NpcType.PofuillySlime,
            NpcType.PouillySlime,
        ).map(::Npc)

        val EP2_ENEMIES: List<ViewerModel> = listOf(
            NpcType.Merillia,
            NpcType.Meriltas,
            NpcType.Mericarol,
            NpcType.Merikle,
            NpcType.Mericus,
            NpcType.UlGibbon,
            NpcType.ZolGibbon,
            NpcType.Gibbles,
            NpcType.Gee,
            NpcType.GiGue,
            NpcType.IllGill,
            NpcType.DelLily,
            NpcType.Epsilon,
            NpcType.SinowBerill,
            NpcType.SinowSpigell,
            NpcType.Dolmolm,
            NpcType.Dolmdarl,
            NpcType.Morfos,
            NpcType.Recobox,
            NpcType.Delbiter,
            NpcType.SinowZoa,
            NpcType.SinowZele,
            NpcType.Deldepth,
        ).map(::Npc)

        val EP4_ENEMIES: List<ViewerModel> = listOf(
            NpcType.Boota,
            NpcType.ZeBoota,
            NpcType.BaBoota,
            NpcType.Dorphon,
            NpcType.DorphonEclair,
            NpcType.Goran,
            NpcType.PyroGoran,
            NpcType.GoranDetonator,
            NpcType.SandRappy,
            NpcType.DelRappy,
            NpcType.Astark,
            NpcType.SatelliteLizard,
            NpcType.Yowie,
            NpcType.MerissaA,
            NpcType.MerissaAA,
            NpcType.Girtablulu,
            NpcType.Zu,
            NpcType.Pazuzu,
        ).map(::Npc)

        val BOSSES: List<ViewerModel> = listOf(
            // EP1
            NpcType.Dragon,
            NpcType.DeRolLe,
            NpcType.VolOptPart1,
            NpcType.VolOptPart2,
            NpcType.DarkFalz,
            // EP2
            NpcType.BarbaRay,
            NpcType.GalGryphon,
            NpcType.OlgaFlow,
            // EP4
            NpcType.SaintMilion,
            NpcType.Shambertin,
            NpcType.Kondrieu,
            NpcType.GolDragon,
        ).map(::Npc)

        val ALL: List<ViewerModel> =
            CHARACTERS + EP1_ENEMIES + EP2_ENEMIES + EP4_ENEMIES + BOSSES

        val GROUPS: List<Group> = listOf(
            Group("Characters", CHARACTERS),
            Group("EP1 Enemies", EP1_ENEMIES),
            Group("EP2 Enemies", EP2_ENEMIES),
            Group("EP4 Enemies", EP4_ENEMIES),
            Group("Bosses", BOSSES),
        )

        fun findBySlug(slug: String): ViewerModel? = ALL.find { it.slug == slug }
    }
}
