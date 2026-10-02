package com.tous.camkey.playback;

import com.tous.camkey.interpolation.Easing;
import com.tous.camkey.model.CameraSequence;
import com.tous.camkey.model.Keyframe;

public class PlaybackSession {

    private final PlaybackTimeline timeline;
    private final double durationSeconds;
    private final Easing easing;
    private double elapsedSeconds;

    public PlaybackSession(CameraSequence sequence, double durationSeconds, Easing easing) {
        if (sequence.size() < 2) {
            throw new IllegalArgumentException("A sequence needs at least 2 keyframes to play back");
        }
        if (durationSeconds <= 0.0) {
            throw new IllegalArgumentException("Duration must be positive");
        }
        this.timeline = new PlaybackTimeline(sequence.keyframes());
        this.durationSeconds = durationSeconds;
        this.easing = easing;
    }

    public void tick(double deltaSeconds) {
        elapsedSeconds = Math.min(elapsedSeconds + deltaSeconds, durationSeconds);
    }

    public Keyframe currentKeyframe() {
        double rawProgress = elapsedSeconds / durationSeconds;
        double easedProgress = easing.apply(rawProgress);
        return timeline.sample(easedProgress);
    }

    public boolean isFinished() {
        return elapsedSeconds >= durationSeconds;
    }
}
