package world.phantasmal.psolib.fileFormats

import world.phantasmal.psolib.cursor.Cursor

class ItemPmt(
    val statBoosts: List<PmtStatBoost>,
    val frames: List<PmtFrame>,
    val barriers: List<PmtBarrier>,
    val units: List<PmtUnit>,
    val tools: List<List<PmtTool>>,
    val weapons: List<List<PmtWeapon>>,
)

class PmtStatBoost(
    val stat1: Int,
    val stat2: Int,
    val amount1: Int,
    val amount2: Int,
)

class PmtFrame(
    val id: Int,
    val type: Int,
    val skin: Int,
    val teamPoints: Int,
    val dfp: Int,
    val evp: Int,
    val blockParticle: Int,
    val blockEffect: Int,
    val frameClass: Int,
    val reserved1: Int,
    val requiredLevel: Int,
    val efr: Int,
    val eth: Int,
    val eic: Int,
    val edk: Int,
    val elt: Int,
    val dfpRange: Int,
    val evpRange: Int,
    val statBoost: Int,
    val techBoost: Int,
    val unknown1: Int,
)

typealias PmtBarrier = PmtFrame

class PmtUnit(
    val id: Int,
    val type: Int,
    val skin: Int,
    val teamPoints: Int,
    val stat: Int,
    val statAmount: Int,
    val plusMinus: Int,
    val reserved: ByteArray,
)

class PmtTool(
    val id: Int,
    val type: Int,
    val skin: Int,
    val teamPoints: Int,
    val amount: Int,
    val tech: Int,
    val cost: Int,
    val itemFlag: Int,
    val reserved: ByteArray,
)

class PmtWeapon(
    val id: Int,
    val type: Int,
    val skin: Int,
    val teamPoints: Int,
    val weaponClass: Int,
    val reserved1: Int,
    val minAtp: Int,
    val maxAtp: Int,
    val reqAtp: Int,
    val reqMst: Int,
    val reqAta: Int,
    val mst: Int,
    val maxGrind: Int,
    val photon: Int,
    val special: Int,
    val ata: Int,
    val statBoost: Int,
    val projectile: Int,
    val photonTrail1x: Int,
    val photonTrail1y: Int,
    val photonTrail2x: Int,
    val photonTrail2y: Int,
    val photonType: Int,
    val unknown1: ByteArray,
    val techBoost: Int,
    val comboType: Int,
)

fun parseItemPmt(cursor: Cursor): ItemPmt {
    val index = parseRel(cursor, parseIndex = true).index

    // This size (65268) of this table seems wrong, so we pass in a hard-coded value.
    val statBoosts = parseStatBoosts(cursor, index[305].offset, 52)
    val frames = parseFrames(cursor, index[7].offset, index[7].size)
    val barriers = parseFrames(cursor, index[8].offset, index[8].size)
    val units = parseUnits(cursor, index[9].offset, index[9].size)
    val tools = mutableListOf<List<PmtTool>>()
    val weapons = mutableListOf<List<PmtWeapon>>()

    for (i in 11..37) {
        tools.add(parseTools(cursor, index[i].offset, index[i].size))
    }

    for (i in 38..275) {
        weapons.add(parseWeapons(cursor, index[i].offset, index[i].size))
    }

    return ItemPmt(
        statBoosts,
        frames,
        barriers,
        units,
        tools,
        weapons,
    )
}

private fun parseStatBoosts(cursor: Cursor, offset: Int, size: Int): List<PmtStatBoost> {
    cursor.seekStart(offset)

    val statBoosts = mutableListOf<PmtStatBoost>()

    repeat(size) {
        statBoosts.add(PmtStatBoost(
            stat1 = cursor.uByte().toInt(),
            stat2 = cursor.uByte().toInt(),
            amount1 = cursor.short().toInt(),
            amount2 = cursor.short().toInt(),
        ))
    }

    return statBoosts
}

