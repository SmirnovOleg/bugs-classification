package org.ml_methods_group.parsing;

import org.ml_methods_group.common.Dataset;
import org.ml_methods_group.common.Solution;
import org.ml_methods_group.common.Solution.Verdict;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.IntPredicate;

import static org.ml_methods_group.common.Solution.Verdict.FAIL;
import static org.ml_methods_group.common.Solution.Verdict.OK;

public class ParsingUtils {

    public static Dataset parseOnlyLastSolutions(InputStream stream, CodeValidator validator,
                                                 IntPredicate problemFilter) throws IOException {
        CSVParser<Column> parser = new CSVParser<>(stream, Column::byName, Column.class);
        final HashMap<Long, Integer> sessionIds = new HashMap<>();
        final Map<Long, Solution> lastSolution = new HashMap<>();
        final Map<Long, Long> lastTime = new HashMap<>();
        final Map<Long, Long> firstTime = new HashMap<>();
        final Map<Integer, List<Long>> timestampsBySessionId = new HashMap<>();

        while (parser.hasNextLine()) {
            parser.nextLine();
            final int problemId = parser.getInt(Column.PROBLEM_ID);
            if (!problemFilter.test(problemId)) {
                continue;
            }
            final Optional<String> code = validator.validate(parser.getToken(Column.CODE));
            if (code.isEmpty()) {
                continue;
            }
            final int userId = parser.getInt(Column.USER_ID);
            final Verdict verdict = parser.getBoolean(Column.VERDICT) ? OK : FAIL;
            final long id = (((long) userId) << 32) | (problemId << 1) | verdict.ordinal();
            final long time = parser.getLongOrDefault(Column.TIME, Long.MAX_VALUE);

            sessionIds.putIfAbsent(id >> 1, sessionIds.size());
            final var sessionId = sessionIds.get(id >> 1);
            timestampsBySessionId.putIfAbsent(sessionId, new ArrayList<>());
            timestampsBySessionId.get(sessionId).add(time);

            if (time >= lastTime.getOrDefault(id, Long.MIN_VALUE) && verdict == FAIL
                    || time <= firstTime.getOrDefault(id, Long.MAX_VALUE) && verdict == OK) {
                final Solution solution = new Solution(
                        code.get(),
                        problemId,
                        sessionId,
                        sessionId * 10 + verdict.ordinal(),
                        verdict);
                lastSolution.put(id, solution);
                if (verdict == FAIL) {
                    lastTime.put(id, time);
                } else {
                    firstTime.put(id, time);
                }
            }
        }
        return new Dataset(lastSolution.values());
    }

    public static Dataset parseAllSolutions(InputStream stream, CodeValidator validator,
                                             IntPredicate problemFilter) throws IOException {
        CSVParser<Column> parser = new CSVParser<>(stream, Column::byName, Column.class);
        final HashMap<Long, Integer> sessionIds = new HashMap<>();
        final Map<Integer, List<Long>> timestampsBySessionId = new HashMap<>();
        final Map<Solution, Long> timestampBySolution = new HashMap<>();
        final ArrayList<Solution> unsortedSolutions = new ArrayList<>();
        final ArrayList<Solution> sortedSolutions = new ArrayList<>();

        while (parser.hasNextLine()) {
            parser.nextLine();
            final int problemId = parser.getInt(Column.PROBLEM_ID);
            if (!problemFilter.test(problemId)) {
                continue;
            }
            final Optional<String> code = validator.validate(parser.getToken(Column.CODE));
            if (code.isEmpty()) {
                continue;
            }
            final int userId = parser.getInt(Column.USER_ID);
            final Verdict verdict = parser.getBoolean(Column.VERDICT) ? OK : FAIL;
            final long id = (((long) userId) << 32) | (problemId << 1) | verdict.ordinal();
            sessionIds.putIfAbsent(id >> 1, sessionIds.size());
            final var sessionId = sessionIds.get(id >> 1);
            final long time = parser.getLongOrDefault(Column.TIME, Long.MAX_VALUE);
            timestampsBySessionId.putIfAbsent(sessionId, new ArrayList<>());
            int timestampIndex = timestampsBySessionId.get(sessionId).size();
            timestampsBySessionId.get(sessionId).add(time);
            final Solution solution = new Solution(
                    code.get(),
                    problemId,
                    sessionId,
                    sessionId * 1000 + timestampIndex,
                    verdict);
            timestampBySolution.put(solution, time);
            unsortedSolutions.add(solution);
        }

        // Update solutions ids to follow natural order of timestamps
        int max = 0;
        for (var timestamps : timestampsBySessionId.values()) {
            timestamps.sort(Comparator.naturalOrder());
            max = Math.max(max, timestamps.size());
        }
        System.out.println(max);
        for (Solution solution : unsortedSolutions) {
            long timestamp = timestampBySolution.get(solution);
            List<Long> timestamps = timestampsBySessionId.get(solution.getSessionId());
            int timestampIndex = Collections.binarySearch(timestamps, timestamp);
            sortedSolutions.add(new Solution(
                    solution.getCode(),
                    solution.getProblemId(),
                    solution.getSessionId(),
                    solution.getSessionId() * 1000 + timestampIndex,
                    solution.getVerdict()));
        }
        return new Dataset(sortedSolutions);
    }

    private enum Column {
        USER_ID("\\S*user_id\\S*", "\\S*data_id\\S*"),
        PROBLEM_ID("\\S*step_id\\S*"),
        VERDICT("\\S*is_passed\\S*", "\\S*status\\S*"),
        CODE("\\S*submission_code\\S*", "\\S*code\\S*"),
        TIME("\\S*timestamp\\S*");

        final String[] names;

        Column(String... names) {
            this.names = names;
        }

        static Optional<Column> byName(String token) {
            for (Column column : values()) {
                if (Arrays.stream(column.names).anyMatch(token::matches)) {
                    return Optional.of(column);
                }
            }
            return Optional.empty();
        }
    }
}
