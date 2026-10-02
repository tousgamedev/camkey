package com.tous.camkey.command;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.tous.camkey.interpolation.Easing;
import com.tous.camkey.model.CameraSequence;
import com.tous.camkey.model.Keyframe;
import com.tous.camkey.playback.PlaybackSession;
import com.tous.camkey.storage.SequenceStorage;

public class CamKeySession {

    // TODO: pull from Config once it's repurposed for camkey settings, instead of a hardcoded default.
    private static final double DEFAULT_PLAYBACK_SECONDS = 10.0;
    private static final double MAX_PLAYBACK_SECONDS = 3600.0;

    private final SequenceStorage storage;
    private String activeSequenceName;
    private PlaybackSession activePlayback;

    public CamKeySession(SequenceStorage storage) {
        this.storage = storage;
    }

    public CommandResult add(String name, Keyframe keyframe) {
        Optional<CameraSequence> existing = storage.load(name);
        CameraSequence sequence;
        if (existing.isPresent()) {
            sequence = existing.get();
        } else if (storage.exists(name)) {
            // A file is there but failed to load — do not silently create a fresh sequence over
            // it, or the next save would overwrite (and lose) whatever was recoverable in it.
            return CommandResult.failure("camkey.error.corrupt_sequence", name);
        } else {
            sequence = new CameraSequence(name);
        }
        CameraSequence updated = sequence.withKeyframe(keyframe);
        storage.save(updated);
        activeSequenceName = name;
        return CommandResult.success("camkey.add.success", name, updated.size());
    }

    public CommandResult addToActive(Keyframe keyframe) {
        if (activeSequenceName == null) {
            return CommandResult.failure("camkey.error.no_active_sequence");
        }
        return add(activeSequenceName, keyframe);
    }

    public CommandResult use(String name) {
        if (storage.load(name).isEmpty()) {
            return missingOrCorrupt(name);
        }
        activeSequenceName = name;
        return CommandResult.success("camkey.use.success", name);
    }

    public CommandResult deleteLast(String name) {
        Optional<CameraSequence> sequence = storage.load(name);
        if (sequence.isEmpty()) {
            return missingOrCorrupt(name);
        }
        Optional<CameraSequence> updated = sequence.get().withoutLastKeyframe();
        if (updated.isEmpty()) {
            return CommandResult.failure("camkey.error.sequence_empty", name);
        }
        storage.save(updated.get());
        return CommandResult.success("camkey.delete.success", updated.get().size());
    }

    public CommandResult deleteLastFromActive() {
        if (activeSequenceName == null) {
            return CommandResult.failure("camkey.error.no_active_sequence");
        }
        return deleteLast(activeSequenceName);
    }

    public CommandResult list() {
        List<String> names = storage.listNames();
        if (names.isEmpty()) {
            return CommandResult.success("camkey.list.empty");
        }
        String summary = names.stream()
                .map(name -> storage.load(name)
                        .map(sequence -> name + " (" + sequence.size() + ")")
                        .orElse(name + " (corrupted)"))
                .collect(Collectors.joining(", "));
        return CommandResult.success("camkey.list.result", summary);
    }

    public Optional<String> activeSequenceName() {
        return Optional.ofNullable(activeSequenceName);
    }

    public CommandResult play(String name, double seconds) {
        if (activePlayback != null) {
            return CommandResult.failure("camkey.error.already_playing");
        }
        return startPlayback(name, seconds);
    }

    public CommandResult toggleActivePlayback() {
        if (activePlayback != null) {
            return cancelPlayback();
        }
        if (activeSequenceName == null) {
            return CommandResult.failure("camkey.error.no_active_sequence");
        }
        return startPlayback(activeSequenceName, DEFAULT_PLAYBACK_SECONDS);
    }

    public CommandResult cancelPlayback() {
        if (activePlayback == null) {
            return CommandResult.failure("camkey.error.not_playing");
        }
        activePlayback = null;
        return CommandResult.success("camkey.play.cancelled");
    }

    public void tickPlayback(double deltaSeconds) {
        if (activePlayback != null) {
            activePlayback.tick(deltaSeconds);
        }
    }

    public Optional<Keyframe> currentPlaybackKeyframe() {
        return activePlayback == null ? Optional.empty() : Optional.of(activePlayback.currentKeyframe());
    }

    public boolean isPlaying() {
        return activePlayback != null;
    }

    private CommandResult startPlayback(String name, double seconds) {
        Optional<CameraSequence> sequence = storage.load(name);
        if (sequence.isEmpty()) {
            return missingOrCorrupt(name);
        }
        if (sequence.get().size() < 2) {
            return CommandResult.failure("camkey.error.insufficient_keyframes", name);
        }
        if (seconds <= 0.0 || seconds > MAX_PLAYBACK_SECONDS) {
            return CommandResult.failure("camkey.error.invalid_duration", seconds);
        }
        activePlayback = new PlaybackSession(sequence.get(), seconds, Easing.SMOOTHSTEP);
        return CommandResult.success("camkey.play.started", name, seconds);
    }

    private CommandResult missingOrCorrupt(String name) {
        return storage.exists(name)
                ? CommandResult.failure("camkey.error.corrupt_sequence", name)
                : CommandResult.failure("camkey.error.unknown_sequence", name);
    }
}
