import { Mat3, Mat4 } from "../math/linear_algebra";
import { VERTEX_NORMAL_LOC, VERTEX_POS_LOC, VERTEX_TEX_LOC } from "./VertexFormat";

export class ShaderProgram {
    private readonly gl: WebGL2RenderingContext;
    private readonly program: WebGLProgram;
    private readonly mat_projection_loc: WebGLUniformLocation;
    private readonly mat_model_view_loc: WebGLUniformLocation;
    private readonly mat_normal_loc: WebGLUniformLocation | null;
    private readonly tex_sampler_loc: WebGLUniformLocation | null;

    constructor(gl: WebGL2RenderingContext, vertex_source: string, frag_source: string) {
        this.gl = gl;
        const program = gl.createProgram();
        if (program == null) throw new Error("Failed to create program.");
        this.program = program;

        let vertex_shader: WebGLShader | null = null;
        let frag_shader: WebGLShader | null = null;

        try {
            vertex_shader = create_shader(gl, gl.VERTEX_SHADER, vertex_source);
            gl.attachShader(program, vertex_shader);

            frag_shader = create_shader(gl, gl.FRAGMENT_SHADER, frag_source);
            gl.attachShader(program, frag_shader);

            gl.bindAttribLocation(program, VERTEX_POS_LOC, "pos");
            gl.bindAttribLocation(program, VERTEX_NORMAL_LOC, "normal");
            gl.bindAttribLocation(program, VERTEX_TEX_LOC, "tex");
            gl.linkProgram(program);

            if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
                const log = gl.getProgramInfoLog(program);
                throw new Error("Shader linking failed. Program log:\n" + log);
            }

            this.mat_projection_loc = this.get_required_uniform_location(program, "mat_projection");
            this.mat_model_view_loc = this.get_required_uniform_location(program, "mat_model_view");
            this.mat_normal_loc = gl.getUniformLocation(program, "mat_normal");

            this.tex_sampler_loc = gl.getUniformLocation(program, "tex_sampler");

            gl.detachShader(program, vertex_shader);
            gl.detachShader(program, frag_shader);
        } catch (e) {
            gl.deleteProgram(program);
            throw e;
        } finally {
            // Always delete shaders after we're done.
            gl.deleteShader(vertex_shader);
            gl.deleteShader(frag_shader);
        }
    }

    set_mat_projection_uniform(matrix: Mat4): void {
        this.gl.uniformMatrix4fv(this.mat_projection_loc, false, matrix.data);
    }

    set_mat_model_view_uniform(matrix: Mat4): void {
        this.gl.uniformMatrix4fv(this.mat_model_view_loc, false, matrix.data);
    }

    set_mat_normal_uniform(matrix: Mat3): void {
        this.gl.uniformMatrix3fv(this.mat_normal_loc, false, matrix.data);
    }

    set_texture_uniform(unit: GLenum): void {
        this.gl.uniform1i(this.tex_sampler_loc, unit - this.gl.TEXTURE0);
    }

    bind(): void {
        this.gl.useProgram(this.program);
    }

    unbind(): void {
        this.gl.useProgram(null);
    }

    delete(): void {
        this.gl.deleteProgram(this.program);
    }

    private get_required_uniform_location(
        program: WebGLProgram,
        uniform: string,
    ): WebGLUniformLocation {
        const loc = this.gl.getUniformLocation(program, uniform);
        if (loc == null) throw new Error(`Couldn't get ${uniform} uniform location.`);
        return loc;
    }
}

function create_shader(gl: WebGL2RenderingContext, type: GLenum, source: string): WebGLShader {
    const shader = gl.createShader(type);
    if (shader == null) throw new Error(`Failed to create shader of type ${type}.`);

    gl.shaderSource(shader, source);
    gl.compileShader(shader);

    if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
        const log = gl.getShaderInfoLog(shader);
        gl.deleteShader(shader);

        throw new Error("Vertex shader compilation failed. Shader log:\n" + log);
    }

    return shader;
}
