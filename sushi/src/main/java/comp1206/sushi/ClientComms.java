package comp1206.sushi;

import comp1206.sushi.client.Client;

import java.io.*;
import java.net.Socket;
import java.time.LocalTime;

import static java.time.temporal.ChronoUnit.SECONDS;


/**
 * ClientComms Class
 * Used for communication from the Client to the server
 */

public class ClientComms extends Thread implements Serializable{

    /**
     * @param clientStocket the client's socket
     * @param portNumber the port that the server is running on
     */

    private Socket clientSocket;
    private OutputStream outputStream;
    private ObjectOutputStream objectOutputStream;
    private InputStream inputStream;
    private ObjectInputStream objectInputStream;
    private Client clientInstace;

    private static int portNumber = 7777;
    private boolean clientRunning;
    private Thread clientThread;

    private MessageWrapper lastMsgSent;


    /**
     * ClientComms constructor
     */

    public ClientComms(Client client) {
        this.clientInstace = client;
        this.clientRunning = true;
        new Thread(this);
    }

    @Override
    public void run() {

        synchronized (this) {
            this.clientThread = Thread.currentThread();
        }


            try {
                    this.openSocket();
                    // Get the output stream from the socket.
                    outputStream = clientSocket.getOutputStream();
                    inputStream = clientSocket.getInputStream();

                    // Create an object output stream from the output stream so we can send an object through it
                    objectOutputStream = new ObjectOutputStream(outputStream);
                    objectInputStream = new ObjectInputStream(inputStream);



            } catch (IOException e ) {
                throw new RuntimeException("Error connecting the client");
            }
        }


    /**
     * Sends a message to the server that is listening
     * @param message to be sent
     * @throws IOException
     */

    public boolean sendMessage(MessageWrapper message) throws IOException{

        if(lastMsgSent == null) {
            lastMsgSent = message;
        } else {
            LocalTime time = LocalTime.now();
            LocalTime lastMsgTime = lastMsgSent.getCreationTime();
            long timeElapsed = Math.abs(SECONDS.between(time, lastMsgTime));
            String lastMsgStatus = lastMsgSent.getMsgStatus();
            lastMsgSent = message;
            if(timeElapsed < 5 && lastMsgStatus.equals(message.getMsgStatus())) return false;
        }

        if(clientSocket == null || clientSocket.isClosed())  {
            clientSocket = new Socket("localhost", portNumber);

            outputStream = clientSocket.getOutputStream();
            inputStream = clientSocket.getInputStream();

            // Create an object output stream from the output stream so we can send an object through it
            objectOutputStream = new ObjectOutputStream(outputStream);
            objectInputStream = new ObjectInputStream(inputStream);
        }

        System.out.println("Sending: " + ((MessageWrapper)message).getMsgStatus());
        objectOutputStream.writeUnshared(message);
        return true;
    }

    /**
     * Receive a message
     * @return the message
     * @throws IOException
     * @throws ClassNotFoundException
     */

    public Object receiveMessage() throws IOException, ClassNotFoundException {
        return objectInputStream.readObject();
    }

    /**
     * Opens the socket
     * @throws IOException
     */
    private void openSocket()  throws IOException {
            clientSocket = new Socket("localhost", portNumber);

    }

    /**
     * Close the client socket
     */

    public synchronized void stopClient(){
        this.clientRunning = false;
        try {
            this.clientSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);
        }
    }

    public ObjectOutputStream getObjectOutputStream() {
        return objectOutputStream;
    }
}

