import { Persister } from "./Persister";
import { Server, HuntMethod } from "../domain";

const METHOD_USER_TIMES_KEY = "HuntMethodStore.methodUserTimes";

class HuntMethodPersister extends Persister {
    persist_method_user_times(hunt_methods: HuntMethod[], server: Server): void {
        const user_times: PersistedUserTimes = {};

        for (const method of hunt_methods) {
            if (method.user_time != undefined) {
                user_times[method.id] = method.user_time;
            }
        }

        this.persist_for_server(server, METHOD_USER_TIMES_KEY, user_times);
    }

    async load_method_user_times(
        hunt_methods: HuntMethod[],
        server: Server
    ): Promise<HuntMethod[]> {
        const user_times = await this.load_for_server<PersistedUserTimes>(
            server,
            METHOD_USER_TIMES_KEY
        );

        if (user_times) {
            for (const method of hunt_methods) {
                method.user_time = user_times[method.id];
            }
        }

        return hunt_methods;
    }
}

type PersistedUserTimes = { [method_id: string]: number };

export const hunt_method_persister = new HuntMethodPersister();
