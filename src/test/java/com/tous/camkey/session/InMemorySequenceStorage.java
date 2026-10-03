package com.tous.camkey.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.tous.camkey.model.CameraSequence;
import com.tous.camkey.storage.SequenceStorage;

/** Test double for {@link SequenceStorage}: keeps sequences in a map and can simulate failures. */
class InMemorySequenceStorage implements SequenceStorage {

    private final Map<String, CameraSequence> sequences = new HashMap<>();
    private final Set<String> corrupt = new HashSet<>();
    boolean failSaves;

    void markCorrupt(String name) {
        corrupt.add(name);
    }

    @Override
    public boolean save(CameraSequence sequence) {
        if (failSaves) {
            return false;
        }
        sequences.put(sequence.name(), sequence);
        corrupt.remove(sequence.name());
        return true;
    }

    @Override
    public Optional<CameraSequence> load(String name) {
        return corrupt.contains(name) ? Optional.empty() : Optional.ofNullable(sequences.get(name));
    }

    @Override
    public boolean exists(String name) {
        return corrupt.contains(name) || sequences.containsKey(name);
    }

    @Override
    public List<String> listNames() {
        Set<String> names = new HashSet<>(sequences.keySet());
        names.addAll(corrupt);
        List<String> sorted = new ArrayList<>(names);
        sorted.sort(String::compareTo);
        return sorted;
    }
}
