import { BufferGeometry, CylinderBufferGeometry, Texture } from "three";
import { LoadingCache } from "./LoadingCache";
import { Endianness } from "../../core/data_formats/block/Endianness";
import { ArrayBufferCursor } from "../../core/data_formats/block/cursor/ArrayBufferCursor";
import { ninja_object_to_buffer_geometry } from "../../core/rendering/conversion/ninja_geometry";
import { NjObject, parse_nj, parse_xj } from "../../core/data_formats/parsing/ninja";
import { parse_xvm } from "../../core/data_formats/parsing/ninja/texture";
import { xvm_to_three_textures } from "../../core/rendering/conversion/ninja_textures";
import { object_data, ObjectType } from "../../core/data_formats/parsing/quest/object_types";
import { NpcType } from "../../core/data_formats/parsing/quest/npc_types";
import {
    entity_type_to_string,
    EntityType,
    is_npc_type,
} from "../../core/data_formats/parsing/quest/Quest";
import { HttpClient } from "../../core/HttpClient";
import { Disposable } from "../../core/observable/Disposable";
import { LogManager } from "../../core/logging";
import { DisposablePromise } from "../../core/DisposablePromise";
import { Disposer } from "../../core/observable/Disposer";

const logger = LogManager.get("quest_editor/loading/EntityAssetLoader");

const DEFAULT_ENTITY = new CylinderBufferGeometry(3, 3, 20);
DEFAULT_ENTITY.translate(0, 10, 0);
DEFAULT_ENTITY.computeBoundingBox();
DEFAULT_ENTITY.computeBoundingSphere();

const DEFAULT_ENTITY_PROMISE = DisposablePromise.resolve<BufferGeometry>(DEFAULT_ENTITY);

const DEFAULT_ENTITY_TEX: Texture[] = [];

const DEFAULT_ENTITY_TEX_PROMISE = DisposablePromise.resolve<Texture[]>(DEFAULT_ENTITY_TEX);

// TODO: load correct parts for entities that can have different geometries.
export class EntityAssetLoader implements Disposable {
    private readonly disposer = new Disposer();
    private readonly geom_cache = this.disposer.add(new LoadingCache<string, BufferGeometry>());
    private readonly tex_cache = this.disposer.add(new LoadingCache<string, Texture[]>());

    constructor(private readonly http_client: HttpClient) {
        this.warm_up_caches();
    }

    dispose(): void {
        this.disposer.dispose();
    }

    load_geometry(type: EntityType, model?: number): DisposablePromise<BufferGeometry> {
        return this.geom_cache.get_or_set(`${type}-${model ?? ""}`, () => {
            return DisposablePromise.all(
                geometry_parts(type).map(no =>
                    this.load_data(type, AssetType.Geometry, no, model)
                        .then(({ url, data }) => {
                            const cursor = new ArrayBufferCursor(data, Endianness.Little);
                            const nj_objects = url.endsWith(".nj")
                                ? parse_nj(cursor)
                                : parse_xj(cursor);

                            if (nj_objects.success && nj_objects.value.length) {
                                return nj_objects.value;
                            } else {
                                logger.warn(
                                    `Couldn't parse ${url} for ${entity_type_to_string(type)}.`,
                                );
                                return [];
                            }
                        })
                        .catch(e => {
                            logger.warn(
                                `Couldn't load geometry file for ${entity_type_to_string(type)}.`,
                                e,
                            );
                            return [];
                        }),
                ),
            ).then((nj_object_arrays: NjObject[][]) => {
                const nj_objects = nj_object_arrays.flat();
                const nj_object = nj_objects[0];

                for (let i = 1; i < nj_objects.length; i++) {
                    nj_object.add_child(nj_objects[i]);
                }

                if (nj_object) {
                    nj_object.evaluation_flags.break_child_trace = false;
                    return ninja_object_to_buffer_geometry(nj_object);
                } else {
                    return DEFAULT_ENTITY;
                }
            });
        });
    }

    load_textures(type: EntityType, model?: number): DisposablePromise<Texture[]> {
        let suffix: string;

        if (
            type === ObjectType.FloatingRocks ||
            (type === ObjectType.BigBrownRock && model == undefined)
        ) {
            suffix = "-0";
        } else {
            suffix = "";
        }

        return this.tex_cache.get_or_set(`${type}-${model ?? ""}`, () =>
            this.load_data(type, AssetType.Texture, suffix, model)
                .then(({ data }) => {
                    const cursor = new ArrayBufferCursor(data, Endianness.Little);
                    const xvm = parse_xvm(cursor);
                    return xvm.success ? xvm_to_three_textures(xvm.value) : [];
                })
                .catch(e => {
                    logger.warn(
                        `Couldn't load texture file for ${entity_type_to_string(type)}.`,
                        e,
                    );
                    return DEFAULT_ENTITY_TEX;
                }),
        );
    }

