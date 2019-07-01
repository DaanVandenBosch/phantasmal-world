import { Vec3 } from '../../../domain';
import { BufferCursor } from '../../BufferCursor';
import { NjModel, parse_nj_model } from './nj';
import { parse_xj_model, XjModel } from './xj';

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

export class NinjaObject<M extends NinjaModel>  {
    private bone_cache = new Map<number, NinjaObject<M> | null>();
    private _bone_count = -1;

    constructor(
        public evaluation_flags: {
            no_translate: boolean,
            no_rotate: boolean,
            no_scale: boolean,
            hidden: boolean,
            break_child_trace: boolean,
            zxy_rotation_order: boolean,
            skip: boolean,
            shape_skip: boolean,
        },
        public model: M | undefined,
        public position: Vec3,
        public rotation: Vec3, // Euler angles in radians.
        public scale: Vec3,
        public children: NinjaObject<M>[]
    ) { }

    bone_count(): number {
        if (this._bone_count === -1) {
            const id_ref: [number] = [0];
            this.get_bone_internal(this, Infinity, id_ref);
            this._bone_count = id_ref[0];
        }

        return this._bone_count;
    }

    get_bone(bone_id: number): NinjaObject<M> | undefined {
        let bone = this.bone_cache.get(bone_id);

        // Strict check because null means there's no bone with this id.
        if (bone === undefined) {
            bone = this.get_bone_internal(this, bone_id, [0]);
            this.bone_cache.set(bone_id, bone || null);
        }

        return bone || undefined;
    }

    private get_bone_internal(
        object: NinjaObject<M>,
        bone_id: number,
        id_ref: [number]
    ): NinjaObject<M> | undefined {
        if (!object.evaluation_flags.skip) {
            const id = id_ref[0]++;
            this.bone_cache.set(id, object);

            if (id === bone_id) {
                return object;
            }
        }

        if (!object.evaluation_flags.break_child_trace) {
            for (const child of object.children) {
                const bone = this.get_bone_internal(child, bone_id, id_ref);
                if (bone) return bone;
            }
        }
    }
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
            if (iff_chunk_size > cursor.bytes_left) {
                break;
            }

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
    const skip = (eval_flags & 0b1000000) !== 0;
    const shape_skip = (eval_flags & 0b10000000) !== 0;

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

    const object = new NinjaObject<M>(
        {
            no_translate,
            no_rotate,
            no_scale,
            hidden,
            break_child_trace,
            zxy_rotation_order,
            skip,
            shape_skip,
        },
        model,
        new Vec3(pos_x, pos_y, pos_z),
        new Vec3(rotation_x, rotation_y, rotation_z),
        new Vec3(scale_x, scale_y, scale_z),
        children
    );

    return [object, ...siblings];
}
