/* eslint-disable no-console */
import glsl_module, { Glslang, ShaderStage } from "@webgpu/glslang";
import * as fs from "fs";
import { RESOURCE_DIR, ASSETS_DIR } from "./index";
const glsl = (glsl_module() as any) as Glslang;

const SHADER_RESOURCES_DIR = `${RESOURCE_DIR}/shaders`;
const SHADER_ASSETS_DIR = `${ASSETS_DIR}/shaders`;

function compile_shader(source_file: string, shader_stage: ShaderStage): void {
    const source = fs.readFileSync(`${SHADER_RESOURCES_DIR}/${source_file}`, "utf8");
    const spir_v = glsl.compileGLSL(source, shader_stage, true);
    fs.writeFileSync(
        `${SHADER_ASSETS_DIR}/${source_file}.spv`,
        new Uint8Array(spir_v.buffer, spir_v.byteOffset, spir_v.byteLength),
    );
}

for (const file of fs.readdirSync(SHADER_RESOURCES_DIR)) {
    console.info(`Compiling ${file}.`);

    let shader_stage: ShaderStage;

    switch (file.slice(-4)) {
        case "vert":
            shader_stage = "vertex";
            break;
        case "frag":
            shader_stage = "fragment";
            break;
        default:
            throw new Error(`Unsupported shader type: ${file.slice(-4)}`);
    }

    compile_shader(file, shader_stage);
}
