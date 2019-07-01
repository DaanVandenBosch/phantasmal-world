import { BufferCursor } from '../../BufferCursor';
import { Vec3 } from '../../../domain';

const ANGLE_TO_RAD = 2 * Math.PI / 65536;

export type NjAction = {
    object_offset: number,
    motion: NjMotion
}

export type NjMotion = {
    motion_data: NjMotionData[],
    frame_count: number,
    type: number,
    interpolation: NjInterpolation,
    element_count: number,
}

export enum NjInterpolation {
    Linear, Spline, UserFunction
}

export type NjMotionData = {
    tracks: NjKeyframeTrack[],
}

export enum NjKeyframeTrackType {
    Position, Rotation, Scale
}

export type NjKeyframeTrack =
    NjKeyframeTrackPosition | NjKeyframeTrackRotation | NjKeyframeTrackScale

export type NjKeyframeTrackPosition = {
    type: NjKeyframeTrackType.Position,
    keyframes: NjKeyframeF[],
}

export type NjKeyframeTrackRotation = {
    type: NjKeyframeTrackType.Rotation,
    keyframes: NjKeyframeA[],
}

export type NjKeyframeTrackScale = {
    type: NjKeyframeTrackType.Scale,
    keyframes: NjKeyframeF[],
}

export type NjKeyframe = NjKeyframeF | NjKeyframeA

/**
 * Used for parallel motion (POS), scale (SCL) and vector (VEC).
 */
export type NjKeyframeF = {
    frame: number,
    value: Vec3,
}

/**
 * Used for rotation (ANG).
 */
export type NjKeyframeA = {
    frame: number,
    value: Vec3, // Euler angles in radians.
}

/**
 * Format used by PSO:BB plymotiondata.rlc.
 */
export function parse_njm_4(cursor: BufferCursor): NjAction {
    cursor.seek_end(16);
    const offset1 = cursor.u32();
    cursor.seek_start(offset1);
    const action_offset = cursor.u32();
    cursor.seek_start(action_offset);
    return parse_action(cursor);
}

function parse_action(cursor: BufferCursor): NjAction {
    const object_offset = cursor.u32();
    const motion_offset = cursor.u32();
    cursor.seek_start(motion_offset);
    const motion = parse_motion(cursor);

    return {
        object_offset,
        motion
    };
}

function parse_motion(cursor: BufferCursor): NjMotion {
    const motion_offset = cursor.position;
    // Points to an array the size of the total amount of objects in the object tree.
    let mdata_offset = cursor.u32();
    const frame_count = cursor.u32();
    const type = cursor.u16();
    const inp_fn = cursor.u16();
    // Linear, spline or user function.
    const interpolation: NjInterpolation = (inp_fn & 0b11000000) >> 6;
    const element_count = inp_fn & 0b1111;
    const motion_data_list = [];

    // The mdata array stops where the motion structure starts.
    while (mdata_offset < motion_offset) {
        cursor.seek_start(mdata_offset);
        mdata_offset = mdata_offset += 8 * element_count;

        let motion_data: NjMotionData = {
            tracks: [],
        };
        motion_data_list.push(motion_data);

        const keyframe_offsets: number[] = [];
        const keyframe_counts: number[] = [];

        for (let i = 0; i < element_count; i++) {
            keyframe_offsets.push(cursor.u32());
        }

        for (let i = 0; i < element_count; i++) {
            const count = cursor.u32();
            keyframe_counts.push(count);
        }

        // NJD_MTYPE_POS_0
        if ((type & (1 << 0)) !== 0) {
            cursor.seek_start(keyframe_offsets.shift()!);
            const count = keyframe_counts.shift();

            if (count) {
                motion_data.tracks.push({
                    type: NjKeyframeTrackType.Position,
                    keyframes: parse_motion_data_f(cursor, count)
                });
            }
        }

        // NJD_MTYPE_ANG_1
        if ((type & (1 << 1)) !== 0) {
            cursor.seek_start(keyframe_offsets.shift()!);
            const count = keyframe_counts.shift();

            if (count) {
                motion_data.tracks.push({
                    type: NjKeyframeTrackType.Rotation,
                    keyframes: parse_motion_data_a(cursor, count)
                });
            }
        }

        // NJD_MTYPE_SCL_2
        if ((type & (1 << 2)) !== 0) {
            cursor.seek_start(keyframe_offsets.shift()!);
            const count = keyframe_counts.shift();

            if (count) {
                motion_data.tracks.push({
                    type: NjKeyframeTrackType.Scale,
                    keyframes: parse_motion_data_f(cursor, count)
                });
            }
        }

        // // NJD_MTYPE_VEC_3
        // if ((type & (1 << 3)) !== 0) {
        //     cursor.seek_start(keyframe_offsets.shift()!);
        //     motion_data.tracks.push(
        //         parse_motion_data_f(cursor, keyframe_counts.shift()!)
        //     );
        // }

        // // NJD_MTYPE_TARGET_3
        // if ((type & (1 << 6)) !== 0) {
        //     cursor.seek_start(keyframe_offsets.shift()!);
        //     motion_data.tracks.push(
        //         parse_motion_data_f(cursor, keyframe_counts.shift()!)
        //     );
        // }

        // TODO: all NJD_MTYPE's
    }

    return {
        motion_data: motion_data_list,
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
            value: new Vec3(cursor.f32(), cursor.f32(), cursor.f32()),
        });
    }

    return frames;
}

function parse_motion_data_a(cursor: BufferCursor, count: number): NjKeyframeA[] {
    const frames: NjKeyframeA[] = [];

    for (let i = 0; i < count; ++i) {
        frames.push({
            frame: cursor.u16(),
            value: new Vec3(
                cursor.u16() * ANGLE_TO_RAD,
                cursor.u16() * ANGLE_TO_RAD,
                cursor.u16() * ANGLE_TO_RAD
            ),
        });
    }

    return frames;
}
