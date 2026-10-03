package com.tous.camkey.playback;

import com.tous.camkey.interpolation.Easing;
import com.tous.camkey.interpolation.Interpolation;
import com.tous.camkey.model.CameraSequence;
import com.tous.camkey.model.Keyframe;

public class PlaybackSession {

    private final PlaybackTimeline timeline;
    private final double durationSeconds;
    private final Easing easing;
    private double previousElapsedSeconds;
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
        previousElapsedSeconds = elapsedSeconds;
        elapsedSeconds = Math.min(elapsedSeconds + deltaSeconds, durationSeconds);
    }

    /**
     * Samples the path within the most recent tick: 0 is where the tick started, 1 is where it
     * ended. Render frames fall between ticks, so they pass the frame's partial tick here.
     */
    public Keyframe keyframeAt(double partialTick) {
        double elapsed = Interpolation.lerp(previousElapsedSeconds, elapsedSeconds, partialTick);
        double easedProgress = easing.apply(elapsed / durationSeconds);
        return timeline.sample(easedProgress);
    }

    public boolean isFinished() {
        return elapsedSeconds >= durationSeconds;
    }
}
