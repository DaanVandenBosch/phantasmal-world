export class Vec2 {
    x: number;
    y: number;

    constructor(x: number, y: number) {
        this.x = x;
        this.y = y;
    }

    set(x: number, y: number): this {
        this.x = x;
        this.y = y;
        return this;
    }

    add(v: Vec2): this {
        this.x += v.x;
        this.y += v.y;
        return this;
    }

    clone(): Vec2 {
        return new Vec2(this.x, this.y);
    }

    equals(v: Vec2): boolean {
        return this.x === v.x && this.y === v.y;
    }
}

export class Vec3 {
    x: number;
    y: number;
    z: number;

    constructor(x: number, y: number, z: number) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    set(x: number, y: number, z: number): this {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    add(v: Vec3): this {
        this.x += v.x;
        this.y += v.y;
        this.z += v.z;
        return this;
    }

    clone(): Vec3 {
        return new Vec3(this.x, this.y, this.z);
    }

    equals(v: Vec3): boolean {
        return this.x === v.x && this.y === v.y && this.z === v.z;
    }
}
