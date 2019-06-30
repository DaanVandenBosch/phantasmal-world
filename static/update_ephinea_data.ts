import fs from 'fs';
import { BufferCursor } from '../src/bin_data/BufferCursor';
import { parseItemPmt, ItemPmt } from '../src/bin_data/parsing/itempmt';
import { parseUnitxt, Unitxt } from '../src/bin_data/parsing/unitxt';
import { Difficulties, Difficulty, Episode, Episodes, NpcType, SectionId, SectionIds } from '../src/domain';
import { NpcTypes } from '../src/domain/NpcType';
import { BoxDropDto, EnemyDropDto, ItemTypeDto, QuestDto } from '../src/dto';
import { updateDropsFromWebsite } from './update_drops_ephinea';
import { parse_quest } from '../src/bin_data/parsing/quest';
import Logger from 'js-logger';

const logger = Logger.get('static/update_ephinea_data');

Logger.useDefaults({ defaultLevel: Logger.ERROR });
logger.setLevel(Logger.INFO);
Logger.get('static/update_drops_ephinea').setLevel(Logger.INFO);
Logger.get('bin_data/parsing/quest').setLevel(Logger.OFF);
Logger.get('bin_data/parsing/quest/bin').setLevel(Logger.OFF);

/**
 * Used by static data generation scripts.
 */
const RESOURCE_DIR = './static/resources/ephinea';
/**
 * Used by production code.
 */
const PUBLIC_DIR = './public';

update().catch(e => logger.error(e));

/**
 * ItemPMT.bin and ItemPT.gsl comes from stock Tethealla. ItemPT.gsl is not used at the moment.
 * unitxt_j.prs comes from the Ephinea client.
 * TODO: manual fixes:
 *  - Clio is equipable by HUnewearls
 *  - Red Ring has a requirement of 180, not 108
 */
async function update() {
    logger.info('Updating static Ephinea data.');

    const unitxt = loadUnitxt();
    const itemNames = unitxt[1];
    const items = updateItems(itemNames);
    await updateDropsFromWebsite(items);
    updateQuests();

    // Use this if we ever get the Ephinea drop files.
    // const itemPt = await loadItemPt();
    // await updateDrops(itemPt);

    logger.info('Done updating static Ephinea data.');
}

/**
 * Shop quests are not processed.
 *
 * TODO: Missing quests:
 *  - Maximum Attack 4th Stage -1R-
 *  - Maximum Attack 4th Stage -2R-
 *  - Maximum Attack 4th Stage -4R-
 *  - Knight of Coral
 *  - Knight of Coral Advent
 *  - CAL's Clock Challenge
 *  - The Value of Money (quest3_e.dat, can't be parsed, luckily doesn't have enemies)
 * Note: The MA4R quests use a random area variation per area from the ABC MA quests. E.g. MA4-1R will use a random caves 2 variation from MA4-1A, MA4-1B or MA4-1C. Same for mines 2 and ruins 2.
 */
function updateQuests() {
    logger.info('Updating quest data.');

    const quests = new Array<QuestDto>();
    processQuestDir(`${RESOURCE_DIR}/ship-config/quest`, quests);

    quests.sort((a, b) => a.episode - b.episode || a.name.localeCompare(b.name));

    const idCounts = quests.reduce(
        (counts, q) => counts.set(q.id, (counts.get(q.id) || 0) + 1),
        new Map<number, number>()
    );

    for (const [id, count] of idCounts.entries()) {
        if (count > 1) {
            logger.error(`Duplicate quest ID ${id}, NOT writing quests file.`);
            return;
        }
    }

    fs.writeFileSync(`${PUBLIC_DIR}/quests.ephinea.json`, JSON.stringify(quests, null, 4));

    logger.info('Done updating quest data.');
}

function processQuestDir(path: string, quests: QuestDto[]) {
    const stat = fs.statSync(path);

    if (stat.isFile()) {
        processQuest(path, quests);
    } else if (stat.isDirectory()) {
        for (const file of fs.readdirSync(path)) {
            processQuestDir(`${path}/${file}`, quests);
        }
    }
}

