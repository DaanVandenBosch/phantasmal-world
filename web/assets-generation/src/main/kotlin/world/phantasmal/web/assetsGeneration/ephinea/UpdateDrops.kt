package world.phantasmal.web.assetsGeneration.ephinea

import kotlinx.serialization.encodeToString
import mu.KotlinLogging
import org.jsoup.Jsoup
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.fileFormats.quest.NpcType
import world.phantasmal.psolib.fileFormats.quest.fromNameAndEpisode
import world.phantasmal.web.shared.JSON_FORMAT_PRETTY
import world.phantasmal.web.shared.dto.*
import java.io.File

private val logger = KotlinLogging.logger {}

fun updateDropsFromWebsite(outputDir: File, itemTypes: List<ItemType>) {
    logger.info("Updating item drops.")

    val enemyDrops = mutableListOf<EnemyDrop>()
    val boxDrops = mutableListOf<BoxDrop>()

    download(itemTypes, Difficulty.Normal, "normal", enemyDrops, boxDrops)
    download(itemTypes, Difficulty.Hard, "hard", enemyDrops, boxDrops)
    download(itemTypes, Difficulty.VHard, "very-hard", enemyDrops, boxDrops)
    download(itemTypes, Difficulty.Ultimate, "ultimate", enemyDrops, boxDrops)

    File(outputDir, "enemy_drops.ephinea.json")
        .writeText(JSON_FORMAT_PRETTY.encodeToString(enemyDrops))

    File(outputDir, "box_drops.ephinea.json")
        .writeText(JSON_FORMAT_PRETTY.encodeToString(boxDrops))

    logger.info("Done updating item drops.")
}

private fun download(
    itemTypes: List<ItemType>,
    difficulty: Difficulty,
    difficultyUrl: String,
    enemyDrops: MutableList<EnemyDrop>,
    @Suppress("UNUSED_PARAMETER") boxDrops: MutableList<BoxDrop>,
) {
    val doc = Jsoup.connect("https://ephinea.pioneer2.net/drop-charts/${difficultyUrl}/").get()

    var episode: Episode? = null

    for ((tableI, table) in doc.select("table").withIndex()) {
        val isBox = tableI >= 3

        for (tr in table.select("tr")) {
            val enemyOrBoxText = tr.child(0).text()

            if (enemyOrBoxText.isBlank()) {
                continue
            } else if (enemyOrBoxText.startsWith("EPISODE ")) {
                val ep = enemyOrBoxText.takeLast(1).toInt()
                episode = Episode.fromInt(ep)
                continue
            }

            checkNotNull(episode) { "Couldn't determine episode." }

            try {
                val sanitizedEnemyOrBoxText = enemyOrBoxText.split("/")
                    .getOrElse(if (difficulty == Difficulty.Ultimate) 1 else 0) {
                        enemyOrBoxText
                    }

                val enemyOrBox = when (sanitizedEnemyOrBoxText) {
                    "Halo Rappy" -> "Hallo Rappy"
                    "Dal Ral Lie" -> "Dal Ra Lie"
                    "Vol Opt ver. 2" -> "Vol Opt ver.2"
                    "Za Boota" -> "Ze Boota"
                    "Saint Million" -> "Saint-Milion"
                    else -> sanitizedEnemyOrBoxText
                }

                for ((tdI, td) in tr.select("td").withIndex()) {
                    if (tdI == 0) {
                        continue
                    }

                    val sectionId = SectionId.VALUES[tdI - 1]

                    if (isBox) {
                        // TODO:
                        // $('font font', td).each((_, font) => {
                        //     const item = $('b', font).text();
                        //     const rateNum = parseFloat($('sup', font).text());
                        //     const rateDenom = parseFloat($('sub', font).text());

                        //     data.boxDrops.push({
                        //         difficulty: Difficulty[difficulty],
                        //         episode,
                        //         sectionId: SectionId[sectionId],
                        //         box: enemyOrBox,
                        //         item,
                        //         dropRate: rateNum / rateDenom
                        //     });

                        //     data.items.add(item);
                        // });
                        continue
                    } else {
                        val item = td.select("font b").text()

                        if (item.isBlank()) {
                            continue
                        }

                        try {
                            val itemType = itemTypes.find { it.name == item }

                            checkNotNull(itemType) { """No item type found with name "$item".""" }

                            val npcType = NpcType.fromNameAndEpisode(enemyOrBox, episode)

                            checkNotNull(npcType) {
                                "Couldn't determine NpcType of $enemyOrBox ($episode)."
                            }

                            val title = td.select("font abbr").attr("title").replace("\r", "")

                            val (dropRateNum, dropRateDenom) =
                                Regex(""".*Drop Rate: (\d+)/(\d+(\.\d+)?).*""")
                                    .matchEntire(title)!!
                                    .destructured

                            val (rareRateNum, rareRateDenom) =
                                Regex(""".*Rare Rate: (\d+)/(\d+(\.\d+)?).*""")
                                    .matchEntire(title)!!
                                    .destructured

                            enemyDrops.add(EnemyDrop(
                                difficulty,
                                episode,
                                sectionId,
                                enemy = npcType,
                                itemTypeId = itemType.id,
                                anythingRate = dropRateNum.toDouble() / dropRateDenom.toDouble(),
                                rareRate = rareRateNum.toDouble() / rareRateDenom.toDouble(),
                            ))
                        } catch (e: Exception) {
                            logger.error(
                                "Error while processing item $item of $enemyOrBox in episode $episode ${difficulty}.",
                                e,
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error(
                    "Error while processing $enemyOrBoxText in episode $episode ${difficulty}.",
                    e,
                )
            }
        }
    }
}
