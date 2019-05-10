package comp1206.sushi;

import comp1206.sushi.server.Server;

import java.io.*;
import java.nio.file.NoSuchFileException;

public class DataPersistence implements Runnable, Serializable {

    private static final long serialVersionUID = 1L;
    private Server server;
    private String fileName;

    public DataPersistence(Server server, String fileName) {
        this.server = server;
        this.fileName = fileName;
        new Thread(this).start();
    }

    @Override
    public void run() {

        while(true) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e ) {
                e.printStackTrace();
            }


           save(server, fileName);
        }
    }


    public static void save(Object data, String fileName)  {

        try {
            File outFile = new File(fileName);
            FileOutputStream fileOutputStream = new FileOutputStream(outFile);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(data);
            //System.out.println("Successfully saved");
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public static Object load(String fileName)  {
        try {
            File inFile = new File(fileName);
            if(!inFile.exists())
                throw new NoSuchFileException("Specified file does not exist");
            FileInputStream fileInputStream = new FileInputStream(inFile);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            System.out.println("[DataPersistence] Loaded "  + fileName + " successfully");
            return objectInputStream.readObject();
        } catch (FileNotFoundException e) {
            System.out.println("Load file does not exist");
        }
        catch (IOException | ClassNotFoundException e) {
            System.out.println("Error loading file");
        }

        return null;
    }



}
