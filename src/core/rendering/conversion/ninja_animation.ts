import {
    AnimationClip,
    Euler,
    InterpolateLinear,
    InterpolateSmooth,
    KeyframeTrack,
    Quaternion,
    QuaternionKeyframeTrack,
    VectorKeyframeTrack,
} from "three";
import { NjModel, NjObject } from "../../data_formats/parsing/ninja";
import {
    NjInterpolation,
    NjKeyframeTrackType,
    NjMotion,
} from "../../data_formats/parsing/ninja/motion";

export const PSO_FRAME_RATE = 30;

export function create_animation_clip(
    nj_object: NjObject<NjModel>,
    nj_motion: NjMotion,
): AnimationClip {
    const interpolation =
        nj_motion.interpolation === NjInterpolation.Spline ? InterpolateSmooth : InterpolateLinear;

    const tracks: KeyframeTrack[] = [];

    nj_motion.motion_data.forEach((motion_data, bone_id) => {
        const bone = nj_object.get_bone(bone_id);
        if (!bone) return;

        motion_data.tracks.forEach(({ type, keyframes }) => {
            const times: number[] = [];
            const values: number[] = [];

            for (const keyframe of keyframes) {
                times.push(keyframe.frame / PSO_FRAME_RATE);

                if (type === NjKeyframeTrackType.Rotation) {
                    const order = bone.evaluation_flags.zxy_rotation_order ? "ZXY" : "ZYX";
                    const quat = new Quaternion().setFromEuler(
                        new Euler(keyframe.value.x, keyframe.value.y, keyframe.value.z, order),
                    );

                    values.push(quat.x, quat.y, quat.z, quat.w);
                } else {
                    values.push(keyframe.value.x, keyframe.value.y, keyframe.value.z);
                }
            }

            if (type === NjKeyframeTrackType.Rotation) {
                tracks.push(
                    new QuaternionKeyframeTrack(
                        `.bones[${bone_id}].quaternion`,
                        times,
                        values,
                        interpolation,
                    ),
                );
            } else {
                const name =
                    type === NjKeyframeTrackType.Position
                        ? `.bones[${bone_id}].position`
                        : `.bones[${bone_id}].scale`;

                tracks.push(new VectorKeyframeTrack(name, times, values, interpolation));
            }
        });
    });

    return new AnimationClip(
        "Animation",
        (nj_motion.frame_count - 1) / PSO_FRAME_RATE,
        tracks,
    ).optimize();
}
