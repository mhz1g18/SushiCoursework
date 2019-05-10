package comp1206.sushi;

import comp1206.sushi.common.User;
import comp1206.sushi.server.Server;

import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


/**
 * ServerComms class
 * Used to run the server's communications
 * The server is multithreaded
 * Each client connection runs on a separate thread
 */

public class ServerComms implements Runnable, Serializable {

    /**
     * @param server an instance of the server class
     * @param serverPort the port that communications will take place on
     * @param isStopped true if ServerComms is disabled
     * @param clientThreads all connected clients
     */

    private transient Server server;
    private int serverPort;
    private boolean isStopped;
    private transient ServerSocket serverSocket;
    private transient List<ServerThread> clientThreads;
    private List<User> connectedUsers;

    public ServerComms(Server server) {
        this.server = server;
        this.serverPort = 7777;
        this.isStopped = false;
       // runningThread = null;
        serverSocket = null;
        clientThreads = new ArrayList<>();
        connectedUsers = new ArrayList<>();
        new Thread(this).start();
    }

    public void run() {
       // synchronized(this){
          //  this.runningThread = Thread.currentThread();
       // }

        openServerSocket();

        /**
         * Run the server as long as isStopped() returns false
         */

        while(!isStopped()) {

            if(serverSocket == null)
                openServerSocket();

            Socket clientSocket = null;
            try {
                clientSocket = this.serverSocket.accept();
                ServerThread newClient = new ServerThread(clientSocket, server);
                clientThreads.add(newClient);
                newClient.start();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }

        }

        System.out.println("Server Stopped.") ;
    }

    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    /**
     * Stop the server
     */

    public synchronized void stop(){
        this.isStopped = true;
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);
        }
    }

    /**
     * Open the server socket
     */

    private synchronized void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.serverPort);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port " + serverPort, e);
        }
    }

    /**
     * Update all connected clients
     * @param message to be sent
     */

    public synchronized void updateClients(Object message) {

        for(ServerThread client : clientThreads) {
            client.sendToClient(message, client.getObjectOutputStream());
        }
    }


    public synchronized List<User> getConnectedUsers() {
        return this.connectedUsers;
    }

    public synchronized void addConnectedUser(User user) {
        this.connectedUsers.add(user);
    }

    public synchronized void removeConnectedUser(User user) {
        this.connectedUsers.remove(user);
    }


}
