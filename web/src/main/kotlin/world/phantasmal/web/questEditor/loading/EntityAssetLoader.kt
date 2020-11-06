package world.phantasmal.web.questEditor.loading

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import mu.KotlinLogging
import org.khronos.webgl.ArrayBuffer
import world.phantasmal.core.PwResult
import world.phantasmal.core.Success
import world.phantasmal.lib.Endianness
import world.phantasmal.lib.cursor.Cursor
import world.phantasmal.lib.cursor.cursor
import world.phantasmal.lib.fileFormats.ninja.NinjaModel
import world.phantasmal.lib.fileFormats.ninja.NinjaObject
import world.phantasmal.lib.fileFormats.ninja.parseNj
import world.phantasmal.lib.fileFormats.ninja.parseXj
import world.phantasmal.lib.fileFormats.quest.EntityType
import world.phantasmal.lib.fileFormats.quest.NpcType
import world.phantasmal.lib.fileFormats.quest.ObjectType
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.rendering.conversion.ninjaObjectToVertexData
import world.phantasmal.web.externals.babylon.*
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.obj

private val logger = KotlinLogging.logger {}

class EntityAssetLoader(
    private val scope: CoroutineScope,
    private val assetLoader: AssetLoader,
    private val scene: Scene,
) : DisposableContainer() {
    private val defaultMesh = MeshBuilder.CreateCylinder("Entity", obj {
        diameter = 6.0
        height = 20.0
    }, scene).apply {
        setEnabled(false)
        position = Vector3(0.0, 10.0, 0.0)
    }

    private val meshCache = addDisposable(LoadingCache<Pair<EntityType, Int?>, Mesh>())

    suspend fun loadMesh(type: EntityType, model: Int?): Mesh {
        return meshCache.getOrPut(Pair(type, model)) {
            scope.async {
                try {
                    loadGeometry(type, model)?.let { vertexData ->
                        // TODO: Remove this check when XJ models are parsed.
                        if (vertexData.indices == null || vertexData.indices!!.length == 0) {
                            defaultMesh
                        } else {
                            val mesh = Mesh("${type.uniqueName}${model?.let { "-$it" }}", scene)
                            mesh.setEnabled(false)
                            vertexData.applyToMesh(mesh)
                            mesh
                        }
                    } ?: defaultMesh
                } catch (e: Exception) {
                    logger.error(e) { "Couldn't load mesh for $type (model: $model)." }
                    defaultMesh
                }
            }
        }.await()
    }

    private suspend fun loadGeometry(type: EntityType, model: Int?): VertexData? {
        val geomFormat = entityTypeToGeometryFormat(type)

        val geomParts = geometryParts(type).mapNotNull { suffix ->
            entityTypeToPath(type, AssetType.Geometry, suffix, model, geomFormat)?.let { path ->
                val data = assetLoader.loadArrayBuffer(path)
                Pair(path, data)
            }
        }

        return when (geomFormat) {
            GeomFormat.Nj -> parseGeometry(type, geomParts, ::parseNj)
            GeomFormat.Xj -> parseGeometry(type, geomParts, ::parseXj)
        }
    }

    private fun <Model : NinjaModel> parseGeometry(
        type: EntityType,
        parts: List<Pair<String, ArrayBuffer>>,
        parse: (Cursor) -> PwResult<List<NinjaObject<Model>>>,
    ): VertexData? {
        val njObjects = parts.flatMap { (path, data) ->
            val njObjects = parse(data.cursor(Endianness.Little))

            if (njObjects is Success && njObjects.value.isNotEmpty()) {
                njObjects.value
            } else {
                logger.warn { "Couldn't parse $path for $type." }
                emptyList()
            }
        }

        if (njObjects.isEmpty()) {
            return null
        }

        val njObject = njObjects.first()
        njObject.evaluationFlags.breakChildTrace = false

        for (njObj in njObjects.drop(1)) {
            njObject.addChild(njObj)
        }

        return ninjaObjectToVertexData(njObject)
    }
}

private enum class AssetType {
    Geometry, Texture
}

private enum class GeomFormat {
    Nj, Xj
}

/**
 * Returns the suffix of each geometry part.
 */
