package edu.uob;

public class SelectCMD extends DBCmd {
    @Override
    public String query(DBServer server) {
        return "[ERROR] Legacy command handler is not used";
    }
}
