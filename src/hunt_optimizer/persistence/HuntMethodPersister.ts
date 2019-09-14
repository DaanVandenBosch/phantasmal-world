import { Persister } from "../../core/persistence";
import { Server } from "../../core/model";
import { HuntMethodModel } from "../model/HuntMethodModel";
import { Duration } from "luxon";

const METHOD_USER_TIMES_KEY = "HuntMethodStore.methodUserTimes";

class HuntMethodPersister extends Persister {
    persist_method_user_times(hunt_methods: HuntMethodModel[], server: Server): void {
        const user_times: PersistedUserTimes = {};

        for (const method of hunt_methods) {
            if (method.user_time.val != undefined) {
                user_times[method.id] = method.user_time.val.as("hours");
            }
        }

        this.persist_for_server(server, METHOD_USER_TIMES_KEY, user_times);
    }

    async load_method_user_times(hunt_methods: HuntMethodModel[], server: Server): Promise<void> {
        const user_times = await this.load_for_server<PersistedUserTimes>(
            server,
            METHOD_USER_TIMES_KEY,
        );

        if (user_times) {
            for (const method of hunt_methods) {
                const hours = user_times[method.id];
                method.set_user_time(
                    hours == undefined ? undefined : Duration.fromObject({ hours }),
                );
            }
        }
    }
}

type PersistedUserTimes = { [method_id: string]: number };

export const hunt_method_persister = new HuntMethodPersister();
