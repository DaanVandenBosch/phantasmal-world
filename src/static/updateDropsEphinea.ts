import 'isomorphic-fetch';
import cheerio from 'cheerio';
import fs from 'fs';

const SECTION_IDS = [
    'Viridia', 'Greenill', 'Skyly', 'Bluefull', 'Purplenum', 'Pinkal', 'Redria', 'Oran', 'Yellowboze', 'Whitill',
];

async function update() {
    const csv =
        [
            ['mode', 'episode', 'section_id', 'monster', 'item', 'drop_rate', 'rare_rate'],
            ...await download('normal'),
            ...await download('hard'),
            ...await download('vhard', 'very-hard'),
            ...await download('ultimate')
        ]
            .map(r => r.join('\t'))
            .join('\n')

    return fs.promises.writeFile('./public/drops.ephinea.tsv', csv);
}

async function download(mode: string, modeUrl: string = mode) {
    const response = await fetch(`https://ephinea.pioneer2.net/drop-charts/${modeUrl}/`);
    const body = await response.text();
    const $ = cheerio.load(body);

    let episode = 1;
    const data: any[][] = [];

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
                const monster = monsterText.split('/')[mode === 'ultimate' ? 1 : 0] || monsterText;

                $('td', tr).each((tdI, td) => {
                    if (tdI === 0) {
                        return;
                    }

                    const sectionId = SECTION_IDS[tdI - 1];

                    if (isBox) {
                        $('font font', td).each((_, font) => {
                            const item = $('b', font).text();
                            const rareRateNum = parseFloat($('sup', font).text());
                            const rareRateDenom = parseFloat($('sub', font).text());

                            data.push(
                                [
                                    mode,
                                    episode,
                                    sectionId,
                                    `${monster} Box`,
                                    item,
                                    1,
                                    rareRateNum / rareRateDenom
                                ]
                            );
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

                            data.push(
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