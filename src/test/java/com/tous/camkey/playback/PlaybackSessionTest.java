package com.tous.camkey.playback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.tous.camkey.interpolation.Easing;
import com.tous.camkey.model.CameraSequence;
import com.tous.camkey.model.Keyframe;

class PlaybackSessionTest {

    private static final double EPSILON = 1e-6;

    private static PlaybackSession tenBlocksInOneSecond() {
        CameraSequence sequence = new CameraSequence("test", List.of(
                new Keyframe(0, 64, 0, 0.0f, 0.0f),
                new Keyframe(10, 64, 0, 0.0f, 0.0f)));
        return new PlaybackSession(sequence, 1.0, Easing.LINEAR);
    }

    @Test
    void partialTickSamplesBetweenTheStartAndEndOfTheLastTick() {
        PlaybackSession playback = tenBlocksInOneSecond();
        playback.tick(0.5);
        assertEquals(0.0, playback.keyframeAt(0.0).x(), EPSILON);
        assertEquals(2.5, playback.keyframeAt(0.5).x(), EPSILON);
        assertEquals(5.0, playback.keyframeAt(1.0).x(), EPSILON);

        playback.tick(0.25);
        assertEquals(5.0, playback.keyframeAt(0.0).x(), EPSILON);
        assertEquals(7.5, playback.keyframeAt(1.0).x(), EPSILON);
    }

    @Test
    void holdsOnTheLastKeyframeAfterFinishing() {
        PlaybackSession playback = tenBlocksInOneSecond();
        playback.tick(0.75);
        assertFalse(playback.isFinished());
        playback.tick(0.75);
        assertTrue(playback.isFinished());
        assertEquals(10.0, playback.keyframeAt(1.0).x(), EPSILON);

        // Once finished, every frame of every later tick is the final keyframe — no drift.
        playback.tick(0.05);
        assertEquals(10.0, playback.keyframeAt(0.0).x(), EPSILON);
        assertEquals(10.0, playback.keyframeAt(0.5).x(), EPSILON);
    }
}
