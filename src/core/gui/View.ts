import { Widget } from "./Widget";
import { array_remove } from "../util";

export abstract class View extends Widget {
    private readonly _children: Widget[] = [];

    get children(): readonly Widget[] {
        return this._children;
    }

    dispose(): void {
        this._children.splice(0);
        super.dispose();
    }

    /**
     * Adds a child widget to the {@link _children} array and makes sure it is disposed when this
     * widget is disposed.
     */
    protected add<T extends Widget>(child: T): T {
        this._children.push(child);
        return this.disposable(child);
    }

    /**
     * Removes a child widget from the {@link _children} array and disposes it.
     */
    protected remove(child: Widget): void {
        array_remove(this._children, child);
        this.remove_disposable(child);
    }
}
