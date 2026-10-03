package com.tous.camkey.command;

import java.util.List;

/**
 * Optional unit word typed after a playback duration, e.g. {@code /camkey play intro 2 minutes}.
 * A bare number means seconds. Everything past the command layer works in seconds only.
 */
enum DurationUnit {
    SECONDS(1.0, "second", "seconds"),
    MINUTES(60.0, "minute", "minutes");

    private final double secondsPerUnit;
    private final List<String> words;

    DurationUnit(double secondsPerUnit, String... words) {
        this.secondsPerUnit = secondsPerUnit;
        this.words = List.of(words);
    }

    List<String> words() {
        return words;
    }

    double toSeconds(double amount) {
        return amount * secondsPerUnit;
    }
}
