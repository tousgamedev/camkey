package com.tous.camkey.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
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
import com.google.gson.JsonSyntaxException;
import com.tous.camkey.model.CameraSequence;

public class JsonSequenceStorage implements SequenceStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonSequenceStorage.class);
    private static final String FILE_EXTENSION = ".json";

    private final Path baseDirectory;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public JsonSequenceStorage(Path baseDirectory) {
        this.baseDirectory = baseDirectory.normalize();
    }

    @Override
    public void save(CameraSequence sequence) {
        Path target = resolveSequenceFile(sequence.name());
        Path tempFile = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            Files.createDirectories(baseDirectory);
            Files.writeString(tempFile, gson.toJson(sequence), StandardCharsets.UTF_8);
            Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save sequence '" + sequence.name() + "'", e);
        }
    }

    @Override
    public Optional<CameraSequence> load(String name) {
        Path file = resolveSequenceFile(name);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            return Optional.ofNullable(gson.fromJson(json, CameraSequence.class));
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.warn("Could not load camera sequence '{}': {}", name, e.getMessage());
            return Optional.empty();
        }
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

    private Path resolveSequenceFile(String name) {
        Path resolved = baseDirectory.resolve(name + FILE_EXTENSION).normalize();
        if (!baseDirectory.equals(resolved.getParent())) {
            throw new IllegalArgumentException("Invalid sequence name: " + name);
        }
        return resolved;
    }
}
