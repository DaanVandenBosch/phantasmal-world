package world.phantasmal.web.assetsGeneration

import mu.KotlinLogging
import world.phantasmal.core.Failure
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.compression.prs.prsDecompress
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psolib.fileFormats.parseBml
import world.phantasmal.psolib.fileFormats.parseGsl
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Mapping from GSL entry name to a list of (BML entry index, output NpcType name) pairs.
 *
 * PSO BB's data.gsl contains BML archives, each of which bundles one or more models. The BML
 * entry index selects which model within the BML to extract. The output name determines the
 * filename written to the assets directory (e.g. "Migium" -> Migium.nj + Migium.xvm).
 */
private data class BmlModelMapping(
    val gslEntryName: String,
    /** Index of the model entry within the BML archive. */
    val bmlEntryIndex: Int,
    /** NpcType name used as the output filename. */
    val outputName: String,
    /** File extension for the geometry file. */
    val geomExtension: String = "nj",
)

/**
 * Mapping for extracting animation (.njm) files from BML archives.
 */
private data class BmlAnimMapping(
    val gslEntryName: String,
    /** Index of the animation entry within the BML archive. */
    val bmlEntryIndex: Int,
    /** Output filename (e.g. "PanArms_wait.njm"). */
    val outputFilename: String,
)

/**
 * Known mappings from data.gsl BML entries to NpcType names.
 *
 * These were determined by inspecting the GSL entry listing and parsing each BML to see internal
 * entry names. BML files bundle PRS-compressed model (.nj/.xj) and texture (.xvm) data.
 */
private val MODEL_MAPPINGS = listOf(
    // Recobox spawned enemy — inside the Recobox BML
    // [0] me7_all.nj = Recon (frog-like robot creature released by Recobox)
    // [1] me7_box_all.nj = Recobox (box form)
    // [2] bomb_body.nj = bomb projectile
    BmlModelMapping("bm_ene_recobox.bml", 0, "Recon"),

    // Dark Gunner family — bm_ene_darkgunner.bml
    // [0] re5_b_cly_cy_line.nj (cylinder effect)
    // [1] re5_b_gunner_body.nj (Dark/Death Gunner body)
    // [2] re5_b_rcly_cy_line.nj (reverse cylinder - DarkGunnerCenter pillar)
    BmlModelMapping("bm_ene_darkgunner.bml", 1, "DeathGunner"),
    BmlModelMapping("bm_ene_darkgunner.bml", 0, "DarkGunnerCenter"),

    // Seasonal Rappies — each in their own BML
    // Entry [0] = generic Rappy base, entry [1] = seasonal variant model
    BmlModelMapping("bm_ene_lappy_xs.bml", 1, "StRappy"),
    BmlModelMapping("bm_ene_lappy_hw.bml", 1, "HalloRappy"),
    BmlModelMapping("bm_ene_lappy_es.bml", 1, "EggRappy"),

    // Vol Opt parts — inside bm_boss3_volopt.bml
    // [17] me5p01_y_all.nj = Vol Opt Phase 1
    // [19] me5p02_y_all.nj = Vol Opt Phase 2 main body
    // [24] me5_y_all.nj = Vol Opt combined/core
    // [16] fs_obj_hiraishin_a.nj = Lightning rod (避雷針)
    // Monitor entries [1-12] are XJ format display/screen objects
    BmlModelMapping("bm_boss3_volopt.bml", 17, "VolOptPart1"),
    BmlModelMapping("bm_boss3_volopt.bml", 20, "VolOptPart1Sub"),
    BmlModelMapping("bm_boss3_volopt.bml", 19, "VolOptCore"),
    BmlModelMapping("bm_boss3_volopt.bml", 1, "VolOptMonitor", geomExtension = "xj"),
    BmlModelMapping("bm_boss3_volopt.bml", 16, "VolOptHiraisin"),
)

/**
 * Animation mappings. BML animation entries contain only model data (PRS-compressed NJM),
 * no texture data.
 */
private val ANIM_MAPPINGS = listOf(
    // PanArms animations (bone animations for ps_ma_body skeleton)
    BmlAnimMapping("bm4_ps_ma_body.bml", 7, "PanArms_apper.njm"),
    BmlAnimMapping("bm4_ps_ma_body.bml", 8, "PanArms_atack.njm"),
    BmlAnimMapping("bm4_ps_ma_body.bml", 9, "PanArms_damage.njm"),
    BmlAnimMapping("bm4_ps_ma_body.bml", 10, "PanArms_kie.njm"),
    BmlAnimMapping("bm4_ps_ma_body.bml", 14, "PanArms_nodam.njm"),
    BmlAnimMapping("bm4_ps_ma_body.bml", 16, "PanArms_wait2.njm"),
    BmlAnimMapping("bm4_ps_ma_body.bml", 17, "PanArms_wait.njm"),
)

/**
 * Try to PRS-decompress a buffer. If decompression fails, return the raw data as-is
 * (some BML entries store uncompressed data).
 */
private fun tryDecompress(data: Buffer): ByteArray {
    val result = prsDecompress(data.cursor())

    return if (result is Failure) {
        logger.info { "    PRS decompression failed, using raw data" }
        data.byteArray.copyOf(data.size)
    } else {
        val cursor = result.unwrap()
        cursor.buffer().let { buf -> buf.byteArray.copyOf(buf.size) }
    }
}

