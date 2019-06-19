import { observable } from "mobx";
import { Difficulties, Difficulty, EnemyDrop, NpcType, SectionId, SectionIds, Server, ItemKind } from "../domain";
import { NpcTypes } from "../domain/NpcType";
import { EnemyDropDto } from "../dto";
import { Loadable } from "../Loadable";
import { itemKindStores } from "./ItemKindStore";
import { ServerMap } from "./ServerMap";

class EnemyDropTable {
    // Mapping of difficulties to section IDs to NpcTypes to EnemyDrops.
    private table: Array<EnemyDrop> =
        new Array(Difficulties.length * SectionIds.length * NpcTypes.length);

    // Mapping of ItemKind ids to EnemyDrops.
    private itemKindToDrops: Array<Array<EnemyDrop>> = new Array();

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

        let drops = this.itemKindToDrops[drop.item.id];

        if (!drops) {
            drops = [];
            this.itemKindToDrops[drop.item.id] = drops;
        }

        drops.push(drop);
    }

    getDropsForItemKind(itemKindId: number): Array<EnemyDrop> {
        return this.itemKindToDrops[itemKindId] || [];
    }
}

class ItemDropStore {
    @observable enemyDrops: EnemyDropTable = new EnemyDropTable();

    load = async (server: Server): Promise<ItemDropStore> => {
        const itemKindStore = await itemKindStores.current.promise;
        const response = await fetch(
            `${process.env.PUBLIC_URL}/enemyDrops.${Server[server].toLowerCase()}.json`
        );
        const data: Array<EnemyDropDto> = await response.json();

        const drops = new EnemyDropTable();

        for (const dropDto of data) {
            const npcType = NpcType.byCode(dropDto.enemy);

            if (!npcType) {
                console.warn(`Couldn't determine NpcType of episode ${dropDto.episode} ${dropDto.enemy}.`);
                continue;
            }

            const difficulty = (Difficulty as any)[dropDto.difficulty];
            const itemKind = itemKindStore.getById(dropDto.itemKindId);

            if (!itemKind) {
                console.warn(`Couldn't find item kind ${dropDto.itemKindId}.`);
                continue;
            }

            const sectionId = (SectionId as any)[dropDto.sectionId];

            if (sectionId == null) {
                console.warn(`Couldn't find section ID ${dropDto.sectionId}.`);
                continue;
            }

            drops.setDrop(difficulty, sectionId, npcType, new EnemyDrop(
                difficulty,
                sectionId,
                npcType,
                itemKind,
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