    private load_data(
        type: EntityType,
        asset_type: AssetType,
        suffix?: string,
        model?: number,
    ): DisposablePromise<{ url: string; data: ArrayBuffer }> {
        const url = entity_type_to_url(type, asset_type, suffix, model);
        return this.http_client
            .get(url)
            .array_buffer()
            .then(data => ({ url, data }));
    }

    /**
     * Warms up the caches with default data for all entities without assets.
     */
    private warm_up_caches(): void {
        for (const type of [
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
        ]) {
            this.geom_cache.set(`${type}-`, DEFAULT_ENTITY_PROMISE);
            this.tex_cache.set(`${type}-`, DEFAULT_ENTITY_TEX_PROMISE);
        }
    }
}

/**
 * @returns the suffix of each geometry part.
 */
function geometry_parts(type: EntityType): (string | undefined)[] {
    switch (type) {
        case ObjectType.Teleporter:
            return ["", "-2"];
        case ObjectType.Warp:
            return ["", "-2"];
        case ObjectType.BossTeleporter:
            return ["", "-2"];
        case ObjectType.QuestWarp:
            return ["", "-2"];
        case ObjectType.Epilogue:
            return ["", "-2"];
        case ObjectType.MainRagolTeleporter:
            return ["", "-2"];
        case ObjectType.PrincipalWarp:
            return ["", "-2"];
        case ObjectType.TeleporterDoor:
            return ["", "-2"];
        case ObjectType.EasterEgg:
            return ["", "-2"];
        case ObjectType.ValentinesHeart:
            return ["", "-2", "-3"];
        case ObjectType.ChristmasTree:
            return ["", "-2", "-3", "-4"];
        case ObjectType.TwentyFirstCentury:
            return ["", "-2"];
        case ObjectType.WelcomeBoard:
            return [""]; // TODO: position part 2 correctly.
        case ObjectType.ForestDoor:
            return ["", "-2", "-3", "-4", "-5"];
        case ObjectType.ForestSwitch:
            return ["", "-2", "-3"];
        case ObjectType.LaserFence:
            return ["", "-2"];
        case ObjectType.LaserSquareFence:
            return ["", "-2"];
        case ObjectType.ForestLaserFenceSwitch:
            return ["", "-2", "-3"];
        case ObjectType.Probe:
            return ["-0"]; // TODO: use correct part.
        case ObjectType.RandomTypeBox1:
            return ["-2"]; // What are the other two parts for?
        case ObjectType.BlackSlidingDoor:
            return ["", "-2"];
        case ObjectType.EnergyBarrier:
            return ["", "-2"];
        case ObjectType.SwitchNoneDoor:
            return ["", "-2"];
        case ObjectType.EnemyBoxGrey:
            return ["-2"]; // What are the other two parts for?
        case ObjectType.FixedTypeBox:
            return ["-3"]; // What are the other three parts for?
        case ObjectType.EnemyBoxBrown:
            return ["-3"]; // What are the other three parts for?
        case ObjectType.LaserFenceEx:
            return ["", "-2"];
        case ObjectType.LaserSquareFenceEx:
            return ["", "-2"];
        case ObjectType.CavesSmashingPillar:
            return ["", "-3"]; // What's part 2 for?
        case ObjectType.RobotRechargeStation:
            return ["", "-2"];
        case ObjectType.RuinsTeleporter:
            return ["", "-2", "-3", "-4"];
        case ObjectType.RuinsWarpSiteToSite:
            return ["", "-2"];
        case ObjectType.RuinsSwitch:
            return ["", "-2"];
        case ObjectType.RuinsPillarTrap:
            return ["", "-2", "-3", "-4"];
        case ObjectType.RuinsCrystal:
            return ["", "-2", "-3"];
        case ObjectType.FloatingRocks:
            return ["-0"];
        case ObjectType.ItemBoxCca:
            return ["", "-3"]; // What are the other two parts for?
        case ObjectType.TeleporterEp2:
            return ["", "-2"];
        case ObjectType.CcaDoor:
            return ["", "-2"];
        case ObjectType.SpecialBoxCca:
            return ["", "-4"]; // What are the other two parts for?
        case ObjectType.BigCcaDoor:
            return ["", "-2", "-3", "-4"];
        case ObjectType.BigCcaDoorSwitch:
            return ["", "-2"];
        case ObjectType.LaserDetect:
            return ["", "-2"]; // TODO: use correct part.
        case ObjectType.LabCeilingWarp:
            return ["", "-2"];
        case ObjectType.BigBrownRock:
            return ["-0"]; // TODO: use correct part.
        case ObjectType.BigBlackRocks:
            return [""];
        case ObjectType.BeeHive:
            return ["", "-0", "-1"];
        default:
            return [undefined];
    }
}