/**
 * Extracts NPC models from PSO BB game data (data.gsl) and writes them as .nj/.xvm files.
 *
 * Usage: ExtractNpcModels <pso-data-dir> <output-dir>
 *   pso-data-dir: Path to PSO BB data directory (e.g. D:/PSO/EphineaPSO2/data)
 *   output-dir:   Path to write extracted assets (e.g. web/src/jsMain/resources/assets/npcs)
 */
fun main(args: Array<String>) {
    require(args.size >= 2) {
        "Usage: ExtractNpcModels <pso-data-dir> <output-dir>"
    }

    val dataDir = File(args[0])
    val outputDir = File(args[1])

    require(dataDir.isDirectory) { "PSO data directory not found: $dataDir" }
    outputDir.mkdirs()

    val gslFile = File(dataDir, "data.gsl")
    require(gslFile.isFile) { "data.gsl not found: $gslFile" }

    logger.info { "Parsing data.gsl from $gslFile..." }
    val gslData = Buffer.fromByteArray(gslFile.readBytes())
    val gslEntries = parseGsl(gslData.cursor()).unwrap()

    // List all GSL entries for inspection.
    logger.info { "GSL entries (${gslEntries.size} total):" }
    for (entry in gslEntries) {
        logger.info { "  ${entry.name} (${entry.data.size} bytes)" }
    }

    // Build a lookup from GSL entry name to GSL entry.
    val gslEntryByName = gslEntries.associateBy { it.name }

    // Group mappings by GSL entry name to avoid parsing the same BML multiple times.
    val mappingsByGslEntry = MODEL_MAPPINGS.groupBy { it.gslEntryName }

    var extracted = 0
    var failed = 0

    for ((gslEntryName, mappings) in mappingsByGslEntry) {
        val gslEntry = gslEntryByName[gslEntryName]

        if (gslEntry == null) {
            logger.warn { "GSL entry '$gslEntryName' not found in data.gsl — skipping ${mappings.size} mapping(s)" }
            failed += mappings.size
            continue
        }

        logger.info { "Parsing BML: $gslEntryName (${gslEntry.data.size} bytes)..." }
        val bmlEntries = parseBml(gslEntry.data.cursor()).unwrap()
        logger.info { "  BML contains ${bmlEntries.size} entries:" }

        for ((i, bmlEntry) in bmlEntries.withIndex()) {
            logger.info { "    [$i] ${bmlEntry.name} (model: ${bmlEntry.data.size} bytes, texture: ${bmlEntry.textureData.size} bytes)" }
        }

        for (mapping in mappings) {
            if (mapping.bmlEntryIndex >= bmlEntries.size) {
                logger.warn { "  BML entry index ${mapping.bmlEntryIndex} out of range for $gslEntryName (has ${bmlEntries.size} entries) — skipping ${mapping.outputName}" }
                failed++
                continue
            }

            val bmlEntry = bmlEntries[mapping.bmlEntryIndex]
            logger.info { "  Extracting ${mapping.outputName} from BML entry [${mapping.bmlEntryIndex}] '${bmlEntry.name}'..." }

            try {
                // Decompress and write model data.
                val modelBytes = tryDecompress(bmlEntry.data)
                val geomFile = File(outputDir, "${mapping.outputName}.${mapping.geomExtension}")
                geomFile.writeBytes(modelBytes)
                logger.info { "    Wrote ${geomFile.name} (${modelBytes.size} bytes)" }

                // Decompress and write texture data (.xvm) if present.
                if (bmlEntry.textureData.size > 0) {
                    val textureBytes = tryDecompress(bmlEntry.textureData)
                    val xvmFile = File(outputDir, "${mapping.outputName}.xvm")
                    xvmFile.writeBytes(textureBytes)
                    logger.info { "    Wrote ${xvmFile.name} (${textureBytes.size} bytes)" }
                } else {
                    logger.info { "    No texture data for ${mapping.outputName}" }
                }

                extracted++
            } catch (e: Exception) {
                logger.error(e) { "  Failed to extract ${mapping.outputName}" }
                failed++
            }
        }
    }

    // Extract animations from BML archives.
    val animMappingsByGslEntry = ANIM_MAPPINGS.groupBy { it.gslEntryName }

    for ((gslEntryName, mappings) in animMappingsByGslEntry) {
        val gslEntry = gslEntryByName[gslEntryName]

        if (gslEntry == null) {
            logger.warn { "GSL entry '$gslEntryName' not found in data.gsl — skipping ${mappings.size} animation(s)" }
            failed += mappings.size
            continue
        }

        logger.info { "Parsing BML for animations: $gslEntryName..." }
        val bmlEntries = parseBml(gslEntry.data.cursor()).unwrap()

        for (mapping in mappings) {
            if (mapping.bmlEntryIndex >= bmlEntries.size) {
                logger.warn { "  BML entry index ${mapping.bmlEntryIndex} out of range — skipping ${mapping.outputFilename}" }
                failed++
                continue
            }

            val bmlEntry = bmlEntries[mapping.bmlEntryIndex]
            logger.info { "  Extracting ${mapping.outputFilename} from BML entry [${mapping.bmlEntryIndex}] '${bmlEntry.name}'..." }

            try {
                val animBytes = tryDecompress(bmlEntry.data)
                val animFile = File(outputDir, mapping.outputFilename)
                animFile.writeBytes(animBytes)
                logger.info { "    Wrote ${animFile.name} (${animBytes.size} bytes)" }
                extracted++
            } catch (e: Exception) {
                logger.error(e) { "  Failed to extract ${mapping.outputFilename}" }
                failed++
            }
        }
    }

    logger.info { "Done. Extracted $extracted model(s) and animation(s), $failed failure(s)." }
}
