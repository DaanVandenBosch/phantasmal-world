import { LogManager } from "../../Logger";
import { MeshBuilder } from "../MeshBuilder";
import { vertex_format_size, VertexFormat } from "../VertexFormat";
import { Texture } from "../Texture";
import { GlRenderer } from "../GlRenderer";
import { WebgpuMesh } from "./WebgpuMesh";
import { WebgpuScene } from "./WebgpuScene";
import { Camera } from "../Camera";
import { Disposable } from "../../observable/Disposable";
import { Mat4, mat4_product, Vec2, vec2_diff } from "../../math";
import { IdentityTransform } from "../Transform";

const logger = LogManager.get("core/rendering/webgpu/WebgpuRenderer");

const VERTEX_SHADER_SOURCE = `#version 450

layout(set = 0, binding = 0) uniform Uniforms {
    mat4 mvp_mat;
} uniforms;

layout(location = 0) in vec3 pos;

void main() {
    gl_Position = uniforms.mvp_mat * vec4(pos, 1.0);
}
`;

const FRAG_SHADER_SOURCE = `#version 450

layout(location = 0) out vec4 out_color;

void main() {
    out_color = vec4(0.0, 0.4, 0.8, 1.0);
}
`;

/**
 * Uses the experimental WebGPU API for rendering.
 */
export class WebgpuRenderer implements GlRenderer<WebgpuMesh> {
    private disposed: boolean = false;
    /**
     * Is defined when an animation frame is scheduled.
     */
    private animation_frame?: number;
    /**
     * Is defined when the renderer is fully initialized.
     */
    private renderer?: InitializedRenderer;
    private width = 800;
    private height = 600;
    private pointer_pos?: Vec2;

    protected scene?: WebgpuScene;
    protected readonly camera = new Camera();

    readonly canvas_element: HTMLCanvasElement = document.createElement("canvas");

    constructor() {
        this.canvas_element.width = this.width;
        this.canvas_element.height = this.height;

        this.canvas_element.addEventListener("mousedown", this.mousedown);
        this.canvas_element.addEventListener("wheel", this.wheel, { passive: true });

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
            const glslang_module = await import(
                // @ts-ignore
                /* webpackIgnore: true */ "https://unpkg.com/@webgpu/glslang@0.0.7/web/glslang.js"
            );
            const glslang = await glslang_module.default();

            if (!this.disposed) {
                this.renderer = new InitializedRenderer(
                    this.canvas_element,
                    context,
                    device,
                    glslang,
                    this.camera,
                );
                this.renderer.set_size(this.width, this.height);

                this.scene = this.renderer.scene;

                this.scene.root_node.add_child(
                    this.mesh_builder(VertexFormat.Pos)
                        .vertex(1, 1, 0.5)
                        .vertex(-1, 1, 0.5)
                        .vertex(-1, -1, 0.5)
                        .vertex(1, -1, 0.5)
                        .triangle(0, 1, 2)
                        .triangle(0, 2, 3)
                        .build(),
                    new IdentityTransform(),
                );

                this.schedule_render();
            }
        } catch (e) {
            logger.error("Failed to initialize WebGPU renderer.", e);
        }
    }

    dispose(): void {
        this.disposed = true;
        this.renderer?.dispose();
    }

    start_rendering(): void {
        this.schedule_render();
    }

    stop_rendering(): void {
        if (this.animation_frame != undefined) {
            cancelAnimationFrame(this.animation_frame);
        }

        this.animation_frame = undefined;
    }

    schedule_render = (): void => {
        if (this.animation_frame == undefined) {
            this.animation_frame = requestAnimationFrame(this.render);
        }
    };

    set_size(width: number, height: number): void {
        this.width = width;
        this.height = height;
        this.renderer?.set_size(width, height);
        this.schedule_render();
    }

    mesh_builder(vertex_format: VertexFormat): MeshBuilder<WebgpuMesh> {
        return new MeshBuilder(this, vertex_format);
    }

    mesh(
        vertex_format: VertexFormat,
        vertex_data: ArrayBuffer,
        index_data: ArrayBuffer,
        index_count: number,
        texture?: Texture,
    ): WebgpuMesh {
        return new WebgpuMesh(vertex_format, vertex_data, index_data, index_count, texture);
    }

    private render = (): void => {
        this.animation_frame = undefined;
        this.renderer?.render();
    };

    private mousedown = (evt: MouseEvent): void => {
        if (evt.buttons === 1) {
            this.pointer_pos = new Vec2(evt.clientX, evt.clientY);

            window.addEventListener("mousemove", this.mousemove);
            window.addEventListener("mouseup", this.mouseup);
        }
    };

    private mousemove = (evt: MouseEvent): void => {
        if (evt.buttons === 1) {
            const new_pos = new Vec2(evt.clientX, evt.clientY);
            const diff = vec2_diff(new_pos, this.pointer_pos!);
            this.camera.pan(-diff.x, diff.y, 0);
            this.pointer_pos = new_pos;
            this.schedule_render();
        }
    };

    private mouseup = (): void => {
        this.pointer_pos = undefined;

        window.removeEventListener("mousemove", this.mousemove);
        window.removeEventListener("mouseup", this.mouseup);
    };

    private wheel = (evt: WheelEvent): void => {
        if (evt.deltaY < 0) {
            this.camera.zoom(1.1);
        } else {
            this.camera.zoom(0.9);
        }

        this.schedule_render();
    };
}

