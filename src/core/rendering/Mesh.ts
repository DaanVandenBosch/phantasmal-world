import { VertexFormat } from "./VertexFormat";
import { Texture } from "./Texture";
import { Gfx } from "./Gfx";

export class Mesh {
    gfx_mesh: unknown;

    constructor(
        private readonly gfx: Gfx,
        readonly format: VertexFormat,
        readonly vertex_data: ArrayBuffer,
        readonly index_data: ArrayBuffer,
        readonly index_count: number,
        readonly texture?: Texture,
    ) {}

    upload(): void {
        this.texture?.upload();
        this.gfx_mesh = this.gfx.create_gfx_mesh(
            this.format,
            this.vertex_data,
            this.index_data,
            this.texture,
        );
    }

    destroy(): void {
        this.gfx.destroy_gfx_mesh(this.gfx_mesh);
    }
}
