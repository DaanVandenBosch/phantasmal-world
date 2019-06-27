import fs from "fs";
import { BufferCursor } from "../src/bin_data/BufferCursor";
import { parse_rlc } from "../src/bin_data/parsing/rlc";
import { parse_njm_4 } from "../src/bin_data/parsing/ninja/motion";
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
    let i = 0;

    for (const file of parse_rlc(new BufferCursor(buf, false))) {
        const action = parse_njm_4(file);
        const nmdm = new BufferCursor(file.size + 8);

        nmdm.write_string_ascii("NMDM", 4);
        nmdm.seek(4); // File size placeholder.
        nmdm.write_u8_array([0xC, 0, 0, 0]);
        nmdm.write_u32(action.motion.frame_count);
        nmdm.write_u8_array([3, 0, 2, 0, 0xC, 4, 0, 0]);
        nmdm.seek(32);

        fs.writeFileSync(
            `${RESOURCE_DIR}/plymotiondata/plymotion_${(i++).toString().padStart(3, '0')}.njm`,
            file.uint8_array_view()
        );
    }
}
