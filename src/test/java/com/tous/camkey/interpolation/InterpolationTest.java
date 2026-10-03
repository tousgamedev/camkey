package com.tous.camkey.interpolation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InterpolationTest {

    private static final double EPSILON = 1e-6;

    @Test
    void lerpHitsEndpointsAndMidpoint() {
        assertEquals(2.0, Interpolation.lerp(2.0, 10.0, 0.0), EPSILON);
        assertEquals(10.0, Interpolation.lerp(2.0, 10.0, 1.0), EPSILON);
        assertEquals(6.0, Interpolation.lerp(2.0, 10.0, 0.5), EPSILON);
    }

    @Test
    void wrapDegreesKeepsAnglesInHalfOpenRange() {
        assertEquals(-170.0f, Interpolation.wrapDegrees(190.0f), EPSILON);
        assertEquals(170.0f, Interpolation.wrapDegrees(-190.0f), EPSILON);
        assertEquals(0.0f, Interpolation.wrapDegrees(720.0f), EPSILON);
        assertEquals(-180.0f, Interpolation.wrapDegrees(180.0f), EPSILON);
    }

    @Test
    void lerpAngleTakesTheShortWayAcrossTheSeam() {
        // 170° -> -170° is a 20° turn through 180°, not a 340° spin the other way.
        assertEquals(180.0f, Interpolation.lerpAngle(170.0f, -170.0f, 0.5), EPSILON);
        assertEquals(-10.0f, Interpolation.lerpAngle(10.0f, -30.0f, 0.5), EPSILON);
    }

    @Test
    void distanceIsEuclidean() {
        assertEquals(5.0, Interpolation.distance(0, 0, 0, 3, 4, 0), EPSILON);
    }

    @Test
    void easingsStartAtZeroAndEndAtOne() {
        for (Easing easing : new Easing[] {Easing.LINEAR, Easing.SMOOTHSTEP}) {
            assertEquals(0.0, easing.apply(0.0), EPSILON);
            assertEquals(1.0, easing.apply(1.0), EPSILON);
        }
    }

    @Test
    void smoothstepIsSymmetricAndNeverGoesBackwards() {
        assertEquals(0.5, Easing.SMOOTHSTEP.apply(0.5), EPSILON);
        double previous = 0.0;
        for (int i = 1; i <= 100; i++) {
            double value = Easing.SMOOTHSTEP.apply(i / 100.0);
            assertTrue(value >= previous, "smoothstep decreased at t=" + i / 100.0);
            previous = value;
        }
    }
}
