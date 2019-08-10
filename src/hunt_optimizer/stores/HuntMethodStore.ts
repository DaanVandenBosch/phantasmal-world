import Logger from "js-logger";
import { autorun, IReactionDisposer, observable } from "mobx";
import { Server } from "../../core/domain";
import { QuestDto } from "../../core/dto";
import { Loadable } from "../../core/Loadable";
import { hunt_method_persister } from "../persistence/HuntMethodPersister";
import { ServerMap } from "../../core/stores/ServerMap";
import { NpcType } from "../../core/data_formats/parsing/quest/npc_types";
import { HuntMethod, SimpleQuest } from "../domain";

const logger = Logger.get("stores/HuntMethodStore");

class HuntMethodStore {
    @observable methods: ServerMap<Loadable<HuntMethod[]>> = new ServerMap(
        server => new Loadable([], () => this.load_hunt_methods(server)),
    );

    private storage_disposer?: IReactionDisposer;

    private async load_hunt_methods(server: Server): Promise<HuntMethod[]> {
        const response = await fetch(
            `${process.env.PUBLIC_URL}/quests.${Server[server].toLowerCase()}.json`,
        );
        const quests = (await response.json()) as QuestDto[];
        const methods = new Array<HuntMethod>();

        for (const quest of quests) {
            let total_count = 0;
            const enemy_counts = new Map<NpcType, number>();

            for (const [code, count] of Object.entries(quest.enemyCounts)) {
                const npc_type = (NpcType as any)[code];

                if (!npc_type) {
                    logger.error(`No NpcType found for code ${code}.`);
                } else {
                    enemy_counts.set(npc_type, count);
                    total_count += count;
                }
            }

            // Filter out some quests.
            /* eslint-disable no-fallthrough */
            switch (quest.id) {
                // The following quests are left out because their enemies don't drop anything.
                case 31: // Black Paper's Dangerous Deal
                case 34: // Black Paper's Dangerous Deal 2
                case 1305: // Maximum Attack S (Ep. 1)
                case 1306: // Maximum Attack S (Ep. 2)
                case 1307: // Maximum Attack S (Ep. 4)
                case 313: // Beyond the Horizon

                // MAXIMUM ATTACK 3 Ver2 is filtered out because its actual enemy count depends on the path taken.
                // TODO: generate a method per path.
                case 314:
                    continue;
            }

            methods.push(
                new HuntMethod(
                    `q${quest.id}`,
                    quest.name,
                    new SimpleQuest(quest.id, quest.name, quest.episode, enemy_counts),
                    /^\d-\d.*/.test(quest.name) ? 0.75 : total_count > 400 ? 0.75 : 0.5,
                ),
            );
        }

        await this.load_user_times(methods, server);
        return methods;
    }

    private load_user_times = async (methods: HuntMethod[], server: Server) => {
        await hunt_method_persister.load_method_user_times(methods, server);

        if (this.storage_disposer) {
            this.storage_disposer();
        }

        this.storage_disposer = autorun(() => this.persist_user_times(methods, server));
    };

    private persist_user_times = (methods: HuntMethod[], server: Server) => {
        hunt_method_persister.persist_method_user_times(methods, server);
    };
}

export const hunt_method_store = new HuntMethodStore();
