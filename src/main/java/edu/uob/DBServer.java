package edu.uob;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** This class implements the DB server. */
public class DBServer {

    private static final char END_OF_TRANSMISSION = 4;
    private final String storageFolderPath;
    private String currentDatabaseName;
    private static final Set<String> RESERVED = Set.of(
        "USE", "CREATE", "DATABASE", "TABLE", "DROP", "ALTER", "INSERT", "INTO", "VALUES",
        "SELECT", "FROM", "WHERE", "UPDATE", "SET", "DELETE", "JOIN", "AND", "OR", "ON",
        "TRUE", "FALSE", "LIKE", "NULL", "SHOW", "TABLES", "DESCRIBE", "GROUP", "BY",
        "COUNT", "SUM", "AVG", "ORDER", "LIMIT"
    );

    public static void main(String args[]) throws IOException {
        DBServer server = new DBServer();
        server.blockingListenOn(8888);
    }

    /**
    * KEEP this signature otherwise we won't be able to mark your submission correctly.
    */
    public DBServer() {
        storageFolderPath = Paths.get("databases").toAbsolutePath().toString();
        try {
            // Create the database storage folder if it doesn't already exist !
            Files.createDirectories(Paths.get(storageFolderPath));
        } catch(IOException ioe) {
            System.out.println("Can't seem to create database storage folder " + storageFolderPath);
        }

    }

    /**
    * KEEP this signature (i.e. {@code edu.uob.DBServer.handleCommand(String)}) otherwise we won't be
    * able to mark your submission correctly.
    *
    * <p>This method handles all incoming DB commands and carries out the required actions.
    */
    public String handleCommand(String command) {
        try {
            List<String> tokens = tokenize(command);
            if (tokens.isEmpty()) {
                return error("Empty command");
            }
            if (!";".equals(tokens.get(tokens.size() - 1))) {
                return error("Missing semicolon");
            }
            tokens = new ArrayList<>(tokens.subList(0, tokens.size() - 1));
            if (tokens.isEmpty()) {
                return error("Empty command");
            }
            String op = tokens.get(0).toUpperCase(Locale.ROOT);
            return switch (op) {
                case "USE" -> useDatabase(tokens);
                case "CREATE" -> create(tokens);
                case "DROP" -> drop(tokens);
                case "ALTER" -> alter(tokens);
                case "INSERT" -> insert(tokens);
                case "SELECT" -> select(tokens);
                case "UPDATE" -> update(tokens);
                case "DELETE" -> deleteRows(tokens);
                case "JOIN" -> join(tokens);
                case "SHOW" -> show(tokens);
                case "DESCRIBE" -> describe(tokens);
                default -> error("Unknown command");
            };
        } catch (DBException e) {
            return error(e.getMessage());
        } catch (Exception e) {
            return error("Internal server error");
        }
    }

    private String useDatabase(List<String> tokens) throws DBException {
        if (tokens.size() != 2) {
            throw new DBException("Malformed USE");
        }
        String db = lower(tokens.get(1));
        File folder = new File(storageFolderPath + File.separator + db);
        if (!folder.exists() || !folder.isDirectory()) {
            throw new DBException("Unknown database");
        }
        currentDatabaseName = db;
        return ok("");
    }

    private String create(List<String> tokens) throws DBException, IOException {
        if (tokens.size() < 3) {
            throw new DBException("Malformed CREATE");
        }
        String kind = tokens.get(1).toUpperCase(Locale.ROOT);
        if ("DATABASE".equals(kind)) {
            if (tokens.size() != 3) {
                throw new DBException("Malformed CREATE DATABASE");
            }
            String dbName = tokens.get(2);
            validateName(dbName, true);
            File folder = new File(storageFolderPath + File.separator + lower(dbName));
            if (folder.exists()) {
                throw new DBException("Database already exists");
            }
            if (!folder.mkdir()) {
                throw new DBException("Failed to create database");
            }
            return ok("");
        }
        if ("TABLE".equals(kind)) {
            ensureUsingDatabase();
            if (tokens.size() < 4 || !"(".equals(tokens.get(3)) || !")".equals(tokens.get(tokens.size() - 1))) {
                throw new DBException("Malformed CREATE TABLE");
            }
            String tableName = tokens.get(2);
            validateName(tableName, true);
            List<String> attributes = parseNameList(tokens.subList(4, tokens.size() - 1));
            validateAttributes(attributes);
            TableData table = new TableData();
            table.headers.add("id");
            table.headers.addAll(attributes);
            saveTable(tableName, table);
            return ok("");
        }
        throw new DBException("Malformed CREATE");
    }

