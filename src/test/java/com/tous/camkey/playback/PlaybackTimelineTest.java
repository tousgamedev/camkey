package com.tous.camkey.playback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.tous.camkey.model.Keyframe;

class PlaybackTimelineTest {

    private static final double EPSILON = 1e-6;

    private static Keyframe at(double x) {
        return new Keyframe(x, 64.0, 0.0, 0.0f, 0.0f);
    }

    @Test
    void endsOfTheTimelineAreTheFirstAndLastKeyframes() {
        PlaybackTimeline timeline = new PlaybackTimeline(List.of(at(0), at(10), at(40)));
        assertEquals(at(0), timeline.sample(0.0));
        assertEquals(at(40), timeline.sample(1.0));
    }

    @Test
    void timeIsSharedByDistanceNotEquallyPerSegment() {
        // Segments of 10 and 30 blocks: the first keyframe boundary is a quarter of the way in, and
        // halfway through the move is 20 blocks along — constant pace regardless of spacing.
        PlaybackTimeline timeline = new PlaybackTimeline(List.of(at(0), at(10), at(40)));
        assertEquals(10.0, timeline.sample(0.25).x(), EPSILON);
        assertEquals(20.0, timeline.sample(0.5).x(), EPSILON);
        assertEquals(25.0, timeline.sample(0.625).x(), EPSILON);
    }

    @Test
    void rotationOnlySequenceFallsBackToEqualSpacing() {
        List<Keyframe> turnOnTheSpot = List.of(
                new Keyframe(0, 64, 0, 0.0f, 0.0f),
                new Keyframe(0, 64, 0, 90.0f, 0.0f),
                new Keyframe(0, 64, 0, 180.0f, 0.0f));
        PlaybackTimeline timeline = new PlaybackTimeline(turnOnTheSpot);
        assertEquals(90.0f, timeline.sample(0.5).yaw(), EPSILON);
        assertEquals(45.0f, timeline.sample(0.25).yaw(), EPSILON);
    }

    @Test
    void rotationInterpolatesInTransitWithPosition() {
        PlaybackTimeline timeline = new PlaybackTimeline(List.of(
                new Keyframe(0, 64, 0, 0.0f, -20.0f),
                new Keyframe(10, 64, 0, 90.0f, 20.0f)));
        Keyframe halfway = timeline.sample(0.5);
        assertEquals(5.0, halfway.x(), EPSILON);
        assertEquals(45.0f, halfway.yaw(), EPSILON);
        assertEquals(0.0f, halfway.pitch(), EPSILON);
    }

    @Test
    void needsAtLeastTwoKeyframes() {
        assertThrows(IllegalArgumentException.class, () -> new PlaybackTimeline(List.of(at(0))));
    }
}
