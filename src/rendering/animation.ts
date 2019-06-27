import { AnimationClip, InterpolateLinear, InterpolateSmooth, KeyframeTrack, VectorKeyframeTrack } from "three";
import { NjAction, NjInterpolation, NjKeyframeTrackType } from "../bin_data/parsing/ninja/motion";

const PSO_FRAME_RATE = 30;

export function create_animation_clip(action: NjAction): AnimationClip {
    const motion = action.motion;
    const interpolation = motion.interpolation === NjInterpolation.Spline
        ? InterpolateSmooth
        : InterpolateLinear;
    // TODO: parse data for all objects.
    const motion_data = motion.motion_data[0];

    const tracks: KeyframeTrack[] = [];

    motion_data.tracks.forEach(({ type, keyframes }) => {
        // TODO: rotation
        if (type === NjKeyframeTrackType.Rotation) return;

        const times: number[] = [];
        const values: number[] = [];

        for (const keyframe of keyframes) {
            times.push(keyframe.frame / PSO_FRAME_RATE);
            values.push(...keyframe.value);
        }

        let name: string;

        switch (type) {
            case NjKeyframeTrackType.Position: name = '.position'; break;
            // case NjKeyframeTrackType.Rotation: name = 'rotation'; break;
            case NjKeyframeTrackType.Scale: name = '.scale'; break;
        }

        tracks.push(new VectorKeyframeTrack(name!, times, values, interpolation));
    });

    return new AnimationClip(
        'Animation',
        motion.frame_count / PSO_FRAME_RATE,
        tracks
    );
}
