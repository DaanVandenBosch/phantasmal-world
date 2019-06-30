import { PlayerModel } from "../../domain";
import { BufferCursor } from "../BufferCursor";
import { NinjaModel, NinjaObject, parse_nj } from "../parsing/ninja";
import { get_player_data } from "./binary_assets";

const cache: Map<string, Promise<NinjaObject<NinjaModel>>> = new Map();

export function get_player_ninja_object(model: PlayerModel): Promise<NinjaObject<NinjaModel>> {
    let ninja_object = cache.get(model.name);

    if (ninja_object) {
        return ninja_object;
    } else {
        ninja_object = get_all_assets(model);
        cache.set(model.name, ninja_object);
        return ninja_object;
    }
}

async function get_all_assets(model: PlayerModel): Promise<NinjaObject<NinjaModel>> {
    const body_data = await get_player_data(model.name, 'Body');
    const body = parse_nj(new BufferCursor(body_data, true))[0];

    if (!body) {
        throw new Error(`Couldn't parse body for player class ${model.name}.`);
    }

    const head_data = await get_player_data(model.name, 'Head', 0);
    const head = parse_nj(new BufferCursor(head_data, true))[0];

    if (head) {
        add_to_bone(body, head, 59);
    }

    if (model.hair_styles_count > 0) {
        const hair_data = await get_player_data(model.name, 'Hair', 0);
        const hair = parse_nj(new BufferCursor(hair_data, true))[0];

        if (hair) {
            add_to_bone(body, hair, 59);
        }

        if (model.hair_styles_with_accessory.has(0)) {
            const accessory_data = await get_player_data(model.name, 'Accessory', 0);
            const accessory = parse_nj(new BufferCursor(accessory_data, true))[0];

            if (accessory) {
                add_to_bone(body, accessory, 59);
            }
        }
    }

    return body;
}

function add_to_bone(
    object: NinjaObject<NinjaModel>,
    head_part: NinjaObject<NinjaModel>,
    bone_id: number,
    id_ref: [number] = [0]
) {
    if (!object.evaluation_flags.eval_skip) {
        const id = id_ref[0]++;

        if (id === bone_id) {
            object.evaluation_flags.hidden = false;
            object.evaluation_flags.break_child_trace = false;
            object.children.push(head_part);
            return;
        }
    }

    for (const child of object.children) {
        add_to_bone(child, head_part, bone_id, id_ref);
    }
}
