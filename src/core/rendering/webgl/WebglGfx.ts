import { Gfx } from "../Gfx";
import { Texture, TextureFormat } from "../Texture";
import {
    VERTEX_FORMATS,
    VERTEX_NORMAL_LOC,
    VERTEX_POS_LOC,
    VERTEX_TEX_LOC,
    VertexFormatType,
} from "../VertexFormat";

export type WebglMesh = {
    readonly vao: WebGLVertexArrayObject;
    readonly vertex_buffer: WebGLBuffer;
    readonly index_buffer: WebGLBuffer;
};

export class WebglGfx implements Gfx<WebglMesh, WebGLTexture> {
    constructor(private readonly gl: WebGL2RenderingContext) {}

    create_gfx_mesh(
        format_type: VertexFormatType,
        vertex_data: ArrayBuffer,
        index_data: ArrayBuffer,
        texture?: Texture,
    ): WebglMesh {
        const gl = this.gl;
        let vao: WebGLVertexArrayObject | null = null;
        let vertex_buffer: WebGLBuffer | null = null;
        let index_buffer: WebGLBuffer | null = null;

        try {
            vao = gl.createVertexArray();
            if (vao == null) throw new Error("Failed to create VAO.");

            vertex_buffer = gl.createBuffer();
            if (vertex_buffer == null) throw new Error("Failed to create vertex buffer.");

            index_buffer = gl.createBuffer();
            if (index_buffer == null) throw new Error("Failed to create index buffer.");

            gl.bindVertexArray(vao);

            // Vertex data.
            gl.bindBuffer(gl.ARRAY_BUFFER, vertex_buffer);
            gl.bufferData(gl.ARRAY_BUFFER, vertex_data, gl.STATIC_DRAW);

            const format = VERTEX_FORMATS[format_type];
            const vertex_size = format.size;

            gl.vertexAttribPointer(VERTEX_POS_LOC, 3, gl.FLOAT, true, vertex_size, 0);
            gl.enableVertexAttribArray(VERTEX_POS_LOC);

            if (format.normal_offset != undefined) {
                gl.vertexAttribPointer(
                    VERTEX_NORMAL_LOC,
                    3,
                    gl.FLOAT,
                    true,
                    vertex_size,
                    format.normal_offset,
                );
                gl.enableVertexAttribArray(VERTEX_NORMAL_LOC);
            }

            if (format.tex_offset != undefined) {
                gl.vertexAttribPointer(
                    VERTEX_TEX_LOC,
                    2,
                    gl.UNSIGNED_SHORT,
                    true,
                    vertex_size,
                    format.tex_offset,
                );
                gl.enableVertexAttribArray(VERTEX_TEX_LOC);
            }

            gl.bindBuffer(gl.ARRAY_BUFFER, null);

            // Index data.
            gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, index_buffer);
            gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, index_data, gl.STATIC_DRAW);

            gl.bindVertexArray(null);

            texture?.upload();

            return {
                vao,
                vertex_buffer,
                index_buffer,
            };
        } catch (e) {
            gl.deleteVertexArray(vao);
            gl.deleteBuffer(vertex_buffer);
            gl.deleteBuffer(index_buffer);
            throw e;
        }
    }

    destroy_gfx_mesh(gfx_mesh?: WebglMesh): void {
        if (gfx_mesh) {
            const gl = this.gl;
            gl.deleteVertexArray(gfx_mesh.vao);
            gl.deleteBuffer(gfx_mesh.vertex_buffer);
            gl.deleteBuffer(gfx_mesh.index_buffer);
        }
    }

    create_texture(
        format: TextureFormat,
        width: number,
        height: number,
        data: ArrayBuffer,
    ): WebGLTexture {
        const gl = this.gl;

        const ext = gl.getExtension("WEBGL_compressed_texture_s3tc");

        if (!ext) {
            throw new Error("Extension WEBGL_compressed_texture_s3tc not supported.");
        }

        const gl_texture = gl.createTexture();
        if (gl_texture == null) throw new Error("Failed to create texture.");

        let gl_format: GLenum;

        switch (format) {
            case TextureFormat.RGBA_S3TC_DXT1:
                gl_format = ext.COMPRESSED_RGBA_S3TC_DXT1_EXT;
                break;
            case TextureFormat.RGBA_S3TC_DXT3:
                gl_format = ext.COMPRESSED_RGBA_S3TC_DXT3_EXT;
                break;
        }

        gl.bindTexture(gl.TEXTURE_2D, gl_texture);
        gl.compressedTexImage2D(
            gl.TEXTURE_2D,
            0,
            gl_format,
            width,
            height,
            0,
            new Uint8Array(data),
        );
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
        gl.bindTexture(gl.TEXTURE_2D, null);

        return gl_texture;
    }

    destroy_texture(texture?: WebGLTexture): void {
        if (texture != undefined) {
            this.gl.deleteTexture(texture);
        }
    }
}
