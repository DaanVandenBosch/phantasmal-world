import { Vec3 } from "../../data_formats/vector";
import { Vector3 } from "three";

export function vec3_to_threejs(v: Vec3): Vector3 {
    return new Vector3(v.x, v.y, v.z);
}
