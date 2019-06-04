import { observable } from "mobx";
import { Difficulty, EnemyDrop, NpcType, SectionId, Server } from "../domain";
import { EnumMap } from "../enums";
import { Loadable } from "../Loadable";
import { itemStore } from "./ItemStore";
import { ServerMap } from "./ServerMap";

class EnemyDropTable {
    private map: EnumMap<Difficulty, EnumMap<SectionId, Map<NpcType, EnemyDrop>>> =
        new EnumMap(Difficulty, new EnumMap(SectionId, new Map()));

    getDrop(difficulty: Difficulty, sectionId: SectionId, npcType: NpcType): EnemyDrop | undefined {
        return this.map.get(difficulty).get(sectionId).get(npcType);
    }

    setDrop(difficulty: Difficulty, sectionId: SectionId, npcType: NpcType, drop: EnemyDrop) {
        this.map.get(difficulty).get(sectionId).set(npcType, drop);
    }
}

class ItemDropStore {
    @observable enemyDrops: ServerMap<Loadable<EnemyDropTable>> = new ServerMap(server =>
        new Loadable(new EnemyDropTable(), () => this.loadEnemyDrops(server))
    );

    private loadEnemyDrops = async (server: Server): Promise<EnemyDropTable> => {
        const response = await fetch(
            `${process.env.PUBLIC_URL}/enemy_drops.${Server[server].toLowerCase()}.tsv`
        );
        const data = await response.text();
        const lines = data.split('\n');
        const lineCount = lines.length;

        const drops = new EnemyDropTable();

        for (let i = 1; i < lineCount; i++) {
            const line = lines[i];
            const lineNo = i + 1;
            const cells = line.split('\t');
            const diffStr = cells[0].toLowerCase();

            const diff =
                diffStr === 'normal' ? Difficulty.Normal
                    : diffStr === 'hard' ? Difficulty.Hard
                        : diffStr === 'vhard' ? Difficulty.VHard
                            : diffStr === 'ultimate' ? Difficulty.Ultimate
                                : undefined;

            if (!diff) {
                console.error(`Couldn't parse difficulty for line ${lineNo}.`);
                continue;
            }

            const episode = parseInt(cells[1], 10);

            if (episode !== 1 && episode !== 2 && episode !== 4) {
                console.error(`Couldn't parse episode for line ${lineNo}.`);
                continue;
            }

            const sectionId: SectionId | undefined = (SectionId as any)[cells[2]];

            if (!sectionId) {
                console.error(`Couldn't parse section_id for line ${lineNo}.`);
                continue;
            }

            const enemyName = cells[3];

            const anythingRate = parseFloat(cells[5]);

            if (!isFinite(anythingRate)) {
                console.error(`Couldn't parse drop_rate for line ${lineNo}.`);
                continue;
            }

            const rareRate = parseFloat(cells[5]);

            if (!rareRate) {
                console.error(`Couldn't parse rare_rate for line ${lineNo}.`);
                continue;
            }

            const npcType = NpcType.byNameAndEpisode(enemyName, episode);

            if (!npcType) {
                console.error(`Couldn't determine enemy type for line ${lineNo}.`);
                continue;
            }

            drops.setDrop(diff, sectionId, npcType, new EnemyDrop(
                itemStore.dedupItem(cells[4]),
                anythingRate,
                rareRate
            ));
        }

        return drops;
    }
}

export const itemDropStore = new ItemDropStore();
