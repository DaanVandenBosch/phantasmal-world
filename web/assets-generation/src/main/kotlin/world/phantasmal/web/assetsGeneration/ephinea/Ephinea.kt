package world.phantasmal.web.assetsGeneration.ephinea

import kotlinx.serialization.encodeToString
import mu.KotlinLogging
import world.phantasmal.core.removeAt
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.compression.prs.prsDecompress
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psolib.fileFormats.ItemPmt
import world.phantasmal.psolib.fileFormats.parseItemPmt
import world.phantasmal.psolib.fileFormats.parseUnitxt
import world.phantasmal.web.shared.JSON_FORMAT_PRETTY
import world.phantasmal.web.shared.dto.*
import java.io.File
import java.util.Comparator.comparing

private val logger = KotlinLogging.logger {}

/**
 * ItemPMT.bin and ItemPT.gsl comes from stock Tethealla. ItemPT.gsl is not used at the moment.
 * unitxt_j.prs comes from the Ephinea client.
 * TODO: manual fixes:
 *  - Clio is equipable by HUnewearls
 *  - Red Ring has a requirement of 180, not 108
 */
fun generateEphineaAssets(outputDir: File) {
    logger.info("Updating static Ephinea assets.")

    val items = loadItems(loadItemNames())

    logger.info("Updating item type data.")

    File(outputDir, "item_types.ephinea.json")
        .writeText(JSON_FORMAT_PRETTY.encodeToString(items))

    updateDropsFromWebsite(outputDir, items)

    logger.info("Done updating static Ephinea assets.")
}

/**
 * Extracts item names from unitxt file.
 */
private fun loadItemNames(): List<String> {
    val file = "/ephinea/client/data/unitxt_j.prs"

    logger.info("Loading item names from $file.")

    val unitxt = parseUnitxt(prsDecompress(Buffer.fromResource(file).cursor()).unwrap())

    val itemNames = unitxt.categories[1].toMutableList()
    // Strip custom Ephinea items until we have the Ephinea ItemPMT.bin.
    itemNames.removeAt(177, 50)
    itemNames.removeAt(639, 59)

    logger.info("Done loading item names.")

    return itemNames
}

/**
 * Loads items from ItemPMT.
 */
private fun loadItems(itemNames: List<String>): List<ItemType> {
    val file = "/ephinea/ship-config/param/ItemPMT.bin"

    logger.info("Loading item type data from $file.")

    val itemPmt = parseItemPmt(Buffer.fromResource(file).cursor())
    val itemTypes = mutableListOf<ItemType>()
    val ids = mutableSetOf<Int>()

    fun checkId(id: Int, type: String, name: String) {
        check(ids.add(id)) {
            """Trying to add $type with ID $id ($name) but ID already exists."""
        }
    }

    for ((categoryI, category) in itemPmt.weapons.withIndex()) {
        for ((i, weapon) in category.withIndex()) {
            val id = (categoryI shl 8) + i
            val name = itemNames[weapon.id]
            checkId(id, "weapon", name)

            itemTypes.add(WeaponItemType(
                id,
                name,
                weapon.minAtp,
                weapon.maxAtp,
                weapon.ata,
                weapon.maxGrind,
                weapon.reqAtp,
            ))
        }
    }

    for ((i, frame) in itemPmt.frames.withIndex()) {
        val id = 0x10100 + i
        val name = itemNames[frame.id]
        checkId(id, "frame", name)

        val stats = getStatBoosts(itemPmt, frame.statBoost)

        itemTypes.add(FrameItemType(
            id,
            name,
            stats.atp,
            stats.ata,
            minEvp = frame.evp + stats.minEvp,
            maxEvp = frame.evp + stats.minEvp + frame.evpRange,
            minDfp = frame.dfp + stats.minDfp,
            maxDfp = frame.dfp + stats.minDfp + frame.dfpRange,
            stats.mst,
            stats.hp,
            stats.lck,
        ))
    }

    for ((i, barrier) in itemPmt.barriers.withIndex()) {
        val id = 0x10200 + i
        val name = itemNames[barrier.id]
        checkId(id, "barrier", name)

        val stats = getStatBoosts(itemPmt, barrier.statBoost)

        itemTypes.add(BarrierItemType(
            id,
            name,
            stats.atp,
            stats.ata,
            minEvp = barrier.evp + stats.minEvp,
            maxEvp = barrier.evp + stats.minEvp + barrier.evpRange,
            minDfp = barrier.dfp + stats.minDfp,
            maxDfp = barrier.dfp + stats.minDfp + barrier.dfpRange,
            stats.mst,
            stats.hp,
            stats.lck,
        ))
    }

    for ((i, unit) in itemPmt.units.withIndex()) {
        val id = 0x10300 + i
        val name = itemNames[unit.id]
        checkId(id, "unit", name)

        itemTypes.add(UnitItemType(
            id,
            name,
        ))
    }

    for ((categoryI, category) in itemPmt.tools.withIndex()) {
        for ((i, tool) in category.withIndex()) {
            val id = (0x30000 or (categoryI shl 8)) + i
            val name = itemNames[tool.id]
            checkId(id, "tool", name)

            itemTypes.add(ToolItemType(
                id,
                name,
            ))
        }
    }

    itemTypes.sortWith(comparing({ it.name }, String.CASE_INSENSITIVE_ORDER))

    logger.info("Done loading item type data.")

    return itemTypes
}

private class Boosts(
    val atp: Int,
    val ata: Int,
    val minEvp: Int,
    val minDfp: Int,
    val mst: Int,
    val hp: Int,
    val lck: Int,
)

private fun getStatBoosts(itemPmt: ItemPmt, index: Int): Boosts {
    val statBoosts = itemPmt.statBoosts[index]
    val amount = statBoosts.amount1

    var atp = 0
    var ata = 0
    var minEvp = 0
    var minDfp = 0
    var mst = 0
    var hp = 0
    var lck = 0

    when (statBoosts.stat1) {
        1 -> atp += amount
        2 -> ata += amount
        3 -> minEvp += amount
        4 -> minDfp += amount
        5 -> mst += amount
        6 -> hp += amount
        7 -> lck += amount
        8 -> {
            atp += amount
            ata += amount
            minEvp += amount
            minDfp += amount
            mst += amount
            hp += amount
            lck += amount
        }
        9 -> atp -= amount
        10 -> ata -= amount
        11 -> minEvp -= amount
        12 -> minDfp -= amount
        13 -> mst -= amount
        14 -> hp -= amount
        15 -> lck -= amount
        16 -> {
            atp -= amount
            ata -= amount
            minEvp -= amount
            minDfp -= amount
            mst -= amount
            hp -= amount
            lck -= amount
        }
    }

    return Boosts(atp, ata, minEvp, minDfp, mst, hp, lck)
}
