import { Texture, TextureFormat } from "./Texture";
import { VertexFormat } from "./VertexFormat";

export interface Gfx<GfxMesh = unknown, GfxTexture = unknown> {
    create_gfx_mesh(
        format: VertexFormat,
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
