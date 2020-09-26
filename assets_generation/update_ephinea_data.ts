import { readdirSync, readFileSync, statSync, writeFileSync } from "fs";
import { ASSETS_DIR, RESOURCE_DIR } from ".";
import { BufferCursor } from "../src/core/data_formats/block/cursor/BufferCursor";
import { ItemPmt, parse_item_pmt } from "../src/core/data_formats/parsing/itempmt";
import { parse_qst_to_quest } from "../src/core/data_formats/parsing/quest";
import { parse_unitxt, Unitxt } from "../src/core/data_formats/parsing/unitxt";
import { Difficulties, Difficulty, SectionId, SectionIds } from "../src/core/model";
import { update_drops_from_website } from "./update_drops_ephinea";
import { Episode, EPISODES } from "../src/core/data_formats/parsing/quest/Episode";
import { npc_data, NPC_TYPES, NpcType } from "../src/core/data_formats/parsing/quest/npc_types";
import { Endianness } from "../src/core/data_formats/block/Endianness";
import { ItemTypeDto } from "../src/core/dto/ItemTypeDto";
import { QuestDto } from "../src/hunt_optimizer/dto/QuestDto";
import { BoxDropDto, EnemyDropDto } from "../src/hunt_optimizer/dto/drops";
import { LogManager } from "../src/core/Logger";
import { Severity } from "../src/core/Severity";
import { unwrap } from "../src/core/Result";
import { get_npc_type } from "../src/core/data_formats/parsing/quest/QuestNpc";

const logger = LogManager.get("assets_generation/update_ephinea_data");

LogManager.default_severity = Severity.Error;
logger.severity = Severity.Info;
LogManager.get("static/update_drops_ephinea").severity = Severity.Info;
LogManager.get("core/data_formats/parsing/quest").severity = Severity.Off;
LogManager.get("core/data_formats/parsing/quest/bin").severity = Severity.Off;

/**
 * Used by static data generation scripts.
 */
const EPHINEA_RESOURCE_DIR = `${RESOURCE_DIR}/ephinea`;
/**
 * Enable this if we ever get the Ephinea ItemPT.gsl file.
 */
const USE_ITEMPT = false;

update().catch(e => logger.error(e));

/**
 * ItemPMT.bin and ItemPT.gsl comes from stock Tethealla. ItemPT.gsl is not used at the moment.
 * unitxt_j.prs comes from the Ephinea client.
 * TODO: manual fixes:
 *  - Clio is equipable by HUnewearls
 *  - Red Ring has a requirement of 180, not 108
 */
