import Logger from 'js-logger';
import { autorun, IReactionDisposer, observable } from "mobx";
import { HuntMethod, NpcType, Server, SimpleQuest } from "../domain";
import { QuestDto } from "../dto";
import { Loadable } from "../Loadable";
import { ServerMap } from "./ServerMap";

const logger = Logger.get('stores/HuntMethodStore');

class HuntMethodStore {
    @observable methods: ServerMap<Loadable<Array<HuntMethod>>> = new ServerMap(server =>
        new Loadable([], () => this.loadHuntMethods(server))
    );

    private storageDisposer?: IReactionDisposer;

    private async loadHuntMethods(server: Server): Promise<HuntMethod[]> {
        const response = await fetch(
            `${process.env.PUBLIC_URL}/quests.${Server[server].toLowerCase()}.json`
        );
        const quests = await response.json() as QuestDto[];
        const methods = new Array<HuntMethod>();

        for (const quest of quests) {
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

            // Filter out some quests.
            /* eslint-disable no-fallthrough */
            switch (quest.id) {
                // The following quests are left out because their enemies don't drop anything.
                case 31:   // Black Paper's Dangerous Deal
                case 34:   // Black Paper's Dangerous Deal 2
                case 1305: // Maximum Attack S (Ep. 1)
                case 1306: // Maximum Attack S (Ep. 2)
                case 1307: // Maximum Attack S (Ep. 4)
                case 313:  // Beyond the Horizon

                // MAXIMUM ATTACK 3 Ver2 is filtered out because its actual enemy count depends on the path taken.
                // TODO: generate a method per path.
                case 314:
                    continue;
            }

            methods.push(
                new HuntMethod(
                    `q${quest.id}`,
                    quest.name,
                    new SimpleQuest(
                        quest.id,
                        quest.name,
                        quest.episode,
                        enemyCounts
                    ),
                    /^\d-\d.*/.test(quest.name) ? 0.75 : (totalCount > 400 ? 0.75 : 0.5)
                )
            );
        }

        this.loadFromLocalStorage(methods, server);
        return methods;
    }

    private loadFromLocalStorage = (methods: HuntMethod[], server: Server) => {
        try {
            const methodUserTimesJson = localStorage.getItem(
                `HuntMethodStore.methodUserTimes.${Server[server]}`
            );

            if (methodUserTimesJson) {
                const userTimes = JSON.parse(methodUserTimesJson);

                for (const method of methods) {
                    method.userTime = userTimes[method.id] as number;
                }
            }

            if (this.storageDisposer) {
                this.storageDisposer();
            }

            this.storageDisposer = autorun(() =>
                this.storeInLocalStorage(methods, server)
            );
        } catch (e) {
            logger.error(e);
        }
    }

    private storeInLocalStorage = (methods: HuntMethod[], server: Server) => {
        try {
            const userTimes: any = {};

            for (const method of methods) {
                if (method.userTime != null) {
                    userTimes[method.id] = method.userTime;
                }
            }

            localStorage.setItem(
                `HuntMethodStore.methodUserTimes.${Server[server]}`,
                JSON.stringify(userTimes)
            );
        } catch (e) {
            logger.error(e);
        }
    }
}

export const huntMethodStore = new HuntMethodStore();