function processQuest(path: string, quests: QuestDto[]) {
    try {
        const buf = fs.readFileSync(path);
        const q = parse_quest(new BufferCursor(buf.buffer, true), true);

        if (q) {
            logger.trace(`Processing quest "${q.name}".`);

            if (q.quest_no == null) {
                throw new Error('No quest_no.');
            }

            const enemyCounts: { [npcTypeCode: string]: number } = {};

            for (const npc of q.npcs) {
                if (npc.type.enemy) {
                    enemyCounts[npc.type.code] = (enemyCounts[npc.type.code] || 0) + 1;
                }
            }

            quests.push({
                id: q.quest_no,
                name: q.name,
                episode: q.episode,
                enemyCounts
            });
        } else {
            logger.error(`Couldn't process ${path}.`);
        }
    } catch (e) {
        logger.error(`Couldn't process ${path}.`, e);
    }
}

function loadUnitxt(): Unitxt {
    logger.info('Loading unitxt_j.prs.');

    const buf = fs.readFileSync(
        `${RESOURCE_DIR}/client/data/unitxt_j.prs`
    );

    const unitxt = parseUnitxt(new BufferCursor(buf.buffer, true));
    // Strip custom Ephinea items until we have the Ephinea ItemPMT.bin.
    unitxt[1].splice(177, 50);
    unitxt[1].splice(639, 59);

    logger.info('Done loading unitxt_j.prs.');
    return unitxt;
}

function updateItems(itemNames: Array<string>): ItemTypeDto[] {
    logger.info('Updating item type data.');

    const buf = fs.readFileSync(
        `${RESOURCE_DIR}/ship-config/param/ItemPMT.bin`
    );

    const itemPmt = parseItemPmt(new BufferCursor(buf.buffer, true));
    const itemTypes = new Array<ItemTypeDto>();
    const ids = new Set<number>();

    itemPmt.weapons.forEach((category, categoryI) => {
        category.forEach((weapon, i) => {
            const id = (categoryI << 8) + i;

            if (!ids.has(id)) {
                ids.add(id);
                itemTypes.push({
                    class: 'weapon',
                    id,
                    name: itemNames[weapon.id],
                    minAtp: weapon.minAtp,
                    maxAtp: weapon.maxAtp,
                    ata: weapon.ata,
                    maxGrind: weapon.maxGrind,
                    requiredAtp: weapon.reqAtp,
                });
            }
        });
    });

    itemPmt.armors.forEach((armor, i) => {
        const id = 0x10100 + i;

        if (!ids.has(id)) {
            ids.add(id);

            const stats = getStatBoosts(itemPmt, armor.statBoost);
            stats.minEvp += armor.evp;
            stats.minDfp += armor.dfp;

            itemTypes.push({
                class: 'armor',
                id,
                name: itemNames[armor.id],
                ...stats,
                maxEvp: stats.minEvp + armor.evpRange,
                maxDfp: stats.minDfp + armor.dfpRange,
            });
        }
    });

    itemPmt.shields.forEach((shield, i) => {
        const id = 0x10200 + i;

        if (!ids.has(id)) {
            ids.add(id);

            const stats = getStatBoosts(itemPmt, shield.statBoost);
            stats.minEvp += shield.evp;
            stats.minDfp += shield.dfp;

            itemTypes.push({
                class: 'shield',
                id,
                name: itemNames[shield.id],
                ...stats,
                maxEvp: stats.minEvp + shield.evpRange,
                maxDfp: stats.minDfp + shield.dfpRange,
            });
        }
    });

    itemPmt.units.forEach((unit, i) => {
        const id = 0x10300 + i;

        if (!ids.has(id)) {
            ids.add(id);
            itemTypes.push({
                class: 'unit',
                id,
                name: itemNames[unit.id],
            });
        }
    });

    itemPmt.tools.forEach((category, categoryI) => {
        category.forEach((tool, i) => {
            const id = (0x30000 | (categoryI << 8)) + i;

            if (!ids.has(id)) {
                ids.add(id);
                itemTypes.push({
                    class: 'tool',
                    id,
                    name: itemNames[tool.id],
                });
            }
        });
    });

    fs.writeFileSync(
        `${PUBLIC_DIR}/itemTypes.ephinea.json`,
        JSON.stringify(itemTypes, null, 4)
    );

    logger.info('Done updating item type data.');
    return itemTypes;
}

