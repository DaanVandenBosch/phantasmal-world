import { Mat4 } from "../math";

export interface Transform {
    readonly mat4: Mat4;
}

export class Mat4Transform implements Transform {
    readonly data: Float32Array;

    constructor(readonly mat4: Mat4) {
        this.data = mat4.data;
    }
}

export class TranslateTransform implements Transform {
    readonly mat4: Mat4;

    constructor(x: number, y: number, z: number) {
        // prettier-ignore
        this.mat4 = Mat4.of(
            1, 0, 0, x,
            0, 1, 0, y,
            0, 0, 1, z,
            0, 0, 0, 1,
        );
    }
}

export class IdentityTransform implements Transform {
    readonly mat4: Mat4;

    constructor() {
        // prettier-ignore
        this.mat4 = Mat4.of(
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1,
        );
    }
}
