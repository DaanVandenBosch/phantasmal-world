import { Vec3 } from "../../data_formats/vector";
import { Vector3 } from "three";
import { Vec3 as MathVec3 } from "../../math/linear_algebra";

export function vec3_to_threejs(v: Vec3): Vector3 {
    return new Vector3(v.x, v.y, v.z);
}

export function vec3_to_math(v: Vec3): MathVec3 {
    return new MathVec3(v.x, v.y, v.z);
}
