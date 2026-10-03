package com.tous.camkey.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tous.camkey.model.CameraSequence;

/**
 * One pretty-printed JSON file per sequence, {@code <baseDirectory>/<name>.json}. Each file carries a
 * {@code formatVersion} so the layout can change later without misreading older saves.
 */
public class JsonSequenceStorage implements SequenceStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonSequenceStorage.class);
    private static final String FILE_EXTENSION = ".json";
    private static final String FORMAT_VERSION_KEY = "formatVersion";
    /** Bump when the file layout changes, and teach {@link #load} to upgrade the older layouts. */
    static final int CURRENT_FORMAT_VERSION = 1;

    private final Path baseDirectory;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public JsonSequenceStorage(Path baseDirectory) {
        this.baseDirectory = baseDirectory.normalize();
    }

    @Override
    public boolean save(CameraSequence sequence) {
        Path target = resolveSequenceFile(sequence.name());
        Path tempFile = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            Files.createDirectories(baseDirectory);
            Files.writeString(tempFile, gson.toJson(toJson(sequence)), StandardCharsets.UTF_8);
            Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (IOException e) {
            LOGGER.warn("Could not save camera sequence '{}': {}", sequence.name(), e.getMessage());
            deleteQuietly(tempFile);
            return false;
        }
    }

    @Override
    public Optional<CameraSequence> load(String name) {
        Path file = resolveSequenceFile(name);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            JsonElement root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (!root.isJsonObject()) {
                LOGGER.warn("Could not load camera sequence '{}': file is not a JSON object", name);
                return Optional.empty();
            }
            JsonObject json = root.getAsJsonObject();
            // Files saved before versioning was added have no version field; their layout is version 1.
            int version = json.has(FORMAT_VERSION_KEY) ? json.remove(FORMAT_VERSION_KEY).getAsInt() : 1;
            if (version > CURRENT_FORMAT_VERSION) {
                LOGGER.warn("Could not load camera sequence '{}': saved in format {} by a newer CamKey, "
                        + "this version reads up to format {}", name, version, CURRENT_FORMAT_VERSION);
                return Optional.empty();
            }
            return Optional.of(gson.fromJson(json, CameraSequence.class));
        } catch (IOException | RuntimeException e) {
            // RuntimeException, not just JsonSyntaxException: valid JSON with the wrong shape (missing
            // name, null keyframe, NaN value) fails inside the record constructors, and Gson rethrows
            // that as a plain RuntimeException.
            LOGGER.warn("Could not load camera sequence '{}': {}", name, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean exists(String name) {
        return Files.isRegularFile(resolveSequenceFile(name));
    }

    @Override
    public List<String> listNames() {
        if (!Files.isDirectory(baseDirectory)) {
            return List.of();
        }
        try (var files = Files.list(baseDirectory)) {
            List<String> names = new ArrayList<>();
            files.filter(path -> path.toString().endsWith(FILE_EXTENSION))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        names.add(fileName.substring(0, fileName.length() - FILE_EXTENSION.length()));
                    });
            names.sort(String::compareTo);
            return names;
        } catch (IOException e) {
            LOGGER.warn("Could not list camera sequences: {}", e.getMessage());
            return List.of();
        }
    }

    private JsonObject toJson(CameraSequence sequence) {
        JsonObject json = new JsonObject();
        // Version first, so it's the first thing anyone hand-editing the file sees.
        json.addProperty(FORMAT_VERSION_KEY, CURRENT_FORMAT_VERSION);
        gson.toJsonTree(sequence).getAsJsonObject().entrySet()
                .forEach(entry -> json.add(entry.getKey(), entry.getValue()));
        return json;
    }

    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // Best effort — a leftover .tmp file is harmless and excluded from listNames().
        }
    }

    private Path resolveSequenceFile(String name) {
        Path resolved = baseDirectory.resolve(name + FILE_EXTENSION).normalize();
        if (!baseDirectory.equals(resolved.getParent())) {
            throw new IllegalArgumentException("Invalid sequence name: " + name);
        }
        return resolved;
    }
}
