package org.ml_methods_group.common.metrics.representatives;

import org.ml_methods_group.common.Cluster;
import org.ml_methods_group.common.DistanceFunction;
import org.ml_methods_group.common.ManyOptionsSelector;
import org.ml_methods_group.common.RepresentativesPicker;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CentroidPicker<V, O> implements RepresentativesPicker<V, O> {

    private final ManyOptionsSelector<V, O> selector;
    private final DistanceFunction<O> metric;

    public CentroidPicker(DistanceFunction<O> metric,
                          ManyOptionsSelector<V, O> selector) {
        this.metric = metric;
        this.selector = selector;
    }

    @Override
    public List<O> getRepresentatives(Cluster<V> values) {
        final List<O> options = values.stream()
                .map(selector::selectOptions)
                .map(Optional::get)
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
        double minimalTotalDistanceToOthers = Double.MAX_VALUE;
        O center = options.get(0);
        for (O current : options) {
            double totalDistance = options.stream()
                    .map(other -> metric.distance(current, other))
                    .mapToDouble(Double::doubleValue)
                    .sum();
            if (totalDistance < minimalTotalDistanceToOthers) {
                minimalTotalDistanceToOthers = totalDistance;
                center = current;
            }
        }
        return List.of(center);
    }

}