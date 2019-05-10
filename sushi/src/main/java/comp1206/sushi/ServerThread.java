package comp1206.sushi;

import comp1206.sushi.common.Order;
import comp1206.sushi.common.User;
import comp1206.sushi.server.Server;
import javafx.util.Pair;

import java.io.*;
import java.net.Socket;

/**
 * ServerThread class
 * A server worker class
 * New instance is created for each client connection
 */

public class ServerThread extends Thread {

    private transient Socket clientSocket = null;
    private Server server;
    boolean active;
    User loggedInUser = null;


    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;

    public ServerThread(Socket clientSocket, Server server) {

        this.server = server;
        this.clientSocket = clientSocket;
        this.active = true;
    }

    public void run() {
        try {
            System.out.println("Connection from : " + clientSocket);

            InputStream input = clientSocket.getInputStream();
            OutputStream output = clientSocket.getOutputStream();

            objectInputStream = new ObjectInputStream(input);
            objectOutputStream = new ObjectOutputStream(output);

            while (this.isActive()) {
                processRequest(objectInputStream);
            }

            /**
             * do stuff
             */

            output.close();
            input.close();
        } catch (IOException e) {
            //report exception somewhere.
            e.printStackTrace();
        } finally {
            if(loggedInUser != null) {
                server.getServerComms().removeConnectedUser(loggedInUser);
            }
        }
    }

    /**
     * Process user requests
     * @param objectInputStream
     */

    private void processRequest(ObjectInputStream objectInputStream) {

        MessageWrapper message = null;
        String messageStatus = null;
        boolean exceptionRaised = false;


        try {
            message = (MessageWrapper)objectInputStream.readObject();
            messageStatus = message.getMsgStatus();
        } catch (ClassNotFoundException | IOException | NullPointerException e) {
            System.out.println("User disconnected");
            if(loggedInUser!= null) {
                server.getServerComms().removeConnectedUser(loggedInUser);
            }
            active = false;
            exceptionRaised = true;
        }

        if(!exceptionRaised) {
            if (messageStatus.equals("getPostcodes")) {
                sendToClient(this.server.getPostcodes(), this.getObjectOutputStream());
            } else if (messageStatus.equals("registerUser")) {
                User user = (User) message.getMsgContent();
                boolean successfullRegister = this.server.registerUser(user);

                if(successfullRegister) {
                        System.out.println("kek");
                        server.getServerComms().addConnectedUser(user);
                        this.loggedInUser = user;
                }

                sendToClient(new Boolean(successfullRegister), this.getObjectOutputStream());
            } else if (messageStatus.equals("loginUser")) {
                Pair<String, String> user = (Pair) message.getMsgContent();

                String name = user.getKey();
                String pass = user.getValue();

                User successfulLogin = server.checkLogin(name, pass);

                if(successfulLogin != null){
                    System.out.println("kek");
                    server.getServerComms().addConnectedUser(successfulLogin);
                    this.loggedInUser = successfulLogin;
                }

                sendToClient(successfulLogin, this.getObjectOutputStream());
            } else if (messageStatus.equals("createOrder")) {
                Order order = (Order) message.getMsgContent();
                Order temp = server.addOrder(order);

                if(temp == null) {
                    sendToClient(new Boolean(false), this.getObjectOutputStream());
                } else if(temp.getStatus().equals("Being delivered")) {
                    sendToClient(new Boolean(true), this.getObjectOutputStream());
                }
                else {
                    sendToClient(new Boolean(true), this.getObjectOutputStream());
                }
            } else if (messageStatus.equals("getDishes")) {
                sendToClient(server.getDishes(), this.getObjectOutputStream());
            } else if(messageStatus.equals("getRestaurant")) {
                sendToClient(this.server.getRestaurant(), this.getObjectOutputStream());
            } else if(messageStatus.equals("cancelOrder")) {
                Order order = (Order) message.getMsgContent();
                for(Order o : server.getOrders()) {
                    if(order.getName().equals(o.getName()) && !order.getStatus().equals("Delivered")){
                        server.cancelOrder(o);
                        break;
                    }
                }
            } else if(messageStatus.equals("getOrders")){
                User user = (User) message.getMsgContent();
                for(User u : server.getUsers()) {
                    if(u.getName().equals(user.getName())){
                        sendToClient(u.getUserOrders(), this.getObjectOutputStream());
                        break;
                    }
                }
            }
        }
    }


    /**
     * Send a message to a client
     * @param message the object to be sent
     * @param objectOutputStream the client's output stream
     */


    protected void sendToClient(Object message, ObjectOutputStream objectOutputStream) {

        if(!(this.clientSocket.isClosed() || this.clientSocket == null)) {

            try {
                objectOutputStream.reset();
                objectOutputStream.writeUnshared(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public ObjectInputStream getObjectInputStream() {
        return objectInputStream;
    }

    public ObjectOutputStream getObjectOutputStream() {
        return objectOutputStream;
    }

    private boolean isActive() { return active; }


    private void stopThread() { this.active = false;}
}
