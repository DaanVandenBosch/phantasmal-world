import Logger from "js-logger";
import { Server } from "./domain";

const logger = Logger.get("core/persistence/Persister");

export abstract class Persister {
    protected persist_for_server(server: Server, key: string, data: any): void {
        this.persist(key + "." + Server[server], data);
    }

    protected persist(key: string, data: any): void {
        try {
            localStorage.setItem(key, JSON.stringify(data));
        } catch (e) {
            logger.error(`Couldn't persist ${key}.`, e);
        }
    }

    protected async load_for_server<T>(server: Server, key: string): Promise<T | undefined> {
        return this.load(key + "." + Server[server]);
    }

    protected async load<T>(key: string): Promise<T | undefined> {
        try {
            const json = localStorage.getItem(key);
            return json && JSON.parse(json);
        } catch (e) {
            logger.error(`Couldn't load ${key}.`, e);
            return undefined;
        }
    }
}
