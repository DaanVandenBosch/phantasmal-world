import { Gfx } from "../Gfx";
import { Texture, TextureFormat } from "../Texture";
import { VertexFormat } from "../VertexFormat";

export type WebgpuMesh = {
    readonly uniform_buffer: GPUBuffer;
    readonly bind_group: GPUBindGroup;
    readonly vertex_buffer: GPUBuffer;
    readonly index_buffer: GPUBuffer;
};

export class WebgpuGfx implements Gfx<WebgpuMesh, GPUTexture> {
    constructor(
        private readonly device: GPUDevice,
        private readonly bind_group_layout: GPUBindGroupLayout,
    ) {}

    create_gfx_mesh(
        format: VertexFormat,
        vertex_data: ArrayBuffer,
        index_data: ArrayBuffer,
        texture?: Texture,
    ): WebgpuMesh {
        const uniform_buffer = this.device.createBuffer({
            size: 4 * 16,
            usage: GPUBufferUsage.COPY_DST | GPUBufferUsage.UNIFORM, // eslint-disable-line no-undef
        });

        const bind_group = this.device.createBindGroup({
            layout: this.bind_group_layout,
            bindings: [
                {
                    binding: 0,
                    resource: {
                        buffer: uniform_buffer,
                    },
                },
                {
                    binding: 1,
                    resource: this.device.createSampler({
                        magFilter: "linear",
                        minFilter: "linear",
                    }),
                },
                {
                    binding: 2,
                    resource: (texture!.gfx_texture as GPUTexture).createView(),
                },
            ],
        });

        const vertex_buffer = this.device.createBuffer({
            size: vertex_data.byteLength,
            usage: GPUBufferUsage.COPY_DST | GPUBufferUsage.VERTEX, // eslint-disable-line no-undef
        });

        vertex_buffer.setSubData(0, new Uint8Array(vertex_data));

        const index_buffer = this.device.createBuffer({
            size: index_data.byteLength,
            usage: GPUBufferUsage.COPY_DST | GPUBufferUsage.INDEX, // eslint-disable-line no-undef
        });

        index_buffer.setSubData(0, new Uint16Array(index_data));

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
        if (format === TextureFormat.RGBA_S3TC_DXT1 || format === TextureFormat.RGBA_S3TC_DXT3) {
            // Chrome's WebGPU implementation doesn't support compressed textures yet. Use a dummy
            // texture instead.
            const ab = new ArrayBuffer(16);
            const ba = new Uint32Array(ab);

            ba[0] = 0xffff0000;
            ba[1] = 0xff00ff00;
            ba[2] = 0xff0000ff;
            ba[3] = 0xff00ffff;

            width = 2;
            height = 2;
            data = ab;
        }

        const texture = this.device.createTexture({
            size: {
                width,
                height,
                depth: 1,
            },
            format: "rgba8unorm",
            usage: GPUTextureUsage.COPY_DST | GPUTextureUsage.SAMPLED, // eslint-disable-line no-undef
        });

        const row_pitch = Math.ceil((4 * width) / 256) * 256;
        const data_size = row_pitch * height;

        const buffer = this.device.createBuffer({
            size: data_size,
            usage: GPUBufferUsage.COPY_DST | GPUBufferUsage.COPY_SRC, // eslint-disable-line no-undef
        });

        let buffer_data: Uint8Array;

        if (data_size === data.byteLength) {
            buffer_data = new Uint8Array(data);
        } else {
            buffer_data = new Uint8Array(data_size);
            const orig_data = new Uint8Array(data);
            let orig_idx = 0;

            for (let y = 0; y < height; y++) {
                for (let x = 0; x < width; x++) {
                    const idx = 4 * x + row_pitch * y;

                    buffer_data[idx] = orig_data[orig_idx];
                    buffer_data[idx + 1] = orig_data[orig_idx + 1];
                    buffer_data[idx + 2] = orig_data[orig_idx + 2];
                    buffer_data[idx + 3] = orig_data[orig_idx + 3];

                    orig_idx += 4;
                }
            }
        }

        buffer.setSubData(0, buffer_data);

        const command_encoder = this.device.createCommandEncoder();
        command_encoder.copyBufferToTexture(
            {
                buffer,
                rowPitch: row_pitch,
                imageHeight: 0,
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
