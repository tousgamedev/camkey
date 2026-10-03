package com.tous.camkey.storage;

import java.util.List;
import java.util.Optional;

import com.tous.camkey.model.CameraSequence;

public interface SequenceStorage {

    /** Returns false (and logs why) if the sequence could not be written. */
    boolean save(CameraSequence sequence);

    Optional<CameraSequence> load(String name);

    boolean exists(String name);

    List<String> listNames();
}
