package com.tous.camkey.session;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tous.camkey.model.Keyframe;

class CamKeySessionTest {

    private static final double DEFAULT_SECONDS = 7.0;

    private InMemorySequenceStorage storage;
    private CamKeySession session;

    @BeforeEach
    void setUp() {
        storage = new InMemorySequenceStorage();
        session = new CamKeySession(storage, () -> DEFAULT_SECONDS);
    }

    private static Keyframe at(double x) {
        return new Keyframe(x, 64, 0, 0.0f, 0.0f);
    }

    private void addTwoKeyframes(String name) {
        session.add(name, at(0));
        session.add(name, at(10));
    }

    private static void assertFailure(String expectedKey, CommandResult result) {
        assertFalse(result.success(), "expected failure " + expectedKey);
        assertEquals(expectedKey, result.translationKey());
    }

    private static void assertSuccess(String expectedKey, CommandResult result) {
        assertTrue(result.success(), "expected success " + expectedKey + " but got " + result.translationKey());
        assertEquals(expectedKey, result.translationKey());
    }

    @Test
    void addCreatesThenAppendsAndMakesTheSequenceActive() {
        assertSuccess("camkey.add.success", session.add("intro", at(0)));
        CommandResult second = session.add("intro", at(10));
        assertArrayEquals(new Object[] {"intro", 2}, second.args());
        assertEquals("intro", session.activeSequenceName().orElseThrow());
    }

    @Test
    void keybindActionsFailWithoutAnActiveSequence() {
        assertFailure("camkey.error.no_active_sequence", session.addToActive(at(0)));
        assertFailure("camkey.error.no_active_sequence", session.deleteLastFromActive());
        assertFailure("camkey.error.no_active_sequence", session.toggleActivePlayback(true));
        assertFailure("camkey.error.no_active_sequence", session.playActive());
    }

    @Test
    void useNeverCreatesAndPointsAtAdd() {
        assertFailure("camkey.error.use_unknown_sequence", session.use("missing"));
        assertTrue(session.activeSequenceName().isEmpty());
        assertFalse(storage.exists("missing"));
    }

    @Test
    void deleteLastPopsInOrderThenReportsEmpty() {
        addTwoKeyframes("intro");
        assertArrayEquals(new Object[] {1}, session.deleteLastFromActive().args());
        assertArrayEquals(new Object[] {0}, session.deleteLastFromActive().args());
        assertFailure("camkey.error.sequence_empty", session.deleteLastFromActive());
    }

    @Test
    void playRejectsUnknownShortAndBadDurations() {
        assertFailure("camkey.error.unknown_sequence", session.play("missing", 10));
        session.add("single", at(0));
        assertFailure("camkey.error.insufficient_keyframes", session.play("single", 10));
        addTwoKeyframes("intro");
        assertFailure("camkey.error.invalid_duration", session.play("intro", 0));
        assertFailure("camkey.error.invalid_duration", session.play("intro", -5));
        assertFailure("camkey.error.invalid_duration", session.play("intro", CamKeySession.MAX_PLAYBACK_SECONDS + 1));
        assertFalse(session.isPlaying());
    }

    @Test
    void playWhilePlayingFailsInsteadOfCancelling() {
        addTwoKeyframes("intro");
        assertSuccess("camkey.play.started", session.play("intro", 10));
        assertFailure("camkey.error.already_playing", session.play("intro", 10));
        assertFailure("camkey.error.already_playing", session.playActive());
        assertTrue(session.isPlaying());
    }

    @Test
    void omittedDurationUsesTheConfiguredDefault() {
        addTwoKeyframes("intro");
        assertArrayEquals(new Object[] {"intro", DEFAULT_SECONDS}, session.play("intro").args());
        session.cancelPlayback();
        assertArrayEquals(new Object[] {"intro", DEFAULT_SECONDS}, session.playActive().args());
    }

    @Test
    void togglesStartAndCancelAndRememberHintMode() {
        addTwoKeyframes("intro");
        assertSuccess("camkey.play.started", session.toggleActivePlayback(false));
        assertFalse(session.isHintVisible(), "Record mode hides the hint");
        assertSuccess("camkey.play.cancelled", session.toggleActivePlayback(true));
        assertFalse(session.isPlaying());

        session.play("intro", 10);
        assertTrue(session.isHintVisible(), "typed commands always show the hint");
    }

    @Test
    void corruptSequenceIsReportedAndNeverOverwritten() {
        addTwoKeyframes("intro");
        storage.markCorrupt("intro");
        assertFailure("camkey.error.corrupt_sequence", session.add("intro", at(20)));
        assertFailure("camkey.error.corrupt_sequence", session.use("intro"));
        assertFailure("camkey.error.corrupt_sequence", session.play("intro", 10));
        assertTrue(storage.exists("intro"));
    }

    @Test
    void saveFailureIsReportedNotThrown() {
        storage.failSaves = true;
        assertFailure("camkey.error.save_failed", session.add("intro", at(0)));
    }

    @Test
    void listFlagsCorruptEntries() {
        addTwoKeyframes("intro");
        storage.markCorrupt("broken");
        List<?> entries = (List<?>) session.list().args()[0];
        assertEquals(2, entries.size());
        Translatable broken = (Translatable) entries.get(0);
        assertEquals("camkey.list.entry_corrupt", broken.key());
        assertArrayEquals(new Object[] {"broken"}, broken.args());
        Translatable intro = (Translatable) entries.get(1);
        assertEquals("camkey.list.entry", intro.key());
        assertArrayEquals(new Object[] {"intro", 2}, intro.args());
    }
}
