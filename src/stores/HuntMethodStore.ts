import { observable } from "mobx";
import { HuntMethod, NpcType, Server, SimpleNpc, SimpleQuest } from "../domain";
import { Loadable } from "../Loadable";
import { ServerMap } from "./ServerMap";
import Logger from 'js-logger';

const logger = Logger.get('stores/HuntMethodStore');

class HuntMethodStore {
    @observable methods: ServerMap<Loadable<Array<HuntMethod>>> = new ServerMap(server =>
        new Loadable([], () => this.loadHuntMethods(server))
    );

    private async loadHuntMethods(server: Server): Promise<HuntMethod[]> {
        const response = await fetch(
            `${process.env.PUBLIC_URL}/quests.${Server[server].toLowerCase()}.tsv`
        );
        const data = await response.text();
        const rows = data.split('\n').map(line => line.split('\t'));

        const npcTypeByIndex = rows[0].slice(2, -2).map((episode, i) => {
            const enemy = rows[1][i + 2];
            return NpcType.byNameAndEpisode(enemy, parseInt(episode, 10))!;
        });

        return rows.slice(2).map((row, i) => {
            const questId = i + 1;
            const questName = row[0];
            const time = parseFloat(row[1]);

            const npcs = row.slice(2, -2).flatMap((cell, cellI) => {
                const amount = parseInt(cell, 10);
                const type = npcTypeByIndex[cellI];
                const enemies = [];

                if (type) {
                    for (let i = 0; i < amount; i++) {
                        enemies.push(new SimpleNpc(type));
                    }
                } else {
                    logger.error(`Couldn't get type for cellI ${cellI}.`);
                }

                return enemies;
            });

            return new HuntMethod(
                `q${questId}`,
                questName,
                new SimpleQuest(
                    questId,
                    questName,
                    npcs
                ),
                time
            );
        });
    }
}

export const huntMethodStore = new HuntMethodStore();
