import { AreaVariantModel } from "./AreaVariantModel";

export class AreaModel {
    /**
     * Matches the PSO ID.
     */
    readonly id: number;
    readonly name: string;
    readonly order: number;
    readonly area_variants: AreaVariantModel[];

    constructor(id: number, name: string, order: number, area_variants: AreaVariantModel[]) {
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
