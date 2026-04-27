package world.phantasmal.web.viewer.loading

import mu.KotlinLogging
import world.phantasmal.core.Success
import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psolib.fileFormats.ninja.*
import world.phantasmal.psolib.fileFormats.quest.NpcType
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.loading.LoadingCache
import world.phantasmal.web.viewer.models.AnimationModel
import world.phantasmal.webui.DisposableContainer

private val logger = KotlinLogging.logger {}

class NpcAssetLoader(private val assetLoader: AssetLoader) : DisposableContainer() {
    private val ninjaObjectCache: LoadingCache<NpcType, NinjaObject<*, *>> =
        addDisposable(LoadingCache(::loadGeometry) { /* Nothing to dispose. */ })

    private val xvrTextureCache: LoadingCache<NpcType, List<XvrTexture>> =
        addDisposable(LoadingCache(::loadTextures) { /* Nothing to dispose. */ })

    suspend fun loadNinjaObject(npcType: NpcType): NinjaObject<*, *> =
        ninjaObjectCache.get(npcType)

    suspend fun loadXvrTextures(npcType: NpcType): List<XvrTexture> =
        xvrTextureCache.get(npcType)

    private suspend fun loadGeometry(npcType: NpcType): NinjaObject<*, *> {
        if (npcType in XJ_GEOMETRY_TYPES) {
            val buffer = assetLoader.loadArrayBuffer("/npcs/${npcType.name}.xj")
            val result = parseXj(buffer.cursor(Endianness.Little))
            if (result !is Success) error("Failed to load XJ geometry for ${npcType.uniqueName}.")
            return result.value.first()
        }

        val buffer = assetLoader.loadArrayBuffer("/npcs/${npcType.name}.nj")
        val result = parseNj(buffer.cursor(Endianness.Little))
        if (result !is Success) error("Failed to load NJ geometry for ${npcType.uniqueName}.")

        val objects = result.value
        val root = objects.first()
        root.evaluationFlags.breakChildTrace = false

        for (obj in objects.drop(1)) {
            root.addChild(obj)
        }

        return root
    }

    private suspend fun loadTextures(npcType: NpcType): List<XvrTexture> {
        val buffer = assetLoader.loadArrayBuffer("/npcs/${npcType.name}.xvm")
        val result = parseXvm(buffer.cursor(Endianness.Little))

        return if (result is Success) {
            result.value.textures
        } else {
            logger.warn { "Couldn't parse textures for ${npcType.uniqueName}." }
            emptyList()
        }
    }

