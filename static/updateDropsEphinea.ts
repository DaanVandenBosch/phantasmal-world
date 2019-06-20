import cheerio from 'cheerio';
import fs from 'fs';
import 'isomorphic-fetch';
import { Difficulty, NpcType, SectionId, SectionIds } from '../src/domain';
import { BoxDropDto, EnemyDropDto, ItemTypeDto } from '../src/dto';

export async function updateDropsFromWebsite(itemTypes: ItemTypeDto[]) {
    const normal = await download(itemTypes, Difficulty.Normal);
    const hard = await download(itemTypes, Difficulty.Hard);
    const vhard = await download(itemTypes, Difficulty.VHard, 'very-hard');
    const ultimate = await download(itemTypes, Difficulty.Ultimate);

    const enemyJson = JSON.stringify([
        ...normal.enemyDrops,
        ...hard.enemyDrops,
        ...vhard.enemyDrops,
        ...ultimate.enemyDrops
    ], null, 4);

    await fs.promises.writeFile('./public/enemyDrops.ephinea.json', enemyJson);

    const boxJson = JSON.stringify([
        ...normal.boxDrops,
        ...hard.boxDrops,
        ...vhard.boxDrops,
        ...ultimate.boxDrops
    ], null, 4);

    await fs.promises.writeFile('./public/boxDrops.ephinea.json', boxJson);
}

async function download(
    itemTypes: ItemTypeDto[],
    difficulty: Difficulty,
    difficultyUrl: string = Difficulty[difficulty].toLowerCase()
) {
    const response = await fetch(`https://ephinea.pioneer2.net/drop-charts/${difficultyUrl}/`);
    const body = await response.text();
    const $ = cheerio.load(body);

    let episode = 1;
    const data: {
        enemyDrops: Array<EnemyDropDto>, boxDrops: Array<BoxDropDto>, items: Set<string>
    } = {
        enemyDrops: [], boxDrops: [], items: new Set()
    };

    $('table').each((tableI, table) => {
        const isBox = tableI >= 3;

        $('tr', table).each((_, tr) => {
            const enemyOrBoxText = $(tr.firstChild).text();

            if (enemyOrBoxText.trim() === '') {
                return;
            } else if (enemyOrBoxText.startsWith('EPISODE ')) {
                episode = parseInt(enemyOrBoxText.slice(-1), 10);
                return;
            }

            try {
                let enemyOrBox = enemyOrBoxText.split('/')[
                    difficulty === Difficulty.Ultimate ? 1 : 0
                ] || enemyOrBoxText;

                if (enemyOrBox === 'Halo Rappy') {
                    enemyOrBox = 'Hallo Rappy';
                } else if (enemyOrBox === 'Dal Ral Lie') {
                    enemyOrBox = 'Dal Ra Lie';
                } else if (enemyOrBox === 'Vol Opt ver. 2') {
                    enemyOrBox = 'Vol Opt ver.2';
                } else if (enemyOrBox === 'Za Boota') {
                    enemyOrBox = 'Ze Boota';
                } else if (enemyOrBox === 'Saint Million') {
                    enemyOrBox = 'Saint-Milion';
                }

                $('td', tr).each((tdI, td) => {
                    if (tdI === 0) {
                        return;
                    }

                    const sectionId = SectionIds[tdI - 1];

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
                        return;
                    } else {
                        const item = $('font b', td).text();

                        if (item.trim() === '') {
                            return;
                        }

                        try {
                            const itemType = itemTypes.find(i => i.name === item);

                            if (!itemType) {
                                throw new Error(`No item type found with name "${item}".`)
                            }

                            const npcType = NpcType.byNameAndEpisode(enemyOrBox, episode);

                            if (!npcType) {
                                throw new Error(`Couldn't retrieve NpcType.`);
                            }

                            const title = $('font abbr', td).attr('title').replace('\r', '');
                            const [, dropRateNum, dropRateDenom] =
                                /Drop Rate: (\d+)\/(\d+(\.\d+)?)/g.exec(title)!.map(parseFloat);
                            const [, rareRateNum, rareRateDenom] =
                                /Rare Rate: (\d+)\/(\d+(\.\d+)?)/g.exec(title)!.map(parseFloat);

                            data.enemyDrops.push({
                                difficulty: Difficulty[difficulty],
                                episode,
                                sectionId: SectionId[sectionId],
                                enemy: npcType.code,
                                itemTypeId: itemType.id,
                                dropRate: dropRateNum / dropRateDenom,
                                rareRate: rareRateNum / rareRateDenom,
                            });

                            data.items.add(item);
                        } catch (e) {
                            console.error(`Error while processing item ${item} of ${enemyOrBox} in episode ${episode} ${Difficulty[difficulty]}.`, e);
                        }
                    }
                });
            } catch (e) {
                console.error(`Error while processing ${enemyOrBoxText} in episode ${episode} ${difficulty}.`, e);
            }
        });
    });

    return data;
}