private fun geometryParts(type: EntityType): List<String?> =
    when (type) {
        ObjectType.Teleporter -> listOf("", "-2")
        ObjectType.Warp -> listOf("", "-2")
        ObjectType.BossTeleporter -> listOf("", "-2")
        ObjectType.QuestWarp -> listOf("", "-2")
        ObjectType.Epilogue -> listOf("", "-2")
        ObjectType.MainRagolTeleporter -> listOf("", "-2")
        ObjectType.PrincipalWarp -> listOf("", "-2")
        ObjectType.TeleporterDoor -> listOf("", "-2")
        ObjectType.EasterEgg -> listOf("", "-2")
        ObjectType.ValentinesHeart -> listOf("", "-2", "-3")
        ObjectType.ChristmasTree -> listOf("", "-2", "-3", "-4")
        ObjectType.TwentyFirstCentury -> listOf("", "-2")
        ObjectType.WelcomeBoard -> listOf("") // TODO: position part 2 correctly.
        ObjectType.ForestDoor -> listOf("", "-2", "-3", "-4", "-5")
        ObjectType.ForestSwitch -> listOf("", "-2", "-3")
        ObjectType.LaserFence -> listOf("", "-2")
        ObjectType.LaserSquareFence -> listOf("", "-2")
        ObjectType.ForestLaserFenceSwitch -> listOf("", "-2", "-3")
        ObjectType.Probe -> listOf("-0") // TODO: use correct part.
        ObjectType.RandomTypeBox1 -> listOf("-2") // What are the other two parts for?
        ObjectType.BlackSlidingDoor -> listOf("", "-2")
        ObjectType.EnergyBarrier -> listOf("", "-2")
        ObjectType.SwitchNoneDoor -> listOf("", "-2")
        ObjectType.EnemyBoxGrey -> listOf("-2") // What are the other two parts for?
        ObjectType.FixedTypeBox -> listOf("-3") // What are the other three parts for?
        ObjectType.EnemyBoxBrown -> listOf("-3") // What are the other three parts for?
        ObjectType.LaserFenceEx -> listOf("", "-2")
        ObjectType.LaserSquareFenceEx -> listOf("", "-2")
        ObjectType.CavesSmashingPillar -> listOf("", "-3") // What's part 2 for?
        ObjectType.RobotRechargeStation -> listOf("", "-2")
        ObjectType.RuinsTeleporter -> listOf("", "-2", "-3", "-4")
        ObjectType.RuinsWarpSiteToSite -> listOf("", "-2")
        ObjectType.RuinsSwitch -> listOf("", "-2")
        ObjectType.RuinsPillarTrap -> listOf("", "-2", "-3", "-4")
        ObjectType.RuinsCrystal -> listOf("", "-2", "-3")
        ObjectType.FloatingRocks -> listOf("-0")
        ObjectType.ItemBoxCca -> listOf("", "-3") // What are the other two parts for?
        ObjectType.TeleporterEp2 -> listOf("", "-2")
        ObjectType.CcaDoor -> listOf("", "-2")
        ObjectType.SpecialBoxCca -> listOf("", "-4") // What are the other two parts for?
        ObjectType.BigCcaDoor -> listOf("", "-2", "-3", "-4")
        ObjectType.BigCcaDoorSwitch -> listOf("", "-2")
        ObjectType.LaserDetect -> listOf("", "-2") // TODO: use correct part.
        ObjectType.LabCeilingWarp -> listOf("", "-2")
        ObjectType.BigBrownRock -> listOf("-0") // TODO: use correct part.
        ObjectType.BigBlackRocks -> listOf("")
        ObjectType.BeeHive -> listOf("", "-0", "-1")
        else -> listOf(null)
    }

