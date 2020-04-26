import { Texture, TextureFormat } from "./Texture";
import { VertexFormatType } from "./VertexFormat";

export interface Gfx<GfxMesh = unknown, GfxTexture = unknown> {
    create_gfx_mesh(
        format: VertexFormatType,
        vertex_data: ArrayBuffer,
        index_data: ArrayBuffer,
        texture?: Texture,
    ): GfxMesh;

    destroy_gfx_mesh(gfx_mesh?: GfxMesh): void;

    create_texture(
        format: TextureFormat,
        width: number,
        height: number,
        data: ArrayBuffer,
    ): GfxTexture;

    destroy_texture(texture?: GfxTexture): void;
}
