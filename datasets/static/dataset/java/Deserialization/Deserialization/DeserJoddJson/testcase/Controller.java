<filename>interfacemodules/iscsi/src/main/java/org/treetank/iscsi/jscsi/TreetankConfiguration.java<fim_prefix>

package sample;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import jodd.json.JsonParser;
import jodd.json.JsonSerializer;
import jodd.json.Path;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    final String DATA_FILE_NAME = "todo.txt";
    String stringOfTodoItems;

    @FXML
    ListView todoList;

    @FXML
    TextField todoText;

    ObservableList<ToDoItem> todoItems = FXCollections.observableArrayList();
    ArrayList<ToDoItem> itemsFromFile;
    String line;
    HolderList holderList;

    String itemsOfTodo = "files/items.txt";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        todoList.setItems(todoItems);
        System.out.println("Hello initialize");
        holderList = new HolderList();
        holderList.toDoItems = new ArrayList<>();

        try {
            File file = new File("another.txt");
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);


            line = bufferedReader.readLine();
            getTodoItems();
            holderList.toDoItems.addAll(todoItems);

        } catch (FileNotFoundException e) {
            System.out.println("Cant find classes");

        } catch (Exception e) {

        }


    }

    public void addItem() {
        System.out.println("Adding item ...");
//        getTodoItems();
        ToDoItem toDoItem = new ToDoItem(todoText.getText());
        todoItems.add(toDoItem);
        todoText.setText("");
        holderList.toDoItems.add(toDoItem);

        stringOfTodoItems = jsonSave(holderList);
        System.out.println(stringOfTodoItems + "Hello String of to do items");

        File file = new File("another.txt");

        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(stringOfTodoItems);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            System.out.println(printWriter.toString());


//            while (true) {
//                String line = bufferedReader.readLine();
//                if (line == null) {
//                    break;
//                } else {
//                    fileWriter.write("items.txt");
//
//                }


        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void viewList() {

    }

    public void removeItem() {
        ToDoItem todoItem = (ToDoItem) todoList.getSelectionModel().getSelectedItem();
        System.out.println("Removing " + todoItem.text + " ...");
        todoItems.remove(todoItem);
        holderList.toDoItems.remove(todoItem);

        stringOfTodoItems = jsonSave(holderList);
        System.out.println(stringOfTodoItems + "Hello String of to do items");

        File file = new File("another.txt");

        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(stringOfTodoItems);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            System.out.println(printWriter.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void toggleItem() {
        System.out.println("Toggling item ...");
        ToDoItem todoItem = (ToDoItem) todoList.getSelectionModel().getSelectedItem();
        if (todoItem != null) {
            todoItem.isDone = !todoItem.isDone;


//            for (ToDoItem item:holderList.toDoItems) {
//                if (item.getText().equalsIgnoreCase(todoItem.getText())) {
//
//                    holderList.toDoItems.get(holderList.toDoItems.indexOf(todoItem)).isDone
//                            = !holderList.toDoItems.get(holderList.toDoItems.indexOf(todoItem)).isDone;
//                }
//
//            }
            todoList.setItems(null);
            todoList.setItems(todoItems);


        }

        stringOfTodoItems = jsonSave(holderList);
        System.out.println(stringOfTodoItems + "Hello String of to do items");

        File file = new File("another.txt");

        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(stringOfTodoItems);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public String jsonSave(HolderList holderList) {
        JsonSerializer jsonSerializer = new JsonSerializer().deep(true);
        String jsonString = jsonSerializer.serialize(holderList);
        return jsonString;
    }

    public ArrayList<ToDoItem> jsonRestore(String jsonTD) {

        JsonParser toDoItemParser = new JsonParser();
        <fim_suffix>
        return holderList.getToDoItems();
    }

    public ObservableList<ToDoItem> getTodoItems() {

        itemsFromFile = jsonRestore(line);
        todoItems.addAll(itemsFromFile);
        return todoItems;
    }
}
<fim_middle>