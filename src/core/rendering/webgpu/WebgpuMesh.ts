import { Mesh } from "../Mesh";
import { Texture } from "../Texture";
import { VertexFormat } from "../VertexFormat";
import { defined } from "../../util";
import { Mat4 } from "../../math";

export class WebgpuMesh implements Mesh {
    private uniform_buffer?: GPUBuffer;
    private bind_group?: GPUBindGroup;
    private vertex_buffer?: GPUBuffer;
    private index_buffer?: GPUBuffer;

    constructor(
        readonly format: VertexFormat,
        private readonly vertex_data: ArrayBuffer,
        private readonly index_data: ArrayBuffer,
        private readonly index_count: number,
        readonly texture?: Texture,
    ) {}

    upload(device: GPUDevice, bind_group_layout: GPUBindGroupLayout): void {
        this.uniform_buffer = device.createBuffer({
            size: 4 * 16,
            usage: GPUBufferUsage.COPY_DST | GPUBufferUsage.UNIFORM, // eslint-disable-line no-undef
        });

        this.bind_group = device.createBindGroup({
            layout: bind_group_layout,
            bindings: [
                {
                    binding: 0,
                    resource: {
                        buffer: this.uniform_buffer,
                    },
                },
            ],
        });

        this.vertex_buffer = device.createBuffer({
            size: this.vertex_data.byteLength,
            usage: GPUBufferUsage.COPY_DST | GPUBufferUsage.VERTEX, // eslint-disable-line no-undef
        });

        this.vertex_buffer.setSubData(0, new Uint8Array(this.vertex_data));

        this.index_buffer = device.createBuffer({
            size: this.index_data.byteLength,
            usage: GPUBufferUsage.COPY_DST | GPUBufferUsage.INDEX, // eslint-disable-line no-undef
        });

        this.index_buffer.setSubData(0, new Uint16Array(this.index_data));
    }

    render(pass_encoder: GPURenderPassEncoder, mat: Mat4): void {
        defined(this.uniform_buffer, "uniform_buffer");
        defined(this.bind_group, "bind_group");
        defined(this.vertex_buffer, "vertex_buffer");
        defined(this.index_buffer, "index_buffer");

        this.uniform_buffer.setSubData(0, mat.data);
        pass_encoder.setBindGroup(0, this.bind_group);
        pass_encoder.setVertexBuffer(0, this.vertex_buffer);
        pass_encoder.setIndexBuffer(this.index_buffer);
        pass_encoder.drawIndexed(this.index_count, 1, 0, 0, 0);
    }

    destroy(): void {
        this.uniform_buffer?.destroy();
        this.vertex_buffer?.destroy();
        this.index_buffer?.destroy();
    }
}
