# Demo Run Transcript

This transcript can be used as a portfolio artifact when introducing the project.

## Commands

```sql
CREATE DATABASE portfolio;
USE portfolio;
CREATE TABLE sales (region, rep, amount);
INSERT INTO sales VALUES ('North', 'Alice', 1200);
INSERT INTO sales VALUES ('North', 'Bob', 900);
INSERT INTO sales VALUES ('South', 'Alice', 1500);
INSERT INTO sales VALUES ('South', 'Chris', 700);
INSERT INTO sales VALUES ('West', 'Bob', 1100);
SELECT COUNT(*) FROM sales;
SELECT SUM(amount), AVG(amount) FROM sales;
SELECT region, COUNT(*) FROM sales GROUP BY region;
```

## Expected Output Snapshot

```text
[OK]
COUNT(*)
5

[OK]
SUM(amount)	AVG(amount)
5400	1080

[OK]
region	COUNT(*)
North	2
South	2
West	1
```

If you want to attach a visual artifact on GitHub, run the same commands in terminal and add screenshots/GIF under `docs/`.
