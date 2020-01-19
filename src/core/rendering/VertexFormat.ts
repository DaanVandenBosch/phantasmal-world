export type GL = WebGL2RenderingContext;

export enum VertexFormat {
    Pos,
    PosTex,
}

export const VERTEX_POS_LOC = 0;
export const VERTEX_TEX_LOC = 1;

export function vertex_format_size(format: VertexFormat): number {
    switch (format) {
        case VertexFormat.Pos:
            return 12;
        case VertexFormat.PosTex:
            return 16;
    }
}

export function vertex_format_tex_offset(format: VertexFormat): number {
    switch (format) {
        case VertexFormat.Pos:
            return -1;
        case VertexFormat.PosTex:
            return 12;
    }
}
