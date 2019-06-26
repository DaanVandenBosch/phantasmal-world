import {
    BufferAttribute,
    BufferGeometry,
    Euler,
    Matrix4,
    Quaternion,
    Vector3
} from 'three';
import { BufferCursor } from '../../BufferCursor';
import { parseNjModel, NjContext } from './nj';
import { parseXjModel, XjContext } from './xj';

// TODO:
// - deal with multiple NJCM chunks
// - deal with other types of chunks

export function parseNj(cursor: BufferCursor): BufferGeometry | undefined {
    return parseNinja(cursor, 'nj');
}

export function parseXj(cursor: BufferCursor): BufferGeometry | undefined {
    return parseNinja(cursor, 'xj');
}

type Format = 'nj' | 'xj';
type Context = NjContext | XjContext;

function parseNinja(cursor: BufferCursor, format: Format): BufferGeometry | undefined {
    while (cursor.bytes_left) {
        // Ninja uses a little endian variant of the IFF format.
        // IFF files contain chunks preceded by an 8-byte header.
        // The header consists of 4 ASCII characters for the "Type ID" and a 32-bit integer specifying the chunk size.
        const iffTypeId = cursor.string_ascii(4, false, false);
        const iffChunkSize = cursor.u32();

        if (iffTypeId === 'NJCM') {
            return parseNjcm(cursor.take(iffChunkSize), format);
        } else {
            cursor.seek(iffChunkSize);
        }
    }
}

function parseNjcm(cursor: BufferCursor, format: Format): BufferGeometry | undefined {
    if (cursor.bytes_left) {
        let context: Context;

        if (format === 'nj') {
            context = {
                format,
                positions: [],
                normals: [],
                cachedChunkOffsets: [],
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

        parseSiblingObjects(cursor, new Matrix4(), context);
        return createBufferGeometry(context);
    }
}

function parseSiblingObjects(
    cursor: BufferCursor,
    parentMatrix: Matrix4,
    context: Context
): void {
    const evalFlags = cursor.u32();
    const noTranslate = (evalFlags & 0b1) !== 0;
    const noRotate = (evalFlags & 0b10) !== 0;
    const noScale = (evalFlags & 0b100) !== 0;
    const hidden = (evalFlags & 0b1000) !== 0;
    const breakChildTrace = (evalFlags & 0b10000) !== 0;
    const zxyRotationOrder = (evalFlags & 0b100000) !== 0;

    const modelOffset = cursor.u32();
    const posX = cursor.f32();
    const posY = cursor.f32();
    const posZ = cursor.f32();
    const rotationX = cursor.i32() * (2 * Math.PI / 0xFFFF);
    const rotationY = cursor.i32() * (2 * Math.PI / 0xFFFF);
    const rotationZ = cursor.i32() * (2 * Math.PI / 0xFFFF);
    const scaleX = cursor.f32();
    const scaleY = cursor.f32();
    const scaleZ = cursor.f32();
    const childOffset = cursor.u32();
    const siblingOffset = cursor.u32();

    const rotation = new Euler(rotationX, rotationY, rotationZ, zxyRotationOrder ? 'ZXY' : 'ZYX');
    const matrix = new Matrix4()
        .compose(
            noTranslate ? new Vector3() : new Vector3(posX, posY, posZ),
            noRotate ? new Quaternion(0, 0, 0, 1) : new Quaternion().setFromEuler(rotation),
            noScale ? new Vector3(1, 1, 1) : new Vector3(scaleX, scaleY, scaleZ)
        )
        .premultiply(parentMatrix);

    if (modelOffset && !hidden) {
        cursor.seek_start(modelOffset);
        parseModel(cursor, matrix, context);
    }

    if (childOffset && !breakChildTrace) {
        cursor.seek_start(childOffset);
        parseSiblingObjects(cursor, matrix, context);
    }

    if (siblingOffset) {
        cursor.seek_start(siblingOffset);
        parseSiblingObjects(cursor, parentMatrix, context);
    }
}

function createBufferGeometry(context: Context): BufferGeometry {
    const geometry = new BufferGeometry();
    geometry.addAttribute('position', new BufferAttribute(new Float32Array(context.positions), 3));
    geometry.addAttribute('normal', new BufferAttribute(new Float32Array(context.normals), 3));

    if ('indices' in context) {
        geometry.setIndex(new BufferAttribute(new Uint16Array(context.indices), 1));
    }

    return geometry;
}

function parseModel(cursor: BufferCursor, matrix: Matrix4, context: Context): void {
    if (context.format === 'nj') {
        parseNjModel(cursor, matrix, context);
    } else {
        parseXjModel(cursor, matrix, context);
    }
}
