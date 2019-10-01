import { WritableProperty } from "../observable/property/WritableProperty";
import { Disposable } from "../observable/Disposable";
import { property } from "../observable";
import { Property } from "../observable/property/Property";
import { Server } from "../model";

export enum GuiTool {
    Viewer,
    QuestEditor,
    HuntOptimizer,
}

const GUI_TOOL_TO_STRING = new Map([
    [GuiTool.Viewer, "viewer"],
    [GuiTool.QuestEditor, "quest_editor"],
    [GuiTool.HuntOptimizer, "hunt_optimizer"],
]);
const STRING_TO_GUI_TOOL = new Map([...GUI_TOOL_TO_STRING.entries()].map(([k, v]) => [v, k]));

class GuiStore implements Disposable {
    readonly tool: WritableProperty<GuiTool> = property(GuiTool.Viewer);
    readonly server: Property<Server>;

    private readonly _server: WritableProperty<Server> = property(Server.Ephinea);
    private readonly hash_disposer = this.tool.observe(({ value: tool }) => {
        window.location.hash = `#/${gui_tool_to_string(tool)}`;
    });
    private readonly global_keydown_handlers = new Map<string, (e: KeyboardEvent) => void>();

    constructor() {
        const tool = window.location.hash.slice(2);
        this.tool.val = string_to_gui_tool(tool) || GuiTool.Viewer;

        this.server = this._server;

        window.addEventListener("keydown", this.dispatch_global_keydown);
    }

    dispose(): void {
        this.hash_disposer.dispose();
        this.global_keydown_handlers.clear();

        window.removeEventListener("keydown", this.dispatch_global_keydown);
    }

    on_global_keydown(
        tool: GuiTool,
        binding: string,
        handler: (e: KeyboardEvent) => void,
    ): Disposable {
        const key = this.handler_key(tool, binding);
        this.global_keydown_handlers.set(key, handler);

        return {
            dispose: () => {
                this.global_keydown_handlers.delete(key);
            },
        };
    }

    private dispatch_global_keydown = (e: KeyboardEvent): void => {
        const binding_parts: string[] = [];
        if (e.ctrlKey) binding_parts.push("Ctrl");
        if (e.shiftKey) binding_parts.push("Shift");
        if (e.altKey) binding_parts.push("Alt");
        binding_parts.push(e.key.toUpperCase());

        const binding = binding_parts.join("-");

        const handler = this.global_keydown_handlers.get(this.handler_key(this.tool.val, binding));

        if (handler) {
            e.preventDefault();
            handler(e);
        }
    };

    private handler_key(tool: GuiTool, binding: string): string {
        return `${(GuiTool as any)[tool]} -> ${binding}`;
    }
}

export const gui_store = new GuiStore();

function string_to_gui_tool(tool: string): GuiTool | undefined {
    return STRING_TO_GUI_TOOL.get(tool);
}

function gui_tool_to_string(tool: GuiTool): string {
    const str = GUI_TOOL_TO_STRING.get(tool);

    if (str) {
        return str;
    } else {
        throw new Error(`To string not implemented for ${(GuiTool as any)[tool]}.`);
    }
}
