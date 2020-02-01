import { LogManager } from "../../Logger";
import { vertex_format_size, VertexFormat } from "../VertexFormat";
import { GfxRenderer } from "../GfxRenderer";
import { mat4_product } from "../../math/linear_algebra";
import { WebgpuGfx, WebgpuMesh } from "./WebgpuGfx";
import { ShaderLoader } from "./ShaderLoader";
import { HttpClient } from "../../HttpClient";
import { Projection } from "../Camera";

const logger = LogManager.get("core/rendering/webgpu/WebgpuRenderer");

/**
 * Uses the experimental WebGPU API for rendering.
 */
export class WebgpuRenderer extends GfxRenderer {
    private disposed: boolean = false;
    /**
     * Is defined when the renderer is fully initialized.
     */
    private gpu?: {
        gfx: WebgpuGfx;
        device: GPUDevice;
        swap_chain: GPUSwapChain;
        pipeline: GPURenderPipeline;
    };
    private width = 800;
    private height = 600;
    private shader_loader: ShaderLoader;

    get gfx(): WebgpuGfx {
        return this.gpu!.gfx;
    }

    constructor(projection: Projection, http_client: HttpClient) {
        super(projection);

        this.shader_loader = new ShaderLoader(http_client);

        this.initialize();
    }

    private async initialize(): Promise<void> {
        try {
            if (window.navigator.gpu == undefined) {
                logger.error("WebGPU not supported on this device.");
                return;
            }

            const context = this.canvas_element.getContext("gpupresent") as GPUCanvasContext | null;

            if (context == null) {
                logger.error("Failed to initialize gpupresent context.");
                return;
            }

            const adapter = await window.navigator.gpu.requestAdapter();
            const device = await adapter.requestDevice();
            const vertex_shader_source = await this.shader_loader.load("vertex_shader.vert");
            const fragment_shader_source = await this.shader_loader.load("fragment_shader.frag");

            if (!this.disposed) {
                const swap_chain_format = "bgra8unorm";

                const swap_chain = context.configureSwapChain({
                    device: device,
                    format: swap_chain_format,
                });

                const bind_group_layout = device.createBindGroupLayout({
                    bindings: [
                        {
                            binding: 0,
                            visibility: GPUShaderStage.VERTEX, // eslint-disable-line no-undef
                            type: "uniform-buffer",
                        },
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
                    ],
                });

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
                    colorStates: [{ format: swap_chain_format }],
                    vertexState: {
                        indexFormat: "uint16",
                        vertexBuffers: [
                            {
                                arrayStride: vertex_format_size(VertexFormat.PosTex),
                                stepMode: "vertex",
                                attributes: [
                                    {
                                        format: "float3",
                                        offset: 0,
                                        shaderLocation: 0,
                                    },
                                    {
                                        format: "ushort2norm",
                                        offset: 12,
                                        shaderLocation: 1,
                                    },
                                ],
                            },
                        ],
                    },
                });

                this.gpu = {
                    gfx: new WebgpuGfx(device, bind_group_layout),
                    device,
                    swap_chain,
                    pipeline,
                };

                this.set_size(this.width, this.height);
            }
        } catch (e) {
            logger.error("Failed to initialize WebGPU renderer.", e);
        }
    }

    dispose(): void {
        this.disposed = true;
        super.dispose();
    }

    set_size(width: number, height: number): void {
        this.width = width;
        this.height = height;

        // There seems to be a bug in chrome's WebGPU implementation that requires you to set a
        // canvas element's width and height after it's added to the DOM.
        if (this.gpu) {
            this.canvas_element.width = width;
            this.canvas_element.height = height;
        }

        super.set_size(width, height);
    }

    protected render(): void {
        if (this.gpu) {
            const { device, swap_chain, pipeline } = this.gpu;

            const command_encoder = device.createCommandEncoder();
            const texture_view = swap_chain.getCurrentTexture().createView();

            const pass_encoder = command_encoder.beginRenderPass({
                colorAttachments: [
                    {
                        attachment: texture_view,
                        loadValue: { r: 0.1, g: 0.1, b: 0.1, a: 1 },
                    },
                ],
            });

            pass_encoder.setPipeline(pipeline);

            const camera_project_mat = mat4_product(this.camera.projection_matrix, this.camera.view_matrix);

            this.scene.traverse((node, parent_mat) => {
                const mat = mat4_product(parent_mat, node.transform);

                if (node.mesh) {
                    const gfx_mesh = node.mesh.gfx_mesh as WebgpuMesh;
                    gfx_mesh.uniform_buffer.setSubData(0, mat.data);
                    pass_encoder.setBindGroup(0, gfx_mesh.bind_group);
                    pass_encoder.setVertexBuffer(0, gfx_mesh.vertex_buffer);
                    pass_encoder.setIndexBuffer(gfx_mesh.index_buffer);
                    pass_encoder.drawIndexed(node.mesh.index_count, 1, 0, 0, 0);
                }

                return mat;
            }, camera_project_mat);

            pass_encoder.endPass();

            device.defaultQueue.submit([command_encoder.finish()]);
        }
    }
}