    private String drop(List<String> tokens) throws DBException {
        if (tokens.size() != 3) {
            throw new DBException("Malformed DROP");
        }
        String kind = tokens.get(1).toUpperCase(Locale.ROOT);
        String name = tokens.get(2);
        if ("DATABASE".equals(kind)) {
            validateName(name, true);
            File db = new File(storageFolderPath + File.separator + lower(name));
            if (!db.exists()) {
                throw new DBException("Unknown database");
            }
            deleteRecursive(db);
            if (lower(name).equals(currentDatabaseName)) {
                currentDatabaseName = null;
            }
            return ok("");
        }
        if ("TABLE".equals(kind)) {
            ensureUsingDatabase();
            validateName(name, true);
            File tablePath = tableFile(name);
            if (!tablePath.exists()) {
                throw new DBException("Unknown table");
            }
            if (!tablePath.delete()) {
                throw new DBException("Failed to drop table");
            }
            return ok("");
        }
        throw new DBException("Malformed DROP");
    }

    private String alter(List<String> tokens) throws DBException, IOException {
        ensureUsingDatabase();
        if (tokens.size() != 5 || !"TABLE".equalsIgnoreCase(tokens.get(1))) {
            throw new DBException("Malformed ALTER");
        }
        String tableName = tokens.get(2);
        String action = tokens.get(3).toUpperCase(Locale.ROOT);
        String attribute = tokens.get(4);
        validateName(tableName, true);
        validateName(attribute, false);
        TableData table = loadTable(tableName);
        int existing = findColumnIndex(table.headers, attribute);
        if ("ADD".equals(action)) {
            if (existing >= 0) {
                throw new DBException("Duplicate attribute");
            }
            table.headers.add(attribute);
            for (List<String> row : table.rows) {
                row.add("NULL");
            }
            saveTableReplacing(tableName, table);
            return ok("");
        }
        if ("DROP".equals(action)) {
            if ("id".equalsIgnoreCase(attribute)) {
                throw new DBException("Cannot drop id");
            }
            if (existing < 0) {
                throw new DBException("Unknown attribute");
            }
            table.headers.remove(existing);
            for (List<String> row : table.rows) {
                row.remove(existing);
            }
            saveTableReplacing(tableName, table);
            return ok("");
        }
        throw new DBException("Malformed ALTER");
    }

    private String show(List<String> tokens) throws DBException {
        ensureUsingDatabase();
        if (tokens.size() != 2 || !"TABLES".equalsIgnoreCase(tokens.get(1))) {
            throw new DBException("Malformed SHOW");
        }
        File folder = new File(storageFolderPath + File.separator + currentDatabaseName);
        File[] tableFiles = folder.listFiles((dir, name) -> name.endsWith(".tab"));
        List<String> tableNames = new ArrayList<>();
        if (tableFiles != null) {
            for (File tableFile : tableFiles) {
                String name = tableFile.getName();
                tableNames.add(name.substring(0, name.length() - 4));
            }
        }
        Collections.sort(tableNames);
        List<String> headers = List.of("table");
        List<List<String>> rows = new ArrayList<>();
        for (String table : tableNames) {
            rows.add(List.of(table));
        }
        return tableResult(headers, rows);
    }

    private String describe(List<String> tokens) throws DBException, IOException {
        ensureUsingDatabase();
        if (tokens.size() != 2) {
            throw new DBException("Malformed DESCRIBE");
        }
        TableData table = loadTable(tokens.get(1));
        List<String> headers = List.of("column");
        List<List<String>> rows = new ArrayList<>();
        for (String column : table.headers) {
            rows.add(List.of(column));
        }
        return tableResult(headers, rows);
    }

    private String insert(List<String> tokens) throws DBException, IOException {
        ensureUsingDatabase();
        if (tokens.size() < 6 || !"INTO".equalsIgnoreCase(tokens.get(1)) || !"VALUES".equalsIgnoreCase(tokens.get(3))
            || !"(".equals(tokens.get(4)) || !")".equals(tokens.get(tokens.size() - 1))) {
            throw new DBException("Malformed INSERT");
        }
        String tableName = tokens.get(2);
        TableData table = loadTable(tableName);
        List<String> values = parseValueList(tokens.subList(5, tokens.size() - 1));
        if (values.size() != table.headers.size() - 1) {
            throw new DBException("Wrong value count");
        }
        List<String> row = new ArrayList<>();
        row.add(String.valueOf(nextId(table)));
        row.addAll(values);
        table.rows.add(row);
        saveTableReplacing(tableName, table);
        return ok("");
    }

