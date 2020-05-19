package org.ml_methods_group.evaluation.preparation;

import org.ml_methods_group.common.Dataset;
import org.ml_methods_group.common.Solution;
import org.ml_methods_group.evaluation.EvaluationInfo;

import java.io.IOException;
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

//        var data = EvaluationInfo.PATH_TO_DATASET.resolve("dataset2.csv");
//        var storage = EvaluationInfo.PATH_TO_DATASET.resolve("dataset2-full.tmp");
//        try (InputStream input = new FileInputStream(data.toFile())) {
//            final Dataset dataset = ParsingUtils.parseAllSolutions(input, new JavaCodeValidator(), x -> true);
//            storeDataset(dataset, storage);
//        }

        final Random random = new Random(seed);
        final Dataset full = loadDataset(EvaluationInfo.PATH_TO_DATASET.resolve("dataset2-full.tmp"));
        List<Solution> solutions = full.getValues(x -> x.getProblemId() == problemId);
        Map<Integer, List<Solution>> sessionById = new HashMap<>();
        for (Solution solution : solutions) {
            sessionById.computeIfAbsent(solution.getSessionId(), x -> new ArrayList<>()).add(solution);
        }
        List<List<Solution>> sessions = new ArrayList<>(sessionById.values());
        Collections.shuffle(sessions, random);
        List<Solution> train = new ArrayList<>();
        List<Solution> test = new ArrayList<>();
        for (var session : sessions) {
            List<Solution> allCorrect = session.stream()
                    .filter(x -> x.getVerdict() == OK)
                    .collect(Collectors.toList());
            List<Solution> allIncorrect = session.stream()
                    .filter(x -> x.getVerdict() == FAIL)
                    .collect(Collectors.toList());
            long currentTestSize = test.stream().filter(x -> x.getVerdict() == FAIL).count();
            Optional<Solution> firstCorrect = allCorrect.stream().min(Comparator.comparing(Solution::getSolutionId));
            if (currentTestSize < testSize && !allIncorrect.isEmpty()) {
                System.out.println(allIncorrect.size() + " " + allCorrect.size());
                Solution incorrect = allIncorrect.get(random.nextInt(allIncorrect.size()));
                test.add(incorrect);
                firstCorrect.ifPresent(test::add);
            } else {
                if (!allIncorrect.isEmpty()) {
                    Solution incorrect = allIncorrect.get(random.nextInt(allIncorrect.size()));
                    train.add(incorrect);
                }
                firstCorrect.ifPresent(train::add);
            }
        }

        try {
            Path pathToDataset = EvaluationInfo.PATH_TO_DATASET.resolve("integral");
            storeDataset(new Dataset(train), pathToDataset.resolve("train_rand.tmp"));
            storeDataset(new Dataset(test), pathToDataset.resolve("test_rand.tmp"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
