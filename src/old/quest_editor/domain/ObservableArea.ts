import { ObservableAreaVariant } from "./ObservableAreaVariant";

export class ObservableArea {
    /**
     * Matches the PSO ID.
     */
    readonly id: number;
    readonly name: string;
    readonly order: number;
    readonly area_variants: ObservableAreaVariant[];

    constructor(id: number, name: string, order: number, area_variants: ObservableAreaVariant[]) {
        if (!Number.isInteger(id) || id < 0)
            throw new Error(`Expected id to be a non-negative integer, got ${id}.`);
        if (!name) throw new Error("name is required.");
        if (!area_variants) throw new Error("area_variants is required.");

        this.id = id;
        this.name = name;
        this.order = order;
        this.area_variants = area_variants;
    }
}