function updateDrops(itemPt: ItemPt) {
    logger.info('Updating drop data.');

    const enemyDrops = new Array<EnemyDropDto>();

    for (const diff of Difficulties) {
        for (const ep of Episodes) {
            for (const sid of SectionIds) {
                enemyDrops.push(...loadEnemyDrops(itemPt, diff, ep, sid));
            }
        }
    }

    fs.writeFileSync(
        `${PUBLIC_DIR}/enemyDrops.ephinea.json`,
        JSON.stringify(enemyDrops, null, 4)
    );

    const boxDrops = new Array<BoxDropDto>();

    for (const diff of Difficulties) {
        for (const ep of Episodes) {
            for (const sid of SectionIds) {
                boxDrops.push(...loadBoxDrops(diff, ep, sid));
            }
        }
    }

    fs.writeFileSync(
        `${PUBLIC_DIR}/boxDrops.ephinea.json`,
        JSON.stringify(boxDrops, null, 4)
    );

    logger.info('Done updating drop data.');
}

type ItemP = {
    darTable: Map<NpcType, number>
}
type ItemPt = Array<Array<Array<ItemP>>>

async function loadItemPt(): Promise<ItemPt> {
    logger.info('Loading ItemPT.gsl.');

    const table: ItemPt = [];
    const buf = await fs.promises.readFile(
        `${RESOURCE_DIR}/ship-config/param/ItemPT.gsl`
    );
    const cursor = new BufferCursor(buf.buffer, false);

    cursor.seek(0x3000);

    // ItemPT.gsl was extracted from PSO for XBox, so it only contains data for ep. I and II.
    // Episode IV data is based on the ep. I and II data.
    for (const episode of [Episode.I, Episode.II]) {
        table[episode] = [];

        for (const diff of Difficulties) {
            table[episode][diff] = [];

            for (const sid of SectionIds) {
                const darTable = new Map<NpcType, number>();

                table[episode][diff][sid] = {
                    darTable
                };

                const startPos = cursor.position;
                cursor.seek(1608);
                const enemyDar = cursor.u8_array(100);

                for (const npc of NpcTypes) {
                    if (npc.episode !== episode) continue;

                    switch (npc) {
                        case NpcType.Dragon:
                        case NpcType.DeRolLe:
                        case NpcType.VolOpt:
                        case NpcType.DarkFalz:
                        case NpcType.BarbaRay:
                        case NpcType.GolDragon:
                        case NpcType.GalGryphon:
                        case NpcType.OlgaFlow:
                        case NpcType.SaintMilion:
                        case NpcType.Shambertin:
                        case NpcType.Kondrieu:
                            darTable.set(npc, 1);
                            continue;
                    }

                    const ptIndex = npcTypeToPtIndex(npc);

                    if (ptIndex != null) {
                        darTable.set(npc, enemyDar[ptIndex] / 100);
                    }
                }

                cursor.seek_start(startPos + 0x1000);
            }
        }
    }

    table[Episode.IV] = [];

    for (const diff of Difficulties) {
        table[Episode.IV][diff] = [];

        for (const sid of SectionIds) {
            const darTable = new Map<NpcType, number>();

            table[Episode.IV][diff][sid] = {
                darTable
            };

            for (const npc of NpcTypes) {
                if (npc.episode !== Episode.IV) continue;

                switch (npc) {
                    case NpcType.SandRappy:
                        darTable.set(
                            npc,
                            table[Episode.I][diff][sid].darTable.get(NpcType.RagRappy)!
                        );
                        break;
                    case NpcType.DelRappy:
                        darTable.set(
                            npc,
                            table[Episode.I][diff][sid].darTable.get(NpcType.AlRappy)!
                        );
                        break;
                    case NpcType.Astark:
                        darTable.set(
                            npc,
                            table[Episode.I][diff][sid].darTable.get(NpcType.Hildebear)!
                        );
                        break;
                    case NpcType.SatelliteLizard:
                        darTable.set(
                            npc,
                            table[Episode.I][diff][sid].darTable.get(NpcType.SavageWolf)!
                        );
                        break;
                    case NpcType.Yowie:
                        darTable.set(
                            npc,
                            table[Episode.I][diff][sid].darTable.get(NpcType.BarbarousWolf)!
                        );
                        break;
                    case NpcType.MerissaA:
                        darTable.set(
                            npc,
                            table[Episode.I][diff][sid].darTable.get(NpcType.PofuillySlime)!
                        );
                        break;
                    case NpcType.MerissaAA:
                        darTable.set(
                            npc,
                            table[Episode.I][diff][sid].darTable.get(NpcType.PouillySlime)!
                        );
                        break;
                    case NpcType.Girtablulu:
                        darTable.set(
                            npc,
                            table[Episode.II][diff][sid].darTable.get(NpcType.Mericarol)!
                        );
                        break;
                    case NpcType.Zu:
                        darTable.set(
                            npc,
                            table[Episode.II][diff][sid].darTable.get(NpcType.GiGue)!
                        );
                        break;
                    case NpcType.Pazuzu:
                        darTable.set(
                            npc,
                            table[Episode.I][diff][sid].darTable.get(NpcType.Hildeblue)!
                        );
                        break;
                    case NpcType.Boota:
                        darTable.set(
                            npc,
                            table[Episode.I][diff][sid].darTable.get(NpcType.Booma)!
                        );
                        break;
                    case NpcType.ZeBoota:
                        darTable.set(
                            npc,
                            table[Episode.I][diff][sid].darTable.get(NpcType.Gobooma)!
                        );
                        break;
                    case NpcType.BaBoota:
                        darTable.set(
                            npc,
                            table[Episode.I][diff][sid].darTable.get(NpcType.Gigobooma)!
                        );
                        break;
                    case NpcType.Dorphon:
                        darTable.set(
                            npc,
                            table[Episode.II][diff][sid].darTable.get(NpcType.Delbiter)!
                        );
                        break;
                    case NpcType.DorphonEclair:
                        darTable.set(
                            npc,
                            table[Episode.I][diff][sid].darTable.get(NpcType.Hildeblue)!
                        );
                        break;
                    case NpcType.Goran:
                        darTable.set(
                            npc,
                            table[Episode.I][diff][sid].darTable.get(NpcType.Dimenian)!
                        );
                        break;
                    case NpcType.PyroGoran:
                        darTable.set(
                            npc,
                            table[Episode.I][diff][sid].darTable.get(NpcType.LaDimenian)!
                        );
                        break;
                    case NpcType.GoranDetonator:
                        darTable.set(
                            npc,
                            table[Episode.I][diff][sid].darTable.get(NpcType.SoDimenian)!
                        );
                        break;
                    case NpcType.SaintMilion:
                        darTable.set(
                            npc,
                            table[Episode.I][diff][sid].darTable.get(NpcType.DarkFalz)!
                        );
                        break;
                    case NpcType.Shambertin:
                        darTable.set(
                            npc,
                            table[Episode.I][diff][sid].darTable.get(NpcType.DarkFalz)!
                        );
                        break;
                    case NpcType.Kondrieu:
                        darTable.set(
                            npc,
                            table[Episode.I][diff][sid].darTable.get(NpcType.DarkFalz)!
                        );
                        break;
                }
            }
        }
    }

    logger.info('Done loading ItemPT.gsl.');
    return table;
}