enum AssetType {
    Geometry,
    Texture,
}

/**
 * @param type
 * @param asset_type
 * @param suffix - Asset suffix. Some entities have multiple assets that need to be combined.
 * @param model - Model variant (e.g. Sonic, Tails, Knuckles or Doctor Eggman).
 */
function entity_type_to_url(
    type: EntityType,
    asset_type: AssetType,
    suffix?: string,
    model?: number,
): string {
    let full_suffix: string;

    if (suffix != undefined) {
        full_suffix = suffix;
    } else if (model != undefined) {
        full_suffix = `-${model}`;
    } else {
        full_suffix = "";
    }

    if (is_npc_type(type)) {
        switch (type) {
            // The dubswitch model is in XJ format.
            case NpcType.Dubswitch:
                return `/npcs/${NpcType[type]}.${asset_type === AssetType.Geometry ? "xj" : "xvm"}`;

            // Episode II VR Temple

            case NpcType.Hildebear2:
                return entity_type_to_url(NpcType.Hildebear, asset_type, suffix, model);
            case NpcType.Hildeblue2:
                return entity_type_to_url(NpcType.Hildeblue, asset_type, suffix, model);
            case NpcType.RagRappy2:
                return entity_type_to_url(NpcType.RagRappy, asset_type, suffix, model);
            case NpcType.Monest2:
                return entity_type_to_url(NpcType.Monest, asset_type, suffix, model);
            case NpcType.Mothmant2:
                return entity_type_to_url(NpcType.Mothmant, asset_type, suffix, model);
            case NpcType.PoisonLily2:
                return entity_type_to_url(NpcType.PoisonLily, asset_type, suffix, model);
            case NpcType.NarLily2:
                return entity_type_to_url(NpcType.NarLily, asset_type, suffix, model);
            case NpcType.GrassAssassin2:
                return entity_type_to_url(NpcType.GrassAssassin, asset_type, suffix, model);
            case NpcType.Dimenian2:
                return entity_type_to_url(NpcType.Dimenian, asset_type, suffix, model);
            case NpcType.LaDimenian2:
                return entity_type_to_url(NpcType.LaDimenian, asset_type, suffix, model);
            case NpcType.SoDimenian2:
                return entity_type_to_url(NpcType.SoDimenian, asset_type, suffix, model);
            case NpcType.DarkBelra2:
                return entity_type_to_url(NpcType.DarkBelra, asset_type, suffix, model);

            // Episode II VR Spaceship

            case NpcType.SavageWolf2:
                return entity_type_to_url(NpcType.SavageWolf, asset_type, suffix, model);
            case NpcType.BarbarousWolf2:
                return entity_type_to_url(NpcType.BarbarousWolf, asset_type, suffix, model);
            case NpcType.PanArms2:
                return entity_type_to_url(NpcType.PanArms, asset_type, suffix, model);
            case NpcType.Dubchic2:
                return entity_type_to_url(NpcType.Dubchic, asset_type, suffix, model);
            case NpcType.Gilchic2:
                return entity_type_to_url(NpcType.Gilchic, asset_type, suffix, model);
            case NpcType.Garanz2:
                return entity_type_to_url(NpcType.Garanz, asset_type, suffix, model);
            case NpcType.Dubswitch2:
                return entity_type_to_url(NpcType.Dubswitch, asset_type, suffix, model);
            case NpcType.Delsaber2:
                return entity_type_to_url(NpcType.Delsaber, asset_type, suffix, model);
            case NpcType.ChaosSorcerer2:
                return entity_type_to_url(NpcType.ChaosSorcerer, asset_type, suffix, model);

            default:
                return `/npcs/${NpcType[type]}${full_suffix}.${
                    asset_type === AssetType.Geometry ? "nj" : "xvm"
                }`;
        }
    } else {
        if (asset_type === AssetType.Geometry) {
            switch (type) {
                case ObjectType.EasterEgg:
                case ObjectType.ChristmasTree:
                case ObjectType.ChristmasWreath:
                case ObjectType.TwentyFirstCentury:
                case ObjectType.Sonic:
                case ObjectType.WelcomeBoard:
                case ObjectType.FloatingJellyfish:
                case ObjectType.RuinsSeal:
                case ObjectType.Dolphin:
                case ObjectType.Cactus:
                case ObjectType.BigBrownRock:
                case ObjectType.PoisonPlant:
                case ObjectType.BigBlackRocks:
                case ObjectType.FallingRock:
                case ObjectType.DesertFixedTypeBoxBreakableCrystals:
                case ObjectType.BeeHive:
                    return `/objects/${object_data(type).type_id}${full_suffix}.nj`;

                default:
                    return `/objects/${object_data(type).type_id}${full_suffix}.xj`;
            }
        } else {
            return `/objects/${object_data(type).type_id}${full_suffix}.xvm`;
        }
    }
}
