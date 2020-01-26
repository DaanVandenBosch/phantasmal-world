import { VertexFormat } from "./VertexFormat";
import { Texture } from "./Texture";
import { Gfx } from "./Gfx";
import {
    MeshBuilder,
    PosNormMeshBuilder,
    PosNormTexMeshBuilder,
    PosTexMeshBuilder,
} from "./MeshBuilder";

export class Mesh {
    /* eslint-disable no-dupe-class-members */
    static builder(format: VertexFormat.PosNorm): PosNormMeshBuilder;
    static builder(format: VertexFormat.PosTex): PosTexMeshBuilder;
    static builder(format: VertexFormat.PosNormTex): PosNormTexMeshBuilder;
    static builder(format: VertexFormat): MeshBuilder {
        switch (format) {
            case VertexFormat.PosNorm:
                return new PosNormMeshBuilder();
            case VertexFormat.PosTex:
                return new PosTexMeshBuilder();
            case VertexFormat.PosNormTex:
                return new PosNormTexMeshBuilder();
        }
    }
    /* eslint-enable no-dupe-class-members */

    gfx_mesh: unknown;

    constructor(
        readonly format: VertexFormat,
        readonly vertex_data: ArrayBuffer,
        readonly index_data: ArrayBuffer,
        readonly index_count: number,
        readonly texture?: Texture,
    ) {}

    upload(gfx: Gfx): void {
        this.texture?.upload();

        if (this.gfx_mesh == undefined) {
            this.gfx_mesh = gfx.create_gfx_mesh(
                this.format,
                this.vertex_data,
                this.index_data,
                this.texture,
            );
        }
    }

    destroy(gfx: Gfx): void {
        gfx.destroy_gfx_mesh(this.gfx_mesh);
    }
}