private fun entityTypeToGeometryFormat(type: EntityType): GeomFormat =
    when (type) {
        is NpcType -> {
            when (type) {
                NpcType.Dubswitch,
                NpcType.Dubswitch2,
                -> GeomFormat.Xj

                else -> GeomFormat.Nj
            }
        }
        is ObjectType -> {
            when (type) {
                ObjectType.EasterEgg,
                ObjectType.ChristmasTree,
                ObjectType.ChristmasWreath,
                ObjectType.TwentyFirstCentury,
                ObjectType.Sonic,
                ObjectType.WelcomeBoard,
                ObjectType.FloatingJellyfish,
                ObjectType.RuinsSeal,
                ObjectType.Dolphin,
                ObjectType.Cactus,
                ObjectType.BigBrownRock,
                ObjectType.PoisonPlant,
                ObjectType.BigBlackRocks,
                ObjectType.FallingRock,
                ObjectType.DesertFixedTypeBoxBreakableCrystals,
                ObjectType.BeeHive,
                -> GeomFormat.Nj

                else -> GeomFormat.Xj
            }
        }
        else -> {
            error("$type not supported.")
        }
    }

private fun entityTypeToPath(
    type: EntityType,
    assetType: AssetType,
    suffix: String?,
    model: Int?,
    geomFormat: GeomFormat,
): String? {
    val fullSuffix = when {
        suffix != null -> suffix
        model != null -> "-$model"
        else -> ""
    }

    val extension = when (assetType) {
        AssetType.Geometry -> when (geomFormat) {
            GeomFormat.Nj -> "nj"
            GeomFormat.Xj -> "xj"
        }
        AssetType.Texture -> "xvm"
    }

    return when (type) {
        is NpcType -> {
            when (type) {
                // We don't have a model for these NPCs.
                NpcType.Unknown,
                NpcType.Migium,
                NpcType.Hidoom,
                NpcType.VolOptPart1,
                NpcType.DeathGunner,
                NpcType.StRappy,
                NpcType.HalloRappy,
                NpcType.EggRappy,
                NpcType.Migium2,
                NpcType.Hidoom2,
                NpcType.Recon,
                -> null

                // Episode II VR Temple

                NpcType.Hildebear2 ->
                    entityTypeToPath(NpcType.Hildebear, assetType, suffix, model, geomFormat)
                NpcType.Hildeblue2 ->
                    entityTypeToPath(NpcType.Hildeblue, assetType, suffix, model, geomFormat)
                NpcType.RagRappy2 ->
                    entityTypeToPath(NpcType.RagRappy, assetType, suffix, model, geomFormat)
                NpcType.Monest2 ->
                    entityTypeToPath(NpcType.Monest, assetType, suffix, model, geomFormat)
                NpcType.Mothmant2 ->
                    entityTypeToPath(NpcType.Mothmant, assetType, suffix, model, geomFormat)
                NpcType.PoisonLily2 ->
                    entityTypeToPath(NpcType.PoisonLily, assetType, suffix, model, geomFormat)
                NpcType.NarLily2 ->
                    entityTypeToPath(NpcType.NarLily, assetType, suffix, model, geomFormat)
                NpcType.GrassAssassin2 ->
                    entityTypeToPath(NpcType.GrassAssassin, assetType, suffix, model, geomFormat)
                NpcType.Dimenian2 ->
                    entityTypeToPath(NpcType.Dimenian, assetType, suffix, model, geomFormat)
                NpcType.LaDimenian2 ->
                    entityTypeToPath(NpcType.LaDimenian, assetType, suffix, model, geomFormat)
                NpcType.SoDimenian2 ->
                    entityTypeToPath(NpcType.SoDimenian, assetType, suffix, model, geomFormat)
                NpcType.DarkBelra2 ->
                    entityTypeToPath(NpcType.DarkBelra, assetType, suffix, model, geomFormat)

                // Episode II VR Spaceship

                NpcType.SavageWolf2 ->
                    entityTypeToPath(NpcType.SavageWolf, assetType, suffix, model, geomFormat)
                NpcType.BarbarousWolf2 ->
                    entityTypeToPath(NpcType.BarbarousWolf, assetType, suffix, model, geomFormat)
                NpcType.PanArms2 ->
                    entityTypeToPath(NpcType.PanArms, assetType, suffix, model, geomFormat)
                NpcType.Dubchic2 ->
                    entityTypeToPath(NpcType.Dubchic, assetType, suffix, model, geomFormat)
                NpcType.Gilchic2 ->
                    entityTypeToPath(NpcType.Gilchic, assetType, suffix, model, geomFormat)
                NpcType.Garanz2 ->
                    entityTypeToPath(NpcType.Garanz, assetType, suffix, model, geomFormat)
                NpcType.Dubswitch2 ->
                    entityTypeToPath(NpcType.Dubswitch, assetType, suffix, model, geomFormat)
                NpcType.Delsaber2 ->
                    entityTypeToPath(NpcType.Delsaber, assetType, suffix, model, geomFormat)
                NpcType.ChaosSorcerer2 ->
                    entityTypeToPath(NpcType.ChaosSorcerer, assetType, suffix, model, geomFormat)

                else -> "/npcs/${type.name}${fullSuffix}.$extension"
            }
        }
        is ObjectType -> {
            when (type) {
                // We don't have a model for these objects.
                ObjectType.Unknown,
                ObjectType.PlayerSet,
                ObjectType.Particle,
                ObjectType.LightCollision,
                ObjectType.EnvSound,
                ObjectType.FogCollision,
                ObjectType.EventCollision,
                ObjectType.CharaCollision,
                ObjectType.ObjRoomID,
                ObjectType.LensFlare,
                ObjectType.ScriptCollision,
                ObjectType.MapCollision,
                ObjectType.ScriptCollisionA,
                ObjectType.ItemLight,
                ObjectType.RadarCollision,
                ObjectType.FogCollisionSW,
                ObjectType.ImageBoard,
                ObjectType.UnknownItem29,
                ObjectType.UnknownItem30,
                ObjectType.UnknownItem31,
                ObjectType.MenuActivation,
                ObjectType.BoxDetectObject,
                ObjectType.SymbolChatObject,
                ObjectType.TouchPlateObject,
                ObjectType.TargetableObject,
                ObjectType.EffectObject,
                ObjectType.CountDownObject,
                ObjectType.UnknownItem38,
                ObjectType.UnknownItem39,
                ObjectType.UnknownItem40,
                ObjectType.UnknownItem41,
                ObjectType.TelepipeLocation,
                ObjectType.BGMCollision,
                ObjectType.Pioneer2InvisibleTouchplate,
                ObjectType.TempleMapDetect,
                ObjectType.Firework,
                ObjectType.MainRagolTeleporterBattleInNextArea,
                ObjectType.Rainbow,
                ObjectType.FloatingBlueLight,
                ObjectType.PopupTrapNoTech,
                ObjectType.Poison,
                ObjectType.EnemyTypeBoxYellow,
                ObjectType.EnemyTypeBoxBlue,
                ObjectType.EmptyTypeBoxBlue,
                ObjectType.FloatingSoul,
                ObjectType.Butterfly,
                ObjectType.UnknownItem400,
                ObjectType.CcaAreaTeleporter,
                ObjectType.UnknownItem523,
                ObjectType.WhiteBird,
                ObjectType.OrangeBird,
                ObjectType.UnknownItem529,
                ObjectType.UnknownItem530,
                ObjectType.Seagull,
                ObjectType.UnknownItem576,
                ObjectType.WarpInBarbaRayRoom,
                ObjectType.UnknownItem672,
                ObjectType.InstaWarp,
                ObjectType.LabInvisibleObject,
                ObjectType.UnknownItem700,
                ObjectType.Ep4LightSource,
                ObjectType.BreakableBrownRock,
                ObjectType.UnknownItem897,
                ObjectType.UnknownItem898,
                ObjectType.OozingDesertPlant,
                ObjectType.UnknownItem901,
                ObjectType.UnknownItem903,
                ObjectType.UnknownItem904,
                ObjectType.UnknownItem905,
                ObjectType.UnknownItem906,
                ObjectType.DesertPlantHasCollision,
                ObjectType.UnknownItem910,
                ObjectType.UnknownItem912,
                ObjectType.Heat,
                ObjectType.TopOfSaintMillionEgg,
                ObjectType.UnknownItem961,
                -> null

                else -> {
                    type.typeId?.let { typeId ->
                        "/objects/${typeId}${fullSuffix}.$extension"
                    }
                }
            }
        }
        else -> {
            error("$type not supported.")
        }
    }
}