function loadEnemyDrops(
    itemPt: ItemPt,
    difficulty: Difficulty,
    episode: Episode,
    sectionId: SectionId
): Array<EnemyDropDto> {
    const drops: Array<EnemyDropDto> = [];
    const dropsBuf = fs.readFileSync(
        `${RESOURCE_DIR}/login-config/drop/ep${episode}_mob_${difficulty}_${sectionId}.txt`
    );

    let lineNo = 0;
    let prevLine = '';

    for (const line of dropsBuf.toString('utf8').split('\n')) {
        const trimmed = line.trim();
        if (trimmed.startsWith('#')) continue;

        if (lineNo % 2 == 1) {
            let enemy = getEnemyType(episode, Math.floor(lineNo / 2));

            if (enemy) {
                const rareRate = expandDropRate(parseInt(prevLine, 10));
                const itemTypeId = parseInt(trimmed, 16);
                const dar = itemPt[episode][difficulty][sectionId].darTable.get(enemy);

                if (dar == null) {
                    logger.error(`No DAR found for ${enemy.name}.`);
                } else if (rareRate > 0 && itemTypeId) {
                    drops.push({
                        difficulty: Difficulty[difficulty],
                        episode: episode,
                        sectionId: SectionId[sectionId],
                        enemy: enemy.code,
                        itemTypeId,
                        dropRate: dar,
                        rareRate,
                    });
                }
            }
        }

        prevLine = trimmed;
        lineNo++;
    }

    return drops;
}

