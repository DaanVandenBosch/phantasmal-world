import { observable } from "mobx";
import { Server } from "../domain";
import { undo_manager } from "../undo";

class ApplicationStore {
    @observable current_server: Server = Server.Ephinea;
    @observable current_tool: string = this.init_tool();

    private global_keyup_handlers = new Map<string, () => void>();

    on_global_keyup(tool: string, binding: string, handler: () => void): void {
        this.global_keyup_handlers.set(`${tool} ${binding}`, handler);
    }

    dispatch_global_keyup = (e: KeyboardEvent) => {
        const binding_parts: string[] = [];
        if (e.ctrlKey) binding_parts.push("Ctrl");
        if (e.shiftKey) binding_parts.push("Shift");
        if (e.altKey) binding_parts.push("Alt");
        binding_parts.push(e.key.toUpperCase());

        const binding = binding_parts.join("-");

        switch (binding) {
            case "Ctrl-Z":
                undo_manager.undo();
                break;
            case "Ctrl-Shift-Z":
                undo_manager.redo();
                break;
            default:
                {
                    const handler = this.global_keyup_handlers.get(binding);
                    if (handler) handler();
                }
                break;
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
