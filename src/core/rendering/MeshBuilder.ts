import { Texture } from "./Texture";
import { VERTEX_FORMATS, VertexFormat, VertexFormatType } from "./VertexFormat";
import { Mesh } from "./Mesh";
import { Vec2, Vec3 } from "../math/linear_algebra";

export abstract class MeshBuilder {
    private readonly format: VertexFormat;

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

    protected constructor(format_type: VertexFormatType) {
        this.format = VERTEX_FORMATS[format_type];
    }

    triangle(v1: number, v2: number, v3: number): this {
        this.index_data.push(v1, v2, v3);
        return this;
    }

    build(): Mesh {
        const v_size = this.format.size;
        const v_normal_offset = this.format.normal_offset;
        const v_tex_offset = this.format.tex_offset;
        const v_data = new ArrayBuffer(this.vertex_data.length * v_size);
        const v_view = new DataView(v_data);
        let i = 0;

        for (const { pos, normal, tex } of this.vertex_data) {
            v_view.setFloat32(i, pos.x, true);
            v_view.setFloat32(i + 4, pos.y, true);
            v_view.setFloat32(i + 8, pos.z, true);

            if (v_normal_offset != undefined) {
                v_view.setFloat32(i + v_normal_offset, normal!.x, true);
                v_view.setFloat32(i + v_normal_offset + 4, normal!.y, true);
                v_view.setFloat32(i + v_normal_offset + 8, normal!.z, true);
            }

            if (v_tex_offset != undefined) {
                v_view.setUint16(i + v_tex_offset, tex!.x * 0xffff, true);
                v_view.setUint16(i + v_tex_offset + 2, tex!.y * 0xffff, true);
            }

            i += v_size;
        }

        // Make index data divisible by 4 for WebGPU.
        const i_data = new ArrayBuffer(4 * Math.ceil(this.index_data.length / 2));
        new Uint16Array(i_data).set(this.index_data);

        return new Mesh(this.format.type, v_data, i_data, this.index_data.length, this._texture);
    }
}

export class PosNormMeshBuilder extends MeshBuilder {
    constructor() {
        super(VertexFormatType.PosNorm);
    }

    vertex(pos: Vec3, normal: Vec3): this {
        this.vertex_data.push({ pos, normal });
        return this;
    }
}

export class PosTexMeshBuilder extends MeshBuilder {
    constructor() {
        super(VertexFormatType.PosTex);
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
        super(VertexFormatType.PosNormTex);
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