    private String select(List<String> tokens) throws DBException, IOException {
        int fromIndex = indexOfIgnoreCase(tokens, "FROM");
        if (fromIndex < 0 || fromIndex < 2) {
            throw new DBException("Malformed SELECT");
        }
        List<SelectField> selected = parseSelection(tokens.subList(1, fromIndex));
        int whereIndex = indexOfIgnoreCase(tokens, "WHERE");
        int groupIndex = indexOfIgnoreCase(tokens, "GROUP");
        int byIndex = indexOfIgnoreCase(tokens, "BY");
        if ((groupIndex >= 0 && byIndex < 0) || (groupIndex < 0 && byIndex >= 0)) {
            throw new DBException("Malformed SELECT");
        }
        if (groupIndex >= 0 && byIndex != groupIndex + 1) {
            throw new DBException("Malformed SELECT");
        }
        if (whereIndex >= 0 && whereIndex < fromIndex + 2) {
            throw new DBException("Malformed SELECT");
        }
        if (whereIndex >= 0 && groupIndex >= 0 && whereIndex > groupIndex) {
            throw new DBException("Malformed SELECT");
        }
        if (whereIndex >= 0 && whereIndex != fromIndex + 2) {
            throw new DBException("Malformed SELECT");
        }
        int endAfterTable = tokens.size();
        if (whereIndex >= 0) {
            endAfterTable = whereIndex;
        } else if (groupIndex >= 0) {
            endAfterTable = groupIndex;
        }
        if (fromIndex + 2 != endAfterTable) {
            throw new DBException("Malformed SELECT");
        }
        String tableName = tokens.get(fromIndex + 1);
        TableData table = loadTable(tableName);
        Condition condition = Condition.alwaysTrue();
        int conditionEnd = tokens.size();
        if (groupIndex >= 0) {
            conditionEnd = groupIndex;
        }
        if (whereIndex >= 0) {
            condition = parseCondition(tokens.subList(whereIndex + 1, conditionEnd), table.headers);
        }
        String groupBy = null;
        if (groupIndex >= 0) {
            if (byIndex + 2 != tokens.size()) {
                throw new DBException("Malformed SELECT");
            }
            groupBy = tokens.get(byIndex + 1);
            requireColumn(table.headers, groupBy);
        }
        return runSelect(table, selected, condition, groupBy);
    }

    private String runSelect(TableData table, List<SelectField> selected, Condition condition, String groupBy) throws DBException {
        List<List<String>> filtered = new ArrayList<>();
        for (List<String> row : table.rows) {
            if (condition.eval(table.headers, row)) {
                filtered.add(row);
            }
        }

        boolean hasAggregate = selected.stream().anyMatch(SelectField::isAggregate);
        if (groupBy == null) {
            if (!hasAggregate) {
                List<String> outHeaders = resolveSelectHeaders(selected, table.headers);
                List<List<String>> outRows = new ArrayList<>();
                for (List<String> row : filtered) {
                    outRows.add(project(row, table.headers, outHeaders));
                }
                return tableResult(outHeaders, outRows);
            }
            List<String> outHeaders = renderFieldHeaders(selected);
            List<List<String>> outRows = new ArrayList<>();
            outRows.add(evaluateFields(selected, table.headers, filtered));
            return tableResult(outHeaders, outRows);
        }

        int groupIndex = requireColumn(table.headers, groupBy);
        if (!hasAggregate) {
            throw new DBException("GROUP BY requires aggregate");
        }
        Map<String, List<List<String>>> groups = new LinkedHashMap<>();
        for (List<String> row : filtered) {
            groups.computeIfAbsent(row.get(groupIndex), key -> new ArrayList<>()).add(row);
        }
        List<String> outHeaders = renderFieldHeaders(selected);
        List<List<String>> outRows = new ArrayList<>();
        for (List<List<String>> groupRows : groups.values()) {
            outRows.add(evaluateFields(selected, table.headers, groupRows));
        }
        return tableResult(outHeaders, outRows);
    }

    private String update(List<String> tokens) throws DBException, IOException {
        ensureUsingDatabase();
        int setIndex = indexOfIgnoreCase(tokens, "SET");
        int whereIndex = indexOfIgnoreCase(tokens, "WHERE");
        if (tokens.size() < 6 || setIndex != 2 || whereIndex < 4) {
            throw new DBException("Malformed UPDATE");
        }
        String tableName = tokens.get(1);
        TableData table = loadTable(tableName);
        Map<Integer, String> assignments = parseAssignments(tokens.subList(setIndex + 1, whereIndex), table.headers);
        Condition condition = parseCondition(tokens.subList(whereIndex + 1, tokens.size()), table.headers);
        for (Integer idx : assignments.keySet()) {
            if (idx == 0) {
                throw new DBException("Cannot update id");
            }
        }
        for (List<String> row : table.rows) {
            if (condition.eval(table.headers, row)) {
                for (Map.Entry<Integer, String> entry : assignments.entrySet()) {
                    row.set(entry.getKey(), entry.getValue());
                }
            }
        }
        saveTableReplacing(tableName, table);
        return ok("");
    }

