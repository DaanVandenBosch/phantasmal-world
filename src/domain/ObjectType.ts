import { EntityType } from ".";

export class ObjectType implements EntityType {
    readonly id: number;
    readonly code: string;
    readonly pso_id?: number;
    readonly name: string;

    constructor(id: number, code: string, pso_id: number | undefined, name: string) {
        if (!Number.isInteger(id) || id < 1)
            throw new Error(`Expected id to be an integer greater than or equal to 1, got ${id}.`);
        if (!code) throw new Error("code is required.");
        if (pso_id != null && (!Number.isInteger(pso_id) || pso_id < 0))
            throw new Error(
                `Expected pso_id to be null or an integer greater than or equal to 0, got ${pso_id}.`
            );
        if (!name) throw new Error("name is required.");

        this.id = id;
        this.code = code;
        this.pso_id = pso_id;
        this.name = name;
    }

    static Unknown: ObjectType;
    static PlayerSet: ObjectType;
    static Particle: ObjectType;
    static Teleporter: ObjectType;
    static Warp: ObjectType;
    static LightCollision: ObjectType;
    static Item: ObjectType;
    static EnvSound: ObjectType;
    static FogCollision: ObjectType;
    static EventCollision: ObjectType;
    static CharaCollision: ObjectType;
    static ElementalTrap: ObjectType;
    static StatusTrap: ObjectType;
    static HealTrap: ObjectType;
    static LargeElementalTrap: ObjectType;
    static ObjRoomID: ObjectType;
    static Sensor: ObjectType;
    static UnknownItem16: ObjectType;
    static Lensflare: ObjectType;
    static ScriptCollision: ObjectType;
    static HealRing: ObjectType;
    static MapCollision: ObjectType;
    static ScriptCollisionA: ObjectType;
    static ItemLight: ObjectType;
    static RadarCollision: ObjectType;
    static FogCollisionSW: ObjectType;
    static BossTeleporter: ObjectType;
    static ImageBoard: ObjectType;
    static QuestWarp: ObjectType;
    static Epilogue: ObjectType;
    static UnknownItem29: ObjectType;
    static UnknownItem30: ObjectType;
    static UnknownItem31: ObjectType;
    static BoxDetectObject: ObjectType;
    static SymbolChatObject: ObjectType;
    static TouchPlateObject: ObjectType;
    static TargetableObject: ObjectType;
    static EffectObject: ObjectType;
    static CountDownObject: ObjectType;
    static UnknownItem38: ObjectType;
    static UnknownItem39: ObjectType;
    static UnknownItem40: ObjectType;
    static UnknownItem41: ObjectType;
    static MenuActivation: ObjectType;
    static TelepipeLocation: ObjectType;
    static BGMCollision: ObjectType;
    static MainRagolTeleporter: ObjectType;
    static LobbyTeleporter: ObjectType;
    static PrincipalWarp: ObjectType;
    static ShopDoor: ObjectType;
    static HuntersGuildDoor: ObjectType;
    static TeleporterDoor: ObjectType;
    static MedicalCenterDoor: ObjectType;
    static Elevator: ObjectType;
    static EasterEgg: ObjectType;
    static ValentinesHeart: ObjectType;
    static ChristmasTree: ObjectType;
    static ChristmasWreath: ObjectType;
    static HalloweenPumpkin: ObjectType;
    static TwentyFirstCentury: ObjectType;
    static Sonic: ObjectType;
    static WelcomeBoard: ObjectType;
    static Firework: ObjectType;
    static LobbyScreenDoor: ObjectType;
    static MainRagolTeleporterBattleInNextArea: ObjectType;
    static LabTeleporterDoor: ObjectType;
    static Pioneer2InvisibleTouchplate: ObjectType;
    static ForestDoor: ObjectType;
    static ForestSwitch: ObjectType;
    static LaserFence: ObjectType;
    static LaserSquareFence: ObjectType;
    static ForestLaserFenceSwitch: ObjectType;
    static LightRays: ObjectType;
    static BlueButterfly: ObjectType;
    static Probe: ObjectType;
    static RandomTypeBox1: ObjectType;
    static ForestWeatherStation: ObjectType;
    static Battery: ObjectType;
    static ForestConsole: ObjectType;
    static BlackSlidingDoor: ObjectType;
    static RicoMessagePod: ObjectType;
    static EnergyBarrier: ObjectType;
    static ForestRisingBridge: ObjectType;
    static SwitchNoneDoor: ObjectType;
    static EnemyBoxGrey: ObjectType;
    static FixedTypeBox: ObjectType;
    static EnemyBoxBrown: ObjectType;
    static EmptyTypeBox: ObjectType;
    static LaserFenseEx: ObjectType;
    static LaserSquareFenceEx: ObjectType;
    static FloorPanel1: ObjectType;
    static Caves4ButtonDoor: ObjectType;
    static CavesNormalDoor: ObjectType;
    static CavesSmashingPillar: ObjectType;
    static CavesSign1: ObjectType;
    static CavesSign2: ObjectType;
    static CavesSign3: ObjectType;
    static HexagalTank: ObjectType;
    static BrownPlatform: ObjectType;
    static WarningLightObject: ObjectType;
    static Rainbow: ObjectType;
    static FloatingJelifish: ObjectType;
    static FloatingDragonfly: ObjectType;
    static CavesSwitchDoor: ObjectType;
    static RobotRechargeStation: ObjectType;
    static CavesCakeShop: ObjectType;
    static Caves1SmallRedRock: ObjectType;
    static Caves1MediumRedRock: ObjectType;
    static Caves1LargeRedRock: ObjectType;
    static Caves2SmallRock1: ObjectType;
    static Caves2MediumRock1: ObjectType;
    static Caves2LargeRock1: ObjectType;
    static Caves2SmallRock2: ObjectType;
    static Caves2MediumRock2: ObjectType;
    static Caves2LargeRock2: ObjectType;
    static Caves3SmallRock: ObjectType;
    static Caves3MediumRock: ObjectType;
    static Caves3LargeRock: ObjectType;
    static FloorPanel2: ObjectType;
    static DestructableRockCaves1: ObjectType;
    static DestructableRockCaves2: ObjectType;
    static DestructableRockCaves3: ObjectType;
    static MinesDoor: ObjectType;
    static FloorPanel3: ObjectType;
    static MinesSwitchDoor: ObjectType;
    static LargeCryoTube: ObjectType;
    static ComputerLikeCalus: ObjectType;
    static GreenScreenOpeningAndClosing: ObjectType;
    static FloatingRobot: ObjectType;
    static FloatingBlueLight: ObjectType;
    static SelfDestructingObject1: ObjectType;
    static SelfDestructingObject2: ObjectType;
    static SelfDestructingObject3: ObjectType;
    static SparkMachine: ObjectType;
    static MinesLargeFlashingCrate: ObjectType;
    static RuinsSeal: ObjectType;
    static RuinsTeleporter: ObjectType;
    static RuinsWarpSiteToSite: ObjectType;
    static RuinsSwitch: ObjectType;
    static FloorPanel4: ObjectType;
    static Ruins1Door: ObjectType;
    static Ruins3Door: ObjectType;
    static Ruins2Door: ObjectType;
    static Ruins11ButtonDoor: ObjectType;
    static Ruins21ButtonDoor: ObjectType;
    static Ruins31ButtonDoor: ObjectType;
    static Ruins4ButtonDoor: ObjectType;
    static Ruins2ButtonDoor: ObjectType;
    static RuinsSensor: ObjectType;
    static RuinsFenceSwitch: ObjectType;
    static RuinsLaserFence4x2: ObjectType;
    static RuinsLaserFence6x2: ObjectType;
    static RuinsLaserFence4x4: ObjectType;
    static RuinsLaserFence6x4: ObjectType;
    static RuinsPoisonBlob: ObjectType;
    static RuinsPilarTrap: ObjectType;
    static PopupTrapNoTech: ObjectType;
    static RuinsCrystal: ObjectType;
    static Monument: ObjectType;
    static RuinsRock1: ObjectType;
    static RuinsRock2: ObjectType;
    static RuinsRock3: ObjectType;
    static RuinsRock4: ObjectType;
    static RuinsRock5: ObjectType;
    static RuinsRock6: ObjectType;
    static RuinsRock7: ObjectType;
    static Poison: ObjectType;
    static FixedBoxTypeRuins: ObjectType;
    static RandomBoxTypeRuins: ObjectType;
    static EnemyTypeBoxYellow: ObjectType;
    static EnemyTypeBoxBlue: ObjectType;
    static EmptyTypeBoxBlue: ObjectType;
    static DestructableRock: ObjectType;
    static PopupTrapsTechs: ObjectType;
    static FlyingWhiteBird: ObjectType;
    static Tower: ObjectType;
    static FloatingRocks: ObjectType;
    static FloatingSoul: ObjectType;
    static Butterfly: ObjectType;
    static LobbyGameMenu: ObjectType;
    static LobbyWarpObject: ObjectType;
    static Lobby1EventObjectDefaultTree: ObjectType;
    static UnknownItem387: ObjectType;
    static UnknownItem388: ObjectType;
    static UnknownItem389: ObjectType;
    static LobbyEventObjectStaticPumpkin: ObjectType;
    static LobbyEventObject3ChristmasWindows: ObjectType;
    static LobbyEventObjectRedAndWhiteCurtain: ObjectType;
    static UnknownItem393: ObjectType;
    static UnknownItem394: ObjectType;
    static LobbyFishTank: ObjectType;
    static LobbyEventObjectButterflies: ObjectType;
    static UnknownItem400: ObjectType;
    static GreyWallLow: ObjectType;
    static SpaceshipDoor: ObjectType;
    static GreyWallHigh: ObjectType;
    static TempleNormalDoor: ObjectType;
    static BreakableWallWallButUnbreakable: ObjectType;
    static BrokenCilinderAndRubble: ObjectType;
    static ThreeBrokenWallPiecesOnFloor: ObjectType;
    static HighBrickCilinder: ObjectType;
    static LyingCilinder: ObjectType;
    static BrickConeWithFlatTop: ObjectType;
    static BreakableTempleWall: ObjectType;
    static TempleMapDetect: ObjectType;
    static SmallBrownBrickRisingBridge: ObjectType;
    static LongRisingBridgeWithPinkHighEdges: ObjectType;
    static FourSwitchTempleDoor: ObjectType;
    static FourButtonSpaceshipDoor: ObjectType;
    static ItemBoxCca: ObjectType;
    static TeleporterEp2: ObjectType;
    static CCADoor: ObjectType;
    static SpecialBoxCCA: ObjectType;
    static BigCCADoor: ObjectType;
    static BigCCADoorSwitch: ObjectType;
    static LittleRock: ObjectType;
    static Little3StoneWall: ObjectType;
    static Medium3StoneWall: ObjectType;
    static SpiderPlant: ObjectType;
    static CCAAreaTeleporter: ObjectType;
    static UnknownItem523: ObjectType;
    static WhiteBird: ObjectType;
    static OrangeBird: ObjectType;
    static Saw: ObjectType;
    static LaserDetect: ObjectType;
    static UnknownItem529: ObjectType;
    static UnknownItem530: ObjectType;
    static Seagull: ObjectType;
    static Fish: ObjectType;
    static SeabedDoorWithBlueEdges: ObjectType;
    static SeabedDoorAlwaysOpenNonTriggerable: ObjectType;
    static LittleCryotube: ObjectType;
    static WideGlassWallBreakable: ObjectType;
    static BlueFloatingRobot: ObjectType;
    static RedFloatingRobot: ObjectType;
    static Dolphin: ObjectType;
    static CaptureTrap: ObjectType;
    static VRLink: ObjectType;
    static UnknownItem576: ObjectType;
    static WarpInBarbaRayRoom: ObjectType;
    static UnknownItem672: ObjectType;
    static GeeNest: ObjectType;
    static LabComputerConsole: ObjectType;
    static LabComputerConsoleGreenScreen: ObjectType;
    static ChairYelllowPillow: ObjectType;
    static OrangeWallWithHoleInMiddle: ObjectType;
    static GreyWallWithHoleInMiddle: ObjectType;
    static LongTable: ObjectType;
    static GBAStation: ObjectType;
    static TalkLinkToSupport: ObjectType;
    static InstaWarp: ObjectType;
    static LabInvisibleObject: ObjectType;
    static LabGlassWindowDoor: ObjectType;
    static UnknownItem700: ObjectType;
    static LabCelingWarp: ObjectType;
    static Ep4LightSource: ObjectType;
    static Cacti: ObjectType;
    static BigBrownRock: ObjectType;
    static BreakableBrownRock: ObjectType;
    static UnknownItem832: ObjectType;
    static UnknownItem833: ObjectType;
    static PoisonPlant: ObjectType;
    static UnknownItem897: ObjectType;
    static UnknownItem898: ObjectType;
    static OozingDesertPlant: ObjectType;
    static UnknownItem901: ObjectType;
    static BigBlackRocks: ObjectType;
    static UnknownItem903: ObjectType;
    static UnknownItem904: ObjectType;
    static UnknownItem905: ObjectType;
    static UnknownItem906: ObjectType;
    static FallingRock: ObjectType;
    static DesertPlantHasCollision: ObjectType;
    static DesertFixedTypeBoxBreakableCrystals: ObjectType;
    static UnknownItem910: ObjectType;
    static BeeHive: ObjectType;
    static UnknownItem912: ObjectType;
    static Heat: ObjectType;
    static TopOfSaintMillionEgg: ObjectType;
    static UnknownItem961: ObjectType;

