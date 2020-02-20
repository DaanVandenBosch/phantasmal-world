import { Mat4, mat4_multiply } from "../../math/linear_algebra";
import { ShaderProgram } from "../ShaderProgram";
import pos_norm_vert_shader_source from "./pos_norm.vert";
import pos_norm_frag_shader_source from "./pos_norm.frag";
import pos_tex_vert_shader_source from "./pos_tex.vert";
import pos_tex_frag_shader_source from "./pos_tex.frag";
import { GfxRenderer } from "../GfxRenderer";
import { WebglGfx, WebglMesh } from "./WebglGfx";
import { Projection } from "../Camera";
import { VertexFormat } from "../VertexFormat";
import { SceneNode } from "../Scene";

export class WebglRenderer extends GfxRenderer {
    private readonly gl: WebGL2RenderingContext;
    private readonly shader_programs: ShaderProgram[];

    readonly gfx: WebglGfx;

    constructor(projection: Projection) {
        super(projection);

        const gl = this.canvas_element.getContext("webgl2");

        if (gl == null) {
            throw new Error("Failed to initialize webgl2 context.");
        }

        this.gl = gl;
        this.gfx = new WebglGfx(gl);

        gl.enable(gl.DEPTH_TEST);
        gl.clearColor(0.1, 0.1, 0.1, 1);

        this.shader_programs = [];
        this.shader_programs[VertexFormat.PosNorm] = new ShaderProgram(
            gl,
            pos_norm_vert_shader_source,
            pos_norm_frag_shader_source,
        );
        this.shader_programs[VertexFormat.PosTex] = new ShaderProgram(
            gl,
            pos_tex_vert_shader_source,
            pos_tex_frag_shader_source,
        );

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

        // this.render_node(this.scene.root_node, this.camera.view_matrix);

        this.scene.traverse((node, parent_mat) => {
            const mat = mat4_multiply(parent_mat, node.transform);

            if (node.mesh) {
                const program = this.shader_programs[node.mesh.format];
                program.bind();

                program.set_mat_projection_uniform(this.camera.projection_matrix);
                program.set_mat_model_view_uniform(mat);
                program.set_mat_normal_uniform(mat.normal_mat3());

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
        }, this.camera.view_matrix);
    }

    private render_node(node: SceneNode, parent_mat: Mat4): void {
        const gl = this.gl;
        const mat = mat4_multiply(parent_mat, node.transform);

        if (node.mesh) {
            const program = this.shader_programs[node.mesh.format];
            program.bind();

            program.set_mat_projection_uniform(this.camera.projection_matrix);
            program.set_mat_model_view_uniform(mat);
            program.set_mat_normal_uniform(mat.normal_mat3());

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

        for (const child of node.children) {
            this.render_node(child, mat);
        }
    }
}
