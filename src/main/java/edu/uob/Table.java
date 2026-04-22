package edu.uob;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

//store data in table, ID control
public class Table {
    private String tableName;
    private ArrayList<String> columnNames;
    private ArrayList<Row> rows;
    private int nextID;

    //constructor构造函数。this.代表“这个对象自己的属性”
    public Table(String name, ArrayList<String> colNames){
        this.tableName = name;
        this.rows = new ArrayList<>();//新表开始时是空的
        this.nextID = 1;
        this.columnNames = colNames;
    }

    //constructor overloading (加载新表）
    public Table (String name) {
        this(name, null);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("id"); // 先放 ID 这一列
        for (String columnName : columnNames) {
            sb.append("\t").append(columnName);
        }
        sb.append("\n"); // 换行

        for (Row row : rows) {
            sb.append(row.toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    //INSERT
    //INSERT INTO marks VALUES ('Simon', 65);
    public void addRow (ArrayList<String> values){
        //1. 创建新的ArrayList for new row
        ArrayList<String> newRow = new ArrayList<String>();
        //3. 放入value
        newRow.addAll(values);
        //4. put new row into arraylist of rows
        //先把newRow封装进Row对象
        Row rowObject = new Row(nextID,newRow);
        this.rows.add(rowObject);
        //5. update nextID
        this.nextID ++;
    }

    public void saveTable(String folderPath) {
        //1. file path
        String filePath = folderPath + File.separator + this.tableName + ".tab";
        //2.write into file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            //2.1 write column names
            writer.write(String.join("\t", columnNames));
            //2.2 recursively write every row
            for (Row row : this.rows) {
                writer.write(row.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("[ERROR]" + e.getMessage());
        }
    }

    public void loadTable (String folderPath) {
        String filePath = folderPath + File.separator + this.tableName + ".tab";
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))){
            String line = reader.readLine();
            if (line != null){
                String[] colparts = line.split("\t");
                ArrayList<String> colNames = new ArrayList<>(Arrays.asList(colparts));
                this.columnNames = colNames;
            }
            //read remaining content
            while ((line = reader.readLine()) != null){
                String[] colparts = line.split("\t");
                ArrayList<String> dataList = new ArrayList<>(Arrays.asList(colparts));
                Row newRow = new Row(nextID,dataList);
                this.rows.add(newRow);
            }
        }catch (IOException e) {
            System.err.println("[ERROR]" + e.getMessage());
        }
        this.nextID = getNextID();
    }

    private int getNextID(){
        //get nextID=maxID+1, loop through IDs the find maxID
        int maxID = 0;
        for (Row row: rows){
            try{
                int ID = Integer.parseInt(row.getValues().get(0));
                if (ID > maxID) {
                    maxID = ID;
                }
            } catch (NumberFormatException e){
                System.err.println("[ERROR]" + e.getMessage());
            } catch (IndexOutOfBoundsException e){
                System.err.println("[ERROR]" + e.getMessage());
            }
        }
        return maxID + 1;
    }


}

