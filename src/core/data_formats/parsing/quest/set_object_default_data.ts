import { ObjectType } from "./object_types";

export function set_object_default_data(type: ObjectType, view: DataView): void {
    view.setFloat32(40, 1, true); // Scale x
    view.setFloat32(44, 1, true); // Scale y
    view.setFloat32(48, 1, true); // Scale z

    switch (type) {
        case ObjectType.PlayerSet:
            view.setInt16(2, 2, true);
            view.setInt16(4, 4, true);
            view.setFloat32(40, 0, true); // Slot ID
            view.setInt32(64, -1929687552, true);
            break;
        case ObjectType.Particle:
            view.setInt16(4, 5639, true);
            view.setInt16(6, 10, true);
            view.setFloat32(40, 38, true); // Scale x
            view.setInt32(64, 1351475840, true);
            break;
        case ObjectType.Teleporter:
            view.setInt16(2, 2, true);
            view.setInt16(4, 90, true);
            view.setInt16(6, 1, true);
            view.setInt32(64, -1929650432, true);
            break;
        case ObjectType.Warp:
            view.setInt16(4, 1332, true);
            view.setInt16(6, 4, true);
            view.setFloat32(40, 0, true); // Destination x
            view.setFloat32(44, 0, true); // Destination y
            view.setFloat32(48, 0, true); // Destination z
            view.setInt32(52, 0, true); // Dst. rotation y
            view.setInt32(64, -1929729040, true);
            break;
        case ObjectType.LightCollision:
            view.setFloat32(48, 0.5, true); // Scale z
            view.setInt32(56, 26112, true);
            view.setInt32(64, -802624128, true);
            break;
        case ObjectType.FogCollision:
            view.setInt16(4, 2019, true);
            view.setInt16(6, 10, true);
            view.setFloat32(40, 140, true); // Radius
            view.setInt32(52, 17, true); // Fog index no.
            view.setInt32(64, 36780240, true);
            break;
        case ObjectType.EventCollision:
            view.setInt16(4, 2131, true);
            view.setInt16(6, 8, true);
            view.setFloat32(40, 30, true); // Radius
            view.setInt32(64, 71359776, true);
            break;
        case ObjectType.ElementalTrap:
            view.setFloat32(40, 0, true); // Radius
            view.setFloat32(44, 0, true); // Scale y
            view.setFloat32(48, -1, true); // Trap link
            view.setInt32(52, 100, true); // Damage
            view.setInt32(60, 20, true); // Delay
            break;
        case ObjectType.StatusTrap:
            view.setFloat32(40, 0, true); // Radius
            view.setFloat32(44, 0, true); // Scale y
            view.setFloat32(48, -1, true); // Trap link
            view.setInt32(56, 17, true); // Subtype
            view.setInt32(60, 20, true); // Delay
            break;
        case ObjectType.HealTrap:
            view.setInt16(4, 1566, true);
            view.setInt16(6, 4, true);
            view.setFloat32(40, -32.00001525878906, true); // Radius
            view.setFloat32(44, -60.00002670288086, true); // Scale y
            view.setFloat32(48, -1.000000238418579, true); // Trap link
            view.setInt32(52, 200, true); // HP
            view.setInt32(60, 60, true); // Delay
            view.setInt32(64, -1929453936, true);
            break;
        case ObjectType.LargeElementalTrap:
            view.setFloat32(40, 0, true); // Radus
            view.setFloat32(44, 0, true); // Scale y
            view.setFloat32(48, -1, true); // Trap link
            view.setInt32(52, 200, true); // Damage
            view.setInt32(60, 50, true); // Delay
            break;
        case ObjectType.ObjRoomID:
            view.setInt16(4, 1545, true);
            view.setInt16(6, 1, true);
            view.setFloat32(40, 3.9999988079071045, true); // SCL_TAMA
            view.setFloat32(44, 5, true); // Next section
            view.setFloat32(48, 10, true); // Previous section
            view.setInt32(56, 16384, true);
            view.setInt32(64, -1929753600, true);
            break;
        case ObjectType.ScriptCollision:
            view.setInt16(4, 1771, true);
            view.setInt16(6, 14, true);
            view.setFloat32(40, 250.00009155273438, true); // Radius
            view.setInt32(64, -1929714224, true);
            break;
        case ObjectType.HealRing:
            view.setFloat32(40, 0, true); // Scale x
            view.setFloat32(44, 0, true); // Scale y
            view.setFloat32(48, 0, true); // Scale z
            break;
        case ObjectType.ItemLight:
            view.setInt16(4, 1990, true);
            view.setInt16(6, 8, true);
            view.setFloat32(40, 3, true); // Subtype
            view.setInt32(64, 39205168, true);
            break;
        case ObjectType.FogCollisionSW:
            view.setInt16(4, 1550, true);
            view.setInt16(6, 3, true);
            view.setFloat32(40, 240.0000762939453, true); // Radius
            view.setFloat32(48, 0, true); // Scale z
            view.setInt32(52, 17, true); // Fog index no.
            view.setInt32(64, -1929715680, true);
            break;
        case ObjectType.BossTeleporter:
            view.setInt16(4, 1555, true);
            view.setInt16(6, 2, true);
            view.setInt32(56, 31, true);
            view.setInt32(64, -1929731696, true);
            break;
        case ObjectType.ImageBoard:
            view.setInt16(4, 384, true);
            view.setFloat32(40, 0.5, true); // Scale x
            view.setFloat32(44, 2.000000476837158, true); // Scale y
            view.setFloat32(48, 1.000000238418579, true); // Scale z
            view.setInt32(64, 72261968, true);
            break;
        case ObjectType.QuestWarp:
            view.setInt16(4, 1555, true);
            view.setInt16(6, 2, true);
            view.setInt32(60, 1, true);
            view.setInt32(64, -1929711344, true);
            break;
        case ObjectType.BoxDetectObject:
            view.setInt16(4, 1776, true);
            view.setInt16(6, 5, true);
            view.setFloat32(40, 30.000015258789062, true); // Radius
            view.setInt32(56, 6, true);
            view.setInt32(60, 3, true);
            view.setInt32(64, -1929661840, true);
            break;
        case ObjectType.SymbolChatObject:
            view.setInt16(4, 1792, true);
            view.setInt16(6, 5, true);
            view.setFloat32(40, 30.000015258789062, true); // Radius
            view.setInt32(56, 30, true);
            view.setInt32(60, 30, true);
            view.setInt32(64, -1929667104, true);
            break;
        case ObjectType.TouchPlateObject:
            view.setInt16(4, 26119, true);
            view.setInt16(6, 11, true);
            view.setFloat32(40, 8, true); // Radius
            view.setInt32(56, -1, true);
            view.setInt32(64, -259264640, true);
            break;
        case ObjectType.TargetableObject:
            view.setInt16(4, -23032, true);
            view.setInt16(6, 2, true);
            view.setFloat32(40, -1, true); // Scale x
            view.setFloat32(48, 0, true); // Switch ID
            view.setInt32(52, 2, true); // HP
            view.setInt32(64, -2144604032, true);
            break;
        case ObjectType.EffectObject:
            view.setInt16(4, 25863, true);
            view.setInt16(6, 11, true);
            view.setFloat32(40, 0, true); // Scale x
            view.setFloat32(44, 0, true); // Scale y
            view.setInt32(60, 1, true);
            view.setInt32(64, -259199104, true);
            break;
        case ObjectType.CountDownObject:
            view.setInt16(4, -31992, true);
            view.setInt16(6, 1, true);
            view.setInt32(64, -252124544, true);
            break;
        case ObjectType.UnknownItem39:
            view.setInt16(4, 30728, true);
            view.setInt16(6, 1, true);
            view.setInt32(56, 65641, true);
            view.setInt32(64, 821944960, true);
            break;
        case ObjectType.MenuActivation:
            view.setInt16(2, 2, true);
            view.setInt32(64, -1929688192, true);
            break;
        case ObjectType.TelepipeLocation:
            view.setInt16(2, 2, true);
            view.setInt16(4, 13, true);
            view.setInt32(64, -1929685680, true);
            break;
        case ObjectType.BGMCollision:
            view.setInt16(2, 512, true);
            view.setInt16(4, 11520, true);
            view.setFloat32(40, 500, true); // Scale x
            view.setInt32(64, -265293952, true);
            break;
        case ObjectType.MainRagolTeleporter:
            view.setInt16(4, 1879, true);
            view.setFloat32(40, 1.000000238418579, true); // Scale x
            view.setInt32(64, -1929740080, true);
            break;
        case ObjectType.LobbyTeleporter:
            view.setInt16(2, 512, true);
            view.setInt32(64, -1073221760, true);
            break;
        case ObjectType.PrincipalWarp:
            view.setInt16(2, 2, true);
            view.setInt16(4, 9, true);
            view.setFloat32(40, 10, true); // Destination x
            view.setFloat32(44, 0, true); // Destination y
            view.setFloat32(48, -1760.001, true); // Destination z
            view.setInt32(52, 32768, true); // Dst. rotation y
            view.setInt32(56, 65536, true);
            view.setInt32(64, -1929686608, true);
            break;
        case ObjectType.ShopDoor:
            view.setInt16(2, 2, true);
            view.setInt16(4, 18, true);
            view.setFloat32(40, 3.0000007152557373, true); // Scale x
            view.setInt32(64, -1929684656, true);
            break;
        case ObjectType.HuntersGuildDoor:
            view.setInt16(2, 2, true);
            view.setInt16(4, 20, true);
            view.setFloat32(40, 3.0000007152557373, true); // Scale x
            view.setInt32(64, -1929684240, true);
            break;
        case ObjectType.TeleporterDoor:
            view.setInt16(2, 2, true);
            view.setInt16(4, 21, true);
            view.setFloat32(40, 3.0000007152557373, true); // Scale x
            view.setInt32(64, -1929683984, true);
            break;
        case ObjectType.MedicalCenterDoor:
            view.setInt16(2, 2, true);
            view.setInt16(4, 17, true);
            view.setFloat32(40, 3.0000007152557373, true); // Scale x
            view.setInt32(64, -1929684912, true);
            break;
        case ObjectType.Sonic:
            view.setInt16(2, 2, true);
            view.setUint32(52, 1, true); // Model
            view.setInt32(64, 79126144, true);
            break;
        case ObjectType.WelcomeBoard:
            view.setInt16(4, 23304, true);
            view.setInt32(64, -1864965504, true);
            break;
        case ObjectType.LobbyScreenDoor:
            view.setInt16(2, 2, true);
            view.setInt16(4, 25, true);
            view.setInt32(64, 118136448, true);
            break;
        case ObjectType.LabTeleporterDoor:
            view.setInt16(4, 16904, true);
            view.setInt32(64, -266145920, true);
            break;
        case ObjectType.Pioneer2InvisibleTouchplate:
            view.setInt16(2, 2, true);
            view.setInt16(4, 27, true);
            view.setFloat32(40, 150, true); // Radius
            view.setInt32(64, 70133216, true);
            break;
        case ObjectType.ForestDoor:
            view.setInt16(4, 1581, true);
            view.setInt16(6, 1, true);
            view.setFloat32(40, 0.9999980926513672, true); // Scale x
            view.setFloat32(44, 1.000000238418579, true); // Scale y
            view.setFloat32(48, 0.9999954700469971, true); // Scale z
            view.setInt32(64, -1929757168, true);
            break;
        case ObjectType.ForestSwitch:
            view.setInt16(4, 1543, true);
            view.setInt16(6, 1, true);
            view.setFloat32(40, 1.000000238418579, true); // Scale x
            view.setFloat32(44, 1.0000005960464478, true); // Scale y
            view.setFloat32(48, 1.0000001192092896, true); // Scale z
            view.setInt32(60, 7, true); // Color
            view.setInt32(64, -1929750128, true);
            break;
        case ObjectType.LaserFence:
            view.setInt16(4, 1542, true);
            view.setInt16(6, 1, true);
            view.setFloat32(40, 0, true); // Color
            view.setFloat32(44, 1.0000009536743164, true); // Scale y
            view.setFloat32(48, 0.9999961853027344, true); // Scale z
            view.setInt32(64, -1929756272, true);
            break;
        case ObjectType.LaserSquareFence:
            view.setInt16(4, 1468, true);
            view.setInt16(6, 3, true);
            view.setFloat32(40, 1.000000238418579, true); // Color
            view.setUint32(60, 1, true); // Model
            view.setInt32(64, -1929753744, true);
            break;
        case ObjectType.ForestLaserFenceSwitch:
            view.setInt16(4, 1478, true);
            view.setInt16(6, 3, true);
            view.setInt32(56, 3, true);
            view.setInt32(60, 3, true); // Color
            view.setInt32(64, -1929696640, true);
            break;
        case ObjectType.LightRays:
            view.setInt16(4, 2369, true);
            view.setInt16(6, 8, true);
            view.setFloat32(44, 10, true); // Scale y
            view.setFloat32(48, 20, true); // Scale z
            view.setInt32(64, 74962176, true);
            break;
        case ObjectType.BlueButterfly:
            view.setInt16(4, 2048, true);
            view.setInt16(6, 2, true);
            view.setInt32(64, -1929720176, true);
            break;
        case ObjectType.Probe:
            view.setInt16(2, 2, true);
            view.setInt16(4, 80, true);
            view.setInt16(6, 1, true);
            view.setFloat32(40, 0, true); // Model
            view.setFloat32(44, 0, true); // Scale y
            view.setFloat32(48, 0, true); // Scale z
            view.setInt32(64, -1929746928, true);
            break;
        case ObjectType.RandomTypeBox1:
            view.setInt16(4, 1635, true);
            view.setInt16(6, 2, true);
            view.setFloat32(40, 8.000000953674316, true); // Scale x
            view.setFloat32(44, 3.1000001430511475, true); // Scale y
            view.setFloat32(48, 10.100005149841309, true); // Scale z
            view.setInt32(56, 57344, true);
            view.setInt32(64, -1929730096, true);
            break;
        case ObjectType.ForestWeatherStation:
            view.setInt16(2, 2, true);
            view.setInt16(4, 147, true);
            view.setInt16(6, 2, true);
            view.setFloat32(40, 1.000000238418579, true); // Scale x
            view.setFloat32(44, 1.0000016689300537, true); // Scale y
            view.setFloat32(48, 1.0000005960464478, true); // Scale z
            view.setInt32(56, 2816, true);
            view.setInt32(64, -1929753232, true);
            break;
        case ObjectType.ForestConsole:
            view.setInt16(4, 1331, true);
            view.setInt16(6, 2, true);
            view.setFloat32(40, 0, true); // Scale x
            view.setInt32(52, 1050, true); // Script label
            view.setInt32(64, -1929750848, true);
            break;
        case ObjectType.BlackSlidingDoor:
            view.setInt16(4, 1625, true);
            view.setInt16(6, 1, true);
            view.setFloat32(40, 30.000015258789062, true); // Distance
            view.setFloat32(44, 0.8999999761581421, true); // Speed
            view.setInt32(52, 101, true); // Switch no.
            view.setInt32(64, -1929721536, true);
            break;
        case ObjectType.RicoMessagePod:
            view.setInt16(4, 1654, true);
            view.setInt16(6, 13, true);
            view.setInt32(60, 259, true);
            view.setInt32(64, -1929755408, true);
            break;
        case ObjectType.EnergyBarrier:
            view.setInt16(4, 1342, true);
            view.setInt16(6, 1, true);
            view.setFloat32(40, 15.000005722045898, true); // Scale x
            view.setFloat32(44, 1.0000001192092896, true); // Scale y
            view.setFloat32(48, 1.000000238418579, true); // Scale z
            view.setInt32(52, -1, true); // Door ID
            view.setInt32(64, -1929730384, true);
            break;
        case ObjectType.ForestRisingBridge:
            view.setInt16(2, 2, true);
            view.setInt16(4, 145, true);
            view.setInt16(6, 2, true);
            view.setFloat32(40, 1.000000238418579, true); // Scale x
            view.setFloat32(44, 1.0000015497207642, true); // Scale y
            view.setFloat32(48, 1.0000005960464478, true); // Scale z
            view.setInt32(52, -1, true); // Door ID
            view.setInt32(56, 2816, true);
            view.setInt32(64, -1929751104, true);
            break;
        case ObjectType.SwitchNoneDoor:
            view.setInt16(4, 1495, true);
            view.setInt16(6, 2, true);
            view.setFloat32(40, 0, true); // Scale x
            view.setInt32(64, -1929748688, true);
            break;
        case ObjectType.EnemyBoxGrey:
            view.setInt16(4, 1525, true);
            view.setInt16(6, 5, true);
            view.setInt32(64, -1929732576, true);
            break;
        case ObjectType.FixedTypeBox:
            view.setFloat32(40, 0, true); // Full random
            view.setFloat32(48, 0, true); // Fixed item
            break;
        case ObjectType.EmptyTypeBox:
            view.setInt16(2, 512, true);
            view.setInt16(4, 32001, true);
            view.setInt16(6, 6, true);
            view.setInt32(64, -1862737024, true);
            break;
        case ObjectType.LaserFenceEx:
            view.setInt16(4, 28167, true);
            view.setInt16(6, 11, true);
            view.setFloat32(40, 0, true); // Color
            view.setFloat32(44, 8, true); // Collision width
            view.setFloat32(48, 25, true); // Collision depth
            view.setInt32(64, -526061696, true);
            break;
        case ObjectType.FloorPanel1:
            view.setInt16(4, 1556, true);
            view.setInt16(6, 4, true);
            view.setFloat32(40, 1.000000238418579, true); // Scale  x
            view.setFloat32(44, 1.000000238418579, true); // Scale  y
            view.setFloat32(48, 0.9990062713623047, true); // Scale  z
            view.setInt32(64, -1929651776, true);
            break;
        case ObjectType.Caves4ButtonDoor:
            view.setInt16(2, 2, true);
            view.setInt16(4, 542, true);
            view.setInt16(6, 4, true);
            view.setInt32(52, -1, true); // Door ID
            view.setInt32(64, -1929625056, true);
            break;
        case ObjectType.CavesNormalDoor:
            view.setInt16(4, 1464, true);
            view.setInt16(6, 4, true);
            view.setFloat32(40, 0.9999891519546509, true); // Scale x
            view.setFloat32(44, 1.000000238418579, true); // Scale y
            view.setFloat32(48, 0.9990062713623047, true); // Scale z
            view.setInt32(52, -1, true); // Door ID
            view.setInt32(56, 3, true);
            view.setInt32(64, -1929741968, true);
            break;
        case ObjectType.CavesSmashingPillar:
            view.setInt16(4, 1559, true);
            view.setInt16(6, 3, true);
            view.setInt32(64, -1929691696, true);
            break;
        case ObjectType.CavesSign1:
            view.setInt16(2, 512, true);
            view.setInt16(4, -23552, true);
            view.setInt16(6, 5, true);
            view.setInt32(64, 814801792, true);
            break;
        case ObjectType.CavesSign2:
            view.setInt16(2, 512, true);
            view.setInt16(4, 26624, true);
            view.setInt16(6, 5, true);
            view.setInt32(64, 1884021632, true);
            break;
        case ObjectType.CavesSign3:
            view.setInt16(2, 512, true);
            view.setInt16(4, -18944, true);
            view.setInt16(6, 5, true);
            view.setInt32(64, 280945536, true);
            break;
        case ObjectType.HexagonalTank:
            view.setInt16(2, 2, true);
            view.setInt16(4, 740, true);
            view.setInt16(6, 5, true);
            view.setInt32(64, -1334188928, true);
            break;
        case ObjectType.BrownPlatform:
            view.setInt16(2, 2, true);
            view.setInt16(4, 559, true);
            view.setInt16(6, 5, true);
            view.setInt32(64, 276292480, true);
            break;
        case ObjectType.FloatingDragonfly:
            view.setInt16(4, 1517, true);
            view.setInt16(6, 6, true);
            view.setInt32(64, 37198656, true);
            break;
        case ObjectType.CavesSwitchDoor:
            view.setInt16(4, 1450, true);
            view.setInt16(6, 3, true);
            view.setInt32(52, -1, true); // Door ID
            view.setInt32(56, 3, true);
            view.setInt32(64, -1929685552, true);
            break;
        case ObjectType.RobotRechargeStation:
            view.setInt16(2, 512, true);
            view.setInt16(4, -29952, true);
            view.setInt16(6, 5, true);
            view.setInt32(64, 275440512, true);
            break;
        case ObjectType.CavesCakeShop:
            view.setInt16(4, -6905, true);
            view.setInt16(6, 5, true);
            view.setInt32(64, -1067911552, true);
            break;
        case ObjectType.Caves1SmallRedRock:
            view.setInt16(4, 1965, true);
            view.setInt16(6, 3, true);
            view.setInt32(64, -1929677200, true);
            break;
        case ObjectType.Caves1MediumRedRock:
            view.setInt16(4, 1968, true);
            view.setInt16(6, 3, true);
            view.setInt32(64, -1929811824, true);
            break;
        case ObjectType.Caves1LargeRedRock:
            view.setInt16(4, 1552, true);
            view.setInt16(6, 3, true);
            view.setInt32(64, -1929724048, true);
            break;
        case ObjectType.Caves2SmallRock1:
            view.setInt16(4, 1563, true);
            view.setInt16(6, 4, true);
            view.setFloat32(40, 1.000000238418579, true); // Scale x
            view.setFloat32(44, 1.000000238418579, true); // Scale y
            view.setInt32(64, -1929710640, true);
            break;
        case ObjectType.Caves2MediumRock1:
            view.setInt16(2, 2, true);
            view.setInt16(4, 569, true);
            view.setInt16(6, 4, true);
            view.setFloat32(40, -20.000011444091797, true); // Scale x
            view.setFloat32(44, -80.00003814697266, true); // Scale y
            view.setFloat32(48, -1.000000238418579, true); // Scale z
            view.setInt32(60, 40, true);
            view.setInt32(64, -1929628992, true);
            break;
        case ObjectType.Caves2LargeRock1:
            view.setInt16(4, 2051, true);
            view.setInt16(6, 4, true);
            view.setInt32(64, -1929711328, true);
            break;
        case ObjectType.Caves2SmallRock2:
            view.setInt16(4, 1961, true);
            view.setInt16(6, 4, true);
            view.setInt32(64, -1929702432, true);
            break;
        case ObjectType.Caves2MediumRock2:
            view.setInt16(4, 1550, true);
            view.setInt16(6, 4, true);
            view.setFloat32(40, 1.000000238418579, true); // Scale x
            view.setFloat32(44, 1.000000238418579, true); // Scale y
            view.setFloat32(48, 0.9990062713623047, true); // Scale z
            view.setInt32(64, -1929604016, true);
            break;
        case ObjectType.Caves2LargeRock2:
            view.setInt16(4, 1517, true);
            view.setInt16(6, 4, true);
            view.setInt32(64, -1929749872, true);
            break;
        case ObjectType.Caves3SmallRock:
            view.setInt16(4, 1722, true);
            view.setInt16(6, 5, true);
            view.setInt32(64, -1929727392, true);
            break;
        case ObjectType.Caves3MediumRock:
            view.setInt16(4, 1580, true);
            view.setInt16(6, 5, true);
            view.setInt32(64, -1929674160, true);
            break;
        case ObjectType.Caves3LargeRock:
            view.setInt16(4, -18425, true);
            view.setInt16(6, 5, true);
            view.setInt32(64, 279115136, true);
            break;
        case ObjectType.FloorPanel2:
            view.setInt16(4, 23560, true);
            view.setInt16(6, 1, true);
            view.setInt32(60, 65537, true);
            view.setInt32(64, 16310912, true);
            break;
        case ObjectType.DestructableRockCaves1:
            view.setInt16(2, 512, true);
            view.setInt16(4, 23553, true);
            view.setInt16(6, 3, true);
            view.setInt32(64, 10347136, true);
            break;
        case ObjectType.DestructableRockCaves2:
            view.setInt16(4, 2006, true);
            view.setInt16(6, 4, true);
            view.setInt32(64, -1929719824, true);
            break;
        case ObjectType.DestructableRockCaves3:
            view.setInt16(4, 1617, true);
            view.setInt16(6, 5, true);
            view.setInt32(64, -1929688384, true);
            break;
        case ObjectType.MinesDoor:
            view.setInt16(2, 2, true);
            view.setInt16(4, 715, true);
            view.setInt16(6, 6, true);
            view.setInt32(52, -1, true); // Door ID
            view.setInt32(56, 3, true); // Switch total
            view.setInt32(64, -1929747520, true);
            break;
        case ObjectType.FloorPanel3:
            view.setInt16(4, 23304, true);
            view.setInt16(6, 3, true);
            view.setInt32(56, -1, true); // Stay active
            view.setInt32(64, 1889001856, true);
            break;
        case ObjectType.MinesSwitchDoor:
            view.setInt16(4, 1887, true);
            view.setInt16(6, 6, true);
            view.setInt32(52, -1, true); // Door ID
            view.setInt32(64, -1929658608, true);
            break;
        case ObjectType.LargeCryoTube:
            view.setInt16(2, 512, true);
            view.setInt16(4, 28161, true);
            view.setInt16(6, 6, true);
            view.setInt32(64, -1329536128, true);
            break;
        case ObjectType.ComputerLikeCalus:
            view.setInt16(4, 2006, true);
            view.setInt16(6, 6, true);
            view.setFloat32(44, 0, true); // Scale y
            view.setInt32(64, -1929695952, true);
            break;
        case ObjectType.GreenScreenOpeningAndClosing:
            view.setInt16(4, 4866, true);
            view.setInt16(6, 6, true);
            view.setInt32(64, 281469568, true);
            break;
        case ObjectType.FloatingRobot:
            view.setInt16(4, 1533, true);
            view.setInt16(6, 6, true);
            view.setFloat32(40, -35.00001525878906, true); // Scale x
            view.setFloat32(44, 20.000011444091797, true); // Scale y
            view.setInt32(64, -1929732496, true);
            break;
        case ObjectType.MinesLargeFlashingCrate:
            view.setInt16(4, 1546, true);
            view.setInt16(6, 6, true);
            view.setFloat32(40, 1.000000238418579, true); // Scale x
            view.setFloat32(44, 0, true); // Scale y
            view.setInt32(64, -1929727328, true);
            break;
        case ObjectType.RuinsSeal:
            view.setInt16(4, 1550, true);
            view.setInt16(6, 13, true);
            view.setInt32(64, -1929755808, true);
            break;
        case ObjectType.RuinsTeleporter:
            view.setInt16(4, 1483, true);
            view.setInt16(6, 8, true);
            view.setInt32(64, -1929759760, true);
            break;
        case ObjectType.RuinsWarpSiteToSite:
            view.setInt16(4, 2017, true);
            view.setInt16(6, 8, true);
            view.setFloat32(40, 0, true); // Destination x
            view.setFloat32(44, 0, true); // Destination y
            view.setFloat32(48, 0, true); // Destination z
            view.setInt32(64, 39228864, true);
            break;
        case ObjectType.RuinsSwitch:
            view.setInt16(4, 1910, true);
            view.setInt16(6, 8, true);
            view.setInt32(64, -1929687952, true);
            break;
        case ObjectType.FloorPanel4:
            view.setInt16(4, 1660, true);
            view.setInt16(6, 9, true);
            view.setFloat32(40, 0, true); // Scale x
            view.setFloat32(44, 0, true); // Scale y
            view.setFloat32(48, 0, true); // Scale z
            view.setInt32(64, -1929749648, true);
            break;
        case ObjectType.Ruins1Door:
            view.setFloat32(44, 0, true); // Scale y
            view.setFloat32(48, 0, true); // Scale z
            view.setInt32(52, -1, true); // Door ID
            break;
        case ObjectType.Ruins3Door:
            view.setFloat32(44, 0, true); // Scale y
            view.setFloat32(48, 0, true); // Scale z
            view.setInt32(52, -1, true); // Door ID
            break;
        case ObjectType.Ruins2Door:
            view.setFloat32(44, 0, true); // Scale y
            view.setFloat32(48, 0, true); // Scale z
            view.setInt32(52, -1, true); // Door ID
            break;
        case ObjectType.Ruins11ButtonDoor:
            view.setInt16(4, 31751, true);
            view.setInt16(6, 8, true);
            view.setInt32(52, -1, true); // Door ID
            view.setInt32(64, -1874992256, true);
            break;
        case ObjectType.Ruins21ButtonDoor:
            view.setInt16(2, 2, true);
            view.setInt16(4, 1371, true);
            view.setInt16(6, 9, true);
            view.setInt32(52, -1, true); // Door ID
            view.setInt32(64, -1929663984, true);
            break;
        case ObjectType.Ruins4ButtonDoor:
            view.setInt16(4, 1480, true);
            view.setInt16(6, 8, true);
            view.setInt32(52, -1, true); // Door ID
            view.setInt32(60, -1, true); // Stay active
            view.setInt32(64, -1929730336, true);
            break;
        case ObjectType.Ruins2ButtonDoor:
            view.setInt16(4, 1909, true);
            view.setInt16(6, 8, true);
            view.setInt32(52, -1, true); // Door ID
            view.setInt32(64, -1929493856, true);
            break;
        case ObjectType.RuinsFenceSwitch:
            view.setInt16(4, 1899, true);
            view.setInt16(6, 8, true);
            view.setInt32(56, 1, true); // Color
            view.setInt32(64, -1929707856, true);
            break;
        case ObjectType.RuinsLaserFence4x2:
            view.setFloat32(40, 0, true); // Scale x
            view.setFloat32(44, 0, true); // Scale y
            view.setFloat32(48, 0, true); // Scale z
            view.setInt32(56, 1, true); // Color
            break;
        case ObjectType.RuinsLaserFence6x2:
            view.setFloat32(40, 0, true); // Scale x
            view.setFloat32(44, 0, true); // Scale y
            view.setFloat32(48, 0, true); // Scale z
            view.setInt32(56, 1, true); // Color
            break;
        case ObjectType.RuinsLaserFence4x4:
            view.setInt16(4, 2064, true);
            view.setInt16(6, 9, true);
            view.setInt32(56, 1, true); // Color
            view.setInt32(64, -1929548960, true);
            break;
        case ObjectType.RuinsPillarTrap:
            view.setFloat32(40, -25, true); // Scale x
            view.setFloat32(44, 100, true); // Scale y
            view.setFloat32(48, 15, true); // Scale z
            view.setInt32(60, 60, true);
            break;
        case ObjectType.PopupTrapNoTech:
            view.setInt16(2, 2, true);
            view.setInt16(4, 1432, true);
            view.setInt16(6, 9, true);
            view.setFloat32(40, 1.000000238418579, true); // Radius
            view.setInt32(64, -1929630640, true);
            break;
        case ObjectType.RuinsCrystal:
            view.setFloat32(40, 0, true); // Scale x
            view.setFloat32(44, 0, true); // Scale y
            view.setFloat32(48, 0, true); // Scale z
            break;
        case ObjectType.Monument:
            view.setInt16(4, 1557, true);
            view.setInt16(6, 4, true);
            view.setFloat32(40, 1.000000238418579, true); // Scale x
            view.setFloat32(44, 1.000000238418579, true); // Scale y
            view.setFloat32(48, 1.000000238418579, true); // Scale z
            view.setInt32(64, -1929727536, true);
            break;
        case ObjectType.RuinsRock1:
            view.setFloat32(40, 0, true); // Scale x
            view.setFloat32(44, 0, true); // Scale y
            view.setFloat32(48, 0, true); // Scale z
            break;
        case ObjectType.RuinsRock2:
            view.setFloat32(40, 0, true); // Scale x
            view.setFloat32(44, 0, true); // Scale y
            view.setFloat32(48, 0, true); // Scale z
            break;
        case ObjectType.RuinsRock3:
            view.setFloat32(40, 0, true); // Scale x
            view.setFloat32(44, 0, true); // Scale y
            view.setFloat32(48, 0, true); // Scale z
            break;
        case ObjectType.RuinsRock4:
            view.setFloat32(40, 0, true); // Scale x
            view.setFloat32(44, 0, true); // Scale y
            view.setFloat32(48, 0, true); // Scale z
            break;
        case ObjectType.RuinsRock5:
            view.setFloat32(40, 0, true); // Scale x
            view.setFloat32(44, 0, true); // Scale y
            view.setFloat32(48, 0, true); // Scale z
            break;
        case ObjectType.RuinsRock6:
            view.setFloat32(40, 0, true); // Scale x
            view.setFloat32(44, 0, true); // Scale y
            view.setFloat32(48, 0, true); // Scale z
            break;
        case ObjectType.RuinsRock7:
            view.setInt16(4, 1548, true);
            view.setInt16(6, 8, true);
            view.setInt32(64, -1929795888, true);
            break;
        case ObjectType.Poison:
            view.setInt16(4, 1983, true);
            view.setInt16(6, 13, true);
            view.setInt32(52, 8, true); // Switch mode
            view.setInt32(64, -1929757344, true);
            break;
        case ObjectType.FixedBoxTypeRuins:
            view.setFloat32(40, 0, true); // Full random
            view.setFloat32(44, 0, true); // Random item
            view.setFloat32(48, 0, true); // Fixed item
            break;
        case ObjectType.RandomBoxTypeRuins:
            view.setFloat32(40, 0, true); // Scale x
            view.setFloat32(44, 0, true); // Scale y
            view.setFloat32(48, 0, true); // Scale z
            break;
        case ObjectType.EnemyTypeBoxYellow:
            view.setInt16(4, 2043, true);
            view.setInt16(6, 10, true);
            view.setInt32(64, 39241584, true);
            break;
        case ObjectType.DestructableRock:
            view.setInt16(2, 2, true);
            view.setInt16(4, 1517, true);
            view.setInt16(6, 9, true);
            view.setInt32(64, -1929579040, true);
            break;
        case ObjectType.PopupTrapsTechs:
            view.setFloat32(40, 50, true); // Radius
            view.setFloat32(44, 0, true); // HP
            view.setFloat32(48, 30, true); // Scale z
            view.setInt32(56, -1, true); // Action
            view.setInt32(60, 2, true); // Tech
            break;
        case ObjectType.GreyWallLow:
            view.setInt16(4, 2095, true);
            view.setInt16(6, 17, true);
            view.setInt32(64, 206220336, true);
            break;
        case ObjectType.SpaceshipDoor:
            view.setInt16(4, -18681, true);
            view.setInt16(6, 18, true);
            view.setInt32(64, -254418560, true);
            break;
        case ObjectType.GreyWallHigh:
            view.setInt16(4, 28424, true);
            view.setInt16(6, 3, true);
            view.setInt32(64, 548332416, true);
            break;
        case ObjectType.TempleNormalDoor:
            view.setInt16(4, 1638, true);
            view.setInt16(6, 17, true);
            view.setInt32(64, 1356460160, true);
            break;
        case ObjectType.BreakableWallWallButUnbreakable:
            view.setInt16(4, 21505, true);
            view.setInt16(6, 2, true);
            view.setInt32(64, -521801600, true);
            break;
        case ObjectType.BrokenCylinderAndRubble:
            view.setInt16(4, 30984, true);
            view.setInt16(6, 1, true);
            view.setInt32(64, 1077601152, true);
            break;
        case ObjectType.ThreeBrokenWallPiecesOnFloor:
            view.setInt16(4, -28409, true);
            view.setInt16(6, 1, true);
            view.setInt32(64, -255991424, true);
            break;
        case ObjectType.HighBrickCylinder:
            view.setInt16(4, -19448, true);
            view.setInt16(6, 1, true);
            view.setInt32(64, -1597054592, true);
            break;
        case ObjectType.LyingCylinder:
            view.setInt16(4, 16136, true);
            view.setInt16(6, 2, true);
            view.setInt32(64, -1069292672, true);
            break;
        case ObjectType.BrickConeWithFlatTop:
            view.setInt16(4, 4361, true);
            view.setInt16(6, 1, true);
            view.setInt32(64, -1864506752, true);
            break;
        case ObjectType.BreakableTempleWall:
            view.setInt16(4, -18936, true);
            view.setInt16(6, 1, true);
            view.setInt32(64, -1873157504, true);
            break;
        case ObjectType.TempleMapDetect:
            view.setInt16(4, 17416, true);
            view.setInt16(6, 14, true);
            view.setFloat32(40, 0, true); // Scale x
            view.setInt32(64, -262672512, true);
            break;
        case ObjectType.SmallBrownBrickRisingBridge:
            view.setInt16(4, -24824, true);
            view.setInt16(6, 1, true);
            view.setFloat32(40, 0.5, true); // Scale x
            view.setFloat32(44, 0.5, true); // Scale y
            view.setInt32(64, -1601638272, true);
            break;
        case ObjectType.LongRisingBridgeWithPinkHighEdges:
            view.setInt16(4, 32264, true);
            view.setInt16(6, 1, true);
            view.setFloat32(40, 0.29999998211860657, true); // Scale x
            view.setFloat32(44, 0.29999998211860657, true); // Scale y
            view.setInt32(64, -265231488, true);
            break;
        case ObjectType.FourSwitchTempleDoor:
            view.setInt16(4, 1289, true);
            view.setInt16(6, 1, true);
            view.setInt32(56, 1, true);
            view.setInt32(64, -254680448, true);
            break;
        case ObjectType.FourButtonSpaceshipDoor:
            view.setInt16(4, 23048, true);
            view.setInt16(6, 3, true);
            view.setInt32(56, 2, true);
            view.setInt32(60, -1, true);
            view.setInt32(64, 1620500864, true);
            break;
        case ObjectType.ItemBoxCca:
            view.setInt16(2, 512, true);
            view.setInt16(4, 22019, true);
            view.setInt16(6, 5, true);
            view.setInt32(64, -1599409280, true);
            break;
        case ObjectType.TeleporterEp2:
            view.setInt16(4, 2110, true);
            view.setInt16(6, 16, true);
            view.setInt32(60, 1, true);
            view.setInt32(64, 71221728, true);
            break;
        case ObjectType.CcaDoor:
            view.setInt16(2, 512, true);
            view.setInt16(4, 21763, true);
            view.setInt16(6, 5, true);
            view.setInt32(56, 1, true); // Switch amount
            view.setInt32(64, -2136345728, true);
            break;
        case ObjectType.SpecialBoxCca:
            view.setInt16(2, 512, true);
            view.setInt16(4, -15612, true);
            view.setInt16(6, 7, true);
            view.setFloat32(40, 0, true); // Scale x
            view.setFloat32(48, 0, true); // Scale z
            view.setInt32(64, -1871846784, true);
            break;
        case ObjectType.BigCcaDoor:
            view.setInt16(4, 2118, true);
            view.setInt16(6, 5, true);
            view.setInt32(64, 70753104, true);
            break;
        case ObjectType.BigCcaDoorSwitch:
            view.setInt16(4, 2111, true);
            view.setInt16(6, 6, true);
            view.setInt32(64, 71229312, true);
            break;
        case ObjectType.LittleRock:
            view.setInt16(4, 2126, true);
            view.setInt16(6, 16, true);
            view.setInt32(64, 71221728, true);
            break;
        case ObjectType.Little3StoneWall:
            view.setInt16(4, 2129, true);
            view.setInt16(6, 16, true);
            view.setInt32(64, 71227424, true);
            break;
        case ObjectType.Medium3StoneWall:
            view.setInt16(4, 2129, true);
            view.setInt16(6, 8, true);
            view.setInt32(64, 71259376, true);
            break;
        case ObjectType.SpiderPlant:
            view.setInt16(4, 2129, true);
            view.setInt16(6, 16, true);
            view.setInt32(64, 71231904, true);
            break;
        case ObjectType.OrangeBird:
            view.setInt16(4, 2086, true);
            view.setInt16(6, 9, true);
            view.setInt32(64, 70881472, true);
            break;
        case ObjectType.Saw:
            view.setInt16(4, 29959, true);
            view.setInt16(6, 11, true);
            view.setFloat32(44, 300, true); // Speed
            view.setInt32(56, 1073742320, true); // Arc
            view.setInt32(60, 1, true); // Switch flag
            view.setInt32(64, 547417984, true);
            break;
        case ObjectType.LaserDetect:
            view.setInt16(4, 32519, true);
            view.setInt16(6, 11, true);
            view.setInt32(56, 1073742160, true); // Arc
            view.setInt32(60, 2, true);
            view.setInt32(64, 1082650496, true);
            break;
        case ObjectType.UnknownItem529:
            view.setInt16(4, 2139, true);
            view.setInt16(6, 6, true);
            view.setInt32(64, 71348128, true);
            break;
        case ObjectType.UnknownItem530:
            view.setInt16(4, 2046, true);
            view.setInt16(6, 9, true);
            view.setInt32(64, 206357088, true);
            break;
        case ObjectType.Seagull:
            view.setInt16(4, 2120, true);
            view.setInt16(6, 8, true);
            view.setInt32(64, 71256080, true);
            break;
        case ObjectType.Fish:
            view.setInt16(4, -29432, true);
            view.setInt16(6, 9, true);
            view.setFloat32(40, 11, true); // Scale x
            view.setFloat32(44, 0.5, true); // Scale y
            view.setFloat32(48, 11, true); // Scale z
            view.setInt32(64, 73579344, true);
            break;
        case ObjectType.SeabedDoorWithBlueEdges:
            view.setInt16(4, 29959, true);
            view.setInt16(6, 11, true);
            view.setInt32(56, 1, true); // Switch amount
            view.setInt32(64, 1611984768, true);
            break;
        case ObjectType.SeabedDoorAlwaysOpenNonTriggerable:
            view.setInt16(4, 1884, true);
            view.setInt16(6, 11, true);
            view.setInt32(64, 45365632, true);
            break;
        case ObjectType.LittleCryotube:
            view.setInt16(4, 2093, true);
            view.setInt16(6, 17, true);
            view.setInt32(64, 206221408, true);
            break;
        case ObjectType.WideGlassWallBreakable:
            view.setInt16(2, 2048, true);
            view.setInt16(4, 25863, true);
            view.setInt16(6, 10, true);
            view.setInt32(56, 1, true);
            view.setInt32(64, 8712064, true);
            break;
        case ObjectType.CaptureTrap:
            view.setInt16(4, 2123, true);
            view.setInt16(6, 5, true);
            view.setFloat32(40, 3, true); // Scale x
            view.setFloat32(44, 400, true); // Scale y
            view.setInt32(60, 3, true);
            view.setInt32(64, 37000640, true);
            break;
        case ObjectType.VRLink:
            view.setInt16(4, 21256, true);
            view.setInt16(6, 3, true);
            view.setInt32(64, -1068113280, true);
            break;
        case ObjectType.WarpInBarbaRayRoom:
            view.setInt16(4, 17416, true);
            view.setInt16(6, 14, true);
            view.setInt32(60, 1, true);
            view.setInt32(64, -266342528, true);
            break;
        case ObjectType.GeeNest:
            view.setInt16(4, 28169, true);
            view.setInt16(6, 6, true);
            view.setInt32(60, -2, true);
            view.setInt32(64, 8053376, true);
            break;
        case ObjectType.LabComputerConsole:
            view.setInt16(4, 14600, true);
            view.setInt32(64, 1075834752, true);
            break;
        case ObjectType.LabComputerConsoleGreenScreen:
            view.setInt16(2, 512, true);
            view.setInt16(4, 11776, true);
            view.setInt32(64, -1334123904, true);
            break;
        case ObjectType.ChairYellowPillow:
            view.setInt16(2, 512, true);
            view.setInt16(4, 13312, true);
            view.setInt32(64, -253697408, true);
            break;
        case ObjectType.OrangeWallWithHoleInMiddle:
            view.setInt16(2, 512, true);
            view.setInt16(4, 13056, true);
            view.setInt32(64, -1327635840, true);
            break;
        case ObjectType.GreyWallWithHoleInMiddle:
            view.setInt16(2, 512, true);
            view.setInt16(4, 8192, true);
            view.setInt32(64, 1080287872, true);
            break;
        case ObjectType.LongTable:
            view.setInt16(2, 2, true);
            view.setInt16(4, 45, true);
            view.setInt32(64, 78812400, true);
            break;
        case ObjectType.GBAStation:
            view.setInt16(4, -31232, true);
            view.setInt32(64, -532547200, true);
            break;
        case ObjectType.TalkLinkToSupport:
            view.setInt16(4, 21768, true);
            view.setInt16(6, 3, true);
            view.setFloat32(40, 22, true); // Scale x
            view.setInt32(64, -1067195776, true);
            break;
        case ObjectType.InstaWarp:
            view.setInt16(4, 2085, true);
            view.setInt16(6, 17, true);
            view.setFloat32(40, -9995, true); // Scale x
            view.setFloat32(48, -385, true); // Scale z
            view.setInt32(56, 4, true);
            view.setInt32(64, 206217872, true);
            break;
        case ObjectType.LabInvisibleObject:
            view.setInt16(2, 512, true);
            view.setInt16(4, 1536, true);
            view.setFloat32(40, 35, true); // Scale x
            view.setInt32(56, 1, true);
            view.setInt32(64, 1611394944, true);
            break;
        case ObjectType.LabGlassWindowDoor:
            view.setInt16(4, 23048, true);
            view.setInt32(64, 272035712, true);
            break;
        case ObjectType.LabCeilingWarp:
            view.setInt16(2, 2, true);
            view.setInt16(4, 24, true);
            view.setFloat32(40, -9990, true); // Scale x
            view.setFloat32(48, 60, true); // Scale z
            view.setInt32(60, 2, true);
            view.setInt32(64, 79130448, true);
            break;
        case ObjectType.Cactus:
            view.setInt16(4, 1495, true);
            view.setInt16(6, 1, true);
            view.setFloat32(40, 2, true); // Scale x
            view.setFloat32(44, 0.5, true); // Scale y
            view.setInt32(64, 75997776, true);
            break;
        case ObjectType.BigBrownRock:
            view.setInt16(4, 1480, true);
            view.setInt16(6, 1, true);
            view.setUint32(52, 2, true); // Model
            view.setInt32(64, 77443856, true);
            break;
        case ObjectType.BreakableBrownRock:
            view.setInt16(4, 1486, true);
            view.setInt16(6, 2, true);
            view.setInt32(64, 77425824, true);
            break;
        case ObjectType.PoisonPlant:
            view.setInt16(4, 1412, true);
            view.setInt16(6, 6, true);
            view.setInt32(64, 37198464, true);
            break;
        case ObjectType.OozingDesertPlant:
            view.setInt16(4, 1409, true);
            view.setInt16(6, 6, true);
            view.setInt32(64, 37197792, true);
            break;
        case ObjectType.UnknownItem901:
            view.setInt16(4, 1476, true);
            view.setInt16(6, 6, true);
            view.setInt32(64, 125843920, true);
            break;
        case ObjectType.BigBlackRocks:
            view.setInt16(4, 1488, true);
            view.setInt16(6, 5, true);
            view.setUint32(52, 1, true); // Model
            view.setInt32(64, 53452336, true);
            break;
        case ObjectType.FallingRock:
            view.setInt16(4, 1579, true);
            view.setInt16(6, 8, true);
            view.setInt32(64, 36923424, true);
            break;
        case ObjectType.DesertPlantHasCollision:
            view.setInt16(4, 1278, true);
            view.setInt16(6, 7, true);
            view.setInt32(64, 42538928, true);
            break;
        case ObjectType.DesertFixedTypeBoxBreakableCrystals:
            view.setInt16(2, 2, true);
            view.setInt16(4, 1308, true);
            view.setInt16(6, 8, true);
            view.setInt32(64, 36805664, true);
            break;
        case ObjectType.UnknownItem910:
            view.setInt16(4, 1309, true);
            view.setInt16(6, 6, true);
            break;
        case ObjectType.BeeHive:
            view.setInt16(4, 1269, true);
            view.setInt16(6, 7, true);
            view.setInt32(64, 42531632, true);
            break;
        case ObjectType.Heat:
            view.setInt16(4, 1246, true);
            view.setInt16(6, 6, true);
            view.setFloat32(40, 251, true); // Radius
            view.setFloat32(44, 3, true); // Scale y
            view.setFloat32(48, 0, true); // Scale z
            view.setInt32(52, 137, true); // Fog index no.
            view.setInt32(56, 1, true);
            view.setInt32(60, 5, true);
            view.setInt32(64, 26818944, true);
            break;
        case ObjectType.UnknownItem961:
            view.setInt16(4, 1322, true);
            view.setInt16(6, 9, true);
            view.setInt32(64, 207524384, true);
            break;
    }
}
