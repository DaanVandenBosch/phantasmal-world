import Logger from "js-logger";
import { ServerMap } from "../../core/stores/ServerMap";
import { LoadableProperty } from "../../core/observable/property/LoadableProperty";
import { Disposable } from "../../core/observable/Disposable";
import { Disposer } from "../../core/observable/Disposer";
import { ServerModel } from "../../core/model";
import { QuestDto } from "../dto/QuestDto";
import { NpcType } from "../../core/data_formats/parsing/quest/npc_types";
import { SimpleQuestModel } from "../model/SimpleQuestModel";
import { HuntMethodModel } from "../model/HuntMethodModel";
import { hunt_method_persister } from "../persistence/HuntMethodPersister";
import { Duration } from "luxon";

const logger = Logger.get("hunt_optimizer/stores/HuntMethodStore");

const DEFAULT_DURATION = Duration.fromObject({ minutes: 30 });
const DEFAULT_GOVERNMENT_TEST_DURATION = Duration.fromObject({ minutes: 45 });
const DEFAULT_LARGE_ENEMY_COUNT_DURATION = Duration.fromObject({ minutes: 45 });

class HuntMethodStore implements Disposable {
    readonly methods: LoadableProperty<HuntMethodModel[]>;

    private readonly disposer = new Disposer();
    private readonly server: ServerModel;

    constructor(server: ServerModel) {
        this.methods = new LoadableProperty([], () => this.load_hunt_methods(server));
        this.server = server;
    }

    dispose(): void {
        this.disposer.dispose();
    }

    private async load_hunt_methods(server: ServerModel): Promise<HuntMethodModel[]> {
        const response = await fetch(
            `${process.env.PUBLIC_URL}/quests.${ServerModel[server].toLowerCase()}.json`,
        );
        const quests = (await response.json()) as QuestDto[];
        const methods = new Array<HuntMethodModel>();

        for (const quest of quests) {
            let total_enemy_count = 0;
            const enemy_counts = new Map<NpcType, number>();

            for (const [code, count] of Object.entries(quest.enemyCounts)) {
                const npc_type = (NpcType as any)[code];

                if (!npc_type) {
                    logger.error(`No NpcType found for code ${code}.`);
                } else {
                    enemy_counts.set(npc_type, count);
                    total_enemy_count += count;
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
                new HuntMethodModel(
                    `q${quest.id}`,
                    quest.name,
                    new SimpleQuestModel(quest.id, quest.name, quest.episode, enemy_counts),
                    /^\d-\d.*/.test(quest.name)
                        ? DEFAULT_GOVERNMENT_TEST_DURATION
                        : total_enemy_count > 400
                        ? DEFAULT_LARGE_ENEMY_COUNT_DURATION
                        : DEFAULT_DURATION,
                ),
            );
        }

        await this.load_user_times(methods, server);
        return methods;
    }

    private load_user_times = async (methods: HuntMethodModel[], server: ServerModel) => {
        await hunt_method_persister.load_method_user_times(methods, server);

        this.disposer.dispose_all();

        this.disposer.add_all(
            ...methods.map(method => method.user_time.observe(this.persist_user_times)),
        );
    };

    private persist_user_times = () => {
        hunt_method_persister.persist_method_user_times(this.methods.val, this.server);
    };
}

export const hunt_method_stores = new ServerMap(server => new HuntMethodStore(server));
