package com.example.echosight.logic;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SemanticMapper {

    private static final Set<String> OBSTACLES = new HashSet<>(Arrays.asList(
            "person", "bench", "chair","bottle","book","laptop"
    ));

    private static final Set<String> IGNORE = new HashSet<>(Arrays.asList(
            "bird", "cat", "dog", "cup",  "book",
            "fork", "knife", "spoon", "banana", "apple","couch", "bed",
            "car", "bus", "truck", "motorcycle", "bicycle",
            "potted plant", "fire hydrant", "stop sign",
            "traffic light", "parking meter"
    ));

    public enum SemanticType {
        OBSTACLE,
        FREE_SPACE,
        IGNORE
    }

    public static SemanticType classify(String label) {
        if (label == null) return SemanticType.IGNORE;

        if (OBSTACLES.contains(label)) return SemanticType.OBSTACLE;
        if (IGNORE.contains(label)) return SemanticType.IGNORE;

        // Unknown objects → treat as obstacle for safety
        return SemanticType.OBSTACLE;
    }
}
