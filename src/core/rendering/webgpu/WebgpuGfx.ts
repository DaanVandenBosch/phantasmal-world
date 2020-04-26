import { Gfx } from "../Gfx";
import { Texture, TextureFormat } from "../Texture";
import { VERTEX_FORMATS, VertexFormatType } from "../VertexFormat";
import { assert } from "../../util";

export type WebgpuMesh = {
    readonly uniform_buffer: GPUBuffer;
    readonly bind_group: GPUBindGroup;
    readonly vertex_buffer: GPUBuffer;
    readonly index_buffer: GPUBuffer;
};

export class WebgpuGfx implements Gfx<WebgpuMesh, GPUTexture> {
    constructor(
        private readonly device: GPUDevice,
        private readonly bind_group_layouts: readonly GPUBindGroupLayout[],
    ) {}

    create_gfx_mesh(
        format_type: VertexFormatType,
        vertex_data: ArrayBuffer,
        index_data: ArrayBuffer,
        texture?: Texture,
    ): WebgpuMesh {
        const format = VERTEX_FORMATS[format_type];

        const uniform_buffer = this.device.createBuffer({
            size: format.uniform_buffer_size,
            usage: GPUBufferUsage.COPY_DST | GPUBufferUsage.UNIFORM, // eslint-disable-line no-undef
        });

        const bind_group_entries: GPUBindGroupEntry[] = [
            {
                binding: 0,
                resource: {
                    buffer: uniform_buffer,
                },
            },
        ];

        if (format.tex_offset != undefined) {
            assert(
                texture,
                () => `Vertex format ${VertexFormatType[format_type]} requires a texture.`,
            );

            bind_group_entries.push(
                {
                    binding: 1,
                    resource: this.device.createSampler({
                        magFilter: "linear",
                        minFilter: "linear",
                    }),
                },
                {
                    binding: 2,
                    resource: (texture.gfx_texture as GPUTexture).createView(),
                },
            );
        }

        const bind_group = this.device.createBindGroup({
            layout: this.bind_group_layouts[format_type],
            entries: bind_group_entries,
        });

        const [vertex_buffer, vertex_array_buffer] = this.device.createBufferMapped({
            size: vertex_data.byteLength,
            usage: GPUBufferUsage.VERTEX, // eslint-disable-line no-undef
        });
        new Uint8Array(vertex_array_buffer).set(new Uint8Array(vertex_data));
        vertex_buffer.unmap();

        const [index_buffer, index_array_buffer] = this.device.createBufferMapped({
            size: index_data.byteLength,
            usage: GPUBufferUsage.INDEX, // eslint-disable-line no-undef
        });
        new Uint8Array(index_array_buffer).set(new Uint8Array(index_data));
        index_buffer.unmap();

        return {
            uniform_buffer,
            bind_group,
            vertex_buffer,
            index_buffer,
        };
    }

    destroy_gfx_mesh(gfx_mesh?: WebgpuMesh): void {
        if (gfx_mesh) {
            gfx_mesh.uniform_buffer.destroy();
            gfx_mesh.vertex_buffer.destroy();
            gfx_mesh.index_buffer.destroy();
        }
    }

    create_texture(
        format: TextureFormat,
        width: number,
        height: number,
        data: ArrayBuffer,
    ): GPUTexture {
        let texture_format: string;
        let bytes_per_pixel: number;

        switch (format) {
            case TextureFormat.RGBA_S3TC_DXT1:
                texture_format = "bc1-rgba-unorm";
                bytes_per_pixel = 2;
                break;

            case TextureFormat.RGBA_S3TC_DXT3:
                texture_format = "bc2-rgba-unorm";
                bytes_per_pixel = 4;
                break;
        }

        const texture = this.device.createTexture({
            size: {
                width,
                height,
                depth: 1,
            },
            format: (texture_format as any) as GPUTextureFormat,
            usage: GPUTextureUsage.COPY_DST | GPUTextureUsage.SAMPLED, // eslint-disable-line no-undef
        });

        // Bytes per row must be a multiple of 256.
        const bytes_per_row = Math.ceil((4 * width) / 256) * 256;
        const data_size = bytes_per_row * height;

        let buffer_data: Uint8Array;

        if (data_size === data.byteLength) {
            buffer_data = new Uint8Array(data);
        } else {
            buffer_data = new Uint8Array(data_size);
            const orig_data = new Uint8Array(data);
            let orig_idx = 0;

            for (let y = 0; y < height; y++) {
                for (let x = 0; x < width; x++) {
                    const idx = bytes_per_pixel * x + bytes_per_row * y;

                    for (let i = 0; i < bytes_per_pixel; i++) {
                        buffer_data[idx + i] = orig_data[orig_idx + i];
                    }

                    orig_idx += bytes_per_pixel;
                }
            }
        }

        const [buffer, array_buffer] = this.device.createBufferMapped({
            size: data_size,
            usage: GPUBufferUsage.COPY_SRC, // eslint-disable-line no-undef
        });
        new Uint8Array(array_buffer).set(buffer_data);
        buffer.unmap();

        const command_encoder = this.device.createCommandEncoder();
        command_encoder.copyBufferToTexture(
            {
                buffer,
                bytesPerRow: bytes_per_row,
                rowsPerImage: 0,
            },
            {
                texture,
            },
            {
                width,
                height,
                depth: 1,
            },
        );
        this.device.defaultQueue.submit([command_encoder.finish()]);

        buffer.destroy();

        return texture;
    }

    destroy_texture(texture?: GPUTexture): void {
        texture?.destroy();
    }
}
