package world.phantasmal.web.core.rendering.conversion

import world.phantasmal.core.asArray
import world.phantasmal.core.jsArrayOf
import world.phantasmal.psolib.fileFormats.ninja.NinjaObject
import world.phantasmal.psolib.fileFormats.ninja.NjInterpolation
import world.phantasmal.psolib.fileFormats.ninja.NjKeyframeTrack
import world.phantasmal.psolib.fileFormats.ninja.NjMotion
import world.phantasmal.web.externals.three.*

const val PSO_FRAME_RATE: Int = 30
const val PSO_FRAME_RATE_DOUBLE: Double = PSO_FRAME_RATE.toDouble()

fun createAnimationClip(njObject: NinjaObject<*, *>, njMotion: NjMotion): AnimationClip {
    val interpolation =
        if (njMotion.interpolation == NjInterpolation.Spline) InterpolateSmooth
        else InterpolateLinear

    val tracks = jsArrayOf<KeyframeTrack>()

    for ((boneIndex, motionData) in njMotion.motionData.withIndex()) {
        val bone = njObject.getBone(boneIndex) ?: continue

        for (track in motionData.tracks) {
            if (track.keyframes.isEmpty()) continue

            val baseName = ".bones[$boneIndex]"
            val times = jsArrayOf<Double>()

            for (keyframe in track.keyframes) {
                times.push(keyframe.frame / PSO_FRAME_RATE_DOUBLE)
            }

            val values = jsArrayOf<Double>()

            tracks.push(
                when (track) {
                    is NjKeyframeTrack.Position -> {
                        for (keyframe in track.keyframes) {
                            values.push(
                                keyframe.value.x.toDouble(),
                                keyframe.value.y.toDouble(),
                                keyframe.value.z.toDouble(),
                            )
                        }

                        VectorKeyframeTrack(
                            "$baseName.position",
                            times.asArray(),
                            values.asArray(),
                            interpolation,
                        )
                    }

                    is NjKeyframeTrack.EulerAngles -> {
                        for (keyframe in track.keyframes) {
                            val quat = Quaternion().setFromEuler(
                                Euler(
                                    keyframe.value.x.toDouble(),
                                    keyframe.value.y.toDouble(),
                                    keyframe.value.z.toDouble(),
                                    if (bone.evaluationFlags.zxyRotationOrder) "ZXY" else "ZYX"
                                ),
                            )

                            values.push(quat.x, quat.y, quat.z, quat.w)
                        }

                        QuaternionKeyframeTrack(
                            "$baseName.quaternion",
                            times.asArray(),
                            values.asArray(),
                            interpolation,
                        )
                    }

                    is NjKeyframeTrack.Scale -> {
                        for (keyframe in track.keyframes) {
                            values.push(
                                keyframe.value.x.toDouble(),
                                keyframe.value.y.toDouble(),
                                keyframe.value.z.toDouble(),
                            )
                        }

                        VectorKeyframeTrack(
                            "$baseName.scale",
                            times.asArray(),
                            values.asArray(),
                            interpolation,
                        )
                    }

                    is NjKeyframeTrack.Quaternion -> {
                        for (keyframe in track.keyframes) {
                            values.push(
                                keyframe.imaginary.x.toDouble(),
                                keyframe.imaginary.y.toDouble(),
                                keyframe.imaginary.z.toDouble(),
                                keyframe.real.toDouble(),
                            )
                        }

                        QuaternionKeyframeTrack(
                            "$baseName.quaternion",
                            times.asArray(),
                            values.asArray(),
                            interpolation,
                        )
                    }
                }
            )
        }
    }

    return AnimationClip(
        "Animation",
        (njMotion.frameCount - 1) / PSO_FRAME_RATE_DOUBLE,
        tracks.asArray(),
    ).optimize()
}
