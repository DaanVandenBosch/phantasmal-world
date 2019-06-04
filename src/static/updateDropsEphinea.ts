import 'isomorphic-fetch';
import cheerio from 'cheerio';
import fs from 'fs';

const SECTION_IDS = [
    'Viridia', 'Greenill', 'Skyly', 'Bluefull', 'Purplenum', 'Pinkal', 'Redria', 'Oran', 'Yellowboze', 'Whitill',
];
const ENEMY_DROPS_HEADER = ['difficulty', 'episode', 'section_id', 'enemy', 'item', 'drop_rate', 'rare_rate'];
const BOX_DROPS_HEADER = ['difficulty', 'episode', 'section_id', 'box', 'item', 'drop_rate'];
const ITEMS_HEADER = ['name'];

async function update() {
    const normal = await download('normal');
    const hard = await download('hard');
    const vhard = await download('vhard', 'very-hard');
    const ultimate = await download('ultimate');

    const enemyCsv =
        [
            ENEMY_DROPS_HEADER,
            ...normal.enemyDrops,
            ...hard.enemyDrops,
            ...vhard.enemyDrops,
            ...ultimate.enemyDrops
        ]
            .map(r => r.join('\t'))
            .join('\n');

    await fs.promises.writeFile('./public/enemy_drops.ephinea.tsv', enemyCsv);

    const boxCsv =
        [
            BOX_DROPS_HEADER,
            ...normal.boxDrops,
            ...hard.boxDrops,
            ...vhard.boxDrops,
            ...ultimate.boxDrops
        ]
            .map(r => r.join('\t'))
            .join('\n');

    await fs.promises.writeFile('./public/box_drops.ephinea.tsv', boxCsv);

    const items = new Set([...normal.items, ...hard.items, ...vhard.items, ...ultimate.items]);

    const itemsCsv =
        [
            ITEMS_HEADER,
            ...[...items].sort()
        ]
            .join('\n');

    await fs.promises.writeFile('./public/items.ephinea.tsv', itemsCsv);
}

async function download(mode: string, modeUrl: string = mode) {
    const response = await fetch(`https://ephinea.pioneer2.net/drop-charts/${modeUrl}/`);
    const body = await response.text();
    const $ = cheerio.load(body);

    let episode = 1;
    const data: {
        enemyDrops: any[][], boxDrops: any[][], items: Set<string>
    } = {
        enemyDrops: [], boxDrops: [], items: new Set()
    };

    $('table').each((tableI, table) => {
        const isBox = tableI >= 3;

        $('tr', table).each((_, tr) => {
            const monsterText = $(tr.firstChild).text();

            if (monsterText.trim() === '') {
                return;
            } else if (monsterText.startsWith('EPISODE ')) {
                episode = parseInt(monsterText.slice(-1), 10);
                return;
            }

            try {
                let monster = monsterText.split('/')[mode === 'ultimate' ? 1 : 0] || monsterText;

                if (monster === 'Halo Rappy') {
                    monster = 'Hallo Rappy';
                } else if (monster === 'Dal Ral Lie') {
                    monster = 'Dal Ra Lie';
                } else if (monster === 'Vol Opt ver. 2') {
                    monster = 'Vol Opt ver.2';
                } else if (monster === 'Za Boota') {
                    monster = 'Ze Boota';
                } else if (monster === 'Saint Million') {
                    monster = 'Saint-Million';
                }

                $('td', tr).each((tdI, td) => {
                    if (tdI === 0) {
                        return;
                    }

                    const sectionId = SECTION_IDS[tdI - 1];

                    if (isBox) {
                        $('font font', td).each((_, font) => {
                            const item = $('b', font).text();
                            const rateNum = parseFloat($('sup', font).text());
                            const rateDenom = parseFloat($('sub', font).text());

                            data.boxDrops.push(
                                [
                                    mode,
                                    episode,
                                    sectionId,
                                    monster,
                                    item,
                                    rateNum / rateDenom
                                ]
                            );

                            data.items.add(item);
                        });
                        return;
                    } else {
                        const item = $('font b', td).text();

                        if (item.trim() === '') {
                            return;
                        }

                        try {
                            const title = $('font abbr', td).attr('title').replace('\r', '');
                            const [, dropRateNum, dropRateDenom] =
                                /Drop Rate: (\d+)\/(\d+(\.\d+)?)/g.exec(title)!.map(parseFloat);
                            const [, rareRateNum, rareRateDenom] =
                                /Rare Rate: (\d+)\/(\d+(\.\d+)?)/g.exec(title)!.map(parseFloat);

                            data.enemyDrops.push(
                                [
                                    mode,
                                    episode,
                                    sectionId,
                                    monster,
                                    item,
                                    dropRateNum / dropRateDenom,
                                    rareRateNum / rareRateDenom,
                                ]
                            );

                            data.items.add(item);
                        } catch (e) {
                            console.error(`Error while processing item ${item} of ${monster} in episode ${episode} ${mode}.`, e);
                        }
                    }
                });
            } catch (e) {
                console.error(`Error while processing ${monsterText} in episode ${episode} ${mode}.`, e);
            }
        });
    });

    return data;
}

update().catch((e) => {
    console.error(e);
});