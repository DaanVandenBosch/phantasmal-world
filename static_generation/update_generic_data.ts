import fs from "fs";
import Logger from "js-logger";
import { Endianness } from "../src/data_formats";
import { BufferCursor } from "../src/data_formats/cursor/BufferCursor";
import { parse_rlc } from "../src/data_formats/parsing/rlc";

const logger = Logger.get("static/update_generic_data");

Logger.useDefaults({ defaultLevel: Logger.TRACE });

/**
 * Used by static data generation scripts.
 */
const RESOURCE_DIR = "./static/resources";
/**
 * Used by production code.
 */
const PUBLIC_DIR = "./public";

update();

function update(): void {
    logger.info("Updating generic static data.");

    logger.info("Extracting player animations.");

    const buf = fs.readFileSync(`${RESOURCE_DIR}/plymotiondata.rlc`);
    let i = 0;

    for (const file of parse_rlc(new BufferCursor(buf, Endianness.Big))) {
        fs.writeFileSync(
            `${PUBLIC_DIR}/player/animation/animation_${(i++).toString().padStart(3, "0")}.njm`,
            new Uint8Array(file.array_buffer())
        );
    }

    logger.info("Done updating generic static data.");
}