private fun parseFrames(cursor: Cursor, offset: Int, size: Int): List<PmtFrame> {
    cursor.seekStart(offset)

    val frames = mutableListOf<PmtFrame>()

    repeat(size) {
        frames.add(PmtFrame(
            id = cursor.int(),
            type = cursor.short().toInt(),
            skin = cursor.short().toInt(),
            teamPoints = cursor.int(),
            dfp = cursor.short().toInt(),
            evp = cursor.short().toInt(),
            blockParticle = cursor.uByte().toInt(),
            blockEffect = cursor.uByte().toInt(),
            frameClass = cursor.uByte().toInt(),
            reserved1 = cursor.uByte().toInt(),
            requiredLevel = cursor.uByte().toInt(),
            efr = cursor.uByte().toInt(),
            eth = cursor.uByte().toInt(),
            eic = cursor.uByte().toInt(),
            edk = cursor.uByte().toInt(),
            elt = cursor.uByte().toInt(),
            dfpRange = cursor.uByte().toInt(),
            evpRange = cursor.uByte().toInt(),
            statBoost = cursor.uByte().toInt(),
            techBoost = cursor.uByte().toInt(),
            unknown1 = cursor.short().toInt(),
        ))
    }

    return frames
}

private fun parseUnits(cursor: Cursor, offset: Int, size: Int): List<PmtUnit> {
    cursor.seekStart(offset)

    val units = mutableListOf<PmtUnit>()

    repeat(size) {
        units.add(PmtUnit(
            id = cursor.int(),
            type = cursor.short().toInt(),
            skin = cursor.short().toInt(),
            teamPoints = cursor.int(),
            stat = cursor.short().toInt(),
            statAmount = cursor.short().toInt(),
            plusMinus = cursor.uByte().toInt(),
            reserved = cursor.byteArray(3),
        ))
    }

    return units
}

private fun parseTools(cursor: Cursor, offset: Int, size: Int): List<PmtTool> {
    cursor.seekStart(offset)

    val tools = mutableListOf<PmtTool>()

    repeat(size) {
        tools.add(PmtTool(
            id = cursor.int(),
            type = cursor.short().toInt(),
            skin = cursor.short().toInt(),
            teamPoints = cursor.int(),
            amount = cursor.short().toInt(),
            tech = cursor.short().toInt(),
            cost = cursor.int(),
            itemFlag = cursor.uByte().toInt(),
            reserved = cursor.byteArray(3),
        ))
    }

    return tools
}

private fun parseWeapons(cursor: Cursor, offset: Int, size: Int): List<PmtWeapon> {
    cursor.seekStart(offset)

    val weapons = mutableListOf<PmtWeapon>()

    repeat(size) {
        weapons.add(PmtWeapon(
            id = cursor.int(),
            type = cursor.short().toInt(),
            skin = cursor.short().toInt(),
            teamPoints = cursor.int(),
            weaponClass = cursor.uByte().toInt(),
            reserved1 = cursor.uByte().toInt(),
            minAtp = cursor.short().toInt(),
            maxAtp = cursor.short().toInt(),
            reqAtp = cursor.short().toInt(),
            reqMst = cursor.short().toInt(),
            reqAta = cursor.short().toInt(),
            mst = cursor.short().toInt(),
            maxGrind = cursor.uByte().toInt(),
            photon = cursor.byte().toInt(),
            special = cursor.uByte().toInt(),
            ata = cursor.uByte().toInt(),
            statBoost = cursor.uByte().toInt(),
            projectile = cursor.uByte().toInt(),
            photonTrail1x = cursor.byte().toInt(),
            photonTrail1y = cursor.byte().toInt(),
            photonTrail2x = cursor.byte().toInt(),
            photonTrail2y = cursor.byte().toInt(),
            photonType = cursor.byte().toInt(),
            unknown1 = cursor.byteArray(5),
            techBoost = cursor.uByte().toInt(),
            comboType = cursor.uByte().toInt(),
        ))
    }

    return weapons
}
