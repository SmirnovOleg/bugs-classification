package ru.spbau.mit.lobanov.preparation;

import ru.spbau.mit.lobanov.changes.AtomicChange;
import ru.spbau.mit.lobanov.changes.ChangeUtils;
import ru.spbau.mit.lobanov.database.Database;
import ru.spbau.mit.lobanov.database.Table;
import ru.spbau.mit.lobanov.database.Tables;

import java.io.IOException;
import java.sql.SQLException;

public class DiffBuilder {
    public static void insertDiffs(Database database) throws SQLException, IOException {
        database.dropTable(Tables.diff_header);
        database.createTable(Tables.diff_header);
        final Table diff = database.getTable(Tables.diff_header);
        final Table code = database.getTable(Tables.codes_header);
        final Object[] buffer = new Object[diff.columnCount()];
        for (Table.ResultWrapper result : code) {
            final String id = result.getStringValue("id");
            if (id.endsWith("_0")) {
                continue;
            }
            final int sessionId = Integer.parseInt(id.substring(0, id.length() - 2));
            final String before = code.findFirst(sessionId + "_0").getStringValue("code");
            final String after = result.getStringValue("code");
            buffer[0] = sessionId;
            for (AtomicChange change : ChangeUtils.calculateChanges(before, after)) {
                change.storeData(buffer, 1);
                diff.insert(buffer);
            }
        }
    }

    public static void main(String[] args) throws IOException, SQLException {
        try (Database database = new Database()) {
            insertDiffs(database);
        }
    }
}
