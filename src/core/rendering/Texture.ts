import { GL } from "./VertexFormat";

export enum TextureFormat {
    RGBA_S3TC_DXT1,
    RGBA_S3TC_DXT3,
}

export class Texture {
    private uploaded = false;
    private texture: WebGLTexture | null = null;

    constructor(
        private readonly width: number,
        private readonly height: number,
        private readonly format: TextureFormat,
        private readonly data: ArrayBuffer,
    ) {}

    upload(gl: GL): void {
        if (this.uploaded) return;

        const ext = gl.getExtension("WEBGL_compressed_texture_s3tc");

        if (!ext) {
            throw new Error("Extension WEBGL_compressed_texture_s3tc not supported.");
        }

        const texture = gl.createTexture();
        if (texture == null) throw new Error("Failed to create texture.");
        this.texture = texture;

        let gl_format: GLenum;

        switch (this.format) {
            case TextureFormat.RGBA_S3TC_DXT1:
                gl_format = ext.COMPRESSED_RGBA_S3TC_DXT1_EXT;
                break;
            case TextureFormat.RGBA_S3TC_DXT3:
                gl_format = ext.COMPRESSED_RGBA_S3TC_DXT3_EXT;
                break;
        }

        gl.bindTexture(gl.TEXTURE_2D, texture);
        gl.compressedTexImage2D(
            gl.TEXTURE_2D,
            0,
            gl_format,
            this.width,
            this.height,
            0,
            new Uint8Array(this.data),
        );
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
        gl.bindTexture(gl.TEXTURE_2D, null);

        this.uploaded = true;
    }

    bind(gl: GL): void {
        gl.bindTexture(gl.TEXTURE_2D, this.texture);
    }

    unbind(gl: GL): void {
        gl.bindTexture(gl.TEXTURE_2D, null);
    }

    delete(gl: GL): void {
        gl.deleteTexture(this.texture);
    }
}