    private String deleteRows(List<String> tokens) throws DBException, IOException {
        ensureUsingDatabase();
        if (tokens.size() < 5 || !"FROM".equalsIgnoreCase(tokens.get(1))) {
            throw new DBException("Malformed DELETE");
        }
        int whereIndex = indexOfIgnoreCase(tokens, "WHERE");
        if (whereIndex < 3) {
            throw new DBException("Malformed DELETE");
        }
        String tableName = tokens.get(2);
        TableData table = loadTable(tableName);
        Condition condition = parseCondition(tokens.subList(whereIndex + 1, tokens.size()), table.headers);
        List<List<String>> kept = new ArrayList<>();
        for (List<String> row : table.rows) {
            if (!condition.eval(table.headers, row)) {
                kept.add(row);
            }
        }
        table.rows = kept;
        saveTableReplacing(tableName, table);
        return ok("");
    }

    private String join(List<String> tokens) throws DBException, IOException {
        ensureUsingDatabase();
        if (tokens.size() != 8 || !"AND".equalsIgnoreCase(tokens.get(2)) || !"ON".equalsIgnoreCase(tokens.get(4))
            || !"AND".equalsIgnoreCase(tokens.get(6))) {
            throw new DBException("Malformed JOIN");
        }
        String table1 = tokens.get(1);
        String table2 = tokens.get(3);
        String attr1 = tokens.get(5);
        String attr2 = tokens.get(7);
        TableData t1 = loadTable(table1);
        TableData t2 = loadTable(table2);
        int idx1 = requireColumn(t1.headers, attr1);
        int idx2 = requireColumn(t2.headers, attr2);

        List<String> outHeaders = new ArrayList<>();
        outHeaders.add("id");
        for (int i = 1; i < t1.headers.size(); i++) {
            outHeaders.add(table1 + "." + t1.headers.get(i));
        }
        for (int i = 1; i < t2.headers.size(); i++) {
            outHeaders.add(table2 + "." + t2.headers.get(i));
        }

        List<List<String>> outRows = new ArrayList<>();
        int joinedId = 1;
        for (List<String> row1 : t1.rows) {
            for (List<String> row2 : t2.rows) {
                if (row1.get(idx1).equals(row2.get(idx2))) {
                    List<String> out = new ArrayList<>();
                    out.add(String.valueOf(joinedId++));
                    for (int i = 1; i < row1.size(); i++) {
                        out.add(row1.get(i));
                    }
                    for (int i = 1; i < row2.size(); i++) {
                        out.add(row2.get(i));
                    }
                    outRows.add(out);
                }
            }
        }
        return tableResult(outHeaders, outRows);
    }

    private List<SelectField> parseSelection(List<String> tokens) throws DBException {
        if (tokens.size() == 1 && "*".equals(tokens.get(0))) {
            return Collections.singletonList(SelectField.wildcard());
        }
        List<SelectField> fields = new ArrayList<>();
        if (tokens.isEmpty()) {
            throw new DBException("Malformed SELECT");
        }
        int i = 0;
        while (i < tokens.size()) {
            if (",".equals(tokens.get(i))) {
                throw new DBException("Malformed SELECT");
            }
            SelectField field;
            if (i + 3 < tokens.size()
                && tokens.get(i + 1).equals("(")
                && tokens.get(i + 3).equals(")")
                && isAggregateFunction(tokens.get(i))) {
                String fn = tokens.get(i).toUpperCase(Locale.ROOT);
                String target = tokens.get(i + 2);
                if (!"COUNT".equals(fn) && "*".equals(target)) {
                    throw new DBException("Malformed SELECT");
                }
                if ("COUNT".equals(fn) && !"*".equals(target) && !target.matches("[A-Za-z][A-Za-z0-9]*")) {
                    throw new DBException("Malformed SELECT");
                }
                if (!"COUNT".equals(fn) && !target.matches("[A-Za-z][A-Za-z0-9]*")) {
                    throw new DBException("Malformed SELECT");
                }
                field = SelectField.aggregate(fn, target);
                i += 4;
            } else {
                String token = tokens.get(i);
                if (!token.matches("[A-Za-z][A-Za-z0-9]*")) {
                    throw new DBException("Malformed SELECT");
                }
                field = SelectField.column(token);
                i++;
            }
            fields.add(field);
            if (i < tokens.size()) {
                if (!",".equals(tokens.get(i))) {
                    throw new DBException("Malformed SELECT");
                }
                i++;
            }
        }
        return fields;
    }

