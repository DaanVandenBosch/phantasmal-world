export enum VertexFormat {
    PosNorm,
    PosTex,
    PosNormTex,
}

export const VERTEX_POS_LOC = 0;
export const VERTEX_NORMAL_LOC = 1;
export const VERTEX_TEX_LOC = 2;

export function vertex_format_size(format: VertexFormat): number {
    switch (format) {
        case VertexFormat.PosNorm:
            return 24;
        case VertexFormat.PosTex:
            return 16;
        case VertexFormat.PosNormTex:
            return 28;
    }
}

export function vertex_format_normal_offset(format: VertexFormat): number {
    switch (format) {
        case VertexFormat.PosTex:
            return -1;
        case VertexFormat.PosNorm:
        case VertexFormat.PosNormTex:
            return 12;
    }
}

export function vertex_format_tex_offset(format: VertexFormat): number {
    switch (format) {
        case VertexFormat.PosNorm:
            return -1;
        case VertexFormat.PosTex:
            return 12;
        case VertexFormat.PosNormTex:
            return 24;
    }
}
