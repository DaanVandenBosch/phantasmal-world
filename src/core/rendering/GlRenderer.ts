import { Renderer } from "./Renderer";
import { VertexFormat } from "./VertexFormat";
import { MeshBuilder } from "./MeshBuilder";
import { Texture } from "./Texture";
import { Mesh } from "./Mesh";

export interface GlRenderer<MeshType extends Mesh> extends Renderer {
    mesh_builder(vertex_format: VertexFormat): MeshBuilder<MeshType>;

    mesh(
        vertex_format: VertexFormat,
        vertex_data: ArrayBuffer,
        index_data: ArrayBuffer,
        index_count: number,
        texture?: Texture,
    ): MeshType;
}