    private List<String> resolveSelectHeaders(List<SelectField> selected, List<String> allHeaders) throws DBException {
        if (selected.size() == 1 && selected.get(0).wildcard) {
            return new ArrayList<>(allHeaders);
        }
        List<String> resolved = new ArrayList<>();
        for (SelectField field : selected) {
            if (field.wildcard || field.aggregate) {
                throw new DBException("Malformed SELECT");
            }
            int idx = requireColumn(allHeaders, field.value);
            resolved.add(allHeaders.get(idx));
        }
        return resolved;
    }

    private List<String> renderFieldHeaders(List<SelectField> selected) {
        List<String> headers = new ArrayList<>();
        for (SelectField field : selected) {
            if (field.aggregate) {
                headers.add(field.function + "(" + field.value + ")");
            } else if (field.wildcard) {
                headers.add("*");
            } else {
                headers.add(field.value);
            }
        }
        return headers;
    }

    private List<String> evaluateFields(List<SelectField> fields, List<String> headers, List<List<String>> rows) throws DBException {
        List<String> result = new ArrayList<>();
        for (SelectField field : fields) {
            if (field.wildcard) {
                throw new DBException("Malformed SELECT");
            }
            if (!field.aggregate) {
                int idx = requireColumn(headers, field.value);
                if (rows.isEmpty()) {
                    result.add("NULL");
                } else {
                    result.add(rows.get(0).get(idx));
                }
                continue;
            }
            result.add(evaluateAggregate(field, headers, rows));
        }
        return result;
    }

    private String evaluateAggregate(SelectField field, List<String> headers, List<List<String>> rows) throws DBException {
        String fn = field.function;
        if ("COUNT".equals(fn) && "*".equals(field.value)) {
            return String.valueOf(rows.size());
        }
        int idx = requireColumn(headers, field.value);
        if ("COUNT".equals(fn)) {
            return String.valueOf(rows.size());
        }
        if (rows.isEmpty()) {
            return "NULL";
        }
        double sum = 0.0;
        for (List<String> row : rows) {
            Double value = parseNumber(row.get(idx));
            if (value == null) {
                throw new DBException("SUM/AVG requires numeric column");
            }
            sum += value;
        }
        if ("SUM".equals(fn)) {
            return formatNumber(sum);
        }
        if ("AVG".equals(fn)) {
            return formatNumber(sum / rows.size());
        }
        throw new DBException("Unsupported aggregate");
    }

    private boolean isAggregateFunction(String token) {
        String upper = token.toUpperCase(Locale.ROOT);
        return "COUNT".equals(upper) || "SUM".equals(upper) || "AVG".equals(upper);
    }

    private Map<Integer, String> parseAssignments(List<String> tokens, List<String> headers) throws DBException {
        Map<Integer, String> map = new LinkedHashMap<>();
        if (tokens.isEmpty()) {
            throw new DBException("Malformed UPDATE");
        }
        int i = 0;
        while (i < tokens.size()) {
            if (i + 2 >= tokens.size() || !"=".equals(tokens.get(i + 1))) {
                throw new DBException("Malformed UPDATE");
            }
            String name = tokens.get(i);
            String value = normalizeValue(tokens.get(i + 2));
            int idx = requireColumn(headers, name);
            map.put(idx, value);
            i += 3;
            if (i < tokens.size()) {
                if (!",".equals(tokens.get(i))) {
                    throw new DBException("Malformed UPDATE");
                }
                i++;
            }
        }
        return map;
    }

    private Condition parseCondition(List<String> tokens, List<String> headers) throws DBException {
        if (tokens.isEmpty()) {
            throw new DBException("Malformed condition");
        }
        ConditionParser parser = new ConditionParser(tokens, headers);
        Condition condition = parser.parseExpression();
        if (!parser.atEnd()) {
            throw new DBException("Malformed condition");
        }
        return condition;
    }

    private List<String> parseNameList(List<String> tokens) throws DBException {
        List<String> names = new ArrayList<>();
        if (tokens.isEmpty()) {
            throw new DBException("Malformed name list");
        }
        boolean expectName = true;
        for (String token : tokens) {
            if (expectName) {
                if (",".equals(token)) {
                    throw new DBException("Malformed name list");
                }
                validateName(token, false);
                names.add(token);
            } else if (!",".equals(token)) {
                throw new DBException("Malformed name list");
            }
            expectName = !expectName;
        }
        if (expectName) {
            throw new DBException("Malformed name list");
        }
        return names;
    }