class InitializedRenderer implements Disposable {
    private readonly swap_chain: GPUSwapChain;
    private readonly pipeline: GPURenderPipeline;
    private projection_mat: Mat4 = Mat4.identity();

    readonly scene: WebgpuScene;

    constructor(
        private readonly canvas_element: HTMLCanvasElement,
        private readonly context: GPUCanvasContext,
        private readonly device: GPUDevice,
        private readonly glslang: any,
        private readonly camera: Camera,
    ) {
        const swap_chain_format = "bgra8unorm";

        this.swap_chain = context.configureSwapChain({
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
            ],
        });

        this.pipeline = device.createRenderPipeline({
            layout: device.createPipelineLayout({ bindGroupLayouts: [bind_group_layout] }),
            vertexStage: {
                module: device.createShaderModule({
                    code: glslang.compileGLSL(VERTEX_SHADER_SOURCE, "vertex", true),
                }),
                entryPoint: "main",
            },
            fragmentStage: {
                module: device.createShaderModule({
                    code: glslang.compileGLSL(FRAG_SHADER_SOURCE, "fragment", true),
                }),
                entryPoint: "main",
            },
            primitiveTopology: "triangle-list",
            colorStates: [{ format: swap_chain_format }],
            vertexState: {
                indexFormat: "uint16",
                vertexBuffers: [
                    {
                        arrayStride: vertex_format_size(VertexFormat.Pos),
                        stepMode: "vertex",
                        attributes: [
                            {
                                format: "float3",
                                offset: 0,
                                shaderLocation: 0,
                            },
                        ],
                    },
                ],
            },
        });

        this.scene = new WebgpuScene(device, bind_group_layout);
    }

    set_size(width: number, height: number): void {
        this.canvas_element.width = width;
        this.canvas_element.height = height;

        // prettier-ignore
        this.projection_mat = Mat4.of(
            2/width, 0,        0,    0,
            0,       2/height, 0,    0,
            0,       0,        2/10, 0,
            0,       0,        0,    1,
        );
    }

    render(): void {
        const command_encoder = this.device.createCommandEncoder();
        const texture_view = this.swap_chain.getCurrentTexture().createView();

        const pass_encoder = command_encoder.beginRenderPass({
            colorAttachments: [
                {
                    attachment: texture_view,
                    loadValue: { r: 0.1, g: 0.1, b: 0.1, a: 1 },
                },
            ],
        });

        pass_encoder.setPipeline(this.pipeline);

        const camera_project_mat = mat4_product(this.projection_mat, this.camera.transform.mat4);

        this.scene.traverse((node, parent_mat) => {
            const mat = mat4_product(parent_mat, node.transform.mat4);

            if (node.mesh) {
                node.mesh.render(pass_encoder, mat);
            }

            return mat;
        }, camera_project_mat);

        pass_encoder.endPass();

        this.device.defaultQueue.submit([command_encoder.finish()]);
    }

    dispose(): void {
        this.scene.destroy();
    }
}
