import cheerio from "cheerio";
import { writeFileSync } from "fs";
import "isomorphic-fetch";
import Logger from "js-logger";
import { ASSETS_DIR } from ".";
import { Difficulty, SectionId, SectionIds } from "../src/core/domain";
import { BoxDropDto, EnemyDropDto, ItemTypeDto } from "../src/core/dto";
import {
    name_and_episode_to_npc_type,
    NpcType,
} from "../src/core/data_formats/parsing/quest/npc_types";

const logger = Logger.get("assets_generation/update_drops_ephinea");

export async function update_drops_from_website(item_types: ItemTypeDto[]): Promise<void> {
    logger.info("Updating item drops.");

    const normal = await download(item_types, Difficulty.Normal);
    const hard = await download(item_types, Difficulty.Hard);
    const vhard = await download(item_types, Difficulty.VHard, "very-hard");
    const ultimate = await download(item_types, Difficulty.Ultimate);

    const enemy_json = JSON.stringify(
        [...normal.enemy_drops, ...hard.enemy_drops, ...vhard.enemy_drops, ...ultimate.enemy_drops],
        null,
        4,
    );

    writeFileSync(`${ASSETS_DIR}/enemyDrops.ephinea.json`, enemy_json);

    const box_json = JSON.stringify(
        [...normal.box_drops, ...hard.box_drops, ...vhard.box_drops, ...ultimate.box_drops],
        null,
        4,
    );

    writeFileSync(`${ASSETS_DIR}/boxDrops.ephinea.json`, box_json);

    logger.info("Done updating item drops.");
}

async function download(
    item_types: ItemTypeDto[],
    difficulty: Difficulty,
    difficulty_url: string = Difficulty[difficulty].toLowerCase(),
): Promise<{ enemy_drops: EnemyDropDto[]; box_drops: BoxDropDto[]; items: Set<string> }> {
    const response = await fetch(`https://ephinea.pioneer2.net/drop-charts/${difficulty_url}/`);
    const body = await response.text();
    const $ = cheerio.load(body);

    let episode = 1;
    const data: {
        enemy_drops: EnemyDropDto[];
        box_drops: BoxDropDto[];
        items: Set<string>;
    } = {
        enemy_drops: [],
        box_drops: [],
        items: new Set(),
    };

    $("table").each((table_i, table) => {
        const is_box = table_i >= 3;

        $("tr", table).each((_, tr) => {
            const enemy_or_box_text = $(tr.firstChild).text();

            if (enemy_or_box_text.trim() === "") {
                return;
            } else if (enemy_or_box_text.startsWith("EPISODE ")) {
                episode = parseInt(enemy_or_box_text.slice(-1), 10);
                return;
            }

            try {
                let enemy_or_box =
                    enemy_or_box_text.split("/")[difficulty === Difficulty.Ultimate ? 1 : 0] ||
                    enemy_or_box_text;

                if (enemy_or_box === "Halo Rappy") {
                    enemy_or_box = "Hallo Rappy";
                } else if (enemy_or_box === "Dal Ral Lie") {
                    enemy_or_box = "Dal Ra Lie";
                } else if (enemy_or_box === "Vol Opt ver. 2") {
                    enemy_or_box = "Vol Opt ver.2";
                } else if (enemy_or_box === "Za Boota") {
                    enemy_or_box = "Ze Boota";
                } else if (enemy_or_box === "Saint Million") {
                    enemy_or_box = "Saint-Milion";
                }

                $("td", tr).each((td_i, td) => {
                    if (td_i === 0) {
                        return;
                    }

                    const section_id = SectionIds[td_i - 1];

                    if (is_box) {
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
                        return;
                    } else {
                        const item = $("font b", td).text();

                        if (item.trim() === "") {
                            return;
                        }

                        try {
                            const item_type = item_types.find(i => i.name === item);

                            if (!item_type) {
                                throw new Error(`No item type found with name "${item}".`);
                            }

                            const npc_type = name_and_episode_to_npc_type(enemy_or_box, episode);

                            if (!npc_type) {
                                throw new Error(`Couldn't retrieve NpcType.`);
                            }

                            const title = $("font abbr", td)
                                .attr("title")
                                .replace("\r", "");
                            const [
                                ,
                                drop_rate_num,
                                drop_rate_denom,
                            ] = /Drop Rate: (\d+)\/(\d+(\.\d+)?)/g.exec(title)!.map(parseFloat);
                            const [
                                ,
                                rare_rate_num,
                                rare_rate_denom,
                            ] = /Rare Rate: (\d+)\/(\d+(\.\d+)?)/g.exec(title)!.map(parseFloat);

                            data.enemy_drops.push({
                                difficulty: Difficulty[difficulty],
                                episode,
                                sectionId: SectionId[section_id],
                                enemy: NpcType[npc_type],
                                itemTypeId: item_type.id,
                                dropRate: drop_rate_num / drop_rate_denom,
                                rareRate: rare_rate_num / rare_rate_denom,
                            });

                            data.items.add(item);
                        } catch (e) {
                            logger.error(
                                `Error while processing item ${item} of ${enemy_or_box} in episode ${episode} ${Difficulty[difficulty]}.`,
                                e,
                            );
                        }
                    }
                });
            } catch (e) {
                logger.error(
                    `Error while processing ${enemy_or_box_text} in episode ${episode} ${difficulty}.`,
                    e,
                );
            }
        });
    });

    return data;
}
