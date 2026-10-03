package com.tous.camkey.session;

import java.util.List;
import java.util.Optional;
import java.util.function.DoubleSupplier;

import com.tous.camkey.interpolation.Easing;
import com.tous.camkey.model.CameraSequence;
import com.tous.camkey.model.Keyframe;
import com.tous.camkey.playback.PlaybackSession;
import com.tous.camkey.storage.SequenceStorage;

/**
 * The mod's one piece of session state (which sequence is active, what's playing) and every user
 * action on it. Commands and keybinds both call into this and only report the result. No Minecraft
 * dependency, so the rules here can be unit-tested without a running game.
 */
public class CamKeySession {

    public static final double MAX_PLAYBACK_SECONDS = 3600.0;

    private final SequenceStorage storage;
    private final DoubleSupplier defaultPlaybackSeconds;
    private String activeSequenceName;
    private PlaybackSession activePlayback;
    private boolean hintVisible;

    public CamKeySession(SequenceStorage storage, DoubleSupplier defaultPlaybackSeconds) {
        this.storage = storage;
        this.defaultPlaybackSeconds = defaultPlaybackSeconds;
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
        if (!storage.save(updated)) {
            return CommandResult.failure("camkey.error.save_failed", name);
        }
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
            // `use` never creates, so point at the command that does.
            return storage.exists(name)
                    ? CommandResult.failure("camkey.error.corrupt_sequence", name)
                    : CommandResult.failure("camkey.error.use_unknown_sequence", name);
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
        if (!storage.save(updated.get())) {
            return CommandResult.failure("camkey.error.save_failed", name);
        }
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
        List<Translatable> entries = names.stream()
                .map(name -> storage.load(name)
                        .map(sequence -> new Translatable("camkey.list.entry", name, sequence.size()))
                        .orElseGet(() -> new Translatable("camkey.list.entry_corrupt", name)))
                .toList();
        return CommandResult.success("camkey.list.result", entries);
    }

    public Optional<String> activeSequenceName() {
        return Optional.ofNullable(activeSequenceName);
    }

    public CommandResult play(String name) {
        return play(name, defaultPlaybackSeconds.getAsDouble());
    }

    public CommandResult play(String name, double seconds) {
        if (activePlayback != null) {
            return CommandResult.failure("camkey.error.already_playing");
        }
        // The typed commands always show the hint — only the Record keybind hides it.
        return startPlayback(name, seconds, true);
    }

    public CommandResult playActive() {
        return playActive(defaultPlaybackSeconds.getAsDouble());
    }

    public CommandResult playActive(double seconds) {
        if (activePlayback != null) {
            return CommandResult.failure("camkey.error.already_playing");
        }
        if (activeSequenceName == null) {
            return CommandResult.failure("camkey.error.no_active_sequence");
        }
        return startPlayback(activeSequenceName, seconds, true);
    }

    public CommandResult toggleActivePlayback(boolean showHint) {
        if (activePlayback != null) {
            return cancelPlayback();
        }
        if (activeSequenceName == null) {
            return CommandResult.failure("camkey.error.no_active_sequence");
        }
        return startPlayback(activeSequenceName, defaultPlaybackSeconds.getAsDouble(), showHint);
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

    public Optional<Keyframe> playbackKeyframeAt(double partialTick) {
        return activePlayback == null ? Optional.empty() : Optional.of(activePlayback.keyframeAt(partialTick));
    }

    public boolean isPlaying() {
        return activePlayback != null;
    }

    public boolean isHintVisible() {
        return activePlayback != null && hintVisible;
    }

    private CommandResult startPlayback(String name, double seconds, boolean showHint) {
        Optional<CameraSequence> sequence = storage.load(name);
        if (sequence.isEmpty()) {
            return missingOrCorrupt(name);
        }
        if (sequence.get().size() < 2) {
            return CommandResult.failure("camkey.error.insufficient_keyframes", name);
        }
        if (seconds <= 0.0 || seconds > MAX_PLAYBACK_SECONDS) {
            return CommandResult.failure("camkey.error.invalid_duration", seconds, (int) MAX_PLAYBACK_SECONDS);
        }
        activePlayback = new PlaybackSession(sequence.get(), seconds, Easing.SMOOTHSTEP);
        hintVisible = showHint;
        return CommandResult.success("camkey.play.started", name, seconds);
    }

    private CommandResult missingOrCorrupt(String name) {
        return storage.exists(name)
                ? CommandResult.failure("camkey.error.corrupt_sequence", name)
                : CommandResult.failure("camkey.error.unknown_sequence", name);
    }
}
