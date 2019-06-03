import { observable } from "mobx";
import { HuntMethod, NpcType, Server, SimpleNpc, SimpleQuest } from "../domain";
import { Loadable } from "../Loadable";
import { PerServer } from "./PerServer";

class HuntMethodStore {
    @observable methods: PerServer<Loadable<Array<HuntMethod>>> = new PerServer(server =>
        new Loadable([], () => this.loadHuntMethods(server))
    );

    private async loadHuntMethods(server: Server): Promise<HuntMethod[]> {
        const response = await fetch(process.env.PUBLIC_URL + `/quests.${Server[server]}.tsv`);
        const data = await response.text();
        const rows = data.split('\n').map(line => line.split('\t'));

        const npcTypeByIndex = rows[0].slice(2, -2).map((episode, i) => {
            const enemy = rows[1][i + 2];
            return NpcType.bySimpleNameAndEpisode(enemy, parseInt(episode, 10))!;
        });

        return rows.slice(2).map(row => {
            const questName = row[0];

            const npcs = row.slice(2, -2).flatMap((cell, cellI) => {
                const amount = parseInt(cell, 10);
                const type = npcTypeByIndex[cellI];
                const enemies = [];

                for (let i = 0; i < amount; i++) {
                    enemies.push(new SimpleNpc(type));
                }

                return enemies;
            });

            return new HuntMethod(
                new SimpleQuest(
                    questName,
                    npcs
                )
            );
        });
    }
}

export const huntMethodStore = new HuntMethodStore();
