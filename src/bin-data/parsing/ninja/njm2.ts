import Logger from 'js-logger';
import { BufferCursor } from '../../BufferCursor';

const logger = Logger.get('bin-data/parsing/ninja/njm2');

export type NjAction = {
    object_offset: number,
    motion: NjMotion
}

export type NjMotion = {
    motion_data: NjMotionData[],
    frame_count: number,
    type: number,
    interpolation: number,
    element_count: number,
}

export type NjMotionData = {
    keyframes: NjKeyframe[][],
    keyframe_count: number[],
}

export type NjKeyframe = NjKeyframeF | NjKeyframeA

/**
 * Used for parallel motion (POS), scale (SCL) and vector (VEC).
 */
export type NjKeyframeF = {
    frame: number,
    value: [number, number, number],
}

/**
 * Used for rotation (ANG).
 */
export type NjKeyframeA = {
    frame: number,
    value: [number, number, number],
}

/**
 * Format used by plymotiondata.rlc.
 */
export function parse_njm2(cursor: BufferCursor): NjAction {
    cursor.seek_end(16);
    const offset1 = cursor.u32();
    log_offset('offset1', offset1);
    cursor.seek_start(offset1);
    const action_offset = cursor.u32();
    log_offset('action_offset', action_offset);
    cursor.seek_start(action_offset);
    return parse_action(cursor);
}

function parse_action(cursor: BufferCursor): NjAction {
    const object_offset = cursor.u32();
    const motion_offset = cursor.u32();
    log_offset('object offset', object_offset);
    log_offset('motion offset', motion_offset);
    cursor.seek_start(motion_offset);
    const motion = parse_motion(cursor);

    return {
        object_offset,
        motion
    };
}

function parse_motion(cursor: BufferCursor): NjMotion {
    // Points to an array the size of the total amount of objects in the object tree.
    const mdata_offset = cursor.u32();
    const frame_count = cursor.u32();
    const type = cursor.u16();
    const inp_fn = cursor.u16();
    // Linear, spline, user function or sampling mask.
    const interpolation = (inp_fn & 0b11000000) >> 6;
    const element_count = inp_fn & 0b1111;

    let motion_data: NjMotionData = {
        keyframes: [],
        keyframe_count: [],
    };

    const size = count_set_bits(type);
    cursor.seek_start(mdata_offset);
    const keyframe_offsets: number[] = [];
    const keyframe_counts: number[] = [];

    for (let i = 0; i < size; i++) {
        keyframe_offsets.push(cursor.u32());
    }

    for (let i = 0; i < size; i++) {
        const count = cursor.u32();
        motion_data.keyframe_count.push(count);
        keyframe_counts.push(count);
    }

    // NJD_MTYPE_POS_0
    if ((type & (1 << 0)) !== 0) {
        cursor.seek_start(keyframe_offsets.shift()!);
        motion_data.keyframes.push(
            parse_motion_data_f(cursor, keyframe_counts.shift()!)
        );
    }

    // NJD_MTYPE_ANG_1
    if ((type & (1 << 1)) !== 0) {
        cursor.seek_start(keyframe_offsets.shift()!);
        motion_data.keyframes.push(
            parse_motion_data_a(cursor, keyframe_counts.shift()!)
        );
    }

    // NJD_MTYPE_SCL_2
    if ((type & (1 << 2)) !== 0) {
        cursor.seek_start(keyframe_offsets.shift()!);
        motion_data.keyframes.push(
            parse_motion_data_f(cursor, keyframe_counts.shift()!)
        );
    }

    // NJD_MTYPE_VEC_3
    if ((type & (1 << 3)) !== 0) {
        cursor.seek_start(keyframe_offsets.shift()!);
        motion_data.keyframes.push(
            parse_motion_data_f(cursor, keyframe_counts.shift()!)
        );
    }

    // NJD_MTYPE_TARGET_3
    if ((type & (1 << 6)) !== 0) {
        cursor.seek_start(keyframe_offsets.shift()!);
        motion_data.keyframes.push(
            parse_motion_data_f(cursor, keyframe_counts.shift()!)
        );
    }

    // TODO: all NJD_MTYPE's

    return {
        motion_data: [motion_data],
        frame_count,
        type,
        interpolation,
        element_count
    };
}

function parse_motion_data_f(cursor: BufferCursor, count: number): NjKeyframeF[] {
    const frames: NjKeyframeF[] = [];

    for (let i = 0; i < count; ++i) {
        frames.push({
            frame: cursor.u32(),
            value: [cursor.f32(), cursor.f32(), cursor.f32()],
        });
    }

    return frames;
}

function parse_motion_data_a(cursor: BufferCursor, count: number): NjKeyframeA[] {
    const frames: NjKeyframeA[] = [];

    for (let i = 0; i < count; ++i) {
        frames.push({
            frame: cursor.u16(),
            value: [cursor.i16(), cursor.i16(), cursor.i16()],
        });
    }

    return frames;
}

function log_offset(name: string, offset: number) {
    logger.debug(`${name}: 0x${offset.toString(16).toUpperCase()}`);
}

function count_set_bits(n: number): number {
    let count = 0;

    while (n) {
        count += n & 1;
        n >>= 1;
    }

    return count;
}
