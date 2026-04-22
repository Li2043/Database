# Mini Relational Database Server (Java)

A lightweight SQL-like database server built from scratch in Java 17.  
The server parses client queries, executes relational operations in memory, and persists data to local `.tab` files.

This project is designed to demonstrate backend engineering skills that are also highly relevant to data workflows:
query parsing, data transformation, filtering/join logic, data persistence, and robust error handling.

## Why this project matters (for Data / Analytics roles)

- Build a query engine end-to-end: tokenization, parsing, execution, and response formatting.
- Implement core data operations analysts use every day (`SELECT`, `WHERE`, `JOIN`, `UPDATE`, `DELETE`).
- Handle mixed data types and edge cases without crashing.
- Keep data persistent and reproducible through filesystem storage.
- Practice test-driven debugging against a comprehensive automated test suite.

## Key Features

- SQL-like command support:
  - `USE`, `CREATE`, `DROP`, `ALTER`
  - `INSERT`, `SELECT`, `UPDATE`, `DELETE`
  - `JOIN` (inner join)
  - extra usability commands: `SHOW TABLES`, `DESCRIBE <table>`
- Analytics-oriented querying:
  - aggregate functions: `COUNT`, `SUM`, `AVG`
  - grouped summaries via `GROUP BY`
- Condition engine:
  - comparators: `==`, `!=`, `>`, `>=`, `<`, `<=`, `LIKE`
  - compound conditions with `AND` / `OR` and nested parentheses
- Robustness and safety:
  - strict `[OK]` / `[ERROR]` protocol
  - malformed query detection (missing semicolon, invalid comparator, bad syntax, etc.)
  - prevents illegal schema actions (e.g., dropping/updating `id`)
- Persistence:
  - each table stored as a tab-separated `.tab` file
  - database/table name case-insensitive handling
  - monotonically increasing IDs (no ID recycling after deletes)

## Tech Stack

- **Language:** Java 17
- **Build:** Maven Wrapper (`mvnw`)
- **Testing:** JUnit 5
- **Storage:** local filesystem (`databases/` directory, `.tab` files)
- **Networking:** Java sockets (`DBServer` + `DBClient`)

## Project Structure

- `src/main/java/edu/uob/DBServer.java` – core parser + execution engine
- `src/main/java/edu/uob/DBClient.java` – simple interactive client
- `src/test/java` – runnable unit/integration tests
- `DB-tests` – coursework-style test cases for validation
- `demo.sql` – interview-friendly demo script
- `docs/demo-run.md` – demo transcript artifact
- `.github/workflows/ci.yml` – automated test workflow on GitHub Actions
- `databases/` – runtime data folder (ignored by git except `.gitkeep`)

## Getting Started

### 1) Prerequisites

- JDK 17
- macOS/Linux/Windows

Check Java:

```bash
java -version
javac -version
```

### 2) Build

```bash
./mvnw clean compile
```

### 3) Run server

```bash
./mvnw exec:java@server
```

### 4) Run client (in another terminal)

```bash
./mvnw exec:java@client
```

Then input SQL-like commands in the client prompt.

## Example Queries

```sql
CREATE DATABASE demo;
USE demo;

CREATE TABLE actors (name, nationality, awards);
INSERT INTO actors VALUES ('Emma', 'British', 10);
INSERT INTO actors VALUES ('Toni', 'Australian', 12);

SELECT * FROM actors;
SELECT name FROM actors WHERE awards >= 10;
SELECT COUNT(*) FROM actors;
SELECT SUM(awards), AVG(awards) FROM actors;
SELECT nationality, COUNT(*) FROM actors GROUP BY nationality;

ALTER TABLE actors ADD age;
UPDATE actors SET age = 35 WHERE name == 'Emma';
SELECT * FROM actors WHERE age == 35;

SHOW TABLES;
DESCRIBE actors;
```

## Testing

Run included tests:

```bash
./mvnw test
```

`DB-tests/` contains additional coursework-style tests for behavior validation.

On GitHub, tests are also executed automatically via `.github/workflows/ci.yml` on every push / pull request.

## Demo For Recruiters

- Run all commands in `demo.sql` in the provided client.
- Use `docs/demo-run.md` as a quick transcript preview in your portfolio.
- Optional: add your own terminal screenshots/GIF to `docs/` for a richer project presentation.

## Engineering Highlights

- Custom tokenizer that preserves quoted string literals while handling flexible whitespace.
- Recursive-descent condition parser for bracketed logical expressions.
- Type-aware comparison strategy:
  - numeric comparisons when both sides are numbers
  - boolean comparisons for `TRUE/FALSE`
  - graceful empty-result behavior for incompatible types
- Join output formatting with fully-qualified attribute names (`table.column`) and regenerated join IDs.

## Improvements Completed for GitHub Readiness

- Added repository hygiene via root `.gitignore`:
  - ignores build artifacts, IDE files, and generated runtime databases.
- Cleaned generated local database snapshots from repository state.
- Added `SHOW TABLES` / `DESCRIBE` to improve discoverability for demo sessions.
- Corrected table write behavior to avoid accidental overwrite on `CREATE TABLE`.
- Added analytics-focused SQL support: `COUNT`, `SUM`, `AVG`, `GROUP BY`.
- Added demo artifacts (`demo.sql`, `docs/demo-run.md`) for interview walkthrough.
- Added GitHub Actions CI to run tests automatically with JDK 17.

## Future Improvements

If extending this toward a production-grade analytics engine:

- Add sorting and pagination (`ORDER BY`, `LIMIT`).
- Add CSV import/export utilities for analyst workflows.
- Introduce query execution planning and indexing for performance.
- Add Docker + expanded CI pipeline with test matrix (Java versions / OS).
- Expose REST API wrapper for BI integration.

## Author

Built by **Shenhua** as a database systems and data-query engine project.

If you are a recruiter or hiring manager, this repository demonstrates practical competency in:
backend data processing, query systems, data persistence, and test-driven engineering.
