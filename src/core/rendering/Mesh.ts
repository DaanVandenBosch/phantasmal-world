import {
    GL,
    vertex_format_size,
    vertex_format_tex_offset,
    VERTEX_POS_LOC,
    VERTEX_TEX_LOC,
    VertexFormat,
} from "./VertexFormat";
import { assert } from "../util";
import { Texture } from "./Texture";

export class Mesh {
    private readonly index_count: number;
    private vao: WebGLVertexArrayObject | null = null;
    private vertex_buffer: WebGLBuffer | null = null;
    private index_buffer: WebGLBuffer | null = null;
    private uploaded = false;

    constructor(
        readonly format: VertexFormat,
        private readonly vertex_data: ArrayBuffer,
        private readonly index_data: ArrayBuffer,
        readonly texture?: Texture,
    ) {
        this.index_count = index_data.byteLength / 2;
    }

    upload(gl: GL): void {
        if (this.uploaded) return;

        try {
            this.vao = gl.createVertexArray();
            if (this.vao == null) throw new Error("Failed to create VAO.");

            this.vertex_buffer = gl.createBuffer();
            if (this.vertex_buffer == null) throw new Error("Failed to create vertex buffer.");

            this.index_buffer = gl.createBuffer();
            if (this.index_buffer == null) throw new Error("Failed to create index buffer.");

            gl.bindVertexArray(this.vao);

            // Vertex data.
            gl.bindBuffer(gl.ARRAY_BUFFER, this.vertex_buffer);
            gl.bufferData(gl.ARRAY_BUFFER, this.vertex_data, gl.STATIC_DRAW);

            const vertex_size = vertex_format_size(this.format);

            gl.vertexAttribPointer(VERTEX_POS_LOC, 3, gl.FLOAT, true, vertex_size, 0);
            gl.enableVertexAttribArray(VERTEX_POS_LOC);

            const tex_offset = vertex_format_tex_offset(this.format);

            if (tex_offset !== -1) {
                gl.vertexAttribPointer(
                    VERTEX_TEX_LOC,
                    2,
                    gl.UNSIGNED_SHORT,
                    true,
                    vertex_size,
                    tex_offset,
                );
                gl.enableVertexAttribArray(VERTEX_TEX_LOC);
            }

            gl.bindBuffer(gl.ARRAY_BUFFER, null);

            // Index data.
            gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, this.index_buffer);
            gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, this.index_data, gl.STATIC_DRAW);

            gl.bindVertexArray(null);

            this.texture?.upload(gl);

            this.uploaded = true;
        } catch (e) {
            gl.deleteVertexArray(this.vao);
            this.vao = null;
            gl.deleteBuffer(this.vertex_buffer);
            this.vertex_buffer = null;
            gl.deleteBuffer(this.index_buffer);
            this.index_buffer = null;
            throw e;
        }
    }

    render(gl: GL): void {
        gl.bindVertexArray(this.vao);

        gl.drawElements(gl.TRIANGLES, this.index_count, gl.UNSIGNED_SHORT, 0);

        gl.bindVertexArray(null);
    }

    delete(gl: GL): void {
        gl.deleteVertexArray(this.vao);
        gl.deleteBuffer(this.vertex_buffer);
        gl.deleteBuffer(this.index_buffer);
    }
}

export class MeshBuilder {
    private readonly vertex_data: {
        x: number;
        y: number;
        z: number;
        u?: number;
        v?: number;
    }[] = [];
    private readonly index_data: number[] = [];
    private _texture?: Texture;

    constructor(private readonly format: VertexFormat) {}

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

    build(): Mesh {
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

        return new Mesh(this.format, v_data, new Uint16Array(this.index_data), this._texture);
    }
}
