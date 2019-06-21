import { observable } from "mobx";
import { Difficulties, Difficulty, EnemyDrop, NpcType, SectionId, SectionIds, Server } from "../domain";
import { NpcTypes } from "../domain/NpcType";
import { EnemyDropDto } from "../dto";
import { Loadable } from "../Loadable";
import { itemTypeStores } from "./ItemTypeStore";
import { ServerMap } from "./ServerMap";
import Logger from 'js-logger';

const logger = Logger.get('stores/ItemDropStore');

class EnemyDropTable {
    // Mapping of difficulties to section IDs to NpcTypes to EnemyDrops.
    private table: Array<EnemyDrop> =
        new Array(Difficulties.length * SectionIds.length * NpcTypes.length);

    // Mapping of ItemType ids to EnemyDrops.
    private itemTypeToDrops: Array<Array<EnemyDrop>> = [];

    getDrop(difficulty: Difficulty, sectionId: SectionId, npcType: NpcType): EnemyDrop | undefined {
        return this.table[
            difficulty * SectionIds.length * NpcTypes.length
            + sectionId * NpcTypes.length
            + npcType.id
        ];
    }

    setDrop(difficulty: Difficulty, sectionId: SectionId, npcType: NpcType, drop: EnemyDrop) {
        this.table[
            difficulty * SectionIds.length * NpcTypes.length
            + sectionId * NpcTypes.length
            + npcType.id
        ] = drop;

        let drops = this.itemTypeToDrops[drop.itemType.id];

        if (!drops) {
            drops = [];
            this.itemTypeToDrops[drop.itemType.id] = drops;
        }

        drops.push(drop);
    }

    getDropsForItemType(itemTypeId: number): Array<EnemyDrop> {
        return this.itemTypeToDrops[itemTypeId] || [];
    }
}

class ItemDropStore {
    @observable enemyDrops: EnemyDropTable = new EnemyDropTable();

    load = async (server: Server): Promise<ItemDropStore> => {
        const itemTypeStore = await itemTypeStores.current.promise;
        const response = await fetch(
            `${process.env.PUBLIC_URL}/enemyDrops.${Server[server].toLowerCase()}.json`
        );
        const data: Array<EnemyDropDto> = await response.json();

        const drops = new EnemyDropTable();

        for (const dropDto of data) {
            const npcType = NpcType.byCode(dropDto.enemy);

            if (!npcType) {
                logger.warn(`Couldn't determine NpcType of episode ${dropDto.episode} ${dropDto.enemy}.`);
                continue;
            }

            const difficulty = (Difficulty as any)[dropDto.difficulty];
            const itemType = itemTypeStore.getById(dropDto.itemTypeId);

            if (!itemType) {
                logger.warn(`Couldn't find item kind ${dropDto.itemTypeId}.`);
                continue;
            }

            const sectionId = (SectionId as any)[dropDto.sectionId];

            if (sectionId == null) {
                logger.warn(`Couldn't find section ID ${dropDto.sectionId}.`);
                continue;
            }

            drops.setDrop(difficulty, sectionId, npcType, new EnemyDrop(
                difficulty,
                sectionId,
                npcType,
                itemType,
                dropDto.dropRate,
                dropDto.rareRate
            ));
        }

        this.enemyDrops = drops;
        return this;
    }
}

export const itemDropStores: ServerMap<Loadable<ItemDropStore>> = new ServerMap(server => {
    const store = new ItemDropStore();
    return new Loadable(store, () => store.load(server));
});
