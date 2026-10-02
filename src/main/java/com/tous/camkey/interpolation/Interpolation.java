package com.tous.camkey.interpolation;

public final class Interpolation {

    private Interpolation() {
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static float lerpAngle(float a, float b, double t) {
        float delta = wrapDegrees(b - a);
        return (float) (a + delta * t);
    }

    public static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0f;
        if (wrapped >= 180.0f) {
            wrapped -= 360.0f;
        } else if (wrapped < -180.0f) {
            wrapped += 360.0f;
        }
        return wrapped;
    }

    public static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
