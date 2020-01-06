import { WritableProperty } from "../observable/property/WritableProperty";
import { Disposable } from "../observable/Disposable";
import { property } from "../observable";
import { Property } from "../observable/property/Property";
import { Server } from "../model";
import { Store } from "./Store";
import { disposable_listener } from "../gui/dom";
import { assert, map_get_or_put } from "../util";
import { Observable } from "../observable/Observable";

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
    private readonly _tool: WritableProperty<GuiTool> = property(GuiTool.Viewer);
    private readonly _path: WritableProperty<string> = property("");

    /**
     * Path prefixed with tool path.
     */
    private get full_path(): string {
        return `/${gui_tool_to_string(this.tool.val)}${this.path.val}`;
    }

    /**
     * Maps full paths to maps of parameters and their values. In other words we keep track of
     * parameter values per {@link full_path}.
     */
    private readonly parameters: Map<string, Map<string, string>> = new Map();
    private readonly _server: WritableProperty<Server> = property(Server.Ephinea);
    private readonly global_keydown_handlers = new Map<string, (e: KeyboardEvent) => void>();
    private readonly features: Set<string> = new Set();

    readonly tool: Property<GuiTool> = this._tool;
    readonly path: Property<string> = this._path;
    readonly server: Property<Server> = this._server;

    constructor() {
        super();

        const url = window.location.hash.slice(1);
        const [full_path, params_str] = url.split("?");
        const second_slash_idx = full_path.indexOf("/", 1);
        const tool_str =
            second_slash_idx === -1 ? full_path.slice(1) : full_path.slice(1, second_slash_idx);

        const tool = string_to_gui_tool(tool_str) ?? GuiTool.Viewer;
        const path = second_slash_idx === -1 ? "" : full_path.slice(second_slash_idx);

        if (params_str) {
            const params = new Map<string, string>();

            for (const [param, value] of params_str.split("&").map(p => p.split("=", 2))) {
                if (param === "features") {
                    for (const feature of value.split(",")) {
                        this.features.add(feature);
                    }
                } else {
                    params.set(param, value);
                }
            }

            this.parameters.set(full_path, params);
        }

        this.disposables(disposable_listener(window, "keydown", this.dispatch_global_keydown));

        this.set_tool(tool, path);
    }

    set_tool(tool: GuiTool, path: string = ""): void {
        this._path.val = path;
        this._tool.val = tool;
        this.update_location();
    }

    /**
     * Updates the path to `path_prefix` if the current path doesn't start with `path_prefix`.
     */
    set_path_prefix(path_prefix: string): void {
        if (!this.path.val.startsWith(path_prefix)) {
            this._path.val = path_prefix;
            this.update_location();
        }
    }

    get_parameter(tool: GuiTool, path: string, parameter: string): string | undefined {
        return map_get_or_put(
            this.parameters,
            `/${gui_tool_to_string(tool)}${path}`,
            () => new Map(),
        ).get(parameter);
    }

    bind_parameter(
        tool: GuiTool,
        path: string,
        parameter: string,
        observable: Observable<string | undefined>,
    ): Disposable {
        assert(
            parameter !== "features",
            "features can't be bound because it is a global parameter.",
        );

        const params: Map<string, string> = map_get_or_put(
            this.parameters,
            this.full_path,
            () => new Map(),
        );

        return observable.observe(({ value }) => {
            if (this.tool.val !== tool || this.path.val !== path) return;

            if (value === undefined) {
                params.delete(parameter);
            } else {
                params.set(parameter, value);
            }

            this.update_location();
        });
    }

    private update_location(): void {
        const params_array: [string, string][] = [];
        const params = this.parameters.get(this.full_path);

        if (params) {
            for (const [param, value] of params.entries()) {
                params_array.push([param, value]);
            }
        }

        if (this.features.size) {
            params_array.push(["features", [...this.features].join(",")]);
        }

        const param_str =
            params_array.length === 0 ? "" : "?" + params_array.map(kv => kv.join("=")).join("&");

        window.location.hash = `#${this.full_path}${param_str}`;
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