    private List<String> parseValueList(List<String> tokens) throws DBException {
        List<String> values = new ArrayList<>();
        if (tokens.isEmpty()) {
            throw new DBException("Malformed VALUES");
        }
        boolean expectValue = true;
        for (String token : tokens) {
            if (expectValue) {
                if (",".equals(token)) {
                    throw new DBException("Malformed VALUES");
                }
                values.add(normalizeValue(token));
            } else if (!",".equals(token)) {
                throw new DBException("Malformed VALUES");
            }
            expectValue = !expectValue;
        }
        if (expectNameTail(expectValue)) {
            throw new DBException("Malformed VALUES");
        }
        return values;
    }

    private boolean expectNameTail(boolean expectValue) {
        return expectValue;
    }

    private void validateAttributes(List<String> attributes) throws DBException {
        Set<String> seen = new HashSet<>();
        for (String attr : attributes) {
            validateName(attr, false);
            String key = lower(attr);
            if ("id".equals(key)) {
                throw new DBException("id is reserved");
            }
            if (!seen.add(key)) {
                throw new DBException("Duplicate attribute");
            }
        }
    }

    private void validateName(String token, boolean allowLowerStorage) throws DBException {
        if (!token.matches("[A-Za-z][A-Za-z0-9]*")) {
            throw new DBException("Illegal name");
        }
        String uc = token.toUpperCase(Locale.ROOT);
        if (RESERVED.contains(uc)) {
            throw new DBException("Reserved keyword");
        }
        if (!allowLowerStorage && "id".equalsIgnoreCase(token)) {
            throw new DBException("id is reserved");
        }
    }

    private String normalizeValue(String token) {
        if (token.startsWith("'") && token.endsWith("'") && token.length() >= 2) {
            return token.substring(1, token.length() - 1);
        }
        if ("TRUE".equalsIgnoreCase(token)) {
            return "TRUE";
        }
        if ("FALSE".equalsIgnoreCase(token)) {
            return "FALSE";
        }
        if ("NULL".equalsIgnoreCase(token)) {
            return "NULL";
        }
        return token;
    }

    private int requireColumn(List<String> headers, String name) throws DBException {
        int idx = findColumnIndex(headers, name);
        if (idx < 0) {
            throw new DBException("Unknown attribute");
        }
        return idx;
    }

    private int findColumnIndex(List<String> headers, String name) {
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i).equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    private List<String> project(List<String> row, List<String> allHeaders, List<String> wantedHeaders) {
        List<String> projected = new ArrayList<>();
        for (String header : wantedHeaders) {
            for (int i = 0; i < allHeaders.size(); i++) {
                if (allHeaders.get(i).equalsIgnoreCase(header)) {
                    projected.add(row.get(i));
                    break;
                }
            }
        }
        return projected;
    }

    private int nextId(TableData table) {
        int max = 0;
        for (List<String> row : table.rows) {
            try {
                max = Math.max(max, Integer.parseInt(row.get(0)));
            } catch (NumberFormatException ignored) {
                // ignore bad rows and continue
            }
        }
        return max + 1;
    }

