package edu.uob.databases;

import edu.uob.DBCmd;
import edu.uob.DBServer;

public class InsertCMD extends DBCmd {
    @Override
    public String query(DBServer server) {
        return "[ERROR] Legacy command handler is not used";
    }
}