import { autorun, observable } from "mobx";
import { Server } from "../domain";

class ApplicationStore {
    @observable current_server: Server = Server.Ephinea;
    @observable current_tool: string = this.init_tool();

    private global_keyup_handlers = new Map<string, () => void>();

    constructor() {
        autorun(() => {
            window.location.hash = `#/${this.current_tool}`;
        });
    }

    on_global_keyup(tool: string, binding: string, handler: () => void): void {
        this.global_keyup_handlers.set(`${tool} -> ${binding}`, handler);
    }

    dispatch_global_keyup = (e: KeyboardEvent) => {
        const binding_parts: string[] = [];
        if (e.ctrlKey) binding_parts.push("Ctrl");
        if (e.shiftKey) binding_parts.push("Shift");
        if (e.altKey) binding_parts.push("Alt");
        binding_parts.push(e.key.toUpperCase());

        const binding = binding_parts.join("-");

        const handler = this.global_keyup_handlers.get(`${this.current_tool} -> ${binding}`);
        if (handler) handler();
    };

    private init_tool(): string {
        const tool = window.location.hash.slice(2);
        return tool.length ? tool : "viewer";
    }
}

export const application_store = new ApplicationStore();
