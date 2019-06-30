import { BufferCursor } from '../../BufferCursor';
import { parse_nj_model, NjModel } from './nj';
import { parse_xj_model, XjModel } from './xj';
import { Vec3 } from '../../../domain';

// TODO:
// - deal with multiple NJCM chunks
// - deal with other types of chunks

const ANGLE_TO_RAD = 2 * Math.PI / 65536;

export type NinjaVertex = {
    position: Vec3,
    normal?: Vec3,
    bone_weight: number,
    bone_weight_status: number,
    calc_continue: boolean
}

export type NinjaModel = NjModel | XjModel;

export type NinjaObject<M extends NinjaModel> = {
    evaluation_flags: {
        no_translate: boolean,
        no_rotate: boolean,
        no_scale: boolean,
        hidden: boolean,
        break_child_trace: boolean,
        zxy_rotation_order: boolean,
        eval_skip: boolean,
        eval_shape_skip: boolean,
    },
    model?: M,
    position: Vec3,
    rotation: Vec3, // Euler angles in radians.
    scale: Vec3,
    children: NinjaObject<M>[],
}

export function parse_nj(cursor: BufferCursor): NinjaObject<NjModel>[] {
    return parse_ninja(cursor, parse_nj_model, []);
}

export function parse_xj(cursor: BufferCursor): NinjaObject<XjModel>[] {
    return parse_ninja(cursor, parse_xj_model, undefined);
}

function parse_ninja<M extends NinjaModel>(
    cursor: BufferCursor,
    parse_model: (cursor: BufferCursor, context: any) => M,
    context: any
): NinjaObject<M>[] {
    while (cursor.bytes_left) {
        // Ninja uses a little endian variant of the IFF format.
        // IFF files contain chunks preceded by an 8-byte header.
        // The header consists of 4 ASCII characters for the "Type ID" and a 32-bit integer specifying the chunk size.
        const iff_type_id = cursor.string_ascii(4, false, false);
        const iff_chunk_size = cursor.u32();

        if (iff_type_id === 'NJCM') {
            return parse_sibling_objects(cursor.take(iff_chunk_size), parse_model, context);
        } else {
            cursor.seek(iff_chunk_size);
        }
    }

    return [];
}

// TODO: cache model and object offsets so we don't reparse the same data.
function parse_sibling_objects<M extends NinjaModel>(
    cursor: BufferCursor,
    parse_model: (cursor: BufferCursor, context: any) => M,
    context: any
): NinjaObject<M>[] {
    const eval_flags = cursor.u32();
    const no_translate = (eval_flags & 0b1) !== 0;
    const no_rotate = (eval_flags & 0b10) !== 0;
    const no_scale = (eval_flags & 0b100) !== 0;
    const hidden = (eval_flags & 0b1000) !== 0;
    const break_child_trace = (eval_flags & 0b10000) !== 0;
    const zxy_rotation_order = (eval_flags & 0b100000) !== 0;
    const eval_skip = (eval_flags & 0b1000000) !== 0;
    const eval_shape_skip = (eval_flags & 0b1000000) !== 0;

    const model_offset = cursor.u32();
    const pos_x = cursor.f32();
    const pos_y = cursor.f32();
    const pos_z = cursor.f32();
    const rotation_x = cursor.i32() * ANGLE_TO_RAD;
    const rotation_y = cursor.i32() * ANGLE_TO_RAD;
    const rotation_z = cursor.i32() * ANGLE_TO_RAD;
    const scale_x = cursor.f32();
    const scale_y = cursor.f32();
    const scale_z = cursor.f32();
    const child_offset = cursor.u32();
    const sibling_offset = cursor.u32();

    let model: M | undefined;
    let children: NinjaObject<M>[];
    let siblings: NinjaObject<M>[];

    if (model_offset) {
        cursor.seek_start(model_offset);
        model = parse_model(cursor, context);
    }

    if (child_offset) {
        cursor.seek_start(child_offset);
        children = parse_sibling_objects(cursor, parse_model, context);
    } else {
        children = [];
    }

    if (sibling_offset) {
        cursor.seek_start(sibling_offset);
        siblings = parse_sibling_objects(cursor, parse_model, context);
    } else {
        siblings = [];
    }

    const object: NinjaObject<M> = {
        evaluation_flags: {
            no_translate,
            no_rotate,
            no_scale,
            hidden,
            break_child_trace,
            zxy_rotation_order,
            eval_skip,
            eval_shape_skip,
        },
        model,
        position: new Vec3(pos_x, pos_y, pos_z),
        rotation: new Vec3(rotation_x, rotation_y, rotation_z),
        scale: new Vec3(scale_x, scale_y, scale_z),
        children,
    };

    return [object, ...siblings];
}