function loadBoxDrops(
    difficulty: Difficulty,
    episode: Episode,
    sectionId: SectionId
): Array<BoxDropDto> {
    const drops: Array<BoxDropDto> = [];
    const dropsBuf = fs.readFileSync(
        `${RESOURCE_DIR}/login-config/drop/ep${episode}_box_${difficulty}_${sectionId}.txt`
    );

    let lineNo = 0;
    let prevLine = '';
    let prevPrevLine = '';

    for (const line of dropsBuf.toString('utf8').split('\n')) {
        const trimmed = line.trim();
        if (trimmed.startsWith('#')) continue;

        if (lineNo % 3 == 2) {
            const areaId = parseInt(prevPrevLine, 10);
            const dropRate = expandDropRate(parseInt(prevLine, 10));
            const itemTypeId = parseInt(trimmed, 16);

            if (dropRate > 0 && itemTypeId) {
                drops.push({
                    difficulty: Difficulty[difficulty],
                    episode: episode,
                    sectionId: SectionId[sectionId],
                    areaId,
                    itemTypeId,
                    dropRate,
                });
            }
        }

        prevPrevLine = prevLine;
        prevLine = trimmed;
        lineNo++;
    }

    return drops;
}

function getStatBoosts(itemPmt: ItemPmt, statBoostIndex: number) {
    const statBoost = itemPmt.statBoosts[statBoostIndex];
    let atp = 0;
    let ata = 0;
    let minEvp = 0;
    let minDfp = 0;
    let mst = 0;
    let hp = 0;
    let lck = 0;

    switch (statBoost.stat1) {
        case 1: atp += statBoost.amount1; break;
        case 2: ata += statBoost.amount1; break;
        case 3: minEvp += statBoost.amount1; break;
        case 4: minDfp += statBoost.amount1; break;
        case 5: mst += statBoost.amount1; break;
        case 6: hp += statBoost.amount1; break;
        case 7: lck += statBoost.amount1; break;
        case 8:
            atp += statBoost.amount1;
            ata += statBoost.amount1;
            minEvp += statBoost.amount1;
            minDfp += statBoost.amount1;
            mst += statBoost.amount1;
            hp += statBoost.amount1;
            lck += statBoost.amount1;
            break;
        case 9: atp -= statBoost.amount1; break;
        case 10: ata -= statBoost.amount1; break;
        case 11: minEvp -= statBoost.amount1; break;
        case 12: minDfp -= statBoost.amount1; break;
        case 13: mst -= statBoost.amount1; break;
        case 14: hp -= statBoost.amount1; break;
        case 15: lck -= statBoost.amount1; break;
        case 16:
            atp -= statBoost.amount1;
            ata -= statBoost.amount1;
            minEvp -= statBoost.amount1;
            minDfp -= statBoost.amount1;
            mst -= statBoost.amount1;
            hp -= statBoost.amount1;
            lck -= statBoost.amount1;
            break;
    }

    return { atp, ata, minEvp, minDfp, mst, hp, lck };
}

