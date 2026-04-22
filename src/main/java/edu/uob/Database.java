package edu.uob;

import java.util.HashMap;

public class Database {
    //class中变量
    private String databaseName;
    //用hashmap在database内部查找table
    public HashMap<String, Table> tables;

    //constructor
    public Database(String name){
        this.databaseName = name;
        this.tables = new HashMap<>();
    }

    public String getDbName() {
        return this.databaseName;
    }

    public Table getTable (String tableName){
        if (tableName == null) return null;
        return tables.get(tableName.toLowerCase());
    }

    public void addTable (String tableName, Table table){
        if (tableName != null && table != null){
            tables.put(tableName.toLowerCase(), table);
        }
    }

    public void removeTable (String tableName){
        if (tableName != null) {
            tables.remove(tableName.toLowerCase());
        }
    }
}
