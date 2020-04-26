import {
    VERTEX_FORMATS,
    VERTEX_NORMAL_LOC,
    VERTEX_POS_LOC,
    VERTEX_TEX_LOC,
    VertexFormat,
    VertexFormatType,
} from "../VertexFormat";
import { GfxRenderer } from "../GfxRenderer";
import { Mat4, mat4_multiply } from "../../math/linear_algebra";
import { WebgpuGfx, WebgpuMesh } from "./WebgpuGfx";
import { ShaderLoader } from "./ShaderLoader";
import { HttpClient } from "../../HttpClient";
import { Projection } from "../Camera";
import { Mesh } from "../Mesh";

type PipelineDetails = {
    readonly pipeline: GPURenderPipeline;
    readonly bind_group_layout: GPUBindGroupLayout;
};

export async function create_webgpu_renderer(
    projection: Projection,
    http_client: HttpClient,
): Promise<WebgpuRenderer> {
    if (window.navigator.gpu == undefined) {
        throw new Error("WebGPU not supported on this device.");
    }

    const canvas_element = document.createElement("canvas");
    const context = canvas_element.getContext("gpupresent") as GPUCanvasContext | null;

    if (context == null) {
        throw new Error("Failed to initialize gpupresent context.");
    }

    const adapter = await window.navigator.gpu.requestAdapter();
    const device = await adapter.requestDevice({
        extensions: ["textureCompressionBC"] as any as GPUExtensionName[],
    });
    const shader_loader = new ShaderLoader(http_client);

    const texture_format = "bgra8unorm";

    const swap_chain = context.configureSwapChain({
        device,
        format: texture_format,
    });

    const pipelines: PipelineDetails[] = await Promise.all(
        VERTEX_FORMATS.map(format =>
            create_pipeline(format, device, texture_format, shader_loader),
        ),
    );

    return new WebgpuRenderer(canvas_element, projection, device, swap_chain, pipelines);
}

async function create_pipeline(
    format: VertexFormat,
    device: GPUDevice,
    texture_format: GPUTextureFormat,
    shader_loader: ShaderLoader,
): Promise<PipelineDetails> {
    const bind_group_layout_entries: GPUBindGroupLayoutEntry[] = [
        {
            binding: 0,
            visibility: GPUShaderStage.VERTEX, // eslint-disable-line no-undef
            type: "uniform-buffer",
        },
    ];

    if (format.tex_offset != undefined) {
        bind_group_layout_entries.push(
            {
                binding: 1,
                visibility: GPUShaderStage.FRAGMENT, // eslint-disable-line no-undef
                type: "sampler",
            },
            {
                binding: 2,
                visibility: GPUShaderStage.FRAGMENT, // eslint-disable-line no-undef
                type: "sampled-texture",
            },
        );
    }

    const bind_group_layout = device.createBindGroupLayout({
        entries: bind_group_layout_entries,
    });

    let shader_name: string;

    switch (format.type) {
        case VertexFormatType.PosNorm:
            shader_name = "pos_norm";
            break;
        case VertexFormatType.PosTex:
            shader_name = "pos_tex";
            break;
        case VertexFormatType.PosNormTex:
            shader_name = "pos_norm_tex";
            break;
    }

    const vertex_shader_source = await shader_loader.load(`${shader_name}.vert`);
    const fragment_shader_source = await shader_loader.load(`${shader_name}.frag`);

    const vertex_attributes: GPUVertexAttributeDescriptor[] = [
        {
            format: "float3",
            offset: 0,
            shaderLocation: VERTEX_POS_LOC,
        },
    ];

    if (format.normal_offset != undefined) {
        vertex_attributes.push({
            format: "float3",
            offset: format.normal_offset,
            shaderLocation: VERTEX_NORMAL_LOC,
        });
    }

    if (format.tex_offset != undefined) {
        vertex_attributes.push({
            format: "ushort2norm",
            offset: format.tex_offset,
            shaderLocation: VERTEX_TEX_LOC,
        });
    }

    const pipeline = device.createRenderPipeline({
        layout: device.createPipelineLayout({ bindGroupLayouts: [bind_group_layout] }),
        vertexStage: {
            module: device.createShaderModule({
                code: vertex_shader_source,
            }),
            entryPoint: "main",
        },
        fragmentStage: {
            module: device.createShaderModule({
                code: fragment_shader_source,
            }),
            entryPoint: "main",
        },
        primitiveTopology: "triangle-list",
        colorStates: [{ format: texture_format }],
        depthStencilState: {
            format: "depth24plus",
            depthWriteEnabled: true,
            depthCompare: "less",
        },
        vertexState: {
            indexFormat: "uint16",
            vertexBuffers: [
                {
                    arrayStride: format.size,
                    stepMode: "vertex",
                    attributes: vertex_attributes,
                },
            ],
        },
    });

    return { pipeline, bind_group_layout };
}

/**
 * Uses the experimental WebGPU API for rendering.
 */
