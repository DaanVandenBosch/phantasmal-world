import { AreaVariantModel } from "./AreaVariantModel";
import { Euler, Quaternion, Vector3 } from "three";

export class SectionModel {
    readonly id: number;
    readonly position: Vector3;
    readonly rotation: Euler;
    readonly inverse_rotation: Euler;
    readonly area_variant: AreaVariantModel;

    constructor(id: number, position: Vector3, rotation: Euler, area_variant: AreaVariantModel) {
        if (!Number.isInteger(id) || id < -1)
            throw new Error(`Expected id to be an integer greater than or equal to -1, got ${id}.`);
        if (!position) throw new Error("position is required.");
        if (!rotation) throw new Error("rotation is required.");
        if (!area_variant) throw new Error("area_variant is required.");

        this.id = id;
        this.position = position;
        this.rotation = rotation;
        this.inverse_rotation = new Euler().setFromQuaternion(
            new Quaternion().setFromEuler(rotation).inverse(),
        );
        this.area_variant = area_variant;
    }
}
