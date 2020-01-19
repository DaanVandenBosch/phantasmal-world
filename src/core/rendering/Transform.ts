import { Matrix4 } from "../math";

export interface Transform {
    readonly matrix4: Matrix4;
}

export class TranslateTransform implements Transform {
    readonly matrix4: Matrix4;

    constructor(x: number, y: number, z: number) {
        // prettier-ignore
        this.matrix4 = Matrix4.of(
            1, 0, 0, x,
            0, 1, 0, y,
            0, 0, 1, z,
            0, 0, 0, 1,
        );
    }
}

export class IdentityTransform implements Transform {
    readonly matrix4: Matrix4;

    constructor() {
        // prettier-ignore
        this.matrix4 = Matrix4.of(
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1,
        );
    }
}
