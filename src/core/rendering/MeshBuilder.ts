import { Texture } from "./Texture";
import { vertex_format_size, vertex_format_tex_offset, VertexFormat } from "./VertexFormat";
import { assert } from "../util";
import { Mesh } from "./Mesh";
import { GlRenderer } from "./GlRenderer";

export class MeshBuilder<MeshType extends Mesh> {
    private readonly vertex_data: {
        x: number;
        y: number;
        z: number;
        u?: number;
        v?: number;
    }[] = [];
    private readonly index_data: number[] = [];
    private _texture?: Texture;

    constructor(
        private readonly renderer: GlRenderer<MeshType>,
        private readonly format: VertexFormat,
    ) {}

    vertex(x: number, y: number, z: number, u?: number, v?: number): this {
        switch (this.format) {
            case VertexFormat.PosTex:
                assert(
                    u != undefined && v != undefined,
                    `Vertex format ${VertexFormat[this.format]} requires texture coordinates.`,
                );
                break;
        }

        this.vertex_data.push({ x, y, z, u, v });
        return this;
    }

    triangle(v1: number, v2: number, v3: number): this {
        this.index_data.push(v1, v2, v3);
        return this;
    }

    texture(tex: Texture): this {
        this._texture = tex;
        return this;
    }

    build(): MeshType {
        const v_size = vertex_format_size(this.format);
        const v_tex_offset = vertex_format_tex_offset(this.format);
        const v_data = new ArrayBuffer(this.vertex_data.length * v_size);
        const v_view = new DataView(v_data);
        let i = 0;

        for (const { x, y, z, u, v } of this.vertex_data) {
            v_view.setFloat32(i, x, true);
            v_view.setFloat32(i + 4, y, true);
            v_view.setFloat32(i + 8, z, true);

            if (v_tex_offset !== -1) {
                v_view.setUint16(i + v_tex_offset, u! * 0xffff, true);
                v_view.setUint16(i + v_tex_offset + 2, v! * 0xffff, true);
            }

            i += v_size;
        }

        const i_data = new Uint16Array(2 * Math.ceil(this.index_data.length / 2));
        i_data.set(this.index_data);

        return this.renderer.mesh(
            this.format,
            v_data,
            i_data,
            this.index_data.length,
            this._texture,
        );
    }
}
