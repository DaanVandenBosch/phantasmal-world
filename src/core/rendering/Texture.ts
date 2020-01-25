import { Gfx } from "./Gfx";

export enum TextureFormat {
    RGBA_S3TC_DXT1,
    RGBA_S3TC_DXT3,
}

export class Texture {
    gfx_texture: unknown;

    constructor(
        private readonly gfx: Gfx,
        private readonly format: TextureFormat,
        private readonly width: number,
        private readonly height: number,
        private readonly data: ArrayBuffer,
    ) {}

    upload(): void {
        if (this.gfx_texture == undefined) {
            this.gfx_texture = this.gfx.create_texture(
                this.format,
                this.width,
                this.height,
                this.data,
            );
        }
    }

    destroy(): void {
        this.gfx.destroy_texture(this.gfx_texture);
    }
}