async function update(): Promise<void> {
    logger.info("Updating static Ephinea data.");

    const unitxt = load_unitxt();
    const item_names = unitxt[1];
    const items = update_items(item_names);

    if (USE_ITEMPT) {
        const item_pt = load_item_pt();
        update_drops(item_pt);
    } else {
        await update_drops_from_website(items);
    }

    update_quests();

    logger.info("Done updating static Ephinea data.");
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
 * Note: The MA4R quests use a random area variation per area from the ABC MA quests. E.g. MA4-1R will use a random caves 2 variation from MA4-1A, MA4-1B or MA4-1C. Same for mines 2 and ruins 2.
 */
function update_quests(): void {
    logger.info("Updating quest data.");

    const quests = new Array<QuestDto>();
    process_quest_dir(`${EPHINEA_RESOURCE_DIR}/ship-config/quest`, quests);

    quests.sort((a, b) => a.episode - b.episode || a.name.localeCompare(b.name));

    const id_counts = quests.reduce(
        (counts, q) => counts.set(q.id, (counts.get(q.id) || 0) + 1),
        new Map<number, number>(),
    );

    for (const [id, count] of id_counts.entries()) {
        if (count > 1) {
            logger.error(`Duplicate quest ID ${id}, NOT writing quests file.`);
            return;
        }
    }

    writeFileSync(`${ASSETS_DIR}/quests.ephinea.json`, JSON.stringify(quests, null, 4));

    logger.info("Done updating quest data.");
}

function process_quest_dir(path: string, quests: QuestDto[]): void {
    const stat = statSync(path);

    if (stat.isFile()) {
        process_quest(path, quests);
    } else if (stat.isDirectory()) {
        for (const file of readdirSync(path)) {
            process_quest_dir(`${path}/${file}`, quests);
        }
    }
}

function process_quest(path: string, quests: QuestDto[]): void {
    try {
        const buf = readFileSync(path);
        const q = parse_qst_to_quest(new BufferCursor(buf, Endianness.Little), true).value?.quest;

        if (q) {
            logger.trace(`Processing quest "${q.name}".`);

            if (q.id == null) {
                throw new Error("No id.");
            }

            const enemy_counts: { [npc_type_code: string]: number } = {};

            for (const npc of q.npcs) {
                const type = get_npc_type(npc);

                if (npc_data(type).enemy) {
                    enemy_counts[NpcType[type]] = (enemy_counts[NpcType[type]] || 0) + 1;
                }
            }

            quests.push({
                id: q.id,
                name: q.name,
                episode: q.episode,
                enemy_counts: enemy_counts,
            });
        } else {
            logger.error(`Couldn't process ${path}.`);
        }
    } catch (e) {
        logger.error(`Couldn't process ${path}.`, e);
    }
}

function load_unitxt(): Unitxt {
    logger.info("Loading unitxt_j.prs.");

    const buf = readFileSync(`${EPHINEA_RESOURCE_DIR}/client/data/unitxt_j.prs`);

    const unitxt = unwrap(parse_unitxt(new BufferCursor(buf, Endianness.Little)));
    // Strip custom Ephinea items until we have the Ephinea ItemPMT.bin.
    unitxt[1].splice(177, 50);
    unitxt[1].splice(639, 59);

    logger.info("Done loading unitxt_j.prs.");
    return unitxt;
}

function update_items(item_names: string[]): ItemTypeDto[] {
    logger.info("Updating item type data.");

    const buf = readFileSync(`${EPHINEA_RESOURCE_DIR}/ship-config/param/ItemPMT.bin`);

    const item_pmt = parse_item_pmt(new BufferCursor(buf, Endianness.Little));
    const item_types = new Array<ItemTypeDto>();
    const ids = new Set<number>();

    item_pmt.weapons.forEach((category, category_i) => {
        category.forEach((weapon, i) => {
            const id = (category_i << 8) + i;

            if (!ids.has(id)) {
                ids.add(id);
                item_types.push({
                    class: "weapon",
                    id,
                    name: item_names[weapon.id],
                    min_atp: weapon.min_atp,
                    max_atp: weapon.max_atp,
                    ata: weapon.ata,
                    max_grind: weapon.max_grind,
                    required_atp: weapon.req_atp,
                });
            }
        });
    });

    item_pmt.armors.forEach((armor, i) => {
        const id = 0x10100 + i;

        if (!ids.has(id)) {
            ids.add(id);

            const stats = get_stat_boosts(item_pmt, armor.stat_boost);
            stats.min_evp += armor.evp;
            stats.min_dfp += armor.dfp;

            item_types.push({
                class: "armor",
                id,
                name: item_names[armor.id],
                ...stats,
                max_evp: stats.min_evp + armor.evp_range,
                max_dfp: stats.min_dfp + armor.dfp_range,
            });
        }
    });

    item_pmt.shields.forEach((shield, i) => {
        const id = 0x10200 + i;

        if (!ids.has(id)) {
            ids.add(id);

            const stats = get_stat_boosts(item_pmt, shield.stat_boost);
            stats.min_evp += shield.evp;
            stats.min_dfp += shield.dfp;

            item_types.push({
                class: "shield",
                id,
                name: item_names[shield.id],
                ...stats,
                max_evp: stats.min_evp + shield.evp_range,
                max_dfp: stats.min_dfp + shield.dfp_range,
            });
        }
    });

    item_pmt.units.forEach((unit, i) => {
        const id = 0x10300 + i;

        if (!ids.has(id)) {
            ids.add(id);
            item_types.push({
                class: "unit",
                id,
                name: item_names[unit.id],
            });
        }
    });

    item_pmt.tools.forEach((category, category_i) => {
        category.forEach((tool, i) => {
            const id = (0x30000 | (category_i << 8)) + i;

            if (!ids.has(id)) {
                ids.add(id);
                item_types.push({
                    class: "tool",
                    id,
                    name: item_names[tool.id],
                });
            }
        });
    });

    writeFileSync(`${ASSETS_DIR}/item_types.ephinea.json`, JSON.stringify(item_types, null, 4));

    logger.info("Done updating item type data.");
    return item_types;
}

function update_drops(item_pt: ItemPt): void {
    logger.info("Updating drop data.");

    const enemy_drops = new Array<EnemyDropDto>();

    for (const diff of Difficulties) {
        for (const ep of EPISODES) {
            for (const sid of SectionIds) {
                enemy_drops.push(...load_enemy_drops(item_pt, diff, ep, sid));
            }
        }
    }

    writeFileSync(`${ASSETS_DIR}/enemy_drops.ephinea.json`, JSON.stringify(enemy_drops, null, 4));

    const box_drops = new Array<BoxDropDto>();

    for (const diff of Difficulties) {
        for (const ep of EPISODES) {
            for (const sid of SectionIds) {
                box_drops.push(...load_box_drops(diff, ep, sid));
            }
        }
    }

    writeFileSync(`${ASSETS_DIR}/box_drops.ephinea.json`, JSON.stringify(box_drops, null, 4));

    logger.info("Done updating drop data.");
}

type ItemP = {
    dar_table: Map<NpcType, number>;
};
type ItemPt = ItemP[][][];

function load_item_pt(): ItemPt {
    logger.info("Loading ItemPT.gsl.");

    const table: ItemPt = [];
    const buf = readFileSync(`${EPHINEA_RESOURCE_DIR}/ship-config/param/ItemPT.gsl`);
    const cursor = new BufferCursor(buf, Endianness.Big);

    cursor.seek(0x3000);

    // ItemPT.gsl was extracted from PSO for XBox, so it only contains data for ep. I and II.
    // Episode IV data is based on the ep. I and II data.
    for (const episode of [Episode.I, Episode.II]) {
        table[episode] = [];

        for (const diff of Difficulties) {
            table[episode][diff] = [];

            for (const sid of SectionIds) {
                const dar_table = new Map<NpcType, number>();

                table[episode][diff][sid] = {
                    dar_table,
                };

                const start_pos = cursor.position;
                cursor.seek(1608);
                const enemy_dar = cursor.u8_array(100);

                for (const npc of NPC_TYPES) {
                    if (npc_data(npc).episode !== episode) continue;

                    switch (npc) {
                        case NpcType.Dragon:
                        case NpcType.DeRolLe:
                        case NpcType.VolOptPart1:
                        case NpcType.DarkFalz:
                        case NpcType.BarbaRay:
                        case NpcType.GolDragon:
                        case NpcType.GalGryphon:
                        case NpcType.OlgaFlow:
                        case NpcType.SaintMilion:
                        case NpcType.Shambertin:
                        case NpcType.Kondrieu:
                            dar_table.set(npc, 1);
                            continue;
                    }

                    const pt_index = npc_type_to_pt_index(npc);

                    if (pt_index != null) {
                        dar_table.set(npc, enemy_dar[pt_index] / 100);
                    }
                }

                cursor.seek_start(start_pos + 0x1000);
            }
        }
    }

    table[Episode.IV] = [];

    for (const diff of Difficulties) {
        table[Episode.IV][diff] = [];

        for (const sid of SectionIds) {
            const dar_table = new Map<NpcType, number>();

            table[Episode.IV][diff][sid] = {
                dar_table,
            };

            for (const npc of NPC_TYPES) {
                if (npc_data(npc).episode !== Episode.IV) continue;

                switch (npc) {
                    case NpcType.SandRappy:
                        dar_table.set(
                            npc,
                            table[Episode.I][diff][sid].dar_table.get(NpcType.RagRappy)!,
                        );
                        break;
                    case NpcType.DelRappy:
                        dar_table.set(
                            npc,
                            table[Episode.I][diff][sid].dar_table.get(NpcType.AlRappy)!,
                        );
                        break;
                    case NpcType.Astark:
                        dar_table.set(
                            npc,
                            table[Episode.I][diff][sid].dar_table.get(NpcType.Hildebear)!,
                        );
                        break;
                    case NpcType.SatelliteLizard:
                        dar_table.set(
                            npc,
                            table[Episode.I][diff][sid].dar_table.get(NpcType.SavageWolf)!,
                        );
                        break;
                    case NpcType.Yowie:
                        dar_table.set(
                            npc,
                            table[Episode.I][diff][sid].dar_table.get(NpcType.BarbarousWolf)!,
                        );
                        break;
                    case NpcType.MerissaA:
                        dar_table.set(
                            npc,
                            table[Episode.I][diff][sid].dar_table.get(NpcType.PofuillySlime)!,
                        );
                        break;
                    case NpcType.MerissaAA:
                        dar_table.set(
                            npc,
                            table[Episode.I][diff][sid].dar_table.get(NpcType.PouillySlime)!,
                        );
                        break;
                    case NpcType.Girtablulu:
                        dar_table.set(
                            npc,
                            table[Episode.II][diff][sid].dar_table.get(NpcType.Mericarol)!,
                        );
                        break;
                    case NpcType.Zu:
                        dar_table.set(
                            npc,
                            table[Episode.II][diff][sid].dar_table.get(NpcType.GiGue)!,
                        );
                        break;
                    case NpcType.Pazuzu:
                        dar_table.set(
                            npc,
                            table[Episode.I][diff][sid].dar_table.get(NpcType.Hildeblue)!,
                        );
                        break;
                    case NpcType.Boota:
                        dar_table.set(
                            npc,
                            table[Episode.I][diff][sid].dar_table.get(NpcType.Booma)!,
                        );
                        break;
                    case NpcType.ZeBoota:
                        dar_table.set(
                            npc,
                            table[Episode.I][diff][sid].dar_table.get(NpcType.Gobooma)!,
                        );
                        break;
                    case NpcType.BaBoota:
                        dar_table.set(
                            npc,
                            table[Episode.I][diff][sid].dar_table.get(NpcType.Gigobooma)!,
                        );
                        break;
                    case NpcType.Dorphon:
                        dar_table.set(
                            npc,
                            table[Episode.II][diff][sid].dar_table.get(NpcType.Delbiter)!,
                        );
                        break;
                    case NpcType.DorphonEclair:
                        dar_table.set(
                            npc,
                            table[Episode.I][diff][sid].dar_table.get(NpcType.Hildeblue)!,
                        );
                        break;
                    case NpcType.Goran:
                        dar_table.set(
                            npc,
                            table[Episode.I][diff][sid].dar_table.get(NpcType.Dimenian)!,
                        );
                        break;
                    case NpcType.PyroGoran:
                        dar_table.set(
                            npc,
                            table[Episode.I][diff][sid].dar_table.get(NpcType.LaDimenian)!,
                        );
                        break;
                    case NpcType.GoranDetonator:
                        dar_table.set(
                            npc,
                            table[Episode.I][diff][sid].dar_table.get(NpcType.SoDimenian)!,
                        );
                        break;
                    case NpcType.SaintMilion:
                        dar_table.set(
                            npc,
                            table[Episode.I][diff][sid].dar_table.get(NpcType.DarkFalz)!,
                        );
                        break;
                    case NpcType.Shambertin:
                        dar_table.set(
                            npc,
                            table[Episode.I][diff][sid].dar_table.get(NpcType.DarkFalz)!,
                        );
                        break;
                    case NpcType.Kondrieu:
                        dar_table.set(
                            npc,
                            table[Episode.I][diff][sid].dar_table.get(NpcType.DarkFalz)!,
                        );
                        break;
                }
            }
        }
    }

    logger.info("Done loading ItemPT.gsl.");
    return table;
}

function load_enemy_drops(
    item_pt: ItemPt,
    difficulty: Difficulty,
    episode: Episode,
    section_id: SectionId,
): EnemyDropDto[] {
    const drops: EnemyDropDto[] = [];
    const drops_buf = readFileSync(
        `${EPHINEA_RESOURCE_DIR}/login-config/drop/ep${episode}_mob_${difficulty}_${section_id}.txt`,
    );

    let line_no = 0;
    let prev_line = "";

    for (const line of drops_buf.toString("utf8").split("\n")) {
        const trimmed = line.trim();
        if (trimmed.startsWith("#")) continue;

        if (line_no % 2 == 1) {
            const enemy = get_enemy_type(episode, Math.floor(line_no / 2));

            if (enemy) {
                const rare_rate = expand_drop_rate(parseInt(prev_line, 10));
                const item_type_id = parseInt(trimmed, 16);
                const dar = item_pt[episode][difficulty][section_id].dar_table.get(enemy);

                if (dar == null) {
                    logger.error(`No DAR found for ${NpcType[enemy]}.`);
                } else if (rare_rate > 0 && item_type_id) {
                    drops.push({
                        difficulty: Difficulty[difficulty],
                        episode,
                        section_id: SectionId[section_id],
                        enemy: NpcType[enemy],
                        item_type_id: item_type_id,
                        drop_rate: dar,
                        rare_rate: rare_rate,
                    });
                }
            }
        }

        prev_line = trimmed;
        line_no++;
    }

    return drops;
}

function load_box_drops(
    difficulty: Difficulty,
    episode: Episode,
    section_id: SectionId,
): BoxDropDto[] {
    const drops: BoxDropDto[] = [];
    const drops_buf = readFileSync(
        `${EPHINEA_RESOURCE_DIR}/login-config/drop/ep${episode}_box_${difficulty}_${section_id}.txt`,
    );

    let line_no = 0;
    let prev_line = "";
    let prev_prev_line = "";

    for (const line of drops_buf.toString("utf8").split("\n")) {
        const trimmed = line.trim();
        if (trimmed.startsWith("#")) continue;

        if (line_no % 3 == 2) {
            const area_id = parseInt(prev_prev_line, 10);
            const drop_rate = expand_drop_rate(parseInt(prev_line, 10));
            const item_type_id = parseInt(trimmed, 16);

            if (drop_rate > 0 && item_type_id) {
                drops.push({
                    difficulty: Difficulty[difficulty],
                    episode,
                    section_id: SectionId[section_id],
                    area_id: area_id,
                    item_type_id: item_type_id,
                    drop_rate: drop_rate,
                });
            }
        }

        prev_prev_line = prev_line;
        prev_line = trimmed;
        line_no++;
    }

    return drops;
}

function get_stat_boosts(
    item_pmt: ItemPmt,
    stat_boost_index: number,
): {
    atp: number;
    ata: number;
    min_evp: number;
    min_dfp: number;
    mst: number;
    hp: number;
    lck: number;
} {
    const stat_boost = item_pmt.stat_boosts[stat_boost_index];
    let atp = 0;
    let ata = 0;
    let min_evp = 0;
    let min_dfp = 0;
    let mst = 0;
    let hp = 0;
    let lck = 0;

    switch (stat_boost.stat_1) {
        case 1:
            atp += stat_boost.amount_1;
            break;
        case 2:
            ata += stat_boost.amount_1;
            break;
        case 3:
            min_evp += stat_boost.amount_1;
            break;
        case 4:
            min_dfp += stat_boost.amount_1;
            break;
        case 5:
            mst += stat_boost.amount_1;
            break;
        case 6:
            hp += stat_boost.amount_1;
            break;
        case 7:
            lck += stat_boost.amount_1;
            break;
        case 8:
            atp += stat_boost.amount_1;
            ata += stat_boost.amount_1;
            min_evp += stat_boost.amount_1;
            min_dfp += stat_boost.amount_1;
            mst += stat_boost.amount_1;
            hp += stat_boost.amount_1;
            lck += stat_boost.amount_1;
            break;
        case 9:
            atp -= stat_boost.amount_1;
            break;
        case 10:
            ata -= stat_boost.amount_1;
            break;
        case 11:
            min_evp -= stat_boost.amount_1;
            break;
        case 12:
            min_dfp -= stat_boost.amount_1;
            break;
        case 13:
            mst -= stat_boost.amount_1;
            break;
        case 14:
            hp -= stat_boost.amount_1;
            break;
        case 15:
            lck -= stat_boost.amount_1;
            break;
        case 16:
            atp -= stat_boost.amount_1;
            ata -= stat_boost.amount_1;
            min_evp -= stat_boost.amount_1;
            min_dfp -= stat_boost.amount_1;
            mst -= stat_boost.amount_1;
            hp -= stat_boost.amount_1;
            lck -= stat_boost.amount_1;
            break;
    }

    return { atp, ata, min_evp, min_dfp, mst, hp, lck };
}

function get_enemy_type(episode: Episode, index: number): NpcType | undefined {
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
            NpcType.VolOptPart1,
            NpcType.DarkFalz,

            undefined,
            undefined,

            NpcType.Gilchic,
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

function expand_drop_rate(pc: number): number {
    let shift = ((pc >> 3) & 0x1f) - 4;
    if (shift < 0) shift = 0;
    return ((2 << shift) * ((pc & 7) + 7)) / 4294967296;
}

function npc_type_to_pt_index(type: NpcType): number | undefined {
    switch (type) {
        // Episode I Forest

        case NpcType.Hildebear:
            return 1;
        case NpcType.Hildeblue:
            return 2;
        case NpcType.RagRappy:
            return 5;
        case NpcType.AlRappy:
            return 6;
        case NpcType.Monest:
            return 4;
        case NpcType.Mothmant:
            return 3;
        case NpcType.SavageWolf:
            return 7;
        case NpcType.BarbarousWolf:
            return 8;
        case NpcType.Booma:
            return 9;
        case NpcType.Gobooma:
            return 10;
        case NpcType.Gigobooma:
            return 11;
        case NpcType.Dragon:
            return 44;

        // Episode I Caves

        case NpcType.GrassAssassin:
            return 12;
        case NpcType.PoisonLily:
            return 13;
        case NpcType.NarLily:
            return 14;
        case NpcType.NanoDragon:
            return 15;
        case NpcType.EvilShark:
            return 16;
        case NpcType.PalShark:
            return 17;
        case NpcType.GuilShark:
            return 18;
        case NpcType.PofuillySlime:
            return 19;
        case NpcType.PouillySlime:
            return 20;
        case NpcType.PanArms:
            return 21;
        case NpcType.Migium:
            return 22;
        case NpcType.Hidoom:
            return 23;
        case NpcType.DeRolLe:
            return 45;

        // Episode I Mines

        case NpcType.Dubchic:
            return 24;
        case NpcType.Gilchic:
            return 50;
        case NpcType.Garanz:
            return 25;
        case NpcType.SinowBeat:
            return 26;
        case NpcType.SinowGold:
            return 27;
        case NpcType.Canadine:
            return 28;
        case NpcType.Canane:
            return 29;
        case NpcType.Dubswitch:
            return undefined;
        case NpcType.VolOptPart1:
            return 46;
        case NpcType.VolOptPart2:
            return undefined;

        // Episode I Ruins

        case NpcType.Delsaber:
            return 30;
        case NpcType.ChaosSorcerer:
            return 31;
        case NpcType.DarkGunner:
            return 34;
        case NpcType.DeathGunner:
            return 35;
        case NpcType.ChaosBringer:
            return 36;
        case NpcType.DarkBelra:
            return 37;
        case NpcType.Dimenian:
            return 41;
        case NpcType.LaDimenian:
            return 42;
        case NpcType.SoDimenian:
            return 43;
        case NpcType.Bulclaw:
            return 40;
        case NpcType.Bulk:
            return 39;
        case NpcType.Claw:
            return 38;
        case NpcType.DarkFalz:
            return 47;

        // Episode II VR Temple

        case NpcType.Hildebear2:
            return 1;
        case NpcType.Hildeblue2:
            return 2;
        case NpcType.RagRappy2:
            return 5;
        case NpcType.LoveRappy:
            return 51;
        case NpcType.StRappy:
            return 79;
        case NpcType.HalloRappy:
            return 80;
        case NpcType.EggRappy:
            return 81;
        case NpcType.Monest2:
            return 4;
        case NpcType.Mothmant2:
            return 3;
        case NpcType.PoisonLily2:
            return 13;
        case NpcType.NarLily2:
            return 14;
        case NpcType.GrassAssassin2:
            return 12;
        case NpcType.Dimenian2:
            return 41;
        case NpcType.LaDimenian2:
            return 42;
        case NpcType.SoDimenian2:
            return 43;
        case NpcType.DarkBelra2:
            return 37;
        case NpcType.BarbaRay:
            return 73;

        // Episode II VR Spaceship

        case NpcType.SavageWolf2:
            return 7;
        case NpcType.BarbarousWolf2:
            return 8;
        case NpcType.PanArms2:
            return 21;
        case NpcType.Migium2:
            return 22;
        case NpcType.Hidoom2:
            return 23;
        case NpcType.Dubchic2:
            return 24;
        case NpcType.Gilchic2:
            return 50;
        case NpcType.Garanz2:
            return 25;
        case NpcType.Dubswitch2:
            return undefined;
        case NpcType.Delsaber2:
            return 30;
        case NpcType.ChaosSorcerer2:
            return 31;
        case NpcType.GolDragon:
            return 76;

        // Episode II Central Control Area

        case NpcType.SinowBerill:
            return 62;
        case NpcType.SinowSpigell:
            return 63;
        case NpcType.Merillia:
            return 52;
        case NpcType.Meriltas:
            return 53;
        case NpcType.Mericarol:
            return 56;
        case NpcType.Mericus:
            return 58;
        case NpcType.Merikle:
            return 57;
        case NpcType.UlGibbon:
            return 59;
        case NpcType.ZolGibbon:
            return 60;
        case NpcType.Gibbles:
            return 61;
        case NpcType.Gee:
            return 54;
        case NpcType.GiGue:
            return 55;
        case NpcType.IllGill:
            return 82;
        case NpcType.DelLily:
            return 83;
        case NpcType.Epsilon:
            return 84;
        case NpcType.GalGryphon:
            return 77;

        // Episode II Seabed

        case NpcType.Deldepth:
            return 71;
        case NpcType.Delbiter:
            return 72;
        case NpcType.Dolmolm:
            return 64;
        case NpcType.Dolmdarl:
            return 65;
        case NpcType.Morfos:
            return 66;
        case NpcType.Recobox:
            return 67;
        case NpcType.Recon:
            return 68;
        case NpcType.SinowZoa:
            return 69;
        case NpcType.SinowZele:
            return 70;
        case NpcType.OlgaFlow:
            return 78;

        // Episode IV

        case NpcType.SandRappy:
            return 17;
        case NpcType.DelRappy:
            return 18;
        case NpcType.Astark:
            return 1;
        case NpcType.SatelliteLizard:
            return 3;
        case NpcType.Yowie:
            return 2;
        case NpcType.MerissaA:
            return 4;
        case NpcType.MerissaAA:
            return 5;
        case NpcType.Girtablulu:
            return 6;
        case NpcType.Zu:
            return 7;
        case NpcType.Pazuzu:
            return 8;
        case NpcType.Boota:
            return 9;
        case NpcType.ZeBoota:
            return 10;
        case NpcType.BaBoota:
            return 11;
        case NpcType.Dorphon:
            return 12;
        case NpcType.DorphonEclair:
            return 13;
        case NpcType.Goran:
            return 14;
        case NpcType.PyroGoran:
            return 16;
        case NpcType.GoranDetonator:
            return 15;
        case NpcType.SaintMilion:
            return 19;
        case NpcType.Shambertin:
            return 20;
        case NpcType.Kondrieu:
            return 21;

        default:
            return undefined;
    }
}