export class WebgpuRenderer extends GfxRenderer {
    private disposed: boolean = false;
    private depth_texture!: GPUTexture;

    readonly gfx: WebgpuGfx;

    constructor(
        canvas_element: HTMLCanvasElement,
        projection: Projection,
        private readonly device: GPUDevice,
        private readonly swap_chain: GPUSwapChain,
        private readonly pipelines: readonly {
            pipeline: GPURenderPipeline;
            bind_group_layout: GPUBindGroupLayout;
        }[],
    ) {
        super(canvas_element, projection);

        this.gfx = new WebgpuGfx(
            device,
            pipelines.map(p => p.bind_group_layout),
        );

        this.set_size(this.width, this.height);
    }

    dispose(): void {
        this.disposed = true;

        this.depth_texture.destroy();

        super.dispose();
    }

    set_size(width: number, height: number): void {
        this.canvas_element.width = width;
        this.canvas_element.height = height;

        this.depth_texture?.destroy();
        this.depth_texture = this.device.createTexture({
            size: {
                width,
                height,
                depth: 1,
            },
            format: "depth24plus",
            usage: GPUTextureUsage.OUTPUT_ATTACHMENT | GPUTextureUsage.COPY_SRC, // eslint-disable-line no-undef
        });

        super.set_size(width, height);
    }

    protected render(): void {
        const command_encoder = this.device.createCommandEncoder();

        // Traverse the scene graph and sort the meshes into vertex format-specific buckets.
        const draw_data: { mesh: Mesh; mvp_mat: Mat4 }[][] = VERTEX_FORMATS.map(() => []);

        const camera_project_mat = mat4_multiply(
            this.camera.projection_matrix,
            this.camera.view_matrix,
        );

        let uniform_buffer_size = 0;

        this.scene.traverse((node, parent_mat) => {
            const mat = mat4_multiply(parent_mat, node.transform);

            if (node.mesh) {
                uniform_buffer_size += VERTEX_FORMATS[node.mesh.format].uniform_buffer_size;

                draw_data[node.mesh.format].push({
                    mesh: node.mesh,
                    mvp_mat: mat,
                });
            }

            return mat;
        }, camera_project_mat);

        let uniform_buffer: GPUBuffer | undefined;

        // Upload uniform data.
        if (uniform_buffer_size > 0) {
            let uniform_array_buffer: ArrayBuffer;

            [uniform_buffer, uniform_array_buffer] = this.device.createBufferMapped({
                size: uniform_buffer_size,
                usage: GPUBufferUsage.COPY_SRC, // eslint-disable-line no-undef
            });

            const uniform_array = new Float32Array(uniform_array_buffer);
            let uniform_buffer_pos = 0;

            for (const vertex_format of VERTEX_FORMATS) {
                for (const { mesh, mvp_mat } of draw_data[vertex_format.type]) {
                    const copy_pos = 4 * uniform_buffer_pos;
                    uniform_array.set(mvp_mat.data, uniform_buffer_pos);
                    uniform_buffer_pos += mvp_mat.data.length;

                    if (vertex_format.normal_offset != undefined) {
                        const normal_mat = mvp_mat.normal_mat3();
                        uniform_array.set(normal_mat.data, uniform_buffer_pos);
                        uniform_buffer_pos += normal_mat.data.length;
                    }

                    command_encoder.copyBufferToBuffer(
                        uniform_buffer,
                        copy_pos,
                        (mesh.gfx_mesh as WebgpuMesh).uniform_buffer,
                        0,
                        vertex_format.uniform_buffer_size,
                    );
                }
            }

            uniform_buffer.unmap();
        }

        const texture_view = this.swap_chain.getCurrentTexture().createView();
        const pass_encoder = command_encoder.beginRenderPass({
            colorAttachments: [
                {
                    attachment: texture_view,
                    loadValue: { r: 0.1, g: 0.1, b: 0.1, a: 1 },
                },
            ],
            depthStencilAttachment: {
                attachment: this.depth_texture.createView(),
                depthLoadValue: 1,
                depthStoreOp: "store",
                stencilLoadValue: "load",
                stencilStoreOp: "store",
            },
        });

        // Render all meshes per vertex format.
        for (const vertex_format of VERTEX_FORMATS) {
            pass_encoder.setPipeline(this.pipelines[vertex_format.type].pipeline);

            for (const { mesh } of draw_data[vertex_format.type]) {
                const gfx_mesh = mesh.gfx_mesh as WebgpuMesh;
                pass_encoder.setBindGroup(0, gfx_mesh.bind_group);
                pass_encoder.setVertexBuffer(0, gfx_mesh.vertex_buffer);
                pass_encoder.setIndexBuffer(gfx_mesh.index_buffer);
                pass_encoder.drawIndexed(mesh.index_count, 1, 0, 0, 0);
            }
        }

        pass_encoder.endPass();
        this.device.defaultQueue.submit([command_encoder.finish()]);

        uniform_buffer?.destroy();
    }
}
