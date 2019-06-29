import { AnimationClip, Euler, InterpolateLinear, InterpolateSmooth, KeyframeTrack, Quaternion, QuaternionKeyframeTrack, VectorKeyframeTrack } from "three";
import { NjAction, NjInterpolation, NjKeyframeTrackType } from "../bin_data/parsing/ninja/motion";

const PSO_FRAME_RATE = 30;

export function create_animation_clip(action: NjAction): AnimationClip {
    const motion = action.motion;
    const interpolation = motion.interpolation === NjInterpolation.Spline
        ? InterpolateSmooth
        : InterpolateLinear;

    const tracks: KeyframeTrack[] = [];

    motion.motion_data.forEach((motion_data, bone_id) => {
        motion_data.tracks.forEach(({ type, keyframes }) => {
            let name: string;
            const times: number[] = [];
            const values: number[] = [];

            for (const keyframe of keyframes) {
                times.push(keyframe.frame / PSO_FRAME_RATE);

                if (type === NjKeyframeTrackType.Position) {
                    name = `.bones[${bone_id}].position`;
                    values.push(keyframe.value.x, keyframe.value.y, keyframe.value.z);
                } else if (type === NjKeyframeTrackType.Scale) {
                    name = `.bones[${bone_id}].scale`;
                    values.push(keyframe.value.x, keyframe.value.y, keyframe.value.z);
                } else {
                    name = `.bones[${bone_id}].quaternion`;
                    
                    const quat = new Quaternion().setFromEuler(
                        new Euler(keyframe.value.x, keyframe.value.y, keyframe.value.z)
                    );

                    values.push(quat.x, quat.y, quat.z, quat.w);
                }
            }

            if (type === NjKeyframeTrackType.Rotation) {
                tracks.push(
                    new QuaternionKeyframeTrack(
                        name!, times, values, interpolation
                    )
                );
            } else {
                tracks.push(new VectorKeyframeTrack(name, times, values, interpolation));
            }
        });
    });

    return new AnimationClip(
        'Animation',
        motion.frame_count / PSO_FRAME_RATE,
        tracks
    );
}
