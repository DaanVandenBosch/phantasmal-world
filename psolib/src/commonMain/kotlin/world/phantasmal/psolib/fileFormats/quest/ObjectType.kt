package world.phantasmal.psolib.fileFormats.quest

import world.phantasmal.psolib.Episode

enum class ObjectType(
    override val uniqueName: String,
    /**
     * The valid area IDs per episode in which this object can appear.
     */
    val areaIds: Map<Episode, List<Int>>,
    val typeId: Short?,
    /**
     * Default object-specific properties.
     */
    override val properties: List<EntityProp> = emptyList(),
) : EntityType {
    Unknown(
        uniqueName = "Unknown",
        areaIds = mapOf(),
        typeId = null,
    ),

    PlayerSet(
        uniqueName = "Player Set",
        areaIds = mapOf(
            Episode.I to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17),
            Episode.II to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0),
        ),
        typeId = 0,
        properties = listOf(
            EntityProp(name = "Slot ID", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Return flag", offset = 52, type = EntityPropType.I32),
        ),
    ),
    Particle(
        uniqueName = "Particle",
        areaIds = mapOf(
            Episode.I to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17),
            Episode.II to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8, 0),
        ),
        typeId = 1,
    ),
    Teleporter(
        uniqueName = "Teleporter",
        areaIds = mapOf(
            Episode.I to listOf(0, 1, 2, 3, 4, 5, 6, 7, 11, 12, 13, 14),
            Episode.II to listOf(0, 1, 2, 3, 4, 12, 13, 14, 15),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0),
        ),
        typeId = 2,
        properties = listOf(
            EntityProp(name = "Area ID", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Color blue", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Color red", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "Floor ID", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Display no.", offset = 56, type = EntityPropType.I32),
            EntityProp(name = "No display no.", offset = 60, type = EntityPropType.I32),
        ),
    ),
    Warp(
        uniqueName = "Warp",
        areaIds = mapOf(
            Episode.I to listOf(0, 1, 2, 3, 4, 5, 6, 7, 11, 12, 13, 14, 16, 17),
            Episode.II to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0),
        ),
        typeId = 3,
        properties = listOf(
            EntityProp(name = "Destination x", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Destination y", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Destination z", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "Dst. rotation y", offset = 52, type = EntityPropType.Angle),
        ),
    ),
    LightCollision(
        uniqueName = "Light Collision",
        areaIds = mapOf(
            Episode.I to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 16, 17),
            Episode.II to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8, 0),
        ),
        typeId = 4,
    ),
    Item(
        uniqueName = "Item",
        areaIds = mapOf(),
        typeId = 5,
    ),
    EnvSound(
        uniqueName = "Env Sound",
        areaIds = mapOf(
            Episode.I to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 16, 17),
            Episode.II to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8, 0),
        ),
        typeId = 6,
        properties = listOf(
            EntityProp(name = "Radius", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "SE", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Volume", offset = 56, type = EntityPropType.I32),
        ),
    ),
    FogCollision(
        uniqueName = "Fog Collision",
        areaIds = mapOf(
            Episode.I to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 16, 17),
            Episode.II to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8, 0),
        ),
        typeId = 7,
        properties = listOf(
            EntityProp(name = "Radius", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Fog index no.", offset = 52, type = EntityPropType.I32),
        ),
    ),
    EventCollision(
        uniqueName = "Event Collision",
        areaIds = mapOf(
            Episode.I to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 16, 17),
            Episode.II to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0),
        ),
        typeId = 8,
        properties = listOf(
            EntityProp(name = "Radius", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Event ID", offset = 52, type = EntityPropType.I32),
        ),
    ),
    CharaCollision(
        uniqueName = "Chara Collision",
        areaIds = mapOf(
            Episode.I to listOf(0, 1, 2, 3, 4, 5, 8, 9, 10),
            Episode.II to listOf(0),
            Episode.IV to listOf(0),
        ),
        typeId = 9,
    ),
    ElementalTrap(
        uniqueName = "Elemental Trap",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 16, 17),
            Episode.II to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9),
        ),
        typeId = 10,
        properties = listOf(
            EntityProp(name = "Radius", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Trap link", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "Damage", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Subtype", offset = 56, type = EntityPropType.I32),
            EntityProp(name = "Delay", offset = 60, type = EntityPropType.I32),
        ),
    ),
    StatusTrap(
        uniqueName = "Status Trap",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 16, 17),
            Episode.II to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9),
        ),
        typeId = 11,
        properties = listOf(
            EntityProp(name = "Radius", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Trap link", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "Subtype", offset = 56, type = EntityPropType.I32),
            EntityProp(name = "Delay", offset = 60, type = EntityPropType.I32),
        ),
    ),
    HealTrap(
        uniqueName = "Heal Trap",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 16, 17),
            Episode.II to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9),
        ),
        typeId = 12,
        properties = listOf(
            EntityProp(name = "Radius", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Trap link", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "HP", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Subtype", offset = 56, type = EntityPropType.I32),
            EntityProp(name = "Delay", offset = 60, type = EntityPropType.I32),
        ),
    ),
    LargeElementalTrap(
        uniqueName = "Large Elemental Trap",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 16, 17),
            Episode.II to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9),
        ),
        typeId = 13,
        properties = listOf(
            EntityProp(name = "Radus", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Trap link", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "Damage", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Subtype", offset = 56, type = EntityPropType.I32),
            EntityProp(name = "Delay", offset = 60, type = EntityPropType.I32),
        ),
    ),
    ObjRoomID(
        uniqueName = "Obj Room ID",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13),
            Episode.II to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9),
        ),
        typeId = 14,
        properties = listOf(
            EntityProp(name = "SCL_TAMA", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Next section", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Previous section ", offset = 48, type = EntityPropType.F32),
        ),
    ),
    Sensor(
        uniqueName = "Sensor",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 4, 5, 6, 7),
        ),
        typeId = 15,
    ),
    UnknownItem16(
        uniqueName = "Unknown Item (16)",
        areaIds = mapOf(),
        typeId = 16,
    ),
    LensFlare(
        uniqueName = "Lens Flare",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 3, 4, 8, 14),
        ),
        typeId = 17,
    ),
    ScriptCollision(
        uniqueName = "Script Collision",
        areaIds = mapOf(
            Episode.I to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14),
            Episode.II to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8, 0),
        ),
        typeId = 18,
        properties = listOf(
            EntityProp(name = "Radius", offset = 40, type = EntityPropType.F32),
        ),
    ),
    HealRing(
        uniqueName = "Heal Ring",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
            Episode.II to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
        typeId = 19,
    ),
    MapCollision(
        uniqueName = "Map Collision",
        areaIds = mapOf(
            Episode.I to listOf(0, 1, 2, 3, 4, 5, 8, 9, 10, 16, 17),
            Episode.II to listOf(0, 5, 6, 7, 8, 9, 10, 11, 16, 17),
            Episode.IV to listOf(0),
        ),
        typeId = 20,
    ),
    ScriptCollisionA(
        uniqueName = "Script Collision A",
        areaIds = mapOf(
            Episode.I to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14),
            Episode.II to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8, 0),
        ),
        typeId = 21,
    ),
    ItemLight(
        uniqueName = "Item Light",
        areaIds = mapOf(
            Episode.I to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15),
            Episode.II to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8, 0),
        ),
        typeId = 22,
        properties = listOf(
            EntityProp(name = "Subtype", offset = 40, type = EntityPropType.F32),
        ),
    ),
    RadarCollision(
        uniqueName = "Radar Collision",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15),
            Episode.II to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
        typeId = 23,
        properties = listOf(
            EntityProp(name = "Radius", offset = 40, type = EntityPropType.F32),
        ),
    ),
    FogCollisionSW(
        uniqueName = "Fog Collision SW",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14),
            Episode.II to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
        typeId = 24,
        properties = listOf(
            EntityProp(name = "Radius", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Status", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Fog index no.", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Switch ID", offset = 60, type = EntityPropType.I32),
        ),
    ),
    BossTeleporter(
        uniqueName = "Boss Teleporter",
        areaIds = mapOf(
            Episode.I to listOf(0, 2, 5, 7, 10),
            Episode.II to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 16, 17),
            Episode.IV to listOf(5, 6, 7, 8, 0),
        ),
        typeId = 25,
    ),
    ImageBoard(
        uniqueName = "Image Board",
        areaIds = mapOf(
            Episode.I to listOf(0),
            Episode.II to listOf(0),
            Episode.IV to listOf(0),
        ),
        typeId = 26,
        properties = listOf(
            EntityProp(name = "Scale x", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Scale y", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Scale z", offset = 48, type = EntityPropType.F32),
        ),
    ),
    QuestWarp(
        uniqueName = "Quest Warp",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 3, 4, 5, 6, 7, 11, 12, 13, 14),
            Episode.IV to listOf(9),
        ),
        typeId = 27,
    ),
    Epilogue(
        uniqueName = "Epilogue",
        areaIds = mapOf(
            Episode.I to listOf(14),
            Episode.II to listOf(13),
            Episode.IV to listOf(9),
        ),
        typeId = 28,
    ),
    UnknownItem29(
        uniqueName = "Unknown Item (29)",
        areaIds = mapOf(
            Episode.I to listOf(1),
        ),
        typeId = 29,
    ),
    UnknownItem30(
        uniqueName = "Unknown Item (30)",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 17),
            Episode.II to listOf(1, 2, 14),
            Episode.IV to listOf(1, 2, 3, 4, 5),
        ),
        typeId = 30,
    ),
    UnknownItem31(
        uniqueName = "Unknown Item (31)",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 16, 17),
            Episode.II to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
        typeId = 31,
    ),
    BoxDetectObject(
        uniqueName = "Box Detect Object",
        areaIds = mapOf(
            Episode.I to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 16, 17),
            Episode.II to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8, 0),
        ),
        typeId = 32,
        properties = listOf(
            EntityProp(name = "Radius", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Plate ID", offset = 52, type = EntityPropType.I32),
        ),
    ),
    SymbolChatObject(
        uniqueName = "Symbol Chat Object",
        areaIds = mapOf(
            Episode.I to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 16, 17),
            Episode.II to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8, 0),
        ),
        typeId = 33,
        properties = listOf(
            EntityProp(name = "Radius", offset = 40, type = EntityPropType.F32),
        ),
    ),
    TouchPlateObject(
        uniqueName = "Touch plate Object",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 16, 17),
            Episode.II to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
        typeId = 34,
        properties = listOf(
            EntityProp(name = "Radius", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Switch ID", offset = 52, type = EntityPropType.I32),
        ),
    ),
    TargetableObject(
        uniqueName = "Targetable Object",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 16, 17),
            Episode.II to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
        typeId = 35,
        properties = listOf(
            EntityProp(name = "Switch ID", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "HP", offset = 52, type = EntityPropType.I32),
        ),
    ),
    EffectObject(
        uniqueName = "Effect object",
        areaIds = mapOf(
            Episode.I to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 16, 17),
            Episode.II to listOf(0, 1, 2, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17),
            Episode.IV to listOf(0),
        ),
        typeId = 36,
    ),
    CountDownObject(
        uniqueName = "Count Down Object",
        areaIds = mapOf(
            Episode.I to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 16, 17),
            Episode.II to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8, 0),
        ),
        typeId = 37,
    ),
    UnknownItem38(
        uniqueName = "Unknown Item (38)",
        areaIds = mapOf(
            Episode.I to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 16, 17),
            Episode.II to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8, 0),
        ),
        typeId = 38,
    ),
    UnknownItem39(
        uniqueName = "Unknown Item (39)",
        areaIds = mapOf(
            Episode.II to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
        typeId = 39,
    ),
    UnknownItem40(
        uniqueName = "Unknown Item (40)",
        areaIds = mapOf(
            Episode.I to listOf(0, 1, 2, 4, 5, 6, 7, 8, 9, 10, 13, 16, 17),
            Episode.II to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8, 0),
        ),
        typeId = 40,
    ),
    UnknownItem41(
        uniqueName = "Unknown Item (41)",
        areaIds = mapOf(
            Episode.I to listOf(0, 1, 2, 4, 5, 6, 7, 8, 9, 10, 13, 16, 17),
            Episode.II to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8, 0),
        ),
        typeId = 41,
    ),
    MenuActivation(
        uniqueName = "Menu activation",
        areaIds = mapOf(
            Episode.I to listOf(0),
            Episode.II to listOf(0),
            Episode.IV to listOf(0),
        ),
        typeId = 64,
        properties = listOf(
            EntityProp(name = "Menu ID", offset = 52, type = EntityPropType.I32),
        ),
    ),
    TelepipeLocation(
        uniqueName = "Telepipe Location",
        areaIds = mapOf(
            Episode.I to listOf(0),
            Episode.II to listOf(0),
            Episode.IV to listOf(0),
        ),
        typeId = 65,
        properties = listOf(
            EntityProp(name = "Slot ID", offset = 52, type = EntityPropType.I32),
        ),
    ),
    BGMCollision(
        uniqueName = "BGM Collision",
        areaIds = mapOf(
            Episode.I to listOf(0),
            Episode.II to listOf(0),
            Episode.IV to listOf(0),
        ),
        typeId = 66,
    ),
    MainRagolTeleporter(
        uniqueName = "Main Ragol Teleporter",
        areaIds = mapOf(
            Episode.I to listOf(0),
            Episode.II to listOf(0),
            Episode.IV to listOf(0),
        ),
        typeId = 67,
    ),
    LobbyTeleporter(
        uniqueName = "Lobby Teleporter",
        areaIds = mapOf(
            Episode.I to listOf(0),
            Episode.II to listOf(0),
            Episode.IV to listOf(0),
        ),
        typeId = 68,
    ),
    PrincipalWarp(
        uniqueName = "Principal warp",
        areaIds = mapOf(
            Episode.I to listOf(0),
            Episode.II to listOf(0),
            Episode.IV to listOf(0),
        ),
        typeId = 69,
        properties = listOf(
            EntityProp(name = "Destination x", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Destination y", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Destination z", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "Dst. rotation y", offset = 52, type = EntityPropType.Angle),
            EntityProp(name = "Model", offset = 60, type = EntityPropType.I32),
        ),
    ),
    ShopDoor(
        uniqueName = "Shop Door",
        areaIds = mapOf(
            Episode.I to listOf(0),
            Episode.IV to listOf(0),
        ),
        typeId = 70,
    ),
    HuntersGuildDoor(
        uniqueName = "Hunter's Guild Door",
        areaIds = mapOf(
            Episode.I to listOf(0),
            Episode.IV to listOf(0),
        ),
        typeId = 71,
    ),
    TeleporterDoor(
        uniqueName = "Teleporter Door",
        areaIds = mapOf(
            Episode.I to listOf(0),
            Episode.IV to listOf(0),
        ),
        typeId = 72,
    ),
    MedicalCenterDoor(
        uniqueName = "Medical Center Door",
        areaIds = mapOf(
            Episode.I to listOf(0),
            Episode.IV to listOf(0),
        ),
        typeId = 73,
    ),
    Elevator(
        uniqueName = "Elevator",
        areaIds = mapOf(
            Episode.I to listOf(0),
            Episode.IV to listOf(0),
        ),
        typeId = 74,
    ),
    EasterEgg(
        uniqueName = "Easter Egg",
        areaIds = mapOf(
            Episode.I to listOf(0),
            Episode.II to listOf(0),
            Episode.IV to listOf(0),
        ),
        typeId = 75,
    ),
    ValentinesHeart(
        uniqueName = "Valentines Heart",
        areaIds = mapOf(
            Episode.I to listOf(0),
            Episode.II to listOf(0),
            Episode.IV to listOf(0),
        ),
        typeId = 76,
    ),
    ChristmasTree(
        uniqueName = "Christmas Tree",
        areaIds = mapOf(
            Episode.I to listOf(0),
            Episode.II to listOf(0),
            Episode.IV to listOf(0),
        ),
        typeId = 77,
    ),
    ChristmasWreath(
        uniqueName = "Christmas Wreath",
        areaIds = mapOf(
            Episode.I to listOf(0),
            Episode.II to listOf(0),
            Episode.IV to listOf(0),
        ),
        typeId = 78,
    ),
    HalloweenPumpkin(
        uniqueName = "Halloween Pumpkin",
        areaIds = mapOf(
            Episode.I to listOf(0),
            Episode.II to listOf(0),
            Episode.IV to listOf(0),
        ),
        typeId = 79,
    ),
    TwentyFirstCentury(
        uniqueName = "21st Century",
        areaIds = mapOf(
            Episode.I to listOf(0),
            Episode.II to listOf(0),
            Episode.IV to listOf(0),
        ),
        typeId = 80,
    ),
    Sonic(
        uniqueName = "Sonic",
        areaIds = mapOf(
            Episode.I to listOf(0),
            Episode.II to listOf(0),
            Episode.IV to listOf(0),
        ),
        typeId = 81,
        properties = listOf(
            EntityProp(name = "Model", offset = 52, type = EntityPropType.I32),
        ),
    ),
    WelcomeBoard(
        uniqueName = "Welcome Board",
        areaIds = mapOf(
            Episode.I to listOf(0),
            Episode.II to listOf(0),
            Episode.IV to listOf(0),
        ),
        typeId = 82,
    ),
    Firework(
        uniqueName = "Firework",
        areaIds = mapOf(
            Episode.I to listOf(0),
            Episode.II to listOf(0, 16),
            Episode.IV to listOf(0),
        ),
        typeId = 83,
        properties = listOf(
            EntityProp(name = "Mdl IDX", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Area width", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Rise height", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "Area depth", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Freq", offset = 56, type = EntityPropType.I32),
        ),
    ),
    LobbyScreenDoor(
        uniqueName = "Lobby Screen Door",
        areaIds = mapOf(
            Episode.I to listOf(0),
            Episode.IV to listOf(0),
        ),
        typeId = 84,
    ),
    MainRagolTeleporterBattleInNextArea(
        uniqueName = "Main Ragol Teleporter (Battle in next area?)",
        areaIds = mapOf(
            Episode.I to listOf(0),
            Episode.II to listOf(0),
            Episode.IV to listOf(0),
        ),
        typeId = 85,
    ),
    LabTeleporterDoor(
        uniqueName = "Lab Teleporter Door",
        areaIds = mapOf(
            Episode.II to listOf(0),
        ),
        typeId = 86,
    ),
    Pioneer2InvisibleTouchplate(
        uniqueName = "Pioneer 2 Invisible Touchplate",
        areaIds = mapOf(
            Episode.I to listOf(0),
            Episode.II to listOf(0),
            Episode.IV to listOf(0),
        ),
        typeId = 87,
        properties = listOf(
            EntityProp(name = "Radius", offset = 40, type = EntityPropType.F32),
        ),
    ),
    ForestDoor(
        uniqueName = "Forest Door",
        areaIds = mapOf(
            Episode.I to listOf(1, 2),
        ),
        typeId = 128,
        properties = listOf(
            EntityProp(name = "Door ID", offset = 52, type = EntityPropType.I32),
        ),
    ),
    ForestSwitch(
        uniqueName = "Forest Switch",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 3, 4, 5),
            Episode.II to listOf(1, 2, 3, 4),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
        typeId = 129,
        properties = listOf(
            EntityProp(name = "Switch ID", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Color", offset = 60, type = EntityPropType.I32),
        ),
    ),
    LaserFence(
        uniqueName = "Laser Fence",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 3, 4, 5, 6, 7, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
        typeId = 130,
        properties = listOf(
            EntityProp(name = "Color", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Switch ID", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Model", offset = 60, type = EntityPropType.I32),
        ),
    ),
    LaserSquareFence(
        uniqueName = "Laser Square Fence",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 3, 4, 5, 6, 7, 16, 17),
            Episode.II to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
        typeId = 131,
        properties = listOf(
            EntityProp(name = "Color", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Switch ID", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Model", offset = 60, type = EntityPropType.I32),
        ),
    ),
    ForestLaserFenceSwitch(
        uniqueName = "Forest Laser Fence Switch",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 3, 4, 5, 6, 7, 16, 17),
            Episode.II to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
        typeId = 132,
        properties = listOf(
            EntityProp(name = "Switch ID", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Color", offset = 60, type = EntityPropType.I32),
        ),
    ),
    LightRays(
        uniqueName = "Light rays",
        areaIds = mapOf(
            Episode.I to listOf(1, 2),
            Episode.II to listOf(5, 6, 7, 8, 9),
            Episode.IV to listOf(6, 7, 8),
        ),
        typeId = 133,
        properties = listOf(
            EntityProp(name = "Scale x", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Scale y", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Scale z", offset = 48, type = EntityPropType.F32),
        ),
    ),
    BlueButterfly(
        uniqueName = "Blue Butterfly",
        areaIds = mapOf(
            Episode.I to listOf(1, 2),
            Episode.IV to listOf(6, 7, 8),
        ),
        typeId = 134,
    ),
    Probe(
        uniqueName = "Probe",
        areaIds = mapOf(
            Episode.I to listOf(1, 2),
        ),
        typeId = 135,
        properties = listOf(
            EntityProp(name = "Model", offset = 40, type = EntityPropType.F32),
        ),
    ),
    RandomTypeBox1(
        uniqueName = "Random Type Box 1",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 3, 4, 5, 6, 7),
            Episode.II to listOf(10, 11, 13),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
        typeId = 136,
    ),
    ForestWeatherStation(
        uniqueName = "Forest Weather Station",
        areaIds = mapOf(
            Episode.I to listOf(1, 2),
        ),
        typeId = 137,
    ),
    Battery(
        uniqueName = "Battery",
        areaIds = mapOf(),
        typeId = 138,
    ),
    ForestConsole(
        uniqueName = "Forest Console",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 16, 17),
            Episode.II to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
        typeId = 139,
        properties = listOf(
            EntityProp(name = "Script label", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Model", offset = 56, type = EntityPropType.I32),
        ),
    ),
    BlackSlidingDoor(
        uniqueName = "Black Sliding Door",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 3),
        ),
        typeId = 140,
        properties = listOf(
            EntityProp(name = "Distance", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Speed", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Switch ID", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "Switch no.", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Disable effect", offset = 56, type = EntityPropType.I32),
            EntityProp(name = "Enable effect", offset = 60, type = EntityPropType.I32),
        ),
    ),
    RicoMessagePod(
        uniqueName = "Rico Message Pod",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 13),
        ),
        typeId = 141,
    ),
    EnergyBarrier(
        uniqueName = "Energy Barrier",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 4, 5, 6, 7),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
        typeId = 142,
        properties = listOf(
            EntityProp(name = "Door ID", offset = 52, type = EntityPropType.I32),
        ),
    ),
    ForestRisingBridge(
        uniqueName = "Forest Rising Bridge",
        areaIds = mapOf(
            Episode.I to listOf(1, 2),
        ),
        typeId = 143,
        properties = listOf(
            EntityProp(name = "Door ID", offset = 52, type = EntityPropType.I32),
        ),
    ),
    SwitchNoneDoor(
        uniqueName = "Switch (none door)",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 6, 7, 16, 17),
            Episode.II to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
        typeId = 144,
        properties = listOf(
            EntityProp(name = "Switch ID", offset = 52, type = EntityPropType.I32),
        ),
    ),
    EnemyBoxGrey(
        uniqueName = "Enemy Box (Grey)",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 3, 4, 5, 6, 7),
            Episode.II to listOf(10, 11),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
        typeId = 145,
        properties = listOf(
            EntityProp(name = "Event ID", offset = 40, type = EntityPropType.F32),
        ),
    ),
    FixedTypeBox(
        uniqueName = "Fixed Type Box",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 3, 4, 5, 6, 7, 11, 12, 13, 14),
            Episode.II to listOf(10, 11, 13),
            Episode.IV to listOf(1, 2, 3, 4, 6, 7, 8, 9),
        ),
        typeId = 146,
        properties = listOf(
            EntityProp(name = "Full random", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Random item", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Fixed item", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "Item parameter", offset = 52, type = EntityPropType.I32),
        ),
    ),
    EnemyBoxBrown(
        uniqueName = "Enemy Box (Brown)",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 3, 4, 5, 6, 7),
            Episode.II to listOf(10, 11),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
        typeId = 147,
        properties = listOf(
            EntityProp(name = "Event ID", offset = 40, type = EntityPropType.F32),
        ),
    ),
    EmptyTypeBox(
        uniqueName = "Empty Type Box",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 3, 4, 5, 6, 7),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
        typeId = 149,
        properties = listOf(
            EntityProp(name = "Event ID", offset = 40, type = EntityPropType.F32),
        ),
    ),
    LaserFenceEx(
        uniqueName = "Laser Fence Ex",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 16, 17),
            Episode.II to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
        typeId = 150,
        properties = listOf(
            EntityProp(name = "Color", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Collision width", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Collision depth", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "Switch ID", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Model", offset = 60, type = EntityPropType.I32),
        ),
    ),
    LaserSquareFenceEx(
        uniqueName = "Laser Square Fence Ex",
        areaIds = mapOf(),
        typeId = 151,
        properties = listOf(
            EntityProp(name = "Color", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Collision width", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Collision depth", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "Switch ID", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Model", offset = 60, type = EntityPropType.I32),
        ),
    ),
    FloorPanel1(
        uniqueName = "Floor Panel 1",
        areaIds = mapOf(
            Episode.I to listOf(3, 4, 5, 16, 17),
            Episode.II to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
        typeId = 192,
        properties = listOf(
            EntityProp(name = "Scale  x", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Scale  y", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Scale  z", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "Switch ID", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Stay active", offset = 56, type = EntityPropType.I32),
        ),
    ),
    Caves4ButtonDoor(
        uniqueName = "Caves 4 Button door",
        areaIds = mapOf(
            Episode.I to listOf(3, 4, 5),
        ),
        typeId = 193,
        properties = listOf(
            EntityProp(name = "Door ID", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Switch total", offset = 56, type = EntityPropType.I32),
            EntityProp(name = "Stay active", offset = 60, type = EntityPropType.I32),
        ),
    ),
    CavesNormalDoor(
        uniqueName = "Caves Normal door",
        areaIds = mapOf(
            Episode.I to listOf(3, 4, 5),
        ),
        typeId = 194,
        properties = listOf(
            EntityProp(name = "Door ID", offset = 52, type = EntityPropType.I32),
        ),
    ),
    CavesSmashingPillar(
        uniqueName = "Caves Smashing Pillar",
        areaIds = mapOf(
            Episode.I to listOf(3, 4, 5),
            Episode.II to listOf(1, 2, 3, 4, 17),
        ),
        typeId = 195,
    ),
    CavesSign1(
        uniqueName = "Caves Sign 1",
        areaIds = mapOf(
            Episode.I to listOf(4, 5),
        ),
        typeId = 196,
    ),
    CavesSign2(
        uniqueName = "Caves Sign 2",
        areaIds = mapOf(
            Episode.I to listOf(4, 5),
        ),
        typeId = 197,
    ),
    CavesSign3(
        uniqueName = "Caves Sign 3",
        areaIds = mapOf(
            Episode.I to listOf(4, 5),
        ),
        typeId = 198,
    ),
    HexagonalTank(
        uniqueName = "Hexagonal Tank",
        areaIds = mapOf(
            Episode.I to listOf(4, 5),
        ),
        typeId = 199,
    ),
    BrownPlatform(
        uniqueName = "Brown Platform",
        areaIds = mapOf(
            Episode.I to listOf(4, 5),
        ),
        typeId = 200,
    ),
    WarningLightObject(
        uniqueName = "Warning Light Object",
        areaIds = mapOf(
            Episode.I to listOf(4, 5),
            Episode.IV to listOf(5),
        ),
        typeId = 201,
    ),
    Rainbow(
        uniqueName = "Rainbow",
        areaIds = mapOf(
            Episode.I to listOf(4),
        ),
        typeId = 203,
        properties = listOf(
            EntityProp(name = "Scale  x", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Scale  y", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Scale  z", offset = 48, type = EntityPropType.F32),
        ),
    ),
    FloatingJellyfish(
        uniqueName = "Floating Jellyfish",
        areaIds = mapOf(
            Episode.I to listOf(4),
            Episode.II to listOf(10, 11),
        ),
        typeId = 204,
    ),
    FloatingDragonfly(
        uniqueName = "Floating Dragonfly",
        areaIds = mapOf(
            Episode.I to listOf(4, 16),
            Episode.II to listOf(3, 4),
            Episode.IV to listOf(6, 7, 8),
        ),
        typeId = 205,
    ),
    CavesSwitchDoor(
        uniqueName = "Caves Switch Door",
        areaIds = mapOf(
            Episode.I to listOf(3, 4, 5),
        ),
        typeId = 206,
        properties = listOf(
            EntityProp(name = "Door ID", offset = 52, type = EntityPropType.I32),
        ),
    ),
    RobotRechargeStation(
        uniqueName = "Robot Recharge Station",
        areaIds = mapOf(
            Episode.I to listOf(3, 4, 5, 6, 7),
            Episode.II to listOf(17),
        ),
        typeId = 207,
    ),
    CavesCakeShop(
        uniqueName = "Caves Cake Shop",
        areaIds = mapOf(
            Episode.I to listOf(5),
        ),
        typeId = 208,
    ),
    Caves1SmallRedRock(
        uniqueName = "Caves 1 Small Red Rock",
        areaIds = mapOf(
            Episode.I to listOf(3),
        ),
        typeId = 209,
    ),
    Caves1MediumRedRock(
        uniqueName = "Caves 1 Medium Red Rock",
        areaIds = mapOf(
            Episode.I to listOf(3),
        ),
        typeId = 210,
    ),
    Caves1LargeRedRock(
        uniqueName = "Caves 1 Large Red Rock",
        areaIds = mapOf(
            Episode.I to listOf(3),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
        typeId = 211,
    ),
    Caves2SmallRock1(
        uniqueName = "Caves 2 Small Rock 1",
        areaIds = mapOf(
            Episode.I to listOf(4),
        ),
        typeId = 212,
    ),
    Caves2MediumRock1(
        uniqueName = "Caves 2 Medium Rock 1",
        areaIds = mapOf(
            Episode.I to listOf(4),
        ),
        typeId = 213,
    ),
    Caves2LargeRock1(
        uniqueName = "Caves 2 Large Rock 1",
        areaIds = mapOf(
            Episode.I to listOf(4),
        ),
        typeId = 214,
    ),
    Caves2SmallRock2(
        uniqueName = "Caves 2 Small Rock 2",
        areaIds = mapOf(
            Episode.I to listOf(4),
        ),
        typeId = 215,
    ),
    Caves2MediumRock2(
        uniqueName = "Caves 2 Medium Rock 2",
        areaIds = mapOf(
            Episode.I to listOf(4),
        ),
        typeId = 216,
    ),
    Caves2LargeRock2(
        uniqueName = "Caves 2 Large Rock 2",
        areaIds = mapOf(
            Episode.I to listOf(4),
        ),
        typeId = 217,
    ),
    Caves3SmallRock(
        uniqueName = "Caves 3 Small Rock",
        areaIds = mapOf(
            Episode.I to listOf(5),
        ),
        typeId = 218,
    ),
    Caves3MediumRock(
        uniqueName = "Caves 3 Medium Rock",
        areaIds = mapOf(
            Episode.I to listOf(5),
        ),
        typeId = 219,
    ),
    Caves3LargeRock(
        uniqueName = "Caves 3 Large Rock",
        areaIds = mapOf(
            Episode.I to listOf(5),
        ),
        typeId = 220,
    ),
    FloorPanel2(
        uniqueName = "Floor Panel 2",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 16, 17),
            Episode.II to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
        typeId = 222,
        properties = listOf(
            EntityProp(name = "Scale x", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Scale y", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Scale z", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "Switch ID", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Stay active", offset = 56, type = EntityPropType.I32),
        ),
    ),
    DestructableRockCaves1(
        uniqueName = "Destructable Rock (Caves 1)",
        areaIds = mapOf(
            Episode.I to listOf(3),
        ),
        typeId = 223,
    ),
    DestructableRockCaves2(
        uniqueName = "Destructable Rock (Caves 2)",
        areaIds = mapOf(
            Episode.I to listOf(4),
        ),
        typeId = 224,
    ),
    DestructableRockCaves3(
        uniqueName = "Destructable Rock (Caves 3)",
        areaIds = mapOf(
            Episode.I to listOf(5),
        ),
        typeId = 225,
    ),
    MinesDoor(
        uniqueName = "Mines Door",
        areaIds = mapOf(
            Episode.I to listOf(6, 7),
        ),
        typeId = 256,
        properties = listOf(
            EntityProp(name = "Door ID", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Switch total", offset = 56, type = EntityPropType.I32),
            EntityProp(name = "Stay active", offset = 60, type = EntityPropType.I32),
        ),
    ),
    FloorPanel3(
        uniqueName = "Floor Panel 3",
        areaIds = mapOf(
            Episode.I to listOf(1, 2, 6, 7, 16, 17),
            Episode.II to listOf(1, 2, 3, 4),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
        typeId = 257,
        properties = listOf(
            EntityProp(name = "Scale x", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Scale y", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Scale z", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "Switch ID", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Stay active", offset = 56, type = EntityPropType.I32),
        ),
    ),
    MinesSwitchDoor(
        uniqueName = "Mines Switch Door",
        areaIds = mapOf(
            Episode.I to listOf(6, 7),
            Episode.IV to listOf(6, 7, 8),
        ),
        typeId = 258,
        properties = listOf(
            EntityProp(name = "Door ID", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Switch total", offset = 56, type = EntityPropType.I32),
            EntityProp(name = "Stay active", offset = 60, type = EntityPropType.I32),
        ),
    ),
    LargeCryoTube(
        uniqueName = "Large Cryo-Tube",
        areaIds = mapOf(
            Episode.I to listOf(6, 7),
            Episode.II to listOf(17),
        ),
        typeId = 259,
    ),
    ComputerLikeCalus(
        uniqueName = "Computer (like calus)",
        areaIds = mapOf(
            Episode.I to listOf(6, 7),
            Episode.II to listOf(17),
        ),
        typeId = 260,
    ),
    GreenScreenOpeningAndClosing(
        uniqueName = "Green Screen opening and closing",
        areaIds = mapOf(
            Episode.I to listOf(6, 7),
            Episode.II to listOf(17),
        ),
        typeId = 261,
    ),
    FloatingRobot(
        uniqueName = "Floating Robot",
        areaIds = mapOf(
            Episode.I to listOf(6, 7),
        ),
        typeId = 262,
    ),
    FloatingBlueLight(
        uniqueName = "Floating Blue Light",
        areaIds = mapOf(
            Episode.I to listOf(6, 7),
        ),
        typeId = 263,
    ),
    SelfDestructingObject1(
        uniqueName = "Self Destructing Object 1",
        areaIds = mapOf(
            Episode.I to listOf(6, 7),
        ),
        typeId = 264,
    ),
    SelfDestructingObject2(
        uniqueName = "Self Destructing Object 2",
        areaIds = mapOf(
            Episode.I to listOf(6, 7),
        ),
        typeId = 265,
    ),
    SelfDestructingObject3(
        uniqueName = "Self Destructing Object 3",
        areaIds = mapOf(
            Episode.I to listOf(6, 7),
        ),
        typeId = 266,
    ),
    SparkMachine(
        uniqueName = "Spark Machine",
        areaIds = mapOf(
            Episode.I to listOf(6, 7),
        ),
        typeId = 267,
        properties = listOf(
            EntityProp(name = "Scale x", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Scale y", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Scale z", offset = 48, type = EntityPropType.F32),
        ),
    ),
    MinesLargeFlashingCrate(
        uniqueName = "Mines Large Flashing Crate",
        areaIds = mapOf(
            Episode.I to listOf(6, 7),
        ),
        typeId = 268,
    ),
    RuinsSeal(
        uniqueName = "Ruins Seal",
        areaIds = mapOf(
            Episode.I to listOf(13),
        ),
        typeId = 304,
    ),
    RuinsTeleporter(
        uniqueName = "Ruins Teleporter",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10),
        ),
        typeId = 320,
        properties = listOf(
            EntityProp(name = "Area no.", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Color blue", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Color red", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "Floor no.", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Display no.", offset = 56, type = EntityPropType.I32),
            EntityProp(name = "No display no.", offset = 60, type = EntityPropType.I32),
        ),
    ),
    RuinsWarpSiteToSite(
        uniqueName = "Ruins Warp (Site to Site)",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10),
        ),
        typeId = 321,
        properties = listOf(
            EntityProp(name = "Destination x", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Destination y", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Destination z", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "Dst. rotation y", offset = 52, type = EntityPropType.Angle),
        ),
    ),
    RuinsSwitch(
        uniqueName = "Ruins Switch",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10),
        ),
        typeId = 322,
        properties = listOf(
            EntityProp(name = "Switch ID", offset = 52, type = EntityPropType.I32),
        ),
    ),
    FloorPanel4(
        uniqueName = "Floor Panel 4",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10),
        ),
        typeId = 323,
        properties = listOf(
            EntityProp(name = "Scale x", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Scale y", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Scale z", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "Plate ID", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Stay active", offset = 56, type = EntityPropType.I32),
        ),
    ),
    Ruins1Door(
        uniqueName = "Ruins 1 Door",
        areaIds = mapOf(
            Episode.I to listOf(8),
        ),
        typeId = 324,
        properties = listOf(
            EntityProp(name = "Door ID", offset = 52, type = EntityPropType.I32),
        ),
    ),
    Ruins3Door(
        uniqueName = "Ruins 3 Door",
        areaIds = mapOf(
            Episode.I to listOf(10),
        ),
        typeId = 325,
        properties = listOf(
            EntityProp(name = "Door ID", offset = 52, type = EntityPropType.I32),
        ),
    ),
    Ruins2Door(
        uniqueName = "Ruins 2 Door",
        areaIds = mapOf(
            Episode.I to listOf(9),
        ),
        typeId = 326,
        properties = listOf(
            EntityProp(name = "Door ID", offset = 52, type = EntityPropType.I32),
        ),
    ),
    Ruins11ButtonDoor(
        uniqueName = "Ruins 1-1 Button Door",
        areaIds = mapOf(
            Episode.I to listOf(8),
        ),
        typeId = 327,
        properties = listOf(
            EntityProp(name = "Door ID", offset = 52, type = EntityPropType.I32),
        ),
    ),
    Ruins21ButtonDoor(
        uniqueName = "Ruins 2-1 Button Door",
        areaIds = mapOf(
            Episode.I to listOf(9),
        ),
        typeId = 328,
        properties = listOf(
            EntityProp(name = "Door ID", offset = 52, type = EntityPropType.I32),
        ),
    ),
    Ruins31ButtonDoor(
        uniqueName = "Ruins 3-1 Button Door",
        areaIds = mapOf(
            Episode.I to listOf(10),
        ),
        typeId = 329,
        properties = listOf(
            EntityProp(name = "Door ID", offset = 52, type = EntityPropType.I32),
        ),
    ),
    Ruins4ButtonDoor(
        uniqueName = "Ruins 4-Button Door",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10),
        ),
        typeId = 330,
        properties = listOf(
            EntityProp(name = "Door ID", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Stay active", offset = 60, type = EntityPropType.I32),
        ),
    ),
    Ruins2ButtonDoor(
        uniqueName = "Ruins 2-Button Door",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10),
        ),
        typeId = 331,
        properties = listOf(
            EntityProp(name = "Door ID", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Stay active", offset = 60, type = EntityPropType.I32),
        ),
    ),
    RuinsSensor(
        uniqueName = "Ruins Sensor",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10),
        ),
        typeId = 332,
    ),
    RuinsFenceSwitch(
        uniqueName = "Ruins Fence Switch",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10),
        ),
        typeId = 333,
        properties = listOf(
            EntityProp(name = "Switch ID", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Color", offset = 56, type = EntityPropType.I32),
        ),
    ),
    RuinsLaserFence4x2(
        uniqueName = "Ruins Laser Fence 4x2",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
        typeId = 334,
        properties = listOf(
            EntityProp(name = "Switch ID", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Color", offset = 56, type = EntityPropType.I32),
        ),
    ),
    RuinsLaserFence6x2(
        uniqueName = "Ruins Laser Fence 6x2",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
        typeId = 335,
        properties = listOf(
            EntityProp(name = "Switch ID", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Color", offset = 56, type = EntityPropType.I32),
        ),
    ),
    RuinsLaserFence4x4(
        uniqueName = "Ruins Laser Fence 4x4",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10),
        ),
        typeId = 336,
        properties = listOf(
            EntityProp(name = "Switch ID", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Color", offset = 56, type = EntityPropType.I32),
        ),
    ),
    RuinsLaserFence6x4(
        uniqueName = "Ruins Laser Fence 6x4",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10),
        ),
        typeId = 337,
        properties = listOf(
            EntityProp(name = "Switch ID", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Color", offset = 56, type = EntityPropType.I32),
        ),
    ),
    RuinsPoisonBlob(
        uniqueName = "Ruins poison Blob",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10),
            Episode.II to listOf(5, 6, 7, 8, 9),
            Episode.IV to listOf(6, 7, 8),
        ),
        typeId = 338,
    ),
    RuinsPillarTrap(
        uniqueName = "Ruins Pillar Trap",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10),
            Episode.II to listOf(1, 2, 3, 4),
        ),
        typeId = 339,
    ),
    PopupTrapNoTech(
        uniqueName = "Popup Trap (No Tech)",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10),
        ),
        typeId = 340,
        properties = listOf(
            EntityProp(name = "Radius", offset = 40, type = EntityPropType.F32),
        ),
    ),
    RuinsCrystal(
        uniqueName = "Ruins Crystal",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10),
        ),
        typeId = 341,
    ),
    Monument(
        uniqueName = "Monument",
        areaIds = mapOf(
            Episode.I to listOf(2, 4, 7),
        ),
        typeId = 342,
    ),
    RuinsRock1(
        uniqueName = "Ruins Rock 1",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10),
        ),
        typeId = 345,
    ),
    RuinsRock2(
        uniqueName = "Ruins Rock 2",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10),
        ),
        typeId = 346,
    ),
    RuinsRock3(
        uniqueName = "Ruins Rock 3",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10),
        ),
        typeId = 347,
    ),
    RuinsRock4(
        uniqueName = "Ruins Rock 4",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10),
        ),
        typeId = 348,
    ),
    RuinsRock5(
        uniqueName = "Ruins Rock 5",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10),
        ),
        typeId = 349,
    ),
    RuinsRock6(
        uniqueName = "Ruins Rock 6",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10),
        ),
        typeId = 350,
    ),
    RuinsRock7(
        uniqueName = "Ruins Rock 7",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10),
        ),
        typeId = 351,
    ),
    Poison(
        uniqueName = "Poison",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10, 13),
            Episode.II to listOf(3, 4, 10, 11),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
        typeId = 352,
        properties = listOf(
            EntityProp(name = "Radius", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Power", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Link", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "Switch mode", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Fog index no.", offset = 56, type = EntityPropType.I32),
            EntityProp(name = "Switch ID", offset = 60, type = EntityPropType.I32),
        ),
    ),
    FixedBoxTypeRuins(
        uniqueName = "Fixed Box Type (Ruins)",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10, 16, 17),
            Episode.II to listOf(1, 2, 3, 4, 14, 15),
        ),
        typeId = 353,
        properties = listOf(
            EntityProp(name = "Full random", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Random item", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Fixed item", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "Item parameter", offset = 52, type = EntityPropType.I32),
        ),
    ),
    RandomBoxTypeRuins(
        uniqueName = "Random Box Type (Ruins)",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10, 16, 17),
            Episode.II to listOf(1, 2, 3, 4, 14, 15),
        ),
        typeId = 354,
    ),
    EnemyTypeBoxYellow(
        uniqueName = "Enemy Type Box (Yellow)",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10, 16, 17),
            Episode.II to listOf(1, 2, 3, 4),
        ),
        typeId = 355,
    ),
    EnemyTypeBoxBlue(
        uniqueName = "Enemy Type Box (Blue)",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10, 16, 17),
            Episode.II to listOf(1, 2, 3, 4),
        ),
        typeId = 356,
    ),
    EmptyTypeBoxBlue(
        uniqueName = "Empty Type Box (Blue)",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10, 16, 17),
            Episode.II to listOf(1, 2, 3, 4),
        ),
        typeId = 357,
    ),
    DestructableRock(
        uniqueName = "Destructable Rock",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10),
        ),
        typeId = 358,
    ),
    PopupTrapsTechs(
        uniqueName = "Popup Traps (techs)",
        areaIds = mapOf(
            Episode.I to listOf(6, 7, 8, 9, 10),
            Episode.II to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 16, 17),
        ),
        typeId = 359,
        properties = listOf(
            EntityProp(name = "Radius", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "HP", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Action", offset = 56, type = EntityPropType.I32),
            EntityProp(name = "Tech", offset = 60, type = EntityPropType.I32),
        ),
    ),
    FlyingWhiteBird(
        uniqueName = "Flying White Bird",
        areaIds = mapOf(
            Episode.I to listOf(14, 16),
            Episode.II to listOf(3, 4),
        ),
        typeId = 368,
    ),
    Tower(
        uniqueName = "Tower",
        areaIds = mapOf(
            Episode.I to listOf(14),
        ),
        typeId = 369,
    ),
    FloatingRocks(
        uniqueName = "Floating Rocks",
        areaIds = mapOf(
            Episode.I to listOf(14),
        ),
        typeId = 370,
    ),
    FloatingSoul(
        uniqueName = "Floating Soul",
        areaIds = mapOf(
            Episode.I to listOf(14),
        ),
        typeId = 371,
    ),
    Butterfly(
        uniqueName = "Butterfly",
        areaIds = mapOf(
            Episode.I to listOf(14),
        ),
        typeId = 372,
    ),
    LobbyGameMenu(
        uniqueName = "Lobby Game menu",
        areaIds = mapOf(
            Episode.I to listOf(15),
        ),
        typeId = 384,
    ),
    LobbyWarpObject(
        uniqueName = "Lobby Warp Object",
        areaIds = mapOf(
            Episode.I to listOf(15),
        ),
        typeId = 385,
    ),
    Lobby1EventObjectDefaultTree(
        uniqueName = "Lobby 1 Event Object (Default Tree)",
        areaIds = mapOf(
            Episode.I to listOf(15),
        ),
        typeId = 386,
    ),
    UnknownItem387(
        uniqueName = "Unknown Item (387)",
        areaIds = mapOf(
            Episode.I to listOf(15),
        ),
        typeId = 387,
    ),
    UnknownItem388(
        uniqueName = "Unknown Item (388)",
        areaIds = mapOf(
            Episode.I to listOf(15),
        ),
        typeId = 388,
    ),
    UnknownItem389(
        uniqueName = "Unknown Item (389)",
        areaIds = mapOf(
            Episode.I to listOf(15),
        ),
        typeId = 389,
    ),
    LobbyEventObjectStaticPumpkin(
        uniqueName = "Lobby Event Object (Static Pumpkin)",
        areaIds = mapOf(
            Episode.I to listOf(15),
        ),
        typeId = 390,
    ),
    LobbyEventObject3ChristmasWindows(
        uniqueName = "Lobby Event Object (3 Christmas Windows)",
        areaIds = mapOf(
            Episode.I to listOf(15),
        ),
        typeId = 391,
    ),
    LobbyEventObjectRedAndWhiteCurtain(
        uniqueName = "Lobby Event Object (Red and White Curtain)",
        areaIds = mapOf(
            Episode.I to listOf(15),
        ),
        typeId = 392,
    ),
    UnknownItem393(
        uniqueName = "Unknown Item (393)",
        areaIds = mapOf(
            Episode.I to listOf(15),
        ),
        typeId = 393,
    ),
    UnknownItem394(
        uniqueName = "Unknown Item (394)",
        areaIds = mapOf(
            Episode.I to listOf(15),
        ),
        typeId = 394,
    ),
    LobbyFishTank(
        uniqueName = "Lobby Fish Tank",
        areaIds = mapOf(
            Episode.I to listOf(15),
        ),
        typeId = 395,
    ),
    LobbyEventObjectButterflies(
        uniqueName = "Lobby Event Object (Butterflies)",
        areaIds = mapOf(
            Episode.I to listOf(15),
        ),
        typeId = 396,
    ),
    UnknownItem400(
        uniqueName = "Unknown Item (400)",
        areaIds = mapOf(
            Episode.I to listOf(16),
            Episode.II to listOf(3, 4),
        ),
        typeId = 400,
    ),
    GreyWallLow(
        uniqueName = "grey wall low",
        areaIds = mapOf(
            Episode.I to listOf(16),
            Episode.II to listOf(3, 4, 17),
        ),
        typeId = 401,
    ),
    SpaceshipDoor(
        uniqueName = "Spaceship Door",
        areaIds = mapOf(
            Episode.I to listOf(16),
            Episode.II to listOf(3, 4),
        ),
        typeId = 402,
        properties = listOf(
            EntityProp(name = "Switch ID", offset = 52, type = EntityPropType.I32),
        ),
    ),
    GreyWallHigh(
        uniqueName = "grey wall high",
        areaIds = mapOf(
            Episode.I to listOf(16),
            Episode.II to listOf(3, 4, 17),
        ),
        typeId = 403,
    ),
    TempleNormalDoor(
        uniqueName = "Temple Normal Door",
        areaIds = mapOf(
            Episode.I to listOf(17),
            Episode.II to listOf(1, 2),
        ),
        typeId = 416,
        properties = listOf(
            EntityProp(name = "Switch ID", offset = 52, type = EntityPropType.I32),
        ),
    ),
    BreakableWallWallButUnbreakable(
        uniqueName = "\"breakable wall wall, but unbreakable\"",
        areaIds = mapOf(
            Episode.I to listOf(17),
            Episode.II to listOf(1, 2),
        ),
        typeId = 417,
    ),
    BrokenCylinderAndRubble(
        uniqueName = "Broken cylinder and rubble",
        areaIds = mapOf(
            Episode.I to listOf(17),
            Episode.II to listOf(1, 2),
        ),
        typeId = 418,
    ),
    ThreeBrokenWallPiecesOnFloor(
        uniqueName = "3 broken wall pieces on floor",
        areaIds = mapOf(
            Episode.I to listOf(17),
            Episode.II to listOf(1, 2),
        ),
        typeId = 419,
    ),
    HighBrickCylinder(
        uniqueName = "high brick cylinder",
        areaIds = mapOf(
            Episode.I to listOf(17),
            Episode.II to listOf(1, 2),
        ),
        typeId = 420,
    ),
    LyingCylinder(
        uniqueName = "lying cylinder",
        areaIds = mapOf(
            Episode.I to listOf(17),
            Episode.II to listOf(1, 2),
        ),
        typeId = 421,
    ),
    BrickConeWithFlatTop(
        uniqueName = "brick cone with flat top",
        areaIds = mapOf(
            Episode.I to listOf(17),
            Episode.II to listOf(1, 2),
        ),
        typeId = 422,
    ),
    BreakableTempleWall(
        uniqueName = "breakable temple wall",
        areaIds = mapOf(
            Episode.I to listOf(17),
            Episode.II to listOf(1, 2),
        ),
        typeId = 423,
    ),
    TempleMapDetect(
        uniqueName = "Temple Map Detect",
        areaIds = mapOf(
            Episode.I to listOf(17),
            Episode.II to listOf(1, 2, 14),
            Episode.IV to listOf(1, 2, 3, 4, 5),
        ),
        typeId = 424,
    ),
    SmallBrownBrickRisingBridge(
        uniqueName = "small brown brick rising bridge",
        areaIds = mapOf(
            Episode.I to listOf(17),
            Episode.II to listOf(1, 2),
        ),
        typeId = 425,
    ),
    LongRisingBridgeWithPinkHighEdges(
        uniqueName = "long rising bridge (with pink high edges)",
        areaIds = mapOf(
            Episode.I to listOf(17),
            Episode.II to listOf(1, 2),
        ),
        typeId = 426,
    ),
    FourSwitchTempleDoor(
        uniqueName = "4 Switch Temple Door",
        areaIds = mapOf(
            Episode.II to listOf(1, 2),
        ),
        typeId = 427,
    ),
    FourButtonSpaceshipDoor(
        uniqueName = "4 button Spaceship Door",
        areaIds = mapOf(
            Episode.II to listOf(3, 4),
        ),
        typeId = 448,
    ),
    ItemBoxCca(
        uniqueName = "Item Box CCA",
        areaIds = mapOf(
            Episode.II to listOf(5, 6, 7, 8, 9, 12, 16, 17),
            Episode.IV to listOf(5),
        ),
        typeId = 512,
    ),
    TeleporterEp2(
        uniqueName = "Teleporter (Ep. II)",
        areaIds = mapOf(
            Episode.II to listOf(5, 6, 7, 8, 9, 10, 11, 12, 13, 16, 17),
        ),
        typeId = 513,
    ),
    CcaDoor(
        uniqueName = "CCA Door",
        areaIds = mapOf(
            Episode.II to listOf(5, 6, 7, 8, 9, 16, 17),
        ),
        typeId = 514,
        properties = listOf(
            EntityProp(name = "Scale x", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Scale y", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Scale z", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "Switch ID", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Switch amount", offset = 56, type = EntityPropType.I32),
            EntityProp(name = "Stay active", offset = 60, type = EntityPropType.I32),
        ),
    ),
    SpecialBoxCca(
        uniqueName = "Special Box CCA",
        areaIds = mapOf(
            Episode.II to listOf(5, 6, 7, 8, 9, 12, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5),
        ),
        typeId = 515,
    ),
    BigCcaDoor(
        uniqueName = "Big CCA Door",
        areaIds = mapOf(
            Episode.II to listOf(5),
        ),
        typeId = 516,
    ),
    BigCcaDoorSwitch(
        uniqueName = "Big CCA Door Switch",
        areaIds = mapOf(
            Episode.II to listOf(5, 6, 7, 8, 9, 16, 17),
        ),
        typeId = 517,
    ),
    LittleRock(
        uniqueName = "Little Rock",
        areaIds = mapOf(
            Episode.II to listOf(5, 6, 7, 8, 9, 16),
        ),
        typeId = 518,
    ),
    Little3StoneWall(
        uniqueName = "Little 3 Stone Wall",
        areaIds = mapOf(
            Episode.II to listOf(5, 6, 7, 8, 9, 16),
        ),
        typeId = 519,
        properties = listOf(
            EntityProp(name = "Switch ID", offset = 52, type = EntityPropType.I32),
        ),
    ),
    Medium3StoneWall(
        uniqueName = "Medium 3 Stone Wall",
        areaIds = mapOf(
            Episode.II to listOf(5, 6, 7, 8, 9, 16),
        ),
        typeId = 520,
    ),
    SpiderPlant(
        uniqueName = "Spider Plant",
        areaIds = mapOf(
            Episode.II to listOf(5, 6, 7, 8, 9, 16),
        ),
        typeId = 521,
    ),
    CcaAreaTeleporter(
        uniqueName = "CCA Area Teleporter",
        areaIds = mapOf(
            Episode.II to listOf(5, 6, 7, 8, 9, 16, 17),
        ),
        typeId = 522,
    ),
    UnknownItem523(
        uniqueName = "Unknown Item (523)",
        areaIds = mapOf(
            Episode.II to listOf(5, 12),
        ),
        typeId = 523,
    ),
    WhiteBird(
        uniqueName = "White Bird",
        areaIds = mapOf(
            Episode.II to listOf(6, 7, 9, 16, 17),
            Episode.IV to listOf(6, 7, 8),
        ),
        typeId = 524,
    ),
    OrangeBird(
        uniqueName = "Orange Bird",
        areaIds = mapOf(
            Episode.II to listOf(6, 7, 9, 17),
        ),
        typeId = 525,
    ),
    Saw(
        uniqueName = "Saw",
        areaIds = mapOf(
            Episode.II to listOf(5, 6, 7, 8, 9, 10, 11, 16, 17),
        ),
        typeId = 527,
        properties = listOf(
            EntityProp(name = "Speed", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Model", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "Switch ID", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Arc", offset = 56, type = EntityPropType.I32),
            EntityProp(name = "Switch flag", offset = 60, type = EntityPropType.I32),
        ),
    ),
    LaserDetect(
        uniqueName = "Laser Detect",
        areaIds = mapOf(
            Episode.II to listOf(5, 6, 7, 8, 9, 10, 11, 16, 17),
        ),
        typeId = 528,
        properties = listOf(
            EntityProp(name = "Model", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "Switch ID", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Arc", offset = 56, type = EntityPropType.I32),
        ),
    ),
    UnknownItem529(
        uniqueName = "Unknown Item (529)",
        areaIds = mapOf(
            Episode.II to listOf(5, 6, 7),
            Episode.IV to listOf(6, 7, 8),
        ),
        typeId = 529,
    ),
    UnknownItem530(
        uniqueName = "Unknown Item (530)",
        areaIds = mapOf(
            Episode.II to listOf(5, 6, 7, 8, 9, 17),
        ),
        typeId = 530,
    ),
    Seagull(
        uniqueName = "Seagull",
        areaIds = mapOf(
            Episode.II to listOf(6, 7, 8, 9, 16),
            Episode.IV to listOf(6, 7, 8),
        ),
        typeId = 531,
    ),
    Fish(
        uniqueName = "Fish",
        areaIds = mapOf(
            Episode.I to listOf(15),
            Episode.II to listOf(6, 9, 10, 11, 16),
        ),
        typeId = 544,
    ),
    SeabedDoorWithBlueEdges(
        uniqueName = "Seabed Door (with Blue Edges)",
        areaIds = mapOf(
            Episode.II to listOf(10, 11),
        ),
        typeId = 545,
        properties = listOf(
            EntityProp(name = "Scale x", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Scale y", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Scale z", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "Switch ID", offset = 52, type = EntityPropType.I32),
            EntityProp(name = "Switch amount", offset = 56, type = EntityPropType.I32),
            EntityProp(name = "Stay active", offset = 60, type = EntityPropType.I32),
        ),
    ),
    SeabedDoorAlwaysOpenNonTriggerable(
        uniqueName = "Seabed Door (Always Open, Non-Triggerable)",
        areaIds = mapOf(
            Episode.II to listOf(10, 11),
        ),
        typeId = 546,
    ),
    LittleCryotube(
        uniqueName = "Little Cryotube",
        areaIds = mapOf(
            Episode.II to listOf(10, 11, 17),
        ),
        typeId = 547,
        properties = listOf(
            EntityProp(name = "Model", offset = 52, type = EntityPropType.I32),
        ),
    ),
    WideGlassWallBreakable(
        uniqueName = "Wide Glass Wall (Breakable)",
        areaIds = mapOf(
            Episode.II to listOf(10, 11),
        ),
        typeId = 548,
    ),
    BlueFloatingRobot(
        uniqueName = "Blue Floating Robot",
        areaIds = mapOf(
            Episode.II to listOf(10, 11),
        ),
        typeId = 549,
    ),
    RedFloatingRobot(
        uniqueName = "Red Floating Robot",
        areaIds = mapOf(
            Episode.II to listOf(10, 11),
        ),
        typeId = 550,
    ),
    Dolphin(
        uniqueName = "Dolphin",
        areaIds = mapOf(
            Episode.II to listOf(10, 11),
        ),
        typeId = 551,
    ),
    CaptureTrap(
        uniqueName = "Capture Trap",
        areaIds = mapOf(
            Episode.II to listOf(5, 6, 7, 8, 9, 10, 11, 16, 17),
        ),
        typeId = 552,
    ),
    VRLink(
        uniqueName = "VR Link",
        areaIds = mapOf(
            Episode.II to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17),
        ),
        typeId = 553,
    ),
    UnknownItem576(
        uniqueName = "Unknown Item (576)",
        areaIds = mapOf(
            Episode.II to listOf(12),
        ),
        typeId = 576,
    ),
    WarpInBarbaRayRoom(
        uniqueName = "Warp in Barba Ray Room",
        areaIds = mapOf(
            Episode.II to listOf(14),
        ),
        typeId = 640,
    ),
    UnknownItem672(
        uniqueName = "Unknown Item (672)",
        areaIds = mapOf(
            Episode.II to listOf(15),
        ),
        typeId = 672,
    ),
    GeeNest(
        uniqueName = "Gee Nest",
        areaIds = mapOf(
            Episode.I to listOf(8, 9, 10),
            Episode.II to listOf(5, 6, 7, 8, 9, 16, 17),
            Episode.IV to listOf(6, 7, 8),
        ),
        typeId = 688,
    ),
    LabComputerConsole(
        uniqueName = "Lab Computer Console",
        areaIds = mapOf(
            Episode.II to listOf(0),
        ),
        typeId = 689,
    ),
    LabComputerConsoleGreenScreen(
        uniqueName = "Lab Computer Console (Green Screen)",
        areaIds = mapOf(
            Episode.II to listOf(0),
        ),
        typeId = 690,
    ),
    ChairYellowPillow(
        uniqueName = "Chair, Yellow Pillow",
        areaIds = mapOf(
            Episode.II to listOf(0),
        ),
        typeId = 691,
    ),
    OrangeWallWithHoleInMiddle(
        uniqueName = "Orange Wall with Hole in Middle",
        areaIds = mapOf(
            Episode.II to listOf(0),
        ),
        typeId = 692,
    ),
    GreyWallWithHoleInMiddle(
        uniqueName = "Grey Wall with Hole in Middle",
        areaIds = mapOf(
            Episode.II to listOf(0),
        ),
        typeId = 693,
    ),
    LongTable(
        uniqueName = "Long Table",
        areaIds = mapOf(
            Episode.II to listOf(0),
        ),
        typeId = 694,
    ),
    GBAStation(
        uniqueName = "GBA Station",
        areaIds = mapOf(),
        typeId = 695,
    ),
    TalkLinkToSupport(
        uniqueName = "Talk (Link to Support)",
        areaIds = mapOf(
            Episode.I to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14),
            Episode.II to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8, 0),
        ),
        typeId = 696,
    ),
    InstaWarp(
        uniqueName = "Insta-Warp",
        areaIds = mapOf(
            Episode.I to listOf(0, 1, 2, 3, 4, 5, 6, 7, 11, 12, 13, 14, 16, 17),
            Episode.II to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0),
        ),
        typeId = 697,
    ),
    LabInvisibleObject(
        uniqueName = "Lab Invisible Object",
        areaIds = mapOf(
            Episode.I to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14),
            Episode.II to listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17),
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8, 0),
        ),
        typeId = 698,
    ),
    LabGlassWindowDoor(
        uniqueName = "Lab Glass Window Door",
        areaIds = mapOf(
            Episode.II to listOf(0),
        ),
        typeId = 699,
    ),
    UnknownItem700(
        uniqueName = "Unknown Item (700)",
        areaIds = mapOf(
            Episode.II to listOf(13),
        ),
        typeId = 700,
    ),
    LabCeilingWarp(
        uniqueName = "Lab Ceiling Warp",
        areaIds = mapOf(
            Episode.II to listOf(0),
        ),
        typeId = 701,
    ),
    Ep4LightSource(
        uniqueName = "Ep. IV Light Source",
        areaIds = mapOf(
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8, 9),
        ),
        typeId = 768,
    ),
    Cactus(
        uniqueName = "Cactus",
        areaIds = mapOf(
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
        typeId = 769,
        properties = listOf(
            EntityProp(name = "Scale x", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Scale y", offset = 44, type = EntityPropType.F32),
            EntityProp(name = "Scale z", offset = 48, type = EntityPropType.F32),
            EntityProp(name = "Model", offset = 52, type = EntityPropType.I32),
        ),
    ),
    BigBrownRock(
        uniqueName = "Big Brown Rock",
        areaIds = mapOf(
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
        typeId = 770,
        properties = listOf(
            EntityProp(name = "Model", offset = 52, type = EntityPropType.I32),
        ),
    ),
    BreakableBrownRock(
        uniqueName = "Breakable Brown Rock",
        areaIds = mapOf(
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
        typeId = 771,
    ),
    UnknownItem832(
        uniqueName = "Unknown Item (832)",
        areaIds = mapOf(),
        typeId = 832,
    ),
    UnknownItem833(
        uniqueName = "Unknown Item (833)",
        areaIds = mapOf(),
        typeId = 833,
    ),
    PoisonPlant(
        uniqueName = "Poison Plant",
        areaIds = mapOf(
            Episode.IV to listOf(6, 7, 8),
        ),
        typeId = 896,
    ),
    UnknownItem897(
        uniqueName = "Unknown Item (897)",
        areaIds = mapOf(
            Episode.IV to listOf(6, 7, 8),
        ),
        typeId = 897,
    ),
    UnknownItem898(
        uniqueName = "Unknown Item (898)",
        areaIds = mapOf(
            Episode.IV to listOf(6, 7, 8),
        ),
        typeId = 898,
    ),
    OozingDesertPlant(
        uniqueName = "Oozing Desert Plant",
        areaIds = mapOf(
            Episode.IV to listOf(6, 7, 8),
        ),
        typeId = 899,
    ),
    UnknownItem901(
        uniqueName = "Unknown Item (901)",
        areaIds = mapOf(
            Episode.IV to listOf(6, 7, 8),
        ),
        typeId = 901,
    ),
    BigBlackRocks(
        uniqueName = "Big Black Rocks",
        areaIds = mapOf(
            Episode.IV to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        ),
        typeId = 902,
        properties = listOf(
            EntityProp(name = "Model", offset = 52, type = EntityPropType.I32),
        ),
    ),
    UnknownItem903(
        uniqueName = "Unknown Item (903)",
        areaIds = mapOf(
            Episode.IV to listOf(6, 7, 8),
        ),
        typeId = 903,
    ),
    UnknownItem904(
        uniqueName = "Unknown Item (904)",
        areaIds = mapOf(
            Episode.IV to listOf(6, 7, 8),
        ),
        typeId = 904,
    ),
    UnknownItem905(
        uniqueName = "Unknown Item (905)",
        areaIds = mapOf(),
        typeId = 905,
    ),
    UnknownItem906(
        uniqueName = "Unknown Item (906)",
        areaIds = mapOf(),
        typeId = 906,
    ),
    FallingRock(
        uniqueName = "Falling Rock",
        areaIds = mapOf(
            Episode.IV to listOf(6, 7, 8),
        ),
        typeId = 907,
    ),
    DesertPlantHasCollision(
        uniqueName = "Desert Plant (Has Collision)",
        areaIds = mapOf(
            Episode.IV to listOf(6, 7, 8),
        ),
        typeId = 908,
    ),
    DesertFixedTypeBoxBreakableCrystals(
        uniqueName = "Desert Fixed Type Box (Breakable Crystals)",
        areaIds = mapOf(
            Episode.IV to listOf(6, 7, 8),
        ),
        typeId = 909,
    ),
    UnknownItem910(
        uniqueName = "Unknown Item (910)",
        areaIds = mapOf(),
        typeId = 910,
    ),
    BeeHive(
        uniqueName = "Bee Hive",
        areaIds = mapOf(
            Episode.IV to listOf(6, 7, 8),
        ),
        typeId = 911,
        properties = listOf(
            EntityProp(name = "Model", offset = 52, type = EntityPropType.I32),
        ),
    ),
    UnknownItem912(
        uniqueName = "Unknown Item (912)",
        areaIds = mapOf(
            Episode.IV to listOf(6, 7, 8),
        ),
        typeId = 912,
    ),
    Heat(
        uniqueName = "Heat",
        areaIds = mapOf(
            Episode.IV to listOf(6, 7, 8),
        ),
        typeId = 913,
        properties = listOf(
            EntityProp(name = "Radius", offset = 40, type = EntityPropType.F32),
            EntityProp(name = "Fog index no.", offset = 52, type = EntityPropType.I32),
        ),
    ),
    TopOfSaintMillionEgg(
        uniqueName = "Top of Saint Million Egg",
        areaIds = mapOf(
            Episode.IV to listOf(9),
        ),
        typeId = 960,
    ),
    UnknownItem961(
        uniqueName = "Unknown Item (961)",
        areaIds = mapOf(
            Episode.IV to listOf(9),
        ),
        typeId = 961,
    );

    override val simpleName = uniqueName

    companion object {
        /**
         * Use this instead of [values] to avoid unnecessary copying.
         */
        val VALUES: Array<ObjectType> = entries.toTypedArray()
    }
}
