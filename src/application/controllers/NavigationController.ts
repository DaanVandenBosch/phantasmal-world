import { Controller } from "../../core/controllers/Controller";
import { Property } from "../../core/observable/property/Property";
import { GuiStore, GuiTool } from "../../core/stores/GuiStore";
import { property } from "../../core/observable";
import { Clock } from "../../core/Clock";

export class NavigationController extends Controller {
    private readonly _internet_time = property("@");
    private readonly internet_time_interval: any;

    readonly tool: Property<GuiTool>;
    readonly internet_time: Property<string> = this._internet_time;

    constructor(private readonly gui_store: GuiStore, private readonly clock: Clock) {
        super();

        this.tool = gui_store.tool;
        this.internet_time_interval = setInterval(this.set_internet_time, 1000);
        this.set_internet_time();
    }

    dispose(): void {
        super.dispose();
        clearInterval(this.internet_time_interval);
    }

    set_tool(tool: GuiTool): void {
        this.gui_store.set_tool(tool);
    }

    private set_internet_time = (): void => {
        const now = this.clock.now();
        const s = now.getUTCSeconds();
        const m = now.getUTCMinutes();
        const h = (now.getUTCHours() + 1) % 24; // Internet time is calculated from UTC+01:00.
        this._internet_time.val = `@${Math.floor((s + 60 * (m + 60 * h)) / 86.4)}`;
    };
}
