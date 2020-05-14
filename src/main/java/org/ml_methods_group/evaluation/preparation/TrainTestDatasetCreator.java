package org.ml_methods_group.evaluation.preparation;

import org.ml_methods_group.common.Dataset;
import org.ml_methods_group.common.Solution;
import org.ml_methods_group.common.serialization.ProtobufSerializationUtils;
import org.ml_methods_group.evaluation.EvaluationInfo;
import org.ml_methods_group.parsing.JavaCodeValidator;
import org.ml_methods_group.parsing.ParsingUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.ml_methods_group.common.Solution.Verdict.FAIL;
import static org.ml_methods_group.common.Solution.Verdict.OK;
import static org.ml_methods_group.common.serialization.ProtobufSerializationUtils.loadDataset;
import static org.ml_methods_group.common.serialization.ProtobufSerializationUtils.storeDataset;

public class TrainTestDatasetCreator {

    public static void main(String[] args) throws IOException {
        long seed = 124345;
        int problemId = 49950;
        int testSize = 200;

        var data = EvaluationInfo.PATH_TO_DATASET.resolve("dataset2.csv");
        var storage = EvaluationInfo.PATH_TO_DATASET.resolve("dataset2_full.tmp");
        try (InputStream input = new FileInputStream(data.toFile())) {
            final Dataset dataset = ParsingUtils.parseAllSolutions(input, new JavaCodeValidator(), x -> true);
            ProtobufSerializationUtils.storeDataset(dataset, storage);
        }

//        final Random random = new Random(seed);
//        final Dataset full = loadDataset(EvaluationInfo.PATH_TO_DATASET.resolve("dataset2.tmp"));
//        List<Solution> solutions = full.getValues(x -> x.getProblemId() == problemId);
//        Map<Integer, List<Solution>> sessionById = new HashMap<>();
//        for (Solution solution : solutions) {
//            sessionById.computeIfAbsent(solution.getSessionId(), x -> new ArrayList<>()).add(solution);
//        }
//        List<List<Solution>> sessions = new ArrayList<>(sessionById.values());
//        Collections.shuffle(sessions, random);
//        List<Solution> train = new ArrayList<>();
//        List<Solution> test = new ArrayList<>();
//        for (var session : sessions) {
//            List<Solution> correct = session.stream()
//                    .filter(x -> x.getVerdict() == OK)
//                    .collect(Collectors.toList());
//            List<Solution> incorrect = session.stream()
//                    .filter(x -> x.getVerdict() == FAIL)
//                    .collect(Collectors.toList());
//            long currentTestSize = test.stream().filter(x -> x.getVerdict() == FAIL).count();
//            if (currentTestSize < testSize && !incorrect.isEmpty()) {
//                test.addAll(incorrect);
//                test.addAll(correct);
//            } else {
//                train.addAll(incorrect);
//                train.addAll(correct);
//            }
//        }
//
//        try {
//            Path pathToDataset = EvaluationInfo.PATH_TO_DATASET.resolve("filter");
//            storeDataset(new Dataset(train), pathToDataset.resolve("train.tmp"));
//            storeDataset(new Dataset(test), pathToDataset.resolve("test.tmp"));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

}
