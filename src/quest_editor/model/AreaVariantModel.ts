import { ListProperty } from "../../core/observable/property/list/ListProperty";
import { WritableListProperty } from "../../core/observable/property/list/WritableListProperty";
import { list_property } from "../../core/observable";
import { AreaModel } from "./AreaModel";
import { SectionModel } from "./SectionModel";

export class AreaVariantModel {
    private readonly _sections: WritableListProperty<SectionModel> = list_property();

    readonly id: number;
    readonly area: AreaModel;
    readonly sections: ListProperty<SectionModel> = this._sections;

    constructor(id: number, area: AreaModel) {
        if (!Number.isInteger(id) || id < 0)
            throw new Error(`Expected id to be a non-negative integer, got ${id}.`);

        this.id = id;
        this.area = area;
    }
}
