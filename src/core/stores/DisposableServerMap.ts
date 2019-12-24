import { Server } from "../model";
import { GuiStore } from "./GuiStore";
import { ServerMap } from "./ServerMap";
import { Disposable } from "../observable/Disposable";
import { Disposer } from "../observable/Disposer";

export class DisposableServerMap<T extends Disposable> extends ServerMap<T> implements Disposable {
    private readonly disposer = new Disposer();

    constructor(gui_store: GuiStore, get_value: (server: Server) => Promise<T>) {
        super(gui_store, async server => {
            const value = await get_value(server);

            if (this.disposer.disposed) {
                value.dispose();
            } else {
                this.disposer.add(value);
            }

            return value;
        });
    }

    dispose(): void {
        this.disposer.dispose();
    }
}
