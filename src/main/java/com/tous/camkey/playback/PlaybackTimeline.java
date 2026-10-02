package com.tous.camkey.playback;

import java.util.List;

import com.tous.camkey.interpolation.Interpolation;
import com.tous.camkey.model.Keyframe;

public final class PlaybackTimeline {

    private final List<Keyframe> keyframes;
    private final double[] cumulativeProgress;

    public PlaybackTimeline(List<Keyframe> keyframes) {
        if (keyframes.size() < 2) {
            throw new IllegalArgumentException("A playback timeline needs at least 2 keyframes");
        }
        this.keyframes = keyframes;
        this.cumulativeProgress = computeCumulativeProgress(keyframes);
    }

    private static double[] computeCumulativeProgress(List<Keyframe> keyframes) {
        double[] cumulativeDistance = new double[keyframes.size()];
        double totalDistance = 0.0;
        for (int i = 1; i < keyframes.size(); i++) {
            Keyframe prev = keyframes.get(i - 1);
            Keyframe curr = keyframes.get(i);
            totalDistance += Interpolation.distance(prev.x(), prev.y(), prev.z(), curr.x(), curr.y(), curr.z());
            cumulativeDistance[i] = totalDistance;
        }

        double[] progress = new double[keyframes.size()];
        if (totalDistance == 0.0) {
            // All keyframes share a position (e.g. a pure rotation move) — fall back to equal spacing
            // so segments stay evenly timed instead of dividing by zero.
            for (int i = 0; i < keyframes.size(); i++) {
                progress[i] = (double) i / (keyframes.size() - 1);
            }
        } else {
            for (int i = 0; i < keyframes.size(); i++) {
                progress[i] = cumulativeDistance[i] / totalDistance;
            }
        }
        return progress;
    }

    public Keyframe sample(double progress) {
        double clamped = Math.max(0.0, Math.min(1.0, progress));
        if (clamped <= 0.0) {
            return keyframes.get(0);
        }
        if (clamped >= 1.0) {
            return keyframes.get(keyframes.size() - 1);
        }

        int segment = findSegment(clamped);
        double segmentStart = cumulativeProgress[segment];
        double segmentEnd = cumulativeProgress[segment + 1];
        double localT = segmentEnd > segmentStart ? (clamped - segmentStart) / (segmentEnd - segmentStart) : 0.0;

        Keyframe from = keyframes.get(segment);
        Keyframe to = keyframes.get(segment + 1);
        return new Keyframe(
                Interpolation.lerp(from.x(), to.x(), localT),
                Interpolation.lerp(from.y(), to.y(), localT),
                Interpolation.lerp(from.z(), to.z(), localT),
                Interpolation.lerpAngle(from.yaw(), to.yaw(), localT),
                Interpolation.lerpAngle(from.pitch(), to.pitch(), localT)
        );
    }

    private int findSegment(double progress) {
        for (int i = 0; i < cumulativeProgress.length - 1; i++) {
            if (progress <= cumulativeProgress[i + 1]) {
                return i;
            }
        }
        return cumulativeProgress.length - 2;
    }
}
