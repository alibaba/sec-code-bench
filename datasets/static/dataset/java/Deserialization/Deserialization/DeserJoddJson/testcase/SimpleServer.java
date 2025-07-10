<filename>SimpleServer.java<fim_prefix>

package tiy.networking;

import jodd.json.JsonParser;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import static sun.misc.PostVMInitHook.run;
import java.util.Scanner;
import jodd.json.JsonParser; // File + project structure + Library + GREEN Plus sign + Maven + org.jodd:jodd-json:3.6.7
import jodd.json.JsonSerializer;
// = to use Jodd.


public class SimpleServer {

    public ArrayList<Message> messageThings = new ArrayList<Message>(); /// delcared in GLobal
    MessageLog myLog = new MessageLog(); // make an instance/object of a class MessageLog



    public static void main(String[] args) throws Exception {
        new SimpleServer().serverThread();  //how to call a Non-static method=serverThread(), WithIn a static method.
        // making a new instance of the whole server.
    }


    public void serverThread() {
        try {

            // start a server on a specific port
            ServerSocket serverListener = new ServerSocket(8005); // listens on port:8005, i want to start a server on port
            // 8005,2001 or 2002
            // type in localhost:8005 in web browser, cre8s a server in a port. got a incoming connection.
            // blocking call





            SaveServerThread oldyThread = new SaveServerThread(this);
            Thread newyThread = new Thread(oldyThread); // cre8 new thread, then pass the instance of that class.
            newyThread.start();

            MessageLog returnItems = retrieveServer();
            if (returnItems != null) {
                messageThings.addAll(returnItems.messageItems);
            }

            while (true) { // loop connection to accept multple Client IP.
                Socket clientSocket = serverListener.accept(); // accept 1 connection
                ServerThread oldThread = new ServerThread(clientSocket, this); // passing clientSocket parameter to
                // ServerThread class
                Thread newThread = new Thread(oldThread); // cre8 new thread, then pass the instance of that class.
                newThread.start();
            }
        }catch (IOException exception){

        }
    }
    @Override
    public String toString() {
        String history = "Message History= \r\n"; // \r\n
        String lineBreaks = "\r\n";
        for (int counter = 0; counter < messageThings.size(); counter++) {
            System.out.println(messageThings.get(counter));
        }
        history = history + lineBreaks;

        return history;
    }

    public void saveServer()  {
        System.out.println("Attempting 2 save ");

            try {
                System.out.println("Saving server started . ..");
                File serverFile = new File("serverFile.json"); // cre8ing a new file named FN, & is being stored into
                FileWriter serverWriter = new FileWriter(serverFile); //  cre8ing a new FileWriter while taking in
                // parameter of my file variable = serverFile, & storing it into testWriter.
                JsonSerializer jsonserializer = new JsonSerializer().deep(true); //  cre8ing a new JsonSerializer &
                // storing it in jsonserializer variable.

                myLog.messageItems.addAll(messageThings); // adding all observable list inside array list whch is inside
                // todoiteslist class

                String jsonString = jsonserializer.serialize(myLog); // serializing todoItem & storing it as a
                // jsonString.


                serverWriter.write(jsonString); // writing whatever is in -> () <- to file .
                System.out.println("file saving");
                serverWriter.close(); // closing
                System.out.println("file closed");

            } catch (Exception exception) {
                exception.printStackTrace();
            }
    }



    public MessageLog retrieveServer() { // named retrieveItem to Return the Item.

        try{
            Scanner fileScanner = new Scanner(new File("serverFile.json"));
            fileScanner.useDelimiter("\\Z"); // read the input until the 'end of the input'.
            String fileContents = fileScanner.next();
            JsonParser ControllerParser = new JsonParser();
            <fim_suffix>
            System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            System.out.println("            Restoring previous Version");
            System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

            return returnItems;

        } catch (IOException ioexception) { // if we can't find the file or run into an issue restoring the object
            // from the file, just return null, so the callrt knows to create an object from
            return null;
        }
    }
}
<fim_middle>