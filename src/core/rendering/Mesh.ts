import { VertexFormat } from "./VertexFormat";
import { Texture } from "./Texture";

export interface Mesh {
    readonly format: VertexFormat;
    readonly texture?: Texture;
}
