import { mat4_product } from "../../math";
import { ShaderProgram } from "../ShaderProgram";
import pos_vert_shader_source from "./pos.vert";
import pos_frag_shader_source from "./pos.frag";
import pos_tex_vert_shader_source from "./pos_tex.vert";
import pos_tex_frag_shader_source from "./pos_tex.frag";
import { GfxRenderer } from "../GfxRenderer";
import { WebglGfx, WebglMesh } from "./WebglGfx";

export class WebglRenderer extends GfxRenderer {
    private readonly gl: WebGL2RenderingContext;
    private readonly shader_programs: ShaderProgram[];

    readonly gfx: WebglGfx;

    constructor() {
        super();

        const gl = this.canvas_element.getContext("webgl2");

        if (gl == null) {
            throw new Error("Failed to initialize webgl2 context.");
        }

        this.gl = gl;
        this.gfx = new WebglGfx(gl);

        gl.enable(gl.DEPTH_TEST);
        gl.enable(gl.CULL_FACE);
        gl.clearColor(0.1, 0.1, 0.1, 1);

        this.shader_programs = [
            new ShaderProgram(gl, pos_vert_shader_source, pos_frag_shader_source),
            new ShaderProgram(gl, pos_tex_vert_shader_source, pos_tex_frag_shader_source),
        ];

        this.set_size(800, 600);
    }

    dispose(): void {
        for (const program of this.shader_programs) {
            program.delete();
        }

        super.dispose();
    }

    set_size(width: number, height: number): void {
        this.canvas_element.width = width;
        this.canvas_element.height = height;
        this.gl.viewport(0, 0, width, height);

        super.set_size(width, height);
    }

    protected render(): void {
        const gl = this.gl;

        gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);

        const camera_project_mat = mat4_product(this.projection_mat, this.camera.transform.mat4);

        this.scene.traverse((node, parent_mat) => {
            const mat = mat4_product(parent_mat, node.transform.mat4);

            if (node.mesh) {
                const program = this.shader_programs[node.mesh.format];
                program.bind();

                program.set_transform_uniform(mat);

                if (node.mesh.texture?.gfx_texture) {
                    gl.activeTexture(gl.TEXTURE0);
                    gl.bindTexture(gl.TEXTURE_2D, node.mesh.texture.gfx_texture as WebGLTexture);
                    program.set_texture_uniform(gl.TEXTURE0);
                }

                const gfx_mesh = node.mesh.gfx_mesh as WebglMesh;
                gl.bindVertexArray(gfx_mesh.vao);
                gl.drawElements(gl.TRIANGLES, node.mesh.index_count, gl.UNSIGNED_SHORT, 0);
                gl.bindVertexArray(null);

                gl.bindTexture(gl.TEXTURE_2D, null);

                program.unbind();
            }

            return mat;
        }, camera_project_mat);
    }
}
