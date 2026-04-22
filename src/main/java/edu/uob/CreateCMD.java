package edu.uob;

public class CreateCMD extends DBCmd {
    private final String name;
    private final String type;

    //constructor
    public CreateCMD(String name, String type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String query(DBServer server) {
        return "[ERROR] Legacy command handler is not used";
    }
}
