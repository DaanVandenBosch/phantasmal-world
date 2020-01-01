import { readFileSync, writeFileSync } from "fs";
import { ASSETS_DIR, RESOURCE_DIR, SRC_DIR } from ".";
import { BufferCursor } from "../src/core/data_formats/cursor/BufferCursor";
import { parse_rlc } from "../src/core/data_formats/parsing/rlc";
import * as yaml from "yaml";
import { Endianness } from "../src/core/data_formats/Endianness";
import { LogLevel, LogManager } from "../src/core/Logger";

const logger = LogManager.get("assets_generation/update_generic_data");

LogManager.default_level = LogLevel.Trace;

const OPCODES_YML_FILE = `${RESOURCE_DIR}/scripting/opcodes.yml`;
const OPCODES_SRC_FILE = `${SRC_DIR}/quest_editor/scripting/opcodes.ts`;

update();

function update(): void {
    logger.info("Updating generic static data.");

    update_opcodes();
    extract_player_animations();

    logger.info("Done updating generic static data.");
}

function extract_player_animations(): void {
    logger.info("Extracting player animations.");

    const buf = readFileSync(`${RESOURCE_DIR}/plymotiondata.rlc`);
    let i = 0;

    for (const file of parse_rlc(new BufferCursor(buf, Endianness.Big))) {
        writeFileSync(
            `${ASSETS_DIR}/player/animation/animation_${(i++).toString().padStart(3, "0")}.njm`,
            new Uint8Array(file.array_buffer()),
        );
    }

    logger.info("Done extracting player animations.");
}

function update_opcodes(): void {
    logger.info("Generating opcodes.");

    // Add manual code.
    const opcodes_src = readFileSync(OPCODES_SRC_FILE, {
        encoding: "UTF-8",
    });
    const file_lines: string[] = [];
    let in_manual_code = true;
    let generated_lines_insert_point = 0;

    opcodes_src.split("\n").forEach((line, i) => {
        if (in_manual_code) {
            if (line.includes("!!! GENERATED_CODE_START !!!")) {
                in_manual_code = false;
                generated_lines_insert_point = i + 1;
            }

            file_lines.push(line);
        } else {
            if (line.includes("!!! GENERATED_CODE_END !!!")) {
                in_manual_code = true;
                file_lines.push(line);
            }
        }
    });

    // Add generated code.
    const yml = readFileSync(OPCODES_YML_FILE, { encoding: "UTF-8" });
    const input = yaml.parse(yml);
    const generated_lines: string[] = [];
    let i = 0;

    for (let code = 0; code <= 0xff; code++) {
        const opcode = input.opcodes[i];

        if (opcode && opcode.code === code) {
            opcode_to_code(generated_lines, code, opcode);
            i++;
        } else {
            opcode_to_code(generated_lines, code);
        }
    }

    for (let code = 0xf800; code <= 0xf9ff; code++) {
        const opcode = input.opcodes[i];

        if (opcode && opcode.code === code) {
            opcode_to_code(generated_lines, code, opcode);
            i++;
        } else {
            opcode_to_code(generated_lines, code);
        }
    }

    // Write final file.
    file_lines.splice(generated_lines_insert_point, 0, ...generated_lines);
    writeFileSync(OPCODES_SRC_FILE, file_lines.join("\n"));

    logger.info("Done generating opcodes.");
}

function opcode_to_code(output: string[], code: number, opcode?: any): void {
    const code_str = code.toString(16).padStart(code < 256 ? 2 : 4, "0");
    const mnemonic: string = (opcode && opcode.mnemonic) || `unknown_${code_str}`;
    const var_name =
        "OP_" +
        mnemonic
            .replace("!=", "ne")
            .replace("<=", "le")
            .replace(">=", "ge")
            .replace("<", "l")
            .replace(">", "g")
            .replace("=", "e")
            .toUpperCase();

    if (opcode) {
        const stack_interaction =
            opcode.stack === "push"
                ? "StackInteraction.Push"
                : opcode.stack === "pop"
                ? "StackInteraction.Pop"
                : "undefined";

        const params = params_to_code(opcode.params);

        output.push(`export const ${var_name} = (OPCODES[0x${code_str}] = new_opcode(
    0x${code_str},
    "${mnemonic}",
    ${(opcode.doc && JSON.stringify(opcode.doc)) || "undefined"},
    [${params}],
    ${stack_interaction}
));`);
    } else {
        output.push(`export const ${var_name} = (OPCODES[0x${code_str}] = new_opcode(
    0x${code_str},
    "${mnemonic}",
    undefined,
    [],
    undefined
));`);
    }
}

function params_to_code(params: any[]): string {
    return params
        .map((param: any) => {
            let type: string;

            switch (param.type) {
                case "any":
                    type = "TYPE_ANY";
                    break;
                case "byte":
                    type = "TYPE_BYTE";
                    break;
                case "word":
                    type = "TYPE_WORD";
                    break;
                case "dword":
                    type = "TYPE_DWORD";
                    break;
                case "float":
                    type = "TYPE_FLOAT";
                    break;
                case "label":
                    type = "TYPE_LABEL";
                    break;
                case "instruction_label":
                    type = "TYPE_I_LABEL";
                    break;
                case "data_label":
                    type = "TYPE_D_LABEL";
                    break;
                case "string_label":
                    type = "TYPE_S_LABEL";
                    break;
                case "string":
                    type = "TYPE_STRING";
                    break;
                case "instruction_label_var":
                    type = "TYPE_I_LABEL_VAR";
                    break;
                case "reg_ref":
                    type = "TYPE_REG_REF";
                    break;
                case "reg_tup_ref":
                    type = `{ kind: Kind.RegTupRef, register_tuples: [${params_to_code(
                        param.reg_tup,
                    )}] }`;
                    break;
                case "reg_ref_var":
                    type = "TYPE_REG_REF_VAR";
                    break;
                case "pointer":
                    type = "TYPE_POINTER";
                    break;
                default:
                    throw new Error(`Type ${param.type} not implemented.`);
            }

            const doc = (param.doc && JSON.stringify(param.doc)) || "undefined";
            let access: string;

            switch (param.access) {
                case "read":
                    access = "ParamAccess.Read";
                    break;
                case "write":
                    access = "ParamAccess.Write";
                    break;
                case "read_write":
                    access = "ParamAccess.ReadWrite";
                    break;
                default:
                    access = "undefined";
                    break;
            }

            return `new_param(${type}, ${doc}, ${access})`;
        })
        .join(", ");
}
