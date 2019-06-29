import { AnimationClip, Euler, InterpolateLinear, InterpolateSmooth, KeyframeTrack, Quaternion, QuaternionKeyframeTrack, VectorKeyframeTrack } from "three";
import { NjAction, NjInterpolation, NjKeyframeTrackType } from "../bin_data/parsing/ninja/motion";

const PSO_FRAME_RATE = 30;

export function create_animation_clip(action: NjAction): AnimationClip {
    const motion = action.motion;
    const interpolation = motion.interpolation === NjInterpolation.Spline
        ? InterpolateSmooth
        : InterpolateLinear;

    const tracks: KeyframeTrack[] = [];

    motion.motion_data.forEach((motion_data, object_id) => {
        motion_data.tracks.forEach(({ type, keyframes }) => {
            const times: number[] = [];
            const values: number[] = [];

            if (type === NjKeyframeTrackType.Position) {
                const name = `obj_${object_id}.position`;

                for (const keyframe of keyframes) {
                    times.push(keyframe.frame / PSO_FRAME_RATE);
                    values.push(keyframe.value.x, keyframe.value.y, keyframe.value.z);
                }

                tracks.push(new VectorKeyframeTrack(name, times, values, interpolation));
            } else if (type === NjKeyframeTrackType.Scale) {
                const name = `obj_${object_id}.scale`;

                for (const keyframe of keyframes) {
                    times.push(keyframe.frame / PSO_FRAME_RATE);
                    values.push(keyframe.value.x, keyframe.value.y, keyframe.value.z);
                }

                tracks.push(new VectorKeyframeTrack(name, times, values, interpolation));
            } else {
                for (const keyframe of keyframes) {
                    times.push(keyframe.frame / PSO_FRAME_RATE);

                    const quat = new Quaternion().setFromEuler(
                        new Euler(keyframe.value.x, keyframe.value.y, keyframe.value.z)
                    );

                    values.push(quat.x, quat.y, quat.z, quat.w);
                }

                tracks.push(
                    new QuaternionKeyframeTrack(
                        `obj_${object_id}.quaternion`, times, values, interpolation
                    )
                );
            }
        });
    });

    return new AnimationClip(
        'Animation',
        motion.frame_count / PSO_FRAME_RATE,
        tracks
    );
}
