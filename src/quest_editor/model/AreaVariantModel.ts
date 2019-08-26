import { ArrayProperty } from "../../core/observable/ArrayProperty";
import { WritableArrayProperty } from "../../core/observable/WritableArrayProperty";
import { array_property } from "../../core/observable";
import { AreaModel } from "./AreaModel";
import { SectionModel } from "./SectionModel";

export class AreaVariantModel {
    readonly id: number;

    readonly area: AreaModel;

    private readonly _sections: WritableArrayProperty<SectionModel> = array_property();
    readonly sections: ArrayProperty<SectionModel> = this._sections;

    constructor(id: number, area: AreaModel) {
        if (!Number.isInteger(id) || id < 0)
            throw new Error(`Expected id to be a non-negative integer, got ${id}.`);

        this.id = id;
        this.area = area;
    }
}