function getEnemyType(episode: Episode, index: number) {
    if (episode === Episode.I) {
        return [
            undefined,

            NpcType.Hildebear,
            NpcType.Hildeblue,
            NpcType.Mothmant,
            NpcType.Monest,
            NpcType.RagRappy,
            NpcType.AlRappy,
            NpcType.SavageWolf,
            NpcType.BarbarousWolf,
            NpcType.Booma,
            NpcType.Gobooma,
            NpcType.Gigobooma,

            NpcType.GrassAssassin,
            NpcType.PoisonLily,
            NpcType.NarLily,
            NpcType.NanoDragon,
            NpcType.EvilShark,
            NpcType.PalShark,
            NpcType.GuilShark,
            NpcType.PofuillySlime,
            NpcType.PouillySlime,
            NpcType.PanArms,
            NpcType.Migium,
            NpcType.Hidoom,

            NpcType.Dubchic,
            NpcType.Garanz,
            NpcType.SinowBeat,
            NpcType.SinowGold,
            NpcType.Canadine,
            NpcType.Canane,
            NpcType.Delsaber,
            NpcType.ChaosSorcerer,
            undefined,
            undefined,
            NpcType.DarkGunner,
            NpcType.DeathGunner,
            NpcType.ChaosBringer,
            NpcType.DarkBelra,
            NpcType.Claw,
            NpcType.Bulk,
            NpcType.Bulclaw,
            NpcType.Dimenian,
            NpcType.LaDimenian,
            NpcType.SoDimenian,

            NpcType.Dragon,
            NpcType.DeRolLe,
            NpcType.VolOpt,
            NpcType.DarkFalz,

            undefined,
            undefined,

            NpcType.Gilchic
        ][index];
    } else if (episode === Episode.II) {
        return [
            undefined,

            NpcType.Hildebear2,
            NpcType.Hildeblue2,
            NpcType.Mothmant2,
            NpcType.Monest2,
            NpcType.RagRappy2,
            undefined,
            NpcType.SavageWolf2,
            NpcType.BarbarousWolf2,
            undefined,
            undefined,
            undefined,
            NpcType.GrassAssassin2,
            NpcType.PoisonLily2,
            NpcType.NarLily2,
            undefined,
            undefined,
            undefined,
            undefined,
            undefined,
            undefined,
            NpcType.PanArms2,
            NpcType.Migium2,
            NpcType.Hidoom2,
            NpcType.Dubchic2,
            NpcType.Garanz2,
            undefined,
            undefined,
            undefined,
            undefined,
            NpcType.Delsaber2,
            NpcType.ChaosSorcerer2,
            undefined,
            undefined,
            undefined,
            undefined,
            undefined,
            NpcType.DarkBelra2,
            undefined,
            undefined,
            undefined,
            NpcType.Dimenian2,
            NpcType.LaDimenian2,
            NpcType.SoDimenian2,
            undefined,
            undefined,
            undefined,
            undefined,
            undefined,
            undefined,
            NpcType.Gilchic2,
            NpcType.LoveRappy,

            NpcType.Merillia,
            NpcType.Meriltas,
            NpcType.Gee,
            NpcType.GiGue,
            NpcType.Mericarol,
            NpcType.Merikle,
            NpcType.Mericus,
            NpcType.UlGibbon,
            NpcType.ZolGibbon,
            NpcType.Gibbles,
            NpcType.SinowBerill,
            NpcType.SinowSpigell,

            NpcType.Dolmolm,
            NpcType.Dolmdarl,
            NpcType.Morfos,
            undefined,
            NpcType.Recon,
            NpcType.SinowZoa,
            NpcType.SinowZele,
            NpcType.Deldepth,
            NpcType.Delbiter,

            NpcType.BarbaRay,
            undefined,
            undefined,
            NpcType.GolDragon,
            NpcType.GalGryphon,
            NpcType.OlgaFlow,

            NpcType.StRappy,
            NpcType.HalloRappy,
            NpcType.EggRappy,

            NpcType.IllGill,
            NpcType.DelLily,
            NpcType.Epsilon,
        ][index];
    } else {
        return [
            undefined,

            NpcType.Astark,
            NpcType.Yowie,
            NpcType.SatelliteLizard,
            NpcType.MerissaA,
            NpcType.MerissaAA,
            NpcType.Girtablulu,
            NpcType.Zu,
            NpcType.Pazuzu,
            NpcType.Boota,
            NpcType.ZeBoota,
            NpcType.BaBoota,
            NpcType.Dorphon,
            NpcType.DorphonEclair,

            NpcType.Goran,
            NpcType.GoranDetonator,
            NpcType.PyroGoran,
            NpcType.SandRappy,
            NpcType.DelRappy,

            NpcType.SaintMilion,
            NpcType.Shambertin,
            NpcType.Kondrieu,
        ][index];
    }
}

