import { observable } from "mobx";
import { Difficulties, Difficulty, EnemyDrop, NpcType, SectionId, SectionIds, Server } from "../domain";
import { NpcTypes } from "../domain/NpcType";
import { EnemyDropDto } from "../dto";
import { Loadable } from "../Loadable";
import { item_type_stores } from "./ItemTypeStore";
import { ServerMap } from "./ServerMap";
import Logger from 'js-logger';

const logger = Logger.get('stores/ItemDropStore');

export class EnemyDropTable {
    // Mapping of difficulties to section IDs to NpcTypes to EnemyDrops.
    private table: EnemyDrop[] =
        new Array(Difficulties.length * SectionIds.length * NpcTypes.length);

    // Mapping of ItemType ids to EnemyDrops.
    private item_type_to_drops: EnemyDrop[][] = [];

    get_drop(difficulty: Difficulty, section_id: SectionId, npc_type: NpcType): EnemyDrop | undefined {
        return this.table[
            difficulty * SectionIds.length * NpcTypes.length
            + section_id * NpcTypes.length
            + npc_type.id
        ];
    }

    set_drop(difficulty: Difficulty, section_id: SectionId, npc_type: NpcType, drop: EnemyDrop) {
        this.table[
            difficulty * SectionIds.length * NpcTypes.length
            + section_id * NpcTypes.length
            + npc_type.id
        ] = drop;

        let drops = this.item_type_to_drops[drop.item_type.id];

        if (!drops) {
            drops = [];
            this.item_type_to_drops[drop.item_type.id] = drops;
        }

        drops.push(drop);
    }

    get_drops_for_item_type(item_type_id: number): EnemyDrop[] {
        return this.item_type_to_drops[item_type_id] || [];
    }
}

export class ItemDropStore {
    @observable.ref enemy_drops: EnemyDropTable = new EnemyDropTable();
}

export const item_drop_stores: ServerMap<Loadable<ItemDropStore>> = new ServerMap(server => {
    const store = new ItemDropStore();
    return new Loadable(store, () => load(store, server));
});

async function load(store: ItemDropStore, server: Server): Promise<ItemDropStore> {
    const item_type_store = await item_type_stores.current.promise;
    const response = await fetch(
        `${process.env.PUBLIC_URL}/enemyDrops.${Server[server].toLowerCase()}.json`
    );
    const data: EnemyDropDto[] = await response.json();

    const drops = new EnemyDropTable();

    for (const drop_dto of data) {
        const npc_type = NpcType.by_code(drop_dto.enemy);

        if (!npc_type) {
            logger.warn(`Couldn't determine NpcType of episode ${drop_dto.episode} ${drop_dto.enemy}.`);
            continue;
        }

        const difficulty = (Difficulty as any)[drop_dto.difficulty];
        const item_type = item_type_store.get_by_id(drop_dto.itemTypeId);

        if (!item_type) {
            logger.warn(`Couldn't find item kind ${drop_dto.itemTypeId}.`);
            continue;
        }

        const section_id = (SectionId as any)[drop_dto.sectionId];

        if (section_id == null) {
            logger.warn(`Couldn't find section ID ${drop_dto.sectionId}.`);
            continue;
        }

        drops.set_drop(difficulty, section_id, npc_type, new EnemyDrop(
            difficulty,
            section_id,
            npc_type,
            item_type,
            drop_dto.dropRate,
            drop_dto.rareRate
        ));
    }

    store.enemy_drops = drops;
    return store;
}
