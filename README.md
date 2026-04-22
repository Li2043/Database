# Mini Relational Database in Java

A lightweight SQL-like database server built from scratch in Java.

This project implements a small relational database engine that can parse client commands, execute core query operations in memory, and persist table data locally as `.tab` files. It is designed to demonstrate how a database works under the hood, including parsing, validation, storage, querying, and client-server communication.

---

## Overview

Modern databases are powerful, but most of their internal logic is hidden behind mature engines like MySQL or PostgreSQL. This project rebuilds a small, simplified version of that workflow from the ground up.

Instead of relying on an existing database system, this project handles:

- command tokenization and parsing
- schema creation and management
- data insertion, selection, update, and deletion
- filtering, joins, and grouped aggregation
- local file-based persistence
- request/response communication between a client and a server

This makes the project useful both as a learning exercise and as a backend portfolio project.

---

## Why build this database?

This project exists to answer a simple question:

**What actually happens inside a database when a query is executed?**

By building a database engine from scratch, this project helps explore:

- how SQL-like queries are parsed and validated
- how relational operations are executed step by step
- how table data can be stored without using an external database
- how a client and server communicate in a database-style workflow
- how error handling and input validation are implemented in backend systems

It is especially useful for students, Java learners, and anyone interested in database systems, compilers, or backend engineering.

---

## Key Features

### 1. SQL-like command support

The database supports a practical subset of SQL-style operations, including:

- `USE`
- `CREATE DATABASE`
- `CREATE TABLE`
- `DROP DATABASE`
- `DROP TABLE`
- `ALTER TABLE ADD`
- `ALTER TABLE DROP`
- `INSERT`
- `SELECT`
- `UPDATE`
- `DELETE`
- `JOIN`
- `SHOW TABLES`
- `DESCRIBE`

### 2. Query and filtering capabilities

This project supports core query functionality such as:

- `SELECT *` and column projection
- `WHERE` filtering
- comparison operators:
  - `==`
  - `!=`
  - `>`
  - `>=`
  - `<`
  - `<=`
  - `LIKE`
- compound conditions with:
  - `AND`
  - `OR`
  - parentheses for nested expressions
- aggregate functions:
  - `COUNT`
  - `SUM`
  - `AVG`
- grouped queries with `GROUP BY`

### 3. Local persistence

Data is not lost when commands finish running.

- each table is stored as a local tab-separated `.tab` file
- databases are stored inside a local `databases/` directory
- table rows use an auto-increment style `id`
- row IDs keep increasing and are not reused after deletion

### 4. Client-server architecture

This project is built as a small database server rather than a single standalone script.

- `DBServer` handles parsing, validation, execution, and storage
- `DBClient` provides an interactive terminal client
- communication happens over Java sockets

### 5. Robust validation and error handling

The database returns responses using a clear protocol:

- `[OK]` for successful commands
- `[ERROR]` for invalid or failed commands

It also validates many error cases, such as:

- missing semicolons
- malformed commands
- illegal or reserved names
- duplicate attributes
- unknown tables or columns
- attempts to modify protected fields such as `id`

---

## How it works

The workflow of the project is:

1. The client sends a SQL-like command to the server.
2. The server tokenizes the input and checks the syntax.
3. The command is dispatched to the correct execution handler.
4. The database reads from or writes to local `.tab` files.
5. The server sends a formatted result back to the client.

This design keeps the project small, readable, and easy to extend.

---

## Tech Stack

- **Language:** Java
- **Architecture:** Client-server
- **Networking:** Java sockets
- **Storage:** Local filesystem (`.tab` files)
- **Core techniques used:**
  - custom tokenizer for SQL-like input
  - command parsing and dispatch
  - condition parsing for nested logical expressions
  - in-memory query execution
  - file-based persistence

---
## Project Structure

```text
DBServer.java    # Core database server, parser, and execution engine
DBClient.java    # Interactive client for sending commands
Database.java    # Database-level structure
Table.java       # Table representation and persistence logic
Row.java         # Row representation
DBCmd.java       # Abstract command type
CreateCMD.java   # Legacy command class
UseCMD.java      # Legacy command class
SelectCMD.java   # Legacy command class
```

---

## Getting Started

### Requirements

- Java 17 or above recommended

### Compile the project

```bash
javac -d out *.java
```

### Run the server

```bash
java -cp out edu.uob.DBServer
```

The server listens on port `8888` and automatically creates a local `databases/` folder if it does not already exist.

### Run the client

Open a second terminal and run:

```bash
java -cp out edu.uob.DBClient
```

You can then type SQL-like commands directly into the client.

---

## Important Notes

- Every command must end with a semicolon `;`
- This is a **SQL-like** database, not a full SQL standard implementation
- `JOIN` uses a custom syntax:

```sql
JOIN table1 AND table2 ON column1 AND column2;
```

---

## Example Usage

```sql
CREATE DATABASE university;
USE university;

CREATE TABLE students (name, age, major);
INSERT INTO students VALUES ('Alice', 20, 'ComputerScience');
INSERT INTO students VALUES ('Bob', 22, 'Mathematics');
INSERT INTO students VALUES ('Carol', 21, 'ComputerScience');

SELECT * FROM students;
SELECT name FROM students WHERE age >= 21;
SELECT COUNT(*) FROM students;
SELECT AVG(age) FROM students;
SELECT major, COUNT(*) FROM students GROUP BY major;

ALTER TABLE students ADD level;
UPDATE students SET level = 'Undergraduate' WHERE name == 'Alice';
SELECT * FROM students WHERE level == 'Undergraduate';

SHOW TABLES;
DESCRIBE students;
```

### Join Example

```sql
CREATE TABLE grades (student, course, mark);
INSERT INTO grades VALUES ('Alice', 'DB', 78);
INSERT INTO grades VALUES ('Bob', 'SE', 82);

JOIN students AND grades ON name AND student;
```

---

## What Makes This Project Interesting?

This project is interesting because it focuses on the core logic behind relational databases rather than simply using an existing database system.

It demonstrates practical understanding of:

- query parsing
- relational data operations
- schema management
- local persistence
- backend validation
- client-server communication

For a small project, it still covers several important software engineering concepts in one place.

---

## Current Limitations

This is a learning-oriented project, so it intentionally stays lightweight.

Current limitations include:

- no transaction support
- no concurrency control
- no indexing
- no query optimization
- no full SQL standard support
- no `ORDER BY` or `LIMIT` execution yet
- local filesystem storage only

---

## Future Improvements

Possible next steps for the project:

- add `ORDER BY` and `LIMIT`
- support CSV import/export
- add indexing for faster lookups
- improve schema inspection and metadata output
- add automated tests and benchmarks
- build a REST API or web interface on top of the database server

---


Built as a small relational database project in Java, focused on:

- query parsing
- relational operations
- file-based persistence
- backend validation
- client-server database communication
