import { Property } from "../../core/observable/property/Property";
import { WritableProperty } from "../../core/observable/property/WritableProperty";
import { require_non_negative_integer } from "../../core/util";
import { property } from "../../core/observable";

export class WaveModel {
    private readonly _id: WritableProperty<number>;
    private readonly _area_id: Property<number>;
    private readonly _section_id: Property<number>;

    readonly id: Property<number>;
    readonly area_id: Property<number>;
    readonly section_id: Property<number>;

    constructor(id: number, area_id: number, section_id: number) {
        require_non_negative_integer(id, "id");
        require_non_negative_integer(area_id, "area_id");
        require_non_negative_integer(section_id, "section_id");

        this._id = property(id);
        this.id = this._id;

        this._area_id = property(area_id);
        this.area_id = this._area_id;

        this._section_id = property(section_id);
        this.section_id = this._section_id;
    }
}
