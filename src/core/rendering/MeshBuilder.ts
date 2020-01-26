import { Texture } from "./Texture";
import {
    vertex_format_normal_offset,
    vertex_format_size,
    vertex_format_tex_offset,
    VertexFormat,
} from "./VertexFormat";
import { Mesh } from "./Mesh";
import { Vec2, Vec3 } from "../math/linear_algebra";

export abstract class MeshBuilder {
    protected readonly vertex_data: {
        pos: Vec3;
        normal?: Vec3;
        tex?: Vec2;
    }[] = [];
    protected readonly index_data: number[] = [];
    protected _texture?: Texture;

    get vertex_count(): number {
        return this.vertex_data.length;
    }

    protected constructor(private readonly format: VertexFormat) {}

    triangle(v1: number, v2: number, v3: number): this {
        this.index_data.push(v1, v2, v3);
        return this;
    }

    build(): Mesh {
        const v_size = vertex_format_size(this.format);
        const v_normal_offset = vertex_format_normal_offset(this.format);
        const v_tex_offset = vertex_format_tex_offset(this.format);
        const v_data = new ArrayBuffer(this.vertex_data.length * v_size);
        const v_view = new DataView(v_data);
        let i = 0;

        for (const { pos, normal, tex } of this.vertex_data) {
            v_view.setFloat32(i, pos.x, true);
            v_view.setFloat32(i + 4, pos.y, true);
            v_view.setFloat32(i + 8, pos.z, true);

            if (v_normal_offset !== -1) {
                v_view.setFloat32(i + v_normal_offset, normal!.x, true);
                v_view.setFloat32(i + v_normal_offset + 4, normal!.y, true);
                v_view.setFloat32(i + v_normal_offset + 8, normal!.z, true);
            }

            if (v_tex_offset !== -1) {
                v_view.setUint16(i + v_tex_offset, tex!.x * 0xffff, true);
                v_view.setUint16(i + v_tex_offset + 2, tex!.y * 0xffff, true);
            }

            i += v_size;
        }

        // Make index data divisible by 4 for WebGPU.
        const i_data = new Uint16Array(2 * Math.ceil(this.index_data.length / 2));
        i_data.set(this.index_data);

        return new Mesh(this.format, v_data, i_data, this.index_data.length, this._texture);
    }
}

export class PosNormMeshBuilder extends MeshBuilder {
    constructor() {
        super(VertexFormat.PosNorm);
    }

    vertex(pos: Vec3, normal: Vec3): this {
        this.vertex_data.push({ pos, normal });
        return this;
    }
}

export class PosTexMeshBuilder extends MeshBuilder {
    constructor() {
        super(VertexFormat.PosTex);
    }

    vertex(pos: Vec3, tex: Vec2): this {
        this.vertex_data.push({ pos, tex });
        return this;
    }

    texture(tex: Texture): this {
        this._texture = tex;
        return this;
    }
}

export class PosNormTexMeshBuilder extends MeshBuilder {
    constructor() {
        super(VertexFormat.PosNormTex);
    }

    vertex(pos: Vec3, normal: Vec3, tex: Vec2): this {
        this.vertex_data.push({ pos, normal, tex });
        return this;
    }

    texture(tex: Texture): this {
        this._texture = tex;
        return this;
    }
}