    companion object {
        /** NPC types whose geometry uses .xj format instead of the default .nj. */
        private val XJ_GEOMETRY_TYPES: Set<NpcType> = setOf(
            NpcType.VolOptMonitor,
        )

        /**
         * Maps NpcType variants to their animation base name (the key in [NPC_ANIMATIONS]).
         * NPC types not in this map use their own [NpcType.name] as the key.
         */
        private val ANIMATION_FALLBACKS: Map<NpcType, String> = mapOf(
            // EP1
            NpcType.Hildeblue to "Hildebear",
            NpcType.AlRappy to "RagRappy",
            NpcType.BarbarousWolf to "SavageWolf",
            NpcType.Gobooma to "Booma",
            NpcType.Gigobooma to "Booma",
            NpcType.EvilShark to "PalShark",
            NpcType.GuilShark to "PalShark",
            NpcType.Dubchic to "Gilchic",
            NpcType.SinowBeat to "SinowGold",
            NpcType.Canane to "Canadine",
            NpcType.DarkGunner to "DarkGunnerCenter",
            NpcType.Bulclaw to "Bulk",
            NpcType.Claw to "Bulk",
            NpcType.PofuillySlime to "PanArms",
            NpcType.PouillySlime to "PanArms",
            NpcType.Dimenian to "SoDimenian",
            NpcType.LaDimenian to "SoDimenian",
            // EP2
            NpcType.Meriltas to "Merillia",
            NpcType.Mericarol to "Merikle",
            NpcType.Mericus to "Merikle",
            NpcType.ZolGibbon to "UlGibbon",
            NpcType.SinowBerill to "SinowSpigell",
            NpcType.Dolmdarl to "Dolmolm",
            NpcType.Recobox to "Recon",
            NpcType.SinowZele to "SinowZoa",
            // EP4
            NpcType.ZeBoota to "Boota",
            NpcType.BaBoota to "Boota",
            NpcType.DorphonEclair to "Dorphon",
            NpcType.PyroGoran to "Goran",
            NpcType.GoranDetonator to "Goran",
            NpcType.DelRappy to "SandRappy",
            NpcType.SatelliteLizard to "NanoDragon",
            NpcType.MerissaAA to "MerissaA",
            NpcType.Pazuzu to "Zu",
        )

        /**
         * NPC animation data extracted from game BML files.
         * Maps model base name to list of animation names.
         */
        private val NPC_ANIMATIONS: Map<String, List<String>> = mapOf(
            "Astark" to listOf("cstand", "cwalk", "damage", "dead", "deadb", "giva", "hoe", "jump", "punch", "stand", "walk"),
            "BarbaRay" to listOf("beam", "beam02", "beamwait", "bite", "ed", "fd02", "fjump", "forward", "hige", "l", "lrjump", "op", "r", "rljump", "scatter", "tloop"),
            "Booma" to listOf("appear", "atackl", "atackr", "damage", "dead", "deadb", "leader", "mihari", "run", "stund", "wakeup", "walk"),
            "Boota" to listOf("appear", "atackl", "atackr", "damage", "dead", "deadb", "leader", "mihari", "run", "rush", "stund", "wakeup", "walk"),
            "Bulk" to listOf("balattack", "balcomb", "baldamage", "baldie", "balshout", "balwait", "bcattack", "bcdamage", "bcdie", "bcwait", "clattack", "cldamage", "cldie", "cllturn", "clrturn", "clwait"),
            "Canadine" to listOf("change01", "change02", "damage01", "damage02", "wait01", "wait02"),
            "ChaosBringer" to listOf("beam", "cold", "damage", "dead", "hoe", "kamae", "kiri", "run", "tpkyu", "wait", "walk"),
            "ChaosSorcerer" to listOf("attack1", "attack2", "attack3", "cure", "damage", "die", "enter", "wait"),
            "DarkBelra" to listOf("attack", "damege", "die", "lattack", "memai", "rattack", "wait", "walk"),
            "DarkGunnerCenter" to listOf("attack", "await", "damage", "damage2", "die", "drop", "duck", "duckdame", "duckdie", "duckroop", "duckwake", "move", "pullback", "wait"),
            "DeRolLe" to listOf("beam", "beam02", "beamwait", "bite", "die", "enter", "fd02", "fjump", "forward", "l", "lrjump", "r", "rljump", "scatter", "tloop"),
            "DelLily" to listOf("attack", "damege", "die", "laugh", "waitc", "waito", "wake"),
            "Delbiter" to listOf("attack", "beam", "damage", "dead", "kamae", "run", "stop", "tpkyu", "wait", "walk"),
            "Deldepth" to listOf("attack", "attack2", "changea", "changeb", "damage", "dead", "move", "wait"),
            "Delsaber" to listOf("aseri", "atack", "damage", "dead", "deadb", "defdam", "defence", "jump", "nail", "wait", "walk"),
            "Dolmolm" to listOf("atack", "damage", "dead", "odori", "wait", "wait2", "wake", "walk"),
            "Dorphon" to listOf("attack", "beam", "damage", "dead", "kamae", "run", "stop", "tokyu", "wait", "walk"),
            "Dragon" to listOf("bossop", "daml", "dams", "dead", "down", "fire", "fly", "flyshot", "frin", "frloop", "frout", "kiri", "land", "lift", "nkdown", "nkup", "nobi", "stand", "tatk", "tobidasi", "tukomi", "walk", "wgwalk", "wing", "wngclose", "wngopn"),
            "EggRappy" to listOf("attack", "damage", "die", "run", "tumble", "wait", "wait2", "wake", "wake2", "walk"),
            "Epsilon" to listOf("bal", "bc", "body", "claw"),
            "GalGryphon" to listOf("dead", "fly", "hoe", "houko", "huse", "husein", "huseout", "kaku", "kakuin", "kakuland", "kakuout", "kiri", "kiriland", "kirin", "kiriout", "land", "op", "run", "runin", "runout", "shot", "stand", "takeoff", "tuno", "tunoin", "tunout", "walk"),
            "Garanz" to listOf("attack", "damage01", "damage02", "deth", "wait", "walk01", "walk02", "walk03"),
            "Gee" to listOf("attack", "damage", "tuki", "wait"),
            "GiGue" to listOf("damage", "exp", "fire", "missile", "move", "on", "redu", "rot", "wait"),
            "Gibbles" to listOf("attack", "damage", "dead", "guard", "jump", "kamae", "landing", "punch", "wait", "walk"),
            "Gilchic" to listOf("damage", "kamae01", "kamae02", "revival", "scratch01", "scratch02", "shoot01", "shoot02", "starting", "wait01", "wait02", "walk01", "walk02"),
            "Girtablulu" to listOf("atack", "damage", "dead", "eyedamage", "fire", "laugh", "wait", "wait2", "wake"),
            "GolDragon" to listOf("bossopgc", "daml", "dams", "dead", "down", "fire", "fly", "flyshot", "frin", "frloop", "frout", "kiri", "land", "lift", "nkdown", "nkup", "nobi", "stand", "tatk", "tobidasi", "tukomi", "walk", "wgwalk", "wing", "wngclose", "wngopn"),
            "Goran" to listOf("appear", "atackl", "atackr", "damage", "dead", "deadb", "leader", "mihari", "run", "stund", "wakeup", "walk"),
            "GrassAssassin" to listOf("damege", "die", "lattack", "mad", "rattack", "spit", "wait", "walk"),
            "HalloRappy" to listOf("attack", "damage", "die", "run", "tumble", "wait", "wait2", "wake", "wake2", "walk"),
            "Hildebear" to listOf("cstand", "cwalk", "damage", "dead", "deadb", "giva", "hoe", "jump", "punch", "stand", "walk"),
            "IllGill" to listOf("damage", "dead", "kage", "kamae", "lattack", "prekamae", "rattack", "tackle", "wait", "walk"),
            "LoveRappy" to listOf("attack", "damage", "die", "run", "tumble", "wait", "wait2", "wake", "wake2", "walk"),
            "Merikle" to listOf("atack", "damage", "dead", "fire", "laugh", "wait", "wait2", "wake"),
            "Merillia" to listOf("appear", "atk", "damage", "dead", "kahun", "run", "stand", "umari", "wait", "wakeup", "walk"),
            "MerissaA" to listOf("apear", "apper", "atack", "attack", "damage", "death", "hi", "jump", "kie", "melissa", "move", "nodam", "tlatk", "tope", "wait", "wait2"),
            "Morfos" to listOf("attack", "beam", "damage", "dead", "wait"),
            "Mothmant" to listOf("atack", "dam", "damage", "dead", "down", "dwndam", "dwnexit", "dwnwait", "exit", "fly", "land", "move", "trance", "wait"),
            "NanoDragon" to listOf("beam", "damfly", "damgrd", "deadg", "deads", "fly", "joy", "land", "lasfly", "lift", "wait", "walk"),
            "NarLily" to listOf("attack", "damege", "die", "laugh", "waitc", "waito", "wake"),
            "PalShark" to listOf("appear", "atackl", "atackr", "damage", "dead", "deadb", "leader", "mihari", "run", "stund", "wakeup", "walk"),
            "PanArms" to listOf("apper", "atack", "damage", "kie", "nodam", "wait", "wait2"),
            "PoisonLily" to listOf("attack", "damege", "die", "laugh", "waitc", "waito", "wake"),
            "RagRappy" to listOf("attack", "damage", "die", "run", "tumble", "wait", "wait2", "wake", "wake2", "walk"),
            "Recon" to listOf("attack", "b", "close", "damage", "dethpose", "open", "start", "wait"),
            "SandRappy" to listOf("attack", "damage", "die", "run", "tumble", "wait", "wait2", "wake", "wake2", "walk"),
            "SavageWolf" to listOf("dams", "deadl", "deadr", "eat", "hoe", "hunt", "okil", "okir", "run", "runb", "sleep", "stdup", "wait", "walk"),
            "SinowGold" to listOf("apper", "backstep", "damage", "death", "f", "sword", "t", "transform", "wait", "walk"),
            "SinowSpigell" to listOf("apper", "backstep", "bareta", "damage", "death", "f", "sword", "t", "transform", "wait", "walk"),
            "SinowZoa" to listOf("apper", "backstep", "bareta", "damage", "death", "f", "sword", "t", "transform", "wait", "walk"),
            "SoDimenian" to listOf("appear", "atackl", "atackr", "damage", "dead", "deadb", "leader", "mihari", "run", "stund", "wakeup", "walk"),
            "StRappy" to listOf("attack", "damage", "die", "run", "tumble", "wait", "wait2", "wake", "wake2", "walk"),
            "UlGibbon" to listOf("attack", "back", "damage", "dead", "fire", "hoe", "run", "wait", "walk"),
            "Yowie" to listOf("dams", "deadl", "deadr", "eat", "hoe", "hunt", "okil", "okir", "run", "runb", "sleep", "stdup", "wait", "walk"),
            "Zu" to listOf("beam", "damfly", "damgrd", "deadg", "deads", "fly", "joy", "land", "lasfly", "lift", "plane", "wait", "walk"),
        )

        fun getAnimations(npcType: NpcType): List<AnimationModel> {
            val key = ANIMATION_FALLBACKS[npcType] ?: npcType.name
            val anims = NPC_ANIMATIONS[key] ?: return emptyList()
            return anims.map { animName ->
                AnimationModel(
                    animName.replaceFirstChar { it.uppercase() },
                    "/npcs/${key}_${animName}.njm",
                )
            }
        }
    }
}
