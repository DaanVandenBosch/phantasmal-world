import { AnimationClip, Euler, InterpolateLinear, InterpolateSmooth, KeyframeTrack, Quaternion, QuaternionKeyframeTrack, VectorKeyframeTrack } from "three";
import { NjAction, NjInterpolation, NjKeyframeTrackType } from "../data_formats/parsing/ninja/motion";

export const PSO_FRAME_RATE = 30;

export function create_animation_clip(action: NjAction): AnimationClip {
    const motion = action.motion;
    const interpolation = motion.interpolation === NjInterpolation.Spline
        ? InterpolateSmooth
        : InterpolateLinear;

    const tracks: KeyframeTrack[] = [];

    motion.motion_data.forEach((motion_data, bone_id) => {
        motion_data.tracks.forEach(({ type, keyframes }) => {
            const times: number[] = [];
            const values: number[] = [];

            for (const keyframe of keyframes) {
                times.push(keyframe.frame / PSO_FRAME_RATE);

                if (type === NjKeyframeTrackType.Rotation) {
                    const quat = new Quaternion().setFromEuler(
                        new Euler(keyframe.value.x, keyframe.value.y, keyframe.value.z)
                    );

                    values.push(quat.x, quat.y, quat.z, quat.w);
                } else {
                    values.push(keyframe.value.x, keyframe.value.y, keyframe.value.z);
                }
            }

            if (type === NjKeyframeTrackType.Rotation) {
                tracks.push(new QuaternionKeyframeTrack(
                    `.bones[${bone_id}].quaternion`, times, values, interpolation
                ));
            } else {
                const name = type === NjKeyframeTrackType.Position
                    ? `.bones[${bone_id}].position`
                    : `.bones[${bone_id}].scale`;

                tracks.push(new VectorKeyframeTrack(
                    name, times, values, interpolation
                ));
            }
        });
    });

    return new AnimationClip(
        'Animation',
        (motion.frame_count - 1) / PSO_FRAME_RATE,
        tracks
    ).optimize();
}
