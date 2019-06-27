import {
    BufferAttribute,
    BufferGeometry,
    Euler,
    Matrix4,
    Quaternion,
    Vector3
} from 'three';
import { BufferCursor } from '../../BufferCursor';
import { parse_nj_model, NjContext } from './nj';
import { parse_xj_model, XjContext } from './xj';

// TODO:
// - deal with multiple NJCM chunks
// - deal with other types of chunks

export function parse_nj(cursor: BufferCursor): BufferGeometry | undefined {
    return parse_ninja(cursor, 'nj');
}

export function parse_xj(cursor: BufferCursor): BufferGeometry | undefined {
    return parse_ninja(cursor, 'xj');
}

type Format = 'nj' | 'xj';
type Context = NjContext | XjContext;

function parse_ninja(cursor: BufferCursor, format: Format): BufferGeometry | undefined {
    while (cursor.bytes_left) {
        // Ninja uses a little endian variant of the IFF format.
        // IFF files contain chunks preceded by an 8-byte header.
        // The header consists of 4 ASCII characters for the "Type ID" and a 32-bit integer specifying the chunk size.
        const iff_type_id = cursor.string_ascii(4, false, false);
        const iff_chunk_size = cursor.u32();

        if (iff_type_id === 'NJCM') {
            return parse_njcm(cursor.take(iff_chunk_size), format);
        } else {
            cursor.seek(iff_chunk_size);
        }
    }
}

function parse_njcm(cursor: BufferCursor, format: Format): BufferGeometry | undefined {
    if (cursor.bytes_left) {
        let context: Context;

        if (format === 'nj') {
            context = {
                format,
                positions: [],
                normals: [],
                cached_chunk_offsets: [],
                vertices: []
            };
        } else {
            context = {
                format,
                positions: [],
                normals: [],
                indices: []
            };
        }

        parse_sibling_objects(cursor, new Matrix4(), context);
        return create_buffer_geometry(context);
    }
}

function parse_sibling_objects(
    cursor: BufferCursor,
    parent_matrix: Matrix4,
    context: Context
): void {
    const eval_flags = cursor.u32();
    const no_translate = (eval_flags & 0b1) !== 0;
    const no_rotate = (eval_flags & 0b10) !== 0;
    const no_scale = (eval_flags & 0b100) !== 0;
    const hidden = (eval_flags & 0b1000) !== 0;
    const break_child_trace = (eval_flags & 0b10000) !== 0;
    const zxy_rotation_order = (eval_flags & 0b100000) !== 0;

    const model_offset = cursor.u32();
    const pos_x = cursor.f32();
    const pos_y = cursor.f32();
    const pos_z = cursor.f32();
    const rotation_x = cursor.i32() * (2 * Math.PI / 0xFFFF);
    const rotation_y = cursor.i32() * (2 * Math.PI / 0xFFFF);
    const rotation_z = cursor.i32() * (2 * Math.PI / 0xFFFF);
    const scale_x = cursor.f32();
    const scale_y = cursor.f32();
    const scale_z = cursor.f32();
    const child_offset = cursor.u32();
    const sibling_offset = cursor.u32();

    const rotation = new Euler(rotation_x, rotation_y, rotation_z, zxy_rotation_order ? 'ZXY' : 'ZYX');
    const matrix = new Matrix4()
        .compose(
            no_translate ? new Vector3() : new Vector3(pos_x, pos_y, pos_z),
            no_rotate ? new Quaternion(0, 0, 0, 1) : new Quaternion().setFromEuler(rotation),
            no_scale ? new Vector3(1, 1, 1) : new Vector3(scale_x, scale_y, scale_z)
        )
        .premultiply(parent_matrix);

    if (model_offset && !hidden) {
        cursor.seek_start(model_offset);
        parse_model(cursor, matrix, context);
    }

    if (child_offset && !break_child_trace) {
        cursor.seek_start(child_offset);
        parse_sibling_objects(cursor, matrix, context);
    }

    if (sibling_offset) {
        cursor.seek_start(sibling_offset);
        parse_sibling_objects(cursor, parent_matrix, context);
    }
}

function create_buffer_geometry(context: Context): BufferGeometry {
    const geometry = new BufferGeometry();
    geometry.addAttribute('position', new BufferAttribute(new Float32Array(context.positions), 3));
    geometry.addAttribute('normal', new BufferAttribute(new Float32Array(context.normals), 3));

    if ('indices' in context) {
        geometry.setIndex(new BufferAttribute(new Uint16Array(context.indices), 1));
    }

    return geometry;
}

function parse_model(cursor: BufferCursor, matrix: Matrix4, context: Context): void {
    if (context.format === 'nj') {
        parse_nj_model(cursor, matrix, context);
    } else {
        parse_xj_model(cursor, matrix, context);
    }
}