    private TableData loadTable(String tableName) throws DBException, IOException {
        ensureUsingDatabase();
        validateName(tableName, true);
        File file = tableFile(tableName);
        if (!file.exists()) {
            throw new DBException("Unknown table");
        }
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            throw new DBException("Corrupt table");
        }
        TableData t = new TableData();
        t.headers = parseTabLine(lines.get(0));
        if (t.headers.isEmpty() || !"id".equalsIgnoreCase(t.headers.get(0))) {
            throw new DBException("Corrupt table");
        }
        for (int i = 1; i < lines.size(); i++) {
            List<String> row = parseTabLine(lines.get(i));
            if (row.size() != t.headers.size()) {
                throw new DBException("Corrupt table");
            }
            t.rows.add(row);
        }
        return t;
    }

    private void saveTable(String tableName, TableData table) throws DBException, IOException {
        ensureUsingDatabase();
        validateName(tableName, true);
        File file = tableFile(tableName);
        if (file.exists()) {
            throw new DBException("Table already exists");
        }
        writeTable(file, table);
    }

    private void saveTableReplacing(String tableName, TableData table) throws IOException, DBException {
        File file = tableFile(tableName);
        writeTable(file, table);
    }

    private void writeTable(File file, TableData table) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join("\t", table.headers)).append("\n");
        for (List<String> row : table.rows) {
            sb.append(String.join("\t", row)).append("\n");
        }
        Files.writeString(file.toPath(), sb.toString(), StandardCharsets.UTF_8);
    }

    private File tableFile(String tableName) {
        return new File(storageFolderPath + File.separator + currentDatabaseName + File.separator + lower(tableName) + ".tab");
    }

    private void ensureUsingDatabase() throws DBException {
        if (currentDatabaseName == null) {
            throw new DBException("No database selected");
        }
        File folder = new File(storageFolderPath + File.separator + currentDatabaseName);
        if (!folder.exists()) {
            throw new DBException("Unknown database");
        }
    }

    private String tableResult(List<String> headers, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("[OK]").append("\n");
        sb.append(String.join("\t", headers));
        for (List<String> row : rows) {
            sb.append("\n").append(String.join("\t", row));
        }
        return sb.toString();
    }

    private String ok(String body) {
        if (body.isEmpty()) {
            return "[OK]";
        }
        return "[OK]\n" + body;
    }

    private String error(String message) {
        return "[ERROR] " + message;
    }

    private void deleteRecursive(File file) throws DBException {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursive(child);
                }
            }
        }
        if (!file.delete()) {
            throw new DBException("Failed to delete resource");
        }
    }

    private List<String> parseTabLine(String line) {
        return new ArrayList<>(Arrays.asList(line.split("\t", -1)));
    }

    private List<String> tokenize(String input) throws DBException {
        if (input == null) {
            return Collections.emptyList();
        }
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\'') {
                if (inString) {
                    current.append(c);
                    tokens.add(current.toString());
                    current.setLength(0);
                    inString = false;
                } else {
                    flushToken(tokens, current);
                    current.append(c);
                    inString = true;
                }
                continue;
            }
            if (inString) {
                current.append(c);
                continue;
            }
            if (Character.isWhitespace(c)) {
                flushToken(tokens, current);
                continue;
            }
            String two = (i + 1 < input.length()) ? ("" + c + input.charAt(i + 1)) : "";
            if ("==".equals(two) || "!=".equals(two) || ">=".equals(two) || "<=".equals(two)) {
                flushToken(tokens, current);
                tokens.add(two);
                i++;
                continue;
            }
            if (c == '>' || c == '<' || c == '=' || c == '(' || c == ')' || c == ',' || c == ';' || c == '*') {
                flushToken(tokens, current);
                tokens.add(String.valueOf(c));
                continue;
            }
            current.append(c);
        }
        if (inString) {
            throw new DBException("Unterminated string literal");
        }
        flushToken(tokens, current);
        tokens.removeIf(String::isEmpty);
        return tokens;
    }

    private void flushToken(List<String> tokens, StringBuilder current) {
        if (!current.isEmpty()) {
            tokens.add(current.toString());
            current.setLength(0);
        }
    }

    private int indexOfIgnoreCase(List<String> tokens, String target) {
        for (int i = 0; i < tokens.size(); i++) {
            if (target.equalsIgnoreCase(tokens.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private String lower(String s) {
        return s.toLowerCase(Locale.ROOT);
    }

    private final class ConditionParser {
        private final List<String> tokens;
        private final List<String> headers;
        private int pos;

        private ConditionParser(List<String> tokens, List<String> headers) {
            this.tokens = tokens;
            this.headers = headers;
            this.pos = 0;
        }

        private boolean atEnd() {
            return pos >= tokens.size();
        }

        private Condition parseExpression() throws DBException {
            Condition left = parseTerm();
            while (!atEnd() && "OR".equalsIgnoreCase(tokens.get(pos))) {
                pos++;
                Condition right = parseTerm();
                Condition leftSnapshot = left;
                Condition rightSnapshot = right;
                left = (h, r) -> leftSnapshot.eval(h, r) || rightSnapshot.eval(h, r);
            }
            return left;
        }

        private Condition parseTerm() throws DBException {
            Condition left = parseFactor();
            while (!atEnd() && "AND".equalsIgnoreCase(tokens.get(pos))) {
                pos++;
                Condition right = parseFactor();
                Condition leftSnapshot = left;
                Condition rightSnapshot = right;
                left = (h, r) -> leftSnapshot.eval(h, r) && rightSnapshot.eval(h, r);
            }
            return left;
        }

        private Condition parseFactor() throws DBException {
            if (!atEnd() && "(".equals(tokens.get(pos))) {
                pos++;
                Condition inside = parseExpression();
                if (atEnd() || !")".equals(tokens.get(pos))) {
                    throw new DBException("Malformed condition");
                }
                pos++;
                return inside;
            }
            return parsePredicate();
        }

        private Condition parsePredicate() throws DBException {
            if (pos + 2 >= tokens.size()) {
                throw new DBException("Malformed condition");
            }
            String attribute = tokens.get(pos++);
            String comparator = tokens.get(pos++);
            String valueToken = tokens.get(pos++);
            int col = requireColumn(headers, attribute);
            String target = normalizeValue(valueToken);
            if (!Set.of("==", "!=", ">", ">=", "<", "<=", "LIKE").contains(comparator.toUpperCase(Locale.ROOT))) {
                throw new DBException("Invalid comparator");
            }
            return (h, row) -> compare(row.get(col), comparator, target);
        }
    }

    private boolean compare(String left, String comparator, String right) {
        String cmp = comparator.toUpperCase(Locale.ROOT);
        if ("LIKE".equals(cmp)) {
            return left.contains(right);
        }
        if ("==".equals(cmp)) {
            return equalsValue(left, right);
        }
        if ("!=".equals(cmp)) {
            return !equalsValue(left, right);
        }
        Double a = parseNumber(left);
        Double b = parseNumber(right);
        if (a == null || b == null) {
            if (isBoolean(left) && isBoolean(right)) {
                int boolCmp = Boolean.compare(Boolean.parseBoolean(left.toLowerCase(Locale.ROOT)),
                    Boolean.parseBoolean(right.toLowerCase(Locale.ROOT)));
                return evaluateCompare(boolCmp, cmp);
            }
            return false;
        }
        int numberCmp = Double.compare(a, b);
        return evaluateCompare(numberCmp, cmp);
    }

    private boolean evaluateCompare(int cmpResult, String comparator) {
        return switch (comparator) {
            case ">" -> cmpResult > 0;
            case ">=" -> cmpResult >= 0;
            case "<" -> cmpResult < 0;
            case "<=" -> cmpResult <= 0;
            default -> false;
        };
    }

    private boolean equalsValue(String left, String right) {
        if (isBoolean(left) && isBoolean(right)) {
            return left.equalsIgnoreCase(right);
        }
        Double a = parseNumber(left);
        Double b = parseNumber(right);
        if (a != null && b != null) {
            return Double.compare(a, b) == 0;
        }
        return left.equals(right);
    }

    private Double parseNumber(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatNumber(double value) {
        if (Math.rint(value) == value) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private boolean isBoolean(String s) {
        return "TRUE".equalsIgnoreCase(s) || "FALSE".equalsIgnoreCase(s);
    }

    @FunctionalInterface
    private interface Condition {
        boolean eval(List<String> headers, List<String> row);

        static Condition alwaysTrue() {
            return (h, r) -> true;
        }
    }

    private static final class TableData {
        private List<String> headers = new ArrayList<>();
        private List<List<String>> rows = new ArrayList<>();
    }

    private static final class SelectField {
        private final boolean wildcard;
        private final boolean aggregate;
        private final String function;
        private final String value;

        private SelectField(boolean wildcard, boolean aggregate, String function, String value) {
            this.wildcard = wildcard;
            this.aggregate = aggregate;
            this.function = function;
            this.value = value;
        }

        private static SelectField wildcard() {
            return new SelectField(true, false, "", "*");
        }

        private static SelectField column(String column) {
            return new SelectField(false, false, "", column);
        }

        private static SelectField aggregate(String fn, String target) {
            return new SelectField(false, true, fn, target);
        }

        private boolean isAggregate() {
            return aggregate;
        }
    }

    private static final class DBException extends Exception {
        private DBException(String message) {
            super(message);
        }
    }






    //  === Methods below handle networking aspects of the project - you will not need to change these ! ===

    public void blockingListenOn(int portNumber) throws IOException {
        try (ServerSocket s = new ServerSocket(portNumber)) {
            System.out.println("Server listening on port " + portNumber);
            while (!Thread.interrupted()) {
                try {
                    blockingHandleConnection(s);
                } catch (IOException e) {
                    System.err.println("Server encountered a non-fatal IO error:");
                    e.printStackTrace();
                    System.err.println("Continuing...");
                }
            }
        }
    }

    private void blockingHandleConnection(ServerSocket serverSocket) throws IOException {
        try (Socket s = serverSocket.accept();
        BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()))) {

            System.out.println("Connection established: " + serverSocket.getInetAddress());
            while (!Thread.interrupted()) {
                String incomingCommand = reader.readLine();
                System.out.println("Received message: " + incomingCommand);
                String result = handleCommand(incomingCommand);
                writer.write(result);
                writer.write("\n" + END_OF_TRANSMISSION + "\n");
                writer.flush();
            }
        }
    }
}