    static from_pso_id(psoId: number): ObjectType {
        switch (psoId) {
            default:
                return ObjectType.Unknown;

            case 0:
                return ObjectType.PlayerSet;
            case 1:
                return ObjectType.Particle;
            case 2:
                return ObjectType.Teleporter;
            case 3:
                return ObjectType.Warp;
            case 4:
                return ObjectType.LightCollision;
            case 5:
                return ObjectType.Item;
            case 6:
                return ObjectType.EnvSound;
            case 7:
                return ObjectType.FogCollision;
            case 8:
                return ObjectType.EventCollision;
            case 9:
                return ObjectType.CharaCollision;
            case 10:
                return ObjectType.ElementalTrap;
            case 11:
                return ObjectType.StatusTrap;
            case 12:
                return ObjectType.HealTrap;
            case 13:
                return ObjectType.LargeElementalTrap;
            case 14:
                return ObjectType.ObjRoomID;
            case 15:
                return ObjectType.Sensor;
            case 16:
                return ObjectType.UnknownItem16;
            case 17:
                return ObjectType.Lensflare;
            case 18:
                return ObjectType.ScriptCollision;
            case 19:
                return ObjectType.HealRing;
            case 20:
                return ObjectType.MapCollision;
            case 21:
                return ObjectType.ScriptCollisionA;
            case 22:
                return ObjectType.ItemLight;
            case 23:
                return ObjectType.RadarCollision;
            case 24:
                return ObjectType.FogCollisionSW;
            case 25:
                return ObjectType.BossTeleporter;
            case 26:
                return ObjectType.ImageBoard;
            case 27:
                return ObjectType.QuestWarp;
            case 28:
                return ObjectType.Epilogue;
            case 29:
                return ObjectType.UnknownItem29;
            case 30:
                return ObjectType.UnknownItem30;
            case 31:
                return ObjectType.UnknownItem31;
            case 32:
                return ObjectType.BoxDetectObject;
            case 33:
                return ObjectType.SymbolChatObject;
            case 34:
                return ObjectType.TouchPlateObject;
            case 35:
                return ObjectType.TargetableObject;
            case 36:
                return ObjectType.EffectObject;
            case 37:
                return ObjectType.CountDownObject;
            case 38:
                return ObjectType.UnknownItem38;
            case 39:
                return ObjectType.UnknownItem39;
            case 40:
                return ObjectType.UnknownItem40;
            case 41:
                return ObjectType.UnknownItem41;
            case 64:
                return ObjectType.MenuActivation;
            case 65:
                return ObjectType.TelepipeLocation;
            case 66:
                return ObjectType.BGMCollision;
            case 67:
                return ObjectType.MainRagolTeleporter;
            case 68:
                return ObjectType.LobbyTeleporter;
            case 69:
                return ObjectType.PrincipalWarp;
            case 70:
                return ObjectType.ShopDoor;
            case 71:
                return ObjectType.HuntersGuildDoor;
            case 72:
                return ObjectType.TeleporterDoor;
            case 73:
                return ObjectType.MedicalCenterDoor;
            case 74:
                return ObjectType.Elevator;
            case 75:
                return ObjectType.EasterEgg;
            case 76:
                return ObjectType.ValentinesHeart;
            case 77:
                return ObjectType.ChristmasTree;
            case 78:
                return ObjectType.ChristmasWreath;
            case 79:
                return ObjectType.HalloweenPumpkin;
            case 80:
                return ObjectType.TwentyFirstCentury;
            case 81:
                return ObjectType.Sonic;
            case 82:
                return ObjectType.WelcomeBoard;
            case 83:
                return ObjectType.Firework;
            case 84:
                return ObjectType.LobbyScreenDoor;
            case 85:
                return ObjectType.MainRagolTeleporterBattleInNextArea;
            case 86:
                return ObjectType.LabTeleporterDoor;
            case 87:
                return ObjectType.Pioneer2InvisibleTouchplate;
            case 128:
                return ObjectType.ForestDoor;
            case 129:
                return ObjectType.ForestSwitch;
            case 130:
                return ObjectType.LaserFence;
            case 131:
                return ObjectType.LaserSquareFence;
            case 132:
                return ObjectType.ForestLaserFenceSwitch;
            case 133:
                return ObjectType.LightRays;
            case 134:
                return ObjectType.BlueButterfly;
            case 135:
                return ObjectType.Probe;
            case 136:
                return ObjectType.RandomTypeBox1;
            case 137:
                return ObjectType.ForestWeatherStation;
            case 138:
                return ObjectType.Battery;
            case 139:
                return ObjectType.ForestConsole;
            case 140:
                return ObjectType.BlackSlidingDoor;
            case 141:
                return ObjectType.RicoMessagePod;
            case 142:
                return ObjectType.EnergyBarrier;
            case 143:
                return ObjectType.ForestRisingBridge;
            case 144:
                return ObjectType.SwitchNoneDoor;
            case 145:
                return ObjectType.EnemyBoxGrey;
            case 146:
                return ObjectType.FixedTypeBox;
            case 147:
                return ObjectType.EnemyBoxBrown;
            case 149:
                return ObjectType.EmptyTypeBox;
            case 150:
                return ObjectType.LaserFenseEx;
            case 151:
                return ObjectType.LaserSquareFenceEx;
            case 192:
                return ObjectType.FloorPanel1;
            case 193:
                return ObjectType.Caves4ButtonDoor;
            case 194:
                return ObjectType.CavesNormalDoor;
            case 195:
                return ObjectType.CavesSmashingPillar;
            case 196:
                return ObjectType.CavesSign1;
            case 197:
                return ObjectType.CavesSign2;
            case 198:
                return ObjectType.CavesSign3;
            case 199:
                return ObjectType.HexagalTank;
            case 200:
                return ObjectType.BrownPlatform;
            case 201:
                return ObjectType.WarningLightObject;
            case 203:
                return ObjectType.Rainbow;
            case 204:
                return ObjectType.FloatingJelifish;
            case 205:
                return ObjectType.FloatingDragonfly;
            case 206:
                return ObjectType.CavesSwitchDoor;
            case 207:
                return ObjectType.RobotRechargeStation;
            case 208:
                return ObjectType.CavesCakeShop;
            case 209:
                return ObjectType.Caves1SmallRedRock;
            case 210:
                return ObjectType.Caves1MediumRedRock;
            case 211:
                return ObjectType.Caves1LargeRedRock;
            case 212:
                return ObjectType.Caves2SmallRock1;
            case 213:
                return ObjectType.Caves2MediumRock1;
            case 214:
                return ObjectType.Caves2LargeRock1;
            case 215:
                return ObjectType.Caves2SmallRock2;
            case 216:
                return ObjectType.Caves2MediumRock2;
            case 217:
                return ObjectType.Caves2LargeRock2;
            case 218:
                return ObjectType.Caves3SmallRock;
            case 219:
                return ObjectType.Caves3MediumRock;
            case 220:
                return ObjectType.Caves3LargeRock;
            case 222:
                return ObjectType.FloorPanel2;
            case 223:
                return ObjectType.DestructableRockCaves1;
            case 224:
                return ObjectType.DestructableRockCaves2;
            case 225:
                return ObjectType.DestructableRockCaves3;
            case 256:
                return ObjectType.MinesDoor;
            case 257:
                return ObjectType.FloorPanel3;
            case 258:
                return ObjectType.MinesSwitchDoor;
            case 259:
                return ObjectType.LargeCryoTube;
            case 260:
                return ObjectType.ComputerLikeCalus;
            case 261:
                return ObjectType.GreenScreenOpeningAndClosing;
            case 262:
                return ObjectType.FloatingRobot;
            case 263:
                return ObjectType.FloatingBlueLight;
            case 264:
                return ObjectType.SelfDestructingObject1;
            case 265:
                return ObjectType.SelfDestructingObject2;
            case 266:
                return ObjectType.SelfDestructingObject3;
            case 267:
                return ObjectType.SparkMachine;
            case 268:
                return ObjectType.MinesLargeFlashingCrate;
            case 304:
                return ObjectType.RuinsSeal;
            case 320:
                return ObjectType.RuinsTeleporter;
            case 321:
                return ObjectType.RuinsWarpSiteToSite;
            case 322:
                return ObjectType.RuinsSwitch;
            case 323:
                return ObjectType.FloorPanel4;
            case 324:
                return ObjectType.Ruins1Door;
            case 325:
                return ObjectType.Ruins3Door;
            case 326:
                return ObjectType.Ruins2Door;
            case 327:
                return ObjectType.Ruins11ButtonDoor;
            case 328:
                return ObjectType.Ruins21ButtonDoor;
            case 329:
                return ObjectType.Ruins31ButtonDoor;
            case 330:
                return ObjectType.Ruins4ButtonDoor;
            case 331:
                return ObjectType.Ruins2ButtonDoor;
            case 332:
                return ObjectType.RuinsSensor;
            case 333:
                return ObjectType.RuinsFenceSwitch;
            case 334:
                return ObjectType.RuinsLaserFence4x2;
            case 335:
                return ObjectType.RuinsLaserFence6x2;
            case 336:
                return ObjectType.RuinsLaserFence4x4;
            case 337:
                return ObjectType.RuinsLaserFence6x4;
            case 338:
                return ObjectType.RuinsPoisonBlob;
            case 339:
                return ObjectType.RuinsPilarTrap;
            case 340:
                return ObjectType.PopupTrapNoTech;
            case 341:
                return ObjectType.RuinsCrystal;
            case 342:
                return ObjectType.Monument;
            case 345:
                return ObjectType.RuinsRock1;
            case 346:
                return ObjectType.RuinsRock2;
            case 347:
                return ObjectType.RuinsRock3;
            case 348:
                return ObjectType.RuinsRock4;
            case 349:
                return ObjectType.RuinsRock5;
            case 350:
                return ObjectType.RuinsRock6;
            case 351:
                return ObjectType.RuinsRock7;
            case 352:
                return ObjectType.Poison;
            case 353:
                return ObjectType.FixedBoxTypeRuins;
            case 354:
                return ObjectType.RandomBoxTypeRuins;
            case 355:
                return ObjectType.EnemyTypeBoxYellow;
            case 356:
                return ObjectType.EnemyTypeBoxBlue;
            case 357:
                return ObjectType.EmptyTypeBoxBlue;
            case 358:
                return ObjectType.DestructableRock;
            case 359:
                return ObjectType.PopupTrapsTechs;
            case 368:
                return ObjectType.FlyingWhiteBird;
            case 369:
                return ObjectType.Tower;
            case 370:
                return ObjectType.FloatingRocks;
            case 371:
                return ObjectType.FloatingSoul;
            case 372:
                return ObjectType.Butterfly;
            case 384:
                return ObjectType.LobbyGameMenu;
            case 385:
                return ObjectType.LobbyWarpObject;
            case 386:
                return ObjectType.Lobby1EventObjectDefaultTree;
            case 387:
                return ObjectType.UnknownItem387;
            case 388:
                return ObjectType.UnknownItem388;
            case 389:
                return ObjectType.UnknownItem389;
            case 390:
                return ObjectType.LobbyEventObjectStaticPumpkin;
            case 391:
                return ObjectType.LobbyEventObject3ChristmasWindows;
            case 392:
                return ObjectType.LobbyEventObjectRedAndWhiteCurtain;
            case 393:
                return ObjectType.UnknownItem393;
            case 394:
                return ObjectType.UnknownItem394;
            case 395:
                return ObjectType.LobbyFishTank;
            case 396:
                return ObjectType.LobbyEventObjectButterflies;
            case 400:
                return ObjectType.UnknownItem400;
            case 401:
                return ObjectType.GreyWallLow;
            case 402:
                return ObjectType.SpaceshipDoor;
            case 403:
                return ObjectType.GreyWallHigh;
            case 416:
                return ObjectType.TempleNormalDoor;
            case 417:
                return ObjectType.BreakableWallWallButUnbreakable;
            case 418:
                return ObjectType.BrokenCilinderAndRubble;
            case 419:
                return ObjectType.ThreeBrokenWallPiecesOnFloor;
            case 420:
                return ObjectType.HighBrickCilinder;
            case 421:
                return ObjectType.LyingCilinder;
            case 422:
                return ObjectType.BrickConeWithFlatTop;
            case 423:
                return ObjectType.BreakableTempleWall;
            case 424:
                return ObjectType.TempleMapDetect;
            case 425:
                return ObjectType.SmallBrownBrickRisingBridge;
            case 426:
                return ObjectType.LongRisingBridgeWithPinkHighEdges;
            case 427:
                return ObjectType.FourSwitchTempleDoor;
            case 448:
                return ObjectType.FourButtonSpaceshipDoor;
            case 512:
                return ObjectType.ItemBoxCca;
            case 513:
                return ObjectType.TeleporterEp2;
            case 514:
                return ObjectType.CCADoor;
            case 515:
                return ObjectType.SpecialBoxCCA;
            case 516:
                return ObjectType.BigCCADoor;
            case 517:
                return ObjectType.BigCCADoorSwitch;
            case 518:
                return ObjectType.LittleRock;
            case 519:
                return ObjectType.Little3StoneWall;
            case 520:
                return ObjectType.Medium3StoneWall;
            case 521:
                return ObjectType.SpiderPlant;
            case 522:
                return ObjectType.CCAAreaTeleporter;
            case 523:
                return ObjectType.UnknownItem523;
            case 524:
                return ObjectType.WhiteBird;
            case 525:
                return ObjectType.OrangeBird;
            case 527:
                return ObjectType.Saw;
            case 528:
                return ObjectType.LaserDetect;
            case 529:
                return ObjectType.UnknownItem529;
            case 530:
                return ObjectType.UnknownItem530;
            case 531:
                return ObjectType.Seagull;
            case 544:
                return ObjectType.Fish;
            case 545:
                return ObjectType.SeabedDoorWithBlueEdges;
            case 546:
                return ObjectType.SeabedDoorAlwaysOpenNonTriggerable;
            case 547:
                return ObjectType.LittleCryotube;
            case 548:
                return ObjectType.WideGlassWallBreakable;
            case 549:
                return ObjectType.BlueFloatingRobot;
            case 550:
                return ObjectType.RedFloatingRobot;
            case 551:
                return ObjectType.Dolphin;
            case 552:
                return ObjectType.CaptureTrap;
            case 553:
                return ObjectType.VRLink;
            case 576:
                return ObjectType.UnknownItem576;
            case 640:
                return ObjectType.WarpInBarbaRayRoom;
            case 672:
                return ObjectType.UnknownItem672;
            case 688:
                return ObjectType.GeeNest;
            case 689:
                return ObjectType.LabComputerConsole;
            case 690:
                return ObjectType.LabComputerConsoleGreenScreen;
            case 691:
                return ObjectType.ChairYelllowPillow;
            case 692:
                return ObjectType.OrangeWallWithHoleInMiddle;
            case 693:
                return ObjectType.GreyWallWithHoleInMiddle;
            case 694:
                return ObjectType.LongTable;
            case 695:
                return ObjectType.GBAStation;
            case 696:
                return ObjectType.TalkLinkToSupport;
            case 697:
                return ObjectType.InstaWarp;
            case 698:
                return ObjectType.LabInvisibleObject;
            case 699:
                return ObjectType.LabGlassWindowDoor;
            case 700:
                return ObjectType.UnknownItem700;
            case 701:
                return ObjectType.LabCelingWarp;
            case 768:
                return ObjectType.Ep4LightSource;
            case 769:
                return ObjectType.Cacti;
            case 770:
                return ObjectType.BigBrownRock;
            case 771:
                return ObjectType.BreakableBrownRock;
            case 832:
                return ObjectType.UnknownItem832;
            case 833:
                return ObjectType.UnknownItem833;
            case 896:
                return ObjectType.PoisonPlant;
            case 897:
                return ObjectType.UnknownItem897;
            case 898:
                return ObjectType.UnknownItem898;
            case 899:
                return ObjectType.OozingDesertPlant;
            case 901:
                return ObjectType.UnknownItem901;
            case 902:
                return ObjectType.BigBlackRocks;
            case 903:
                return ObjectType.UnknownItem903;
            case 904:
                return ObjectType.UnknownItem904;
            case 905:
                return ObjectType.UnknownItem905;
            case 906:
                return ObjectType.UnknownItem906;
            case 907:
                return ObjectType.FallingRock;
            case 908:
                return ObjectType.DesertPlantHasCollision;
            case 909:
                return ObjectType.DesertFixedTypeBoxBreakableCrystals;
            case 910:
                return ObjectType.UnknownItem910;
            case 911:
                return ObjectType.BeeHive;
            case 912:
                return ObjectType.UnknownItem912;
            case 913:
                return ObjectType.Heat;
            case 960:
                return ObjectType.TopOfSaintMillionEgg;
            case 961:
                return ObjectType.UnknownItem961;
        }
    }
}

