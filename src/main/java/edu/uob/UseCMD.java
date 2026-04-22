package edu.uob;

public class UseCMD extends DBCmd{
    private final String databaseName;

    public UseCMD(String dbName){
        this.databaseName = dbName.toLowerCase();
    }
    @Override
    public String query(DBServer server) {
        return "[ERROR] Legacy command handler is not used: " + this.databaseName;
    }
}
