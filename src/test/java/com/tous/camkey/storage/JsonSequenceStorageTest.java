package com.tous.camkey.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.tous.camkey.model.CameraSequence;
import com.tous.camkey.model.Keyframe;

class JsonSequenceStorageTest {

    @TempDir
    Path directory;

    private JsonSequenceStorage storage;

    @BeforeEach
    void setUp() {
        storage = new JsonSequenceStorage(directory);
    }

    private static CameraSequence intro() {
        return new CameraSequence("intro", List.of(
                new Keyframe(1.5, 64, -3, 90.0f, -10.0f),
                new Keyframe(10, 70, 5, 180.0f, 15.0f)));
    }

    private void writeRaw(String name, String json) throws IOException {
        Files.writeString(directory.resolve(name + ".json"), json, StandardCharsets.UTF_8);
    }

    @Test
    void roundTripsASequence() {
        assertTrue(storage.save(intro()));
        assertEquals(intro(), storage.load("intro").orElseThrow());
        assertEquals(List.of("intro"), storage.listNames());
    }

    @Test
    void writesTheFormatVersion() throws IOException {
        storage.save(intro());
        String json = Files.readString(directory.resolve("intro.json"), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"formatVersion\": " + JsonSequenceStorage.CURRENT_FORMAT_VERSION), json);
    }

    @Test
    void readsFilesSavedBeforeVersioningExisted() throws IOException {
        writeRaw("legacy", """
                {"name": "legacy", "keyframes": [
                  {"x": 0, "y": 64, "z": 0, "yaw": 0, "pitch": 0},
                  {"x": 5, "y": 64, "z": 0, "yaw": 0, "pitch": 0}]}
                """);
        assertEquals(2, storage.load("legacy").orElseThrow().size());
    }

    @Test
    void refusesFilesFromANewerFormatRatherThanMisreadingThem() throws IOException {
        writeRaw("future", """
                {"formatVersion": 99, "name": "future", "keyframes": []}
                """);
        assertTrue(storage.load("future").isEmpty());
        assertTrue(storage.exists("future"), "reported as unreadable, not missing");
    }

    @Test
    void badFilesLoadAsEmptyInsteadOfThrowing() throws IOException {
        writeRaw("syntax", "{ not json");
        writeRaw("empty", "");
        writeRaw("array", "[]");
        writeRaw("noname", "{\"keyframes\": []}");
        writeRaw("nullframe", "{\"name\": \"nullframe\", \"keyframes\": [null]}");
        writeRaw("nan", "{\"name\": \"nan\", \"keyframes\": [{\"x\": NaN, \"y\": 0, \"z\": 0, \"yaw\": 0, \"pitch\": 0}]}");
        for (String name : List.of("syntax", "empty", "array", "noname", "nullframe", "nan")) {
            assertTrue(storage.load(name).isEmpty(), name + " should not load");
            assertTrue(storage.exists(name), name + " should still count as existing");
        }
    }

    @Test
    void missingSequenceIsNeitherLoadedNorExisting() {
        assertTrue(storage.load("nothing").isEmpty());
        assertFalse(storage.exists("nothing"));
        assertEquals(List.of(), storage.listNames());
    }

    @Test
    void leftoverTempFilesAreNotListed() throws IOException {
        storage.save(intro());
        Files.writeString(directory.resolve("other.json.tmp"), "{}", StandardCharsets.UTF_8);
        assertEquals(List.of("intro"), storage.listNames());
    }
}
