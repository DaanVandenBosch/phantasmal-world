import { Renderer } from "./Renderer";
import { Mat4, mat4_product, Vec2, vec2_diff } from "../math";
import { ShaderProgram } from "./ShaderProgram";
import { GL } from "./VertexFormat";
import { Scene } from "./Scene";
import {
    POS_FRAG_SHADER_SOURCE,
    POS_TEX_FRAG_SHADER_SOURCE,
    POS_TEX_VERTEX_SHADER_SOURCE,
    POS_VERTEX_SHADER_SOURCE,
} from "./shader_sources";
import { Camera } from "./Camera";

export class WebglRenderer extends Renderer {
    private readonly gl: GL;
    private readonly shader_programs: ShaderProgram[];
    private animation_frame?: number;
    private projection!: Mat4;
    private pointer_pos?: Vec2;

    protected readonly scene: Scene;
    protected readonly camera = new Camera();

    readonly canvas_element: HTMLCanvasElement;

    constructor() {
        super();

        this.canvas_element = document.createElement("canvas");

        const gl = this.canvas_element.getContext("webgl2");

        if (gl == null) {
            throw new Error("Failed to initialize webgl2 context.");
        }

        this.gl = gl;

        gl.enable(gl.DEPTH_TEST);
        gl.enable(gl.CULL_FACE);
        gl.clearColor(0.1, 0.1, 0.1, 1);

        this.shader_programs = [
            new ShaderProgram(gl, POS_VERTEX_SHADER_SOURCE, POS_FRAG_SHADER_SOURCE),
            new ShaderProgram(gl, POS_TEX_VERTEX_SHADER_SOURCE, POS_TEX_FRAG_SHADER_SOURCE),
        ];

        this.scene = new Scene(gl);

        this.set_size(800, 600);

        this.canvas_element.addEventListener("mousedown", this.mousedown);
        this.canvas_element.addEventListener("wheel", this.wheel, { passive: true });
    }

    dispose(): void {
        for (const program of this.shader_programs) {
            program.delete();
        }

        this.scene.delete();
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
        this.canvas_element.width = width;
        this.canvas_element.height = height;
        this.gl.viewport(0, 0, width, height);

        // prettier-ignore
        this.projection = Mat4.of(
            2/width, 0,        0,    0,
            0,       2/height, 0,    0,
            0,       0,        2/10, 0,
            0,       0,        0,    1,
        );

        this.schedule_render();
    }

    private render = (): void => {
        this.animation_frame = undefined;
        const gl = this.gl;

        gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);

        const camera_project_mat = mat4_product(this.projection, this.camera.transform.mat4);

        this.scene.traverse((node, parent_mat) => {
            const mat = mat4_product(parent_mat, node.transform.mat4);

            if (node.mesh) {
                const program = this.shader_programs[node.mesh.format];
                program.bind();

                program.set_transform_uniform(mat);

                if (node.mesh.texture) {
                    gl.activeTexture(gl.TEXTURE0);
                    node.mesh.texture.bind(gl);
                    program.set_texture_uniform(gl.TEXTURE0);
                }

                node.mesh.render(gl);

                node.mesh.texture?.unbind(gl);
                program.unbind();
            }

            return mat;
        }, camera_project_mat);
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
