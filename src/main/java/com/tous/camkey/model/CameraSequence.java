package com.tous.camkey.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record CameraSequence(String name, List<Keyframe> keyframes) {

    public CameraSequence {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(keyframes, "keyframes must not be null");
        keyframes = List.copyOf(keyframes);
    }

    public CameraSequence(String name) {
        this(name, List.of());
    }

    public boolean isEmpty() {
        return keyframes.isEmpty();
    }

    public int size() {
        return keyframes.size();
    }

    public CameraSequence withKeyframe(Keyframe keyframe) {
        List<Keyframe> updated = new ArrayList<>(keyframes);
        updated.add(keyframe);
        return new CameraSequence(name, updated);
    }

    public Optional<CameraSequence> withoutLastKeyframe() {
        if (isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CameraSequence(name, keyframes.subList(0, keyframes.size() - 1)));
    }
}
