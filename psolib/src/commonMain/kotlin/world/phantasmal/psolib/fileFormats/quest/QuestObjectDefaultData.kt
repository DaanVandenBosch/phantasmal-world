package world.phantasmal.psolib.fileFormats.quest

import world.phantasmal.psolib.buffer.Buffer

internal fun setObjectDefaultData(type: ObjectType, data: Buffer) {
    data.setFloat(40, 1f) // Scale x
    data.setFloat(44, 1f) // Scale y
    data.setFloat(48, 1f) // Scale z

    @Suppress("NON_EXHAUSTIVE_WHEN")
    when (type) {
        ObjectType.PlayerSet -> {
            data.setShort(2, 2)
            data.setShort(4, 4)
            data.setFloat(40, 0f) // Slot ID
            data.setInt(64, -1929687552)
        }
        ObjectType.Particle -> {
            data.setShort(4, 5639)
            data.setShort(6, 10)
            data.setFloat(40, 38f) // Scale x
            data.setInt(64, 1351475840)
        }
        ObjectType.Teleporter -> {
            data.setShort(2, 2)
            data.setShort(4, 90)
            data.setShort(6, 1)
            data.setInt(64, -1929650432)
        }
        ObjectType.Warp -> {
            data.setShort(4, 1332)
            data.setShort(6, 4)
            data.setFloat(40, 0f) // Destination x
            data.setFloat(44, 0f) // Destination y
            data.setFloat(48, 0f) // Destination z
            data.setInt(52, 0) // Dst. rotation y
            data.setInt(64, -1929729040)
        }
        ObjectType.LightCollision -> {
            data.setFloat(48, 0.5f) // Scale z
            data.setInt(56, 26112)
            data.setInt(64, -802624128)
        }
        ObjectType.FogCollision -> {
            data.setShort(4, 2019)
            data.setShort(6, 10)
            data.setFloat(40, 140f) // Radius
            data.setInt(52, 17) // Fog index no.
            data.setInt(64, 36780240)
        }
        ObjectType.EventCollision -> {
            data.setShort(4, 2131)
            data.setShort(6, 8)
            data.setFloat(40, 30f) // Radius
            data.setInt(64, 71359776)
        }
        ObjectType.ElementalTrap -> {
            data.setFloat(40, 0f) // Radius
            data.setFloat(44, 0f) // Scale y
            data.setFloat(48, -1f) // Trap link
            data.setInt(52, 100) // Damage
            data.setInt(60, 20) // Delay
        }
        ObjectType.StatusTrap -> {
            data.setFloat(40, 0f) // Radius
            data.setFloat(44, 0f) // Scale y
            data.setFloat(48, -1f) // Trap link
            data.setInt(56, 17) // Subtype
            data.setInt(60, 20) // Delay
        }
        ObjectType.HealTrap -> {
            data.setShort(4, 1566)
            data.setShort(6, 4)
            data.setFloat(40, -32.00001525878906f) // Radius
            data.setFloat(44, -60.00002670288086f) // Scale y
            data.setFloat(48, -1.000000238418579f) // Trap link
            data.setInt(52, 200) // HP
            data.setInt(60, 60) // Delay
            data.setInt(64, -1929453936)
        }
        ObjectType.LargeElementalTrap -> {
            data.setFloat(40, 0f) // Radus
            data.setFloat(44, 0f) // Scale y
            data.setFloat(48, -1f) // Trap link
            data.setInt(52, 200) // Damage
            data.setInt(60, 50) // Delay
        }
        ObjectType.ObjRoomID -> {
            data.setShort(4, 1545)
            data.setShort(6, 1)
            data.setFloat(40, 3.9999988079071045f) // SCL_TAMA
            data.setFloat(44, 5f) // Next section
            data.setFloat(48, 10f) // Previous section
            data.setInt(56, 16384)
            data.setInt(64, -1929753600)
        }
        ObjectType.ScriptCollision -> {
            data.setShort(4, 1771)
            data.setShort(6, 14)
            data.setFloat(40, 250.00009155273438f) // Radius
            data.setInt(64, -1929714224)
        }
        ObjectType.HealRing -> {
            data.setFloat(40, 0f) // Scale x
            data.setFloat(44, 0f) // Scale y
            data.setFloat(48, 0f) // Scale z
        }
        ObjectType.ItemLight -> {
            data.setShort(4, 1990)
            data.setShort(6, 8)
            data.setFloat(40, 3f) // Subtype
            data.setInt(64, 39205168)
        }
        ObjectType.FogCollisionSW -> {
            data.setShort(4, 1550)
            data.setShort(6, 3)
            data.setFloat(40, 240.0000762939453f) // Radius
            data.setFloat(48, 0f) // Scale z
            data.setInt(52, 17) // Fog index no.
            data.setInt(64, -1929715680)
        }
        ObjectType.BossTeleporter -> {
            data.setShort(4, 1555)
            data.setShort(6, 2)
            data.setInt(56, 31)
            data.setInt(64, -1929731696)
        }
        ObjectType.ImageBoard -> {
            data.setShort(4, 384)
            data.setFloat(40, 0.5f) // Scale x
            data.setFloat(44, 2.000000476837158f) // Scale y
            data.setFloat(48, 1.000000238418579f) // Scale z
            data.setInt(64, 72261968)
        }
        ObjectType.QuestWarp -> {
            data.setShort(4, 1555)
            data.setShort(6, 2)
            data.setInt(60, 1)
            data.setInt(64, -1929711344)
        }
        ObjectType.BoxDetectObject -> {
            data.setShort(4, 1776)
            data.setShort(6, 5)
            data.setFloat(40, 30.000015258789062f) // Radius
            data.setInt(56, 6)
            data.setInt(60, 3)
            data.setInt(64, -1929661840)
        }
        ObjectType.SymbolChatObject -> {
            data.setShort(4, 1792)
            data.setShort(6, 5)
            data.setFloat(40, 30.000015258789062f) // Radius
            data.setInt(56, 30)
            data.setInt(60, 30)
            data.setInt(64, -1929667104)
        }
        ObjectType.TouchPlateObject -> {
            data.setShort(4, 26119)
            data.setShort(6, 11)
            data.setFloat(40, 8f) // Radius
            data.setInt(56, -1)
            data.setInt(64, -259264640)
        }
        ObjectType.TargetableObject -> {
            data.setShort(4, -23032)
            data.setShort(6, 2)
            data.setFloat(40, -1f) // Scale x
            data.setFloat(48, 0f) // Switch ID
            data.setInt(52, 2) // HP
            data.setInt(64, -2144604032)
        }
        ObjectType.EffectObject -> {
            data.setShort(4, 25863)
            data.setShort(6, 11)
            data.setFloat(40, 0f) // Scale x
            data.setFloat(44, 0f) // Scale y
            data.setInt(60, 1)
            data.setInt(64, -259199104)
        }
        ObjectType.CountDownObject -> {
            data.setShort(4, -31992)
            data.setShort(6, 1)
            data.setInt(64, -252124544)
        }
        ObjectType.UnknownItem39 -> {
            data.setShort(4, 30728)
            data.setShort(6, 1)
            data.setInt(56, 65641)
            data.setInt(64, 821944960)
        }
        ObjectType.MenuActivation -> {
            data.setShort(2, 2)
            data.setInt(64, -1929688192)
        }
        ObjectType.TelepipeLocation -> {
            data.setShort(2, 2)
            data.setShort(4, 13)
            data.setInt(64, -1929685680)
        }
        ObjectType.BGMCollision -> {
            data.setShort(2, 512)
            data.setShort(4, 11520)
            data.setFloat(40, 500f) // Scale x
            data.setInt(64, -265293952)
        }
        ObjectType.MainRagolTeleporter -> {
            data.setShort(4, 1879)
            data.setFloat(40, 1.000000238418579f) // Scale x
            data.setInt(64, -1929740080)
        }
        ObjectType.LobbyTeleporter -> {
            data.setShort(2, 512)
            data.setInt(64, -1073221760)
        }
        ObjectType.PrincipalWarp -> {
            data.setShort(2, 2)
            data.setShort(4, 9)
            data.setFloat(40, 10f) // Destination x
            data.setFloat(44, 0f) // Destination y
            data.setFloat(48, -1760.001f) // Destination z
            data.setInt(52, 32768) // Dst. rotation y
            data.setInt(56, 65536)
            data.setInt(64, -1929686608)
        }
        ObjectType.ShopDoor -> {
            data.setShort(2, 2)
            data.setShort(4, 18)
            data.setFloat(40, 3.0000007152557373f) // Scale x
            data.setInt(64, -1929684656)
        }
        ObjectType.HuntersGuildDoor -> {
            data.setShort(2, 2)
            data.setShort(4, 20)
            data.setFloat(40, 3.0000007152557373f) // Scale x
            data.setInt(64, -1929684240)
        }
        ObjectType.TeleporterDoor -> {
            data.setShort(2, 2)
            data.setShort(4, 21)
            data.setFloat(40, 3.0000007152557373f) // Scale x
            data.setInt(64, -1929683984)
        }
        ObjectType.MedicalCenterDoor -> {
            data.setShort(2, 2)
            data.setShort(4, 17)
            data.setFloat(40, 3.0000007152557373f) // Scale x
            data.setInt(64, -1929684912)
        }
        ObjectType.Sonic -> {
            data.setShort(2, 2)
            data.setInt(52, 1) // Model
            data.setInt(64, 79126144)
        }
        ObjectType.WelcomeBoard -> {
            data.setShort(4, 23304)
            data.setInt(64, -1864965504)
        }
        ObjectType.LobbyScreenDoor -> {
            data.setShort(2, 2)
            data.setShort(4, 25)
            data.setInt(64, 118136448)
        }
        ObjectType.LabTeleporterDoor -> {
            data.setShort(4, 16904)
            data.setInt(64, -266145920)
        }
        ObjectType.Pioneer2InvisibleTouchplate -> {
            data.setShort(2, 2)
            data.setShort(4, 27)
            data.setFloat(40, 150f) // Radius
            data.setInt(64, 70133216)
        }
        ObjectType.ForestDoor -> {
            data.setShort(4, 1581)
            data.setShort(6, 1)
            data.setFloat(40, 0.9999980926513672f) // Scale x
            data.setFloat(44, 1.000000238418579f) // Scale y
            data.setFloat(48, 0.9999954700469971f) // Scale z
            data.setInt(64, -1929757168)
        }
        ObjectType.ForestSwitch -> {
            data.setShort(4, 1543)
            data.setShort(6, 1)
            data.setFloat(40, 1.000000238418579f) // Scale x
            data.setFloat(44, 1.0000005960464478f) // Scale y
            data.setFloat(48, 1.0000001192092896f) // Scale z
            data.setInt(60, 7) // Color
            data.setInt(64, -1929750128)
        }
        ObjectType.LaserFence -> {
            data.setShort(4, 1542)
            data.setShort(6, 1)
            data.setFloat(40, 0f) // Color
            data.setFloat(44, 1.0000009536743164f) // Scale y
            data.setFloat(48, 0.9999961853027344f) // Scale z
            data.setInt(64, -1929756272)
        }
        ObjectType.LaserSquareFence -> {
            data.setShort(4, 1468)
            data.setShort(6, 3)
            data.setFloat(40, 1.000000238418579f) // Color
            data.setInt(60, 1) // Model
            data.setInt(64, -1929753744)
        }
        ObjectType.ForestLaserFenceSwitch -> {
            data.setShort(4, 1478)
            data.setShort(6, 3)
            data.setInt(56, 3)
            data.setInt(60, 3) // Color
            data.setInt(64, -1929696640)
        }
        ObjectType.LightRays -> {
            data.setShort(4, 2369)
            data.setShort(6, 8)
            data.setFloat(44, 10f) // Scale y
            data.setFloat(48, 20f) // Scale z
            data.setInt(64, 74962176)
        }
        ObjectType.BlueButterfly -> {
            data.setShort(4, 2048)
            data.setShort(6, 2)
            data.setInt(64, -1929720176)
        }
        ObjectType.Probe -> {
            data.setShort(2, 2)
            data.setShort(4, 80)
            data.setShort(6, 1)
            data.setFloat(40, 0f) // Model
            data.setFloat(44, 0f) // Scale y
            data.setFloat(48, 0f) // Scale z
            data.setInt(64, -1929746928)
        }
        ObjectType.RandomTypeBox1 -> {
            data.setShort(4, 1635)
            data.setShort(6, 2)
            data.setFloat(40, 8.000000953674316f) // Scale x
            data.setFloat(44, 3.1000001430511475f) // Scale y
            data.setFloat(48, 10.100005149841309f) // Scale z
            data.setInt(56, 57344)
            data.setInt(64, -1929730096)
        }
        ObjectType.ForestWeatherStation -> {
            data.setShort(2, 2)
            data.setShort(4, 147)
            data.setShort(6, 2)
            data.setFloat(40, 1.000000238418579f) // Scale x
            data.setFloat(44, 1.0000016689300537f) // Scale y
            data.setFloat(48, 1.0000005960464478f) // Scale z
            data.setInt(56, 2816)
            data.setInt(64, -1929753232)
        }
        ObjectType.ForestConsole -> {
            data.setShort(4, 1331)
            data.setShort(6, 2)
            data.setFloat(40, 0f) // Scale x
            data.setInt(52, 1050) // Script label
            data.setInt(64, -1929750848)
        }
        ObjectType.BlackSlidingDoor -> {
            data.setShort(4, 1625)
            data.setShort(6, 1)
            data.setFloat(40, 30.000015258789062f) // Distance
            data.setFloat(44, 0.8999999761581421f) // Speed
            data.setInt(52, 101) // Switch no.
            data.setInt(64, -1929721536)
        }
        ObjectType.RicoMessagePod -> {
            data.setShort(4, 1654)
            data.setShort(6, 13)
            data.setInt(60, 259)
            data.setInt(64, -1929755408)
        }
        ObjectType.EnergyBarrier -> {
            data.setShort(4, 1342)
            data.setShort(6, 1)
            data.setFloat(40, 15.000005722045898f) // Scale x
            data.setFloat(44, 1.0000001192092896f) // Scale y
            data.setFloat(48, 1.000000238418579f) // Scale z
            data.setInt(52, -1) // Door ID
            data.setInt(64, -1929730384)
        }
        ObjectType.ForestRisingBridge -> {
            data.setShort(2, 2)
            data.setShort(4, 145)
            data.setShort(6, 2)
            data.setFloat(40, 1.000000238418579f) // Scale x
            data.setFloat(44, 1.0000015497207642f) // Scale y
            data.setFloat(48, 1.0000005960464478f) // Scale z
            data.setInt(52, -1) // Door ID
            data.setInt(56, 2816)
            data.setInt(64, -1929751104)
        }
        ObjectType.SwitchNoneDoor -> {
            data.setShort(4, 1495)
            data.setShort(6, 2)
            data.setFloat(40, 0f) // Scale x
            data.setInt(64, -1929748688)
        }
        ObjectType.EnemyBoxGrey -> {
            data.setShort(4, 1525)
            data.setShort(6, 5)
            data.setInt(64, -1929732576)
        }
        ObjectType.FixedTypeBox -> {
            data.setFloat(40, 0f) // Full random
            data.setFloat(48, 0f) // Fixed item
        }
        ObjectType.EmptyTypeBox -> {
            data.setShort(2, 512)
            data.setShort(4, 32001)
            data.setShort(6, 6)
            data.setInt(64, -1862737024)
        }
        ObjectType.LaserFenceEx -> {
            data.setShort(4, 28167)
            data.setShort(6, 11)
            data.setFloat(40, 0f) // Color
            data.setFloat(44, 8f) // Collision width
            data.setFloat(48, 25f) // Collision depth
            data.setInt(64, -526061696)
        }
        ObjectType.FloorPanel1 -> {
            data.setShort(4, 1556)
            data.setShort(6, 4)
            data.setFloat(40, 1.000000238418579f) // Scale  x
            data.setFloat(44, 1.000000238418579f) // Scale  y
            data.setFloat(48, 0.9990062713623047f) // Scale  z
            data.setInt(64, -1929651776)
        }
        ObjectType.Caves4ButtonDoor -> {
            data.setShort(2, 2)
            data.setShort(4, 542)
            data.setShort(6, 4)
            data.setInt(52, -1) // Door ID
            data.setInt(64, -1929625056)
        }
        ObjectType.CavesNormalDoor -> {
            data.setShort(4, 1464)
            data.setShort(6, 4)
            data.setFloat(40, 0.9999891519546509f) // Scale x
            data.setFloat(44, 1.000000238418579f) // Scale y
            data.setFloat(48, 0.9990062713623047f) // Scale z
            data.setInt(52, -1) // Door ID
            data.setInt(56, 3)
            data.setInt(64, -1929741968)
        }
        ObjectType.CavesSmashingPillar -> {
            data.setShort(4, 1559)
            data.setShort(6, 3)
            data.setInt(64, -1929691696)
        }
        ObjectType.CavesSign1 -> {
            data.setShort(2, 512)
            data.setShort(4, -23552)
            data.setShort(6, 5)
            data.setInt(64, 814801792)
        }
        ObjectType.CavesSign2 -> {
            data.setShort(2, 512)
            data.setShort(4, 26624)
            data.setShort(6, 5)
            data.setInt(64, 1884021632)
        }
        ObjectType.CavesSign3 -> {
            data.setShort(2, 512)
            data.setShort(4, -18944)
            data.setShort(6, 5)
            data.setInt(64, 280945536)
        }
        ObjectType.HexagonalTank -> {
            data.setShort(2, 2)
            data.setShort(4, 740)
            data.setShort(6, 5)
            data.setInt(64, -1334188928)
        }
        ObjectType.BrownPlatform -> {
            data.setShort(2, 2)
            data.setShort(4, 559)
            data.setShort(6, 5)
            data.setInt(64, 276292480)
        }
        ObjectType.FloatingDragonfly -> {
            data.setShort(4, 1517)
            data.setShort(6, 6)
            data.setInt(64, 37198656)
        }
        ObjectType.CavesSwitchDoor -> {
            data.setShort(4, 1450)
            data.setShort(6, 3)
            data.setInt(52, -1) // Door ID
            data.setInt(56, 3)
            data.setInt(64, -1929685552)
        }
        ObjectType.RobotRechargeStation -> {
            data.setShort(2, 512)
            data.setShort(4, -29952)
            data.setShort(6, 5)
            data.setInt(64, 275440512)
        }
        ObjectType.CavesCakeShop -> {
            data.setShort(4, -6905)
            data.setShort(6, 5)
            data.setInt(64, -1067911552)
        }
        ObjectType.Caves1SmallRedRock -> {
            data.setShort(4, 1965)
            data.setShort(6, 3)
            data.setInt(64, -1929677200)
        }
        ObjectType.Caves1MediumRedRock -> {
            data.setShort(4, 1968)
            data.setShort(6, 3)
            data.setInt(64, -1929811824)
        }
        ObjectType.Caves1LargeRedRock -> {
            data.setShort(4, 1552)
            data.setShort(6, 3)
            data.setInt(64, -1929724048)
        }
        ObjectType.Caves2SmallRock1 -> {
            data.setShort(4, 1563)
            data.setShort(6, 4)
            data.setFloat(40, 1.000000238418579f) // Scale x
            data.setFloat(44, 1.000000238418579f) // Scale y
            data.setInt(64, -1929710640)
        }
        ObjectType.Caves2MediumRock1 -> {
            data.setShort(2, 2)
            data.setShort(4, 569)
            data.setShort(6, 4)
            data.setFloat(40, -20.000011444091797f) // Scale x
            data.setFloat(44, -80.00003814697266f) // Scale y
            data.setFloat(48, -1.000000238418579f) // Scale z
            data.setInt(60, 40)
            data.setInt(64, -1929628992)
        }
        ObjectType.Caves2LargeRock1 -> {
            data.setShort(4, 2051)
            data.setShort(6, 4)
            data.setInt(64, -1929711328)
        }
        ObjectType.Caves2SmallRock2 -> {
            data.setShort(4, 1961)
            data.setShort(6, 4)
            data.setInt(64, -1929702432)
        }
        ObjectType.Caves2MediumRock2 -> {
            data.setShort(4, 1550)
            data.setShort(6, 4)
            data.setFloat(40, 1.000000238418579f) // Scale x
            data.setFloat(44, 1.000000238418579f) // Scale y
            data.setFloat(48, 0.9990062713623047f) // Scale z
            data.setInt(64, -1929604016)
        }
        ObjectType.Caves2LargeRock2 -> {
            data.setShort(4, 1517)
            data.setShort(6, 4)
            data.setInt(64, -1929749872)
        }
        ObjectType.Caves3SmallRock -> {
            data.setShort(4, 1722)
            data.setShort(6, 5)
            data.setInt(64, -1929727392)
        }
        ObjectType.Caves3MediumRock -> {
            data.setShort(4, 1580)
            data.setShort(6, 5)
            data.setInt(64, -1929674160)
        }
        ObjectType.Caves3LargeRock -> {
            data.setShort(4, -18425)
            data.setShort(6, 5)
            data.setInt(64, 279115136)
        }
        ObjectType.FloorPanel2 -> {
            data.setShort(4, 23560)
            data.setShort(6, 1)
            data.setInt(60, 65537)
            data.setInt(64, 16310912)
        }
        ObjectType.DestructableRockCaves1 -> {
            data.setShort(2, 512)
            data.setShort(4, 23553)
            data.setShort(6, 3)
            data.setInt(64, 10347136)
        }
        ObjectType.DestructableRockCaves2 -> {
            data.setShort(4, 2006)
            data.setShort(6, 4)
            data.setInt(64, -1929719824)
        }
        ObjectType.DestructableRockCaves3 -> {
            data.setShort(4, 1617)
            data.setShort(6, 5)
            data.setInt(64, -1929688384)
        }
        ObjectType.MinesDoor -> {
            data.setShort(2, 2)
            data.setShort(4, 715)
            data.setShort(6, 6)
            data.setInt(52, -1) // Door ID
            data.setInt(56, 3) // Switch total
            data.setInt(64, -1929747520)
        }
        ObjectType.FloorPanel3 -> {
            data.setShort(4, 23304)
            data.setShort(6, 3)
            data.setInt(56, -1) // Stay active
            data.setInt(64, 1889001856)
        }
        ObjectType.MinesSwitchDoor -> {
            data.setShort(4, 1887)
            data.setShort(6, 6)
            data.setInt(52, -1) // Door ID
            data.setInt(64, -1929658608)
        }
        ObjectType.LargeCryoTube -> {
            data.setShort(2, 512)
            data.setShort(4, 28161)
            data.setShort(6, 6)
            data.setInt(64, -1329536128)
        }
        ObjectType.ComputerLikeCalus -> {
            data.setShort(4, 2006)
            data.setShort(6, 6)
            data.setFloat(44, 0f) // Scale y
            data.setInt(64, -1929695952)
        }
        ObjectType.GreenScreenOpeningAndClosing -> {
            data.setShort(4, 4866)
            data.setShort(6, 6)
            data.setInt(64, 281469568)
        }
        ObjectType.FloatingRobot -> {
            data.setShort(4, 1533)
            data.setShort(6, 6)
            data.setFloat(40, -35.00001525878906f) // Scale x
            data.setFloat(44, 20.000011444091797f) // Scale y
            data.setInt(64, -1929732496)
        }
        ObjectType.MinesLargeFlashingCrate -> {
            data.setShort(4, 1546)
            data.setShort(6, 6)
            data.setFloat(40, 1.000000238418579f) // Scale x
            data.setFloat(44, 0f) // Scale y
            data.setInt(64, -1929727328)
        }
        ObjectType.RuinsSeal -> {
            data.setShort(4, 1550)
            data.setShort(6, 13)
            data.setInt(64, -1929755808)
        }
        ObjectType.RuinsTeleporter -> {
            data.setShort(4, 1483)
            data.setShort(6, 8)
            data.setInt(64, -1929759760)
        }
        ObjectType.RuinsWarpSiteToSite -> {
            data.setShort(4, 2017)
            data.setShort(6, 8)
            data.setFloat(40, 0f) // Destination x
            data.setFloat(44, 0f) // Destination y
            data.setFloat(48, 0f) // Destination z
            data.setInt(64, 39228864)
        }
        ObjectType.RuinsSwitch -> {
            data.setShort(4, 1910)
            data.setShort(6, 8)
            data.setInt(64, -1929687952)
        }
        ObjectType.FloorPanel4 -> {
            data.setShort(4, 1660)
            data.setShort(6, 9)
            data.setFloat(40, 0f) // Scale x
            data.setFloat(44, 0f) // Scale y
            data.setFloat(48, 0f) // Scale z
            data.setInt(64, -1929749648)
        }
        ObjectType.Ruins1Door -> {
            data.setFloat(44, 0f) // Scale y
            data.setFloat(48, 0f) // Scale z
            data.setInt(52, -1) // Door ID
        }
        ObjectType.Ruins3Door -> {
            data.setFloat(44, 0f) // Scale y
            data.setFloat(48, 0f) // Scale z
            data.setInt(52, -1) // Door ID
        }
        ObjectType.Ruins2Door -> {
            data.setFloat(44, 0f) // Scale y
            data.setFloat(48, 0f) // Scale z
            data.setInt(52, -1) // Door ID
        }
        ObjectType.Ruins11ButtonDoor -> {
            data.setShort(4, 31751)
            data.setShort(6, 8)
            data.setInt(52, -1) // Door ID
            data.setInt(64, -1874992256)
        }
        ObjectType.Ruins21ButtonDoor -> {
            data.setShort(2, 2)
            data.setShort(4, 1371)
            data.setShort(6, 9)
            data.setInt(52, -1) // Door ID
            data.setInt(64, -1929663984)
        }
        ObjectType.Ruins4ButtonDoor -> {
            data.setShort(4, 1480)
            data.setShort(6, 8)
            data.setInt(52, -1) // Door ID
            data.setInt(60, -1) // Stay active
            data.setInt(64, -1929730336)
        }
        ObjectType.Ruins2ButtonDoor -> {
            data.setShort(4, 1909)
            data.setShort(6, 8)
            data.setInt(52, -1) // Door ID
            data.setInt(64, -1929493856)
        }
        ObjectType.RuinsFenceSwitch -> {
            data.setShort(4, 1899)
            data.setShort(6, 8)
            data.setInt(56, 1) // Color
            data.setInt(64, -1929707856)
        }
        ObjectType.RuinsLaserFence4x2 -> {
            data.setFloat(40, 0f) // Scale x
            data.setFloat(44, 0f) // Scale y
            data.setFloat(48, 0f) // Scale z
            data.setInt(56, 1) // Color
        }
        ObjectType.RuinsLaserFence6x2 -> {
            data.setFloat(40, 0f) // Scale x
            data.setFloat(44, 0f) // Scale y
            data.setFloat(48, 0f) // Scale z
            data.setInt(56, 1) // Color
        }
        ObjectType.RuinsLaserFence4x4 -> {
            data.setShort(4, 2064)
            data.setShort(6, 9)
            data.setInt(56, 1) // Color
            data.setInt(64, -1929548960)
        }
        ObjectType.RuinsPillarTrap -> {
            data.setFloat(40, -25f) // Scale x
            data.setFloat(44, 100f) // Scale y
            data.setFloat(48, 15f) // Scale z
            data.setInt(60, 60)
        }
        ObjectType.PopupTrapNoTech -> {
            data.setShort(2, 2)
            data.setShort(4, 1432)
            data.setShort(6, 9)
            data.setFloat(40, 1.000000238418579f) // Radius
            data.setInt(64, -1929630640)
        }
        ObjectType.RuinsCrystal -> {
            data.setFloat(40, 0f) // Scale x
            data.setFloat(44, 0f) // Scale y
            data.setFloat(48, 0f) // Scale z
        }
        ObjectType.Monument -> {
            data.setShort(4, 1557)
            data.setShort(6, 4)
            data.setFloat(40, 1.000000238418579f) // Scale x
            data.setFloat(44, 1.000000238418579f) // Scale y
            data.setFloat(48, 1.000000238418579f) // Scale z
            data.setInt(64, -1929727536)
        }
        ObjectType.RuinsRock1 -> {
            data.setFloat(40, 0f) // Scale x
            data.setFloat(44, 0f) // Scale y
            data.setFloat(48, 0f) // Scale z
        }
        ObjectType.RuinsRock2 -> {
            data.setFloat(40, 0f) // Scale x
            data.setFloat(44, 0f) // Scale y
            data.setFloat(48, 0f) // Scale z
        }
        ObjectType.RuinsRock3 -> {
            data.setFloat(40, 0f) // Scale x
            data.setFloat(44, 0f) // Scale y
            data.setFloat(48, 0f) // Scale z
        }
        ObjectType.RuinsRock4 -> {
            data.setFloat(40, 0f) // Scale x
            data.setFloat(44, 0f) // Scale y
            data.setFloat(48, 0f) // Scale z
        }
        ObjectType.RuinsRock5 -> {
            data.setFloat(40, 0f) // Scale x
            data.setFloat(44, 0f) // Scale y
            data.setFloat(48, 0f) // Scale z
        }
        ObjectType.RuinsRock6 -> {
            data.setFloat(40, 0f) // Scale x
            data.setFloat(44, 0f) // Scale y
            data.setFloat(48, 0f) // Scale z
        }
        ObjectType.RuinsRock7 -> {
            data.setShort(4, 1548)
            data.setShort(6, 8)
            data.setInt(64, -1929795888)
        }
        ObjectType.Poison -> {
            data.setShort(4, 1983)
            data.setShort(6, 13)
            data.setInt(52, 8) // Switch mode
            data.setInt(64, -1929757344)
        }
        ObjectType.FixedBoxTypeRuins -> {
            data.setFloat(40, 0f) // Full random
            data.setFloat(44, 0f) // Random item
            data.setFloat(48, 0f) // Fixed item
        }
        ObjectType.RandomBoxTypeRuins -> {
            data.setFloat(40, 0f) // Scale x
            data.setFloat(44, 0f) // Scale y
            data.setFloat(48, 0f) // Scale z
        }
        ObjectType.EnemyTypeBoxYellow -> {
            data.setShort(4, 2043)
            data.setShort(6, 10)
            data.setInt(64, 39241584)
        }
        ObjectType.DestructableRock -> {
            data.setShort(2, 2)
            data.setShort(4, 1517)
            data.setShort(6, 9)
            data.setInt(64, -1929579040)
        }
        ObjectType.PopupTrapsTechs -> {
            data.setFloat(40, 50f) // Radius
            data.setFloat(44, 0f) // HP
            data.setFloat(48, 30f) // Scale z
            data.setInt(56, -1) // Action
            data.setInt(60, 2) // Tech
        }
        ObjectType.GreyWallLow -> {
            data.setShort(4, 2095)
            data.setShort(6, 17)
            data.setInt(64, 206220336)
        }
        ObjectType.SpaceshipDoor -> {
            data.setShort(4, -18681)
            data.setShort(6, 18)
            data.setInt(64, -254418560)
        }
        ObjectType.GreyWallHigh -> {
            data.setShort(4, 28424)
            data.setShort(6, 3)
            data.setInt(64, 548332416)
        }
        ObjectType.TempleNormalDoor -> {
            data.setShort(4, 1638)
            data.setShort(6, 17)
            data.setInt(64, 1356460160)
        }
        ObjectType.BreakableWallWallButUnbreakable -> {
            data.setShort(4, 21505)
            data.setShort(6, 2)
            data.setInt(64, -521801600)
        }
        ObjectType.BrokenCylinderAndRubble -> {
            data.setShort(4, 30984)
            data.setShort(6, 1)
            data.setInt(64, 1077601152)
        }
        ObjectType.ThreeBrokenWallPiecesOnFloor -> {
            data.setShort(4, -28409)
            data.setShort(6, 1)
            data.setInt(64, -255991424)
        }
        ObjectType.HighBrickCylinder -> {
            data.setShort(4, -19448)
            data.setShort(6, 1)
            data.setInt(64, -1597054592)
        }
        ObjectType.LyingCylinder -> {
            data.setShort(4, 16136)
            data.setShort(6, 2)
            data.setInt(64, -1069292672)
        }
        ObjectType.BrickConeWithFlatTop -> {
            data.setShort(4, 4361)
            data.setShort(6, 1)
            data.setInt(64, -1864506752)
        }
        ObjectType.BreakableTempleWall -> {
            data.setShort(4, -18936)
            data.setShort(6, 1)
            data.setInt(64, -1873157504)
        }
        ObjectType.TempleMapDetect -> {
            data.setShort(4, 17416)
            data.setShort(6, 14)
            data.setFloat(40, 0f) // Scale x
            data.setInt(64, -262672512)
        }
        ObjectType.SmallBrownBrickRisingBridge -> {
            data.setShort(4, -24824)
            data.setShort(6, 1)
            data.setFloat(40, 0.5f) // Scale x
            data.setFloat(44, 0.5f) // Scale y
            data.setInt(64, -1601638272)
        }
        ObjectType.LongRisingBridgeWithPinkHighEdges -> {
            data.setShort(4, 32264)
            data.setShort(6, 1)
            data.setFloat(40, 0.29999998211860657f) // Scale x
            data.setFloat(44, 0.29999998211860657f) // Scale y
            data.setInt(64, -265231488)
        }
        ObjectType.FourSwitchTempleDoor -> {
            data.setShort(4, 1289)
            data.setShort(6, 1)
            data.setInt(56, 1)
            data.setInt(64, -254680448)
        }
        ObjectType.FourButtonSpaceshipDoor -> {
            data.setShort(4, 23048)
            data.setShort(6, 3)
            data.setInt(56, 2)
            data.setInt(60, -1)
            data.setInt(64, 1620500864)
        }
        ObjectType.ItemBoxCca -> {
            data.setShort(2, 512)
            data.setShort(4, 22019)
            data.setShort(6, 5)
            data.setInt(64, -1599409280)
        }
        ObjectType.TeleporterEp2 -> {
            data.setShort(4, 2110)
            data.setShort(6, 16)
            data.setInt(60, 1)
            data.setInt(64, 71221728)
        }
        ObjectType.CcaDoor -> {
            data.setShort(2, 512)
            data.setShort(4, 21763)
            data.setShort(6, 5)
            data.setInt(56, 1) // Switch amount
            data.setInt(64, -2136345728)
        }
        ObjectType.SpecialBoxCca -> {
            data.setShort(2, 512)
            data.setShort(4, -15612)
            data.setShort(6, 7)
            data.setFloat(40, 0f) // Scale x
            data.setFloat(48, 0f) // Scale z
            data.setInt(64, -1871846784)
        }
        ObjectType.BigCcaDoor -> {
            data.setShort(4, 2118)
            data.setShort(6, 5)
            data.setInt(64, 70753104)
        }
        ObjectType.BigCcaDoorSwitch -> {
            data.setShort(4, 2111)
            data.setShort(6, 6)
            data.setInt(64, 71229312)
        }
        ObjectType.LittleRock -> {
            data.setShort(4, 2126)
            data.setShort(6, 16)
            data.setInt(64, 71221728)
        }
        ObjectType.Little3StoneWall -> {
            data.setShort(4, 2129)
            data.setShort(6, 16)
            data.setInt(64, 71227424)
        }
        ObjectType.Medium3StoneWall -> {
            data.setShort(4, 2129)
            data.setShort(6, 8)
            data.setInt(64, 71259376)
        }
        ObjectType.SpiderPlant -> {
            data.setShort(4, 2129)
            data.setShort(6, 16)
            data.setInt(64, 71231904)
        }
        ObjectType.OrangeBird -> {
            data.setShort(4, 2086)
            data.setShort(6, 9)
            data.setInt(64, 70881472)
        }
        ObjectType.Saw -> {
            data.setShort(4, 29959)
            data.setShort(6, 11)
            data.setFloat(44, 300f) // Speed
            data.setInt(56, 1073742320) // Arc
            data.setInt(60, 1) // Switch flag
            data.setInt(64, 547417984)
        }
        ObjectType.LaserDetect -> {
            data.setShort(4, 32519)
            data.setShort(6, 11)
            data.setInt(56, 1073742160) // Arc
            data.setInt(60, 2)
            data.setInt(64, 1082650496)
        }
        ObjectType.UnknownItem529 -> {
            data.setShort(4, 2139)
            data.setShort(6, 6)
            data.setInt(64, 71348128)
        }
        ObjectType.UnknownItem530 -> {
            data.setShort(4, 2046)
            data.setShort(6, 9)
            data.setInt(64, 206357088)
        }
        ObjectType.Seagull -> {
            data.setShort(4, 2120)
            data.setShort(6, 8)
            data.setInt(64, 71256080)
        }
        ObjectType.Fish -> {
            data.setShort(4, -29432)
            data.setShort(6, 9)
            data.setFloat(40, 11f) // Scale x
            data.setFloat(44, 0.5f) // Scale y
            data.setFloat(48, 11f) // Scale z
            data.setInt(64, 73579344)
        }
        ObjectType.SeabedDoorWithBlueEdges -> {
            data.setShort(4, 29959)
            data.setShort(6, 11)
            data.setInt(56, 1) // Switch amount
            data.setInt(64, 1611984768)
        }
        ObjectType.SeabedDoorAlwaysOpenNonTriggerable -> {
            data.setShort(4, 1884)
            data.setShort(6, 11)
            data.setInt(64, 45365632)
        }
        ObjectType.LittleCryotube -> {
            data.setShort(4, 2093)
            data.setShort(6, 17)
            data.setInt(64, 206221408)
        }
        ObjectType.WideGlassWallBreakable -> {
            data.setShort(2, 2048)
            data.setShort(4, 25863)
            data.setShort(6, 10)
            data.setInt(56, 1)
            data.setInt(64, 8712064)
        }
        ObjectType.CaptureTrap -> {
            data.setShort(4, 2123)
            data.setShort(6, 5)
            data.setFloat(40, 3f) // Scale x
            data.setFloat(44, 400f) // Scale y
            data.setInt(60, 3)
            data.setInt(64, 37000640)
        }
        ObjectType.VRLink -> {
            data.setShort(4, 21256)
            data.setShort(6, 3)
            data.setInt(64, -1068113280)
        }
        ObjectType.WarpInBarbaRayRoom -> {
            data.setShort(4, 17416)
            data.setShort(6, 14)
            data.setInt(60, 1)
            data.setInt(64, -266342528)
        }
        ObjectType.GeeNest -> {
            data.setShort(4, 28169)
            data.setShort(6, 6)
            data.setInt(60, -2)
            data.setInt(64, 8053376)
        }
        ObjectType.LabComputerConsole -> {
            data.setShort(4, 14600)
            data.setInt(64, 1075834752)
        }
        ObjectType.LabComputerConsoleGreenScreen -> {
            data.setShort(2, 512)
            data.setShort(4, 11776)
            data.setInt(64, -1334123904)
        }
        ObjectType.ChairYellowPillow -> {
            data.setShort(2, 512)
            data.setShort(4, 13312)
            data.setInt(64, -253697408)
        }
        ObjectType.OrangeWallWithHoleInMiddle -> {
            data.setShort(2, 512)
            data.setShort(4, 13056)
            data.setInt(64, -1327635840)
        }
        ObjectType.GreyWallWithHoleInMiddle -> {
            data.setShort(2, 512)
            data.setShort(4, 8192)
            data.setInt(64, 1080287872)
        }
        ObjectType.LongTable -> {
            data.setShort(2, 2)
            data.setShort(4, 45)
            data.setInt(64, 78812400)
        }
        ObjectType.GBAStation -> {
            data.setShort(4, -31232)
            data.setInt(64, -532547200)
        }
        ObjectType.TalkLinkToSupport -> {
            data.setShort(4, 21768)
            data.setShort(6, 3)
            data.setFloat(40, 22f) // Scale x
            data.setInt(64, -1067195776)
        }
        ObjectType.InstaWarp -> {
            data.setShort(4, 2085)
            data.setShort(6, 17)
            data.setFloat(40, -9995f) // Scale x
            data.setFloat(48, -385f) // Scale z
            data.setInt(56, 4)
            data.setInt(64, 206217872)
        }
        ObjectType.LabInvisibleObject -> {
            data.setShort(2, 512)
            data.setShort(4, 1536)
            data.setFloat(40, 35f) // Scale x
            data.setInt(56, 1)
            data.setInt(64, 1611394944)
        }
        ObjectType.LabGlassWindowDoor -> {
            data.setShort(4, 23048)
            data.setInt(64, 272035712)
        }
        ObjectType.LabCeilingWarp -> {
            data.setShort(2, 2)
            data.setShort(4, 24)
            data.setFloat(40, -9990f) // Scale x
            data.setFloat(48, 60f) // Scale z
            data.setInt(60, 2)
            data.setInt(64, 79130448)
        }
        ObjectType.Cactus -> {
            data.setShort(4, 1495)
            data.setShort(6, 1)
            data.setFloat(40, 2f) // Scale x
            data.setFloat(44, 0.5f) // Scale y
            data.setInt(64, 75997776)
        }
        ObjectType.BigBrownRock -> {
            data.setShort(4, 1480)
            data.setShort(6, 1)
            data.setInt(52, 2) // Model
            data.setInt(64, 77443856)
        }
        ObjectType.BreakableBrownRock -> {
            data.setShort(4, 1486)
            data.setShort(6, 2)
            data.setInt(64, 77425824)
        }
        ObjectType.PoisonPlant -> {
            data.setShort(4, 1412)
            data.setShort(6, 6)
            data.setInt(64, 37198464)
        }
        ObjectType.OozingDesertPlant -> {
            data.setShort(4, 1409)
            data.setShort(6, 6)
            data.setInt(64, 37197792)
        }
        ObjectType.UnknownItem901 -> {
            data.setShort(4, 1476)
            data.setShort(6, 6)
            data.setInt(64, 125843920)
        }
        ObjectType.BigBlackRocks -> {
            data.setShort(4, 1488)
            data.setShort(6, 5)
            data.setInt(52, 1) // Model
            data.setInt(64, 53452336)
        }
        ObjectType.FallingRock -> {
            data.setShort(4, 1579)
            data.setShort(6, 8)
            data.setInt(64, 36923424)
        }
        ObjectType.DesertPlantHasCollision -> {
            data.setShort(4, 1278)
            data.setShort(6, 7)
            data.setInt(64, 42538928)
        }
        ObjectType.DesertFixedTypeBoxBreakableCrystals -> {
            data.setShort(2, 2)
            data.setShort(4, 1308)
            data.setShort(6, 8)
            data.setInt(64, 36805664)
        }
        ObjectType.UnknownItem910 -> {
            data.setShort(4, 1309)
            data.setShort(6, 6)
        }
        ObjectType.BeeHive -> {
            data.setShort(4, 1269)
            data.setShort(6, 7)
            data.setInt(64, 42531632)
        }
        ObjectType.Heat -> {
            data.setShort(4, 1246)
            data.setShort(6, 6)
            data.setFloat(40, 251f) // Radius
            data.setFloat(44, 3f) // Scale y
            data.setFloat(48, 0f) // Scale z
            data.setInt(52, 137) // Fog index no.
            data.setInt(56, 1)
            data.setInt(60, 5)
            data.setInt(64, 26818944)
        }
        ObjectType.UnknownItem961 -> {
            data.setShort(4, 1322)
            data.setShort(6, 9)
            data.setInt(64, 207524384)
        }
    }
}
