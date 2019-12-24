import { WritableProperty } from "../observable/property/WritableProperty";
import { Disposable } from "../observable/Disposable";
import { property } from "../observable";
import { Property } from "../observable/property/Property";
import { Server } from "../model";
import { Store } from "./Store";
import { disposable_listener } from "../gui/dom";

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

export class GuiStore extends Store {
    private readonly _server: WritableProperty<Server> = property(Server.Ephinea);
    private readonly global_keydown_handlers = new Map<string, (e: KeyboardEvent) => void>();
    private readonly features: Set<string> = new Set();

    readonly tool: WritableProperty<GuiTool> = property(GuiTool.Viewer);
    readonly server: Property<Server> = this._server;

    constructor() {
        super();

        const url = window.location.hash.slice(2);
        const [tool_str, params_str] = url.split("?");

        if (params_str) {
            const features = params_str
                .split("&")
                .map(p => p.split("="))
                .find(([key]) => key === "features");

            if (features && features.length >= 2) {
                for (const feature of features[1].split(",")) {
                    this.features.add(feature);
                }
            }
        }

        this.disposables(
            this.tool.observe(({ value: tool }) => {
                let hash = `#/${gui_tool_to_string(tool)}`;

                if (this.features.size) {
                    hash += "?features=" + [...this.features].join(",");
                }

                window.location.hash = hash;
            }),

            disposable_listener(window, "keydown", this.dispatch_global_keydown),
        );

        this.tool.val = string_to_gui_tool(tool_str) || GuiTool.Viewer;
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

    feature_active(feature: string): boolean {
        return this.features.has(feature);
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
