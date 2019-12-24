import { Server } from "../../core/model";
import { QuestDto } from "../dto/QuestDto";
import { NpcType } from "../../core/data_formats/parsing/quest/npc_types";
import { SimpleQuestModel } from "../model/SimpleQuestModel";
import { HuntMethodModel } from "../model/HuntMethodModel";
import { HuntMethodPersister } from "../persistence/HuntMethodPersister";
import { Duration } from "luxon";
import { ListProperty } from "../../core/observable/property/list/ListProperty";
import { list_property } from "../../core/observable";
import { GuiStore } from "../../core/stores/GuiStore";
import { HttpClient } from "../../core/HttpClient";
import { Store } from "../../core/stores/Store";
import { DisposableServerMap } from "../../core/stores/DisposableServerMap";
import { LogManager } from "../../core/Logger";

const logger = LogManager.get("hunt_optimizer/stores/HuntMethodStore");

const DEFAULT_DURATION = Duration.fromObject({ minutes: 30 });
const DEFAULT_GOVERNMENT_TEST_DURATION = Duration.fromObject({ minutes: 45 });
const DEFAULT_LARGE_ENEMY_COUNT_DURATION = Duration.fromObject({ minutes: 45 });

export function create_hunt_method_stores(
    http_client: HttpClient,
    gui_store: GuiStore,
    hunt_method_persister: HuntMethodPersister,
): DisposableServerMap<HuntMethodStore> {
    return new DisposableServerMap(gui_store, create_loader(http_client, hunt_method_persister));
}

export class HuntMethodStore extends Store {
    readonly methods: ListProperty<HuntMethodModel>;

    constructor(
        hunt_method_persister: HuntMethodPersister,
        server: Server,
        methods: HuntMethodModel[],
    ) {
        super();

        this.methods = list_property(method => [method.user_time], ...methods);

        this.disposables(
            this.methods.observe_list(() =>
                hunt_method_persister.persist_method_user_times(this.methods.val, server),
            ),
        );
    }
}

function create_loader(
    http_client: HttpClient,
    hunt_method_persister: HuntMethodPersister,
): (server: Server) => Promise<HuntMethodStore> {
    return async server => {
        const quests: QuestDto[] = await http_client
            .get(`/quests.${Server[server].toLowerCase()}.json`)
            .json();
        const methods: HuntMethodModel[] = [];

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

        await hunt_method_persister.load_method_user_times(methods, server);

        return new HuntMethodStore(hunt_method_persister, server, methods);
    };
}
