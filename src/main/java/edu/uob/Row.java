package edu.uob;

import java.util.ArrayList;

public class Row {
    //保存row里面的数据
    private ArrayList<String> values;
    private int id;

    //constructor
    public Row (int id, ArrayList<String> row){
        this.id = id;
        this.values = row;
    }

    //method1: 读取row里面的数据
    public ArrayList<String> getValues(){
        return this.values;
    }

    @Override
    //convert arraylist new row into string separated by tabs
    public String toString (){
        String data = String.join("\t", this.values);
        return this.id + "\t" + data;
    }
}
