import { Renderer } from "./Renderer";
import { Matrix4, matrix4_product } from "../math";
import { ShaderProgram } from "./ShaderProgram";
import { GL } from "./VertexFormat";
import { Scene } from "./Scene";
import {
    POS_FRAG_SHADER_SOURCE,
    POS_TEX_FRAG_SHADER_SOURCE,
    POS_TEX_VERTEX_SHADER_SOURCE,
    POS_VERTEX_SHADER_SOURCE,
} from "./shader_sources";
import { LogManager } from "../Logger";

const logger = LogManager.get("core/rendering/WebglRenderer");

export class WebglRenderer extends Renderer {
    private readonly gl: GL;
    private readonly shader_programs: ShaderProgram[];
    private render_scheduled = false;
    private projection!: Matrix4;

    protected readonly scene: Scene;

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

        this.shader_programs = [
            new ShaderProgram(gl, POS_VERTEX_SHADER_SOURCE, POS_FRAG_SHADER_SOURCE),
            new ShaderProgram(gl, POS_TEX_VERTEX_SHADER_SOURCE, POS_TEX_FRAG_SHADER_SOURCE),
        ];

        this.scene = new Scene(gl);

        this.set_size(800, 600);

        requestAnimationFrame(this.render);
    }

    dispose(): void {
        for (const program of this.shader_programs) {
            program.delete();
        }

        this.scene.delete();
    }

    start_rendering(): void {
        // TODO
    }

    stop_rendering(): void {
        // TODO
    }

    schedule_render = (): void => {
        this.render_scheduled = true;
    };

    set_size(width: number, height: number): void {
        this.canvas_element.width = width;
        this.canvas_element.height = height;
        this.gl.viewport(0, 0, width, height);

        // prettier-ignore
        this.projection = Matrix4.of(
            2/width, 0,        0,    0,
            0,       2/height, 0,    0,
            0,       0,        2/10, 0,
            0,       0,        0,    1,
        );
    }

    private render = (): void => {
        const gl = this.gl;

        gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);

        this.scene.traverse((node, parent_transform) => {
            const transform = matrix4_product(parent_transform, node.transform.matrix4);

            if (node.mesh) {
                const program = this.shader_programs[node.mesh.format];
                program.bind();

                program.set_transform(transform);

                if (node.mesh.texture) {
                    gl.activeTexture(gl.TEXTURE0);
                    node.mesh.texture.bind(gl);
                    program.set_texture(node.mesh.texture);
                }

                node.mesh.render(gl);

                node.mesh.texture?.unbind(gl);
                program.unbind();
            }

            return transform;
        }, this.projection);

        requestAnimationFrame(this.render);
    };
}
