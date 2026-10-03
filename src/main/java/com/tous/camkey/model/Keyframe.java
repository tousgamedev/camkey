package com.tous.camkey.model;

public record Keyframe(double x, double y, double z, float yaw, float pitch) {

    public Keyframe {
        // Gson parses leniently, so a hand-edited file can smuggle in NaN/Infinity — reject them
        // here rather than teleporting the player to a non-finite position during playback.
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            throw new IllegalArgumentException("Keyframe values must be finite");
        }
    }
}