(function() {
    let id = 1;

    ObjectType.Unknown = new ObjectType(id++, "Unknown", undefined, "Unknown");

    ObjectType.PlayerSet = new ObjectType(id++, "PlayerSet", 0, "Player Set");
    ObjectType.Particle = new ObjectType(id++, "Particle", 1, "Particle");
    ObjectType.Teleporter = new ObjectType(id++, "Teleporter", 2, "Teleporter");
    ObjectType.Warp = new ObjectType(id++, "Warp", 3, "Warp");
    ObjectType.LightCollision = new ObjectType(id++, "LightCollision", 4, "Light Collision");
    ObjectType.Item = new ObjectType(id++, "Item", 5, "Item");
    ObjectType.EnvSound = new ObjectType(id++, "EnvSound", 6, "Env Sound");
    ObjectType.FogCollision = new ObjectType(id++, "FogCollision", 7, "Fog Collision");
    ObjectType.EventCollision = new ObjectType(id++, "EventCollision", 8, "Event Collision");
    ObjectType.CharaCollision = new ObjectType(id++, "CharaCollision", 9, "Chara Collision");
    ObjectType.ElementalTrap = new ObjectType(id++, "ElementalTrap", 10, "Elemental Trap");
    ObjectType.StatusTrap = new ObjectType(id++, "StatusTrap", 11, "Status Trap");
    ObjectType.HealTrap = new ObjectType(id++, "HealTrap", 12, "Heal Trap");
    ObjectType.LargeElementalTrap = new ObjectType(
        id++,
        "LargeElementalTrap",
        13,
        "Large Elemental Trap"
    );
    ObjectType.ObjRoomID = new ObjectType(id++, "ObjRoomID", 14, "Obj Room ID");
    ObjectType.Sensor = new ObjectType(id++, "Sensor", 15, "Sensor");
    ObjectType.UnknownItem16 = new ObjectType(id++, "UnknownItem16", 16, "Unknown Item (16)");
    ObjectType.Lensflare = new ObjectType(id++, "Lensflare", 17, "Lensflare");
    ObjectType.ScriptCollision = new ObjectType(id++, "ScriptCollision", 18, "Script Collision");
    ObjectType.HealRing = new ObjectType(id++, "HealRing", 19, "Heal Ring");
    ObjectType.MapCollision = new ObjectType(id++, "MapCollision", 20, "Map Collision");
    ObjectType.ScriptCollisionA = new ObjectType(
        id++,
        "ScriptCollisionA",
        21,
        "Script Collision A"
    );
    ObjectType.ItemLight = new ObjectType(id++, "ItemLight", 22, "Item Light");
    ObjectType.RadarCollision = new ObjectType(id++, "RadarCollision", 23, "Radar Collision");
    ObjectType.FogCollisionSW = new ObjectType(id++, "FogCollisionSW", 24, "Fog Collision SW");
    ObjectType.BossTeleporter = new ObjectType(id++, "BossTeleporter", 25, "Boss Teleporter");
    ObjectType.ImageBoard = new ObjectType(id++, "ImageBoard", 26, "Image Board");
    ObjectType.QuestWarp = new ObjectType(id++, "QuestWarp", 27, "Quest Warp");
    ObjectType.Epilogue = new ObjectType(id++, "Epilogue", 28, "Epilogue");
    ObjectType.UnknownItem29 = new ObjectType(id++, "UnknownItem29", 29, "Unknown Item (29)");
    ObjectType.UnknownItem30 = new ObjectType(id++, "UnknownItem30", 30, "Unknown Item (30)");
    ObjectType.UnknownItem31 = new ObjectType(id++, "UnknownItem31", 31, "Unknown Item (31)");
    ObjectType.BoxDetectObject = new ObjectType(id++, "BoxDetectObject", 32, "Box Detect Object");
    ObjectType.SymbolChatObject = new ObjectType(
        id++,
        "SymbolChatObject",
        33,
        "Symbol Chat Object"
    );
    ObjectType.TouchPlateObject = new ObjectType(
        id++,
        "TouchPlateObject",
        34,
        "Touch plate Object"
    );
    ObjectType.TargetableObject = new ObjectType(id++, "TargetableObject", 35, "Targetable Object");
    ObjectType.EffectObject = new ObjectType(id++, "EffectObject", 36, "Effect object");
    ObjectType.CountDownObject = new ObjectType(id++, "CountDownObject", 37, "Count Down Object");
    ObjectType.UnknownItem38 = new ObjectType(id++, "UnknownItem38", 38, "Unknown Item (38)");
    ObjectType.UnknownItem39 = new ObjectType(id++, "UnknownItem39", 39, "Unknown Item (39)");
    ObjectType.UnknownItem40 = new ObjectType(id++, "UnknownItem40", 40, "Unknown Item (40)");
    ObjectType.UnknownItem41 = new ObjectType(id++, "UnknownItem41", 41, "Unknown Item (41)");
    ObjectType.MenuActivation = new ObjectType(id++, "MenuActivation", 64, "Menu activation");
    ObjectType.TelepipeLocation = new ObjectType(id++, "TelepipeLocation", 65, "Telepipe Location");
    ObjectType.BGMCollision = new ObjectType(id++, "BGMCollision", 66, "BGM Collision");
    ObjectType.MainRagolTeleporter = new ObjectType(
        id++,
        "MainRagolTeleporter",
        67,
        "Main Ragol Teleporter"
    );
    ObjectType.LobbyTeleporter = new ObjectType(id++, "LobbyTeleporter", 68, "Lobby Teleporter");
    ObjectType.PrincipalWarp = new ObjectType(id++, "PrincipalWarp", 69, "Principal warp");
    ObjectType.ShopDoor = new ObjectType(id++, "ShopDoor", 70, "Shop Door");
    ObjectType.HuntersGuildDoor = new ObjectType(
        id++,
        "HuntersGuildDoor",
        71,
        "Hunter's Guild Door"
    );
    ObjectType.TeleporterDoor = new ObjectType(id++, "TeleporterDoor", 72, "Teleporter Door");
    ObjectType.MedicalCenterDoor = new ObjectType(
        id++,
        "MedicalCenterDoor",
        73,
        "Medical Center Door"
    );
    ObjectType.Elevator = new ObjectType(id++, "Elevator", 74, "Elevator");
    ObjectType.EasterEgg = new ObjectType(id++, "EasterEgg", 75, "Easter Egg");
    ObjectType.ValentinesHeart = new ObjectType(id++, "ValentinesHeart", 76, "Valentines Heart");
    ObjectType.ChristmasTree = new ObjectType(id++, "ChristmasTree", 77, "Christmas Tree");
    ObjectType.ChristmasWreath = new ObjectType(id++, "ChristmasWreath", 78, "Christmas Wreath");
    ObjectType.HalloweenPumpkin = new ObjectType(id++, "HalloweenPumpkin", 79, "Halloween Pumpkin");
    ObjectType.TwentyFirstCentury = new ObjectType(id++, "TwentyFirstCentury", 80, "21st Century");
    ObjectType.Sonic = new ObjectType(id++, "Sonic", 81, "Sonic");
    ObjectType.WelcomeBoard = new ObjectType(id++, "WelcomeBoard", 82, "Welcome Board");
    ObjectType.Firework = new ObjectType(id++, "Firework", 83, "Firework");
    ObjectType.LobbyScreenDoor = new ObjectType(id++, "LobbyScreenDoor", 84, "Lobby Screen Door");
    ObjectType.MainRagolTeleporterBattleInNextArea = new ObjectType(
        id++,
        "MainRagolTeleporterBattleInNextArea",
        85,
        "Main Ragol Teleporter (Battle in next area?)"
    );
    ObjectType.LabTeleporterDoor = new ObjectType(
        id++,
        "LabTeleporterDoor",
        86,
        "Lab Teleporter Door"
    );
    ObjectType.Pioneer2InvisibleTouchplate = new ObjectType(
        id++,
        "Pioneer2InvisibleTouchplate",
        87,
        "Pioneer 2 Invisible Touchplate"
    );
    ObjectType.ForestDoor = new ObjectType(id++, "ForestDoor", 128, "Forest Door");
    ObjectType.ForestSwitch = new ObjectType(id++, "ForestSwitch", 129, "Forest Switch");
    ObjectType.LaserFence = new ObjectType(id++, "LaserFence", 130, "Laser Fence");
    ObjectType.LaserSquareFence = new ObjectType(
        id++,
        "LaserSquareFence",
        131,
        "Laser Square Fence"
    );
    ObjectType.ForestLaserFenceSwitch = new ObjectType(
        id++,
        "ForestLaserFenceSwitch",
        132,
        "Forest Laser Fence Switch"
    );
    ObjectType.LightRays = new ObjectType(id++, "LightRays", 133, "Light rays");
    ObjectType.BlueButterfly = new ObjectType(id++, "BlueButterfly", 134, "Blue Butterfly");
    ObjectType.Probe = new ObjectType(id++, "Probe", 135, "Probe");
    ObjectType.RandomTypeBox1 = new ObjectType(id++, "RandomTypeBox1", 136, "Random Type Box 1");
    ObjectType.ForestWeatherStation = new ObjectType(
        id++,
        "ForestWeatherStation",
        137,
        "Forest Weather Station"
    );
    ObjectType.Battery = new ObjectType(id++, "Battery", 138, "Battery");
    ObjectType.ForestConsole = new ObjectType(id++, "ForestConsole", 139, "Forest Console");
    ObjectType.BlackSlidingDoor = new ObjectType(
        id++,
        "BlackSlidingDoor",
        140,
        "Black Sliding Door"
    );
    ObjectType.RicoMessagePod = new ObjectType(id++, "RicoMessagePod", 141, "Rico Message Pod");
    ObjectType.EnergyBarrier = new ObjectType(id++, "EnergyBarrier", 142, "Energy Barrier");
    ObjectType.ForestRisingBridge = new ObjectType(
        id++,
        "ForestRisingBridge",
        143,
        "Forest Rising Bridge"
    );
    ObjectType.SwitchNoneDoor = new ObjectType(id++, "SwitchNoneDoor", 144, "Switch (none door)");
    ObjectType.EnemyBoxGrey = new ObjectType(id++, "EnemyBoxGrey", 145, "Enemy Box (Grey)");
    ObjectType.FixedTypeBox = new ObjectType(id++, "FixedTypeBox", 146, "Fixed Type Box");
    ObjectType.EnemyBoxBrown = new ObjectType(id++, "EnemyBoxBrown", 147, "Enemy Box (Brown)");
    ObjectType.EmptyTypeBox = new ObjectType(id++, "EmptyTypeBox", 149, "Empty Type Box");
    ObjectType.LaserFenseEx = new ObjectType(id++, "LaserFenseEx", 150, "Laser Fense Ex");
    ObjectType.LaserSquareFenceEx = new ObjectType(
        id++,
        "LaserSquareFenceEx",
        151,
        "Laser Square Fence Ex"
    );
    ObjectType.FloorPanel1 = new ObjectType(id++, "FloorPanel1", 192, "Floor Panel 1");
    ObjectType.Caves4ButtonDoor = new ObjectType(
        id++,
        "Caves4ButtonDoor",
        193,
        "Caves 4 Button door"
    );
    ObjectType.CavesNormalDoor = new ObjectType(id++, "CavesNormalDoor", 194, "Caves Normal door");
    ObjectType.CavesSmashingPillar = new ObjectType(
        id++,
        "CavesSmashingPillar",
        195,
        "Caves Smashing Pillar"
    );
    ObjectType.CavesSign1 = new ObjectType(id++, "CavesSign1", 196, "Caves Sign 1");
    ObjectType.CavesSign2 = new ObjectType(id++, "CavesSign2", 197, "Caves Sign 2");
    ObjectType.CavesSign3 = new ObjectType(id++, "CavesSign3", 198, "Caves Sign 3");
    ObjectType.HexagalTank = new ObjectType(id++, "HexagalTank", 199, "Hexagal Tank");
    ObjectType.BrownPlatform = new ObjectType(id++, "BrownPlatform", 200, "Brown Platform");
    ObjectType.WarningLightObject = new ObjectType(
        id++,
        "WarningLightObject",
        201,
        "Warning Light Object"
    );
    ObjectType.Rainbow = new ObjectType(id++, "Rainbow", 203, "Rainbow");
    ObjectType.FloatingJelifish = new ObjectType(
        id++,
        "FloatingJelifish",
        204,
        "Floating Jelifish"
    );
    ObjectType.FloatingDragonfly = new ObjectType(
        id++,
        "FloatingDragonfly",
        205,
        "Floating Dragonfly"
    );
    ObjectType.CavesSwitchDoor = new ObjectType(id++, "CavesSwitchDoor", 206, "Caves Switch Door");
    ObjectType.RobotRechargeStation = new ObjectType(
        id++,
        "RobotRechargeStation",
        207,
        "Robot Recharge Station"
    );
    ObjectType.CavesCakeShop = new ObjectType(id++, "CavesCakeShop", 208, "Caves Cake Shop");
    ObjectType.Caves1SmallRedRock = new ObjectType(
        id++,
        "Caves1SmallRedRock",
        209,
        "Caves 1 Small Red Rock"
    );
    ObjectType.Caves1MediumRedRock = new ObjectType(
        id++,
        "Caves1MediumRedRock",
        210,
        "Caves 1 Medium Red Rock"
    );
    ObjectType.Caves1LargeRedRock = new ObjectType(
        id++,
        "Caves1LargeRedRock",
        211,
        "Caves 1 Large Red Rock"
    );
    ObjectType.Caves2SmallRock1 = new ObjectType(
        id++,
        "Caves2SmallRock1",
        212,
        "Caves 2 Small Rock 1"
    );
    ObjectType.Caves2MediumRock1 = new ObjectType(
        id++,
        "Caves2MediumRock1",
        213,
        "Caves 2 Medium Rock 1"
    );
    ObjectType.Caves2LargeRock1 = new ObjectType(
        id++,
        "Caves2LargeRock1",
        214,
        "Caves 2 Large Rock 1"
    );
    ObjectType.Caves2SmallRock2 = new ObjectType(
        id++,
        "Caves2SmallRock2",
        215,
        "Caves 2 Small Rock 2"
    );
    ObjectType.Caves2MediumRock2 = new ObjectType(
        id++,
        "Caves2MediumRock2",
        216,
        "Caves 2 Medium Rock 2"
    );
    ObjectType.Caves2LargeRock2 = new ObjectType(
        id++,
        "Caves2LargeRock2",
        217,
        "Caves 2 Large Rock 2"
    );
    ObjectType.Caves3SmallRock = new ObjectType(id++, "Caves3SmallRock", 218, "Caves 3 Small Rock");
    ObjectType.Caves3MediumRock = new ObjectType(
        id++,
        "Caves3MediumRock",
        219,
        "Caves 3 Medium Rock"
    );
    ObjectType.Caves3LargeRock = new ObjectType(id++, "Caves3LargeRock", 220, "Caves 3 Large Rock");
    ObjectType.FloorPanel2 = new ObjectType(id++, "FloorPanel2", 222, "Floor Panel 2");
    ObjectType.DestructableRockCaves1 = new ObjectType(
        id++,
        "DestructableRockCaves1",
        223,
        "Destructable Rock (Caves 1)"
    );
    ObjectType.DestructableRockCaves2 = new ObjectType(
        id++,
        "DestructableRockCaves2",
        224,
        "Destructable Rock (Caves 2)"
    );
    ObjectType.DestructableRockCaves3 = new ObjectType(
        id++,
        "DestructableRockCaves3",
        225,
        "Destructable Rock (Caves 3)"
    );
    ObjectType.MinesDoor = new ObjectType(id++, "MinesDoor", 256, "Mines Door");
    ObjectType.FloorPanel3 = new ObjectType(id++, "FloorPanel3", 257, "Floor Panel 3");
    ObjectType.MinesSwitchDoor = new ObjectType(id++, "MinesSwitchDoor", 258, "Mines Switch Door");
    ObjectType.LargeCryoTube = new ObjectType(id++, "LargeCryoTube", 259, "Large Cryo-Tube");
    ObjectType.ComputerLikeCalus = new ObjectType(
        id++,
        "ComputerLikeCalus",
        260,
        "Computer (like calus)"
    );
    ObjectType.GreenScreenOpeningAndClosing = new ObjectType(
        id++,
        "GreenScreenOpeningAndClosing",
        261,
        "Green Screen opening and closing"
    );
    ObjectType.FloatingRobot = new ObjectType(id++, "FloatingRobot", 262, "Floating Robot");
    ObjectType.FloatingBlueLight = new ObjectType(
        id++,
        "FloatingBlueLight",
        263,
        "Floating Blue Light"
    );
    ObjectType.SelfDestructingObject1 = new ObjectType(
        id++,
        "SelfDestructingObject1",
        264,
        "Self Destructing Object 1"
    );
    ObjectType.SelfDestructingObject2 = new ObjectType(
        id++,
        "SelfDestructingObject2",
        265,
        "Self Destructing Object 2"
    );
    ObjectType.SelfDestructingObject3 = new ObjectType(
        id++,
        "SelfDestructingObject3",
        266,
        "Self Destructing Object 3"
    );
    ObjectType.SparkMachine = new ObjectType(id++, "SparkMachine", 267, "Spark Machine");
    ObjectType.MinesLargeFlashingCrate = new ObjectType(
        id++,
        "MinesLargeFlashingCrate",
        268,
        "Mines Large Flashing Crate"
    );
    ObjectType.RuinsSeal = new ObjectType(id++, "RuinsSeal", 304, "Ruins Seal");
    ObjectType.RuinsTeleporter = new ObjectType(id++, "RuinsTeleporter", 320, "Ruins Teleporter");
    ObjectType.RuinsWarpSiteToSite = new ObjectType(
        id++,
        "RuinsWarpSiteToSite",
        321,
        "Ruins Warp (Site to site)"
    );
    ObjectType.RuinsSwitch = new ObjectType(id++, "RuinsSwitch", 322, "Ruins Switch");
    ObjectType.FloorPanel4 = new ObjectType(id++, "FloorPanel4", 323, "Floor Panel 4");
    ObjectType.Ruins1Door = new ObjectType(id++, "Ruins1Door", 324, "Ruins 1 Door");
    ObjectType.Ruins3Door = new ObjectType(id++, "Ruins3Door", 325, "Ruins 3 Door");
    ObjectType.Ruins2Door = new ObjectType(id++, "Ruins2Door", 326, "Ruins 2 Door");
    ObjectType.Ruins11ButtonDoor = new ObjectType(
        id++,
        "Ruins11ButtonDoor",
        327,
        "Ruins 1-1 Button Door"
    );
    ObjectType.Ruins21ButtonDoor = new ObjectType(
        id++,
        "Ruins21ButtonDoor",
        328,
        "Ruins 2-1 Button Door"
    );
    ObjectType.Ruins31ButtonDoor = new ObjectType(
        id++,
        "Ruins31ButtonDoor",
        329,
        "Ruins 3-1 Button Door"
    );
    ObjectType.Ruins4ButtonDoor = new ObjectType(
        id++,
        "Ruins4ButtonDoor",
        330,
        "Ruins 4-Button Door"
    );
    ObjectType.Ruins2ButtonDoor = new ObjectType(
        id++,
        "Ruins2ButtonDoor",
        331,
        "Ruins 2-Button Door"
    );
    ObjectType.RuinsSensor = new ObjectType(id++, "RuinsSensor", 332, "Ruins Sensor");
    ObjectType.RuinsFenceSwitch = new ObjectType(
        id++,
        "RuinsFenceSwitch",
        333,
        "Ruins Fence Switch"
    );
    ObjectType.RuinsLaserFence4x2 = new ObjectType(
        id++,
        "RuinsLaserFence4x2",
        334,
        "Ruins Laser Fence 4x2"
    );
    ObjectType.RuinsLaserFence6x2 = new ObjectType(
        id++,
        "RuinsLaserFence6x2",
        335,
        "Ruins Laser Fence 6x2"
    );
    ObjectType.RuinsLaserFence4x4 = new ObjectType(
        id++,
        "RuinsLaserFence4x4",
        336,
        "Ruins Laser Fence 4x4"
    );
    ObjectType.RuinsLaserFence6x4 = new ObjectType(
        id++,
        "RuinsLaserFence6x4",
        337,
        "Ruins Laser Fence 6x4"
    );
    ObjectType.RuinsPoisonBlob = new ObjectType(id++, "RuinsPoisonBlob", 338, "Ruins poison Blob");
    ObjectType.RuinsPilarTrap = new ObjectType(id++, "RuinsPilarTrap", 339, "Ruins Pilar Trap");
    ObjectType.PopupTrapNoTech = new ObjectType(
        id++,
        "PopupTrapNoTech",
        340,
        "Popup Trap (No Tech)"
    );
    ObjectType.RuinsCrystal = new ObjectType(id++, "RuinsCrystal", 341, "Ruins Crystal");
    ObjectType.Monument = new ObjectType(id++, "Monument", 342, "Monument");
    ObjectType.RuinsRock1 = new ObjectType(id++, "RuinsRock1", 345, "Ruins Rock 1");
    ObjectType.RuinsRock2 = new ObjectType(id++, "RuinsRock2", 346, "Ruins Rock 2");
    ObjectType.RuinsRock3 = new ObjectType(id++, "RuinsRock3", 347, "Ruins Rock 3");
    ObjectType.RuinsRock4 = new ObjectType(id++, "RuinsRock4", 348, "Ruins Rock 4");
    ObjectType.RuinsRock5 = new ObjectType(id++, "RuinsRock5", 349, "Ruins Rock 5");
    ObjectType.RuinsRock6 = new ObjectType(id++, "RuinsRock6", 350, "Ruins Rock 6");
    ObjectType.RuinsRock7 = new ObjectType(id++, "RuinsRock7", 351, "Ruins Rock 7");
    ObjectType.Poison = new ObjectType(id++, "Poison", 352, "Poison");
    ObjectType.FixedBoxTypeRuins = new ObjectType(
        id++,
        "FixedBoxTypeRuins",
        353,
        "Fixed Box Type (Ruins)"
    );
    ObjectType.RandomBoxTypeRuins = new ObjectType(
        id++,
        "RandomBoxTypeRuins",
        354,
        "Random Box Type (Ruins)"
    );
    ObjectType.EnemyTypeBoxYellow = new ObjectType(
        id++,
        "EnemyTypeBoxYellow",
        355,
        "Enemy Type Box (Yellow)"
    );
    ObjectType.EnemyTypeBoxBlue = new ObjectType(
        id++,
        "EnemyTypeBoxBlue",
        356,
        "Enemy Type Box (Blue)"
    );
    ObjectType.EmptyTypeBoxBlue = new ObjectType(
        id++,
        "EmptyTypeBoxBlue",
        357,
        "Empty Type Box (Blue)"
    );
    ObjectType.DestructableRock = new ObjectType(
        id++,
        "DestructableRock",
        358,
        "Destructable Rock"
    );
    ObjectType.PopupTrapsTechs = new ObjectType(
        id++,
        "PopupTrapsTechs",
        359,
        "Popup Traps (techs)"
    );
    ObjectType.FlyingWhiteBird = new ObjectType(id++, "FlyingWhiteBird", 368, "Flying White Bird");
    ObjectType.Tower = new ObjectType(id++, "Tower", 369, "Tower");
    ObjectType.FloatingRocks = new ObjectType(id++, "FloatingRocks", 370, "Floating Rocks");
    ObjectType.FloatingSoul = new ObjectType(id++, "FloatingSoul", 371, "Floating Soul");
    ObjectType.Butterfly = new ObjectType(id++, "Butterfly", 372, "Butterfly");
    ObjectType.LobbyGameMenu = new ObjectType(id++, "LobbyGameMenu", 384, "Lobby Game menu");
    ObjectType.LobbyWarpObject = new ObjectType(id++, "LobbyWarpObject", 385, "Lobby Warp Object");
    ObjectType.Lobby1EventObjectDefaultTree = new ObjectType(
        id++,
        "Lobby1EventObjectDefaultTree",
        386,
        "Lobby 1 Event Object (Default Tree)"
    );
    ObjectType.UnknownItem387 = new ObjectType(id++, "UnknownItem387", 387, "Unknown Item (387)");
    ObjectType.UnknownItem388 = new ObjectType(id++, "UnknownItem388", 388, "Unknown Item (388)");
    ObjectType.UnknownItem389 = new ObjectType(id++, "UnknownItem389", 389, "Unknown Item (389)");
    ObjectType.LobbyEventObjectStaticPumpkin = new ObjectType(
        id++,
        "LobbyEventObjectStaticPumpkin",
        390,
        "Lobby Event Object (Static Pumpkin)"
    );
    ObjectType.LobbyEventObject3ChristmasWindows = new ObjectType(
        id++,
        "LobbyEventObject3ChristmasWindows",
        391,
        "Lobby Event Object (3 Christmas Windows)"
    );
    ObjectType.LobbyEventObjectRedAndWhiteCurtain = new ObjectType(
        id++,
        "LobbyEventObjectRedAndWhiteCurtain",
        392,
        "Lobby Event Object (Red and White Curtain)"
    );
    ObjectType.UnknownItem393 = new ObjectType(id++, "UnknownItem393", 393, "Unknown Item (393)");
    ObjectType.UnknownItem394 = new ObjectType(id++, "UnknownItem394", 394, "Unknown Item (394)");
    ObjectType.LobbyFishTank = new ObjectType(id++, "LobbyFishTank", 395, "Lobby Fish Tank");
    ObjectType.LobbyEventObjectButterflies = new ObjectType(
        id++,
        "LobbyEventObjectButterflies",
        396,
        "Lobby Event Object (Butterflies)"
    );
    ObjectType.UnknownItem400 = new ObjectType(id++, "UnknownItem400", 400, "Unknown Item (400)");
    ObjectType.GreyWallLow = new ObjectType(id++, "GreyWallLow", 401, "grey wall low");
    ObjectType.SpaceshipDoor = new ObjectType(id++, "SpaceshipDoor", 402, "Spaceship Door");
    ObjectType.GreyWallHigh = new ObjectType(id++, "GreyWallHigh", 403, "grey wall high");
    ObjectType.TempleNormalDoor = new ObjectType(
        id++,
        "TempleNormalDoor",
        416,
        "Temple Normal Door"
    );
    ObjectType.BreakableWallWallButUnbreakable = new ObjectType(
        id++,
        "BreakableWallWallButUnbreakable",
        417,
        '"breakable wall wall, but unbreakable"'
    );
    ObjectType.BrokenCilinderAndRubble = new ObjectType(
        id++,
        "BrokenCilinderAndRubble",
        418,
        "Broken cilinder and rubble"
    );
    ObjectType.ThreeBrokenWallPiecesOnFloor = new ObjectType(
        id++,
        "ThreeBrokenWallPiecesOnFloor",
        419,
        "3 broken wall pieces on floor"
    );
    ObjectType.HighBrickCilinder = new ObjectType(
        id++,
        "HighBrickCilinder",
        420,
        "high brick cilinder"
    );
    ObjectType.LyingCilinder = new ObjectType(id++, "LyingCilinder", 421, "lying cilinder");
    ObjectType.BrickConeWithFlatTop = new ObjectType(
        id++,
        "BrickConeWithFlatTop",
        422,
        "brick cone with flat top"
    );
    ObjectType.BreakableTempleWall = new ObjectType(
        id++,
        "BreakableTempleWall",
        423,
        "breakable temple wall"
    );
    ObjectType.TempleMapDetect = new ObjectType(id++, "TempleMapDetect", 424, "Temple Map Detect");
    ObjectType.SmallBrownBrickRisingBridge = new ObjectType(
        id++,
        "SmallBrownBrickRisingBridge",
        425,
        "small brown brick rising bridge"
    );
    ObjectType.LongRisingBridgeWithPinkHighEdges = new ObjectType(
        id++,
        "LongRisingBridgeWithPinkHighEdges",
        426,
        "long rising bridge (with pink high edges)"
    );
    ObjectType.FourSwitchTempleDoor = new ObjectType(
        id++,
        "FourSwitchTempleDoor",
        427,
        "4 switch temple door"
    );
    ObjectType.FourButtonSpaceshipDoor = new ObjectType(
        id++,
        "FourButtonSpaceshipDoor",
        448,
        "4 button spaceship door"
    );
    ObjectType.ItemBoxCca = new ObjectType(id++, "ItemBoxCca", 512, "item box cca");
    ObjectType.TeleporterEp2 = new ObjectType(id++, "TeleporterEp2", 513, "Teleporter (Ep 2)");
    ObjectType.CCADoor = new ObjectType(id++, "CCADoor", 514, "CCA Door");
    ObjectType.SpecialBoxCCA = new ObjectType(id++, "SpecialBoxCCA", 515, "Special Box CCA");
    ObjectType.BigCCADoor = new ObjectType(id++, "BigCCADoor", 516, "Big CCA Door");
    ObjectType.BigCCADoorSwitch = new ObjectType(
        id++,
        "BigCCADoorSwitch",
        517,
        "Big CCA Door Switch"
    );
    ObjectType.LittleRock = new ObjectType(id++, "LittleRock", 518, "Little Rock");
    ObjectType.Little3StoneWall = new ObjectType(
        id++,
        "Little3StoneWall",
        519,
        "Little 3 Stone Wall"
    );
    ObjectType.Medium3StoneWall = new ObjectType(
        id++,
        "Medium3StoneWall",
        520,
        "Medium 3 stone wall"
    );
    ObjectType.SpiderPlant = new ObjectType(id++, "SpiderPlant", 521, "Spider Plant");
    ObjectType.CCAAreaTeleporter = new ObjectType(
        id++,
        "CCAAreaTeleporter",
        522,
        "CCA Area Teleporter"
    );
    ObjectType.UnknownItem523 = new ObjectType(id++, "UnknownItem523", 523, "Unknown Item (523)");
    ObjectType.WhiteBird = new ObjectType(id++, "WhiteBird", 524, "White Bird");
    ObjectType.OrangeBird = new ObjectType(id++, "OrangeBird", 525, "Orange Bird");
    ObjectType.Saw = new ObjectType(id++, "Saw", 527, "Saw");
    ObjectType.LaserDetect = new ObjectType(id++, "LaserDetect", 528, "Laser Detect");
    ObjectType.UnknownItem529 = new ObjectType(id++, "UnknownItem529", 529, "Unknown Item (529)");
    ObjectType.UnknownItem530 = new ObjectType(id++, "UnknownItem530", 530, "Unknown Item (530)");
    ObjectType.Seagull = new ObjectType(id++, "Seagull", 531, "Seagull");
    ObjectType.Fish = new ObjectType(id++, "Fish", 544, "Fish");
    ObjectType.SeabedDoorWithBlueEdges = new ObjectType(
        id++,
        "SeabedDoorWithBlueEdges",
        545,
        "Seabed Door (with blue edges)"
    );
    ObjectType.SeabedDoorAlwaysOpenNonTriggerable = new ObjectType(
        id++,
        "SeabedDoorAlwaysOpenNonTriggerable",
        546,
        "Seabed door (always open, non-triggerable)"
    );
    ObjectType.LittleCryotube = new ObjectType(id++, "LittleCryotube", 547, "Little Cryotube");
    ObjectType.WideGlassWallBreakable = new ObjectType(
        id++,
        "WideGlassWallBreakable",
        548,
        "Wide Glass Wall (breakable)"
    );
    ObjectType.BlueFloatingRobot = new ObjectType(
        id++,
        "BlueFloatingRobot",
        549,
        "Blue floating robot"
    );
    ObjectType.RedFloatingRobot = new ObjectType(
        id++,
        "RedFloatingRobot",
        550,
        "Red floating robot"
    );
    ObjectType.Dolphin = new ObjectType(id++, "Dolphin", 551, "Dolphin");
    ObjectType.CaptureTrap = new ObjectType(id++, "CaptureTrap", 552, "Capture Trap");
    ObjectType.VRLink = new ObjectType(id++, "VRLink", 553, "VR link");
    ObjectType.UnknownItem576 = new ObjectType(id++, "UnknownItem576", 576, "Unknown Item (576)");
    ObjectType.WarpInBarbaRayRoom = new ObjectType(
        id++,
        "WarpInBarbaRayRoom",
        640,
        "Warp in Barba Ray Room"
    );
    ObjectType.UnknownItem672 = new ObjectType(id++, "UnknownItem672", 672, "Unknown Item (672)");
    ObjectType.GeeNest = new ObjectType(id++, "GeeNest", 688, "Gee Nest");
    ObjectType.LabComputerConsole = new ObjectType(
        id++,
        "LabComputerConsole",
        689,
        "Lab Computer Console"
    );
    ObjectType.LabComputerConsoleGreenScreen = new ObjectType(
        id++,
        "LabComputerConsoleGreenScreen",
        690,
        "Lab Computer Console (Green Screen)"
    );
    ObjectType.ChairYelllowPillow = new ObjectType(
        id++,
        "ChairYelllowPillow",
        691,
        "Chair, Yelllow Pillow"
    );
    ObjectType.OrangeWallWithHoleInMiddle = new ObjectType(
        id++,
        "OrangeWallWithHoleInMiddle",
        692,
        "orange wall with hole in middle"
    );
    ObjectType.GreyWallWithHoleInMiddle = new ObjectType(
        id++,
        "GreyWallWithHoleInMiddle",
        693,
        "grey wall with hole in middle"
    );
    ObjectType.LongTable = new ObjectType(id++, "LongTable", 694, "long table");
    ObjectType.GBAStation = new ObjectType(id++, "GBAStation", 695, "GBA Station");
    ObjectType.TalkLinkToSupport = new ObjectType(
        id++,
        "TalkLinkToSupport",
        696,
        "Talk (link to support)"
    );
    ObjectType.InstaWarp = new ObjectType(id++, "InstaWarp", 697, "insta-warp");
    ObjectType.LabInvisibleObject = new ObjectType(
        id++,
        "LabInvisibleObject",
        698,
        "Lab Invisible Object"
    );
    ObjectType.LabGlassWindowDoor = new ObjectType(
        id++,
        "LabGlassWindowDoor",
        699,
        "Lab Glass window Door"
    );
    ObjectType.UnknownItem700 = new ObjectType(id++, "UnknownItem700", 700, "Unknown Item (700)");
    ObjectType.LabCelingWarp = new ObjectType(id++, "LabCelingWarp", 701, "Lab Celing Warp");
    ObjectType.Ep4LightSource = new ObjectType(id++, "Ep4LightSource", 768, "Ep4 Light Source");
    ObjectType.Cacti = new ObjectType(id++, "Cacti", 769, "cacti");
    ObjectType.BigBrownRock = new ObjectType(id++, "BigBrownRock", 770, "Big Brown Rock");
    ObjectType.BreakableBrownRock = new ObjectType(
        id++,
        "BreakableBrownRock",
        771,
        "Breakable Brown Rock"
    );
    ObjectType.UnknownItem832 = new ObjectType(id++, "UnknownItem832", 832, "Unknown Item (832)");
    ObjectType.UnknownItem833 = new ObjectType(id++, "UnknownItem833", 833, "Unknown Item (833)");
    ObjectType.PoisonPlant = new ObjectType(id++, "PoisonPlant", 896, "Poison Plant");
    ObjectType.UnknownItem897 = new ObjectType(id++, "UnknownItem897", 897, "Unknown Item (897)");
    ObjectType.UnknownItem898 = new ObjectType(id++, "UnknownItem898", 898, "Unknown Item (898)");
    ObjectType.OozingDesertPlant = new ObjectType(
        id++,
        "OozingDesertPlant",
        899,
        "Oozing Desert Plant"
    );
    ObjectType.UnknownItem901 = new ObjectType(id++, "UnknownItem901", 901, "Unknown Item (901)");
    ObjectType.BigBlackRocks = new ObjectType(id++, "BigBlackRocks", 902, "big black rocks");
    ObjectType.UnknownItem903 = new ObjectType(id++, "UnknownItem903", 903, "Unknown Item (903)");
    ObjectType.UnknownItem904 = new ObjectType(id++, "UnknownItem904", 904, "Unknown Item (904)");
    ObjectType.UnknownItem905 = new ObjectType(id++, "UnknownItem905", 905, "Unknown Item (905)");
    ObjectType.UnknownItem906 = new ObjectType(id++, "UnknownItem906", 906, "Unknown Item (906)");
    ObjectType.FallingRock = new ObjectType(id++, "FallingRock", 907, "Falling Rock");
    ObjectType.DesertPlantHasCollision = new ObjectType(
        id++,
        "DesertPlantHasCollision",
        908,
        "Desert Plant (has collision)"
    );
    ObjectType.DesertFixedTypeBoxBreakableCrystals = new ObjectType(
        id++,
        "DesertFixedTypeBoxBreakableCrystals",
        909,
        "Desert Fixed Type Box (Breakable Crystals)"
    );
    ObjectType.UnknownItem910 = new ObjectType(id++, "UnknownItem910", 910, "Unknown Item (910)");
    ObjectType.BeeHive = new ObjectType(id++, "BeeHive", 911, "Bee Hive");
    ObjectType.UnknownItem912 = new ObjectType(id++, "UnknownItem912", 912, "Unknown Item (912)");
    ObjectType.Heat = new ObjectType(id++, "Heat", 913, "Heat");
    ObjectType.TopOfSaintMillionEgg = new ObjectType(
        id++,
        "TopOfSaintMillionEgg",
        960,
        "Top of saint million egg"
    );
    ObjectType.UnknownItem961 = new ObjectType(id++, "UnknownItem961", 961, "Unknown Item (961)");
})();
