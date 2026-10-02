package com.tous.camkey.storage;

import java.util.List;
import java.util.Optional;

import com.tous.camkey.model.CameraSequence;

public interface SequenceStorage {

    void save(CameraSequence sequence);

    Optional<CameraSequence> load(String name);

    List<String> listNames();
}
