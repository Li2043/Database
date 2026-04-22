-- Demo script for GitHub and interview walkthrough
CREATE DATABASE portfolio;
USE portfolio;

CREATE TABLE sales (region, rep, amount);
INSERT INTO sales VALUES ('North', 'Alice', 1200);
INSERT INTO sales VALUES ('North', 'Bob', 900);
INSERT INTO sales VALUES ('South', 'Alice', 1500);
INSERT INTO sales VALUES ('South', 'Chris', 700);
INSERT INTO sales VALUES ('West', 'Bob', 1100);

-- Basic retrieval
SELECT * FROM sales;
SELECT rep, amount FROM sales WHERE amount >= 1000;

-- Aggregation and GROUP BY (analytics-oriented)
SELECT COUNT(*) FROM sales;
SELECT SUM(amount), AVG(amount) FROM sales;
SELECT region, COUNT(*) FROM sales GROUP BY region;
SELECT rep, SUM(amount) FROM sales GROUP BY rep;

-- Schema discoverability
SHOW TABLES;
DESCRIBE sales;
