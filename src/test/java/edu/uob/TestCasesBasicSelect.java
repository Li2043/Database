package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestCasesBasicSelect
{
   DBServerHarness db;

   @BeforeEach
   void setup()
   {
      db = new DBServerHarness();
      db.createMovieDatabase();
   }

   @Test
   void testBasicWildstarSelect()
   {
      db.sendCommandAndCheckResponse("SELECT * FROM actors;", new String[]{"Toni","Emma","James","Hugh"}, new String[]{}, "Testing basic SELECT * query");
   }

   @Test
   void testStringEqualsConditionalSelect()
   {
      db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE name == 'Toni';", new String[]{"Toni","Australian"}, new String[]{"Emma","James","Hugh"}, "Testing string equals conditional SELECT query");
   }

   @Test
   void testIntegerEqualsConditionalSelect()
   {
      db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE awards == 10;", new String[]{"Emma","10"}, new String[]{"Toni","James","Hugh"}, "Testing integer equals conditional SELECT query");
   }

   @Test
   void testBooleanConditionalSelect()
   {
      db.sendCommandAndCheckResponse("SELECT * FROM movies WHERE UK == TRUE;", new String[]{"Boy","Sense"}, new String[]{"Mickey"}, "Testing boolean conditional SELECT query");
   }

   @Test
   void testStringNotEqualConditionalSelect()
   {
      db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE name != 'James';", new String[]{"Emma","Toni","Hugh"}, new String[]{"James"}, "Testing string not equals conditional SELECT query");
   }

   @Test
   void testIntegerComparisons()
   {
      // Check for "12 < 5" error (due to alphabetical rather than numberical comparison)
      db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE awards < 5;", new String[]{"Hugh"}, new String[]{"Toni","James","Emma"}, "Testing integer comparisons use numerical rather than character comparisons");
   }

   @Test
   void testSelectionOfSpecifiedColumns()
   {
      db.sendCommandAndCheckResponse("SELECT nationality FROM actors WHERE name == 'Hugh';", new String[]{"British"}, new String[]{"awards"}, "Testing selecting some specific columns in SELECT query");
   }

   @Test
   void testLikeQuery()
   {
      db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE name LIKE 'am';", new String[]{"James"}, new String[]{"Toni","Emma","Hugh"}, "Testing the LIKE query");
   }

   static class TestCasesIllegalNames
   {
      DBServerHarness db;

      @BeforeEach
      void setup()
      {
         db = new DBServerHarness();
         db.createMovieDatabase();
      }

      @Test
      void testPreventingSQLKeywordsAsTableNames()
      {
         db.sendCommandAndCheckResponse("CREATE TABLE SELECT (name, age);", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that SQL reserved keywords are prevented as table names");
         db.sendCommandAndCheckResponse("CREATE TABLE insert (name, age);", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that SQL reserved keywords are prevented as table names");
      }

      @Test
      void testPreventingSQLKeywordsAsAttributeNames()
      {
         db.sendCommandAndCheckResponse("CREATE TABLE stunts (table, UPDATE);", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that SQL reserved keywords are prevented as attribute names");
         db.sendCommandAndCheckResponse("CREATE TABLE crew (table, insert);", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that SQL reserved keywords are prevented as attribute names");
      }

      @Test
      void testPreventIDAsColumnName()
      {
         db.sendCommandAndCheckResponse("CREATE TABLE stunts (name, id);", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that 'id' can't be used as a column name");
      }

      @Test
      void testDuplicateAttributeName()
      {
         db.sendCommandAndCheckResponse("ALTER TABLE actors ADD name;", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that a table can't contain duplicate column names");
         db.sendCommandAndCheckResponse("CREATE TABLE stunts (name, name);", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that a table can't contain duplicate column names");
      }

      @Test
      void testDatabaseNameAlreadyTaken()
      {
         db.sendCommandAndIgnoreResponse("CREATE DATABASE existing;");
         db.sendCommandAndCheckResponse("CREATE DATABASE existing;", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that trying to create a database with an existing database name results in an [ERROR]");
      }

      @Test
      void testTableNameAlreadyTaken()
      {
         db.sendCommandAndCheckResponse("CREATE TABLE actors (name);", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that trying to create a table with an existing table name results in an [ERROR]");
      }

      @Test
      void testIllegalTableName()
      {
         db.sendCommandAndCheckResponse("CREATE TABLE $tunts (name);", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that trying to create a table with an illegal character causes an [ERROR]");
      }

      @Test
      void testIllegalColumnName()
      {
         db.sendCommandAndCheckResponse("CREATE TABLE stunts (person.name);", new String[]{"[ERROR]"}, new String[]{"[OK]"}, "Testing that trying to create a column with an illegal character causes an [ERROR]");
      }

   }

   static class TestCasesUpdate
   {
      DBServerHarness db;

      @BeforeEach
      void setup()
      {
         db = new DBServerHarness();
         db.createMovieDatabase();
      }

      @Test
      void testRowUpdate()
      {
         db.sendCommandAndIgnoreResponse("UPDATE actors SET awards = 11 WHERE name == 'Emma';");
         db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE name == 'Emma';", new String[]{"11"}, new String[]{"10"}, "Testing that Emma's awards were successfully updated to 11");
      }

      @Test
      void testAddingColumnToTable()
      {
         db.sendCommandAndIgnoreResponse("ALTER TABLE actors ADD age;");
         db.sendCommandAndCheckResponse("SELECT * FROM actors;", new String[]{"age"}, new String[]{}, "Testing altering table to add a new column called 'age'");
      }

      @Test
      void testInsertingIntoAlteredTable()
      {
         db.sendCommandAndIgnoreResponse("ALTER TABLE actors ADD age;");
         db.sendCommandAndIgnoreResponse("UPDATE actors SET age = 45 WHERE name == 'Hugh';");
         db.sendCommandAndCheckResponse("SELECT * FROM actors WHERE name == 'Hugh';", new String[]{"45"}, new String[]{}, "Testing updating an entry in a table after adding a new column and setting the age of Hugh to be 45");
      }

   }
}
