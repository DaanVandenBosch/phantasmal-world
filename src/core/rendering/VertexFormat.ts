export enum VertexFormatType {
    PosNorm,
    PosTex,
    PosNormTex,
}

export type VertexFormat = {
    readonly type: VertexFormatType;
    readonly size: number;
    readonly normal_offset?: number;
    readonly tex_offset?: number;
    readonly uniform_buffer_size: number;
};

export const VERTEX_FORMATS: readonly VertexFormat[] = [
    {
        type: VertexFormatType.PosNorm,
        size: 24,
        normal_offset: 12,
        tex_offset: undefined,
        uniform_buffer_size: 4 * (16 + 9),
    },
    {
        type: VertexFormatType.PosTex,
        size: 16,
        normal_offset: undefined,
        tex_offset: 12,
        uniform_buffer_size: 4 * 16,
    },
    // TODO: add VertexFormat for PosNormTex.
    // {
    //     type: VertexFormatType.PosNormTex,
    //     size: 28,
    //     normal_offset: 12,
    //     tex_offset: 24,
    // },
];

export const VERTEX_POS_LOC = 0;
export const VERTEX_NORMAL_LOC = 1;
export const VERTEX_TEX_LOC = 2;
