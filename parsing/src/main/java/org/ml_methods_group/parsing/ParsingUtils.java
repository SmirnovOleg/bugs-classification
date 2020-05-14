package org.ml_methods_group.parsing;

import org.ml_methods_group.common.Dataset;
import org.ml_methods_group.common.Solution;
import org.ml_methods_group.common.Solution.Verdict;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.IntPredicate;

import static org.ml_methods_group.common.Solution.Verdict.FAIL;
import static org.ml_methods_group.common.Solution.Verdict.OK;

public class ParsingUtils {

    public static Dataset parseAllSolutions(InputStream stream, CodeValidator validator,
                                            IntPredicate problemFilter) throws IOException {
        return parseSolutions(stream, new JavaCodeValidator(), problemFilter, false);
    }

    public static Dataset parseOnlyLastSolutions(InputStream stream, CodeValidator validator,
                                                 IntPredicate problemFilter) throws IOException {
        return parseSolutions(stream, new JavaCodeValidator(), problemFilter, true);
    }

    private static Dataset parseSolutions(InputStream stream, CodeValidator validator,
                                          IntPredicate problemFilter, boolean onlyLast) throws IOException {
        CSVParser<Column> parser = new CSVParser<>(stream, Column::byName, Column.class);
        final HashMap<Long, Integer> sessionIds = new HashMap<>();
        final Map<Long, Solution> lastSolution = new HashMap<>();
        final Map<Long, Long> lastTime = new HashMap<>();
        final Map<Long, Long> firstTime = new HashMap<>();
        final ArrayList<Solution> allSolutions = new ArrayList<>();

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
            final long time = parser.getLongOrDefault(Column.TIME, Long.MAX_VALUE);

            if (onlyLast &&
                    (time >= lastTime.getOrDefault(id, Long.MIN_VALUE) && verdict == FAIL
                            || time <= firstTime.getOrDefault(id, Long.MAX_VALUE) && verdict == OK)) {
                final var sessionId = sessionIds.get(id >> 1);
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
            } else if (!onlyLast) {
                final var sessionId = sessionIds.get(id >> 1);
                final Solution solution = new Solution(
                        code.get(),
                        problemId,
                        sessionId,
                        sessionId * 100 + verdict.ordinal(),
                        verdict);
                allSolutions.add(solution);
            }
        }
        if (onlyLast)
            return new Dataset(lastSolution.values());
        else
            return new Dataset(allSolutions);
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
