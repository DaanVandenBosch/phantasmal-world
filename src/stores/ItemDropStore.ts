import { observable } from "mobx";
import { Difficulty, EnemyDrop, NpcType, SectionId, Server } from "../domain";
import { EnumMap } from "../enums";
import { Loadable } from "../Loadable";
import { itemStore } from "./ItemStore";
import { ServerMap } from "./ServerMap";
import { EnemyDropDto } from "../dto";

class EnemyDropTable {
    private map: EnumMap<Difficulty, EnumMap<SectionId, Map<NpcType, EnemyDrop>>> =
        new EnumMap(Difficulty, () => new EnumMap(SectionId, () => new Map()));

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
            `${process.env.PUBLIC_URL}/enemyDrops.${Server[server].toLowerCase()}.json`
        );
        const data: Array<EnemyDropDto> = await response.json();

        const drops = new EnemyDropTable();

        for (const dropDto of data) {
            const npcType = NpcType.byNameAndEpisode(dropDto.enemy, dropDto.episode);

            if (!npcType) {
                console.error(`Couldn't determine NpcType of episode ${dropDto.episode} ${dropDto.enemy}.`);
                continue;
            }

            drops.setDrop(dropDto.difficulty, dropDto.sectionId, npcType, new EnemyDrop(
                itemStore.dedupItem(dropDto.item),
                dropDto.dropRate,
                dropDto.rareRate
            ));
        }

        return drops;
    }
}

export const itemDropStore = new ItemDropStore();
