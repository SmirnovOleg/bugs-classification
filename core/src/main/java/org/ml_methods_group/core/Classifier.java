package org.ml_methods_group.core;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

public interface Classifier<T, M> extends Serializable {
    void train(Map<T, M> samples);

    default M classify(T value) {
        return reliability(value)
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    default Map<M, Double> reliability(T value) {
        return Collections.singletonMap(classify(value), 1.0);
    }
}
