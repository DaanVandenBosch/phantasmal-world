import Logger from 'js-logger';
import { observable } from "mobx";
import { HuntMethod, NpcType, Server, SimpleQuest } from "../domain";
import { QuestDto } from "../dto";
import { Loadable } from "../Loadable";
import { ServerMap } from "./ServerMap";

const logger = Logger.get('stores/HuntMethodStore');

class HuntMethodStore {
    @observable methods: ServerMap<Loadable<Array<HuntMethod>>> = new ServerMap(server =>
        new Loadable([], () => this.loadHuntMethods(server))
    );

    private async loadHuntMethods(server: Server): Promise<HuntMethod[]> {
        const response = await fetch(
            `${process.env.PUBLIC_URL}/quests.${Server[server].toLowerCase()}.json`
        );
        const quests = await response.json() as QuestDto[];

        return quests.map(quest => {
            let totalCount = 0;
            const enemyCounts = new Map<NpcType, number>();

            for (const [code, count] of Object.entries(quest.enemyCounts)) {
                const npcType = NpcType.byCode(code);

                if (!npcType) {
                    logger.error(`No NpcType found for code ${code}.`);
                } else {
                    enemyCounts.set(npcType, count);
                    totalCount += count;
                }
            }

            return new HuntMethod(
                `q${quest.id}`,
                quest.name,
                new SimpleQuest(
                    quest.id,
                    quest.name,
                    quest.episode,
                    enemyCounts
                ),
                /^\d-\d.*/.test(quest.name) ? 0.75 : (totalCount > 400 ? 0.75 : 0.5)
            );
        });
    }
}

export const huntMethodStore = new HuntMethodStore();