function expandDropRate(pc: number): number {
    let shift = ((pc >> 3) & 0x1F) - 4;
    if (shift < 0) shift = 0;
    return ((2 << shift) * ((pc & 7) + 7)) / 4294967296;
}

function npcTypeToPtIndex(type: NpcType): number | undefined {
    switch (type) {
        // Episode I Forest

        case NpcType.Hildebear: return 1;
        case NpcType.Hildeblue: return 2;
        case NpcType.RagRappy: return 5;
        case NpcType.AlRappy: return 6;
        case NpcType.Monest: return 4;
        case NpcType.Mothmant: return 3;
        case NpcType.SavageWolf: return 7;
        case NpcType.BarbarousWolf: return 8;
        case NpcType.Booma: return 9;
        case NpcType.Gobooma: return 10;
        case NpcType.Gigobooma: return 11;
        case NpcType.Dragon: return 44;

        // Episode I Caves

        case NpcType.GrassAssassin: return 12;
        case NpcType.PoisonLily: return 13;
        case NpcType.NarLily: return 14;
        case NpcType.NanoDragon: return 15;
        case NpcType.EvilShark: return 16;
        case NpcType.PalShark: return 17;
        case NpcType.GuilShark: return 18;
        case NpcType.PofuillySlime: return 19;
        case NpcType.PouillySlime: return 20;
        case NpcType.PanArms: return 21;
        case NpcType.Migium: return 22;
        case NpcType.Hidoom: return 23;
        case NpcType.DeRolLe: return 45;

        // Episode I Mines

        case NpcType.Dubchic: return 24;
        case NpcType.Gilchic: return 50;
        case NpcType.Garanz: return 25;
        case NpcType.SinowBeat: return 26;
        case NpcType.SinowGold: return 27;
        case NpcType.Canadine: return 28;
        case NpcType.Canane: return 29;
        case NpcType.Dubswitch: return undefined;
        case NpcType.VolOpt: return 46;

        // Episode I Ruins

        case NpcType.Delsaber: return 30;
        case NpcType.ChaosSorcerer: return 31;
        case NpcType.DarkGunner: return 34;
        case NpcType.DeathGunner: return 35;
        case NpcType.ChaosBringer: return 36;
        case NpcType.DarkBelra: return 37;
        case NpcType.Dimenian: return 41;
        case NpcType.LaDimenian: return 42;
        case NpcType.SoDimenian: return 43;
        case NpcType.Bulclaw: return 40;
        case NpcType.Bulk: return 39;
        case NpcType.Claw: return 38;
        case NpcType.DarkFalz: return 47;

        // Episode II VR Temple

        case NpcType.Hildebear2: return 1;
        case NpcType.Hildeblue2: return 2;
        case NpcType.RagRappy2: return 5;
        case NpcType.LoveRappy: return 51;
        case NpcType.StRappy: return 79;
        case NpcType.HalloRappy: return 80;
        case NpcType.EggRappy: return 81;
        case NpcType.Monest2: return 4;
        case NpcType.Mothmant2: return 3;
        case NpcType.PoisonLily2: return 13;
        case NpcType.NarLily2: return 14;
        case NpcType.GrassAssassin2: return 12;
        case NpcType.Dimenian2: return 41;
        case NpcType.LaDimenian2: return 42;
        case NpcType.SoDimenian2: return 43;
        case NpcType.DarkBelra2: return 37;
        case NpcType.BarbaRay: return 73;

        // Episode II VR Spaceship

        case NpcType.SavageWolf2: return 7;
        case NpcType.BarbarousWolf2: return 8;
        case NpcType.PanArms2: return 21;
        case NpcType.Migium2: return 22;
        case NpcType.Hidoom2: return 23;
        case NpcType.Dubchic2: return 24;
        case NpcType.Gilchic2: return 50;
        case NpcType.Garanz2: return 25;
        case NpcType.Dubswitch2: return undefined;
        case NpcType.Delsaber2: return 30;
        case NpcType.ChaosSorcerer2: return 31;
        case NpcType.GolDragon: return 76;

        // Episode II Central Control Area

        case NpcType.SinowBerill: return 62;
        case NpcType.SinowSpigell: return 63;
        case NpcType.Merillia: return 52;
        case NpcType.Meriltas: return 53;
        case NpcType.Mericarol: return 56;
        case NpcType.Mericus: return 58;
        case NpcType.Merikle: return 57;
        case NpcType.UlGibbon: return 59;
        case NpcType.ZolGibbon: return 60;
        case NpcType.Gibbles: return 61;
        case NpcType.Gee: return 54;
        case NpcType.GiGue: return 55;
        case NpcType.IllGill: return 82;
        case NpcType.DelLily: return 83;
        case NpcType.Epsilon: return 84;
        case NpcType.GalGryphon: return 77;

        // Episode II Seabed

        case NpcType.Deldepth: return 71;
        case NpcType.Delbiter: return 72;
        case NpcType.Dolmolm: return 64;
        case NpcType.Dolmdarl: return 65;
        case NpcType.Morfos: return 66;
        case NpcType.Recobox: return 67;
        case NpcType.Recon: return 68;
        case NpcType.SinowZoa: return 69;
        case NpcType.SinowZele: return 70;
        case NpcType.OlgaFlow: return 78;

        // Episode IV

        case NpcType.SandRappy: return 17;
        case NpcType.DelRappy: return 18;
        case NpcType.Astark: return 1;
        case NpcType.SatelliteLizard: return 3;
        case NpcType.Yowie: return 2;
        case NpcType.MerissaA: return 4;
        case NpcType.MerissaAA: return 5;
        case NpcType.Girtablulu: return 6;
        case NpcType.Zu: return 7;
        case NpcType.Pazuzu: return 8;
        case NpcType.Boota: return 9;
        case NpcType.ZeBoota: return 10;
        case NpcType.BaBoota: return 11;
        case NpcType.Dorphon: return 12;
        case NpcType.DorphonEclair: return 13;
        case NpcType.Goran: return 14;
        case NpcType.PyroGoran: return 16;
        case NpcType.GoranDetonator: return 15;
        case NpcType.SaintMilion: return 19;
        case NpcType.Shambertin: return 20;
        case NpcType.Kondrieu: return 21;

        default: return undefined;
    }
}
