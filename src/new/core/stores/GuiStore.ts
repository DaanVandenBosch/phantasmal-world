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

class GuiStore {
    tool_prop = new Observable<GuiTool>(GuiTool.Viewer);

    get tool(): GuiTool {
        return this.tool_prop.get();
    }

    set tool(tool: GuiTool) {
        window.location.hash = `#/${gui_tool_to_string(tool)}`;
        this.tool_prop.set(tool);
    }

    constructor() {
        const tool = window.location.hash.slice(2);
        this.tool = string_to_gui_tool(tool) || GuiTool.Viewer;
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
