import fs from "fs";
import { BufferCursor } from "../src/bin-data/BufferCursor";
import { parse_rlc } from "../src/bin-data/parsing/rlc";
import { parse_njm2 } from "../src/bin-data/parsing/ninja/njm2";
import Logger from 'js-logger';

const logger = Logger.get('static/updateGenericData');

Logger.useDefaults({ defaultLevel: Logger.TRACE });

/**
 * Used by static data generation scripts.
 */
const RESOURCE_DIR = './static/resources';
/**
 * Used by production code.
 */
const PUBLIC_DIR = './public';

update();

function update() {
    const buf = fs.readFileSync(`${RESOURCE_DIR}/plymotiondata.rlc`);

    for (const file of parse_rlc(new BufferCursor(buf, false))) {
        logger.info(`Frame count: ${parse_njm2(file).motion.frame_count}`);
    }
}
