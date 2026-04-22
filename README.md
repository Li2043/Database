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
