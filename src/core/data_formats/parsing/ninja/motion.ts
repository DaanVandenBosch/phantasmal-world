import { ANGLE_TO_RAD } from "./index";
import { Cursor } from "../../block/cursor/Cursor";
import { Vec3 } from "../../vector";

const NMDM = 0x4d444d4e;

export type NjMotion = {
    motion_data: NjMotionData[];
    frame_count: number;
    type: number;
    interpolation: NjInterpolation;
    element_count: number;
};

export enum NjInterpolation {
    Linear,
    Spline,
    UserFunction,
}

export type NjMotionData = {
    tracks: NjKeyframeTrack[];
};

export enum NjKeyframeTrackType {
    Position,
    Rotation,
    Scale,
}

export type NjKeyframeTrack =
    | NjKeyframeTrackPosition
    | NjKeyframeTrackRotation
    | NjKeyframeTrackScale;

export type NjKeyframeTrackPosition = {
    type: NjKeyframeTrackType.Position;
    keyframes: NjKeyframeF[];
};

export type NjKeyframeTrackRotation = {
    type: NjKeyframeTrackType.Rotation;
    keyframes: NjKeyframeA[];
};

export type NjKeyframeTrackScale = {
    type: NjKeyframeTrackType.Scale;
    keyframes: NjKeyframeF[];
};

export type NjKeyframe = NjKeyframeF | NjKeyframeA;

/**
 * Used for parallel motion (POS), scale (SCL) and vector (VEC).
 */
export type NjKeyframeF = {
    frame: number;
    value: Vec3;
};

/**
 * Used for rotation (ANG).
 */
export type NjKeyframeA = {
    frame: number;
    value: Vec3; // Euler angles in radians.
};

export function parse_njm(cursor: Cursor, bone_count: number): NjMotion {
    if (cursor.u32() === NMDM) {
        return parse_njm_v2(cursor, bone_count);
    } else {
        cursor.seek_start(0);
        return parse_njm_bb(cursor, bone_count);
    }
}

/**
 * Format used by PSO v2 and for the enemies in PSO:BB.
 */
function parse_njm_v2(cursor: Cursor, bone_count: number): NjMotion {
    const chunk_size = cursor.u32();
    return parse_motion(cursor.take(chunk_size), bone_count);
}

/**
 * Format used by PSO:BB plymotiondata.rlc.
 */
function parse_njm_bb(cursor: Cursor, bone_count: number): NjMotion {
    cursor.seek_end(16);
    const offset1 = cursor.u32();
    cursor.seek_start(offset1);
    const action_offset = cursor.u32();
    cursor.seek_start(action_offset);
    return parse_action(cursor, bone_count);
}

function parse_action(cursor: Cursor, bone_count: number): NjMotion {
    cursor.seek(4); // Object pointer placeholder.
    const motion_offset = cursor.u32();
    cursor.seek_start(motion_offset);
    return parse_motion(cursor, bone_count);
}

function parse_motion(cursor: Cursor, bone_count: number): NjMotion {
    // Points to an array the size of bone_count.
    let mdata_offset = cursor.u32();
    const frame_count = cursor.u32();
    const type = cursor.u16();
    const inp_fn = cursor.u16();
    // Linear, spline or user function.
    const interpolation: NjInterpolation = (inp_fn & 0b11000000) >> 6;
    const element_count = inp_fn & 0b1111;
    const motion_data_list = [];

    for (let i = 0; i < bone_count; i++) {
        cursor.seek_start(mdata_offset);
        mdata_offset = mdata_offset += 8 * element_count;

        const motion_data: NjMotionData = {
            tracks: [],
        };
        motion_data_list.push(motion_data);

        const keyframe_offsets: number[] = [];
        const keyframe_counts: number[] = [];

        for (let j = 0; j < element_count; j++) {
            keyframe_offsets.push(cursor.u32());
        }

        for (let j = 0; j < element_count; j++) {
            const count = cursor.u32();
            keyframe_counts.push(count);
        }

        // NJD_MTYPE_POS_0
        if (type & (1 << 0)) {
            cursor.seek_start(keyframe_offsets.shift()!);
            const count = keyframe_counts.shift();

            if (count) {
                motion_data.tracks.push({
                    type: NjKeyframeTrackType.Position,
                    keyframes: parse_motion_data_f(cursor, count),
                });
            }
        }

        // NJD_MTYPE_ANG_1
        if (type & (1 << 1)) {
            cursor.seek_start(keyframe_offsets.shift()!);
            const count = keyframe_counts.shift();

            if (count) {
                motion_data.tracks.push({
                    type: NjKeyframeTrackType.Rotation,
                    keyframes: parse_motion_data_a(cursor, count, frame_count),
                });
            }
        }

        // NJD_MTYPE_SCL_2
        if (type & (1 << 2)) {
            cursor.seek_start(keyframe_offsets.shift()!);
            const count = keyframe_counts.shift();

            if (count) {
                motion_data.tracks.push({
                    type: NjKeyframeTrackType.Scale,
                    keyframes: parse_motion_data_f(cursor, count),
                });
            }
        }

        // TODO: all NJD_MTYPE's
    }

    return {
        motion_data: motion_data_list,
        frame_count,
        type,
        interpolation,
        element_count,
    };
}

function parse_motion_data_f(cursor: Cursor, count: number): NjKeyframeF[] {
    const frames: NjKeyframeF[] = [];

    for (let i = 0; i < count; ++i) {
        frames.push({
            frame: cursor.u32(),
            value: cursor.vec3_f32(),
        });
    }

    return frames;
}

function parse_motion_data_a(
    cursor: Cursor,
    keyframe_count: number,
    frame_count: number,
): NjKeyframeA[] {
    const frames: NjKeyframeA[] = [];
    const start_pos = cursor.position;

    for (let i = 0; i < keyframe_count; ++i) {
        frames.push({
            frame: cursor.u16(),
            value: {
                x: cursor.u16() * ANGLE_TO_RAD,
                y: cursor.u16() * ANGLE_TO_RAD,
                z: cursor.u16() * ANGLE_TO_RAD,
            },
        });
    }

    let prev = -1;

    for (const { frame } of frames) {
        if (frame < prev || frame >= frame_count) {
            cursor.seek_start(start_pos);
            return parse_motion_data_a_wide(cursor, keyframe_count);
        }

        prev = frame;
    }

    return frames;
}

function parse_motion_data_a_wide(cursor: Cursor, keyframe_count: number): NjKeyframeA[] {
    const frames: NjKeyframeA[] = [];

    for (let i = 0; i < keyframe_count; ++i) {
        frames.push({
            frame: cursor.u32(),
            value: {
                x: cursor.i32() * ANGLE_TO_RAD,
                y: cursor.i32() * ANGLE_TO_RAD,
                z: cursor.i32() * ANGLE_TO_RAD,
            },
        });
    }

    return frames;
}
