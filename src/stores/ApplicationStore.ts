import { observable } from "mobx";
import { Server } from "../domain";

class ApplicationStore {
    @observable current_server: Server = Server.Ephinea;
    @observable current_tool: string = this.init_tool();

    private key_event_handlers = new Map<string, (e: KeyboardEvent) => void>();

    on_global_keyup = (tool: string, handler: (e: KeyboardEvent) => void) => {
        this.key_event_handlers.set(tool, handler);
    };

    dispatch_global_keyup = (e: KeyboardEvent) => {
        const handler = this.key_event_handlers.get(this.current_tool);

        if (handler) {
            handler(e);
        }
    };

    private init_tool(): string {
        const param = window.location.search
            .slice(1)
            .split("&")
            .find(p => p.startsWith("tool="));
        return param ? param.slice(5) : "viewer";
    }
}

export const application_store = new ApplicationStore();
